package me.dmadouros.infrastructure.database.dtos

data class UserRegistrationDto(val id: String, val email: String, val passwordHash: String)
