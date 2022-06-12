package me.dmadouros.api.dtos

import io.konform.validation.Validation
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern
import java.util.UUID

data class RegisterUserDto(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val password: String,
)

val validateRegisterUserDto = Validation<RegisterUserDto> {
    RegisterUserDto::id required {
        pattern("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}") hint ("must be a valid UUID")
    }

    RegisterUserDto::email required {
        pattern(".*@.*") hint ("must be a valid email address")
    }

    RegisterUserDto::password required {
        minLength(8)
    }
}
