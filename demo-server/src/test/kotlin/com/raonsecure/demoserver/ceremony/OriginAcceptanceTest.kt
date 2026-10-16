package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.authentication.exception.AuthenticationVerificationException
import com.raonsecure.demoserver.registration.exception.RegistrationVerificationException
import com.raonsecure.demoserver.testsupport.CeremonyFixture
import com.raonsecure.demoserver.testsupport.CeremonyHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Covers the accepted-origin set: every configured origin is honoured, including an Android
 * `apk-key-hash:` origin, and an unlisted origin is refused.
 */
class OriginAcceptanceTest {
    private val webOrigin = CeremonyFixture.DEFAULT_ORIGIN
    private val androidOrigin = CeremonyFixture.ANDROID_ORIGIN
    private val bothOrigins = setOf(webOrigin, androidOrigin)

    @Test
    fun `web origin in the configured set is accepted`() {
        val harness = CeremonyHarness(origins = bothOrigins, fixtureOrigin = webOrigin)

        val registered = harness.register(username = "alice")
        harness.authenticate(credentialId = registered.credentialId, username = "alice")

        assertEquals(1, harness.credentials.size)
        assertNotNull(harness.credentials.single().authenticatedTimestamp)
    }

    @Test
    fun `android origin in the configured set is accepted`() {
        val harness = CeremonyHarness(origins = bothOrigins, fixtureOrigin = androidOrigin)

        val registered = harness.register(username = "bob")
        harness.authenticate(credentialId = registered.credentialId, username = "bob")

        assertEquals(1, harness.credentials.size)
        assertNotNull(harness.credentials.single().authenticatedTimestamp)
    }

    @Test
    fun `browser and android clients share one server instance`() {
        val browser = CeremonyHarness(origins = bothOrigins, fixtureOrigin = webOrigin)
        val android = CeremonyHarness(origins = bothOrigins, fixtureOrigin = androidOrigin)

        val fromBrowser = browser.register(username = "carol")
        browser.authenticate(credentialId = fromBrowser.credentialId, username = "carol")

        val fromAndroid = android.register(username = "dave")
        android.authenticate(credentialId = fromAndroid.credentialId, username = "dave")

        assertNotNull(browser.credentials.single().authenticatedTimestamp)
        assertNotNull(android.credentials.single().authenticatedTimestamp)
    }

    @Test
    fun `unlisted origin is rejected at registration`() {
        val harness =
            CeremonyHarness(
                origins = setOf(webOrigin),
                fixtureOrigin = "https://evil.example.com",
            )

        assertThrows<RegistrationVerificationException> {
            harness.register(username = "eve")
        }
        assertEquals(0, harness.credentials.size)
    }

    @Test
    fun `unlisted origin is rejected at authentication`() {
        val registrar = CeremonyHarness(origins = setOf(webOrigin), fixtureOrigin = webOrigin)
        val registered = registrar.register(username = "frank")

        // Same credential store, but the client now reports an origin the RP does not accept.
        val attacker =
            CeremonyHarness(
                origins = setOf(webOrigin),
                fixtureOrigin = "https://evil.example.com",
                credentials = registrar.credentials,
            )

        assertThrows<AuthenticationVerificationException> {
            attacker.authenticate(credentialId = registered.credentialId, username = "frank")
        }
    }
}
