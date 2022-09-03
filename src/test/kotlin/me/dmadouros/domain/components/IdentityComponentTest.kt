package me.dmadouros.domain.components

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.checkUnnecessaryStub
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import me.dmadouros.domain.`test-support`.ObjectCreation
import me.dmadouros.domain.events.RegisteredEvent
import me.dmadouros.infrastructure.message_store.MessageStore
import me.dmadouros.infrastructure.message_store.Projection
import org.junit.After
import org.junit.Test

internal class IdentityComponentTest {
    private val messageStore = mockk<MessageStore>()
    private val subject = IdentityComponent(
        messageStore = messageStore,
        objectMapper = jacksonObjectMapper()
    )
    private val command = ObjectCreation.createRegisterCommand(
        traceId = "39246e4c-8548-400a-8aa5-9e40333047d6",
        actorId = "actorId",
        userId = "e9a9fa4d-8421-4075-a8de-2660fbd472f2",
        email = "bilbo@example.com",
        passwordHash = "passwordHash"
    )

    @After
    fun teardown() {
        confirmVerified(messageStore)
        checkUnnecessaryStub(messageStore)
    }

    @Test
    fun `writes registered event when not already registered`() {
        val registeredEvent = RegisteredEvent(
            traceId = "39246e4c-8548-400a-8aa5-9e40333047d6",
            actorId = "actorId",
            data = RegisteredEvent.Data(
                userId = "e9a9fa4d-8421-4075-a8de-2660fbd472f2",
                email = "bilbo@example.com",
                passwordHash = "passwordHash",
            )
        )

        every {
            messageStore.fetch(
                "identity-e9a9fa4d-8421-4075-a8de-2660fbd472f2",
                any<Projection<IdentityComponent.IdentityDto>>()
            )
        }.returns(IdentityComponent.IdentityDto(
            id = "e9a9fa4d-8421-4075-a8de-2660fbd472f2",
            email = "bilbo@example.com",
            isRegistered = false
        ))
        justRun {
            messageStore.write("identity-e9a9fa4d-8421-4075-a8de-2660fbd472f2", registeredEvent)
        }

        subject.handleEvent(ObjectCreation.createRecordedEvent(command))

        verify {
            messageStore.fetch(
                "identity-e9a9fa4d-8421-4075-a8de-2660fbd472f2",
                any<Projection<IdentityComponent.IdentityDto>>()
            )
        }
        verify {
            messageStore.write("identity-e9a9fa4d-8421-4075-a8de-2660fbd472f2", registeredEvent)
        }
    }

    @Test
    fun `does nothing when already registered`() {
        val registeredEvent = RegisteredEvent(
            traceId = "39246e4c-8548-400a-8aa5-9e40333047d6",
            actorId = "actorId",
            data = RegisteredEvent.Data(
                userId = "e9a9fa4d-8421-4075-a8de-2660fbd472f2",
                email = "bilbo@example.com",
                passwordHash = "passwordHash",
            )
        )

        every {
            messageStore.fetch(
                "identity-e9a9fa4d-8421-4075-a8de-2660fbd472f2",
                any<Projection<IdentityComponent.IdentityDto>>()
            )
        }.returns(IdentityComponent.IdentityDto(
            id = "e9a9fa4d-8421-4075-a8de-2660fbd472f2",
            email = "bilbo@example.com",
            isRegistered = true
        ))

        subject.handleEvent(ObjectCreation.createRecordedEvent(command))

        verify {
            messageStore.fetch(
                "identity-e9a9fa4d-8421-4075-a8de-2660fbd472f2",
                any<Projection<IdentityComponent.IdentityDto>>()
            )
        }
    }
}
