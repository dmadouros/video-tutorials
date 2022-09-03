package me.dmadouros.domain.errors

import me.dmadouros.domain.DomainError

data class ValidationError(val dataPath: String, val message: String) : DomainError
