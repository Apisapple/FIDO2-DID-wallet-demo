package com.raonsecure.demoserver.authentication.challenge

import com.raonsecure.demoserver.common.data.UserVerificationRequirement
import java.io.Serializable

data class PendingAuthenticationContext(
    val username: String?,
    val challenge: String,
    val credentialIds: List<String> = emptyList(),
    /**
     * The user verification requirement advertised in the options, carried across the two
     * stateless requests so verification enforces the policy the server itself stated.
     */
    val userVerification: String = UserVerificationRequirement.PREFERRED.value,
    /**
     * Whether the ceremony was started with a username. Recorded explicitly rather than inferred
     * from [credentialIds] being empty, because a usernameless ceremony and a named one for a user
     * with no credentials would otherwise look alike. A usernameless ceremony must carry a
     * `userHandle` in the assertion; a named one need not.
     */
    val usernameProvided: Boolean = true,
) : Serializable
