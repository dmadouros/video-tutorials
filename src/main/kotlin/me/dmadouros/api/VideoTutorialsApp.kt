package me.dmadouros.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureVideoTutorials() {
    var videos = emptyMap<String, Int>()

    routing {
        get("/") {
            call.respond(videos)
        }
        post("/record-viewing/{videoId}") {
            call.parameters["videoId"]?.let { videoId ->
                call.application.log.info("VideoId: $videoId")
                val count = videos[videoId]?.let { it + 1 } ?: 1
                videos = videos + mapOf(videoId to count)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
