package com.example.demo.library.handler

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.demo.library.model.fido2.Fido2PromptInfo
import kotlinx.coroutines.suspendCancellableCoroutine

class KeyguardManagerWrapper {
    class KeyguardNotSecuredException(
        message: String,
    ) : Exception(message)

    class DeviceCredentialIntentNotAvailableException(
        message: String,
    ) : Exception(message)

    class KeyguardManagerAuthenticationFailedException(
        val errorCode: Int?,
        message: String,
    ) : Exception(message)

    fun isSupported(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isDeviceSecure
    }

    suspend fun authenticate(
        context: Context,
        fido2PromptInfo: Fido2PromptInfo?,
    ): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (!keyguardManager.isKeyguardSecure) {
            throw KeyguardNotSecuredException("Keyguard not secured")
        }

        val intent =
            keyguardManager.createConfirmDeviceCredentialIntent(
                fido2PromptInfo?.title ?: "Device Credential Authentication",
                fido2PromptInfo?.description
                    ?: "Please authenticate using your device credentials.",
            )
                ?: throw DeviceCredentialIntentNotAvailableException("Device credential intent not available")

        Log.d("KeyguardManagerWrapper", "Starting device credential authentication")

        return suspendCancellableCoroutine { continuation ->
        }
    }

    class AuthenticationActivity : AppCompatActivity() {
        companion object {
            private const val REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1
            private var callback: ((Boolean, Int?) -> Unit)? = null

            fun start(
                context: Context,
                intent: Intent,
                callback: ((Boolean, Int?) -> Unit),
            ) {
                this.callback = callback
                val activityIntent =
                    Intent(context, AuthenticationActivity::class.java).apply {
                        putExtra("fido2_auth_intent", intent)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                context.startActivity(activityIntent)
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val intent = intent.getParcelableExtra<Intent>("fido2_auth_intent")
            if (intent != null) {
                startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS)
            } else {
                callback?.invoke(false, BiometricPrompt.BIOMETRIC_ERROR_UNABLE_TO_PROCESS)
                finish()
            }
        }

        override fun onActivityResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent?,
        ) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
                Log.d("AuthenticationActivity", "Received result: $resultCode")
                when (resultCode) {
                    RESULT_OK -> {
                        callback?.invoke(true, null)
                        Log.d("AuthenticationActivity", "Authentication succeeded")
                    }

                    RESULT_CANCELED -> {
                        Log.d("AuthenticationActivity", "Authentication canceled")
                        callback?.invoke(false, BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED)
                    }

                    else -> {
                        Log.d("AuthenticationActivity", "Authentication failed")
                        callback?.invoke(false, BiometricPrompt.BIOMETRIC_ERROR_UNABLE_TO_PROCESS)
                    }
                }
            }
            finish()
        }
    }
}
