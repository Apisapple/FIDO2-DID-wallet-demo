package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.common.data.AuthenticationOptionsRequest
import com.raonsecure.demoserver.common.data.AuthenticatorSelectionCriteria
import com.raonsecure.demoserver.common.data.RegistrationOptionsRequest
import com.raonsecure.demoserver.testsupport.CeremonyHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The advertised user verification requirement has to survive the two stateless requests, and the
 * stored policy must match what the client was told -- otherwise verification could enforce
 * something the options never advertised.
 */
class AdvertisedUserVerificationTest {
    @Test
    fun `registration stores the requirement it advertised`() {
        for (requested in listOf("required", "preferred", "discouraged")) {
            val harness = CeremonyHarness()

            val options =
                harness.registrationService.createOptions(
                    RegistrationOptionsRequest(
                        username = "alice",
                        authenticatorSelection = AuthenticatorSelectionCriteria(userVerification = requested),
                    ),
                )

            assertEquals(requested, options.authenticatorSelection?.userVerification)
            assertEquals(
                requested,
                harness.challengeStore.savedRegistration(options.challenge)?.userVerification,
                "stored policy should match advertised for $requested",
            )
        }
    }

    @Test
    fun `registration defaults to preferred when the request omits it`() {
        val harness = CeremonyHarness()

        val options =
            harness.registrationService.createOptions(
                RegistrationOptionsRequest(username = "alice"),
            )

        assertEquals("preferred", options.authenticatorSelection?.userVerification)
        assertEquals(
            "preferred",
            harness.challengeStore.savedRegistration(options.challenge)?.userVerification,
        )
    }

    @Test
    fun `authentication stores the requirement it advertised`() {
        for (requested in listOf("required", "preferred", "discouraged")) {
            val harness = CeremonyHarness()
            harness.register(username = "alice")

            val options =
                harness.authenticationService.createOptions(
                    AuthenticationOptionsRequest(username = "alice", userVerification = requested),
                )

            assertEquals(requested, options.userVerification)
            assertEquals(
                requested,
                harness.challengeStore.savedAuthentication(options.challenge)?.userVerification,
                "stored policy should match advertised for $requested",
            )
        }
    }

    @Test
    fun `authentication defaults to preferred when the request omits it`() {
        val harness = CeremonyHarness()
        harness.register(username = "alice")

        val options =
            harness.authenticationService.createOptions(
                AuthenticationOptionsRequest(username = "alice", userVerification = null),
            )

        assertEquals("preferred", options.userVerification)
        assertEquals(
            "preferred",
            harness.challengeStore.savedAuthentication(options.challenge)?.userVerification,
        )
    }
}
