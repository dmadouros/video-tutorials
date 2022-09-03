package me.dmadouros.domain.errors

import me.dmadouros.domain.DomainError
import me.dmadouros.infrastructure.database.dtos.UserRegistrationDto

data class CredentialMismatchError(val userRegistrationDto: UserRegistrationDto) : DomainError
