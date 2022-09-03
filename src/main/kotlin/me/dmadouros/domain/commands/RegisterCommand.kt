package me.dmadouros.domain.commands

import me.dmadouros.infrastructure.message_store.dtos.CommandDto

data class RegisterCommand(
    override val traceId: String,
    override val actorId: String,
    override val data: Data
) : CommandDto<RegisterCommand.Data>(type = "Register", traceId = traceId, actorId = actorId, data = data) {
    data class Data(
        val userId: String,
        val email: String,
        val passwordHash: String
    )
}
