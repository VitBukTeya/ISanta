package org.vitbuk.persistence

interface StateStore {
    fun loadOrNull(): BotState?
    fun save(state: BotState)
}
