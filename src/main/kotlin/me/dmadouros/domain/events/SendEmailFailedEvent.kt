package me.dmadouros.domain.events

import me.dmadouros.infrastructure.message_store.dtos.DomainEventDto

class SendEmailFailedEvent(
    override val traceId: String,
    override val data: Data,
    override val actorId: String,
    val originStreamName: String,
) : DomainEventDto<SendEmailFailedEvent.Data>(type = "SendEmailFailed", data = data, traceId = traceId, actorId = actorId) {
    data class Data(
        val emailId: String,
        val reason: String,
        val to: String,
        val from: String,
        val subject: String,
        val text: String,
        val html: String
    )
}
