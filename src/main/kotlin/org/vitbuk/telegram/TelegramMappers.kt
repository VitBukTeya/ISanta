package org.vitbuk.telegram

import com.github.kotlintelegrambot.entities.User
import org.vitbuk.model.Participant

fun User.toParticipant(): Participant =
    Participant(
        userId = this.id,
        username = this.username,
        firstName = this.firstName,
        lastName = this.lastName
    )
