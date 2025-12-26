package org.vitbuk.service

import org.vitbuk.model.DrawResult
import org.vitbuk.model.Participant

fun interface SecretSantaDrawAlgorithm {
    fun draw(participants: List<Participant>): DrawResult
}