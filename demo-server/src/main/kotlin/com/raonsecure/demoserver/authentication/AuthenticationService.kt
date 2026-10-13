package com.raonsecure.demoserver.authentication

import com.raonsecure.demoserver.authentication.challenge.PendingAuthenticationContext
import com.raonsecure.demoserver.authentication.exception.AuthenticationChallengeNotFoundException
import com.raonsecure.demoserver.authentication.exception.AuthenticationVerificationException
import com.raonsecure.demoserver.authentication.exception.CredentialNotFoundException
import com.raonsecure.demoserver.base.entity.UserKeyEntity
import com.raonsecure.demoserver.base.repository.UserKeyRepository
import com.raonsecure.demoserver.common.data.AuthenticationDataResponse
import com.raonsecure.demoserver.common.data.AuthenticationOptionsRequest
import com.raonsecure.demoserver.common.data.PublicKeyCredentialDescriptor
import com.raonsecure.demoserver.common.data.PublicKeyCredentialGetResultDto
import com.raonsecure.demoserver.common.data.UserVerificationRequirement
import com.raonsecure.demoserver.config.WebAuthnRpProperties
import com.raonsecure.demoserver.registration.challenge.ChallengeStore
import com.webauthn4j.WebAuthnManager
import com.webauthn4j.credential.CredentialRecordImpl
import com.webauthn4j.data.AuthenticationParameters
import com.webauthn4j.data.AuthenticationRequest
import com.webauthn4j.data.attestation.authenticator.AAGUID
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData
import com.webauthn4j.data.attestation.authenticator.COSEKey
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey
import com.webauthn4j.data.attestation.authenticator.RSACOSEKey
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.Duration
import java.util.Base64
import java.util.Date

@Service
class AuthenticationService(
    private val rpProperties: WebAuthnRpProperties,
    private val userKeyRepository: UserKeyRepository,
    private val challengeStore: ChallengeStore,
    private val webAuthnManager: WebAuthnManager,
) {
    private val random = SecureRandom()
    private val challengeTtl = Duration.ofSeconds(60)

    fun createOptions(request: AuthenticationOptionsRequest): AuthenticationDataResponse {
        val username = request.username?.trim()
        val matchingCredentials: List<UserKeyEntity> = if (!username.isNullOrEmpty()) {
            val creds = userKeyRepository.findAllByUsername(username)
            if (creds.isEmpty()) {
                throw CredentialNotFoundException("No credentials registered for user: $username")
            }
            creds
        } else {
            emptyList()
        }

        val allowCredentials = matchingCredentials.map { cred ->
            PublicKeyCredentialDescriptor(
                id = cred.credentialId,
                transports = cred.transports.map { it.transport }.ifEmpty { null },
            )
        }.ifEmpty { null }

        val challengeBytes = ByteArray(32).also { random.nextBytes(it) }
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes)

        val context = PendingAuthenticationContext(
            username = username,
            challenge = challenge,
            credentialIds = matchingCredentials.map { it.credentialId },
        )
        challengeStore.saveAuthentication(challenge, context, challengeTtl)

        return AuthenticationDataResponse(
            challenge = challenge,
            rpId = rpProperties.id,
            allowCredentials = allowCredentials,
            timeout = 60_000L,
            userVerification = request.userVerification ?: UserVerificationRequirement.PREFERRED.value,
        )
    }

    @Transactional
    fun verify(result: PublicKeyCredentialGetResultDto) {
        val credentialIdRaw = result.rawId ?: result.id
        val credentialIdBytes = Base64.getUrlDecoder().decode(credentialIdRaw)

        val clientDataBytes = Base64.getUrlDecoder().decode(result.response.clientDataJSON)
        val authDataBytes = Base64.getUrlDecoder().decode(result.response.authenticatorData)
        val signatureBytes = Base64.getUrlDecoder().decode(result.response.signature)
        val userHandleBytes = result.response.userHandle?.let { Base64.getUrlDecoder().decode(it) }

        val authRequest = AuthenticationRequest(
            credentialIdBytes,
            userHandleBytes,
            authDataBytes,
            clientDataBytes,
            signatureBytes,
        )

        val parsed = try {
            webAuthnManager.parse(authRequest)
        } catch (e: Exception) {
            throw AuthenticationVerificationException("Failed to parse assertion: ${e.message}", e)
        }

        val challengeBytes = parsed.collectedClientData?.challenge?.value
            ?: throw AuthenticationVerificationException("Missing challenge in clientData")
        val challengeStr = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes)

        val pendingCtx = challengeStore.consumeAuthentication(challengeStr)
            ?: throw AuthenticationChallengeNotFoundException(challengeStr)

        val userKey = userKeyRepository.findByCredentialId(credentialIdRaw)
            ?: throw CredentialNotFoundException(credentialIdRaw)

        if (pendingCtx.credentialIds.isNotEmpty() && !pendingCtx.credentialIds.contains(credentialIdRaw)) {
            throw AuthenticationVerificationException("Credential ID $credentialIdRaw is not in allowed credentials")
        }

        val coseKey = restoreCoseKey(userKey)
        val aaguid = if (userKey.aaguid.isNotBlank()) AAGUID(userKey.aaguid) else AAGUID.ZERO
        val attestedCredentialData = AttestedCredentialData(aaguid, credentialIdBytes, coseKey)
        val credentialRecord = CredentialRecordImpl(
            null,
            null,
            null,
            null,
            userKey.signCounter ?: 0L,
            attestedCredentialData,
            null,
            null,
            null,
            null,
        )

        val serverProperty = ServerProperty.builder()
            .origins(rpProperties.origins.map { Origin.create(it) }.toSet())
            .rpId(rpProperties.id)
            .challenge(DefaultChallenge(Base64.getUrlDecoder().decode(pendingCtx.challenge)))
            .build()

        val authParameters = AuthenticationParameters(
            serverProperty,
            credentialRecord,
            null,
            false,
            true,
        )

        val authData = try {
            webAuthnManager.verify(authRequest, authParameters)
        } catch (e: Exception) {
            throw AuthenticationVerificationException("Assertion verification failed: ${e.message}", e)
        }

        val newSignCount = authData.authenticatorData?.signCount ?: 0L
        userKey.signCounter = newSignCount
        userKey.authenticatedTimestamp = Date()
        userKeyRepository.save(userKey)
    }

    private fun restoreCoseKey(userKey: UserKeyEntity): COSEKey {
        val keyBytes = Base64.getDecoder().decode(userKey.publicKey)
        return when (userKey.signatureAlgorithm) {
            -7 -> {
                val kf = KeyFactory.getInstance("EC")
                val pubKey = kf.generatePublic(X509EncodedKeySpec(keyBytes)) as ECPublicKey
                EC2COSEKey.create(pubKey, COSEAlgorithmIdentifier.create(-7L))
            }
            -257 -> {
                val kf = KeyFactory.getInstance("RSA")
                val pubKey = kf.generatePublic(X509EncodedKeySpec(keyBytes)) as RSAPublicKey
                RSACOSEKey.create(pubKey, COSEAlgorithmIdentifier.create(-257L))
            }
            else -> throw AuthenticationVerificationException("Unsupported algorithm: ${userKey.signatureAlgorithm}")
        }
    }
}
