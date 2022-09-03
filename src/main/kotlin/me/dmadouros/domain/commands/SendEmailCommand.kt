package me.dmadouros.domain.commands

import me.dmadouros.infrastructure.message_store.dtos.CommandDto

class SendEmailCommand(
    override val traceId: String,
    override val data: Data,
    override val actorId: String,
    val originStreamName: String,
) : CommandDto<SendEmailCommand.Data>(type = "Send", data = data, traceId = traceId, actorId = actorId) {
    data class Data(
        val emailId: String,
        val to: String,
        val subject: String,
        val text: String,
        val html: String
    )
}
