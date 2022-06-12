package me.dmadouros.domain.components

import com.eventstore.dbclient.RecordedEvent
import com.eventstore.dbclient.ResolvedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.onSuccess
import me.dmadouros.domain.Aggregator
import me.dmadouros.domain.AlreadyRegisteredError
import me.dmadouros.domain.commands.RegisterCommand
import me.dmadouros.domain.events.RegisteredEvent
import me.dmadouros.infrastructure.message_store.EventHandler
import me.dmadouros.infrastructure.message_store.MessageStore
import me.dmadouros.infrastructure.message_store.Projection

class IdentityComponent(
    private val messageStore: MessageStore,
    private val objectMapper: ObjectMapper,
) : Aggregator {
    override fun start() {
        messageStore.createSubscription("identity:command", "components:identity:command", commandHandlers())
    }

    private fun commandHandlers(): Map<String, EventHandler> {
        return mapOf(
            "Register" to { command: ResolvedEvent ->
                val registerCommand = objectMapper.readValue<RegisterCommand>(command.originalEvent.eventData)
                Ok(loadIdentity(registerCommand.data.userId))
                    .andThen { ensureNotRegistered(it) }
                    .onSuccess { writeRegisteredEvent(registerCommand) }
            }
        )
    }

    private fun identityProjection(): Projection<IdentityDto> {
        return object : Projection<IdentityDto> {
            override val init: IdentityDto = IdentityDto()
            override val handlers: Map<String, (IdentityDto, RecordedEvent) -> IdentityDto> =
                mapOf(
                    "Registered" to { _: IdentityDto, event: RecordedEvent ->
                        objectMapper.readValue(event.eventData)
                    }
                )
        }
    }

    private fun loadIdentity(identityId: String): IdentityDto {
        val identityStreamName = "identity-$identityId"

        return messageStore.fetch(identityStreamName, identityProjection())
    }

    private fun ensureNotRegistered(identityDto: IdentityDto): Result<IdentityDto, AlreadyRegisteredError> =
        if (identityDto.isRegistered) {
            Err(AlreadyRegisteredError())
        } else {
            Ok(identityDto)
        }

    private fun writeRegisteredEvent(registerCommand: RegisterCommand) {
        val registeredEvent = RegisteredEvent(
            traceId = registerCommand.traceId,
            userId = registerCommand.userId,
            data = RegisteredEvent.Data(
                userId = registerCommand.data.userId,
                email = registerCommand.data.email,
                passwordHash = registerCommand.data.passwordHash
            )
        )

        val identitySteamName = "identity-${registerCommand.data.userId}"

        messageStore.write(identitySteamName, registeredEvent)
    }

    private data class IdentityDto(
        val id: String? = null,
        val email: String? = null,
        val isRegistered: Boolean = false
    )
}
