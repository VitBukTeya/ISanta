plugins {
    kotlin("jvm") version "2.2.21"
    application
}

group = "org.vitbuk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    testImplementation(kotlin("test"))
}
kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("org.vitbuk.MainKt")
}

tasks.named<JavaExec>("run") {
    val envFile = rootProject.file(".env")

    if (!envFile.exists()) {
        logger.warn(".env not found: ${envFile.absolutePath}")
        return@named
    }

    envFile.readLines()
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .forEach { line ->
            val cleaned = line.removePrefix("export ").trim()

            val idx = cleaned.indexOf('=')
            if (idx <= 0) return@forEach

            val key = cleaned.substring(0, idx).trim()
            val value = cleaned.substring(idx + 1).trim().trim('"').trim('\'')

            environment(key, value)
        }
}

tasks.test {
    useJUnitPlatform()
    testLogging { showStandardStreams = true }
}