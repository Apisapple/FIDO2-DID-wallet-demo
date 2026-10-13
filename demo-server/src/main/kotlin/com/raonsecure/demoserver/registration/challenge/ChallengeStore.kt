package com.raonsecure.demoserver.registration.challenge

import com.raonsecure.demoserver.authentication.challenge.PendingAuthenticationContext
import java.time.Duration

interface ChallengeStore {
    fun save(challenge: String, context: PendingRegistrationContext, ttl: Duration)
    fun consume(challenge: String): PendingRegistrationContext?

    fun saveAuthentication(challenge: String, context: PendingAuthenticationContext, ttl: Duration)
    fun consumeAuthentication(challenge: String): PendingAuthenticationContext?
}
