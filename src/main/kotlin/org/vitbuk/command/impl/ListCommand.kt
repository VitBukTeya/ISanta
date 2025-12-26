package org.vitbuk.command.impl

import org.vitbuk.command.Command
import org.vitbuk.command.CommandContext
import org.vitbuk.command.isGroupChat
import org.vitbuk.service.EventService

class ListCommand(
    private val eventService: EventService
) : Command {
    override val name: String = "list"

    override fun execute(ctx: CommandContext) {
        if (!isGroupChat(ctx.chatType)) return
        ctx.reply(eventService.list(ctx.chatId))
    }
}
