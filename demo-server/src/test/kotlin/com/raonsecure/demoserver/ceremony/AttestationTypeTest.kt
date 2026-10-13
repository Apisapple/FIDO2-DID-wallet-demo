package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.base.entity.AttestationType
import com.raonsecure.demoserver.testsupport.CeremonyHarness
import com.webauthn4j.data.AttestationConveyancePreference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The persisted attestation type must come from the verified statement; it was previously
 * hardcoded to NONE regardless of what the authenticator sent.
 *
 * Self attestation is covered in [AttestationTypeMappingTest] rather than here: the emulator's
 * PackedAuthenticator always attaches a certificate path, so a packed statement with a null x5c
 * cannot be produced end to end.
 */
class AttestationTypeTest {

    @Test
    fun `packed attestation with a certificate chain is recorded as basic`() {
        val harness = CeremonyHarness()

        harness.register(username = "alice", attestation = AttestationConveyancePreference.DIRECT)

        assertEquals(AttestationType.BASIC, harness.credentials.single().attestationType)
    }

    @Test
    fun `none attestation is recorded as none`() {
        // Conveyance of "none" makes the client platform substitute a none-attestation statement,
        // which is what a browser does by default.
        val harness = CeremonyHarness()

        harness.register(username = "alice", attestation = AttestationConveyancePreference.NONE)

        assertEquals(AttestationType.NONE, harness.credentials.single().attestationType)
    }

    @Test
    fun `attestation type is not hardcoded across authenticators`() {
        val packed = CeremonyHarness()
        val none = CeremonyHarness()

        packed.register(username = "alice", attestation = AttestationConveyancePreference.DIRECT)
        none.register(username = "bob", attestation = AttestationConveyancePreference.NONE)

        assertEquals(AttestationType.BASIC, packed.credentials.single().attestationType)
        assertEquals(AttestationType.NONE, none.credentials.single().attestationType)
    }
}
