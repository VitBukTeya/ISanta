package org.vitbuk.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.vitbuk.model.Event
import org.vitbuk.model.EventState
import org.vitbuk.model.Participant
import java.security.SecureRandom
import java.time.Instant

class StartEventServiceConsoleSimulationTest {

    @Test
    fun `prints dm messages for 11 participants`() {
        val seededRandom = SecureRandom.getInstance("SHA1PRNG").apply { setSeed(42L) }
        val algorithm = SattoloDrawAlgorithm(reseedEachDraw = true)
        val service = StartEventService(algorithm)

        val eventName = "ISanta test event"

        val userIds = listOf(
            101000001L,
            101000002L,
            101000003L,
            101000004L,
            101000005L,
            101000006L,
            101000007L,
            101000008L,
            101000009L,
            101000010L,
            101000011L
        )

        val participants = userIds
            .mapIndexed { idx, id -> Participant(userId = id, username = "user${idx + 1}", firstName = null, lastName = null) }

        val hostId = userIds.first()
        val event = Event(
            chatId = -1001234567890L,
            name = eventName,
            hostUserId = hostId,
            createdAt = Instant.now(),
            state = EventState.REGISTRATION,
            participants = LinkedHashMap<Long, Participant>().apply {
                participants.forEach { put(it.userId, it) }
            },
            drawResult = null
        )

        val dmReadyUserIds = userIds.toSet()

        val attempt = service.start(event, dmReadyUserIds)
        Assertions.assertTrue(attempt is StartEventAttempt.Started)

        val snapshot = (attempt as StartEventAttempt.Started).snapshot
        println("===== SIMULATION OUTPUT =====")
        println("event=\"${snapshot.eventName}\", participants=${snapshot.participants.size}")

        snapshot.assignments.forEach { (giverId, receiverId) ->
            val receiver = snapshot.participants.getValue(receiverId)
            val dmText = "🎁 Жеребьёвка для «${snapshot.eventName}»\n" +
                    "Ты даришь: ${receiver.display()}"

            println("юзер с таким айди $giverId получил бы в личку такое сообщение:\n$dmText")
            println("---")
        }

        if (snapshot.warnings.isNotEmpty()) {
            println("WARNINGS:")
            snapshot.warnings.forEach { println("- $it") }
        }
    }

    @Test
    fun `prints cant-start message when someone didn't press start in dm`() {
        val seededRandom = SecureRandom.getInstance("SHA1PRNG").apply { setSeed(42L) }
        val algorithm = SattoloDrawAlgorithm(random = seededRandom)
        val service = StartEventService(algorithm)

        val userIds = (1L..11L).map { 101000000L + it }
        val participants = userIds
            .mapIndexed { idx, id -> Participant(userId = id, username = "user${idx + 1}", firstName = null, lastName = null) }

        val event = Event(
            chatId = -1001234567890L,
            name = "ISanta test event",
            hostUserId = userIds.first(),
            createdAt = Instant.now(),
            state = EventState.REGISTRATION,
            participants = LinkedHashMap<Long, Participant>().apply {
                participants.forEach { put(it.userId, it) }
            },
            drawResult = null
        )

        val dmReadyUserIds = userIds.dropLast(2).toSet()

        val attempt = service.start(event, dmReadyUserIds)
        Assertions.assertTrue(attempt is StartEventAttempt.NotReady)

        println("===== NOT READY OUTPUT =====")
        println((attempt as StartEventAttempt.NotReady).message)
    }
}