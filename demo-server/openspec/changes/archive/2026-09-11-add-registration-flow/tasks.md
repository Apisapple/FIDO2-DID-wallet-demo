## 1. Configuration & RP bootstrap

- [x] 1.1 Add a `webauthn.rp` block (`id`, `name`, `origin`) to `application.yml` and bind it via a `@ConfigurationProperties` class (`WebAuthnRpProperties`); verify the app boots and the properties are injectable (context loads test passes).
- [x] 1.2 Add a startup seeder (`ApplicationRunner`) that ensures an `RpEntity` for the configured `id` exists via `RpRepository`; verify by starting the app and confirming the RP row is present (H2 console or a repository lookup in an integration test).

## 2. Challenge store (Redis)

- [x] 2.1 Define a `ChallengeStore` interface (`save(challenge, context, ttl)`, `consume(challenge): context?`) and a serializable pending-registration context (username, userId/userHandle, requested options); verify it compiles.
- [x] 2.2 Implement `RedisChallengeStore` backed by `StringRedisTemplate` with key `reg:challenge:{challenge}`, TTL, and consume = read-then-delete (single use); verify with an integration test against embedded Redis that a saved challenge is retrievable once and gone after consume.

## 3. Registration options endpoint

- [x] 3.1 Implement `RegistrationService.createOptions(request)` — generate a ≥16-byte random base64url challenge, resolve/generate the user handle for the username, build `RegistrationDataResponse` (RP, user, `pubKeyCredParams` incl. ES256, `excludeCredentials` from existing credentials), and save the challenge context to the store; verify with a unit test asserting a unique challenge and correct `excludeCredentials`.
- [x] 3.2 Add `AttestationController.options` mapped to `POST /attestation/options` returning the options wrapped in a `status: "ok"` envelope; verify with a MockMvc test that a request with a username returns 200 and a base64url challenge.
- [x] 3.3 Handle the missing-username case with a `status: "failed"` envelope and no challenge generated; verify with a MockMvc test.

## 4. Registration result endpoint

- [x] 4.1 Configure a webauthn4j `WebAuthnManager` bean (non-strict) and implement `RegistrationService.verify(result)` — consume the stored challenge, build `ServerProperty` (configured origin + rpId + challenge), run webauthn4j verification for `attestation=none`; verify with a unit test using a known-good attestation fixture that verification succeeds.
- [x] 4.2 On success, persist a `UserKeyEntity` (credentialId, publicKey, signatureAlgorithm, signCounter, aaguid, `attestationType=NONE`, transports, `registeredTimestamp`) associated with the RP and user handle; verify the credential is saved and retrievable by `findByCredentialId`.
- [x] 4.3 Add `AttestationController.result` mapped to `POST /attestation/result` returning `WebAuthnVerificationResponse` in a `status: "ok"` envelope on success; verify with a MockMvc test using a fixture that a valid result returns 200 `status: "ok"`.
- [x] 4.4 Reject unknown/expired challenge, origin/RP-ID mismatch, and duplicate credential with `status: "failed"` and a non-empty `errorMessage`, persisting nothing; verify with MockMvc/unit tests for each failure case.
- [x] 4.5 Ensure the challenge is consumed (single-use) on any verification attempt; verify with a test that replaying the same challenge is rejected.

## 5. Error handling & wiring

- [x] 5.1 Add domain exceptions (challenge-not-found, verification-failed, duplicate-credential) and a `@RestControllerAdvice` mapping them to the `status: "failed"` envelope; verify with a MockMvc test that a thrown exception yields the expected envelope.
- [x] 5.2 Run `./gradlew test` and confirm the full suite (existing + new registration tests) passes.
