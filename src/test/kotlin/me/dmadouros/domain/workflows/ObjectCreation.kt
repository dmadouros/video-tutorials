package me.dmadouros.domain.workflows

import me.dmadouros.api.dtos.LoginDto
import me.dmadouros.api.dtos.RegisterUserDto
import me.dmadouros.domain.events.UserLoggedInEvent
import me.dmadouros.domain.events.UserLoginFailedEvent
import me.dmadouros.infrastructure.database.dtos.UserRegistrationDto

object ObjectCreation {
    fun createRegisterUserDto(
        id: String = "9ab03c5c-e810-47f2-bf86-877f54eb226a",
        email: String = "david@example.com",
        password: String = "password1234"
    ): RegisterUserDto {
        return RegisterUserDto(
            id = id,
            email = email,
            password = password
        )
    }

    fun createUserRegistrationDto(
        id: String = "9ab03c5c-e810-47f2-bf86-877f54eb226a",
        email: String = "david@example.com",
        passwordHash: String = "hashedPassword"
    ): UserRegistrationDto {
        return UserRegistrationDto(
            id = id,
            email = email,
            passwordHash = passwordHash
        )
    }

    fun createLoginDto(
        id: String = "9ab03c5c-e810-47f2-bf86-877f54eb226a",
        email: String = "david@example.com",
        password: String = "password1234"
    ): LoginDto {
        return LoginDto(
            id = id,
            email = email,
            password = password,
        )
    }

    fun createUserLoggedInEvent(
        traceId: String = "",
        data: UserLoggedInEvent.Data = UserLoggedInEvent.Data(userId = "9ab03c5c-e810-47f2-bf86-877f54eb226a"),
        userId: String = "9ab03c5c-e810-47f2-bf86-877f54eb226a"
    ): UserLoggedInEvent {
        return UserLoggedInEvent(
            traceId = traceId,
            data = data,
            actorId = userId
        )
    }

    fun createUserLoginFailedEvent(
        traceId: String = "",
        data: UserLoginFailedEvent.Data = UserLoginFailedEvent.Data(
            userId = "",
            reason = ""
        ),
        userId: String? = ""
    ): UserLoginFailedEvent {
        return UserLoginFailedEvent(
            traceId = traceId,
            data = data,
            actorId = userId,
        )
    }
}
