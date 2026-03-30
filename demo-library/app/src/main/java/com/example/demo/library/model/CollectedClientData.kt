package com.example.demo.library.model

import kotlinx.serialization.Serializable

@Serializable
data class CollectedClientData(
    val type: String,
    val challenge: String,
    val origin: String,
)
