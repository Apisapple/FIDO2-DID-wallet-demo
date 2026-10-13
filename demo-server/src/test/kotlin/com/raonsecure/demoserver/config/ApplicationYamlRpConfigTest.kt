package com.raonsecure.demoserver.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

/**
 * Binds the shipped `application.yml` so the `webauthn.rp.origins` list is proven to bind rather
 * than silently falling back to the property defaults.
 *
 * Uses [ApplicationContextRunner] against the real YAML rather than `@SpringBootTest`: a full
 * context would start the embedded Redis server, and an extra context that binds to port 6379
 * collides with the other context-loading tests.
 */
class ApplicationYamlRpConfigTest {

    @Configuration
    @EnableConfigurationProperties(WebAuthnRpProperties::class)
    class PropertiesOnlyConfig

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesOnlyConfig::class.java)
        .withInitializer(ConfigDataApplicationContextInitializer())

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
            // Proves the YAML list bound rather than the single-entry default.
            assertEquals(2, origins.size, "origins=$origins")
        }
    }
}
