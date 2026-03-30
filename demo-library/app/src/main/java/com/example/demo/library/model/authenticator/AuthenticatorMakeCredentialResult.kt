package com.example.demo.library.model.authenticator

data class AuthenticatorMakeCredentialResult(
    val credentialId: ByteArray,
    val attestationObject: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuthenticatorMakeCredentialResult

        if (!credentialId.contentEquals(other.credentialId)) return false
        if (!attestationObject.contentEquals(other.attestationObject)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = credentialId.contentHashCode()
        result = 31 * result + attestationObject.contentHashCode()
        return result
    }
}
