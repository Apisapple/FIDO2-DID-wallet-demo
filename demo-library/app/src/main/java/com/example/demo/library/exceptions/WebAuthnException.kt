package com.example.demo.library.exceptions

sealed class WebAuthnException(
    override val message: String?,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    class NotSupported(
        message: String,
    ) : WebAuthnException(message)

    class InvalidState(
        message: String,
    ) : WebAuthnException(message)

    class OperationFailed(
        message: String,
    ) : WebAuthnException(message)
}
