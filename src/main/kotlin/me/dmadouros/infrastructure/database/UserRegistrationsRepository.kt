package me.dmadouros.infrastructure.database

import me.dmadouros.infrastructure.database.dtos.UserRegistrationDto
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

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

    fun createUserCredential(id: UUID, email: String, passwordHash: String) {
        transaction {
            UserRegistrations.insertIgnore {
                it[UserRegistrations.id] = id
                it[UserRegistrations.email] = email
                it[UserRegistrations.passwordHash] = passwordHash
            }
        }
    }
}
