package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.testsupport.CeremonyFixture
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64

/**
 * Pins the flag-suppression helpers, since the UP-absent and UV-absent rejection scenarios are
 * only testable if the emulator really clears those bits.
 */
class CeremonyFlagHelperTest {

    /** Flags byte sits at offset 32, right after the 32-byte RP ID hash. */
    private fun flagsOf(authenticatorDataB64: String): Byte =
        Base64.getUrlDecoder().decode(authenticatorDataB64)[32]

    @Test
    fun `assertion helper clears user presence when asked`() {
        val fixture = CeremonyFixture()
        val registered = fixture.register(challenge = fixture.newChallenge(), username = "alice")

        val flags = flagsOf(
            fixture.authenticateWithFlags(
                challenge = fixture.newChallenge(),
                credentialId = registered.credentialId,
                userPresent = false,
                userVerified = true,
            ).authenticatorData,
        )

        assertFalse(AuthenticatorData.checkFlagUP(flags), "UP should be clear")
        assertTrue(AuthenticatorData.checkFlagUV(flags), "UV should be set")
    }

    @Test
    fun `assertion helper clears user verification when asked`() {
        val fixture = CeremonyFixture()
        val registered = fixture.register(challenge = fixture.newChallenge(), username = "bob")

        val flags = flagsOf(
            fixture.authenticateWithFlags(
                challenge = fixture.newChallenge(),
                credentialId = registered.credentialId,
                userPresent = true,
                userVerified = false,
            ).authenticatorData,
        )

        assertTrue(AuthenticatorData.checkFlagUP(flags), "UP should be set")
        assertFalse(AuthenticatorData.checkFlagUV(flags), "UV should be clear")
    }

    @Test
    fun `assertion helper sets both flags when asked`() {
        val fixture = CeremonyFixture()
        val registered = fixture.register(challenge = fixture.newChallenge(), username = "carol")

        val flags = flagsOf(
            fixture.authenticateWithFlags(
                challenge = fixture.newChallenge(),
                credentialId = registered.credentialId,
                userPresent = true,
                userVerified = true,
            ).authenticatorData,
        )

        assertTrue(AuthenticatorData.checkFlagUP(flags))
        assertTrue(AuthenticatorData.checkFlagUV(flags))
    }
}
