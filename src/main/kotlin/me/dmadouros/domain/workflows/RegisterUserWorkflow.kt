package me.dmadouros.domain.workflows

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onSuccess
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.ValidationResult
import me.dmadouros.api.dtos.RegisterUserDto
import me.dmadouros.api.dtos.validateRegisterUserDto
import me.dmadouros.domain.errors.ValidationError
import me.dmadouros.domain.commands.RegisterCommand
import me.dmadouros.infrastructure.database.UserRegistrationsRepository
import me.dmadouros.infrastructure.database.dtos.UserRegistrationDto
import me.dmadouros.infrastructure.message_store.MessageStore
import org.springframework.security.crypto.bcrypt.BCrypt

class RegisterUserWorkflow(
    private val messageStore: MessageStore,
    private val userRegistrationsRepository: UserRegistrationsRepository,
) {
    fun call(registerUserDto: RegisterUserDto, traceId: String): Result<String, List<ValidationError>> {
        return validate(registerUserDto)
            .map { loadExistingIdentity(userRegistrationsRepository, it) }
            .andThen { ensureThereWasNoExistingIdentity(registerUserDto, it) }
            .map { hashPassword(registerUserDto) }
            .onSuccess { writeRegisterCommand(messageStore, registerUserDto, traceId, it) }
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
            actorId = registerUserDto.id,
            traceId = traceId,
            data = RegisterCommand.Data(
                userId = registerUserDto.id,
                email = registerUserDto.email,
                passwordHash = passwordHash
            )
        )

        messageStore.write(streamName, registerCommand)
    }
}
