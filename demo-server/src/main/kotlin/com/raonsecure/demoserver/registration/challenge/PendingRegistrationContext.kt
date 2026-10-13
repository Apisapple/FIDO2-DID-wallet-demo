package com.raonsecure.demoserver.registration.challenge

import com.raonsecure.demoserver.common.data.UserVerificationRequirement
import java.io.Serializable

data class PendingRegistrationContext(
    val username: String,
    val userId: String,
    val userDisplayName: String,
    val challenge: String,
    /**
     * The user verification requirement advertised in the options, carried across the two
     * stateless requests so verification enforces the policy the server itself stated.
     */
    val userVerification: String = UserVerificationRequirement.PREFERRED.value,
) : Serializable
