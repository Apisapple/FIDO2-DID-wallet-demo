package com.raonsecure.demoserver.common.web

import com.raonsecure.demoserver.common.data.WebAuthnVerificationResponse
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Answers a request whose body could not be read with the same envelope as any other handled
 * failure, so a malformed payload does not fall through to the framework's error page.
 *
 * Application-wide on purpose: an unreadable body is a transport-level failure identical for every
 * endpoint, so it is handled once here rather than duplicated in each per-package advice.
 */
@RestControllerAdvice
class MalformedRequestExceptionHandler {
    /**
     * The parser's own message names the failing class and field and carries the offending bytes,
     * so it is deliberately discarded in favour of a fixed message. A client cannot act on the
     * internal detail, and echoing it would expose the DTO's shape.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(e: HttpMessageNotReadableException): ResponseEntity<WebAuthnVerificationResponse> =
        ResponseEntity.badRequest().body(
            WebAuthnVerificationResponse(
                success = false,
                message = "Request body could not be read: expected a JSON object with the required fields",
            ),
        )
}
