package org.vitbuk.service

import org.vitbuk.draw.SecretSantaDrawAlgorithm
import org.vitbuk.model.DrawResult
import org.vitbuk.model.Event
import org.vitbuk.model.EventState
import org.vitbuk.model.Participant

sealed interface StartEventAttempt {
    data class NotReady(
        val missing: List<Participant>,
        val message: String
    ) : StartEventAttempt

    data class Started(
        val snapshot: StartedSnapshot
    ) : StartEventAttempt
}

data class StartedSnapshot(
    val eventName: String,
    val participants: Map<Long, Participant>,
    val assignments: Map<Long, Long>,
    val warnings: List<String>
)

class StartEventService(
    private val algorithm: SecretSantaDrawAlgorithm
) {
    fun start(event: Event, dmReadyUserIds: Set<Long>): StartEventAttempt {
        val missing = event.participants.values
            .filter { it.userId !in dmReadyUserIds }
            .sortedBy { it.display() }

        if (missing.isNotEmpty()) {
            return StartEventAttempt.NotReady(
                missing = missing,
                message = cantStartMessage(missing)
            )
        }

        val participants = event.participants.values.toList()
        val result = algorithm.draw(participants)

        event.drawResult = DrawResult(result.assignments.toMap(), result.warnings.toList())
        event.state = EventState.STARTED

        return StartEventAttempt.Started(
            StartedSnapshot(
                eventName = event.name,
                participants = LinkedHashMap(event.participants),
                assignments = result.assignments.toMap(),
                warnings = result.warnings
            )
        )
    }

    companion object {
        fun cantStartMessage(missing: List<Participant>): String = buildString {
            append("НЕ можем стартовать пока:\n")
            missing.forEach { append(it.display()).append('\n') }
            append("не напишут /start в личку бота")
        }
    }
}
