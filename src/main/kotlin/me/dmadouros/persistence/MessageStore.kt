package me.dmadouros.persistence

import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.EventStoreDBClient
import me.dmadouros.persistence.dtos.DomainEventDto
import java.util.UUID

class MessageStore(private val client: EventStoreDBClient) {
    fun <T> writeEvent(streamName: String, event: DomainEventDto<T>) {
        val eventData = EventData
            .builderAsJson(UUID.fromString(event.id), event.type, event)
            .build()

        client.appendToStream(streamName, eventData).get()
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
}
