package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.authentication.exception.AuthenticationVerificationException
import com.raonsecure.demoserver.authentication.exception.UserHandleMismatchException
import com.raonsecure.demoserver.common.data.AuthenticationOptionsRequest
import com.raonsecure.demoserver.testsupport.CeremonyHarness
import com.raonsecure.demoserver.testsupport.toDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * The asserted `userHandle` must belong to the credential being used. A usernameless ceremony
 * additionally has to carry one, since nothing else tells the server who is authenticating.
 */
class UserHandleBindingTest {
    @Test
    fun `options record whether a username was provided`() {
        val harness = CeremonyHarness()
        harness.register(username = "alice")

        val named =
            harness.authenticationService.createOptions(
                AuthenticationOptionsRequest(username = "alice"),
            )
        assertTrue(
            harness.challengeStore.savedAuthentication(named.challenge)!!.usernameProvided,
        )

        val usernameless =
            harness.authenticationService.createOptions(
                AuthenticationOptionsRequest(username = null),
            )
        assertFalse(
            harness.challengeStore.savedAuthentication(usernameless.challenge)!!.usernameProvided,
        )
    }

    @Test
    fun `matching user handle is accepted`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")

        harness.authenticate(credentialId = registered.credentialId, username = "alice")

        assertNotNull(harness.credentials.single().authenticatedTimestamp)
    }

    @Test
    fun `user handle belonging to another user is rejected`() {
        val harness = CeremonyHarness()
        val alice = harness.register(username = "alice")
        harness.register(username = "bob")
        val bobHandle = harness.credentials.single { it.username == "bob" }.userId

        val options =
            harness.authenticationService.createOptions(
                AuthenticationOptionsRequest(username = "alice"),
            )
        val asserted =
            harness.fixture.authenticate(
                challenge = options.challenge,
                credentialId = alice.credentialId,
            )

        assertThrows<UserHandleMismatchException> {
            harness.authenticationService.verify(asserted.toDto(overrideUserHandle = bobHandle))
        }
    }

    @Test
    fun `usernameless ceremony without a user handle is rejected`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")

        val options =
            harness.authenticationService.createOptions(
                AuthenticationOptionsRequest(username = null),
            )
        val asserted =
            harness.fixture.authenticate(
                challenge = options.challenge,
                credentialId = registered.credentialId,
            )

        val failure =
            assertThrows<AuthenticationVerificationException> {
                harness.authenticationService.verify(asserted.toDto(overrideUserHandle = null))
            }
        assertTrue(
            failure.message!!.contains("must carry a userHandle"),
            failure.message,
        )
    }

    @Test
    fun `usernameless ceremony with the right user handle is accepted`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")
        val handle = harness.credentials.single().userId

        val options =
            harness.authenticationService.createOptions(
                AuthenticationOptionsRequest(username = null),
            )
        val asserted =
            harness.fixture.authenticate(
                challenge = options.challenge,
                credentialId = registered.credentialId,
            )
        harness.authenticationService.verify(asserted.toDto(overrideUserHandle = handle))

        assertNotNull(harness.credentials.single().authenticatedTimestamp)
    }

    @Test
    fun `named ceremony without a user handle is accepted`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")

        val options =
            harness.authenticationService.createOptions(
                AuthenticationOptionsRequest(username = "alice"),
            )
        val asserted =
            harness.fixture.authenticate(
                challenge = options.challenge,
                credentialId = registered.credentialId,
            )
        harness.authenticationService.verify(asserted.toDto(overrideUserHandle = null))

        assertEquals(1, harness.credentials.size)
        assertNotNull(harness.credentials.single().authenticatedTimestamp)
    }
}
