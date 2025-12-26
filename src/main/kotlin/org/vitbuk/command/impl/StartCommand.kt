package org.vitbuk.command.impl

import org.vitbuk.command.Command
import org.vitbuk.command.CommandContext
import org.vitbuk.command.isPrivateChat
import org.vitbuk.service.EventService

class StartCommand(
    private val eventService: EventService
) : Command {
    override val name: String = "start"

    override fun execute(ctx: CommandContext) {
        val chatType = ctx.chatType
        val from = ctx.from

        if (isPrivateChat(chatType) && from != null) {
            eventService.markDmReady(from.id)
            ctx.reply("✅ Отлично! Теперь я могу писать тебе в ЛС.\nВернись в группу и жди жеребьёвку 🎁")
        } else {
            ctx.reply("Чтобы я смог отправить тебе результат жеребьёвки — открой личку с ботом и нажми /start.")
        }
    }
}
