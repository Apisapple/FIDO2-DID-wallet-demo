package com.example.demo.library.util

import com.example.demo.library.exceptions.WebAuthnException
import java.util.Base64

fun String.toBase64url(): String {
    try {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(this.encodeToByteArray())
    } catch (e: Exception) {
        throw WebAuthnException.UtilityException("Failed to encode string to Base64url: ${e.message}")
    }
}

fun String.base64urlToString(): String {
    try {
        return Base64.getUrlDecoder().decode(this).decodeToString()
    } catch (e: Exception) {
        throw WebAuthnException.UtilityException("Failed to decode base64url String to String", e)
    }
}

fun String.base64urlToByteArray(): ByteArray {
    try {
        return Base64.getUrlDecoder().decode(this)
    } catch (e: Exception) {
        throw WebAuthnException.UtilityException("Failed to decode base64url String to ByteArray", e)
    }
}

fun ByteArray.toBase64url(): String {
    try {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(this)
    } catch (e: Exception) {
        throw WebAuthnException.UtilityException("Failed to encode ByteArray to base64url String", e)
    }
}
