package org.vitbuk

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import org.vitbuk.command.CommandContext
import org.vitbuk.command.CommandFactory
import org.vitbuk.command.impl.*
import org.vitbuk.persistence.JsonStateStore
import org.vitbuk.service.EventService
import org.vitbuk.service.SattoloDrawAlgorithm
import org.vitbuk.service.StartEventService
import java.nio.file.Paths

fun main() {
    val token = System.getenv("BOT_TOKEN")
        ?: error("Env BOT_TOKEN is not set. Put it in .env (BOT_TOKEN=...) or export BOT_TOKEN=...")

    val statePath = System.getenv("STATE_PATH") ?: "state/isanta-state.json"
    val stateStore = JsonStateStore(Paths.get(statePath))

    val algorithm = SattoloDrawAlgorithm(reseedEachDraw = true)
    val startEventService = StartEventService(algorithm)
    val eventService = EventService(startEventService, stateStore)

    val factory = CommandFactory(
        listOf(
            HelpCommand(),
            StartCommand(eventService),
            CreateCommand(eventService),
            JoinCommand(eventService),
            LeaveCommand(eventService),
            WishCommand(eventService),
            ListCommand(eventService),
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
