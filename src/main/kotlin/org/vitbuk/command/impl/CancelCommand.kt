package org.vitbuk.command.impl

import org.vitbuk.command.Command
import org.vitbuk.command.CommandContext
import org.vitbuk.command.isGroupChat
import org.vitbuk.service.EventService

class CancelCommand(
    private val eventService: EventService
) : Command {
    override val name: String = "cancel"

    override fun execute(ctx: CommandContext) {
        if (!isGroupChat(ctx.chatType)) {
            ctx.reply("Эта команда работает только в группе/супергруппе.")
            return
        }

        val from = ctx.from ?: return
        ctx.reply(eventService.cancel(ctx.chatId, from.id))
    }
}
