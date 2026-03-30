package com.example.demo.library.relyingparty

import com.example.demo.library.model.CredentialProtection
import com.example.demo.library.model.attestation.AttestationConveyancePreference
import com.example.demo.library.model.authenticator.AuthenticatorSelectionCriteria
import com.example.demo.library.model.authenticator.UserVerificationRequirement
import com.example.demo.library.model.publickeycredential.PublicKeyCredentialCreateResult
import com.example.demo.library.model.publickeycredential.PublicKeyCredentialDescriptor
import com.example.demo.library.model.publickeycredential.PublicKeyCredentialGetResult
import com.example.demo.library.model.publickeycredential.PublicKeyCredentialParams
import com.example.demo.library.model.publickeycredential.PublicKeyCredentialRpEntity
import com.example.demo.library.model.publickeycredential.PublicKeyCredentialUserEntity

interface RelyingParty {
    suspend fun getRegistrationData(options: RegistrationOptions): RegistrationData

    suspend fun verifyRegistration(result: PublicKeyCredentialCreateResult)

    suspend fun getAuthenticationData(options: AuthenticationOptions): AuthenticationData

    suspend fun verifyAuthentication(result: PublicKeyCredentialGetResult)
}

data class RegistrationOptions(
    val attestation: AttestationConveyancePreference,
    val authenticatorSelection: AuthenticatorSelectionCriteria?,
    val credProtect: CredentialProtection?,
    val displayName: String,
    val username: String,
)

data class RegistrationData(
    val attestation: AttestationConveyancePreference,
    val authenticatorSelection: AuthenticatorSelectionCriteria?,
    val challenge: String,
    val excludeCredentials: List<PublicKeyCredentialDescriptor>?,
    val extensions: ClientExtensionInput?,
    val pubKeyCredParams: List<PublicKeyCredentialParams>,
    val rp: PublicKeyCredentialRpEntity,
    val user: PublicKeyCredentialUserEntity,
)

data class AuthenticationOptions(
    val userVerification: UserVerificationRequirement,
    val username: String,
)

data class AuthenticationData(
    val allowCredentials: List<PublicKeyCredentialDescriptor>?,
    val challenge: String,
    val extensions: ClientExtensionInput?,
    val rpId: String,
    val userVerification: UserVerificationRequirement,
)
