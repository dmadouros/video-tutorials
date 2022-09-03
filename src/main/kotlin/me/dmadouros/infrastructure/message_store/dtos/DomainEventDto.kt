package me.dmadouros.infrastructure.message_store.dtos

abstract class DomainEventDto<T>(
    override val type: String,
    open val actorId: String? = null,
    open val traceId: String = "",
    override val data: T
) : MessageDto<T>(type = type, data = data)
