package me.dmadouros.domain.events

import me.dmadouros.infrastructure.message_store.dtos.DomainEventDto

data class RegisteredEvent(
    override val traceId: String,
    override val data: Data,
    override val actorId: String
) : DomainEventDto<RegisteredEvent.Data>(type = "Registered", data = data, traceId = traceId, actorId = actorId) {
    data class Data(val userId: String, val email: String, val passwordHash: String)
}
