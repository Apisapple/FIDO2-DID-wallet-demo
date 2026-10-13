package com.raonsecure.demoserver.testsupport

import com.raonsecure.demoserver.authentication.AuthenticationService
import com.raonsecure.demoserver.base.entity.RpEntity
import com.raonsecure.demoserver.base.entity.UserKeyEntity
import com.raonsecure.demoserver.base.repository.RpRepository
import com.raonsecure.demoserver.common.data.AuthenticationOptionsRequest
import com.raonsecure.demoserver.common.data.AuthenticatorAssertionResponseDto
import com.raonsecure.demoserver.common.data.AuthenticatorAttestationResponseDto
import com.raonsecure.demoserver.common.data.PublicKeyCredentialCreateResultDto
import com.raonsecure.demoserver.common.data.PublicKeyCredentialGetResultDto
import com.raonsecure.demoserver.common.data.RegistrationOptionsRequest
import com.raonsecure.demoserver.config.WebAuthnRpProperties
import com.raonsecure.demoserver.registration.RegistrationService
import com.webauthn4j.WebAuthnManager
import com.webauthn4j.data.UserVerificationRequirement
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional

/**
 * Wires the real [RegistrationService] and [AuthenticationService] against a real
 * [WebAuthnManager] and in-memory persistence, so a test can run a whole ceremony through the
 * same code the controllers call.
 */
class CeremonyHarness(
    val origin: String = CeremonyFixture.DEFAULT_ORIGIN,
    val rpId: String = CeremonyFixture.DEFAULT_RP_ID,
) {
    val credentials: MutableList<UserKeyEntity> = mutableListOf()
    val challengeStore = InMemoryChallengeStore()
    val fixture = CeremonyFixture(origin = origin, rpId = rpId)

    private val rp = RpEntity(id = rpId, name = "Fixture RP")
    private val rpRepository: RpRepository = mock<RpRepository>().also {
        whenever(it.findById(rpId)).thenReturn(Optional.of(rp))
    }
    private val userKeyRepository = fakeUserKeyRepository(credentials)
    private val rpProperties = WebAuthnRpProperties(id = rpId, name = "Fixture RP", origin = origin)
    private val webAuthnManager: WebAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager()

    val registrationService = RegistrationService(
        rpProperties, rpRepository, userKeyRepository, challengeStore, webAuthnManager,
    )
    val authenticationService = AuthenticationService(
        rpProperties, userKeyRepository, challengeStore, webAuthnManager,
    )

    /** Issues options, runs the emulator's create ceremony, and verifies the result. */
    fun register(
        username: String = "alice",
        userVerification: UserVerificationRequirement = UserVerificationRequirement.PREFERRED,
    ): RegistrationFixtureResult {
        val options = registrationService.createOptions(
            RegistrationOptionsRequest(
                username = username,
                authenticatorSelection = com.raonsecure.demoserver.common.data.AuthenticatorSelectionCriteria(
                    userVerification = userVerification.value,
                ),
            ),
        )
        val created = fixture.register(
            challenge = options.challenge,
            username = username,
            userHandle = java.util.Base64.getUrlDecoder().decode(options.user.id),
            userVerification = userVerification,
        )
        registrationService.verify(created.toDto())
        return created
    }

    /** Issues options, runs the emulator's get ceremony, and verifies the assertion. */
    fun authenticate(
        credentialId: String,
        username: String? = "alice",
        userVerification: UserVerificationRequirement = UserVerificationRequirement.PREFERRED,
    ): AuthenticationFixtureResult {
        val options = authenticationService.createOptions(
            AuthenticationOptionsRequest(username = username, userVerification = userVerification.value),
        )
        val asserted = fixture.authenticate(
            challenge = options.challenge,
            credentialId = credentialId,
            userVerification = userVerification,
        )
        authenticationService.verify(asserted.toDto())
        return asserted
    }
}

fun RegistrationFixtureResult.toDto() = PublicKeyCredentialCreateResultDto(
    id = credentialId,
    rawId = credentialId,
    response = AuthenticatorAttestationResponseDto(
        clientDataJSON = clientDataJSON,
        attestationObject = attestationObject,
    ),
)

fun AuthenticationFixtureResult.toDto(
    overrideUserHandle: String? = userHandle,
) = PublicKeyCredentialGetResultDto(
    id = credentialId,
    rawId = credentialId,
    response = AuthenticatorAssertionResponseDto(
        clientDataJSON = clientDataJSON,
        authenticatorData = authenticatorData,
        signature = signature,
        userHandle = overrideUserHandle,
    ),
)
