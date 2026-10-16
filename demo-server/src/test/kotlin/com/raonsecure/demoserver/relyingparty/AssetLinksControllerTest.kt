package com.raonsecure.demoserver.relyingparty

import com.raonsecure.demoserver.config.WebAuthnRpProperties
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(controllers = [AssetLinksController::class])
class AssetLinksControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var rpProperties: WebAuthnRpProperties

    @Test
    fun `serves a statement per configured android app`() {
        whenever(rpProperties.androidApps).thenReturn(
            listOf(
                WebAuthnRpProperties.AndroidApp(
                    packageName = "com.raonsecure.wallet",
                    sha256CertFingerprints = listOf("AA:BB:CC", "DD:EE:FF"),
                ),
                WebAuthnRpProperties.AndroidApp(
                    packageName = "com.raonsecure.wallet.debug",
                    sha256CertFingerprints = listOf("11:22:33"),
                ),
            ),
        )

        mockMvc.get("/.well-known/assetlinks.json").andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].relation[0]") { value("delegate_permission/common.get_login_creds") }
            jsonPath("$[0].target.namespace") { value("android_app") }
            jsonPath("$[0].target.package_name") { value("com.raonsecure.wallet") }
            jsonPath("$[0].target.sha256_cert_fingerprints[0]") { value("AA:BB:CC") }
            jsonPath("$[0].target.sha256_cert_fingerprints[1]") { value("DD:EE:FF") }
            jsonPath("$[1].relation[0]") { value("delegate_permission/common.get_login_creds") }
            jsonPath("$[1].target.package_name") { value("com.raonsecure.wallet.debug") }
        }
    }

    @Test
    fun `serves an empty statement list when no android app is configured`() {
        whenever(rpProperties.androidApps).thenReturn(emptyList())

        mockMvc.get("/.well-known/assetlinks.json").andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `requires no authentication`() {
        whenever(rpProperties.androidApps).thenReturn(emptyList())

        // No credential, no session, no auth header -- the Android platform fetches anonymously.
        mockMvc.get("/.well-known/assetlinks.json").andExpect {
            status { isOk() }
        }
    }
}
