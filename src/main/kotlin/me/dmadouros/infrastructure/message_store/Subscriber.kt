package me.dmadouros.infrastructure.message_store

import com.eventstore.dbclient.RecordedEvent

abstract class Subscriber {
    abstract val category: String
    abstract val subscriberId: String
    val streamName = "subscriberPosition-$subscriberId"

    fun handleEvent(event: RecordedEvent) {
        val eventHandler = commandHandlers()[event.eventType]
        eventHandler?.let { it(event) }
    }

    internal abstract fun commandHandlers(): Map<String, (RecordedEvent) -> Unit>
}
