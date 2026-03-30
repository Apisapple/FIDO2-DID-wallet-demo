package com.example.demo.library.model.authenticator

enum class AuthenticatorTransport(
    val value: String,
) {
    BLE("ble"),
    INTERNAL("internal"),
    ;

    companion object {
        fun fromValue(value: String): AuthenticatorTransport? = entries.find { it.value == value }
    }
}
