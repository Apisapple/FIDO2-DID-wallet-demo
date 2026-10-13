package com.raonsecure.demoserver.config

import com.webauthn4j.WebAuthnManager
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(WebAuthnRpProperties::class)
class WebAuthnConfig {

    @Bean
    fun webAuthnManager(): WebAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager()
}
