package me.dmadouros.domain.events

import me.dmadouros.infrastructure.message_store.dtos.DomainEventDto

data class ViewedEvent(
    override val traceId: String,
    override val data: Data
) : DomainEventDto<ViewedEvent.Data>(type = "VideoViewed", traceId = traceId, data = data) {
    data class Data(val videoId: String)
}
