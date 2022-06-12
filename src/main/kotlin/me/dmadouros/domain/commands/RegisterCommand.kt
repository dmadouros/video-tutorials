package me.dmadouros.domain.commands

import me.dmadouros.infrastructure.message_store.dtos.CommandDto

data class RegisterCommand(
    override val traceId: String,
    override val userId: String,
    override val data: Data
) : CommandDto<RegisterCommand.Data>(type = "Register", traceId = traceId, userId = userId, data = data) {
    data class Data(
        val userId: String,
        val email: String,
        val passwordHash: String
    )
}
