package org.vitbuk.command.impl

import org.vitbuk.command.Command

    import org.vitbuk.command.CommandContext
import org.vitbuk.command.isGroupChat
import org.vitbuk.service.EventService
import org.vitbuk.telegram.toParticipant

class CreateCommand(
    private val eventService: EventService
) : Command {
    override val name: String = "create"

    override fun execute(ctx: CommandContext) {
        if (!isGroupChat(ctx.chatType)) {
            ctx.reply("Эта команда работает только в группе/супергруппе.")
            return
        }

        val from = ctx.from ?: run {
            ctx.reply("Не смог определить автор:ку сообщения.")
            return
        }

        val eventName = ctx.args.joinToString(" ")
        val text = eventService.create(ctx.chatId, from.toParticipant(), eventName)

        ctx.reply(text)
    }
}
