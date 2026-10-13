package com.raonsecure.demoserver.authentication

import com.raonsecure.demoserver.authentication.exception.AuthenticationChallengeNotFoundException
import com.raonsecure.demoserver.authentication.exception.AuthenticationVerificationException
import com.raonsecure.demoserver.authentication.exception.CredentialNotFoundException
import com.raonsecure.demoserver.authentication.exception.SignCounterRegressionException
import com.raonsecure.demoserver.authentication.exception.UserHandleMismatchException
import com.raonsecure.demoserver.common.data.WebAuthnVerificationResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackageClasses = [AuthenticationController::class])
class AuthenticationExceptionHandler {

    @ExceptionHandler(AuthenticationChallengeNotFoundException::class)
    fun handleChallengeNotFound(e: AuthenticationChallengeNotFoundException): ResponseEntity<WebAuthnVerificationResponse> =
        ResponseEntity.ok(WebAuthnVerificationResponse(success = false, message = e.message))

    @ExceptionHandler(CredentialNotFoundException::class)
    fun handleCredentialNotFound(e: CredentialNotFoundException): ResponseEntity<WebAuthnVerificationResponse> =
        ResponseEntity.ok(WebAuthnVerificationResponse(success = false, message = e.message))

    @ExceptionHandler(SignCounterRegressionException::class)
    fun handleSignCounterRegression(e: SignCounterRegressionException): ResponseEntity<WebAuthnVerificationResponse> =
        ResponseEntity.ok(WebAuthnVerificationResponse(success = false, message = e.message))

    @ExceptionHandler(UserHandleMismatchException::class)
    fun handleUserHandleMismatch(e: UserHandleMismatchException): ResponseEntity<WebAuthnVerificationResponse> =
        ResponseEntity.ok(WebAuthnVerificationResponse(success = false, message = e.message))

    @ExceptionHandler(AuthenticationVerificationException::class)
    fun handleVerificationFailed(e: AuthenticationVerificationException): ResponseEntity<WebAuthnVerificationResponse> =
        ResponseEntity.ok(WebAuthnVerificationResponse(success = false, message = e.message))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<WebAuthnVerificationResponse> =
        ResponseEntity.badRequest().body(WebAuthnVerificationResponse(success = false, message = e.message))
}
