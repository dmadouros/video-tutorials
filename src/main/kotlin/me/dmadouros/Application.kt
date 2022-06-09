package me.dmadouros

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBClientSettings
import com.eventstore.dbclient.EventStoreDBConnectionString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callid.CallId
import me.dmadouros.api.configureVideoTutorials
import me.dmadouros.persistence.MessageStore
import me.dmadouros.plugins.configureSerialization
import java.util.UUID

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        val connectionString = "esdb://admin:changeit@video-tutorials.eventstore.db:2113?tls=false"
        val settings: EventStoreDBClientSettings = EventStoreDBConnectionString.parse(connectionString)
        val client: EventStoreDBClient = EventStoreDBClient.create(settings)
        val messageStore = MessageStore(client)
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

        install(CallId) {
            generate {
                UUID.randomUUID().toString()
            }
            verify { callId: String ->
                callId.isNotEmpty()
            }
        }
        configureVideoTutorials(messageStore = messageStore)
        configureSerialization()
    }.start(wait = true)
}
