package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.common.data.RegistrationOptionsRequest
import com.raonsecure.demoserver.testsupport.CeremonyHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64

/**
 * A user handle must be unpredictable: it previously base64-encoded the username, which let
 * anyone recover it. One user's credentials still have to share a single handle.
 */
class UserHandleTest {
    private val urlDecoder = Base64.getUrlDecoder()

    private fun handleFor(
        harness: CeremonyHarness,
        username: String,
    ): String =
        harness.registrationService
            .createOptions(RegistrationOptionsRequest(username = username))
            .user.id

    @Test
    fun `a new handle is 32 random bytes`() {
        val harness = CeremonyHarness()

        val handle = handleFor(harness, "alice")

        assertEquals(32, urlDecoder.decode(handle).size)
    }

    @Test
    fun `a new handle does not encode the username`() {
        val harness = CeremonyHarness()
        val username = "alice"

        val handle = handleFor(harness, username)
        val decoded = urlDecoder.decode(handle)

        val legacy =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(username.toByteArray(Charsets.UTF_8))
        assertNotEquals(legacy, handle, "handle must not be the base64 of the username")
        assertFalse(
            String(decoded, Charsets.ISO_8859_1).contains(username),
            "handle bytes must not contain the username",
        )
    }

    @Test
    fun `two usernames get unrelated handles`() {
        val harness = CeremonyHarness()

        val alice = handleFor(harness, "alice")
        val bob = handleFor(harness, "bob")

        assertNotEquals(alice, bob)
        // Neither can be derived from the other: no shared prefix of any meaningful length.
        val a = urlDecoder.decode(alice)
        val b = urlDecoder.decode(bob)
        val sharedPrefix = a.zip(b).takeWhile { (x, y) -> x == y }.size
        assertTrue(sharedPrefix < 8, "handles share a $sharedPrefix-byte prefix")
    }

    @Test
    fun `repeated options for a new user yield different handles`() {
        val harness = CeremonyHarness()

        assertNotEquals(handleFor(harness, "alice"), handleFor(harness, "alice"))
    }

    @Test
    fun `a returning user keeps the handle stored for their credentials`() {
        val harness = CeremonyHarness()
        harness.register(username = "alice")
        val stored = harness.credentials.single().userId

        val handle = handleFor(harness, "alice")

        assertEquals(stored, handle)
    }

    @Test
    fun `a second credential for one user reuses the same handle`() {
        val harness = CeremonyHarness()
        harness.register(username = "alice")
        val first = harness.credentials.single().userId

        harness.register(username = "alice")

        assertEquals(2, harness.credentials.size)
        assertTrue(harness.credentials.all { it.userId == first }, "all handles should match")
    }
}
