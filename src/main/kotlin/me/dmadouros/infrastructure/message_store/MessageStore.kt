package me.dmadouros.infrastructure.message_store

import com.eventstore.dbclient.AppendToStreamOptions
import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.Position
import com.eventstore.dbclient.ReadStreamOptions
import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.SubscribeToAllOptions
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.SubscriptionFilter
import com.eventstore.dbclient.SubscriptionListener
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import me.dmadouros.infrastructure.message_store.dtos.DomainEventDto
import java.util.UUID
import java.util.concurrent.ExecutionException

typealias EventHandler = (ResolvedEvent) -> Unit

class MessageStore(private val client: EventStoreDBClient, private val objectMapper: ObjectMapper) {
    companion object {
        const val ANY_STREAM = "\$any"
    }

    fun <T> writeEvent(streamName: String, event: DomainEventDto<T>, expectedVersion: Long? = null) {
        val eventData = EventData
            .builderAsJson(UUID.fromString(event.id), event.type, event)
            .build()

        val appendToStreamOptions = expectedVersion?.let {
            AppendToStreamOptions.get().expectedRevision(it)
        } ?: AppendToStreamOptions.get()

        client.appendToStream(streamName, appendToStreamOptions, eventData).get()
    }

    fun createSubscription(
        category: String,
        subscriberId: String,
        eventHandlers: Map<String, EventHandler>
    ) {
        val subscriberStreamName = "subscriberPosition-$subscriberId"

        val listener: SubscriptionListener = object : SubscriptionListener() {
            override fun onEvent(subscription: Subscription, event: ResolvedEvent) {
                handleEvent(eventHandlers, event)
                updateReadPosition(subscriberStreamName, event.originalEvent.position)
            }
        }

        val filter = SubscriptionFilter.newBuilder()
            .withStreamNamePrefix("$category-")
            .build()
        val position = loadPosition(subscriberStreamName)
        val options = SubscribeToAllOptions.get().filter(filter).fromPosition(Position(position, position))

        client.subscribeToAll(listener, options)
    }

//    fun readEvents(category: String, id: String): List<RecordedEvent> {
//        val options = ReadStreamOptions.get()
//            .forwards()
//            .fromStart()
//
//        return client.readStream("$category-$id", options).get()
//            .events
//            .map { it.originalEvent }
//    }

//    fun readAllEvents(): List<RecordedEvent> {
//        val options = ReadAllOptions.get()
//            .forwards()
//            .fromStart()
//
//        return client.readAll(options).get()
//            .events
//            .map { it.originalEvent }
//    }

    private data class PositionEvent(override val data: Data) :
        DomainEventDto<PositionEvent.Data>(type = "Read", data = data) {
        data class Data(val position: Long)
    }

    private fun loadPosition(subscriberStreamName: String): Long =
        try {
            readLastMessage(subscriberStreamName)?.data?.position ?: 0
        } catch (e: ExecutionException) {
            0
        }

    private fun readLastMessage(subscriberStreamName: String): PositionEvent? {
        val options = ReadStreamOptions.get()
            .backwards()
            .fromEnd()

        return client.readStream(subscriberStreamName, 1, options).get()
            .events
            .firstOrNull()?.let { objectMapper.readValue<PositionEvent>(it.originalEvent.eventData) }
    }

    private fun updateReadPosition(subscriberStreamName: String, position: Position) {
        writeEvent(
            subscriberStreamName,
            PositionEvent(
                data = PositionEvent.Data(
                    position = position.commitUnsigned
                )
            )
        )
    }

    private fun handleEvent(eventHandlers: Map<String, EventHandler>, event: ResolvedEvent) {
        val eventHandler = eventHandlers[event.originalEvent.eventType] ?: eventHandlers[ANY_STREAM]
        eventHandler?.let { it(event) }
    }
}
