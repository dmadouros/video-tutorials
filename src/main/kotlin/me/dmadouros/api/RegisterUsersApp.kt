package me.dmadouros.api

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import me.dmadouros.api.dtos.RegisterUserDto
import me.dmadouros.domain.workflows.RegisterUserWorkflow
import me.dmadouros.infrastructure.database.UserRegistrationsRepository
import me.dmadouros.infrastructure.message_store.MessageStore

fun Application.configureRegisterUsers(
    messageStore: MessageStore,
    userRegistrationsRepository: UserRegistrationsRepository,
) {
    routing {
        post("/register") {
            val registerUserDto = call.receive<RegisterUserDto>()
            call.callId?.let { traceId ->
                RegisterUserWorkflow(
                    messageStore = messageStore,
                    userRegistrationsRepository = userRegistrationsRepository
                ).call(registerUserDto, traceId)
                    .onSuccess { call.respond(HttpStatusCode.Created) }
                    .onFailure { call.respond(HttpStatusCode.BadRequest, it) }
            }
        }
    }
}
