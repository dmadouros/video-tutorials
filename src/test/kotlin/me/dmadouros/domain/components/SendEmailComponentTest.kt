package me.dmadouros.domain.components

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.checkUnnecessaryStub
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import me.dmadouros.domain.`test-support`.ObjectCreation
import me.dmadouros.domain.events.EmailSentEvent
import me.dmadouros.infrastructure.message_store.MessageStore
import me.dmadouros.infrastructure.message_store.Projection
import org.junit.After
import org.junit.Test

internal class SendEmailComponentTest {
    private val messageStore = mockk<MessageStore>()
    private val subject = SendEmailComponent(
        messageStore = messageStore,
        systemSenderEmailAddress = "",
        objectMapper = jacksonObjectMapper()
    )
    private val command = ObjectCreation.createSendEmailCommand(
        traceId = "39246e4c-8548-400a-8aa5-9e40333047d6",
        userId = "userId",
        originStreamName = "originStreamName",
        emailId = "e9a9fa4d-8421-4075-a8de-2660fbd472f2",
        to = "to",
        subject = "subject",
        text = "text",
        html = "html",
    )


    @After
    fun teardown() {
        confirmVerified(messageStore)
        checkUnnecessaryStub(messageStore)
    }

    @Test
    fun `writes email sent event when email not already sent`() {
        val emailSentEvent = EmailSentEvent(
            traceId = "39246e4c-8548-400a-8aa5-9e40333047d6",
            actorId = "userId",
            originStreamName = "originStreamName",
            data = EmailSentEvent.Data(
                emailId = "e9a9fa4d-8421-4075-a8de-2660fbd472f2",
                to = "to",
                subject = "subject",
                text = "text",
                html = "html",
            )
        )

        every {
            messageStore.fetch(
                "sendEmail-e9a9fa4d-8421-4075-a8de-2660fbd472f2",
                any<Projection<SendEmailComponent.EmailDto>>()
            )
        }.returns(SendEmailComponent.EmailDto(isSent = false))
        justRun {
            messageStore.write("sendEmail-e9a9fa4d-8421-4075-a8de-2660fbd472f2", emailSentEvent)
        }

        subject.handleEvent(ObjectCreation.createRecordedEvent(command))

        verify {
            messageStore.fetch(
                "sendEmail-e9a9fa4d-8421-4075-a8de-2660fbd472f2",
                any<Projection<SendEmailComponent.EmailDto>>()
            )
        }
        verify {
            messageStore.write("sendEmail-e9a9fa4d-8421-4075-a8de-2660fbd472f2", emailSentEvent)
        }
    }

    @Test
    fun `does nothing when email already sent`() {
        every {
            messageStore.fetch(
                "sendEmail-e9a9fa4d-8421-4075-a8de-2660fbd472f2",
                any<Projection<SendEmailComponent.EmailDto>>()
            )
        }.returns(SendEmailComponent.EmailDto(isSent = true))

        subject.handleEvent(ObjectCreation.createRecordedEvent(command))

        verify {
            messageStore.fetch(
                "sendEmail-e9a9fa4d-8421-4075-a8de-2660fbd472f2",
                any<Projection<SendEmailComponent.EmailDto>>()
            )
        }
    }
}
