package org.vitbuk.command.impl

import org.vitbuk.command.Command
import org.vitbuk.command.CommandContext
import org.vitbuk.command.isGroupChat
import org.vitbuk.command.isPrivateChat
import org.vitbuk.service.EventService

class WishCommand(
    private val eventService: EventService
) : Command {
    override val name: String = "wish"

    override fun execute(ctx: CommandContext) {
        val from = ctx.from ?: return
        val wishText = ctx.args.joinToString(" ").trim()

        if (wishText.isBlank()) {
            ctx.reply("Напиши так: /wish хочу тетрадку или зонтик")
            return
        }

        val reply = when {
            isGroupChat(ctx.chatType) -> eventService.wishInEvent(ctx.chatId, from.id, wishText)
            isPrivateChat(ctx.chatType) -> eventService.wishInPrivate(from.id, wishText)
            else -> "Не понимаю тип чата, попробуй написать /wish в группе."
        }

        ctx.reply(reply)
    }
}
