package com.example.demo.library.model

import co.nstant.`in`.cbor.builder.AbstractBuilder
import co.nstant.`in`.cbor.builder.MapBuilder

class AssertionObject(val authenticatorData: ByteArray, val signature: ByteArray) :
    CborSerializable {

    override fun <T : AbstractBuilder<*>?> toCBOR(builder: MapBuilder<T>): T = builder
        .put("authenticatorData", this.authenticatorData)
        .put("signature", this.signature)
        .end()
}