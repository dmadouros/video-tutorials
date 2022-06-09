package me.dmadouros.domain

import com.eventstore.dbclient.ResolvedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import me.dmadouros.persistence.EventHandler
import me.dmadouros.persistence.MessageStore
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

class HomePageAggregator(private val messageStore: MessageStore, private val objectMapper: ObjectMapper) {
    private val incrementVideosWatched = { event: ResolvedEvent ->
        val globalPosition = event.originalEvent.position.commitUnsigned

        transaction {
            var stmt: PreparedStatementApi? = null
            try {
                val conn = TransactionManager.current().connection
                val query = """
                    UPDATE pages
                       SET page_data = jsonb_set(
                           jsonb_set(
                               page_data,
                               '{videosWatched}',
                               ((page_data ->> 'videosWatched')::int + 1)::text::jsonb
                           ),
                           '{lastViewProcessed}',
                           ?::text::jsonb
                       ) 
                    WHERE page_name = 'home' 
                      AND (page_data->>'lastViewProcessed')::int < ?
                """.trimIndent()
                stmt = conn.prepareStatement(query, false)
                stmt.fillParameters(
                    listOf(
                        Pair(IntegerColumnType(), globalPosition),
                        Pair(IntegerColumnType(), globalPosition)
                    )
                )
                stmt.executeUpdate()
            } finally {
                stmt?.closeIfPossible()
            }
        }

        Unit
    }

    fun start() {
        ensureHomePage()
        messageStore.createSubscription("viewing", "aggregators:home-page", eventHandlers())
    }

    private fun eventHandlers(): Map<String, EventHandler> {
        return mapOf(
            "VideoViewed" to incrementVideosWatched
        )
    }

    private fun ensureHomePage() {
        transaction {
            val initialData = mapOf(
                "lastViewProcessed" to 0,
                "videosWatched" to 0,
            )

            val initialDataString = objectMapper.writeValueAsString(initialData)

            var stmt: PreparedStatementApi? = null
            try {
                val conn = TransactionManager.current().connection
                val query = """
                    INSERT INTO pages(page_name, page_data)
                    VALUES ('home', ?::jsonb)
                    ON CONFLICT DO NOTHING
                """.trimIndent()
                stmt = conn.prepareStatement(query, false)
                stmt.fillParameters(
                    listOf(
                        Pair(TextColumnType(), initialDataString),
                    )
                )
                stmt.executeUpdate()
            } finally {
                stmt?.closeIfPossible()
            }
        }
    }
}
