package com.raonsecure.demoserver.registration

import com.raonsecure.demoserver.authentication.challenge.PendingAuthenticationContext
import com.raonsecure.demoserver.registration.challenge.PendingRegistrationContext
import com.raonsecure.demoserver.base.repository.RpRepository
import com.raonsecure.demoserver.config.WebAuthnRpProperties
import com.raonsecure.demoserver.registration.challenge.RedisChallengeStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Duration

@SpringBootTest
class RedisChallengeStoreTest {

    @Autowired
    lateinit var challengeStore: RedisChallengeStore

    @Autowired
    lateinit var rpRepository: RpRepository

    @Autowired
    lateinit var rpProperties: WebAuthnRpProperties

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
