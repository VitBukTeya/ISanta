package org.vitbuk.command.impl

import org.vitbuk.command.Command
import org.vitbuk.command.CommandContext
import org.vitbuk.command.isGroupChat
import org.vitbuk.service.EventService

class LeaveCommand(
    private val eventService: EventService
) : Command {
    override val name: String = "leave"

    override fun execute(ctx: CommandContext) {
        if (!isGroupChat(ctx.chatType)) return
        val from = ctx.from ?: return
        ctx.reply(eventService.leave(ctx.chatId, from.id))
    }
}
