package com.raonsecure.demoserver.registration

import com.raonsecure.demoserver.authentication.challenge.PendingAuthenticationContext
import com.raonsecure.demoserver.registration.challenge.PendingRegistrationContext
import com.raonsecure.demoserver.base.entity.UserKeyEntity
import com.raonsecure.demoserver.base.repository.RpRepository
import com.raonsecure.demoserver.base.repository.UserKeyRepository
import com.raonsecure.demoserver.config.WebAuthnRpProperties
import com.raonsecure.demoserver.registration.challenge.RedisChallengeStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.Date

@SpringBootTest
@ActiveProfiles("rp-defaults")
class RedisChallengeStoreTest {

    @Autowired
    lateinit var challengeStore: RedisChallengeStore

    @Autowired
    lateinit var rpRepository: RpRepository

    @Autowired
    lateinit var rpProperties: WebAuthnRpProperties

    @Autowired
    lateinit var userKeyRepository: UserKeyRepository

    @Test
    fun `saved challenge is retrievable`() {
        val challenge = "test-challenge-${System.nanoTime()}"
        val ctx = PendingRegistrationContext("user", "userId", "User", challenge)

        challengeStore.save(challenge, ctx, Duration.ofSeconds(30))
        val result = challengeStore.consume(challenge)

        assertNotNull(result)
        assertEquals("user", result!!.username)
    }

    @Test
    fun `challenge is consumed and not available again`() {
        val challenge = "single-use-${System.nanoTime()}"
        val ctx = PendingRegistrationContext("user2", "userId2", "User2", challenge)

        challengeStore.save(challenge, ctx, Duration.ofSeconds(30))
        val first = challengeStore.consume(challenge)
        val second = challengeStore.consume(challenge)

        assertNotNull(first)
        assertNull(second)
    }

    /**
     * Folded into this class rather than given its own `@SpringBootTest`: an extra full context
     * starts a second embedded Redis on the same port and the two collide.
     */
    @Test
    fun `a relying party record exists for the configured id after startup`() {
        assertTrue(rpRepository.existsById(rpProperties.id), "no RP record for ${rpProperties.id}")

        val rp = rpRepository.findById(rpProperties.id).orElseThrow()
        assertEquals(rpProperties.id, rp.id)
        assertEquals(rpProperties.name, rp.name)
    }

    /**
     * Persists a credential through real JPA. The ceremony tests use a repository fake, so a
     * column too narrow for a real credential ID passed every one of them while failing with
     * HTTP 500 against the running server. WebAuthn allows credential IDs up to 1023 bytes --
     * about 1364 base64url characters -- and some authenticators pack state into the ID rather
     * than issuing a short handle (webauthn4j's emulator produces ~307 characters).
     *
     * Lives in this class because an extra full context would start a competing embedded Redis.
     */
    @Test
    fun `a credential id the length of a real authenticator's persists`() {
        for (length in listOf(22, 307, 1364)) {
            val credentialId = "c".repeat(length) + System.nanoTime()
            val rp = rpRepository.findById(rpProperties.id).orElseThrow()

            userKeyRepository.save(
                UserKeyEntity.builder()
                    .rpEntity(rp)
                    .userId("a".repeat(43))
                    .username("persist-test")
                    .userDisplayName("Persist Test")
                    .aaguid("00000000-0000-0000-0000-000000000000")
                    .credentialId(credentialId)
                    .publicKey("k".repeat(400))
                    .signatureAlgorithm(-7)
                    .signCounter(0L)
                    .registeredTimestamp(Date())
                    .build(),
            )

            assertEquals(
                credentialId,
                userKeyRepository.findByCredentialId(credentialId)?.credentialId,
                "a $length-character credential id should round-trip",
            )
        }
    }

    @Test
    fun `unknown challenge returns null`() {
        val result = challengeStore.consume("nonexistent-challenge")
        assertNull(result)
    }

    @Test
    fun `saved authentication challenge is retrievable and single-use`() {
        val challenge = "auth-challenge-${System.nanoTime()}"
        val ctx = PendingAuthenticationContext("authUser", challenge, listOf("cred1", "cred2"))

        challengeStore.saveAuthentication(challenge, ctx, Duration.ofSeconds(30))
        val result = challengeStore.consumeAuthentication(challenge)

        assertNotNull(result)
        assertEquals("authUser", result!!.username)
        assertEquals(listOf("cred1", "cred2"), result.credentialIds)

        val second = challengeStore.consumeAuthentication(challenge)
        assertNull(second)
    }

    @Test
    fun `registration context round-trips the advertised user verification`() {
        val challenge = "reg-uv-${System.nanoTime()}"
        val ctx = PendingRegistrationContext("user", "userId", "User", challenge, "required")

        challengeStore.save(challenge, ctx, Duration.ofSeconds(30))
        val result = challengeStore.consume(challenge)

        assertNotNull(result)
        assertEquals("required", result!!.userVerification)
    }

    @Test
    fun `authentication context round-trips the advertised user verification`() {
        val challenge = "auth-uv-${System.nanoTime()}"
        val ctx = PendingAuthenticationContext("user", challenge, listOf("cred1"), "required")

        challengeStore.saveAuthentication(challenge, ctx, Duration.ofSeconds(30))
        val result = challengeStore.consumeAuthentication(challenge)

        assertNotNull(result)
        assertEquals("required", result!!.userVerification)
    }

    @Test
    fun `contexts default to preferred when the field is absent`() {
        val regChallenge = "reg-default-${System.nanoTime()}"
        challengeStore.save(
            regChallenge,
            PendingRegistrationContext("user", "userId", "User", regChallenge),
            Duration.ofSeconds(30),
        )
        assertEquals("preferred", challengeStore.consume(regChallenge)!!.userVerification)

        val authChallenge = "auth-default-${System.nanoTime()}"
        challengeStore.saveAuthentication(
            authChallenge,
            PendingAuthenticationContext("user", authChallenge),
            Duration.ofSeconds(30),
        )
        assertEquals("preferred", challengeStore.consumeAuthentication(authChallenge)!!.userVerification)
    }

    @Test
    fun `unknown authentication challenge returns null`() {
        val result = challengeStore.consumeAuthentication("nonexistent-auth-challenge")
        assertNull(result)
    }
}
