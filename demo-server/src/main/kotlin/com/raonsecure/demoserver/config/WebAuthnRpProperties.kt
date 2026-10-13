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
    /**
     * Android applications permitted to act as clients for this relying party, published as a
     * Digital Asset Links statement list. Each entry's key hash must also appear in [origins].
     */
    val androidApps: List<AndroidApp> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "webauthn.rp.id must not be blank" }
        require(origins.isNotEmpty()) { "webauthn.rp.origins must not be empty" }
        require(origins.none { it.isBlank() }) { "webauthn.rp.origins must not contain a blank origin" }
    }

    data class AndroidApp(
        val packageName: String = "",
        val sha256CertFingerprints: List<String> = emptyList(),
    ) {
        init {
            require(packageName.isNotBlank()) {
                "webauthn.rp.android-apps[].package-name must not be blank"
            }
            require(sha256CertFingerprints.isNotEmpty()) {
                "webauthn.rp.android-apps[].sha256-cert-fingerprints must not be empty"
            }
            require(sha256CertFingerprints.none { it.isBlank() }) {
                "webauthn.rp.android-apps[].sha256-cert-fingerprints must not contain a blank fingerprint"
            }
        }
    }
}
