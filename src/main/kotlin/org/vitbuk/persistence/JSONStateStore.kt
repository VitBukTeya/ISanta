package org.vitbuk.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class JsonStateStore(
    private val file: Path
) : StateStore {

    private val lock = Any()

    private val mapper: ObjectMapper = jacksonObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.INDENT_OUTPUT)

    override fun loadOrNull(): BotState? = synchronized(lock) {
        if (!Files.exists(file)) return@synchronized null
        val json = Files.readString(file)
        mapper.readValue(json, BotState::class.java)
    }

    override fun save(state: BotState) {
        synchronized(lock) {
            file.parent?.let { Files.createDirectories(it) }

            val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
            mapper.writeValue(tmp.toFile(), state)

            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
