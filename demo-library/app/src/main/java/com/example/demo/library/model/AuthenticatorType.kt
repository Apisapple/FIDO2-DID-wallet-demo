package com.example.demo.library.model

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

enum class AuthenticatorType(
    val authenticatorName: String,
    val aaguid: UUID,
) {
    BIOMETRIC_NONE("BiometricNone", UUID.fromString("fca28135-9915-4c70-de30-56de200fd2cb")),
    BIOMETRIC_ANDROID_KEY(
        "BiometricAndroidKey",
        UUID.fromString("8c120a4d-52b3-99ef-eaf6-7cfb2a3e3f89"),
    ),
    DEVICE_CREDENTIAL_NONE(
        "DeviceCredentialNone",
        UUID.fromString("616fcfb7-8fe1-4b6a-ec90-5db1424a9b36"),
    ),
    DEVICE_CREDENTIAL_ANDROID_KEY(
        "DeviceCredentialAndroidKey",
        UUID.fromString("2b7a96a3-f571-ee4c-632c-c5458dfadfe3"),
    ),
    ;

    fun aaguidBytes(): ByteArray =
        ByteBuffer
            .wrap(ByteArray(16))
            .apply {
                order(ByteOrder.BIG_ENDIAN)
                putLong(aaguid.mostSignificantBits)
                putLong(aaguid.leastSignificantBits)
            }.array()

    fun getAuthenticationMethod(): AuthenticationMethod =
        when (this) {
            BIOMETRIC_NONE -> AuthenticationMethod.BIOMETRIC
            BIOMETRIC_ANDROID_KEY -> AuthenticationMethod.BIOMETRIC
            DEVICE_CREDENTIAL_NONE -> AuthenticationMethod.DEVICE_CREDENTIAL
            DEVICE_CREDENTIAL_ANDROID_KEY -> AuthenticationMethod.DEVICE_CREDENTIAL
        }

    fun getAttestationStatementFormat(): AttestationStatementFormat =
        when (this) {
            BIOMETRIC_NONE -> AttestationStatementFormat.NONE
            BIOMETRIC_ANDROID_KEY -> AttestationStatementFormat.ANDROID_KEY
            DEVICE_CREDENTIAL_NONE -> AttestationStatementFormat.NONE
            DEVICE_CREDENTIAL_ANDROID_KEY -> AttestationStatementFormat.ANDROID_KEY
        }

    companion object {
        fun getAuthenticatorType(
            authenticationMethod: AuthenticationMethod,
            attestationStateFormat: AttestationStatementFormat,
        ): AuthenticatorType =
            when (authenticationMethod) {
                AuthenticationMethod.BIOMETRIC -> {
                    when (attestationStateFormat) {
                        AttestationStatementFormat.NONE -> BIOMETRIC_NONE
                        AttestationStatementFormat.ANDROID_KEY -> BIOMETRIC_ANDROID_KEY
                    }
                }

                AuthenticationMethod.DEVICE_CREDENTIAL -> {
                    when (attestationStateFormat) {
                        AttestationStatementFormat.NONE -> DEVICE_CREDENTIAL_NONE
                        AttestationStatementFormat.ANDROID_KEY -> DEVICE_CREDENTIAL_ANDROID_KEY
                    }
                }
            }
    }
}
