package org.vitbuk.command.impl

import org.vitbuk.command.Command
import org.vitbuk.command.CommandContext
import org.vitbuk.command.isGroupChat
import org.vitbuk.service.EventService
import org.vitbuk.telegram.toParticipant

class JoinCommand(
    private val eventService: EventService
) : Command {
    override val name: String = "join"

    override fun execute(ctx: CommandContext) {
        if (!isGroupChat(ctx.chatType)) {
            ctx.reply("Регистрироваться нужно в группе, где проходит ивент.")
            return
        }

        val from = ctx.from ?: return
        val text = eventService.join(ctx.chatId, from.toParticipant())
        ctx.reply(text)
    }
}
