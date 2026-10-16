package com.raonsecure.demoserver.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

/**
 * Binds the shipped `application-rp-defaults.yml` so the profile that carries the demo relying
 * party identity is proven to bind -- `WebAuthnRpProperties` has no defaults of its own, so a
 * profile that failed to bind would fail startup rather than degrade quietly.
 *
 * Uses [ApplicationContextRunner] against the real YAML rather than `@SpringBootTest`: a full
 * context would start the embedded Redis server, and an extra context that binds to port 6379
 * collides with the other context-loading tests.
 */
class ApplicationYamlRpConfigTest {
    @Configuration
    @EnableConfigurationProperties(WebAuthnRpProperties::class)
    class PropertiesOnlyConfig

    private val runner =
        ApplicationContextRunner()
            .withUserConfiguration(PropertiesOnlyConfig::class.java)
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.profiles.active=rp-defaults")

    @Test
    fun `shipped configuration binds the relying party identity`() {
        runner.run { context ->
            val properties = context.getBean(WebAuthnRpProperties::class.java)
            assertEquals("localhost", properties.id)
            assertEquals("FIDO2 Demo Server", properties.name)
        }
    }

    @Test
    fun `shipped configuration binds both a web and an android origin`() {
        runner.run { context ->
            val origins = context.getBean(WebAuthnRpProperties::class.java).origins
            assertTrue(origins.contains("http://localhost:8080"), "origins=$origins")
            assertTrue(
                origins.any { it.startsWith("android:apk-key-hash:") },
                "origins=$origins",
            )
            assertEquals(2, origins.size, "origins=$origins")
        }
    }

    /**
     * The reason the identity lives in a profile rather than in `application.yml`: the base
     * configuration alone must not start, so a deployment that states no relying party is
     * rejected instead of inheriting the demo one.
     */
    @Test
    fun `base configuration without the profile fails startup`() {
        ApplicationContextRunner()
            .withUserConfiguration(PropertiesOnlyConfig::class.java)
            .withInitializer(ConfigDataApplicationContextInitializer())
            .run { context ->
                val failure = context.startupFailure
                assertTrue(failure != null, "context should fail without the rp-defaults profile")
                assertTrue(
                    failure!!.stackTraceToString().contains("webauthn.rp.id must be configured"),
                    failure.stackTraceToString().take(500),
                )
            }
    }
}
