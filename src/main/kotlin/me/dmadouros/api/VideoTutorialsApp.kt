package me.dmadouros.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import me.dmadouros.persistence.MessageStore
import me.dmadouros.persistence.dtos.DomainEventDto

data class ViewedEvent(
    override val traceId: String,
    override val data: Data
) : DomainEventDto<ViewedEvent.Data>(type = "VideoViewed", traceId = traceId, data = data) {
    data class Data(val videoId: String)
}

fun Application.configureVideoTutorials(messageStore: MessageStore) {
    var videos = emptyMap<String, Int>()

    routing {
        get("/") {
            call.respond(videos)
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
