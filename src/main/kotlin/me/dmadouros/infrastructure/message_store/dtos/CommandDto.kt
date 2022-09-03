package me.dmadouros.infrastructure.message_store.dtos

abstract class CommandDto<T>(
    override val type: String,
    open val actorId: String = "",
    open val traceId: String = "",
    override val data: T
) : MessageDto<T>(type = type, data = data)
