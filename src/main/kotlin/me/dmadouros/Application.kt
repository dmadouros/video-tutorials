package me.dmadouros

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callid.CallId
import me.dmadouros.api.configureVideoTutorials
import me.dmadouros.plugins.configureSerialization
import java.util.UUID

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CallId) {
            generate {
                UUID.randomUUID().toString()
            }
        }
        configureVideoTutorials()
        configureSerialization()
    }.start(wait = true)
}
