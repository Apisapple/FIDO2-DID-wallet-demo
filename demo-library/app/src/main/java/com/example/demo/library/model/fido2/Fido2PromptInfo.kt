package com.example.demo.library.model.fido2

data class Fido2PromptInfo(
    val title: CharSequence? = "Biometric Authentication",
    val subtitle: CharSequence? = "Enter biometric credentials to proceed",
    val description: CharSequence? = "Input your Fingerprint or FaceID to ensure it's you",
    val negativeButtonText: CharSequence? = "Cancel",
)
