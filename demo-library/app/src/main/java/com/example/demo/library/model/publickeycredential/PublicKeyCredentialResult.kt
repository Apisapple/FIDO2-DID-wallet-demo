package com.example.demo.library.model.publickeycredential

import com.example.demo.library.model.authenticator.AuthenticatorAssertionResponse
import com.example.demo.library.model.authenticator.AuthenticatorAttestationResponse
import com.example.demo.library.util.base64urlToByteArray

class PublicKeyCredentialCreateResult(
    val id: String,
    val authenticatorAttestationResponse: AuthenticatorAttestationResponse,
    val clientExtensionOutput: ClientExteionsionOutput? = null,
) {
    var rawId: ByteArray = id.base64urlToByteArray()
    val type: String = PublicKeyCredentialType.PUBLIC_KEY.value
}

class PublicKeyCredentialGetResult(
    val id: String,
    val authenticatorAssertionResponse: AuthenticatorAssertionResponse,
    val clientExtensionOutput: ClientExteionsionOutput? = null,
) {
    val rawId: ByteArray = id.base64urlToByteArray()
    val type: String = PublicKeyCredentialType.PUBLIC_KEY.value
}
