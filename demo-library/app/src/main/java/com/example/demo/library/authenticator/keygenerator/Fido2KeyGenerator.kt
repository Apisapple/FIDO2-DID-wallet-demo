package com.example.demo.library.authenticator.keygenerator

import com.example.demo.library.model.attestation.COSEAlgorithmIdentifier
import java.security.KeyPair

abstract class Fido2KeyGenerator {
    val lock = Any()

    abstract fun generateFido2Key(
        keyAlias: String,
        challenge: ByteArray?,
        publicKEyAlgorithm: COSEAlgorithmIdentifier,
        isStrongBoxBacked: Boolean,
        userAuthenticationRequired: Boolean = true,
    ): KeyPair
}
