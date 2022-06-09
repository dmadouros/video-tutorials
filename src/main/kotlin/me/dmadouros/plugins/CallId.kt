package me.dmadouros.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import java.util.UUID

fun Application.configureCallId() {
    install(CallId) {
        generate {
            UUID.randomUUID().toString()
        }
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
}
