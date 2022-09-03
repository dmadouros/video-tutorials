package me.dmadouros.infrastructure.message_store

import com.eventstore.dbclient.Position
import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.SubscriptionListener
import me.dmadouros.domain.Component
import me.dmadouros.infrastructure.message_store.dtos.DomainEventDto

class Subscription(
    private val messageStore: MessageStore,
    private val subscriber: Subscriber,
) : SubscriptionListener(), Component { // TODO Remove Component dependency
    // TODO Remove this
    override fun start() {
        messageStore.createSubscription(subscriber.category, subscriber.subscriberId, this)
    }

    override fun onEvent(subscription: Subscription, event: ResolvedEvent) {
        subscriber.handleEvent(event.originalEvent)
        updateReadPosition(subscriber.streamName, event.originalEvent.position)
    }

    private fun updateReadPosition(subscriberStreamName: String, position: Position) {
        messageStore.write(
            subscriberStreamName,
            PositionEvent(
                data = PositionEvent.Data(
                    position = position.commitUnsigned
                )
            )
        )
    }

    private data class PositionEvent(override val data: Data) :
        DomainEventDto<PositionEvent.Data>(type = "Read", data = data) {
        data class Data(val position: Long)
    }
}
