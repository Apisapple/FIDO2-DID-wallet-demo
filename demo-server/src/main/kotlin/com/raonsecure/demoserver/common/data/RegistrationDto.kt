package com.raonsecure.demoserver.common.data

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RegistrationOptionsRequest(
    val username: String,
    val displayName: String? = null,
    val attestation: String? = AttestationConveyancePreference.NONE.value,
    val authenticatorSelection: AuthenticatorSelectionCriteria? = null,
)

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
