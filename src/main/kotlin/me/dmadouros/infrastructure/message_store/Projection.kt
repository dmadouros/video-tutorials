package me.dmadouros.infrastructure.message_store

import com.eventstore.dbclient.RecordedEvent

interface Projection<T> {
    val init: T
    val handlers: Map<String, (T, RecordedEvent) -> T>
}
