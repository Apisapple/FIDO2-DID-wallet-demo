package com.example.demo.library.model

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.builder.AbstractBuilder
import co.nstant.`in`.cbor.builder.MapBuilder
import com.example.demo.library.exceptions.WebAuthnException
import java.io.ByteArrayOutputStream

interface CborSerializable {
    fun <T : AbstractBuilder<*>?> toCBOR(builder: MapBuilder<T>): T

    fun toCBOR(canonical: Boolean = true): ByteArray {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            CborEncoder(byteArrayOutputStream).encode(toCBOR(CborBuilder().startMap()).build())

            return byteArrayOutputStream.toByteArray()
        } catch (e: Exception) {
            throw WebAuthnException.EncodingException("Failed to encode object to CBOR", e)
        }
    }
}

