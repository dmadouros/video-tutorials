package me.dmadouros.domain.events

import me.dmadouros.infrastructure.message_store.dtos.DomainEventDto

data class UserLoggedInEvent(
    override val traceId: String,
    override val data: Data,
    override val actorId: String
) : DomainEventDto<UserLoggedInEvent.Data>(type = "UserLoggedIn", data = data, traceId = traceId, actorId = actorId) {
    data class Data(val userId: String)
}
