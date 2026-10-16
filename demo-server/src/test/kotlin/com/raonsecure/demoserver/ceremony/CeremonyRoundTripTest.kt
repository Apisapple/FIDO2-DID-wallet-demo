package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.testsupport.CeremonyHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Exercises registration followed by authentication against a real WebAuthnManager and the
 * authenticator emulator, so the parse/verify path the mocked service tests skip is covered.
 */
class CeremonyRoundTripTest {
    @Test
    fun `registration then authentication succeeds end to end`() {
        val harness = CeremonyHarness()

        val registered = harness.register(username = "alice")

        assertEquals(1, harness.credentials.size)
        val stored = harness.credentials.single()
        assertEquals(registered.credentialId, stored.credentialId)
        assertEquals("alice", stored.username)
        assertTrue(stored.publicKey.isNotBlank())
        assertNotNull(stored.registeredTimestamp)

        harness.authenticate(credentialId = registered.credentialId, username = "alice")

        val afterAuth = harness.credentials.single()
        assertNotNull(afterAuth.authenticatedTimestamp)
    }
}
