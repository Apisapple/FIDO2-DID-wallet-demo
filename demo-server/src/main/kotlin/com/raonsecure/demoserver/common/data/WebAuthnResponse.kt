package com.raonsecure.demoserver.common.data

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WebAuthnVerificationResponse(
    val success: Boolean,
    val message: String? = null,
    val userId: String? = null,
    val username: String? = null,
)
