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

        when {
            isGroupChat(ctx.chatType) -> {
                val msg = eventService.addWishInChat(ctx.chatId, from.id, wishText)
                ctx.reply(msg)
            }

            isPrivateChat(ctx.chatType) -> {
                // Раз пользователь написал в личку — отмечаем, что можем писать ему в ЛС.
                eventService.markDmReady(from.id)

                eventService.addWish(from.id, wishText)
                val now = eventService.getWish(from.id).orEmpty()

                ctx.reply(
                    "✅ Пожелание добавлено!\n" +
                            "Вернись в группу и жди жеребьёвку 🎁\n\n" +
                            "Твои пожелания сейчас:\n$now"
                )
            }

            else -> {
                // На всякий случай (если Telegram добавит новые типы чатов)
                eventService.addWish(from.id, wishText)
                ctx.reply("✅ Пожелание добавлено!")
            }
        }
    }
}
