package com.raonsecure.demoserver.authentication

import com.raonsecure.demoserver.authentication.exception.AuthenticationChallengeNotFoundException
import com.raonsecure.demoserver.authentication.exception.AuthenticationVerificationException
import com.raonsecure.demoserver.authentication.exception.CredentialNotFoundException
import com.raonsecure.demoserver.common.data.AuthenticationDataResponse
import com.raonsecure.demoserver.common.data.PublicKeyCredentialDescriptor
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

@WebMvcTest(controllers = [AuthenticationController::class])
class AuthenticationControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var authenticationService: AuthenticationService

    private val sampleOptions =
        AuthenticationDataResponse(
            challenge = "Y2hhbGxlbmdl",
            rpId = "localhost",
            allowCredentials = listOf(PublicKeyCredentialDescriptor(id = "cred123")),
        )

    @Test
    fun `options with valid username returns ok and challenge`() {
        whenever(authenticationService.createOptions(any())).thenReturn(sampleOptions)

        mockMvc
            .post("/assertion/options") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"username":"testuser"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.status") { value("ok") }
                jsonPath("$.options.challenge") { value("Y2hhbGxlbmdl") }
                jsonPath("$.options.allowCredentials[0].id") { value("cred123") }
            }
    }

    @Test
    fun `options without username returns ok for resident key flow`() {
        val userlessOptions =
            AuthenticationDataResponse(
                challenge = "Y2hhbGxlbmdl",
                rpId = "localhost",
                allowCredentials = null,
            )
        whenever(authenticationService.createOptions(any())).thenReturn(userlessOptions)

        mockMvc
            .post("/assertion/options") {
                contentType = MediaType.APPLICATION_JSON
                content = """{}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.status") { value("ok") }
                jsonPath("$.options.challenge") { value("Y2hhbGxlbmdl") }
            }
    }

    @Test
    fun `options for non-existent user returns failed status`() {
        doThrow(CredentialNotFoundException("No credentials registered for user: non_existent"))
            .whenever(authenticationService)
            .createOptions(any())

        mockMvc
            .post("/assertion/options") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"username":"non_existent"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.message") { value("Credential not found: No credentials registered for user: non_existent") }
            }
    }

    @Test
    fun `result returns ok on successful verification`() {
        mockMvc
            .post("/assertion/result") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                "id":"cred123","type":"public-key",
                "response":{
                    "clientDataJSON":"abc",
                    "authenticatorData":"def",
                    "signature":"ghi"
                }
            }"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.message") { value("Authentication successful") }
            }
    }

    @Test
    fun `result returns failed when challenge not found`() {
        doThrow(AuthenticationChallengeNotFoundException("abc"))
            .whenever(authenticationService)
            .verify(any())

        mockMvc
            .post("/assertion/result") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                "id":"cred123","type":"public-key",
                "response":{
                    "clientDataJSON":"abc",
                    "authenticatorData":"def",
                    "signature":"ghi"
                }
            }"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(false) }
            }
    }

    @Test
    fun `result returns failed on verification error`() {
        doThrow(AuthenticationVerificationException("invalid signature"))
            .whenever(authenticationService)
            .verify(any())

        mockMvc
            .post("/assertion/result") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                "id":"cred123","type":"public-key",
                "response":{
                    "clientDataJSON":"abc",
                    "authenticatorData":"def",
                    "signature":"ghi"
                }
            }"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(false) }
            }
    }

    @Test
    fun `result returns failed on unknown credential`() {
        doThrow(CredentialNotFoundException("cred123"))
            .whenever(authenticationService)
            .verify(any())

        mockMvc
            .post("/assertion/result") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                "id":"cred123","type":"public-key",
                "response":{
                    "clientDataJSON":"abc",
                    "authenticatorData":"def",
                    "signature":"ghi"
                }
            }"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(false) }
            }
    }
}
