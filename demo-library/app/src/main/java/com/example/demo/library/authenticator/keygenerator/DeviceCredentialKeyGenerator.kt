package com.example.demo.library.authenticator.keygenerator

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.demo.library.model.attestation.COSEAlgorithmIdentifier
import com.example.demo.library.model.attestation.getAlgorithmParameterSpec
import com.example.demo.library.model.attestation.getDigests
import com.example.demo.library.model.attestation.getKeyProperties
import com.example.demo.library.model.attestation.getSignaturePaddings
import java.security.KeyPair
import java.security.KeyPairGenerator

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
        publicKeyAlgorithm: COSEAlgorithmIdentifier,
        isStrongBoxBacked: Boolean,
        userAuthenticationRequired: Boolean,
        userAuthenticationValidityDurationSeconds: Int = 5,
    ): KeyPair {
        synchronized(lock) {
            val keyProperties =
                publicKeyAlgorithm.getKeyProperties()
                    ?: throw IllegalArgumentException("Unsupported public key algorithm: $publicKeyAlgorithm")
            val kpg: KeyPairGenerator =
                KeyPairGenerator.getInstance(keyProperties, "AndroidKeyStore")
            val parameterSpec: KeyGenParameterSpec =
                KeyGenParameterSpec
                    .Builder(
                        keyAlias,
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                    ).run {
                        setIsStrongBoxBacked(isStrongBoxBacked)
                        publicKeyAlgorithm.getSignaturePaddings()?.let {
                            setSignaturePaddings(it)
                        }
                        publicKeyAlgorithm.getAlgorithmParameterSpec()?.let {
                            setAlgorithmParameterSpec(it)
                        }
                        publicKeyAlgorithm.getDigests()?.let {
                            setDigests(it)
                        }
                        setUserAuthenticationRequired(userAuthenticationRequired)
                        setInvalidatedByBiometricEnrollment(true)
                        if (challenge != null) {
                            setAttestationChallenge(challenge)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            setUserAuthenticationParameters(
                                0,
                                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                            )
                        } else {
                            setUserAuthenticationValidityDurationSeconds(
                                userAuthenticationValidityDurationSeconds,
                            )
                        }
                        build()
                    }

            kpg.initialize(parameterSpec)
            return kpg.generateKeyPair()
        }
    }
}
