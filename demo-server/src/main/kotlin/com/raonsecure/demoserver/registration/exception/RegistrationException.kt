package com.raonsecure.demoserver.registration.exception

class ChallengeNotFoundException(
    challenge: String,
) : RuntimeException("Challenge not found or expired: $challenge")

class RegistrationVerificationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class DuplicateCredentialException(
    credentialId: String,
) : RuntimeException("Credential already registered: $credentialId")
