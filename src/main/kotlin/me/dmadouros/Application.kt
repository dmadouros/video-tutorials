package me.dmadouros

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import me.dmadouros.plugins.configureRouting
import me.dmadouros.plugins.configureSerialization

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureSerialization()
    }.start(wait = true)
}
