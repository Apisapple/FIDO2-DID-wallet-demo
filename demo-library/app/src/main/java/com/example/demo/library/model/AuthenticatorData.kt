package com.example.demo.library.model

import co.nstant.`in`.cbor.builder.AbstractBuilder
import co.nstant.`in`.cbor.builder.MapBuilder
import com.example.demo.library.exceptions.WebAuthnException
import java.nio.ByteBuffer
import java.security.interfaces.ECPublicKey

data class AuthenticatorData(
    val rpIdHash: ByteArray,
    var flags: UByte,
    val signCount: UInt,
    val attestedCredData: ByteArray? = null,
    val extensions: ByteArray? = null,
) {
    fun toByteArray(): ByteArray {
        if (attestedCredData != null) {
            val atFlag = AuthenticatorDataFlags.AT.value
            flags = flags or atFlag
        }
        if (extensions != null) {
            val edFlag = AuthenticatorDataFlags.ED.value
            flags = flags or edFlag
        }

        try {
            return ByteBuffer
                .allocate(
                    rpIdHash.size +
                        1 +
                        4 +
                        (attestedCredData?.size ?: 0) +
                        (extensions?.size ?: 0),
                ).apply {
                    put(rpIdHash)
                    put(flags.toByte())
                    putInt(signCount.toInt())
                    attestedCredData?.let { put(it) }
                    extensions?.let { put(it) }
                }.array()
        } catch (e: Exception) {
            throw WebAuthnException.EncodingException(
                "Cannot convert AuthenticatorData to ByteArray",
                e,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuthenticatorData

        if (!rpIdHash.contentEquals(other.rpIdHash)) return false
        if (flags != other.flags) return false
        if (signCount != other.signCount) return false
        if (!attestedCredData.contentEquals(other.attestedCredData)) return false
        if (!extensions.contentEquals(other.extensions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rpIdHash.contentHashCode()
        result = 31 * result + flags.hashCode()
        result = 31 * result + signCount.hashCode()
        result = 31 * result + (attestedCredData?.contentHashCode() ?: 0)
        result = 31 * result + (extensions?.contentHashCode() ?: 0)
        return result
    }
}

class AttestedCredData(
    val aaguid: ByteArray,
    val credId: ByteArray,
    val publicKey: ByteArray,
) {
    fun toByteArray(): ByteArray {
        try {
            return ByteBuffer
                .allocate(aaguid.size + 2 + credId.size + publicKey.size)
                .apply {
                    put(aaguid)
                    putShort(credId.size.toShort())
                    put(credId)
                    put(publicKey)
                }.array()
        } catch (e: Exception) {
            throw WebAuthnException.EncodingException(
                "Cannot convert AttestedCredData to ByteArray",
                e,
            )
        }
    }
}

class EC2COSEKey(
    var kty: Int,
    var alg: Int,
    var crv: Int,
    var x: ByteArray,
    var y: ByteArray,
) : CborSerializable {
    constructor(ecPublicKey: ECPublicKey) : this(
        kty = 2,
        alg = -7,
        crv = 1,
        x = ecPublicKey.w.affineX.toByteArray(),
        y = ecPublicKey.w.affineY.toByteArray(),
    )

    override fun <T : AbstractBuilder<*>?> toCBOR(builder: MapBuilder<T>): T =
        builder
            .put(1, kty.toLong())
            .put(3, alg.toLong())
            .put(-1, crv.toLong())
            .put(-2, x)
            .put(-3, y)
            .end()
}

enum class AuthenticatorDataFlags(
    val value: UByte,
) {
    UP(0b0000_0001u),
    UV(0b0000_0100u),
    AT(0b0100_0000u),
    ED(0b1000_0000u),
    ;

    companion object {
        fun makeFlags(
            userPresent: Boolean = false,
            userVerified: Boolean = false,
            attestedCredentialDataIncluded: Boolean = false,
            extensionDataIncluded: Boolean = false,
        ): UByte {
            var flags: UByte = 0u
            if (userPresent) flags = flags or UP.value
            if (userVerified) flags = flags or UV.value
            if (attestedCredentialDataIncluded) flags = flags or AT.value
            if (extensionDataIncluded) flags = flags or ED.value
            return flags
        }
    }
}
