package com.raonsecure.demoserver.registration

import com.raonsecure.demoserver.base.entity.AttestationType
import com.raonsecure.demoserver.base.entity.AuthenticatorTransportEntity
import com.raonsecure.demoserver.base.entity.UserKeyEntity
import com.raonsecure.demoserver.base.repository.RpRepository
import com.raonsecure.demoserver.base.repository.UserKeyRepository
import com.raonsecure.demoserver.common.data.AuthenticatorSelectionCriteria
import com.raonsecure.demoserver.common.data.PublicKeyCredentialCreateResultDto
import com.raonsecure.demoserver.common.data.PublicKeyCredentialDescriptor
import com.raonsecure.demoserver.common.data.PublicKeyCredentialParams
import com.raonsecure.demoserver.common.data.PublicKeyCredentialRpEntity
import com.raonsecure.demoserver.common.data.PublicKeyCredentialUserEntity
import com.raonsecure.demoserver.common.data.RegistrationDataResponse
import com.raonsecure.demoserver.common.data.RegistrationOptionsRequest
import com.raonsecure.demoserver.common.data.UserVerificationRequirement
import com.raonsecure.demoserver.config.WebAuthnRpProperties
import com.raonsecure.demoserver.registration.challenge.ChallengeStore
import com.raonsecure.demoserver.registration.challenge.PendingRegistrationContext
import com.raonsecure.demoserver.registration.exception.ChallengeNotFoundException
import com.raonsecure.demoserver.registration.exception.DuplicateCredentialException
import com.raonsecure.demoserver.registration.exception.RegistrationVerificationException
import com.webauthn4j.WebAuthnManager
import com.webauthn4j.data.RegistrationData
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.data.RegistrationRequest
import com.webauthn4j.data.attestation.statement.AppleAnonymousAttestationStatement
import com.webauthn4j.data.attestation.statement.CertificateBaseAttestationStatement
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64
import java.util.Date

