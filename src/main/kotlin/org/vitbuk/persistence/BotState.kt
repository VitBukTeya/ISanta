package org.vitbuk.persistence

import org.vitbuk.model.Event

data class BotState(
    val version: Int = 1,
    val dmReadyUserIds: Set<Long> = emptySet(),
    val eventsByChatId: Map<Long, Event> = emptyMap()
)
