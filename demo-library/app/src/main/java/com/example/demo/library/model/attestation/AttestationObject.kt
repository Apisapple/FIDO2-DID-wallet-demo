package com.example.demo.library.model.attestation

import co.nstant.`in`.cbor.builder.AbstractBuilder
import co.nstant.`in`.cbor.builder.MapBuilder
import com.example.demo.library.model.CborSerializable

data class AttestationObject(
    val authData: ByteArray,
    val fmt: String,
    val attestationStatement: AttestationStatement,
) : CborSerializable {
    override fun <T : AbstractBuilder<*>?> toCBOR(builder: MapBuilder<T>): T {
        builder.put("authData", authData)
        builder.put("fmt", fmt)
        if (fmt == AttestationStatementFormat.NONE.value) {
            builder.putMap("attStmt")
        } else {
            attestationStatement.toCBOR(builder.startMap("attStmt"))
        }

        return builder.end()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttestationObject

        if (!authData.contentEquals(other.authData)) return false
        if (fmt != other.fmt) return false
        if (attestationStatement != other.attestationStatement) return false

        return true
    }

    override fun hashCode(): Int {
        var result = authData.contentHashCode()
        result = 31 * result + fmt.hashCode()
        result = 31 * result + attestationStatement.hashCode()
        return result
    }
}
