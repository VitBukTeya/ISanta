package org.vitbuk.command.impl

import org.vitbuk.command.Command
import org.vitbuk.command.CommandContext

class RepoCommand : Command {
    override val name: String = "repo"

    override fun execute(ctx: CommandContext) {
        ctx.reply("📦 Репозиторий проекта: https://github.com/VitBukTeya/ISanta")
    }
}
