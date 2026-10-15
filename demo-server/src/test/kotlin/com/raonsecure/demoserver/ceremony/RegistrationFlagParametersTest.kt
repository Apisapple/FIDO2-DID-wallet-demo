package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.base.entity.RpEntity
import com.raonsecure.demoserver.common.data.AuthenticatorAttestationResponseDto
import com.raonsecure.demoserver.common.data.AuthenticatorSelectionCriteria
import com.raonsecure.demoserver.common.data.PublicKeyCredentialCreateResultDto
import com.raonsecure.demoserver.common.data.RegistrationOptionsRequest
import com.raonsecure.demoserver.config.WebAuthnRpProperties
import com.raonsecure.demoserver.registration.RegistrationService
import com.raonsecure.demoserver.testsupport.CeremonyFixture
import com.raonsecure.demoserver.testsupport.InMemoryChallengeStore
import com.raonsecure.demoserver.testsupport.fakeUserKeyRepository
import com.webauthn4j.WebAuthnManager
import com.webauthn4j.data.RegistrationData
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.data.RegistrationRequest
import com.webauthn4j.data.client.CollectedClientData
import com.webauthn4j.data.client.challenge.DefaultChallenge
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Base64
import java.util.Optional

/**
 * Asserts the user-presence and user-verification flags the registration service hands to
 * webauthn4j.
 *
 * This is the parameter-level fallback for the registration rejection scenarios: webauthn4j's
 * emulator hardcodes `userConsent`/`userVerification` to true inside `makeCredential`
 * (WebAuthnModelAuthenticator), so an attestation with either flag clear cannot be produced and
 * the end-to-end rejection cannot be driven. webauthn4j itself does perform the check
 * (RegistrationDataVerifier calls UPUVFlagsVerifier.verify with these two parameters), so pinning
 * the parameters pins the behaviour. The assertion path has no such limitation and is covered
 * end to end in [UserPresenceVerificationTest].
 */
class RegistrationFlagParametersTest {

    private val rp = RpEntity(id = "localhost", name = "Fixture RP")

    private fun captureParameters(advertisedUserVerification: String): RegistrationParameters {
        val rpRepository = mock<com.raonsecure.demoserver.base.repository.RpRepository>().also {
            whenever(it.findById("localhost")).thenReturn(Optional.of(rp))
        }
        val challengeStore = InMemoryChallengeStore()
        val webAuthnManager: WebAuthnManager = mock()
        val service = RegistrationService(
            WebAuthnRpProperties(
                id = CeremonyFixture.DEFAULT_RP_ID,
                name = "Fixture RP",
                origins = setOf(CeremonyFixture.DEFAULT_ORIGIN),
            ),
            rpRepository,
            fakeUserKeyRepository(),
            challengeStore,
            webAuthnManager,
        )

        val options = service.createOptions(
            RegistrationOptionsRequest(
                username = "alice",
                authenticatorSelection = AuthenticatorSelectionCriteria(
                    userVerification = advertisedUserVerification,
                ),
            ),
        )

        // Real ceremony bytes, so parse() yields the challenge the service looks up; verify() is
        // stubbed because this test is about what the service asks for, not what webauthn4j does.
        val fixture = CeremonyFixture()
        val created = fixture.register(challenge = options.challenge, username = "alice")

        val parsed: com.webauthn4j.data.RegistrationData = mock()
        val clientData: CollectedClientData = mock()
        whenever(clientData.challenge).thenReturn(
            DefaultChallenge(Base64.getUrlDecoder().decode(options.challenge)),
        )
        whenever(parsed.collectedClientData).thenReturn(clientData)
        whenever(webAuthnManager.parse(any<RegistrationRequest>())).thenReturn(parsed)

        val verified: RegistrationData = mock()
        whenever(webAuthnManager.verify(any<RegistrationRequest>(), any<RegistrationParameters>()))
            .thenReturn(verified)

        runCatching {
            service.verify(
                PublicKeyCredentialCreateResultDto(
                    id = created.credentialId,
                    rawId = created.credentialId,
                    response = AuthenticatorAttestationResponseDto(
                        clientDataJSON = created.clientDataJSON,
                        attestationObject = created.attestationObject,
                    ),
                ),
            )
        }

        val captor = argumentCaptor<RegistrationParameters>()
        verify(webAuthnManager).verify(any<RegistrationRequest>(), captor.capture())
        return captor.firstValue
    }

    @Test
    fun `user presence is always required`() {
        for (advertised in listOf("required", "preferred", "discouraged")) {
            assertTrue(
                captureParameters(advertised).isUserPresenceRequired,
                "UP must be required when advertising $advertised",
            )
        }
    }

    @Test
    fun `user verification is required only when advertised as required`() {
        assertTrue(captureParameters("required").isUserVerificationRequired)
        assertFalse(captureParameters("preferred").isUserVerificationRequired)
        assertFalse(captureParameters("discouraged").isUserVerificationRequired)
    }
}
