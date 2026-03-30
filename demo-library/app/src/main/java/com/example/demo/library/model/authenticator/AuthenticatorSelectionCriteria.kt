package com.example.demo.library.model.authenticator

data class AuthenticatorSelectionCriteria(
    val authenticatorAttachment: String?,
    val userVerification: String = UserVerificationRequirement.PREFERRED.value,
)

enum class UserVerificationRequirement(
    val value: String,
) {
    REQUIRED("required"),
    PREFERRED("preferred"),
    ;

    companion object {
        fun fromValue(value: String): UserVerificationRequirement? = entries.find { it.value == value }
    }
}
