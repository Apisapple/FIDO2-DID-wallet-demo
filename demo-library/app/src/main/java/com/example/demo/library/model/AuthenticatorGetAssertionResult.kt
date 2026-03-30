package com.example.demo.library.model

data class AuthenticatorGetAssertionResult(
    val credential: ByteArray,
    val authenticatorData: ByteArray,
    val signature: ByteArray,
    val userHandle: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuthenticatorGetAssertionResult

        if (!credential.contentEquals(other.credential)) return false
        if (!authenticatorData.contentEquals(other.authenticatorData)) return false
        if (!signature.contentEquals(other.signature)) return false
        if (!userHandle.contentEquals(other.userHandle)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = credential.contentHashCode()
        result = 31 * result + authenticatorData.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        result = 31 * result + (userHandle?.contentHashCode() ?: 0)
        return result
    }
}
