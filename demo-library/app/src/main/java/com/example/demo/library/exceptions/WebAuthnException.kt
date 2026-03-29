package com.example.demo.library.exceptions

sealed class WebAuthnException(
    override val message: String?,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    sealed class CoreException(message: String?, cause: Throwable? = null) :
        WebAuthnException(message, cause) {
        class ConstraintException(
            message: String? = "A mutation operation in a transaction failed because a contraint was not satisfied.",
            cause: Throwable? = null
        ) : CoreException(message, cause)

        class InvalidStateException(
            message: String? = "The object is in an invalid state.",
            cause: Throwable? = null
        ) :
            CoreException(message, cause)

        class NotAllowedException(
            message: String? = "The request is not allowed by the user agent or the platform in the current context, " +
                    "possibly because the user denied permission.",
            cause: Throwable? = null
        ) : CoreException(message, cause)

        class NotSupportedException(
            message: String? = "The operation is not supported.",
            cause: Throwable? = null
        ) :
            CoreException(message, cause)

        class TypeException(message: String? = null, cause: Throwable? = null) :
            CoreException(message, cause)

    }

    class CredSrcStorageException(message: String?, cause: Throwable? = null) :
        WebAuthnException(message, cause)

    class RpException(message: String? = null, cause: Throwable? = null) :
        WebAuthnException(message, cause)

    sealed class AuthenticationException(message: String? = null, cause: Throwable? = null) :
        WebAuthnException(message, cause) {
        class KeyPermanentlyInvalidatedException(
            message: String? =
                "The key can no longer be used. Check if a new fingerprint is enrolled or the secure lock is disabled.",
            cause: Throwable? = null
        ) : AuthenticationException(message, cause)
    }

    class SecureExecutionException(message: String? = null, cause: Throwable? = null) :
        WebAuthnException(message, cause)

    class KeyNotFoundException(message: String? = null, cause: Throwable? = null) :
        WebAuthnException(message, cause)

    class UnknownException(message: String, cause: Throwable? = null) :
        WebAuthnException(message, cause)

    class UtilityException(message: String, cause: Throwable? = null) :
        WebAuthnException(message, cause)

    class EncodingException(message: String, cause: Throwable? = null) :
        WebAuthnException(message, cause)

    class DeletionException(message: String, cause: Throwable? = null, trigger: Throwable? = null) :
        WebAuthnException(message, cause)
}
