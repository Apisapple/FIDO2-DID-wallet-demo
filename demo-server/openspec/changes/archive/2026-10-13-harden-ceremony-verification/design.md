# Design

## Context

See `proposal.md` — Why. Requirements live in `specs/`.

Constraints that shape the approach:

- **webauthn4j already implements every check.** `RegistrationParameters` and `AuthenticationParameters` both take `(…, userVerificationRequired, userPresenceRequired)`, and `ServerProperty` has a `Set<Origin>` constructor. Sign-counter regression raises `MaliciousCounterValueException`, which exposes `getStoredCounter()` and `getPresentedCounter()`. The library is not missing anything these specs need — the current code passes `false` where it should pass a real policy value and wraps every failure into one generic exception.
- **The policy value must survive a round trip.** A ceremony's `userVerification` is chosen at options time and enforced at verify time, across two stateless HTTP requests. The only state shared between them is the Redis challenge context.
- **The existing tests mock `WebAuthnManager`.** Every assertion these specs make concerns what the parse/verify path does, so the current tests cannot observe any of it. This change is not meaningfully verifiable without real authenticator fixtures.
- **`webauthn.rp.origin` is read in two services.** Both build `ServerProperty` with `Origin.create(rpProperties.origin)`.

## Goals / Non-Goals

**Goals**

- Keep all cryptographic and flag verification inside webauthn4j; this change supplies correct parameters rather than reimplementing checks.
- Make the challenge context the single carrier of per-ceremony policy.
- Let one server instance serve browser and Android clients without reconfiguration.

**Non-Goals**

- Rewriting the response envelope. The envelope inconsistency between `FidoOptionsResponse` and `WebAuthnVerificationResponse` is real but orthogonal; it belongs in its own change so this one stays reviewable.
- Unifying the two `@RestControllerAdvice` classes, for the same reason.
- Replacing the mock-based service tests. New fixture-based tests are added alongside them; the existing tests still pin controller and options-building behavior.

## Decisions

### Carry the ceremony's user verification policy in the challenge context

`PendingRegistrationContext` and `PendingAuthenticationContext` gain the advertised `userVerification` value. At verify time, `userVerificationRequired` is `advertised == "required"`, and `userPresenceRequired` is always `true`.

The alternative is to re-read `webauthn.rp` configuration at verify time. Rejected: the requirement is per-request (the client may ask for `required` on one ceremony and `preferred` on the next), so configuration cannot answer it, and deriving it from the assertion itself would let the client choose its own policy.

Both contexts are JSON-serialized into Redis by `RedisChallengeStore`. Adding a field is backward-compatible for reads only if deserialization tolerates missing properties; since challenges have a 60-second TTL and the store is embedded Redis that starts with the server, no migration is needed — any in-flight challenge is from this process.

### Validate `userHandle` against the stored credential, not the pending context

`PendingAuthenticationContext` already carries `credentialIds` for named ceremonies. For usernameless ceremonies that list is empty, so the pending context cannot establish the user. The check therefore compares the assertion's `userHandle` to `UserKeyEntity.userId` of the credential the assertion references.

The spec distinguishes two cases: a usernameless ceremony **must** carry a handle (nothing else identifies the user), while a named ceremony may omit it (the `allowCredentials` list already constrained the choice). Knowing which case applies requires a flag in the pending context — whether the ceremony was initiated with a username — rather than inferring it from `credentialIds` being empty, which is also what a named ceremony for a user with no credentials would look like. That case currently throws `CredentialNotFoundException` at options time, so the two are distinguishable today, but relying on that couples the check to an unrelated policy.

Alternative considered: pass `userHandle` to webauthn4j and let it check. Rejected — webauthn4j compares the handle against the `CredentialRecord`'s user entity, and the `CredentialRecordImpl` built here passes `null` for that entity, so the library has nothing to compare against. Populating the user entity would work but spreads the user model across two representations.

### Replace `origin: String` with `origins: Set<String>`

`WebAuthnRpProperties.origin` becomes `origins: Set<String>`, mapped to `Set<Origin>` once and handed to `ServerProperty`'s set-accepting constructor. `Origin.create` already parses `android:apk-key-hash:<hash>` into a scheme plus scheme-specific part, so Android origins need no special handling — only configuration.

This is a breaking configuration change. A single-origin compatibility shim was considered and rejected: the demo has no deployed configuration to preserve, and two ways to configure one thing is worse than one.

Startup validation (non-empty set, non-blank `id`) belongs in `WebAuthnRpProperties` so a misconfiguration fails fast rather than at the first ceremony.

### Map webauthn4j's attestation statement format to `AttestationType`

webauthn4j returns a verified `AttestationObject` whose statement format determines the type. `packed` with no x5c and no ecdaaKeyId is self attestation; `packed`/`tpm`/`android-key` with x5c is basic or attestation-CA depending on the certificate; `none` is `NONE`. The mapping is small enough to keep in the registration service but needs its own unit coverage per format, since getting it wrong silently mislabels stored credentials.

