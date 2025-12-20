package org.vitbuk.model

data class Participant(
    val userId: Long,
    val username: String?,
    val firstName: String?,
    val lastName: String?
) {
    fun display(): String =
        when {
            !username.isNullOrBlank() -> "@$username"
            !firstName.isNullOrBlank() -> firstName
            else -> "userId=$userId"
        }
}
