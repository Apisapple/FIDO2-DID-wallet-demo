package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.authentication.exception.AuthenticationVerificationException
import com.raonsecure.demoserver.authentication.exception.SignCounterRegressionException
import com.raonsecure.demoserver.common.data.AuthenticationOptionsRequest
import com.raonsecure.demoserver.testsupport.CeremonyHarness
import com.raonsecure.demoserver.testsupport.toDto
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

/**
 * A counter that did not advance has to be reported as its own condition, so a possible cloned or
 * replayed authenticator can be told apart from a malformed or unauthentic assertion. The
 * dedicated exception was previously unreachable, because every webauthn4j failure was wrapped
 * into one generic type.
 */
class SignCounterRegressionTest {
    @Test
    fun `a regressed counter is reported as a counter regression`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")
        harness.authenticate(credentialId = registered.credentialId, username = "alice")

        // The emulator advances its counter on every ceremony, so raise the stored value above
        // whatever the next assertion will report.
        harness.credentials.single().signCounter = 10_000L

        val failure =
            assertThrows<SignCounterRegressionException> {
                harness.authenticate(credentialId = registered.credentialId, username = "alice")
            }

        assertTrue(
            failure.message!!.contains("Sign counter regressed"),
            failure.message,
        )
    }

    @Test
    fun `a non-counter failure is not reported as a counter regression`() {
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

        // Corrupt the signature, leaving everything else intact.
        val signature = Base64.getUrlDecoder().decode(asserted.signature)
        signature[signature.size - 1] = (signature[signature.size - 1] + 1).toByte()
        val tampered =
            asserted.copy(
                signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature),
            )

        val failure =
            assertThrows<AuthenticationVerificationException> {
                harness.authenticationService.verify(tampered.toDto())
            }

        assertFalse(
            failure.message!!.contains("Sign counter regressed"),
            "an invalid signature must not be reported as a counter regression: ${failure.message}",
        )
        assertFalse(
            failure is SignCounterRegressionException,
            "an invalid signature must not raise the regression exception",
        )
    }

    @Test
    fun `an authenticator reporting zero is accepted when the stored counter is zero`() {
        val harness = CeremonyHarness()
        val registered = harness.register(username = "alice")

        // Model an authenticator that does not maintain a counter: it reports zero every time.
        harness.credentials.single().signCounter = 0L
        val options =
            harness.authenticationService.createOptions(
                AuthenticationOptionsRequest(username = "alice"),
            )
        val asserted =
            harness.fixture.authenticateWithFlags(
                challenge = options.challenge,
                credentialId = registered.credentialId,
                userPresent = true,
                userVerified = true,
            )

        harness.authenticationService.verify(asserted.toDto())

        assertNotNull(harness.credentials.single().authenticatedTimestamp)
    }
}
