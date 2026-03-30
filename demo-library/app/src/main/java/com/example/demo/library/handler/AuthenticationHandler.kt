package com.example.demo.library.handler

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.example.demo.library.model.fido2.Fido2PromptInfo
import com.example.demo.library.model.fido2.Fido2UserAuthResult
import java.security.Signature

interface AuthenticationHandler {
    class AuthenticationFailedException(
        val errorCode: Int? = null,
        message: String? = null,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    class AuthenticationErrorException(
        val errorCode: Int? = null,
        message: String? = null,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    fun isSupported(context: Context): Boolean

    suspend fun authenticate(
        activity: FragmentActivity,
        fido2PromptInfo: Fido2PromptInfo?,
        signatureProvider: (() -> Signature)? = null,
    ): Fido2UserAuthResult
}
