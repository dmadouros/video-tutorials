package me.dmadouros.infrastructure.database

import org.jetbrains.exposed.sql.Table

object UserRegistrations : Table(name = "user_registrations") {
    val id = uuid(name = "id")
    val email = varchar(name = "email", length = 35)
    val passwordHash = varchar(name = "password_hash", length = 255)
}
