package com.raonsecure.demoserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * A relying party identity must be stated, not inherited. The constructor parameters are nullable
 * and carry no default so that configuration which is absent *entirely* is rejected by name here,
 * rather than binding to a fallback that then satisfies the checks below; the exposed properties
 * are non-null, so callers see a validated identity or the context never starts.
 */
@ConfigurationProperties(prefix = "webauthn.rp")
class WebAuthnRpProperties(
    id: String?,
    name: String?,
    origins: Set<String>?,
    /**
     * Android applications permitted to act as clients for this relying party, published as a
     * Digital Asset Links statement list. Each entry's key hash must also appear in [origins].
     */
    val androidApps: List<AndroidApp> = emptyList(),
) {
    val id: String = requireNotNull(id) { "webauthn.rp.id must be configured" }

    val name: String = requireNotNull(name) { "webauthn.rp.name must be configured" }

    /**
     * Client origins accepted during verification. Holds web origins and native Android
     * origins (`android:apk-key-hash:<base64url-sha256>`) so one instance can serve both.
     */
    val origins: Set<String> = requireNotNull(origins) { "webauthn.rp.origins must be configured" }

    init {
        require(this.id.isNotBlank()) { "webauthn.rp.id must not be blank" }
        require(this.name.isNotBlank()) { "webauthn.rp.name must not be blank" }
        require(this.origins.isNotEmpty()) { "webauthn.rp.origins must not be empty" }
        require(this.origins.none { it.isBlank() }) {
            "webauthn.rp.origins must not contain a blank origin"
        }
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
