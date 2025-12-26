package org.vitbuk.command

interface Command {
    val name: String
    fun execute(ctx: CommandContext)
}