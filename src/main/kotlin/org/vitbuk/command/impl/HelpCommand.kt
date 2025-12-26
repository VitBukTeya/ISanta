package org.vitbuk.command.impl

import org.vitbuk.command.Command
import org.vitbuk.command.CommandContext

class HelpCommand : Command {
    override val name: String = "help"

    override fun execute(ctx: CommandContext) {
        ctx.reply(
            """
            Команды (в группе):
            /create [название] — создать ивент (хост)
            /cancel — отменить ивент (только хост)
            /join — зарегистрироваться
            /leave — выйти
            /list — список участни:ц и готовность ЛС
            /wish [текст] — добавить пожелание (будет видно тому, кто вытянет тебя)
            /start_event — провести жеребьёвку (только хост)

            Важно: чтобы получить результат в ЛС — открой личку с ботом и нажми /start.
            """.trimIndent()
        )
    }
}
