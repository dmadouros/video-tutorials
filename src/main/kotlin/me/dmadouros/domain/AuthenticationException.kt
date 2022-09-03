package me.dmadouros.domain

class AuthenticationException : RuntimeException {
    constructor() : super("authentication error")
}
