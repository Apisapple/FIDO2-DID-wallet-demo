package com.example.demo.library.model

enum class AuthenticatorAttachment(val value: String) {
    PLATFORM("platform"),
    CROSS_PLATFORM("cross-platform");

    companion object {
        fun fromValue(value: String): AuthenticatorAttachment? =
            entries.find { it.value == value }
    }
}