package com.example.demo.library.authenticator.keygenerator

import android.security.keystore.StrongBoxUnavailableException
import com.example.demo.library.model.attestation.COSEAlgorithmIdentifier
import java.security.KeyPair

class DeviceCredentialKeyGenerator : Fido2KeyGenerator() {
    override fun generateFido2Key(
        keyAlias: String,
        challenge: ByteArray?,
        publicKEyAlgorithm: COSEAlgorithmIdentifier,
        isStrongBoxBacked: Boolean,
        userAuthenticationRequired: Boolean,
    ): KeyPair {
        TODO()
    }

    private fun generateDeviceCredentialFido2Key(
        keyAlias: String,
        challenge: ByteArray?,
        publicKEyAlgorithm: COSEAlgorithmIdentifier,
        isStrongBoxBacked: Boolean,
        userAuthenticationRequired: Boolean,
        userAuthenticationValidityDurationSeconds: Int = 5,
    ): KeyPair {
        TODO()
    }
}
