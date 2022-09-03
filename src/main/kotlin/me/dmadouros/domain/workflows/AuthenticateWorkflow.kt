package me.dmadouros.domain.workflows

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import me.dmadouros.api.dtos.LoginDto
import me.dmadouros.domain.errors.CredentialMismatchError
import me.dmadouros.domain.DomainError
import me.dmadouros.domain.errors.NotFoundError
import me.dmadouros.domain.events.UserLoggedInEvent
import me.dmadouros.domain.events.UserLoginFailedEvent
import me.dmadouros.infrastructure.database.UserRegistrationsRepository
import me.dmadouros.infrastructure.database.dtos.UserRegistrationDto
import me.dmadouros.infrastructure.message_store.MessageStore
import org.springframework.security.crypto.bcrypt.BCrypt

class AuthenticateWorkflow(
    private val messageStore: MessageStore,
    private val userRegistrationsRepository: UserRegistrationsRepository
) {

    fun exec(loginDto: LoginDto, traceId: String): Result<UserRegistrationDto, DomainError> {
        return Ok(loadUserCredential(loginDto))
            .andThen { userRegistrationDto ->  ensureUserCredentialFound(userRegistrationDto) }
            .andThen { userRegistrationDto -> validatePassword(loginDto, userRegistrationDto) }
            .onSuccess { userRegistrationDto -> writeLoggedInEvent(traceId, userRegistrationDto) }
            .onFailure { error ->
                when (error) {
                    is NotFoundError -> {
                        handleCredentialNotFound()
                    }
                    is CredentialMismatchError -> {
                        handleCredentialsMismatch(traceId, error.userRegistrationDto)
                    }
                }
            }
    }

    private fun loadUserCredential(
        loginDto: LoginDto
    ): UserRegistrationDto? {
        return userRegistrationsRepository.findByEmail(loginDto.email)
    }

    private fun ensureUserCredentialFound(userRegistrationDto: UserRegistrationDto?): Result<UserRegistrationDto, NotFoundError> {
        return if (userRegistrationDto != null) {
            Ok(userRegistrationDto)
        } else {
            Err(NotFoundError)
        }
    }

    private fun validatePassword(
        loginDto: LoginDto,
        userRegistrationDto: UserRegistrationDto,
    ): Result<UserRegistrationDto, CredentialMismatchError> {
        return if (BCrypt.checkpw(loginDto.password, userRegistrationDto.passwordHash)) {
            Ok(userRegistrationDto)
        } else {
            Err(CredentialMismatchError(userRegistrationDto))
        }
    }

    private fun writeLoggedInEvent(
        traceId: String,
        userRegistrationDto: UserRegistrationDto
    ) {
        val streamName = "authentication-${userRegistrationDto.id}"

        val userLoggedInEvent = UserLoggedInEvent(
            actorId = userRegistrationDto.id,
            traceId = traceId,
            data = UserLoggedInEvent.Data(
                userId = userRegistrationDto.id
            )
        )

        messageStore.write(streamName, userLoggedInEvent)
    }

    private fun handleCredentialNotFound() {
//    throw AuthenticationException()
    }

    private fun handleCredentialsMismatch(traceId: String, userRegistrationDto: UserRegistrationDto) {
        val streamName = "authentication-${userRegistrationDto.id}"

        val userLoginFailedEvent = UserLoginFailedEvent(
            actorId = null,
            traceId = traceId,
            data = UserLoginFailedEvent.Data(
                userId = userRegistrationDto.id,
                reason = "Incorrect password"
            )
        )

        messageStore.write(streamName, userLoginFailedEvent)
    }
}
