package com.raonsecure.demoserver.registration

import com.raonsecure.demoserver.common.web.MalformedRequestExceptionHandler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

/**
 * A request body the server cannot read must answer with the project's envelope rather than the
 * framework's error page, and must not leak the parser's own message. The advice under test is
 * application-wide, so it is imported explicitly: `@WebMvcTest` would otherwise only pick up the
 * advice living in the controller's own package.
 */
@WebMvcTest(controllers = [RegistrationController::class])
@Import(MalformedRequestExceptionHandler::class)
class MalformedRequestBodyTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var registrationService: RegistrationService

    @Test
    fun `body omitting username answers with the failure envelope`() {
        mockMvc
            .post("/attestation/options") {
                contentType = MediaType.APPLICATION_JSON
                content = "{}"
            }.andExpect {
                jsonPath("$.success") { value(false) }
                jsonPath("$.message") { exists() }
                jsonPath("$.trace") { doesNotExist() }
            }
    }

    @Test
    fun `malformed json answers with the failure envelope and no parser detail`() {
        mockMvc
            .post("/attestation/options") {
                contentType = MediaType.APPLICATION_JSON
                content = "this is not json"
            }.andExpect {
                jsonPath("$.success") { value(false) }
                jsonPath("$.message") { exists() }
                jsonPath("$.trace") { doesNotExist() }
            }
    }

    /**
     * The message must name the problem without echoing the parser, which would otherwise expose
     * the DTO class name and the offending bytes.
     */
    @Test
    fun `malformed json message carries no internal detail`() {
        val body =
            mockMvc
                .post("/attestation/options") {
                    contentType = MediaType.APPLICATION_JSON
                    content = "this is not json"
                }.andReturn()
                .response.contentAsString

        listOf("RegistrationOptionsRequest", "JsonParseException", "tools.jackson", "at [Source")
            .forEach { leak ->
                assert(!body.contains(leak)) { "response leaked internal detail '$leak': $body" }
            }
    }

    /**
     * Covers the registration spec's single "Missing username" scenario: a blank field and an
     * absent one are one condition and are reported the same way.
     */
    @Test
    fun `absent and blank username produce the same envelope`() {
        fun envelopeFor(content: String): String =
            mockMvc
                .post("/attestation/options") {
                    contentType = MediaType.APPLICATION_JSON
                    this.content = content
                }.andReturn()
                .response.contentAsString

        val absent = envelopeFor("{}")
        val blank = envelopeFor("""{"username":""}""")

        assert(absent == blank) { "absent=$absent blank=$blank" }
    }
}
