package org.vitbuk.command

class CommandFactory(commands: List<Command>) {
    private val byName: Map<String, Command> = commands.associateBy { it.name }

    fun all(): Collection<Command> = byName.values
    fun get(name: String): Command? = byName[name]
}
