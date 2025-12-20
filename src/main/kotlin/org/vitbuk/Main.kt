package org.vitbuk

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId.Companion.fromId
import com.github.kotlintelegrambot.entities.User
import org.vitbuk.draw.SattoloDrawAlgorithm
import org.vitbuk.model.Event
import org.vitbuk.model.EventState
import org.vitbuk.model.Participant
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_EVENT_NAME = "Тайный Сайта 🎁"

fun main() {
    val token = System.getenv("BOT_TOKEN")
        ?: error("Env BOT_TOKEN is not set. Put it in .env (BOT_TOKEN=...) or export BOT_TOKEN=...")

    val algorithm = SattoloDrawAlgorithm()

    val eventsByChatId = ConcurrentHashMap<Long, Event>()

    val locksByChatId = ConcurrentHashMap<Long, Any>()
    fun lockFor(chatId: Long): Any = locksByChatId.computeIfAbsent(chatId) { Any() }

    val bot = bot {
        this.token = token

        dispatch {

            command("help") {
                bot.sendMessage(
                    chatId = fromId(message.chat.id),
                    text = """
                        Команды (в группе):
                        /create [название] — создать ивент (хост)
                        /cancel - хост отменяет ивент
                        /join — зарегистрироваться
                        /leave — выйти
                        /list — список участни:ц
                        /start_event — провести жеребьёвку (хост)

                        Важно: чтобы получить результат в ЛС — открой личку с ботом и нажми /start.
                    """.trimIndent()
                )
            }

            command("start") {
                bot.sendMessage(
                    chatId = fromId(message.chat.id),
                    text = "Привет! Если ты участни:ца Secret Santa — теперь я смогу писать тебе в ЛС ✅\n" +
                            "Вернись в группу и жди жеребьёвку."
                )
            }

            command("create") {
                val chat = message.chat
                if (!isGroupChat(chat.type)) {
                    bot.sendMessage(fromId(chat.id), "Эта команда работает только в группе/супергруппе.")
                    return@command
                }

                val from = message.from ?: run {
                    bot.sendMessage(fromId(chat.id), "Не смог определить автор:ку сообщения.")
                    return@command
                }

                val eventName = args.joinToString(" ").trim().ifBlank { DEFAULT_EVENT_NAME }

                val lock = lockFor(chat.id)
                synchronized(lock) {
                    val existing = eventsByChatId[chat.id]
                    if (existing != null && existing.state != EventState.FINISHED) {
                        bot.sendMessage(
                            fromId(chat.id),
                            "Ивент уже существует: «${existing.name}» (статус: ${existing.state})."
                        )
                        return@command
                    }

                    val host = from.toParticipant()

                    val event = Event(
                        chatId = chat.id,
                        name = eventName,
                        hostUserId = from.id,
                        createdAt = Instant.now(),
                        state = EventState.REGISTRATION,
                        participants = linkedMapOf(from.id to host),
                        drawResult = null
                    )

                    eventsByChatId[chat.id] = event

                    bot.sendMessage(
                        fromId(chat.id),
                        "Создан ивент «${event.name}».\nХост: ${host.display()}\n\nПишите /join чтобы зарегистрироваться."
                    )
                }
            }

            command("join") {
                val chat = message.chat
                if (!isGroupChat(chat.type)) {
                    bot.sendMessage(fromId(chat.id), "Регистрироваться нужно в группе, где проходит ивент.")
                    return@command
                }

                val from = message.from ?: return@command
                val lock = lockFor(chat.id)

                synchronized(lock) {
                    val event = eventsByChatId[chat.id]
                    if (event == null) {
                        bot.sendMessage(fromId(chat.id), "Сначала создай ивент командой /create.")
                        return@command
                    }
                    if (event.state != EventState.REGISTRATION) {
                        bot.sendMessage(fromId(chat.id), "Регистрация закрыта (статус: ${event.state}).")
                        return@command
                    }
                    if (event.participants.containsKey(from.id)) {
                        bot.sendMessage(fromId(chat.id), "Ты уже зарегистрирован:а 🙂")
                        return@command
                    }

                    val p = from.toParticipant()
                    event.participants[from.id] = p

                    bot.sendMessage(
                        fromId(chat.id),
                        "✅ ${p.display()} зарегистрирован:а. Сейчас участни:ц : ${event.participants.size}"
                    )
                }
            }

            command("leave") {
                val chat = message.chat
                if (!isGroupChat(chat.type)) return@command
                val from = message.from ?: return@command
                val lock = lockFor(chat.id)

                synchronized(lock) {
                    val event = eventsByChatId[chat.id]
                    if (event == null) {
                        bot.sendMessage(fromId(chat.id), "Ивент не найден. /create")
                        return@command
                    }
                    if (event.state != EventState.REGISTRATION) {
                        bot.sendMessage(fromId(chat.id), "Нельзя выйти — регистрация закрыта (статус: ${event.state}).")
                        return@command
                    }

                    val removed = event.participants.remove(from.id)
                    if (removed == null) {
                        bot.sendMessage(fromId(chat.id), "Тебя нет в списке участни:ц.")
                        return@command
                    }

                    bot.sendMessage(
                        fromId(chat.id),
                        "➖ ${removed.display()} вышл:а. Сейчас участни:ц: ${event.participants.size}"
                    )
                }
            }

            command("list") {
                val chat = message.chat
                if (!isGroupChat(chat.type)) return@command
                val lock = lockFor(chat.id)

                val text = synchronized(lock) {
                    val event = eventsByChatId[chat.id]
                        ?: return@synchronized "Ивент не найден. Создай /create"

                    val people = event.participants.values.joinToString("\n") { "• ${it.display()}" }
                    "Ивент: «${event.name}» (статус: ${event.state})\nУчастни:цы (${event.participants.size}):\n$people"
                }

                bot.sendMessage(fromId(chat.id), text)
            }

            command("cancel") {
                val chat = message.chat
                if (!isGroupChat(chat.type)) {
                    bot.sendMessage(fromId(chat.id), "Эта команда работает только в группе/супергруппе.")
                    return@command
                }

                val from = message.from ?: return@command
                val lock = lockFor(chat.id)

                val replyText = synchronized(lock) {
                    val event = eventsByChatId[chat.id]
                        ?: return@synchronized "Ивент не найден. Нечего отменять."

                    if (event.hostUserId != from.id) {
                        return@synchronized "Только хост может отменить ивент."
                    }

                    event.state = EventState.CANCELLED
                    eventsByChatId.remove(chat.id)

                    "🛑 Ивент «${event.name}» отменён хостом.\nТеперь можно создать новый: /create"
                }

                bot.sendMessage(fromId(chat.id), replyText)
            }
            command("start_event") {
                val chat = message.chat
                if (!isGroupChat(chat.type)) {
                    bot.sendMessage(fromId(chat.id), "Эта команда работает только в группе/супергруппе.")
                    return@command
                }

                val from = message.from ?: return@command
                val lock = lockFor(chat.id)

                data class Snapshot(
                    val eventName: String,
                    val participants: Map<Long, Participant>,
                    val assignments: Map<Long, Long>,
                    val warnings: List<String>
                )

                val snapshot: Snapshot = synchronized(lock) {
                    val event = eventsByChatId[chat.id]
                    if (event == null) {
                        bot.sendMessage(fromId(chat.id), "Ивент не найден. /create")
                        return@command
                    }
                    if (event.hostUserId != from.id) {
                        bot.sendMessage(fromId(chat.id), "Только хост может запускать жеребьёвку.")
                        return@command
                    }
                    if (event.state != EventState.REGISTRATION) {
                        bot.sendMessage(fromId(chat.id), "Жеребьёвка уже запускалась/регистрация закрыта (статус: ${event.state}).")
                        return@command
                    }

                    val participants = event.participants.values.toList()
                    if (participants.size < 2) {
                        bot.sendMessage(fromId(chat.id), "Нужно минимум 2 участни:цы.")
                        return@command
                    }

                    val result = algorithm.draw(participants)
                    event.drawResult = result
                    event.state = EventState.STARTED

                    Snapshot(
                        eventName = event.name,
                        participants = LinkedHashMap(event.participants),
                        assignments = result.assignments.toMap(),
                        warnings = result.warnings
                    )
                }

                val failed = mutableListOf<Participant>()

                for ((giverId, receiverId) in snapshot.assignments) {
                    val giver = snapshot.participants[giverId] ?: continue
                    val receiver = snapshot.participants[receiverId] ?: continue

                    val dmChatId = fromId(giverId)
                    val dmText = "🎁 Жеребьёвка для «${snapshot.eventName}»\n" +
                            "Ты даришь: ${receiver.display()}"

                    val sendRes = bot.sendMessage(dmChatId, dmText)
                    sendRes.fold(
                        { /* ok */ },
                        { failed += giver }
                    )
                }

                val groupMsg = buildString {
                    append("✅ Жеребьёвка проведена! Результаты отправлены в ЛС.\n")

                    if (snapshot.warnings.isNotEmpty()) {
                        append("\n⚠️ Замечания:\n")
                        snapshot.warnings.forEach { append("• ").append(it).append('\n') }
                    }

                    if (failed.isNotEmpty()) {
                        append("\n⚠️ Не смог написать в ЛС этим людям (скорее всего, они не нажали /start в личке с ботом):\n")
                        append(failed.joinToString("\n") { "• ${it.display()}" })
                    }
                }

                bot.sendMessage(fromId(chat.id), groupMsg)
            }
        }
    }

    bot.startPolling()
}

private fun isGroupChat(type: String?): Boolean =
    type == "group" || type == "supergroup"

private fun User.toParticipant(): Participant =
    Participant(
        userId = this.id,
        username = this.username,
        firstName = this.firstName,
        lastName = this.lastName
    )