@Service
class RegistrationService(
    private val rpProperties: WebAuthnRpProperties,
    private val rpRepository: RpRepository,
    private val userKeyRepository: UserKeyRepository,
    private val challengeStore: ChallengeStore,
    private val webAuthnManager: WebAuthnManager,
) {
    private val random = SecureRandom()
    private val challengeTtl = Duration.ofSeconds(60)

    fun createOptions(request: RegistrationOptionsRequest): RegistrationDataResponse {
        val rp = rpRepository.findById(rpProperties.id).orElseThrow {
            IllegalStateException("RP not configured: ${rpProperties.id}")
        }

        val challengeBytes = ByteArray(32).also { random.nextBytes(it) }
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes)

        val userId = resolveUserId(request.username)
        val displayName = request.displayName ?: request.username

        // One value drives both the advertised selection criteria and the stored policy, so
        // verification cannot enforce something different from what the client was told.
        val authenticatorSelection = request.authenticatorSelection ?: AuthenticatorSelectionCriteria()
        val userVerification = authenticatorSelection.userVerification
            ?: UserVerificationRequirement.PREFERRED.value

        val context = PendingRegistrationContext(
            username = request.username,
            userId = userId,
            userDisplayName = displayName,
            challenge = challenge,
            userVerification = userVerification,
        )
        challengeStore.save(challenge, context, challengeTtl)

        val existingCredentials = userKeyRepository.findAllByUsername(request.username)
            .map { PublicKeyCredentialDescriptor(id = it.credentialId) }
            .ifEmpty { null }

        return RegistrationDataResponse(
            rp = PublicKeyCredentialRpEntity(id = rp.id, name = rp.name),
            user = PublicKeyCredentialUserEntity(
                id = userId,
                displayName = displayName,
                name = request.username,
            ),
            challenge = challenge,
            pubKeyCredParams = listOf(
                PublicKeyCredentialParams(alg = -7L),   // ES256
                PublicKeyCredentialParams(alg = -257L), // RS256
            ),
            timeout = 60_000L,
            excludeCredentials = existingCredentials,
            authenticatorSelection = authenticatorSelection.copy(userVerification = userVerification),
            attestation = request.attestation ?: "none",
            // Ask the client to report whether it made a discoverable credential. No
            // authenticator-data flag conveys this, and a client may legitimately omit the output.
            extensions = mapOf(CRED_PROPS_EXTENSION to true),
        )
    }

    @Transactional
    fun verify(result: PublicKeyCredentialCreateResultDto) {
        val credentialIdRaw = result.rawId ?: result.id
        if (userKeyRepository.findByCredentialId(credentialIdRaw) != null) {
            throw DuplicateCredentialException(credentialIdRaw)
        }

        val clientDataBytes = Base64.getUrlDecoder().decode(result.response.clientDataJSON)
        val attestationBytes = Base64.getUrlDecoder().decode(result.response.attestationObject)
        val transports = result.response.transports?.toSet()

        val parsed = try {
            webAuthnManager.parse(
                RegistrationRequest(attestationBytes, clientDataBytes, null, transports)
            )
        } catch (e: Exception) {
            throw RegistrationVerificationException("Failed to parse attestation: ${e.message}", e)
        }

        val challengeBytes2 = parsed.collectedClientData?.challenge?.value
            ?: throw RegistrationVerificationException("Missing challenge in clientData")
        val challengeStr = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes2)

        val pendingCtx = challengeStore.consume(challengeStr)
            ?: throw ChallengeNotFoundException(challengeStr)

        val serverProperty = ServerProperty.builder()
            .origins(rpProperties.origins.map { Origin.create(it) }.toSet())
            .rpId(rpProperties.id)
            .challenge(DefaultChallenge(Base64.getUrlDecoder().decode(pendingCtx.challenge)))
            .build()

        // User presence is always required; user verification is required only when the options
        // advertised it. Parameter order is (serverProperty, pubKeyCredParams,
        // userVerificationRequired, userPresenceRequired).
        val userVerificationRequired =
            pendingCtx.userVerification == UserVerificationRequirement.REQUIRED.value

        val registrationData = try {
            webAuthnManager.verify(
                RegistrationRequest(attestationBytes, clientDataBytes, null, transports),
                RegistrationParameters(serverProperty, null, userVerificationRequired, true),
            )
        } catch (e: Exception) {
            throw RegistrationVerificationException("Attestation verification failed: ${e.message}", e)
        }

        val authData = registrationData.attestationObject?.authenticatorData
        val attestedData = authData?.attestedCredentialData
            ?: throw RegistrationVerificationException("Missing attested credential data")

        val coseKey = attestedData.coseKey
        val publicKeyBytes = coseKey.getPublicKey()?.encoded
            ?: throw RegistrationVerificationException("Cannot extract public key")

        val aaguid = attestedData.aaguid?.value?.toString() ?: ""
        val signCount = authData.signCount
        val alg = (coseKey.algorithm?.value ?: -7L).toInt()

        val rp = rpRepository.findById(rpProperties.id).orElseThrow {
            IllegalStateException("RP not found")
        }

        val transportEntities = transports?.map { AuthenticatorTransportEntity(it) }?.toMutableList()
            ?: mutableListOf()

        val entity = UserKeyEntity.builder()
            .rpEntity(rp)
            .userId(pendingCtx.userId)
            .username(pendingCtx.username)
            .userDisplayName(pendingCtx.userDisplayName)
            .aaguid(aaguid)
            .credentialId(credentialIdRaw)
            .publicKey(Base64.getEncoder().encodeToString(publicKeyBytes))
            .signatureAlgorithm(alg)
            .signCounter(signCount)
            .attestationType(resolveAttestationType(registrationData))
            .rk(resolveResidentKey(result))
            .registeredTimestamp(Date())
            .build()

        userKeyRepository.save(entity)

        transportEntities.forEach { it.link(entity) }
        if (transportEntities.isNotEmpty()) {
            userKeyRepository.save(entity.apply { this.transports = transportEntities })
        }
    }

    /**
     * A user handle must not be derivable from the username, so a new one is 32 random bytes.
     * A user who already has a credential keeps their handle, so all of one user's credentials
     * share a single handle.
     */
    private fun resolveUserId(username: String): String {
        val existing = userKeyRepository.findAllByUsername(username)
        if (existing.isNotEmpty()) return existing.first().userId
        val handle = ByteArray(USER_HANDLE_BYTES).also { random.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(handle)
    }

    /**
     * Derives the attestation type from the verified attestation statement rather than recording a
     * fixed value. A `packed`/`tpm`/`android-key` statement with no certificate chain is self
     * attestation; with one it is basic attestation (an RP cannot tell basic from attestation-CA
     * from the attestation data alone, which is why webauthn4j's own enum folds them together).
     * Trust-chain validation is out of scope, so no attempt is made to distinguish ATT_CA.
     */
    private fun resolveAttestationType(registrationData: RegistrationData): AttestationType {
        val statement = registrationData.attestationObject?.attestationStatement
            ?: return AttestationType.NONE

        return when (statement) {
            is NoneAttestationStatement -> AttestationType.NONE
            is AppleAnonymousAttestationStatement -> AttestationType.ANON_CA
            is CertificateBaseAttestationStatement ->
                if (statement.x5c == null) AttestationType.SELF else AttestationType.BASIC
            else -> AttestationType.BASIC
        }
    }

    /**
     * Reads the resident-key state the client reports in the `credProps` extension output.
     * Returns null -- unknown -- when the client returns no output, so a missing extension is not
     * read as a positive claim either way. The value is client-reported and unauthenticated;
     * nothing here makes an authorization decision from it.
     */
    private fun resolveResidentKey(result: PublicKeyCredentialCreateResultDto): Boolean? {
        val credProps = result.clientExtensionOutput?.get(CRED_PROPS_EXTENSION) as? Map<*, *>
            ?: return null
        return credProps["rk"] as? Boolean
    }

    private companion object {
        /** 32 bytes of entropy, well inside the WebAuthn 64-byte limit on user handles. */
        const val USER_HANDLE_BYTES = 32

        const val CRED_PROPS_EXTENSION = "credProps"
    }
}
