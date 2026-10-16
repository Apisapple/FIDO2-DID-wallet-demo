package com.raonsecure.demoserver.ceremony

import com.raonsecure.demoserver.authentication.challenge.PendingAuthenticationContext
import com.raonsecure.demoserver.base.entity.RpEntity
import com.raonsecure.demoserver.base.entity.UserKeyEntity
import com.raonsecure.demoserver.registration.challenge.PendingRegistrationContext
import com.raonsecure.demoserver.testsupport.CeremonyFixture
import com.raonsecure.demoserver.testsupport.InMemoryChallengeStore
import com.raonsecure.demoserver.testsupport.fakeUserKeyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Base64

/** Covers the ceremony test doubles themselves, so a broken fixture is not mistaken for a broken server. */
class CeremonyFixtureTest {
    private val urlDecoder = Base64.getUrlDecoder()

    @Test
    fun `fixture produces a complete attestation response`() {
        val fixture = CeremonyFixture()
        val challenge = fixture.newChallenge()

        val registered = fixture.register(challenge = challenge, username = "alice")

        assertTrue(registered.credentialId.isNotBlank())
        assertTrue(registered.clientDataJSON.isNotBlank())
        assertTrue(registered.attestationObject.isNotBlank())
        assertTrue(urlDecoder.decode(registered.credentialId).isNotEmpty())
        assertTrue(urlDecoder.decode(registered.attestationObject).isNotEmpty())

        val clientData = String(urlDecoder.decode(registered.clientDataJSON))
        assertTrue(clientData.contains("webauthn.create"), clientData)
        assertTrue(clientData.contains(challenge), clientData)
    }

    @Test
    fun `fixture produces a complete assertion response`() {
        val fixture = CeremonyFixture()
        val registered = fixture.register(challenge = fixture.newChallenge(), username = "bob")
        val challenge = fixture.newChallenge()

        val asserted = fixture.authenticate(challenge = challenge, credentialId = registered.credentialId)

        assertEquals(registered.credentialId, asserted.credentialId)
        assertTrue(asserted.authenticatorData.isNotBlank())
        assertTrue(asserted.signature.isNotBlank())
        assertTrue(urlDecoder.decode(asserted.signature).isNotEmpty())

        val clientData = String(urlDecoder.decode(asserted.clientDataJSON))
        assertTrue(clientData.contains("webauthn.get"), clientData)
        assertTrue(clientData.contains(challenge), clientData)
    }

    @Test
    fun `in-memory store hands back a saved context exactly once`() {
        val store = InMemoryChallengeStore()
        val ttl = Duration.ofSeconds(30)

        store.save("reg", PendingRegistrationContext("alice", "handle", "Alice", "reg"), ttl)
        assertNotNull(store.consume("reg"))
        assertNull(store.consume("reg"))

        store.saveAuthentication("auth", PendingAuthenticationContext("alice", "auth", listOf("cred")), ttl)
        assertNotNull(store.consumeAuthentication("auth"))
        assertNull(store.consumeAuthentication("auth"))
    }

    @Test
    fun `in-memory store returns null for an unknown challenge`() {
        val store = InMemoryChallengeStore()
        assertNull(store.consume("missing"))
        assertNull(store.consumeAuthentication("missing"))
    }

    @Test
    fun `repository fake reads back what it saved`() {
        val rp = RpEntity(id = "localhost", name = "Fixture RP")
        val repository = fakeUserKeyRepository()

        val saved =
            UserKeyEntity
                .builder()
                .rpEntity(rp)
                .userId("handle-1")
                .username("alice")
                .userDisplayName("Alice")
                .aaguid("")
                .credentialId("cred-1")
                .publicKey("key")
                .signatureAlgorithm(-7)
                .build()
        repository.save(saved)

        assertEquals("alice", repository.findByCredentialId("cred-1")?.username)
        assertEquals(listOf("cred-1"), repository.findAllByUsername("alice").map { it.credentialId })
        assertEquals(listOf("cred-1"), repository.findAllByUserId("handle-1").map { it.credentialId })
        assertNull(repository.findByCredentialId("cred-absent"))
        assertTrue(repository.findAllByUsername("nobody").isEmpty())
    }

    @Test
    fun `repository fake replaces a credential saved twice`() {
        val rp = RpEntity(id = "localhost", name = "Fixture RP")
        val repository = fakeUserKeyRepository()

        repeat(2) {
            repository.save(
                UserKeyEntity
                    .builder()
                    .rpEntity(rp)
                    .userId("handle-1")
                    .username("alice")
                    .userDisplayName("Alice")
                    .aaguid("")
                    .credentialId("cred-1")
                    .publicKey("key")
                    .signatureAlgorithm(-7)
                    .signCounter(it.toLong())
                    .build(),
            )
        }

        assertEquals(1, repository.findAllByUsername("alice").size)
        assertEquals(1L, repository.findByCredentialId("cred-1")?.signCounter)
    }
}
