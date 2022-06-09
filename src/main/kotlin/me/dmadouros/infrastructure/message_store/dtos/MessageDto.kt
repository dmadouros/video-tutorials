package me.dmadouros.infrastructure.message_store.dtos

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

abstract class MessageDto<T>(
    open val id: String = UUID.randomUUID().toString(),
    open val type: String,
    open val timestamp: String = OffsetDateTime.now(ZoneOffset.UTC).toString(),
    open val data: T
)
