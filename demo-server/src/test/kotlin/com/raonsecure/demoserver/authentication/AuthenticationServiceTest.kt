package com.raonsecure.demoserver.authentication

import com.raonsecure.demoserver.authentication.challenge.PendingAuthenticationContext
import com.raonsecure.demoserver.authentication.exception.AuthenticationChallengeNotFoundException
import com.raonsecure.demoserver.authentication.exception.CredentialNotFoundException
import com.raonsecure.demoserver.base.entity.AuthenticatorTransportEntity
import com.raonsecure.demoserver.base.entity.RpEntity
import com.raonsecure.demoserver.base.entity.UserKeyEntity
import com.raonsecure.demoserver.base.repository.UserKeyRepository
import com.raonsecure.demoserver.common.data.AuthenticationOptionsRequest
import com.raonsecure.demoserver.common.data.AuthenticatorAssertionResponseDto
import com.raonsecure.demoserver.common.data.PublicKeyCredentialGetResultDto
import com.raonsecure.demoserver.config.WebAuthnRpProperties
import com.raonsecure.demoserver.registration.challenge.ChallengeStore
import com.webauthn4j.WebAuthnManager
import com.webauthn4j.data.AuthenticationData
import com.webauthn4j.data.client.CollectedClientData
import com.webauthn4j.data.client.challenge.DefaultChallenge
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Base64

class AuthenticationServiceTest {
    private val rpProperties =
        WebAuthnRpProperties(
            id = "localhost",
            name = "Demo",
            origins = setOf("http://localhost:8080"),
        )
    private val userKeyRepository: UserKeyRepository = mock()
    private val challengeStore: ChallengeStore = mock()
    private val webAuthnManager: WebAuthnManager = mock()

    private val service =
        AuthenticationService(
            rpProperties,
            userKeyRepository,
            challengeStore,
            webAuthnManager,
        )

    private val rp = RpEntity(id = "localhost", name = "Demo")

    @Test
    fun `createOptions generates unique challenges`() {
        val existing =
            UserKeyEntity
                .builder()
                .rpEntity(rp)
                .userId("u1")
                .username("alice")
                .userDisplayName("Alice")
                .aaguid("")
                .credentialId("cred-123")
                .publicKey("key")
                .signatureAlgorithm(-7)
                .build()
        whenever(userKeyRepository.findAllByUsername("alice")).thenReturn(listOf(existing))

        val resp1 = service.createOptions(AuthenticationOptionsRequest(username = "alice"))
        val resp2 = service.createOptions(AuthenticationOptionsRequest(username = "alice"))

        assertNotEquals(resp1.challenge, resp2.challenge)
    }

    @Test
    fun `createOptions challenge is valid base64url`() {
        val existing =
            UserKeyEntity
                .builder()
                .rpEntity(rp)
                .userId("u1")
                .username("bob")
                .userDisplayName("Bob")
                .aaguid("")
                .credentialId("cred-123")
                .publicKey("key")
                .signatureAlgorithm(-7)
                .build()
        whenever(userKeyRepository.findAllByUsername("bob")).thenReturn(listOf(existing))

        val resp = service.createOptions(AuthenticationOptionsRequest(username = "bob"))

        val decoded = Base64.getUrlDecoder().decode(resp.challenge)
        assert(decoded.size >= 16) { "Challenge must be at least 16 bytes" }
    }

    @Test
    fun `createOptions populates allowCredentials with transports for known user`() {
        val cred =
            UserKeyEntity
                .builder()
                .rpEntity(rp)
                .userId("u1")
                .username("carol")
                .userDisplayName("Carol")
                .aaguid("")
                .credentialId("cred-abc")
                .publicKey("key")
                .signatureAlgorithm(-7)
                .transports(mutableListOf(AuthenticatorTransportEntity("internal")))
                .build()
        whenever(userKeyRepository.findAllByUsername("carol")).thenReturn(listOf(cred))

        val resp = service.createOptions(AuthenticationOptionsRequest(username = "carol"))

        assertNotNull(resp.allowCredentials)
        assertEquals(1, resp.allowCredentials!!.size)
        assertEquals("cred-abc", resp.allowCredentials!!.first().id)
        assertEquals(listOf("internal"), resp.allowCredentials!!.first().transports)
    }

    @Test
    fun `createOptions throws CredentialNotFoundException when user has no credentials`() {
        whenever(userKeyRepository.findAllByUsername("ghost")).thenReturn(emptyList())

        assertThrows<CredentialNotFoundException> {
            service.createOptions(AuthenticationOptionsRequest(username = "ghost"))
        }
    }

    @Test
    fun `createOptions supports userless request`() {
        val resp = service.createOptions(AuthenticationOptionsRequest(username = null))

        assertNull(resp.allowCredentials)
        val captor = argumentCaptor<PendingAuthenticationContext>()
        verify(challengeStore).saveAuthentication(any(), captor.capture(), any())
        assertNull(captor.firstValue.username)
    }

    @Test
    fun `verify throws AuthenticationChallengeNotFoundException when challenge not in store`() {
        val challengeBytes = "test-challenge-123".toByteArray()
        val challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes)

        val clientDataJson = """{"type":"webauthn.get","challenge":"$challengeBase64","origin":"http://localhost:8080"}"""
        val clientDataBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(clientDataJson.toByteArray())

        val mockAuthData: AuthenticationData = mock()
        val mockClientData: CollectedClientData = mock()
        whenever(mockClientData.challenge).thenReturn(DefaultChallenge(challengeBytes))
        whenever(mockAuthData.collectedClientData).thenReturn(mockClientData)
        whenever(webAuthnManager.parse(any<com.webauthn4j.data.AuthenticationRequest>())).thenReturn(mockAuthData)

        whenever(challengeStore.consumeAuthentication(challengeBase64)).thenReturn(null)

        val credId = Base64.getUrlEncoder().withoutPadding().encodeToString("cred-123".toByteArray())
        val req =
            PublicKeyCredentialGetResultDto(
                id = credId,
                response =
                    AuthenticatorAssertionResponseDto(
                        clientDataJSON = clientDataBase64,
                        authenticatorData = Base64.getUrlEncoder().encodeToString(ByteArray(37)),
                        signature = Base64.getUrlEncoder().encodeToString(ByteArray(64)),
                    ),
            )

        assertThrows<AuthenticationChallengeNotFoundException> {
            service.verify(req)
        }
    }
}
