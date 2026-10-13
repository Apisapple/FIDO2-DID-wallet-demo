package com.raonsecure.demoserver.registration

import com.raonsecure.demoserver.common.data.PublicKeyCredentialCreateResultDto
import com.raonsecure.demoserver.common.data.RegistrationDataResponse
import com.raonsecure.demoserver.common.data.RegistrationOptionsRequest
import com.raonsecure.demoserver.common.data.WebAuthnVerificationResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/attestation")
class RegistrationController(private val registrationService: RegistrationService) {

    @PostMapping("/options")
    fun options(@RequestBody request: RegistrationOptionsRequest): ResponseEntity<Any> {
        if (request.username.isBlank()) {
            return ResponseEntity.badRequest().body(
                WebAuthnVerificationResponse(success = false, message = "username is required")
            )
        }
        val options = registrationService.createOptions(request)
        return ResponseEntity.ok(FidoOptionsResponse(status = "ok", options = options))
    }

    @PostMapping("/result")
    fun result(@RequestBody result: PublicKeyCredentialCreateResultDto): ResponseEntity<Any> {
        registrationService.verify(result)
        return ResponseEntity.ok(
            WebAuthnVerificationResponse(success = true, message = "Registration successful")
        )
    }
}

data class FidoOptionsResponse(
    val status: String,
    val errorMessage: String = "",
    val options: RegistrationDataResponse,
)
