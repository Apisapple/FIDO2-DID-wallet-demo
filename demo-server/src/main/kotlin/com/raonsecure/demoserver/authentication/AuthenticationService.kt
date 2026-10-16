package com.raonsecure.demoserver.authentication

import com.raonsecure.demoserver.authentication.challenge.PendingAuthenticationContext
import com.raonsecure.demoserver.authentication.exception.AuthenticationChallengeNotFoundException
import com.raonsecure.demoserver.authentication.exception.AuthenticationVerificationException
import com.raonsecure.demoserver.authentication.exception.CredentialNotFoundException
import com.raonsecure.demoserver.authentication.exception.SignCounterRegressionException
import com.raonsecure.demoserver.authentication.exception.UserHandleMismatchException
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
import com.webauthn4j.converter.CollectedClientDataConverter
import com.webauthn4j.converter.util.ObjectConverter
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
import com.webauthn4j.verifier.exception.MaliciousCounterValueException
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

    /** Reads clientDataJSON on its own, ahead of assertion parsing, to consume the challenge. */
    private val collectedClientDataConverter = CollectedClientDataConverter(ObjectConverter())

    fun createOptions(request: AuthenticationOptionsRequest): AuthenticationDataResponse {
        val username = request.username?.trim()
        val matchingCredentials: List<UserKeyEntity> =
            if (!username.isNullOrEmpty()) {
                val creds = userKeyRepository.findAllByUsername(username)
                if (creds.isEmpty()) {
                    throw CredentialNotFoundException("No credentials registered for user: $username")
                }
                creds
            } else {
                emptyList()
            }

        val allowCredentials =
            matchingCredentials
                .map { cred ->
                    PublicKeyCredentialDescriptor(
                        id = cred.credentialId,
                        transports = cred.transports.map { it.transport }.ifEmpty { null },
                    )
                }.ifEmpty { null }

        val challengeBytes = ByteArray(32).also { random.nextBytes(it) }
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes)

        // One value drives both the advertised requirement and the stored policy, so
        // verification cannot enforce something different from what the client was told.
        val userVerification = request.userVerification ?: UserVerificationRequirement.PREFERRED.value

        val context =
            PendingAuthenticationContext(
                username = username,
                challenge = challenge,
                credentialIds = matchingCredentials.map { it.credentialId },
                userVerification = userVerification,
                usernameProvided = !username.isNullOrEmpty(),
            )
        challengeStore.saveAuthentication(challenge, context, challengeTtl)

        return AuthenticationDataResponse(
            challenge = challenge,
            rpId = rpProperties.id,
            allowCredentials = allowCredentials,
            timeout = 60_000L,
            userVerification = userVerification,
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

        val authRequest =
            AuthenticationRequest(
                credentialIdBytes,
                userHandleBytes,
                authDataBytes,
                clientDataBytes,
                signatureBytes,
            )

        // The challenge is read and consumed before the assertion is parsed, so a verification
        // attempt that fails during parsing still burns the challenge. Reading it from the parsed
        // result instead would leave a replayable challenge behind whenever parsing threw.
        val collectedClientData =
            try {
                collectedClientDataConverter.convert(clientDataBytes)
            } catch (e: Exception) {
                throw AuthenticationVerificationException("Failed to parse clientDataJSON: ${e.message}", e)
            }

        val challengeValue =
            collectedClientData?.challenge?.value
                ?: throw AuthenticationVerificationException("Missing challenge in clientData")
        val challengeStr = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeValue)

        val pendingCtx =
            challengeStore.consumeAuthentication(challengeStr)
                ?: throw AuthenticationChallengeNotFoundException(challengeStr)

        try {
            webAuthnManager.parse(authRequest)
        } catch (e: Exception) {
            throw AuthenticationVerificationException("Failed to parse assertion: ${e.message}", e)
        }

        val userKey =
            userKeyRepository.findByCredentialId(credentialIdRaw)
                ?: throw CredentialNotFoundException(credentialIdRaw)

        if (pendingCtx.credentialIds.isNotEmpty() && !pendingCtx.credentialIds.contains(credentialIdRaw)) {
            throw AuthenticationVerificationException("Credential ID $credentialIdRaw is not in allowed credentials")
        }

        // Bind the asserted user handle to the credential. webauthn4j cannot do this for us: the
        // CredentialRecordImpl built below carries no user entity to compare against.
        val assertedUserHandle = result.response.userHandle
        if (assertedUserHandle != null) {
            if (assertedUserHandle != userKey.userId) {
                throw UserHandleMismatchException(credentialIdRaw)
            }
        } else if (!pendingCtx.usernameProvided) {
            // Nothing else identifies the user in a usernameless ceremony.
            throw AuthenticationVerificationException(
                "Assertion for a usernameless ceremony must carry a userHandle",
            )
        }

        val coseKey = restoreCoseKey(userKey)
        val aaguid = if (userKey.aaguid.isNotBlank()) AAGUID(userKey.aaguid) else AAGUID.ZERO
        val attestedCredentialData = AttestedCredentialData(aaguid, credentialIdBytes, coseKey)
        val credentialRecord =
            CredentialRecordImpl(
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

        val serverProperty =
            ServerProperty
                .builder()
                .origins(rpProperties.origins.map { Origin.create(it) }.toSet())
                .rpId(rpProperties.id)
                .challenge(DefaultChallenge(Base64.getUrlDecoder().decode(pendingCtx.challenge)))
                .build()

        // User presence is always required; user verification is required only when the options
        // advertised it. Parameter order is (serverProperty, credentialRecord, allowCredentials,
        // userVerificationRequired, userPresenceRequired).
        val authParameters =
            AuthenticationParameters(
                serverProperty,
                credentialRecord,
                null,
                pendingCtx.userVerification == UserVerificationRequirement.REQUIRED.value,
                true,
            )

        val authData =
            try {
                webAuthnManager.verify(authRequest, authParameters)
            } catch (e: MaliciousCounterValueException) {
                // Distinguishable from other verification failures: a counter that did not advance
                // suggests a cloned or replayed authenticator rather than a malformed assertion.
                throw SignCounterRegressionException(e.storedCounter, e.presentedCounter)
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
