package com.raonsecure.demoserver.registration

import com.raonsecure.demoserver.common.data.RegistrationDataResponse
import com.raonsecure.demoserver.common.data.RegistrationOptionsRequest
import com.raonsecure.demoserver.registration.exception.ChallengeNotFoundException
import com.raonsecure.demoserver.registration.exception.DuplicateCredentialException
import com.raonsecure.demoserver.registration.exception.RegistrationVerificationException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import com.raonsecure.demoserver.common.data.PublicKeyCredentialRpEntity
import com.raonsecure.demoserver.common.data.PublicKeyCredentialUserEntity
import com.raonsecure.demoserver.common.data.PublicKeyCredentialParams

@WebMvcTest(controllers = [RegistrationController::class])
class RegistrationControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var registrationService: RegistrationService

    private val sampleOptions = RegistrationDataResponse(
        rp = PublicKeyCredentialRpEntity(id = "localhost", name = "Demo"),
        user = PublicKeyCredentialUserEntity(id = "dXNlcg", displayName = "User", name = "user"),
        challenge = "Y2hhbGxlbmdl",
        pubKeyCredParams = listOf(PublicKeyCredentialParams()),
    )

    @Test
    fun `options with valid username returns ok and challenge`() {
        whenever(registrationService.createOptions(any())).thenReturn(sampleOptions)

        mockMvc.post("/attestation/options") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"testuser"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ok") }
            jsonPath("$.options.challenge") { value("Y2hhbGxlbmdl") }
        }
    }

    @Test
    fun `options with blank username returns failed status`() {
        mockMvc.post("/attestation/options") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":""}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `result returns ok on successful verification`() {
        mockMvc.post("/attestation/result") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "id":"cred123","type":"public-key",
                "response":{"clientDataJSON":"abc","attestationObject":"def"}
            }"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }

    @Test
    fun `result returns failed when challenge not found`() {
        doThrow(ChallengeNotFoundException("abc")).whenever(registrationService).verify(any())

        mockMvc.post("/attestation/result") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "id":"cred123","type":"public-key",
                "response":{"clientDataJSON":"abc","attestationObject":"def"}
            }"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(false) }
        }
    }

    @Test
    fun `result returns failed on verification error`() {
        doThrow(RegistrationVerificationException("bad attestation"))
            .whenever(registrationService).verify(any())

        mockMvc.post("/attestation/result") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "id":"cred123","type":"public-key",
                "response":{"clientDataJSON":"abc","attestationObject":"def"}
            }"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(false) }
        }
    }

    @Test
    fun `result returns failed on duplicate credential`() {
        doThrow(DuplicateCredentialException("cred123"))
            .whenever(registrationService).verify(any())

        mockMvc.post("/attestation/result") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "id":"cred123","type":"public-key",
                "response":{"clientDataJSON":"abc","attestationObject":"def"}
            }"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(false) }
        }
    }
}
