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
import com.raonsecure.demoserver.config.WebAuthnRpProperties
import com.raonsecure.demoserver.registration.challenge.ChallengeStore
import com.raonsecure.demoserver.registration.challenge.PendingRegistrationContext
import com.raonsecure.demoserver.registration.exception.ChallengeNotFoundException
import com.raonsecure.demoserver.registration.exception.DuplicateCredentialException
import com.raonsecure.demoserver.registration.exception.RegistrationVerificationException
import com.webauthn4j.WebAuthnManager
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.data.RegistrationRequest
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

        val context = PendingRegistrationContext(
            username = request.username,
            userId = userId,
            userDisplayName = displayName,
            challenge = challenge,
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
            authenticatorSelection = request.authenticatorSelection
                ?: AuthenticatorSelectionCriteria(),
            attestation = request.attestation ?: "none",
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

        val registrationData = try {
            webAuthnManager.verify(
                RegistrationRequest(attestationBytes, clientDataBytes, null, transports),
                RegistrationParameters(serverProperty, null, false, false),
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
            .attestationType(AttestationType.NONE)
            .registeredTimestamp(Date())
            .build()

        userKeyRepository.save(entity)

        transportEntities.forEach { it.link(entity) }
        if (transportEntities.isNotEmpty()) {
            userKeyRepository.save(entity.apply { this.transports = transportEntities })
        }
    }

    private fun resolveUserId(username: String): String {
        val existing = userKeyRepository.findAllByUsername(username)
        if (existing.isNotEmpty()) return existing.first().userId
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(username.toByteArray(Charsets.UTF_8))
    }
}
