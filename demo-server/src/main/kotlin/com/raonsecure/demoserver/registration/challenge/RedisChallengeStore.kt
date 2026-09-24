package com.raonsecure.demoserver.registration.challenge

import com.raonsecure.demoserver.authentication.challenge.PendingAuthenticationContext
import tools.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisChallengeStore(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : ChallengeStore {

    override fun save(challenge: String, context: PendingRegistrationContext, ttl: Duration) {
        val key = "reg:challenge:$challenge"
        val value = objectMapper.writeValueAsString(context)
        redisTemplate.opsForValue().set(key, value, ttl)
    }

    override fun consume(challenge: String): PendingRegistrationContext? {
        val key = "reg:challenge:$challenge"
        val value = redisTemplate.opsForValue().getAndDelete(key) ?: return null
        return objectMapper.readValue(value, PendingRegistrationContext::class.java)
    }

    override fun saveAuthentication(challenge: String, context: PendingAuthenticationContext, ttl: Duration) {
        val key = "auth:challenge:$challenge"
        val value = objectMapper.writeValueAsString(context)
        redisTemplate.opsForValue().set(key, value, ttl)
    }

    override fun consumeAuthentication(challenge: String): PendingAuthenticationContext? {
        val key = "auth:challenge:$challenge"
        val value = redisTemplate.opsForValue().getAndDelete(key) ?: return null
        return objectMapper.readValue(value, PendingAuthenticationContext::class.java)
    }
}
