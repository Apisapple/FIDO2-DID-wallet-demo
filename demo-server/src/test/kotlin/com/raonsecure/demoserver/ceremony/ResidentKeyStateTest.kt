package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.common.data.AuthenticatorAttestationResponseDto
import com.raonsecure.demoserver.common.data.PublicKeyCredentialCreateResultDto
import com.raonsecure.demoserver.common.data.RegistrationOptionsRequest
import com.raonsecure.demoserver.testsupport.CeremonyHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The resident-key state comes from the client's `credProps` extension output. A client may omit
 * it, and no authenticator-data flag conveys discoverability, so absence must be recorded as
 * unknown rather than false.
 */
class ResidentKeyStateTest {
    private fun registerReporting(
        harness: CeremonyHarness,
        username: String,
        clientExtensionOutput: Map<String, Any>?,
    ) {
        val options =
            harness.registrationService.createOptions(
                RegistrationOptionsRequest(username = username),
            )
        val created =
            harness.fixture.register(
                challenge = options.challenge,
                username = username,
                userHandle =
                    java.util.Base64
                        .getUrlDecoder()
                        .decode(options.user.id),
            )
        harness.registrationService.verify(
            PublicKeyCredentialCreateResultDto(
                id = created.credentialId,
                rawId = created.credentialId,
                response =
                    AuthenticatorAttestationResponseDto(
                        clientDataJSON = created.clientDataJSON,
                        attestationObject = created.attestationObject,
                    ),
                clientExtensionOutput = clientExtensionOutput,
            ),
        )
    }

    @Test
    fun `options request the credProps extension`() {
        val harness = CeremonyHarness()

        val options =
            harness.registrationService.createOptions(
                RegistrationOptionsRequest(username = "alice"),
            )

        assertEquals(true, options.extensions?.get("credProps"))
    }

    @Test
    fun `a reported resident key is recorded as true`() {
        val harness = CeremonyHarness()

        registerReporting(harness, "alice", mapOf("credProps" to mapOf("rk" to true)))

        assertEquals(true, harness.credentials.single().rk)
    }

    @Test
    fun `a reported non-resident key is recorded as false`() {
        val harness = CeremonyHarness()

        registerReporting(harness, "alice", mapOf("credProps" to mapOf("rk" to false)))

        assertEquals(false, harness.credentials.single().rk)
    }

    @Test
    fun `an omitted extension is recorded as unknown`() {
        val harness = CeremonyHarness()

        registerReporting(harness, "alice", null)

        assertNull(harness.credentials.single().rk, "absence must not be read as false")
        assertEquals(1, harness.credentials.size, "registration should still succeed")
    }

    @Test
    fun `an extension output without rk is recorded as unknown`() {
        val harness = CeremonyHarness()

        registerReporting(harness, "alice", mapOf("credProps" to emptyMap<String, Any>()))

        assertNull(harness.credentials.single().rk)
    }

    @Test
    fun `an unrelated extension output does not set the resident key state`() {
        val harness = CeremonyHarness()

        registerReporting(harness, "alice", mapOf("appidExclude" to true))

        assertNull(harness.credentials.single().rk)
        assertTrue(harness.credentials.isNotEmpty())
    }
}
