package com.raonsecure.demoserver.registration.challenge

import java.time.Duration

interface ChallengeStore {
    fun save(challenge: String, context: PendingRegistrationContext, ttl: Duration)
    fun consume(challenge: String): PendingRegistrationContext?
}
