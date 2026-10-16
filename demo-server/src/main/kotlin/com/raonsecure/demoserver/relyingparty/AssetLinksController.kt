package com.raonsecure.demoserver.relyingparty

import com.fasterxml.jackson.annotation.JsonProperty
import com.raonsecure.demoserver.config.WebAuthnRpProperties
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Serves the Digital Asset Links statement list that associates the configured Android
 * applications with this relying party. The Android platform fetches this anonymously, so the
 * endpoint takes no credential.
 */
@RestController
class AssetLinksController(
    private val rpProperties: WebAuthnRpProperties,
) {
    @GetMapping("/.well-known/assetlinks.json", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun assetLinks(): ResponseEntity<List<AssetLinkStatement>> =
        ResponseEntity.ok(
            rpProperties.androidApps.map { app ->
                AssetLinkStatement(
                    relation = listOf(LOGIN_CREDS_RELATION),
                    target =
                        AssetLinkTarget(
                            packageName = app.packageName,
                            sha256CertFingerprints = app.sha256CertFingerprints,
                        ),
                )
            },
        )

    companion object {
        const val LOGIN_CREDS_RELATION = "delegate_permission/common.get_login_creds"
    }
}

data class AssetLinkStatement(
    val relation: List<String>,
    val target: AssetLinkTarget,
)

/**
 * Field names follow the Digital Asset Links wire format, which is snake_case; the Android
 * platform will not match a statement whose keys are camelCase.
 */
data class AssetLinkTarget(
    @JsonProperty("package_name")
    val packageName: String,
    @JsonProperty("sha256_cert_fingerprints")
    val sha256CertFingerprints: List<String>,
    val namespace: String = "android_app",
)
