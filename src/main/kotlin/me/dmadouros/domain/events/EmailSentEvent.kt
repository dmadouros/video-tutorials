package me.dmadouros.domain.events

import me.dmadouros.infrastructure.message_store.dtos.DomainEventDto

data class EmailSentEvent(
    override val traceId: String,
    override val data: Data,
    override val actorId: String,
    val originStreamName: String,
) : DomainEventDto<EmailSentEvent.Data>(type = "EmailSent", data = data, traceId = traceId, actorId = actorId) {
    data class Data(
        val emailId: String,
        val to: String,
        val subject: String,
        val text: String,
        val html: String
    )
}
