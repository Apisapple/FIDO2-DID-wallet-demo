package com.raonsecure.demoserver.common.data

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RegistrationOptionsRequest(
    /**
     * Nullable so that an omitted field reaches the endpoint's own validation rather than failing
     * during deserialization. A blank username and an absent one are the same error and are
     * reported the same way; [requiredUsername] is the validated value.
     */
    val username: String? = null,
    val displayName: String? = null,
    val attestation: String? = AttestationConveyancePreference.NONE.value,
    val authenticatorSelection: AuthenticatorSelectionCriteria? = null,
) {
    /** The username, once validated as present and non-blank. */
    val requiredUsername: String
        get() = requireNotNull(username) { "username is required" }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RegistrationDataResponse(
    val rp: PublicKeyCredentialRpEntity,
    val user: PublicKeyCredentialUserEntity,
    val challenge: String,
    val pubKeyCredParams: List<PublicKeyCredentialParams>,
    val timeout: Long? = 60000L,
    val excludeCredentials: List<PublicKeyCredentialDescriptor>? = null,
    val authenticatorSelection: AuthenticatorSelectionCriteria? = null,
    val attestation: String = AttestationConveyancePreference.NONE.value,
    val extensions: Map<String, Any>? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuthenticatorAttestationResponseDto(
    @JsonAlias("clientDataJson", "clientDataJSON")
    val clientDataJSON: String,
    val attestationObject: String,
    val transports: List<String>? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicKeyCredentialCreateResultDto(
    val id: String,
    val rawId: String? = null,
    val type: String = PublicKeyCredentialType.PUBLIC_KEY.value,
    @JsonAlias("response", "authenticatorAttestationResponse")
    val response: AuthenticatorAttestationResponseDto,
    @JsonAlias("clientExtensionOutput", "clientExtensionOutputs", "extensions")
    val clientExtensionOutput: Map<String, Any>? = null,
)
