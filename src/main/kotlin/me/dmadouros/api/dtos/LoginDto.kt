package me.dmadouros.api.dtos

import java.util.UUID

data class LoginDto(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val password: String,
)
