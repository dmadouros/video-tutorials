package me.dmadouros.persistence.dtos

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

abstract class DomainEventDto<T>(
    open val id: String = UUID.randomUUID().toString(),
//    open val userId: String,
    open val traceId: String,
    open val timestamp: String = OffsetDateTime.now(ZoneOffset.UTC).toString(),
    open val type: String,
    open val data: T
)
