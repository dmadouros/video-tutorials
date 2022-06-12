package me.dmadouros.infrastructure.database

import me.dmadouros.infrastructure.database.dtos.UserRegistrationDto
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class UserRegistrationsRepository {
    fun findByEmail(email: String): UserRegistrationDto? =
        transaction {
            UserRegistrations.select { UserRegistrations.email eq email }
                .map { row ->
                    UserRegistrationDto(
                        id = row[UserRegistrations.id].toString(),
                        email = row[UserRegistrations.email],
                        passwordHash = row[UserRegistrations.passwordHash],
                    )
                }.firstOrNull()
        }
}
