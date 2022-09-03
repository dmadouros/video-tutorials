package me.dmadouros.domain.errors

import me.dmadouros.domain.DomainError

data class SendError(val message: String) : DomainError
