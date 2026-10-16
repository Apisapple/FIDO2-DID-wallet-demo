package com.raonsecure.demoserver.common.data

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import java.util.Base64

enum class PublicKeyCredentialType(
    @JsonValue val value: String,
) {
    PUBLIC_KEY("public-key"),
    ;

    companion object {
        fun fromValue(value: String): PublicKeyCredentialType? = entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

data class Credential(
    val id: String,
    val type: PublicKeyCredentialType,
)

enum class COSEAlgorithm(
    val value: Long,
    val jcaName: String,
) {
    ES256(-7L, "SHA256withECDSA"),
    ES384(-35L, "SHA384withECDSA"),
    ES512(-36L, "SHA512withECDSA"),
    RS256(-257L, "SHA256withRSA"),
    RS384(-258L, "SHA384withRSA"),
    RS512(-259L, "SHA512withRSA"),
    PS256(-37L, "SHA256withRSA/PSS"),
    PS384(-38L, "SHA384withRSA/PSS"),
    PS512(-39L, "SHA512withRSA/PSS"),
    EdDSA(-8L, "EdDSA"),
    ;

    companion object {
        fun fromValue(value: Long): COSEAlgorithm? = entries.find { it.value == value }
    }
}

enum class AttestationConveyancePreference(
    @JsonValue val value: String,
) {
    NONE("none"),
    INDIRECT("indirect"),
    DIRECT("direct"),
    ENTERPRISE("enterprise"),
    ;

    companion object {
        fun fromValue(value: String): AttestationConveyancePreference? = entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

enum class UserVerificationRequirement(
    @JsonValue val value: String,
) {
    REQUIRED("required"),
    PREFERRED("preferred"),
    DISCOURAGED("discouraged"),
    ;

    companion object {
        fun fromValue(value: String): UserVerificationRequirement? = entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

enum class AuthenticatorTransport(
    @JsonValue val value: String,
) {
    USB("usb"),
    NFC("nfc"),
    BLE("ble"),
    SMART_CARD("smart-card"),
    HYBRID("hybrid"),
    INTERNAL("internal"),
    ;

    companion object {
        fun fromValue(value: String): AuthenticatorTransport? = entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
open class PublicKeyCredentialEntity(
    var name: String = "",
    var icon: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicKeyCredentialRpEntity(
    val id: String,
    val name: String,
    val icon: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
class PublicKeyCredentialUserEntity(
    var id: String = "",
    var displayName: String = "",
    name: String = "",
    icon: String? = null,
) : PublicKeyCredentialEntity(
        name = name,
        icon = icon,
    ) {
    constructor(
        id: ByteArray,
        displayName: String,
        name: String,
        icon: String? = null,
    ) : this(
        id = Base64.getUrlEncoder().withoutPadding().encodeToString(id),
        displayName = displayName,
        name = name,
        icon = icon,
    )
}

data class PublicKeyCredentialParams(
    val type: String = PublicKeyCredentialType.PUBLIC_KEY.value,
    val alg: Long = COSEAlgorithm.ES256.value,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicKeyCredentialDescriptor(
    val id: String,
    val type: String = PublicKeyCredentialType.PUBLIC_KEY.value,
    val transports: List<String>? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuthenticatorSelectionCriteria(
    val authenticatorAttachment: String? = null,
    val requireResidentKey: Boolean? = null,
    val residentKey: String? = null,
    val userVerification: String? = UserVerificationRequirement.PREFERRED.value,
)
