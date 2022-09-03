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
import me.dmadouros.domain.errors.ValidationError
import me.dmadouros.domain.commands.RegisterCommand
import me.dmadouros.infrastructure.database.UserRegistrationsRepository
import me.dmadouros.infrastructure.message_store.MessageStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.springframework.security.crypto.bcrypt.BCrypt

internal class RegisterUserWorkflowTest {
    private val messageStore = mockk<MessageStore>()
    private val userRegistrationsRepository = mockk<UserRegistrationsRepository>()
    private val subject = RegisterUserWorkflow(
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
    fun `it registers user when all fields are valid and email is not taken`() {
        mockkStatic(BCrypt::class)
        every { BCrypt.hashpw(ofType(String::class), any()) }.returns("hashedPassword")

        val registerUserDto = ObjectCreation.createRegisterUserDto(
            email = "david@example.com",
        )

        every { userRegistrationsRepository.findByEmail("david@example.com") }.returns(null)
        justRun {
            messageStore.write(
                "identity:command-${registerUserDto.id}", ofType(RegisterCommand::class)
            )
        }

        val result = subject.call(registerUserDto = registerUserDto, traceId = traceId)

        assertThat(result).isEqualTo(Ok("hashedPassword"))

        verify { userRegistrationsRepository.findByEmail("david@example.com") }
        verify {
            messageStore.write(
                "identity:command-${registerUserDto.id}", ofType(RegisterCommand::class)
            )
        }
    }

    @Test
    fun `it fails when id is not valid UUID`() {
        val registerUserDto = ObjectCreation.createRegisterUserDto(
            id = "",
        )

        val result = subject.call(registerUserDto = registerUserDto, traceId = traceId)

        assertThat(result).isEqualTo(Err(listOf(ValidationError(dataPath = ".id", message = "must be a valid UUID"))))
    }

    @Test
    fun `it fails when email is not present`() {
        val registerUserDto = ObjectCreation.createRegisterUserDto(
            email = "",
        )

        val result = subject.call(registerUserDto = registerUserDto, traceId = traceId)

        assertThat(result).isEqualTo(
            Err(
                listOf(
                    ValidationError(
                        dataPath = ".email",
                        message = "must be a valid email address"
                    )
                )
            )
        )
    }

    @Test
    fun `it fails when password is not present`() {
        val registerUserDto = ObjectCreation.createRegisterUserDto(
            password = "",
        )

        val result = subject.call(registerUserDto = registerUserDto, traceId = traceId)

        assertThat(result).isEqualTo(
            Err(
                listOf(
                    ValidationError(
                        dataPath = ".password",
                        message = "must have at least 8 characters"
                    )
                )
            )
        )
    }

    @Test
    fun `it fails when email is already taken`() {
        val registerUserDto = ObjectCreation.createRegisterUserDto(
            email = "david@example.com",
        )

        val userRegistrationDto = ObjectCreation.createUserRegistrationDto(
            email = "david@example.com",
        )

        every { userRegistrationsRepository.findByEmail("david@example.com") }.returns(userRegistrationDto)

        val result = subject.call(registerUserDto = registerUserDto, traceId = traceId)

        assertThat(result).isEqualTo(
            Err(
                listOf(
                    ValidationError(
                        dataPath = ".email",
                        message = "is already taken"
                    )
                )
            )
        )

        verify { userRegistrationsRepository.findByEmail("david@example.com") }
    }
}
