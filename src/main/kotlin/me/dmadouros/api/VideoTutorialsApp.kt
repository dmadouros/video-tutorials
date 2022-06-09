package me.dmadouros.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import me.dmadouros.domain.HomePageAggregator
import me.dmadouros.persistence.MessageStore
import me.dmadouros.persistence.dtos.DomainEventDto
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

data class ViewedEvent(
    override val traceId: String,
    override val data: Data
) : DomainEventDto<ViewedEvent.Data>(type = "VideoViewed", traceId = traceId, data = data) {
    data class Data(val videoId: String)
}

fun Application.configureVideoTutorials(messageStore: MessageStore, objectMapper: ObjectMapper) {
    val aggregators = listOf(HomePageAggregator(messageStore, objectMapper))
    aggregators.forEach { it.start() }

    routing {
        get("/") {
            var pageDataString: String? = null
            transaction {
                var stmt: PreparedStatementApi? = null
                try {
                    val conn = TransactionManager.current().connection
                    val query = """
                    SELECT *
                      FROM pages
                     WHERE page_name = 'home'
                     LIMIT 1
                    """.trimIndent()
                    stmt = conn.prepareStatement(query, false)
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        pageDataString = rs.getString("page_data")
                    }
                } finally {
                    stmt?.closeIfPossible()
                }
            }
            val pageData: Map<String, Any> = pageDataString?.let {
                objectMapper.readValue(it)
            } ?: emptyMap()

            call.respond(pageData)
        }
        post("/record-viewing/{videoId}") {
            call.parameters["videoId"]?.let { videoId ->
                call.callId?.let { traceId ->
                    val viewedEvent = ViewedEvent(
                        traceId = traceId,
                        data = ViewedEvent.Data(
                            videoId = videoId
                        )
                    )

                    val streamName = "viewing-$videoId"

                    messageStore.writeEvent(streamName, viewedEvent)

                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
