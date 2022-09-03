package me.dmadouros.api

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receive
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import me.dmadouros.api.dtos.LoginDto
import me.dmadouros.domain.workflows.AuthenticateWorkflow
import me.dmadouros.infrastructure.database.UserRegistrationsRepository
import me.dmadouros.infrastructure.message_store.MessageStore

fun Application.configureAuthenticate(
    messageStore: MessageStore,
    userRegistrationsRepository: UserRegistrationsRepository,
) {
    routing {
        post("/login") {
            val loginDto = call.receive<LoginDto>()
            call.callId?.let { traceId ->
                AuthenticateWorkflow(
                    messageStore = messageStore,
                    userRegistrationsRepository = userRegistrationsRepository
                )
                    .exec(loginDto = loginDto, traceId = traceId)
                    .onSuccess { call.response.status(HttpStatusCode.OK) }
                    .onFailure { _ ->
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
            }
        }
    }
}
