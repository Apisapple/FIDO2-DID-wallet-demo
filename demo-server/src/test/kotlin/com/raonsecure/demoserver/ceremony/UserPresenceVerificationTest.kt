package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.authentication.exception.AuthenticationVerificationException
import com.raonsecure.demoserver.testsupport.CeremonyHarness
import com.webauthn4j.data.UserVerificationRequirement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * User presence must always be asserted, and user verification must be enforced exactly when the
 * options advertised `required`. Before this change both flags were passed to webauthn4j as
 * `false`, so neither was ever checked.
 *
 * The registration *rejection* cases live in [RegistrationFlagParametersTest] instead: the
 * emulator hardcodes userConsent and userVerification to true in `makeCredential`, so a
 * UP-absent or UV-absent attestation cannot be produced. Only the accept cases are covered here.
 */
class UserPresenceVerificationTest {

    // ----- registration -----

    @Test
    fun `registration is accepted when user presence is asserted`() {
        val harness = CeremonyHarness()

        harness.registerWithFlags(username = "alice", userPresent = true, userVerified = true)

        assertEquals(1, harness.credentials.size)
    }

    @Test
    fun `registration is accepted when required user verification is satisfied`() {
        val harness = CeremonyHarness()

        harness.registerWithFlags(
            username = "alice",
            advertisedUserVerification = UserVerificationRequirement.REQUIRED,
            userVerified = true,
        )

        assertEquals(1, harness.credentials.size)
    }

    @Test
    fun `registration is accepted when preferred user verification is absent`() {
        val harness = CeremonyHarness()

        harness.registerWithFlags(
            username = "alice",
            advertisedUserVerification = UserVerificationRequirement.PREFERRED,
            userVerified = false,
        )

        assertEquals(1, harness.credentials.size)
    }

    // ----- authentication -----

    @Test
    fun `authentication is rejected when user presence is absent`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")

        assertThrows<AuthenticationVerificationException> {
            harness.authenticateWithFlags(
                credentialId = registered.credentialId,
                userPresent = false,
                userVerified = true,
            )
        }
    }

    @Test
    fun `authentication is accepted when user presence is asserted`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")

        harness.authenticateWithFlags(
            credentialId = registered.credentialId,
            userPresent = true,
            userVerified = true,
        )

        assertNotNull(harness.credentials.single().authenticatedTimestamp)
    }

    @Test
    fun `authentication is rejected when required user verification is absent`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")

        assertThrows<AuthenticationVerificationException> {
            harness.authenticateWithFlags(
                credentialId = registered.credentialId,
                advertisedUserVerification = UserVerificationRequirement.REQUIRED,
                userVerified = false,
            )
        }
    }

    @Test
    fun `authentication is accepted when required user verification is satisfied`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")

        harness.authenticateWithFlags(
            credentialId = registered.credentialId,
            advertisedUserVerification = UserVerificationRequirement.REQUIRED,
            userVerified = true,
        )

        assertNotNull(harness.credentials.single().authenticatedTimestamp)
    }

    @Test
    fun `authentication is accepted when preferred user verification is absent`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")

        harness.authenticateWithFlags(
            credentialId = registered.credentialId,
            advertisedUserVerification = UserVerificationRequirement.PREFERRED,
            userVerified = false,
        )

        assertNotNull(harness.credentials.single().authenticatedTimestamp)
    }
}
