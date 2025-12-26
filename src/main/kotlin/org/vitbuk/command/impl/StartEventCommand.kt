package org.vitbuk.command.impl

import com.github.kotlintelegrambot.entities.ChatId
import org.vitbuk.command.Command
import org.vitbuk.command.CommandContext
import org.vitbuk.command.isGroupChat
import org.vitbuk.model.Participant
import org.vitbuk.service.EventService
import org.vitbuk.service.StartEventAttempt

class StartEventCommand(
    private val eventService: EventService
) : Command {
    override val name: String = "start_event"

    override fun execute(ctx: CommandContext) {
        if (!isGroupChat(ctx.chatType)) {
            ctx.reply("Эта команда работает только в группе/супергруппе.")
            return
        }

        val from = ctx.from ?: return

        val attempt = eventService.startEvent(ctx.chatId, from.id)

        when (attempt) {
            is StartEventAttempt.NotReady -> {
                ctx.reply(attempt.message)
            }

            is StartEventAttempt.Started -> {
                val snapshot = attempt.snapshot
                val failed = mutableListOf<Participant>()

                for ((giverId, receiverId) in snapshot.assignments) {
                    val giver = snapshot.participants[giverId] ?: continue
                    val receiver = snapshot.participants[receiverId] ?: continue

                    val dmText = "🎁 Жеребьёвка для «${snapshot.eventName}»\nТы даришь: ${receiver.display()}"
                    val sendRes = ctx.bot.sendMessage(ChatId.fromId(giverId), dmText)

                    sendRes.fold(
                        { /* ok */ },
                        { failed += giver }
                    )
                }

                val groupMsg = buildString {
                    append("✅ Жеребьёвка проведена! Результаты отправлены в ЛС.\n")

                    if (snapshot.warnings.isNotEmpty()) {
                        append("\n⚠️ Замечания:\n")
                        snapshot.warnings.forEach { append("• ").append(it).append('\n') }
                    }

                    if (failed.isNotEmpty()) {
                        append("\n⚠️ Не смог написать в ЛС этим людям (возможно, они заблокировали бота):\n")
                        failed.forEach { append("• ").append(it.display()).append('\n') }
                    }
                }

                ctx.reply(groupMsg)
            }
        }
    }
}
