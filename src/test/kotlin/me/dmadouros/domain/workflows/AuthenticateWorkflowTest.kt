package me.dmadouros.domain.workflows

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.mockk.checkUnnecessaryStub
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import me.dmadouros.domain.errors.CredentialMismatchError
import me.dmadouros.domain.errors.NotFoundError
import me.dmadouros.domain.events.UserLoginFailedEvent
import me.dmadouros.infrastructure.database.UserRegistrationsRepository
import me.dmadouros.infrastructure.message_store.MessageStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.springframework.security.crypto.bcrypt.BCrypt

internal class AuthenticateWorkflowTest {
    private val messageStore = mockk<MessageStore>()
    private val userRegistrationsRepository = mockk<UserRegistrationsRepository>()
    private val subject = AuthenticateWorkflow(
        messageStore = messageStore,
        userRegistrationsRepository = userRegistrationsRepository
    )
    private val traceId = "9b8c53f4-7a40-4129-919c-1e1a5ab30e87"

    @After
    fun teardown() {
        confirmVerified(userRegistrationsRepository, messageStore)
        checkUnnecessaryStub(userRegistrationsRepository, messageStore)
    }

    @Test
    fun `when user exists and matching password, it writes user logged in event`() {
        mockkStatic(BCrypt::class)
        every { BCrypt.checkpw("password1234", "hashedPassword") }.returns(true)

        val userRegistrationDto = ObjectCreation.createUserRegistrationDto(
            email = "david@example.com",
            passwordHash = "hashedPassword"
        )

        every { userRegistrationsRepository.findByEmail("david@example.com") }.returns(userRegistrationDto)

        val loginDto = ObjectCreation.createLoginDto(
            email = "david@example.com",
            password = "password1234"
        )

        val userLoggedInEvent = ObjectCreation.createUserLoggedInEvent(traceId = traceId, userId = userRegistrationDto.id)

        justRun { messageStore.write("authentication-${userRegistrationDto.id}", userLoggedInEvent) }

        val actual = subject.exec(loginDto = loginDto, traceId = traceId)

        val expected = ObjectCreation.createUserRegistrationDto()

        assertThat(actual).isEqualTo(Ok(expected))

        verify { userRegistrationsRepository.findByEmail("david@example.com") }
        verify { messageStore.write("authentication-${userRegistrationDto.id}", userLoggedInEvent) }
    }

    @Test
    fun `when user not found for email address`() {
        val loginDto = ObjectCreation.createLoginDto(
            email = "nosuchemail@example.com",
            password = "password1234"
        )

        every { userRegistrationsRepository.findByEmail("nosuchemail@example.com") }.returns(null)

        val actual = subject.exec(loginDto = loginDto, traceId = traceId)

        assertThat(actual).isEqualTo(Err(NotFoundError))

        verify { userRegistrationsRepository.findByEmail("nosuchemail@example.com") }
    }

    @Test
    fun `when password does not match`() {
        mockkStatic(BCrypt::class)
        every { BCrypt.checkpw("badPassword", "hashedPassword") }.returns(false)

        val userRegistrationDto = ObjectCreation.createUserRegistrationDto(
            email = "david@example.com",
            passwordHash = "hashedPassword"
        )

        every { userRegistrationsRepository.findByEmail("david@example.com") }.returns(userRegistrationDto)

        val loginDto = ObjectCreation.createLoginDto(
            email = "david@example.com",
            password = "badPassword"
        )

        val userLoginFailedEvent = ObjectCreation.createUserLoginFailedEvent(
            traceId = traceId,
            data = UserLoginFailedEvent.Data(userId = userRegistrationDto.id, reason = "Incorrect password"),
            userId = null,
        )

        justRun { messageStore.write("authentication-${userRegistrationDto.id}", userLoginFailedEvent) }

        val actual = subject.exec(loginDto = loginDto, traceId = traceId)

        val expected = CredentialMismatchError(userRegistrationDto = userRegistrationDto)

        assertThat(actual).isEqualTo(Err(expected))

        verify { userRegistrationsRepository.findByEmail("david@example.com") }
        verify { messageStore.write("authentication-${userRegistrationDto.id}", userLoginFailedEvent) }
    }
}
