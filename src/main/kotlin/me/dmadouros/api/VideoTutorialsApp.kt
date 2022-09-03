package me.dmadouros.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import me.dmadouros.domain.aggregators.HomePageAggregator
import me.dmadouros.domain.aggregators.UserCredentials
import me.dmadouros.domain.components.IdentityComponent
import me.dmadouros.domain.components.SendEmailComponent
import me.dmadouros.infrastructure.message_store.Subscription
import me.dmadouros.domain.events.ViewedEvent
import me.dmadouros.infrastructure.database.PagesRepository
import me.dmadouros.infrastructure.database.UserRegistrationsRepository
import me.dmadouros.infrastructure.message_store.MessageStore

fun Application.configureVideoTutorials(
    messageStore: MessageStore,
    objectMapper: ObjectMapper,
    pagesRepository: PagesRepository,
    userRegistrationsRepository: UserRegistrationsRepository
) {
    val aggregators = listOf(
        HomePageAggregator(messageStore, objectMapper, pagesRepository),
        UserCredentials(messageStore, objectMapper, userRegistrationsRepository)
    )
    aggregators.forEach { it.start() }
    val components = listOf(
        Subscription(messageStore, IdentityComponent(messageStore, objectMapper)),
        Subscription(messageStore, SendEmailComponent(messageStore, "", objectMapper))
    )
    components.forEach { it.start() }

    routing {
        get("/") {
            val pageData: Map<String, Any> = pagesRepository.findByPageName("home")

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

                    messageStore.write(streamName, viewedEvent)

                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
