package com.raonsecure.demoserver.authentication

import com.raonsecure.demoserver.common.data.AuthenticationDataResponse
import com.raonsecure.demoserver.common.data.AuthenticationOptionsRequest
import com.raonsecure.demoserver.common.data.PublicKeyCredentialGetResultDto
import com.raonsecure.demoserver.common.data.WebAuthnVerificationResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/assertion")
class AuthenticationController(
    private val authenticationService: AuthenticationService,
) {
    @PostMapping("/options")
    fun options(
        @RequestBody(required = false) request: AuthenticationOptionsRequest?,
    ): ResponseEntity<Any> {
        val req = request ?: AuthenticationOptionsRequest()
        val options = authenticationService.createOptions(req)
        return ResponseEntity.ok(FidoAssertionOptionsResponse(status = "ok", options = options))
    }

    @PostMapping("/result")
    fun result(
        @RequestBody result: PublicKeyCredentialGetResultDto,
    ): ResponseEntity<Any> {
        authenticationService.verify(result)
        return ResponseEntity.ok(
            WebAuthnVerificationResponse(success = true, message = "Authentication successful"),
        )
    }
}

data class FidoAssertionOptionsResponse(
    val status: String,
    val errorMessage: String = "",
    val options: AuthenticationDataResponse,
)
