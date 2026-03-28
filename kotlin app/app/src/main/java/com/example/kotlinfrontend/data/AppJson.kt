package com.example.kotlinfrontend.data

import kotlinx.serialization.json.Json

object AppJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
}
