package com.example.demo.library.model.authenticator

interface AuthenticatorResponse {
    val clientDataJson: ByteArray
}

class AuthenticatorAttestationResponse(
    override val clientDataJson: ByteArray,
    val attestationObject: ByteArray,
) : AuthenticatorResponse

class AuthenticatorAssertionResponse(
    val authenticatorData: ByteArray,
    val signature: ByteArray,
    val userHandle: ByteArray?,
    override val clientDataJson: ByteArray,
) : AuthenticatorResponse
