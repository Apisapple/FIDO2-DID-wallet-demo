package com.example.demo.library.model.publickeycredential

import java.util.UUID

data class PublicKeyCredentialSource(
    val type: String = PublicKeyCredentialType.PUBLIC_KEY.value,
    val id: String,
    val rpId: String,
    val userHandle: String?,
    val aaguid: UUID,
)
