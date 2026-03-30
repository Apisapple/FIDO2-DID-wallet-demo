package com.example.demo.library.model.publickeycredential

import com.example.demo.library.model.authenticator.UserVerificationRequirement

class PublicKeyCredentialRequestOptions(
    val challenge: String,
    val rpId: String,
    val allowCredentials: List<PublicKeyCredentialDescriptor>?,
    val userVerification: UserVerificationRequirement = UserVerificationRequirement.PREFERRED,
    val extension: ClientExtensionInput?,
)
