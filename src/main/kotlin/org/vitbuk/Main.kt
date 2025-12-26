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
import org.vitbuk.service.StartEventAttempt
import org.vitbuk.service.StartEventService
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_EVENT_NAME = "Тайный Санта 🎁"

fun main() {
    val algorithm = SattoloDrawAlgorithm(reseedEachDraw = true)
    val startEventService = StartEventService(algorithm)
    val dmReadyUserIds = ConcurrentHashMap.newKeySet<Long>()
    val token = System.getenv("BOT_TOKEN")
        ?: error("Env BOT_TOKEN is not set. Put it in .env (BOT_TOKEN=...) or export BOT_TOKEN=...")
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
                        /cancel — отменить ивент (только хост)
                        /join — зарегистрироваться
                        /leave — выйти
                        /list — список участни:ц и готовность ЛС
                        /start_event — провести жеребьёвку (только хост)

                        Важно: чтобы получить результат в ЛС — открой личку с ботом и нажми /start.
                    """.trimIndent()
                )
            }

            command("start") {
                val chat = message.chat
                val from = message.from

                if (isPrivateChat(chat.type) && from != null) {
                    dmReadyUserIds += from.id

                    bot.sendMessage(
                        chatId = fromId(chat.id),
                        text = "✅ Отлично! Теперь я могу писать тебе в ЛС.\n" +
                                "Вернись в группу и жди жеребьёвку 🎁"
                    )
                } else {
                    bot.sendMessage(
                        chatId = fromId(chat.id),
                        text = "Чтобы я смог отправить тебе результат жеребьёвки — открой личку с ботом и нажми /start."
                    )
                }
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
                val replyText = synchronized(lock) {
                    val existing = eventsByChatId[chat.id]
                    if (existing != null && existing.state != EventState.FINISHED) {
                        return@synchronized "Ивент уже существует: «${existing.name}» (статус: ${existing.state}).\n" +
                                "Если нужно начать заново — хост может сделать /cancel."
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

                    buildString {
                        append("Создан ивент «${event.name}».\n")
                        append("Хост: ${host.display()}\n\n")
                        append("Пишите /join чтобы зарегистрироваться.\n")
                        append("Важно: кажд:ая участни:ца долж:на нажать /start в личке с ботом, иначе жеребьёвка не стартанёт.")
                    }
                }

                bot.sendMessage(fromId(chat.id), replyText)
            }

            command("join") {
                val chat = message.chat
                if (!isGroupChat(chat.type)) {
                    bot.sendMessage(fromId(chat.id), "Регистрироваться нужно в группе, где проходит ивент.")
                    return@command
                }

                val from = message.from ?: return@command
                val lock = lockFor(chat.id)

                val replyText = synchronized(lock) {
                    val event = eventsByChatId[chat.id]
                        ?: return@synchronized "Сначала создай ивент командой /create."

                    if (event.state != EventState.REGISTRATION) {
                        return@synchronized "Регистрация закрыта (статус: ${event.state})."
                    }

                    if (event.participants.containsKey(from.id)) {
                        return@synchronized "Ты уже зарегистрирован:а 🙂"
                    }

                    val p = from.toParticipant()
                    event.participants[from.id] = p

                    val readyMark = if (from.id in dmReadyUserIds) "✅" else "❌"

                    buildString {
                        append("✅ ${p.display()} зарегистрирован:а. Сейчас участни:ц: ${event.participants.size}\n")
                        if (readyMark == "❌") {
                            append("⚠️ Чтобы получить результат в ЛС — открой личку с ботом и нажми /start.")
                        }
                    }
                }

                bot.sendMessage(fromId(chat.id), replyText)
            }

            command("leave") {
                val chat = message.chat
                if (!isGroupChat(chat.type)) return@command

                val from = message.from ?: return@command
                val lock = lockFor(chat.id)

                val replyText = synchronized(lock) {
                    val event = eventsByChatId[chat.id]
                        ?: return@synchronized "Ивент не найден. /create"

                    if (event.state != EventState.REGISTRATION) {
                        return@synchronized "Нельзя выйти — регистрация закрыта (статус: ${event.state})."
                    }

                    val removed = event.participants.remove(from.id)
                        ?: return@synchronized "Тебя нет в списке участни:ц."

                    "➖ ${removed.display()} вышл:а. Сейчас участни:ц: ${event.participants.size}"
                }

                bot.sendMessage(fromId(chat.id), replyText)
            }

            command("list") {
                val chat = message.chat
                if (!isGroupChat(chat.type)) return@command

                val lock = lockFor(chat.id)

                val text = synchronized(lock) {
                    val event = eventsByChatId[chat.id]
                        ?: return@synchronized "Ивент не найден. Создай /create"

                    val people = event.participants.values.joinToString("\n") { p ->
                        val ready = if (p.userId in dmReadyUserIds) "✅" else "❌"
                        "• $ready ${p.display()}"
                    }

                    "Ивент: «${event.name}» (статус: ${event.state})\n" +
                            "Участни:цы (${event.participants.size}):\n$people\n\n" +
                            "✅ = готовы получать подарки!\n" +
                            "❌ = ещё не нажал:а /start в личке"
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

                val attempt: StartEventAttempt = synchronized(lock) {
                    val event = eventsByChatId[chat.id]
                        ?: return@synchronized StartEventAttempt.NotReady(
                            missing = emptyList(),
                            message = "Ивент не найден. /create"
                        )

                    if (event.hostUserId != from.id) {
                        return@synchronized StartEventAttempt.NotReady(
                            missing = emptyList(),
                            message = "Только хост может запускать жеребьёвку."
                        )
                    }

                    if (event.state != EventState.REGISTRATION) {
                        return@synchronized StartEventAttempt.NotReady(
                            missing = emptyList(),
                            message = "Жеребьёвка уже запускалась/регистрация закрыта (статус: ${event.state})."
                        )
                    }

                    if (event.participants.size < 2) {
                        return@synchronized StartEventAttempt.NotReady(
                            missing = emptyList(),
                            message = "Нужно минимум 2 участни:цы."
                        )
                    }

                    startEventService.start(event, dmReadyUserIds)
                }

                when (attempt) {
                    is StartEventAttempt.NotReady -> {
                        bot.sendMessage(fromId(chat.id), attempt.message)
                        return@command
                    }

                    is StartEventAttempt.Started -> {
                        val snapshot = attempt.snapshot

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
                                append("\n⚠️ Не смог написать в ЛС этим людям (возможно, они заблокировали бота):\n")
                                failed.forEach { append("• ").append(it.display()).append('\n') }
                            }
                        }

                        bot.sendMessage(fromId(chat.id), groupMsg)
                    }
                }
            }
        }
    }

    bot.startPolling()
}

private fun isGroupChat(type: String?): Boolean =
    type == "group" || type == "supergroup"

private fun isPrivateChat(type: String?): Boolean =
    type == "private"

private fun User.toParticipant(): Participant =
    Participant(
        userId = this.id,
        username = this.username,
        firstName = this.firstName,
        lastName = this.lastName
    )
