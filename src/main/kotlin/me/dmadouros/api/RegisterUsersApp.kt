package me.dmadouros.api

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.ValidationResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import me.dmadouros.api.dtos.RegisterUserDto
import me.dmadouros.api.dtos.validateRegisterUserDto
import me.dmadouros.domain.ValidationError
import me.dmadouros.domain.commands.RegisterCommand
import me.dmadouros.infrastructure.database.UserRegistrationsRepository
import me.dmadouros.infrastructure.database.dtos.UserRegistrationDto
import me.dmadouros.infrastructure.message_store.MessageStore
import org.springframework.security.crypto.bcrypt.BCrypt

fun Application.configureRegisterUsers(
    messageStore: MessageStore,
    userRegistrationsRepository: UserRegistrationsRepository,
) {
    routing {
        post("/register") {
            val registerUserDto = call.receive<RegisterUserDto>()
            call.callId?.let { traceId ->
                validate(registerUserDto)
                    .map { loadExistingIdentity(userRegistrationsRepository, it) }
                    .andThen { ensureThereWasNoExistingIdentity(registerUserDto, it) }
                    .map { hashPassword(registerUserDto) }
                    .onSuccess { writeRegisterCommand(messageStore, registerUserDto, traceId, it) }
                    .onSuccess { call.respond(HttpStatusCode.Created) }
                    .onFailure { call.respond(HttpStatusCode.BadRequest, it) }
            }
        }
    }
}

private fun validate(registerUserDto: RegisterUserDto): Result<RegisterUserDto, List<ValidationError>> =
    when (val validationResult: ValidationResult<RegisterUserDto> = validateRegisterUserDto(registerUserDto)) {
        is Valid<RegisterUserDto> -> {
            Ok(validationResult.value)
        }
        is Invalid<RegisterUserDto> -> {
            Err(
                validationResult.errors.map { validationError ->
                    ValidationError(
                        dataPath = validationError.dataPath,
                        message = validationError.message

                    )
                }
            )
        }
    }

private fun loadExistingIdentity(
    userRegistrationsRepository: UserRegistrationsRepository,
    registerUserDto: RegisterUserDto
): UserRegistrationDto? =
    userRegistrationsRepository.findByEmail(registerUserDto.email)

private fun ensureThereWasNoExistingIdentity(
    registerUserDto: RegisterUserDto,
    userRegistrationDto: UserRegistrationDto?
): Result<RegisterUserDto, List<ValidationError>> =
    if (userRegistrationDto == null) {
        Ok(registerUserDto)
    } else {
        Err(
            listOf(
                ValidationError(
                    dataPath = ".email",
                    message = "is already taken"
                )
            )
        )
    }

private fun hashPassword(registerUserDto: RegisterUserDto): String =
    BCrypt.hashpw(registerUserDto.password, BCrypt.gensalt(10))

private fun writeRegisterCommand(
    messageStore: MessageStore,
    registerUserDto: RegisterUserDto,
    traceId: String,
    passwordHash: String
) {
    val streamName = "identity:command-${registerUserDto.id}"

    val registerCommand = RegisterCommand(
        userId = registerUserDto.id,
        traceId = traceId,
        data = RegisterCommand.Data(
            userId = registerUserDto.id,
            email = registerUserDto.email,
            passwordHash = passwordHash
        )
    )

    messageStore.write(streamName, registerCommand)
}
