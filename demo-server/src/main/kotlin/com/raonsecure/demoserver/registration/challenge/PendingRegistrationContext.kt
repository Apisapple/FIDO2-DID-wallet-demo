package com.raonsecure.demoserver.registration.challenge

import java.io.Serializable

data class PendingRegistrationContext(
    val username: String,
    val userId: String,
    val userDisplayName: String,
    val challenge: String,
) : Serializable
