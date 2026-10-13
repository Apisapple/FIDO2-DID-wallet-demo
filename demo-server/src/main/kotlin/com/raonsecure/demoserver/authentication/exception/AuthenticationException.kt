package com.raonsecure.demoserver.authentication.exception

open class AuthenticationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class AuthenticationChallengeNotFoundException(challenge: String) :
    AuthenticationException("Challenge not found or expired: $challenge")

class CredentialNotFoundException(credentialId: String) :
    AuthenticationException("Credential not found: $credentialId")

class SignCounterRegressionException(storedCount: Long, receivedCount: Long) :
    AuthenticationException("Sign counter regressed: stored $storedCount, received $receivedCount")

class AuthenticationVerificationException(message: String, cause: Throwable? = null) :
    AuthenticationException(message, cause)

class UserHandleMismatchException(credentialId: String) :
    AuthenticationException("Asserted userHandle does not match the credential: $credentialId")
