package com.example.demo.library.model.publickeycredential

import java.util.UUID

data class PublicKeyCredentialSource(
    val type: String = PublicKeyCredentialType.PUBLIC_KEY.value,
    val id: String,
    val rpId: String,
    val userHandle: String?,
    val aaguid: UUID,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PublicKeyCredentialSource

        if (type != other.type) return false
        if (!id.contentEquals(other.id)) return false
        if (rpId != other.rpId) return false
        if (userHandle != null) {
            if (other.userHandle == null) return false

            if (!userHandle.contentEquals(other.userHandle)) return false
        } else if (other.userHandle != null) {
            return false
        }

        return aaguid == other.aaguid
    }

    override fun hashCode(): Int {
        var result = type.hashCode()

        result = 31 * result + id.hashCode()
        result = 31 * result + rpId.hashCode()
        result = 31 * result + (userHandle?.hashCode() ?: 0)
        result = 31 * result + aaguid.hashCode()
        return result
    }
}
