package com.example.demo.library.model

enum class AuthenticationMethod(
    val value: String,
) {
    BIOMETRIC("biometric"),
    DEVICE_CREDENTIAL("DeviceCredential"),
    ;

    companion object {
        fun fromValue(value: String): AuthenticationMethod? = entries.find { it.value == value }
    }
}
