package com.raonsecure.demoserver.registration

import com.raonsecure.demoserver.common.data.WebAuthnVerificationResponse
import com.raonsecure.demoserver.registration.exception.ChallengeNotFoundException
import com.raonsecure.demoserver.registration.exception.DuplicateCredentialException
import com.raonsecure.demoserver.registration.exception.RegistrationVerificationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class RegistrationExceptionHandler {

    @ExceptionHandler(ChallengeNotFoundException::class)
    fun handleChallengeNotFound(e: ChallengeNotFoundException): ResponseEntity<WebAuthnVerificationResponse> =
        ResponseEntity.ok(WebAuthnVerificationResponse(success = false, message = e.message))

    @ExceptionHandler(RegistrationVerificationException::class)
    fun handleVerificationFailed(e: RegistrationVerificationException): ResponseEntity<WebAuthnVerificationResponse> =
        ResponseEntity.ok(WebAuthnVerificationResponse(success = false, message = e.message))

    @ExceptionHandler(DuplicateCredentialException::class)
    fun handleDuplicate(e: DuplicateCredentialException): ResponseEntity<WebAuthnVerificationResponse> =
        ResponseEntity.ok(WebAuthnVerificationResponse(success = false, message = e.message))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<WebAuthnVerificationResponse> =
        ResponseEntity.badRequest().body(WebAuthnVerificationResponse(success = false, message = e.message))
}
