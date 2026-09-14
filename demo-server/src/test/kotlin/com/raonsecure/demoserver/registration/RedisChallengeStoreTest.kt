package com.raonsecure.demoserver.registration

import com.raonsecure.demoserver.registration.challenge.PendingRegistrationContext
import com.raonsecure.demoserver.registration.challenge.RedisChallengeStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Duration

@SpringBootTest
class RedisChallengeStoreTest {

    @Autowired
    lateinit var challengeStore: RedisChallengeStore

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

    @Test
    fun `unknown challenge returns null`() {
        val result = challengeStore.consume("nonexistent-challenge")
        assertNull(result)
    }
}
