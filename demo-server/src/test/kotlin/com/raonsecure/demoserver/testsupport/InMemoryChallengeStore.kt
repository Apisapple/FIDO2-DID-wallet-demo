package com.raonsecure.demoserver.testsupport

import com.raonsecure.demoserver.authentication.challenge.PendingAuthenticationContext
import com.raonsecure.demoserver.registration.challenge.ChallengeStore
import com.raonsecure.demoserver.registration.challenge.PendingRegistrationContext
import java.time.Duration

/**
 * Single-use in-memory [ChallengeStore], so ceremony tests exercise the real consume-once
 * behaviour without needing Redis.
 */
class InMemoryChallengeStore : ChallengeStore {
    private val registrations = mutableMapOf<String, PendingRegistrationContext>()
    private val authentications = mutableMapOf<String, PendingAuthenticationContext>()

    override fun save(challenge: String, context: PendingRegistrationContext, ttl: Duration) {
        registrations[challenge] = context
    }

    override fun consume(challenge: String): PendingRegistrationContext? = registrations.remove(challenge)

    override fun saveAuthentication(challenge: String, context: PendingAuthenticationContext, ttl: Duration) {
        authentications[challenge] = context
    }

    override fun consumeAuthentication(challenge: String): PendingAuthenticationContext? =
        authentications.remove(challenge)

    fun savedRegistration(challenge: String): PendingRegistrationContext? = registrations[challenge]

    fun savedAuthentication(challenge: String): PendingAuthenticationContext? = authentications[challenge]
}
