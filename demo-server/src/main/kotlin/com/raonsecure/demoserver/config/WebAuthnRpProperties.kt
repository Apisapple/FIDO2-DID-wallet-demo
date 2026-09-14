package com.raonsecure.demoserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "webauthn.rp")
data class WebAuthnRpProperties(
    val id: String = "localhost",
    val name: String = "Demo Server",
    val origin: String = "http://localhost:8080",
)
