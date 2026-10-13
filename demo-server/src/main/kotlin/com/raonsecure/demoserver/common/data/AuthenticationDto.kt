package com.raonsecure.demoserver.common.data

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuthenticationOptionsRequest(
    val username: String? = null,
    val userVerification: String? = UserVerificationRequirement.PREFERRED.value,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuthenticationDataResponse(
    val challenge: String,
    val rpId: String,
    val allowCredentials: List<PublicKeyCredentialDescriptor>? = null,
    val timeout: Long? = 60000L,
    val userVerification: String = UserVerificationRequirement.PREFERRED.value,
    val extensions: Map<String, Any>? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuthenticatorAssertionResponseDto(
    @JsonAlias("clientDataJson", "clientDataJSON")
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
    val userHandle: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicKeyCredentialGetResultDto(
    val id: String,
    val rawId: String? = null,
    val type: String = PublicKeyCredentialType.PUBLIC_KEY.value,
    @JsonAlias("response", "authenticatorAssertionResponse")
    val response: AuthenticatorAssertionResponseDto,
    @JsonAlias("clientExtensionOutput", "clientExtensionOutputs", "extensions")
    val clientExtensionOutput: Map<String, Any>? = null,
)
