package me.dmadouros.infrastructure.message_store

import com.eventstore.dbclient.AppendToStreamOptions
import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.Position
import com.eventstore.dbclient.ReadStreamOptions
import com.eventstore.dbclient.RecordedEvent
import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.StreamNotFoundException
import com.eventstore.dbclient.SubscribeToAllOptions
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.SubscriptionFilter
import com.eventstore.dbclient.SubscriptionListener
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import me.dmadouros.infrastructure.message_store.dtos.DomainEventDto
import me.dmadouros.infrastructure.message_store.dtos.MessageDto
import java.util.UUID
import java.util.concurrent.ExecutionException

typealias EventHandler = (RecordedEvent) -> Unit

class MessageStore(private val client: EventStoreDBClient, private val objectMapper: ObjectMapper) {
    companion object {
        const val ANY_STREAM = "\$any"
    }

    fun <T> write(streamName: String, event: MessageDto<T>, expectedVersion: Long? = null) {
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

    fun createSubscription(
        category: String,
        subscriberId: String,
        listener: SubscriptionListener
    ) {
        val subscriberStreamName = "subscriberPosition-$subscriberId"

        val filter = SubscriptionFilter.newBuilder()
            .withStreamNamePrefix("$category-")
            .build()
        val position = loadPosition(subscriberStreamName)
        val options = SubscribeToAllOptions.get().filter(filter).fromPosition(Position(position, position))

        client.subscribeToAll(listener, options)
    }

    fun <T> fetch(streamName: String, projection: Projection<T>): T =
        project(read(streamName), projection)

    private fun read(streamName: String): List<RecordedEvent> {
        val options = ReadStreamOptions.get()
            .forwards()
            .fromStart()

        return try {
            client.readStream(streamName, options).get()
                .events
                .map { it.originalEvent }
        } catch (e: ExecutionException) {
            when (e.cause) {
                is StreamNotFoundException -> {
                    return emptyList()
                }
                else -> {
                    throw e
                }
            }
        }
    }

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
        write(
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
        eventHandler?.let { it(event.originalEvent) }
    }

    private fun <T> project(events: List<RecordedEvent>, projection: Projection<T>): T {
        return events.fold(projection.init) { memo, event ->
            projection.handlers[event.eventType]?.let { handler -> handler(memo, event) } ?: memo
        }
    }
}
