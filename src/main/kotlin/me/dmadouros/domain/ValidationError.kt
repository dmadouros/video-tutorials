package me.dmadouros.domain

data class ValidationError(val dataPath: String, val message: String)
