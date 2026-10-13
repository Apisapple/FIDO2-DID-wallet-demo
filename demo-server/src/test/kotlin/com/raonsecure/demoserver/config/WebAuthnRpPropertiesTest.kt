package com.raonsecure.demoserver.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

/**
 * Binds `webauthn.rp.*` through Spring so a misconfiguration fails at startup rather than at the
 * first ceremony.
 */
class WebAuthnRpPropertiesTest {

    @Configuration
    @EnableAutoConfiguration(exclude = [])
    @EnableConfigurationProperties(WebAuthnRpProperties::class)
    class PropertiesOnlyConfig

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesOnlyConfig::class.java)

    @Test
    fun `valid configuration binds both web and android origins`() {
        runner.withPropertyValues(
            "webauthn.rp.id=localhost",
            "webauthn.rp.name=Demo",
            "webauthn.rp.origins[0]=http://localhost:8080",
            "webauthn.rp.origins[1]=android:apk-key-hash:abc123",
        ).run { context ->
            assertTrue(context.startupFailure == null, "context should start")
            val properties = context.getBean(WebAuthnRpProperties::class.java)
            assertEquals("localhost", properties.id)
            assertEquals(
                setOf("http://localhost:8080", "android:apk-key-hash:abc123"),
                properties.origins,
            )
        }
    }

    @Test
    fun `android apps bind when configured`() {
        runner.withPropertyValues(
            "webauthn.rp.id=localhost",
            "webauthn.rp.origins[0]=http://localhost:8080",
            "webauthn.rp.android-apps[0].package-name=com.raonsecure.wallet",
            "webauthn.rp.android-apps[0].sha256-cert-fingerprints[0]=AA:BB:CC",
            "webauthn.rp.android-apps[0].sha256-cert-fingerprints[1]=DD:EE:FF",
            "webauthn.rp.android-apps[1].package-name=com.raonsecure.wallet.debug",
            "webauthn.rp.android-apps[1].sha256-cert-fingerprints[0]=11:22:33",
        ).run { context ->
            assertTrue(context.startupFailure == null, "context should start")
            val apps = context.getBean(WebAuthnRpProperties::class.java).androidApps
            assertEquals(2, apps.size)
            assertEquals("com.raonsecure.wallet", apps[0].packageName)
            assertEquals(listOf("AA:BB:CC", "DD:EE:FF"), apps[0].sha256CertFingerprints)
            assertEquals("com.raonsecure.wallet.debug", apps[1].packageName)
        }
    }

    @Test
    fun `android apps default to empty when absent`() {
        runner.withPropertyValues(
            "webauthn.rp.id=localhost",
            "webauthn.rp.origins[0]=http://localhost:8080",
        ).run { context ->
            assertTrue(context.startupFailure == null, "context should start")
            assertTrue(context.getBean(WebAuthnRpProperties::class.java).androidApps.isEmpty())
        }
    }

    @Test
    fun `android app without a fingerprint fails startup`() {
        runner.withPropertyValues(
            "webauthn.rp.id=localhost",
            "webauthn.rp.origins[0]=http://localhost:8080",
            "webauthn.rp.android-apps[0].package-name=com.raonsecure.wallet",
        ).run { context ->
            val failure = context.startupFailure
            assertTrue(failure != null, "context should fail to start")
            assertTrue(
                failure!!.stackTraceToString().contains("sha256-cert-fingerprints must not be empty"),
                failure.stackTraceToString().take(500),
            )
        }
    }

    @Test
    fun `blank relying party id fails startup`() {
        runner.withPropertyValues(
            "webauthn.rp.id=",
            "webauthn.rp.origins[0]=http://localhost:8080",
        ).run { context ->
            val failure = context.startupFailure
            assertTrue(failure != null, "context should fail to start")
            assertTrue(
                failure!!.stackTraceToString().contains("webauthn.rp.id must not be blank"),
                failure.stackTraceToString().take(500),
            )
        }
    }

    @Test
    fun `empty origin set fails startup`() {
        runner.withPropertyValues(
            "webauthn.rp.id=localhost",
            "webauthn.rp.origins=",
        ).run { context ->
            val failure = context.startupFailure
            assertTrue(failure != null, "context should fail to start")
            assertTrue(
                failure!!.stackTraceToString().contains("webauthn.rp.origins must not be empty"),
                failure.stackTraceToString().take(500),
            )
        }
    }

    @Test
    fun `blank origin inside the set fails startup`() {
        runner.withPropertyValues(
            "webauthn.rp.id=localhost",
            "webauthn.rp.origins[0]=",
        ).run { context ->
            val failure = context.startupFailure
            assertTrue(failure != null, "context should fail to start")
            assertTrue(
                failure!!.stackTraceToString()
                    .contains("webauthn.rp.origins must not contain a blank origin"),
                failure.stackTraceToString().take(500),
            )
        }
    }
}
