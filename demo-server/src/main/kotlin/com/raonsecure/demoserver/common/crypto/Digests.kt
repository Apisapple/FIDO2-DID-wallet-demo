package com.raonsecure.demoserver.common.crypto

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object Digests {
    fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    fun sha1(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-1").digest(data)

    fun digest(
        alg: String,
        data: ByteArray,
    ): ByteArray {
        try {
            val messageDigests = MessageDigest.getInstance(alg)

            return messageDigests.digest(data)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }
}
