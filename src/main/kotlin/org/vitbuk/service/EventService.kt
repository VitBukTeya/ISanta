package org.vitbuk.service

import org.vitbuk.model.Event
import org.vitbuk.model.EventState
import org.vitbuk.model.Participant
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class EventService(
    private val startEventService: StartEventService,
    private val defaultEventName: String = "Тайный Санта 🎁"
) {
    private val dmReadyUserIds = ConcurrentHashMap.newKeySet<Long>()
    private val eventsByChatId = ConcurrentHashMap<Long, Event>()
    private val locksByChatId = ConcurrentHashMap<Long, Any>()
    private fun lockFor(chatId: Long): Any = locksByChatId.computeIfAbsent(chatId) { Any() }

    fun markDmReady(userId: Long) {
        dmReadyUserIds += userId
    }

    fun isDmReady(userId: Long): Boolean = userId in dmReadyUserIds

    fun create(chatId: Long, host: Participant, eventNameRaw: String?): String {
        val eventName = eventNameRaw?.trim().orEmpty().ifBlank { defaultEventName }

        return synchronized(lockFor(chatId)) {
            val existing = eventsByChatId[chatId]
            if (existing != null && existing.state != EventState.FINISHED) {
                return@synchronized "Ивент уже существует: «${existing.name}» (статус: ${existing.state}).\n" +
                        "Если нужно начать заново — хост может сделать /cancel."
            }

            val event = Event(
                chatId = chatId,
                name = eventName,
                hostUserId = host.userId,
                createdAt = Instant.now(),
                state = EventState.REGISTRATION,
                participants = linkedMapOf(host.userId to host),
                drawResult = null
            )
            eventsByChatId[chatId] = event

            buildString {
                append("Создан ивент «${event.name}».\n")
                append("Хост: ${host.display()}\n\n")
                append("Пишите /join чтобы зарегистрироваться.\n")
                append("Важно: кажд:ая участни:ца долж:на нажать /start в личке с ботом, иначе жеребьёвка не стартанёт.")
            }
        }
    }

    fun join(chatId: Long, participant: Participant): String {
        return synchronized(lockFor(chatId)) {
            val event = eventsByChatId[chatId]
                ?: return@synchronized "Сначала создай ивент командой /create."

            if (event.state != EventState.REGISTRATION) {
                return@synchronized "Регистрация закрыта (статус: ${event.state})."
            }

            if (event.participants.containsKey(participant.userId)) {
                return@synchronized "Ты уже зарегистрирован:а 🙂"
            }

            event.participants[participant.userId] = participant

            val readyMark = if (isDmReady(participant.userId)) "✅" else "❌"

            buildString {
                append("✅ ${participant.display()} зарегистрирован:а. Сейчас участни:ц: ${event.participants.size}\n")
                if (readyMark == "❌") {
                    append("⚠️ Чтобы получить результат в ЛС — открой личку с ботом и нажми /start.")
                }
            }
        }
    }

    fun leave(chatId: Long, userId: Long): String {
        return synchronized(lockFor(chatId)) {
            val event = eventsByChatId[chatId]
                ?: return@synchronized "Ивент не найден. /create"

            if (event.state != EventState.REGISTRATION) {
                return@synchronized "Нельзя выйти — регистрация закрыта (статус: ${event.state})."
            }

            val removed = event.participants.remove(userId)
                ?: return@synchronized "Тебя нет в списке участни:ц."

            "➖ ${removed.display()} вышл:а. Сейчас участни:ц: ${event.participants.size}"
        }
    }

    fun list(chatId: Long): String {
        return synchronized(lockFor(chatId)) {
            val event = eventsByChatId[chatId]
                ?: return@synchronized "Ивент не найден. Создай /create"

            val people = event.participants.values.joinToString("\n") { p ->
                val ready = if (isDmReady(p.userId)) "✅" else "❌"
                "• $ready ${p.display()}"
            }

            "Ивент: «${event.name}» (статус: ${event.state})\n" +
                    "Участни:цы (${event.participants.size}):\n$people\n\n" +
                    "✅ = готовы получать результат в ЛС\n" +
                    "❌ = ещё не нажал:а /start в личке"
        }
    }

    fun cancel(chatId: Long, requesterId: Long): String {
        return synchronized(lockFor(chatId)) {
            val event = eventsByChatId[chatId]
                ?: return@synchronized "Ивент не найден. Нечего отменять."

            if (event.hostUserId != requesterId) {
                return@synchronized "Только хост может отменить ивент."
            }

            event.state = EventState.CANCELLED
            eventsByChatId.remove(chatId)

            "🛑 Ивент «${event.name}» отменён хостом.\nТеперь можно создать новый: /create"
        }
    }

    fun startEvent(chatId: Long, requesterId: Long): StartEventAttempt {
        return synchronized(lockFor(chatId)) {
            val event = eventsByChatId[chatId]
                ?: return@synchronized StartEventAttempt.NotReady(
                    missing = emptyList(),
                    message = "Ивент не найден. /create"
                )

            if (event.hostUserId != requesterId) {
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
    }
}
