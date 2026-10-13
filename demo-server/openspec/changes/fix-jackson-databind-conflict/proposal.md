# Proposal

## Why

The server cannot complete a WebAuthn registration. `WebAuthnManager.verify` throws `NoSuchMethodError` on every real attestation, so `POST /attestation/result` fails for any genuine client:

```
java.lang.NoSuchMethodError: 'java.lang.Object tools.jackson.dataformat.cbor.CBORMapper
  .treeToValue(tools.jackson.databind.JsonNode, java.lang.Class)'
  at ...AuthenticationExtensionsAuthenticatorOutputs.getExtensions
  at ...RegistrationDataVerifier.verify
  at RegistrationService.verify(RegistrationService.kt:122)
```

webauthn4j 0.31.10 is compiled against `tools.jackson:jackson-databind:3.2.1`, but Spring Boot 4.0.3's dependency management downgrades it to 3.0.4. The `treeToValue(JsonNode, Class)` overload was added in 3.2.1; 3.0.4 only has the `TreeNode` form, so the call links at compile time and fails at runtime. The downgrade is present on `runtimeClasspath`, not only in tests.

`./gradlew test` passes on this broken server, because every existing service test mocks `WebAuthnManager` and therefore never executes `verify`. The suite cannot distinguish a working verification path from one that throws on first contact with a real authenticator.

This blocks `harden-ceremony-verification`, whose every verification step runs through `WebAuthnManager.verify`.

## What Changes

- Override both Jackson BOM versions Spring Boot manages, so webauthn4j gets the Jackson it was built against: `jackson-bom.version` to `3.2.1` and `jackson-2-bom.version` to `2.22.3`. Both are required together — databind 3.2.1 needs `jackson-annotations` 2.22+ for `JsonSerializeAs`, and pinning only the Jackson 3 BOM substitutes one `NoClassDefFoundError` for the original `NoSuchMethodError`.
- Add a test fixture that drives real registration and authentication ceremonies through webauthn4j's authenticator emulator against a real (non-mocked) `WebAuthnManager`, plus a round-trip regression test, so a recurrence fails the build instead of passing it.
- Add `com.webauthn4j:webauthn4j-test` as a test dependency, which the fixture needs.

## Capabilities

### New Capabilities

None. This restores conformance to behavior the specs already require.

### Modified Capabilities

None. `webauthn-registration` already requires that a valid attestation "verifies the response, persists a credential ... and returns HTTP 200" — the defect is the implementation failing a stated requirement, not a requirement that needs changing. `.openspec.yaml` sets `skip_specs: true` accordingly.

## Impact

**Build**

- `build.gradle`: two `ext[...]` Jackson BOM overrides; `webauthn4j-test` on the test classpath.

**Risk surface**

Raising Jackson affects Spring's own JSON handling, not just webauthn4j. Verification therefore has to cover request/response serialization through the running application, not only the webauthn4j path.

**Tests**

- New: a ceremony fixture, an in-memory `ChallengeStore`, a list-backed `UserKeyRepository` fake, and a registration-then-authentication round-trip test.
- No existing test changes behavior; the mock-based tests keep pinning controller and options-building logic.

**Not addressed here**

- The user-presence, user-verification, user-handle, attestation-type and multi-origin work in `harden-ceremony-verification`. This change only makes the verification path executable; it does not alter what is verified.
