package com.raonsecure.demoserver.common.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class WebAuthnDtoTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun testRegistrationDataResponseSerialization() {
        val response =
            RegistrationDataResponse(
                rp = PublicKeyCredentialRpEntity(id = "localhost", name = "Demo RP"),
                user =
                    PublicKeyCredentialUserEntity(
                        id = "dXNlci1pZA",
                        displayName = "Demo User",
                        name = "demouser",
                    ),
                challenge = "Y2hhbGxlbmdlLTEyMw",
                pubKeyCredParams =
                    listOf(
                        PublicKeyCredentialParams(type = "public-key", alg = -7L),
                        PublicKeyCredentialParams(type = "public-key", alg = -257L),
                    ),
                authenticatorSelection =
                    AuthenticatorSelectionCriteria(
                        authenticatorAttachment = "platform",
                        userVerification = "preferred",
                    ),
            )

        val json = objectMapper.writeValueAsString(response)
        assertNotNull(json)
        assertTrue(json.contains("\"challenge\":\"Y2hhbGxlbmdlLTEyMw\""))
        assertTrue(json.contains("\"id\":\"localhost\""))
        assertTrue(json.contains("\"alg\":-7"))
    }

    @Test
    fun testPublicKeyCredentialCreateResultDeserialization() {
        val json =
            """
            {
                "id": "cred-id-123",
                "rawId": "cred-id-123",
                "type": "public-key",
                "authenticatorAttestationResponse": {
                    "clientDataJson": "eyJjaGFsbGVuZ2UiOiIxMjMifQ",
                    "attestationObject": "b2JqZWN0"
                }
            }
            """.trimIndent()

        val dto = objectMapper.readValue<PublicKeyCredentialCreateResultDto>(json)
        assertEquals("cred-id-123", dto.id)
        assertEquals("public-key", dto.type)
        assertEquals("eyJjaGFsbGVuZ2UiOiIxMjMifQ", dto.response.clientDataJSON)
        assertEquals("b2JqZWN0", dto.response.attestationObject)
    }

    @Test
    fun testPublicKeyCredentialGetResultDeserialization() {
        val json =
            """
            {
                "id": "cred-id-456",
                "type": "public-key",
                "response": {
                    "clientDataJSON": "eyJjaGFsbGVuZ2UiOiI0NTYifQ",
                    "authenticatorData": "YXV0aERhdGE",
                    "signature": "c2lnbmF0dXJl",
                    "userHandle": "dXNlckhhbmRsZQ"
                }
            }
            """.trimIndent()

        val dto = objectMapper.readValue<PublicKeyCredentialGetResultDto>(json)
        assertEquals("cred-id-456", dto.id)
        assertEquals("eyJjaGFsbGVuZ2UiOiI0NTYifQ", dto.response.clientDataJSON)
        assertEquals("YXV0aERhdGE", dto.response.authenticatorData)
        assertEquals("c2lnbmF0dXJl", dto.response.signature)
        assertEquals("dXNlckhhbmRsZQ", dto.response.userHandle)
    }
}
