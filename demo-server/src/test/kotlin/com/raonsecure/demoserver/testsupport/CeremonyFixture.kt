package com.raonsecure.demoserver.testsupport

import com.webauthn4j.data.AuthenticatorAssertionResponse
import com.webauthn4j.data.AuthenticatorAttestationResponse
import com.webauthn4j.data.AuthenticatorSelectionCriteria
import com.webauthn4j.data.PublicKeyCredential
import com.webauthn4j.data.PublicKeyCredentialCreationOptions
import com.webauthn4j.data.PublicKeyCredentialDescriptor
import com.webauthn4j.data.PublicKeyCredentialParameters
import com.webauthn4j.data.PublicKeyCredentialRequestOptions
import com.webauthn4j.data.PublicKeyCredentialRpEntity
import com.webauthn4j.data.PublicKeyCredentialType
import com.webauthn4j.data.PublicKeyCredentialUserEntity
import com.webauthn4j.data.ResidentKeyRequirement
import com.webauthn4j.data.UserVerificationRequirement
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.ClientDataType
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.data.extension.client.AuthenticationExtensionClientOutput
import com.webauthn4j.data.extension.client.RegistrationExtensionClientOutput
import com.webauthn4j.test.EmulatorUtil
import com.webauthn4j.test.authenticator.webauthn.WebAuthnAuthenticatorAdaptor
import com.webauthn4j.test.authenticator.webauthn.WebAuthnModelAuthenticator
import com.webauthn4j.test.client.ClientPlatform
import java.util.Base64

/**
 * Drives real WebAuthn ceremonies against webauthn4j's authenticator emulator, so tests can
 * exercise the parse/verify path instead of mocking [com.webauthn4j.WebAuthnManager].
 *
 * The emulator's `makeCredential`/`getAssertion` take explicit user-presence and
 * user-verification booleans, which [ClientPlatform] derives from the options it is given.
 * UP is driven by the authenticator-selection / request options the caller passes here.
 */
class CeremonyFixture(
    val origin: String = DEFAULT_ORIGIN,
    val rpId: String = DEFAULT_RP_ID,
    authenticator: WebAuthnModelAuthenticator = EmulatorUtil.PACKED_AUTHENTICATOR,
) {
    private val urlEncoder = Base64.getUrlEncoder().withoutPadding()
    private val urlDecoder = Base64.getUrlDecoder()

    val clientPlatform: ClientPlatform =
        ClientPlatform(Origin.create(origin), WebAuthnAuthenticatorAdaptor(authenticator))

    /** Runs the create ceremony and returns the raw pieces a `/attestation/result` body carries. */
    fun register(
        challenge: String,
        username: String = "alice",
        userHandle: ByteArray = randomBytes(32),
        userVerification: UserVerificationRequirement = UserVerificationRequirement.PREFERRED,
        residentKey: ResidentKeyRequirement? = null,
    ): RegistrationFixtureResult {
        val options = PublicKeyCredentialCreationOptions(
            PublicKeyCredentialRpEntity(rpId, "Fixture RP"),
            PublicKeyCredentialUserEntity(userHandle, username, username),
            DefaultChallenge(urlDecoder.decode(challenge)),
            listOf(
                PublicKeyCredentialParameters(
                    PublicKeyCredentialType.PUBLIC_KEY,
                    COSEAlgorithmIdentifier.ES256,
                ),
            ),
            null,
            null,
            AuthenticatorSelectionCriteria(null, residentKey, userVerification),
            null,
            null,
        )

        val credential: PublicKeyCredential<AuthenticatorAttestationResponse, RegistrationExtensionClientOutput> =
            clientPlatform.create(options)

        val response = credential.response!!
        return RegistrationFixtureResult(
            credentialId = urlEncoder.encodeToString(credential.rawId),
            clientDataJSON = urlEncoder.encodeToString(response.clientDataJSON),
            attestationObject = urlEncoder.encodeToString(response.attestationObject),
            userHandle = urlEncoder.encodeToString(userHandle),
        )
    }

    /** Runs the get ceremony and returns the raw pieces an `/assertion/result` body carries. */
    fun authenticate(
        challenge: String,
        credentialId: String,
        userVerification: UserVerificationRequirement = UserVerificationRequirement.PREFERRED,
    ): AuthenticationFixtureResult {
        val options = PublicKeyCredentialRequestOptions(
            DefaultChallenge(urlDecoder.decode(challenge)),
            null,
            rpId,
            listOf(
                PublicKeyCredentialDescriptor(
                    PublicKeyCredentialType.PUBLIC_KEY,
                    urlDecoder.decode(credentialId),
                    null,
                ),
            ),
            userVerification,
            null,
        )

        val credential: PublicKeyCredential<AuthenticatorAssertionResponse, AuthenticationExtensionClientOutput> =
            clientPlatform.get(options)
        val response = credential.response!!

        return AuthenticationFixtureResult(
            credentialId = urlEncoder.encodeToString(credential.rawId),
            clientDataJSON = urlEncoder.encodeToString(response.clientDataJSON),
            authenticatorData = urlEncoder.encodeToString(response.authenticatorData),
            signature = urlEncoder.encodeToString(response.signature),
            userHandle = response.userHandle?.let { urlEncoder.encodeToString(it) },
        )
    }

    fun collectedClientData(type: ClientDataType, challenge: String) =
        clientPlatform.createCollectedClientData(type, DefaultChallenge(urlDecoder.decode(challenge)))

    fun newChallenge(): String = urlEncoder.encodeToString(randomBytes(32))

    private fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also { java.security.SecureRandom().nextBytes(it) }

    companion object {
        const val DEFAULT_RP_ID = "localhost"
        const val DEFAULT_ORIGIN = "http://localhost:8080"
        const val ANDROID_ORIGIN = "android:apk-key-hash:pNiP5iKyQ8JwgGOaKA1zGPUPJIS-0H1xKCQcfIoGLck"
    }
}

data class RegistrationFixtureResult(
    val credentialId: String,
    val clientDataJSON: String,
    val attestationObject: String,
    val userHandle: String,
)

data class AuthenticationFixtureResult(
    val credentialId: String,
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
    val userHandle: String?,
)