### Serve Digital Asset Links from configuration

A controller returns the statement list built from a configured list of `(packageName, sha256CertFingerprints)` pairs. Serving a static resource file was considered and rejected: the fingerprints belong with the rest of the relying-party configuration, and a static file would drift from `webauthn.rp.origins`, which must list the matching `apk-key-hash` origin.

An empty statement list is served when nothing is configured, rather than 404, so a probing client gets a well-formed answer.

### Use `webauthn4j-test` fixtures for the new behavior

Add `com.webauthn4j:webauthn4j-test` as a test dependency and drive registration and authentication through `EmulatorUtil` with a real `WebAuthnManager`, asserting the accept and reject outcomes each scenario names. Hand-crafting CBOR attestation blobs was considered and rejected as unmaintainable.

The emulator must be able to produce assertions **without** UV and **without** UP to cover the rejection scenarios; if it cannot suppress a flag, that scenario is covered at the parameter level instead (asserting the flags passed to webauthn4j) and the gap recorded. This is the one place where the task breakdown may need to adapt to what the library permits.

## Risks / Trade-offs

- **The emulator cannot produce a registration with UP or UV clear** → Confirmed during implementation: `WebAuthnModelAuthenticator.makeCredential` hardcodes `userConsent = true` and `userVerification = true` and derives the attestation's flags from those locals, so a UP-absent or UV-absent *attestation* is unreachable through the emulator. The assertion path has no such limitation — `getAssertion` takes its flags from the request — so authentication is covered end to end. The registration rejection scenarios are therefore covered at the parameter level (`RegistrationFlagParametersTest` pins the two booleans passed to `RegistrationParameters`), which is sound because webauthn4j performs the check itself: `RegistrationDataVerifier` calls `UPUVFlagsVerifier.verify` with exactly those parameters. Residual gap: a real authenticator returning a UP-clear attestation is not exercised in this repository.
- **Enforcing UP breaks silent-authentication clients** → Any client relying on UP not being checked was already outside the spec; there is no such client in this demo.
- **Random user handles invalidate credentials registered before the change** → H2 is in-memory and does not survive a restart, so there is nothing to migrate. Called out as BREAKING in the proposal in case a persistent datasource is adopted later.
- **The `credProps` resident-key state is client-reported, not authenticated** → It is recorded as informational only; nothing in these specs makes an authorization decision based on `rk`. Treating absence as unknown (not `false`) keeps a missing extension from being read as a positive claim.
- **Self attestation is not reachable end to end in tests** → The emulator's `PackedAuthenticator` always attaches a certificate path, so a packed statement with a null x5c cannot be produced through a ceremony. `AttestationTypeMappingTest` pins the mapping for that case directly; basic and none are additionally covered through real ceremonies. Note also that the client platform substitutes a none-attestation statement unless the options request `direct`/`indirect` conveyance, mirroring browser behaviour — so an attestation-type test must ask for direct conveyance or it will only ever observe `none`.
- **The attestation-format mapping can silently mislabel** → Per-format unit tests; a wrong label is a data-quality bug rather than a verification bypass, since trust-chain validation is out of scope.
- **Android origin config and Digital Asset Links can drift apart** → Both are derived from the same configuration block, and the design keeps the key hash in `origins` next to the fingerprints in the association config. A mismatch surfaces as an origin rejection, which the error message names.

- **Each additional full `@SpringBootTest` context starts its own embedded Redis on the fixed port** → Surfaced while adding configuration tests: `EmbeddedRedisConfig` binds `spring.data.redis.port` unconditionally and fails the context when the port is taken, so two context-loading test classes collide. Configuration-only tests therefore use `ApplicationContextRunner` with `ConfigDataApplicationContextInitializer`, and the seeded-RP assertion lives in the existing `RedisChallengeStoreTest` context. Making `EmbeddedRedisConfig` pick a free port, or tolerate an already-running server, is a separate concern and is left alone here.

## Migration Plan

1. `webauthn.rp.origin: <value>` → `webauthn.rp.origins: [<value>]` in `application.yml`. Startup fails with a named error if the old key is left alone, so the break is loud rather than silent.
2. Android support additionally requires adding the `android:apk-key-hash:` origin and the matching package name and certificate fingerprints, and an RP ID resolvable by the Android platform — `localhost` cannot serve `/.well-known/assetlinks.json` to a device. This is a deployment prerequisite, not a code change, and it is the reason the association endpoint is testable while end-to-end Android authentication is not verifiable from this repository alone.

Rollback is reverting the commit and restoring the `origin` key; no persisted state changes shape.

## Open Questions

- Which Android package name and signing certificate fingerprints the wallet client will use. Needed to populate configuration for a live Android ceremony, but not to implement or test the endpoint, which is driven by whatever configuration is present.