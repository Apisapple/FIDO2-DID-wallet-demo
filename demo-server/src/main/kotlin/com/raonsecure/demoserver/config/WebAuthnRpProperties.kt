package com.raonsecure.demoserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "webauthn.rp")
data class WebAuthnRpProperties(
    val id: String = "localhost",
    val name: String = "Demo Server",
    /**
     * Client origins accepted during verification. Holds web origins and native Android
     * origins (`android:apk-key-hash:<base64url-sha256>`) so one instance can serve both.
     */
    val origins: Set<String> = setOf("http://localhost:8080"),
) {
    init {
        require(id.isNotBlank()) { "webauthn.rp.id must not be blank" }
        require(origins.isNotEmpty()) { "webauthn.rp.origins must not be empty" }
        require(origins.none { it.isBlank() }) { "webauthn.rp.origins must not contain a blank origin" }
    }
}
