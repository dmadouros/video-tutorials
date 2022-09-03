package me.dmadouros.domain.components

import com.eventstore.dbclient.RecordedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import me.dmadouros.domain.commands.SendEmailCommand
import me.dmadouros.domain.errors.AlreadySentError
import me.dmadouros.domain.errors.SendError
import me.dmadouros.domain.events.EmailSentEvent
import me.dmadouros.domain.events.SendEmailFailedEvent
import me.dmadouros.infrastructure.message_store.MessageStore
import me.dmadouros.infrastructure.message_store.Projection
import me.dmadouros.infrastructure.message_store.Subscriber

class SendEmailComponent(
    private val messageStore: MessageStore,
    private val systemSenderEmailAddress: String,
    private val objectMapper: ObjectMapper,
): Subscriber() {
    override val category: String = "sendEmail:command"
    override val subscriberId: String = "components:send-email"

    override fun commandHandlers(): Map<String, (RecordedEvent) -> Unit> {
        return mapOf(
            "Send" to { command: RecordedEvent ->
                val sendEmailCommand = objectMapper.readValue<SendEmailCommand>(command.eventData)
                Ok(loadEmail(sendEmailCommand.data.emailId))
                    .andThen { ensureEmailHasNotBeenSent(it) }
                    .andThen { sendEmail(it) }
                    .onSuccess { writeSentEvent(sendEmailCommand) }
                    .onFailure { error ->
                        when (error) {
                            is AlreadySentError -> Unit
                            is SendError -> writeFailedEvent(sendEmailCommand, error)
                        }
                    }
            }
        )
    }

    private fun emailProjection(): Projection<EmailDto> {
        return object : Projection<EmailDto> {
            override val init: EmailDto = EmailDto()
            override val handlers: Map<String, (EmailDto, RecordedEvent) -> EmailDto> =
                mapOf(
                    "EmailSent" to { email: EmailDto, _: RecordedEvent ->
                        email.copy(isSent = true)
                    }
                )
        }
    }

    private fun loadEmail(emailId: String): EmailDto {
        val emailStreamName = "sendEmail-$emailId"

        return messageStore.fetch(emailStreamName, emailProjection())
    }

    private fun ensureEmailHasNotBeenSent(emailDto: EmailDto): Result<EmailDto, AlreadySentError> =
        if (emailDto.isSent) {
            Err(AlreadySentError())
        } else {
            Ok(emailDto)
        }

    private fun sendEmail(emailDto: EmailDto): Result<EmailDto, SendError> {
        return Ok(emailDto)
    }

    private fun writeSentEvent(sendEmailCommand: SendEmailCommand) {
        val emailSentEvent = EmailSentEvent(
            traceId = sendEmailCommand.traceId,
            actorId = sendEmailCommand.actorId,
            originStreamName = sendEmailCommand.originStreamName,
            data = EmailSentEvent.Data(
                emailId = sendEmailCommand.data.emailId,
                to = sendEmailCommand.data.to,
                subject = sendEmailCommand.data.subject,
                text = sendEmailCommand.data.text,
                html = sendEmailCommand.data.html,
            )
        )

        val emailSteamName = "sendEmail-${sendEmailCommand.data.emailId}"

        messageStore.write(emailSteamName, emailSentEvent)
    }

    private fun writeFailedEvent(command: SendEmailCommand, error: SendError) {
        val event = SendEmailFailedEvent(
            originStreamName = command.originStreamName,
            traceId = command.traceId,
            actorId = command.actorId,
            data = SendEmailFailedEvent.Data(
                emailId = command.data.emailId,
                reason = error.message,
                to = command.data.to,
                from = systemSenderEmailAddress,
                subject = command.data.subject,
                text = command.data.text,
                html = command.data.html,
            )
        )

        val emailSteamName = "sendEmail-${command.data.emailId}"

        messageStore.write(emailSteamName, event)
    }

    data class EmailDto(
        val isSent: Boolean = false
    )
}
