package org.vitbuk.model


import java.time.Instant

data class Event(
    val chatId: Long,
    val name: String,
    val hostUserId: Long,
    val createdAt: Instant = Instant.now(),
    var state: EventState = EventState.REGISTRATION,
    val participants: MutableMap<Long, Participant> = LinkedHashMap(),
    var drawResult: DrawResult? = null
) {
    val assignments: Map<Long, Long>
        get() = drawResult?.assignments.orEmpty()
}