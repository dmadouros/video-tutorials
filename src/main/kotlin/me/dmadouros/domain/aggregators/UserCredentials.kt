package me.dmadouros.domain.aggregators

import com.eventstore.dbclient.RecordedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID
import me.dmadouros.domain.Aggregator
import me.dmadouros.domain.events.RegisteredEvent
import me.dmadouros.infrastructure.database.UserRegistrationsRepository
import me.dmadouros.infrastructure.message_store.EventHandler
import me.dmadouros.infrastructure.message_store.MessageStore

class UserCredentials(
    private val messageStore: MessageStore,
    private val objectMapper: ObjectMapper,
    private val repository: UserRegistrationsRepository,
) : Aggregator {
    private val createUserCredential = { event: RecordedEvent ->
        val registered: RegisteredEvent = objectMapper.readValue(event.eventData)

        repository.createUserCredential(
            UUID.fromString(registered.data.userId),
            registered.data.email,
            registered.data.passwordHash
        )
    }

    override fun start() {
        messageStore.createSubscription("identity", "aggregators:user-credentials", eventHandlers())
    }

    private fun eventHandlers(): Map<String, EventHandler> {
        return mapOf(
            "Registered" to createUserCredential
        )
    }
}
