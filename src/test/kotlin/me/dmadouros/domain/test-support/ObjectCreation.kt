package me.dmadouros.domain.`test-support`

import com.eventstore.dbclient.Position
import com.eventstore.dbclient.RecordedEvent
import com.eventstore.dbclient.StreamRevision
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID
import me.dmadouros.domain.commands.RegisterCommand
import me.dmadouros.domain.commands.SendEmailCommand
import me.dmadouros.infrastructure.message_store.dtos.MessageDto

object ObjectCreation {

    fun <T> createRecordedEvent(domainEvent: MessageDto<T>): RecordedEvent {
        val eventStreamId = UUID.randomUUID().toString()
        val streamRevision = StreamRevision.START
        val eventId = UUID.randomUUID()
        val position = Position.START
        val systemMetadata = mapOf(
            "type" to domainEvent.type,
            "contentType" to "application/json",
            "created" to System.currentTimeMillis().toString()
        )
        val eventData = jacksonObjectMapper().writeValueAsBytes(domainEvent)
        val userMetadata = byteArrayOf()

        return RecordedEvent(
            eventStreamId,
            streamRevision,
            eventId,
            position,
            systemMetadata,
            eventData,
            userMetadata
        )
    }

    fun createSendEmailCommand(
        traceId: String = "39246e4c-8548-400a-8aa5-9e40333047d6",
        userId: String = "userId",
        originStreamName: String = "originStreamName",
        emailId: String = "e9a9fa4d-8421-4075-a8de-2660fbd472f2",
        to: String = "to",
        subject: String = "subject",
        text: String = "text",
        html: String = "html",
    ): SendEmailCommand {
        return SendEmailCommand(
            traceId = traceId,
            actorId = userId,
            originStreamName = originStreamName,
            data = SendEmailCommand.Data(
                emailId = emailId,
                to = to,
                subject = subject,
                text = text,
                html = html,
            )
        )
    }

    fun createRegisterCommand(
        traceId: String = "39246e4c-8548-400a-8aa5-9e40333047d6",
        actorId: String = "actorId",
        userId: String = "e9a9fa4d-8421-4075-a8de-2660fbd472f2",
        email: String = "bilbo@example.com",
        passwordHash: String = "passwordHash",
    ): RegisterCommand {
        return RegisterCommand(
            traceId = traceId,
            actorId = actorId,
            data = RegisterCommand.Data(
                userId = userId,
                email = email,
                passwordHash = passwordHash,
            )
        )
    }
}
