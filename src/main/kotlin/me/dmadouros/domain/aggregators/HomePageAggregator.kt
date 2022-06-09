package me.dmadouros.domain.aggregators

import com.eventstore.dbclient.ResolvedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import me.dmadouros.domain.Aggregator
import me.dmadouros.infrastructure.database.PagesRepository
import me.dmadouros.infrastructure.message_store.EventHandler
import me.dmadouros.infrastructure.message_store.MessageStore

class HomePageAggregator(
    private val messageStore: MessageStore,
    private val objectMapper: ObjectMapper,
    private val repository: PagesRepository,
) : Aggregator {
    private val incrementVideosWatched = { event: ResolvedEvent ->
        val globalPosition = event.originalEvent.position.commitUnsigned

        repository.incrementVideosWatched(globalPosition)
    }

    override fun start() {
        ensureHomePage()
        messageStore.createSubscription("viewing", "aggregators:home-page", eventHandlers())
    }

    private fun eventHandlers(): Map<String, EventHandler> {
        return mapOf(
            "VideoViewed" to incrementVideosWatched
        )
    }

    private fun ensureHomePage() {
        val initialData = mapOf(
            "lastViewProcessed" to 0,
            "videosWatched" to 0,
        )

        val initialDataString = objectMapper.writeValueAsString(initialData)

        repository.ensurePage("home", initialDataString)
    }
}
