package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.base.entity.AttestationType
import com.raonsecure.demoserver.base.repository.RpRepository
import com.raonsecure.demoserver.config.WebAuthnRpProperties
import com.raonsecure.demoserver.registration.RegistrationService
import com.raonsecure.demoserver.testsupport.InMemoryChallengeStore
import com.raonsecure.demoserver.testsupport.fakeUserKeyRepository
import com.webauthn4j.WebAuthnManager
import com.webauthn4j.data.RegistrationData
import com.webauthn4j.data.attestation.AttestationObject
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement
import com.webauthn4j.data.attestation.statement.PackedAttestationStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Covers the statement-to-[AttestationType] mapping directly.
 *
 * Self attestation is a packed statement whose x5c is null. The emulator's PackedAuthenticator
 * always attaches a certificate path, so that case is unreachable end to end and is pinned here
 * instead; the basic and none cases are additionally covered through a real ceremony in
 * [AttestationTypeTest].
 */
class AttestationTypeMappingTest {

    private val service = RegistrationService(
        WebAuthnRpProperties(id = "localhost", name = "Fixture RP"),
        mock<RpRepository>(),
        fakeUserKeyRepository(),
        InMemoryChallengeStore(),
        mock<WebAuthnManager>(),
    )

    /** Calls the private mapping through its only observable contract: the persisted value. */
    private fun mapOf(attestationObject: AttestationObject?): AttestationType {
        val data: RegistrationData = mock()
        whenever(data.attestationObject).thenReturn(attestationObject)

        val method = RegistrationService::class.java
            .getDeclaredMethod("resolveAttestationType", RegistrationData::class.java)
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        return method.invoke(service, data) as AttestationType
    }

    @Test
    fun `packed statement without a certificate chain is self attestation`() {
        val statement: PackedAttestationStatement = mock()
        whenever(statement.x5c).thenReturn(null)
        val attestationObject: AttestationObject = mock()
        whenever(attestationObject.attestationStatement).thenReturn(statement)

        assertEquals(AttestationType.SELF, mapOf(attestationObject))
    }

    @Test
    fun `packed statement with a certificate chain is basic attestation`() {
        val statement: PackedAttestationStatement = mock()
        whenever(statement.x5c).thenReturn(mock())
        val attestationObject: AttestationObject = mock()
        whenever(attestationObject.attestationStatement).thenReturn(statement)

        assertEquals(AttestationType.BASIC, mapOf(attestationObject))
    }

    @Test
    fun `none statement is none attestation`() {
        val attestationObject: AttestationObject = mock()
        whenever(attestationObject.attestationStatement).thenReturn(NoneAttestationStatement())

        assertEquals(AttestationType.NONE, mapOf(attestationObject))
    }

    @Test
    fun `a missing attestation object is none attestation`() {
        assertEquals(AttestationType.NONE, mapOf(null))
    }
}
