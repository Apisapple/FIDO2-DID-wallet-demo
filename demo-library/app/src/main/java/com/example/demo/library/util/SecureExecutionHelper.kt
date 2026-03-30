package com.example.demo.library.util

import com.example.demo.library.exceptions.WebAuthnException
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.PublicKey
import java.security.cert.X509Certificate

internal class SecureExecutionHelper {
    private val lock = Any()

    fun deleteKey(keyAlias: String) {
        synchronized(lock) {
            try {
                KeyStore.getInstance("AndroidKeyStore").also {
                    it.load(null)
                    it.deleteEntry(keyAlias)
                }
            } catch (e: Throwable) {
                throw WebAuthnException.SecureExecutionException(
                    "Cannot delete key from KeyStore.",
                    e,
                )
            }
        }
    }

    fun getKey(keyAlias: String): Key? {
        synchronized(lock) {
            try {
                val keyStore =
                    KeyStore.getInstance("AndroidKeyStore").also {
                        it.load(null, null)
                    }
                return keyStore.getKey(keyAlias, null)
            } catch (e: Throwable) {
                throw WebAuthnException.SecureExecutionException(
                    "Cannot fetch key from KeyStore.",
                    e,
                )
            }
        }
    }

    fun getX509Certificates(keyAlias: String): List<X509Certificate> {
        synchronized(lock) {
            try {
                val keyStore =
                    KeyStore.getInstance("AndroidKeyStore").also {
                        it.load(null, null)
                    }
                val certChain =
                    keyStore.getCertificateChain(keyAlias)
                        ?: throw KeyStoreException("Cannot find certificate chain")
                val certChainList = certChain.toList()
                return certChainList.map { it as X509Certificate }
            } catch (e: Throwable) {
                throw WebAuthnException.SecureExecutionException(
                    "Cannot fetch X509 certificate from KeyStore.",
                    e,
                )
            }
        }
    }

    fun getX509Certificate(keyAlias: String): X509Certificate {
        synchronized(lock) {
            try {
                val keyStore =
                    KeyStore.getInstance("AndroidKeyStore").also {
                        it.load(null, null)
                    }
                val certificate =
                    keyStore.getCertificate(keyAlias)
                        ?: throw KeyStoreException("Cannot find certificate")
                return certificate as X509Certificate
            } catch (e: Throwable) {
                throw WebAuthnException.SecureExecutionException(
                    "Cannot fetch X509 certificate from KeyStore.",
                    e,
                )
            }
        }
    }

    fun containAlias(keyAlias: String): Boolean {
        synchronized(lock) {
            try {
                val keyStore =
                    KeyStore.getInstance("AndroidKeyStore").also {
                        it.load(null, null)
                    }
                return keyStore.containsAlias(keyAlias)
            } catch (e: Throwable) {
                throw WebAuthnException.SecureExecutionException(
                    "Cannot check key alias from KeyStore.",
                    e,
                )
            }
        }
    }

    fun getPublicKey(keyAlias: String): PublicKey {
        try {
            val x509CertificateChain = getX509Certificates(keyAlias)
            return x509CertificateChain[0].publicKey
        } catch (e: Throwable) {
            throw WebAuthnException.SecureExecutionException(
                "Cannot fetch public key from KeyStore.",
                e,
            )
        }
    }
}
