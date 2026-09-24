package com.raonsecure.demoserver.authentication.challenge

import java.io.Serializable

data class PendingAuthenticationContext(
    val username: String?,
    val challenge: String,
    val credentialIds: List<String> = emptyList(),
) : Serializable
