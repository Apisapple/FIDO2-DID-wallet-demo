package com.example.demo.library.model.publickeycredential

import com.example.demo.library.model.authenticator.AuthenticatorTransport

class PublicKeyCredentialDescriptor(
    var type: String,
    var id: String,
    var transport: List<AuthenticatorTransport>? = listOf(AuthenticatorTransport.INTERNAL),
)
