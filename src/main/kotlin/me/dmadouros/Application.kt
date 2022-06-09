package me.dmadouros

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import me.dmadouros.api.configureVideoTutorials
import me.dmadouros.infrastructure.database.PagesRepository
import me.dmadouros.infrastructure.message_store.MessageStore
import me.dmadouros.plugins.configureCallId
import me.dmadouros.plugins.configureDatabase
import me.dmadouros.plugins.configureSerialization

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        val eventStoreDbClient: EventStoreDBClient = createEventstoreDbClient()
        val objectMapper = createObjectMapper()
        val messageStore = createMessageStore(eventStoreDbClient, objectMapper)
        val pagesRepository = createPagesRepository(objectMapper)

        configureCallId()
        configureDatabase()
        configureVideoTutorials(
            messageStore = messageStore,
            objectMapper = objectMapper,
            pagesRepository = pagesRepository,
        )
        configureSerialization()
    }.start(wait = true)
}

private fun createPagesRepository(objectMapper: ObjectMapper) =
    PagesRepository(
        objectMapper = objectMapper
    )

private fun createMessageStore(
    eventStoreDbClient: EventStoreDBClient,
    objectMapper: ObjectMapper
) =
    MessageStore(
        client = eventStoreDbClient,
        objectMapper = objectMapper
    )

private fun createObjectMapper() =
    ObjectMapper().registerModule(KotlinModule.Builder().build())

private fun createEventstoreDbClient(): EventStoreDBClient =
    System.getenv("VIDEO_TUTORIALS_EVENTSTOREDB_URL")
        .let { EventStoreDBConnectionString.parse(it) }
        .let { EventStoreDBClient.create(it) }
