package com.raonsecure.demoserver.registration

import com.raonsecure.demoserver.base.entity.RpEntity
import com.raonsecure.demoserver.base.entity.UserKeyEntity
import com.raonsecure.demoserver.base.repository.RpRepository
import com.raonsecure.demoserver.base.repository.UserKeyRepository
import com.raonsecure.demoserver.common.data.RegistrationOptionsRequest
import com.raonsecure.demoserver.config.WebAuthnRpProperties
import com.raonsecure.demoserver.registration.challenge.ChallengeStore
import com.raonsecure.demoserver.registration.challenge.PendingRegistrationContext
import com.webauthn4j.WebAuthnManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.Base64

class RegistrationServiceTest {

    private val rpProperties = WebAuthnRpProperties(
        id = "localhost",
        name = "Demo",
        origin = "http://localhost:8080",
    )
    private val rpRepository: RpRepository = mock()
    private val userKeyRepository: UserKeyRepository = mock()
    private val challengeStore: ChallengeStore = mock()
    private val webAuthnManager: WebAuthnManager = mock()

    private val service = RegistrationService(
        rpProperties, rpRepository, userKeyRepository, challengeStore, webAuthnManager
    )

    private val rp = RpEntity(id = "localhost", name = "Demo")

    @Test
    fun `createOptions generates unique challenges`() {
        whenever(rpRepository.findById("localhost")).thenReturn(Optional.of(rp))
        whenever(userKeyRepository.findAllByUsername(any())).thenReturn(emptyList())

        val resp1 = service.createOptions(RegistrationOptionsRequest(username = "alice"))
        val resp2 = service.createOptions(RegistrationOptionsRequest(username = "alice"))

        assertNotEquals(resp1.challenge, resp2.challenge)
    }

    @Test
    fun `createOptions challenge is valid base64url`() {
        whenever(rpRepository.findById("localhost")).thenReturn(Optional.of(rp))
        whenever(userKeyRepository.findAllByUsername(any())).thenReturn(emptyList())

        val resp = service.createOptions(RegistrationOptionsRequest(username = "bob"))

        val decoded = Base64.getUrlDecoder().decode(resp.challenge)
        assert(decoded.size >= 16) { "Challenge must be at least 16 bytes" }
    }

    @Test
    fun `createOptions includes excludeCredentials for existing user`() {
        val existing = UserKeyEntity.builder()
            .rpEntity(rp).userId("u1").username("carol").userDisplayName("Carol")
            .aaguid("").credentialId("cred-abc").publicKey("key").signatureAlgorithm(-7)
            .build()
        whenever(rpRepository.findById("localhost")).thenReturn(Optional.of(rp))
        whenever(userKeyRepository.findAllByUsername("carol")).thenReturn(listOf(existing))

        val resp = service.createOptions(RegistrationOptionsRequest(username = "carol"))

        assertNotNull(resp.excludeCredentials)
        assertEquals("cred-abc", resp.excludeCredentials!!.first().id)
    }

    @Test
    fun `createOptions saves challenge to store`() {
        whenever(rpRepository.findById("localhost")).thenReturn(Optional.of(rp))
        whenever(userKeyRepository.findAllByUsername(any())).thenReturn(emptyList())

        service.createOptions(RegistrationOptionsRequest(username = "dave"))

        val captor = argumentCaptor<PendingRegistrationContext>()
        verify(challengeStore).save(any(), captor.capture(), any())
        assertEquals("dave", captor.firstValue.username)
    }

    @Test
    fun `createOptions returns no excludeCredentials for new user`() {
        whenever(rpRepository.findById("localhost")).thenReturn(Optional.of(rp))
        whenever(userKeyRepository.findAllByUsername(any())).thenReturn(emptyList())

        val resp = service.createOptions(RegistrationOptionsRequest(username = "newuser"))

        assertNull(resp.excludeCredentials)
    }
}
