package org.vitbuk.command

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message

data class CommandContext(
    val bot: Bot,
    val message: Message,
    val args: List<String>
) {
    val chatId: Long get() = message.chat.id
    val chatType: String? get() = message.chat.type
    val from = message.from

    fun reply(text: String) {
        bot.sendMessage(ChatId.fromId(chatId), text)
    }

    fun sendTo(chatId: Long, text: String) {
        bot.sendMessage(ChatId.fromId(chatId), text)
    }
}
