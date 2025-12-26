package org.vitbuk.model

data class Participant(
    val userId: Long,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    var wish: String = ""
) {
    fun display(): String =
        when {
            !username.isNullOrBlank() -> "@$username"
            !firstName.isNullOrBlank() -> firstName
            else -> "userId=$userId"
        }

    fun addWish(text: String) {
        val t = text.trim()
        if (t.isBlank()) return

        wish = if (wish.isBlank()) t else wish.trimEnd() + "\n" + t
    }
}
