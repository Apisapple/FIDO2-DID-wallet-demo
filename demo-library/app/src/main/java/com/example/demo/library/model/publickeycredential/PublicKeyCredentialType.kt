package com.example.demo.library.model.publickeycredential

enum class PublicKeyCredentialType(
    val value: String,
) {
    PUBLIC_KEY("public-key"),
    ;

    companion object {
        fun fromValue(value: String): PublicKeyCredentialType? = entries.find { it.value == value }
    }
}
