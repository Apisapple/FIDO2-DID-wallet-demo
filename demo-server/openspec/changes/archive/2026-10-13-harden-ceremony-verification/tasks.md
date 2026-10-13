# Tasks

## 1. Test harness for real ceremonies

The existing service tests mock `WebAuthnManager`, so nothing currently executes the parse/verify path every later group changes. This group lands first so each subsequent group has a way to prove its behavior.

- [x] 1.1 Add `com.webauthn4j:webauthn4j-test` as a test dependency and verify `./gradlew test` still passes with the dependency resolved
- [x] 1.2 Add a test fixture that drives a full registration then authentication round trip through `EmulatorUtil` against a real (non-mocked) `WebAuthnManager`, and verify the round trip passes end to end
- [x] 1.3 Extend the fixture with helpers that produce assertions with UV absent and with UP absent, and verify each helper yields authenticator data whose corresponding flag is clear; if the emulator cannot suppress a flag, record that in `design.md` under Risks and cover that case at the parameter level instead

## 2. Relying party origins

- [x] 2.1 Replace `WebAuthnRpProperties.origin: String` with `origins: Set<String>`, reject a blank `id` or empty `origins` at startup, and verify a context-load test fails for each invalid configuration and succeeds for a valid one
- [x] 2.2 Build `ServerProperty` from the full origin set in both `RegistrationService` and `AuthenticationService`, and verify a fixture ceremony is accepted for each configured origin and rejected for an unlisted one
- [x] 2.3 Update `application.yml` to `webauthn.rp.origins` with the localhost web origin plus a placeholder `android:apk-key-hash:` entry, and verify `./gradlew bootRun` starts and a registration options request still succeeds
- [x] 2.4 Verify an Android-style origin is accepted by registering and authenticating a fixture credential whose client data origin is the configured `android:apk-key-hash:` value

## 3. Digital Asset Links association

- [x] 3.1 Add Digital Asset Links configuration (list of package name plus SHA-256 certificate fingerprints) to the relying-party properties, and verify a test binds a configured list and an absent one
- [x] 3.2 Serve `GET /.well-known/assetlinks.json` from that configuration, and verify a slice test asserts HTTP 200, `application/json`, and a `delegate_permission/common.get_login_creds` entry per configured application
- [x] 3.3 Verify the endpoint returns HTTP 200 with an empty statement list when no application is configured, and requires no authentication

## 4. User presence and user verification enforcement

- [x] 4.1 Add the advertised `userVerification` to `PendingRegistrationContext` and `PendingAuthenticationContext`, and verify `RedisChallengeStoreTest` round-trips the new field for both context types
- [x] 4.2 Store the advertised `userVerification` when building options in both services, and verify unit tests assert the persisted context matches the value returned in the options response, including the default when the request omits it
- [x] 4.3 Pass `userPresenceRequired = true` and `userVerificationRequired = (advertised == "required")` to `RegistrationParameters`, and verify fixture tests cover: UP absent rejected; UV absent rejected under `required`; UV absent accepted under `preferred`
- [x] 4.4 Pass the same two flags to `AuthenticationParameters`, and verify fixture tests cover the same three cases for the assertion path

## 5. User handle binding and generation

- [x] 5.1 Generate user handles from 32 `SecureRandom` bytes for a user with no existing credentials, keep the existing handle otherwise, and verify unit tests assert a new handle is not derivable from the username, two usernames get unrelated handles, and a returning user keeps theirs
- [x] 5.2 Record in `PendingAuthenticationContext` whether the ceremony was initiated with a username, and verify the stored context reflects both a named and a usernameless options request
- [x] 5.3 Reject an assertion whose `userHandle` does not equal the referenced credential's stored handle, and verify a fixture test rejects a handle belonging to a different registered user
- [x] 5.4 Reject an assertion carrying no `userHandle` when the ceremony was usernameless, while accepting an absent handle for a named ceremony, and verify fixture tests cover both

## 6. Recorded credential properties

- [x] 6.1 Map the verified attestation statement to `AttestationType` and persist it instead of the hardcoded `NONE`, and verify unit tests cover the self, basic, and none formats
- [x] 6.2 Request the `credProps` extension in registration options, persist the reported resident-key state, and record it as unknown when the client returns no extension output; verify unit tests cover resident key reported true, false, and absent

## 7. Sign counter regression

- [x] 7.1 Catch webauthn4j's `MaliciousCounterValueException` and rethrow it as `SignCounterRegressionException` carrying the stored and presented counters, and verify a fixture test replaying a stale `signCount` produces an error message naming a counter regression
- [x] 7.2 Verify a non-counter failure (invalid signature) does not produce a counter-regression message, and that a credential with a stored counter of zero accepts an assertion reporting zero

## 8. Integration verification

- [x] 8.1 Run `./gradlew test` and verify the full suite passes with no regression in the existing registration and authentication tests
- [x] 8.2 Verify every scenario in `specs/webauthn-relying-party/spec.md`, `specs/webauthn-registration/spec.md`, and `specs/webauthn-authentication/spec.md` is covered by a test or an explicitly recorded gap, and list any gaps with their reason

## Scenario coverage

All 36 scenarios across the three spec deltas are covered by a test. The relying-party startup
scenarios are covered by `WebAuthnRpPropertiesTest`, `ApplicationYamlRpConfigTest` and
`RedisChallengeStoreTest` (which also asserts the seeded RP record — folded in there because a
second full `@SpringBootTest` context starts a competing embedded Redis on the same port).

Three scenarios are covered below the end-to-end level, for reasons confirmed against webauthn4j's
source and recorded in `design.md`:

| Scenario | Covered at | Reason |
|---|---|---|
| registration / User presence not asserted | `RegistrationFlagParametersTest` (parameter level) | `WebAuthnModelAuthenticator.makeCredential` hardcodes `userConsent = true`, so a UP-clear attestation cannot be produced. webauthn4j performs the check itself via `UPUVFlagsVerifier`, so pinning the parameter pins the behaviour. |
| registration / Required user verification is absent | `RegistrationFlagParametersTest` (parameter level) | Same cause: `makeCredential` hardcodes `userVerification = true`. |
| registration / Self attestation is recorded | `AttestationTypeMappingTest` (unit level) | The emulator's `PackedAuthenticator` always attaches a certificate path, so a packed statement with a null x5c is unreachable through a ceremony. |

Every authentication scenario, including both UP-absent and UV-absent rejections, is covered end
to end: `getAssertion` takes its flags from the request, so the assertion path has no such limit.

## Workflow follow-up

- Resolved: `add-authentication-flow` was archived as `2026-10-13-add-authentication-flow`, creating `openspec/specs/webauthn-authentication/spec.md`, so this change's `webauthn-authentication` delta now applies to an existing capability.
- Resolved: `fix-jackson-databind-conflict` was archived as `2026-10-13-fix-jackson-databind-conflict`, making `WebAuthnManager.verify` executable so the fixture-based verification steps in this change can run.
- Archive this change after the project's review requirements are satisfied, and verify the archived result.
