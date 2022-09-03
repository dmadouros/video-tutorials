package me.dmadouros.domain.events

import me.dmadouros.infrastructure.message_store.dtos.DomainEventDto

data class UserLoginFailedEvent(
    override val traceId: String,
    override val data: Data,
    override val actorId: String?
) : DomainEventDto<UserLoginFailedEvent.Data>(type = "UserLoginFailed", data = data, traceId = traceId, actorId = actorId) {
    data class Data(val userId: String, val reason: String)
}
