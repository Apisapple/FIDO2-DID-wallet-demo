package com.example.demo.library.model.attestation

enum class AttestationConveyancePreference(
    val value: String,
) {
    NONE("none"),
    INDIRECT("indirect"),
    DIRECT("direct"),
    ENTERPRISE("enterprise"),
    ;

    companion object {
        fun fromValue(value: String): AttestationConveyancePreference? = entries.find { it.value == value }
    }
}
