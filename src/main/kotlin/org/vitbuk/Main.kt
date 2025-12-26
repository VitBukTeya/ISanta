package org.vitbuk

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import org.vitbuk.command.CommandContext
import org.vitbuk.command.CommandFactory
import org.vitbuk.command.impl.*
import org.vitbuk.service.EventService
import org.vitbuk.service.SattoloDrawAlgorithm
import org.vitbuk.service.StartEventService

fun main() {
    val token = System.getenv("BOT_TOKEN")
        ?: error("Env BOT_TOKEN is not set. Put it in .env (BOT_TOKEN=...) or export BOT_TOKEN=...")

    val algorithm = SattoloDrawAlgorithm(reseedEachDraw = true)
    val startEventService = StartEventService(algorithm)
    val eventService = EventService(startEventService)

    val factory = CommandFactory(
        listOf(
            HelpCommand(),
            StartCommand(eventService),
            CreateCommand(eventService),
            JoinCommand(eventService),
            LeaveCommand(eventService),
            ListCommand(eventService),
            WishCommand(eventService),
            CancelCommand(eventService),
            StartEventCommand(eventService),
        )
    )

    val bot = bot {
        this.token = token

        dispatch {
            factory.all().forEach { cmd ->
                command(cmd.name) {
                    cmd.execute(CommandContext(bot, message, args))
                }
            }
        }
    }

    bot.startPolling()
}
