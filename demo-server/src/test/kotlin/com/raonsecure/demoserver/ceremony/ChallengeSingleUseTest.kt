package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.authentication.exception.AuthenticationChallengeNotFoundException
import com.raonsecure.demoserver.common.data.AuthenticationOptionsRequest
import com.raonsecure.demoserver.common.data.AuthenticatorAssertionResponseDto
import com.raonsecure.demoserver.common.data.AuthenticatorAttestationResponseDto
import com.raonsecure.demoserver.common.data.PublicKeyCredentialCreateResultDto
import com.raonsecure.demoserver.common.data.PublicKeyCredentialGetResultDto
import com.raonsecure.demoserver.common.data.RegistrationOptionsRequest
import com.raonsecure.demoserver.registration.exception.ChallengeNotFoundException
import com.raonsecure.demoserver.registration.exception.DuplicateCredentialException
import com.raonsecure.demoserver.testsupport.CeremonyHarness
import com.raonsecure.demoserver.testsupport.toDto
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

/**
 * A challenge must be consumed once a verification attempt completes, whether it succeeded or
 * failed. The failure cases here reach the service with a well-formed clientDataJSON — so the
 * challenge is readable — but an unparseable attestation object or assertion, which is what makes
 * them regression tests: while the challenge was read from the *parsed* result, a parse failure
 * returned before anything was consumed and left the challenge replayable for the rest of its TTL.
 */
class ChallengeSingleUseTest {

    @Test
    fun `registration challenge is consumed when attestation parsing fails`() {
        val harness = CeremonyHarness()

        val options = harness.registrationService.createOptions(
            RegistrationOptionsRequest(username = "alice"),
        )
        val created = harness.fixture.register(
            challenge = options.challenge,
            username = "alice",
            userHandle = Base64.getUrlDecoder().decode(options.user.id),
        )

        // Real clientDataJSON carrying the issued challenge, with a garbage attestation object.
        val corrupted = PublicKeyCredentialCreateResultDto(
            id = created.credentialId,
            rawId = created.credentialId,
            response = AuthenticatorAttestationResponseDto(
                clientDataJSON = created.clientDataJSON,
                attestationObject = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("not a CBOR attestation object".toByteArray()),
            ),
        )

        assertThrows<Exception> { harness.registrationService.verify(corrupted) }

        assertNull(
            harness.challengeStore.savedRegistration(options.challenge),
            "challenge must not survive a failed verification attempt",
        )

        // The genuine attestation for that same challenge can no longer be replayed.
        assertThrows<ChallengeNotFoundException> {
            harness.registrationService.verify(created.toDto())
        }
    }

    @Test
    fun `authentication challenge is consumed when assertion parsing fails`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")

        val options = harness.authenticationService.createOptions(
            AuthenticationOptionsRequest(username = "alice"),
        )
        val asserted = harness.fixture.authenticate(
            challenge = options.challenge,
            credentialId = registered.credentialId,
        )

        // Real clientDataJSON carrying the issued challenge, with garbage authenticator data.
        val corrupted = PublicKeyCredentialGetResultDto(
            id = asserted.credentialId,
            rawId = asserted.credentialId,
            response = AuthenticatorAssertionResponseDto(
                clientDataJSON = asserted.clientDataJSON,
                authenticatorData = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("short".toByteArray()),
                signature = asserted.signature,
                userHandle = asserted.userHandle,
            ),
        )

        assertThrows<Exception> { harness.authenticationService.verify(corrupted) }

        assertNull(
            harness.challengeStore.savedAuthentication(options.challenge),
            "challenge must not survive a failed verification attempt",
        )

        // The genuine assertion for that same challenge can no longer be replayed.
        assertThrows<AuthenticationChallengeNotFoundException> {
            harness.authenticationService.verify(asserted.toDto())
        }
    }

    /**
     * Replaying a successful registration is caught by the duplicate-credential check, which runs
     * before the challenge is read, so the rejection is a duplicate rather than a missing
     * challenge. The challenge is nonetheless gone, which is what this asserts after the fact.
     */
    /**
     * Replaying a successful registration is caught by the duplicate-credential check, which runs
     * before the challenge is read, so the rejection is a duplicate rather than a missing
     * challenge. The challenge is nonetheless gone, which is what this asserts after the fact.
     */
    @Test
    fun `a successful registration consumes its challenge`() {
        val harness = CeremonyHarness()

        val options = harness.registrationService.createOptions(
            RegistrationOptionsRequest(username = "alice"),
        )
        val created = harness.fixture.register(
            challenge = options.challenge,
            username = "alice",
            userHandle = Base64.getUrlDecoder().decode(options.user.id),
        )
        harness.registrationService.verify(created.toDto())

        assertNull(
            harness.challengeStore.savedRegistration(options.challenge),
            "a verified registration must leave no reusable challenge",
        )
        assertThrows<DuplicateCredentialException> {
            harness.registrationService.verify(created.toDto())
        }
    }

    @Test
    fun `a successful assertion cannot be replayed`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")

        val options = harness.authenticationService.createOptions(
            AuthenticationOptionsRequest(username = "alice"),
        )
        val asserted = harness.fixture.authenticate(
            challenge = options.challenge,
            credentialId = registered.credentialId,
        )
        harness.authenticationService.verify(asserted.toDto())

        assertNull(
            harness.challengeStore.savedAuthentication(options.challenge),
            "a verified assertion must leave no reusable challenge",
        )
        assertThrows<AuthenticationChallengeNotFoundException> {
            harness.authenticationService.verify(asserted.toDto())
        }
    }
}
