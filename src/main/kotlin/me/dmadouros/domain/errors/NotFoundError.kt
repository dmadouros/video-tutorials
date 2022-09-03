package me.dmadouros.domain.errors

import me.dmadouros.domain.DomainError

object NotFoundError : DomainError {
    val message = "no record found with that email"
}
