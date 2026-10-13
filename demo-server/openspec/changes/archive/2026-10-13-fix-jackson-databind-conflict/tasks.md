# Tasks

## 1. Align the Jackson version

- [x] 1.1 Override `jackson-bom.version` to `3.2.1` and `jackson-2-bom.version` to `2.22.3` in `build.gradle`, with a comment naming webauthn4j as the reason, and verify `./gradlew dependencies --configuration runtimeClasspath` resolves `jackson-databind:3.2.1`, `jackson-dataformat-cbor:3.2.1` and `jackson-annotations:2.22` with no `-> 3.0.4` or `-> 2.20` downgrade
- [x] 1.2 Verify the build still resolves and compiles by running `./gradlew compileKotlin compileTestKotlin` clean, confirming both overridden BOM coordinates exist in the repositories

## 2. Regression coverage through a real authenticator

- [x] 2.1 Add `com.webauthn4j:webauthn4j-test` as a test dependency and verify `./gradlew test` still passes with it resolved
- [x] 2.2 Add a ceremony fixture driving webauthn4j's `ClientPlatform` emulator (create and get) and returning the raw base64url fields the `/attestation/result` and `/assertion/result` bodies carry, and verify it compiles and produces a non-blank credential ID, client data, attestation object, authenticator data and signature
- [x] 2.3 Add an in-memory single-use `ChallengeStore` and a list-backed `UserKeyRepository` fake, and verify a test asserts the store returns a saved context once and null on a second consume
- [x] 2.4 Add a harness wiring the real `RegistrationService` and `AuthenticationService` to a real (non-mocked) `WebAuthnManager`, and verify a registration-then-authentication round-trip test passes, asserting the credential is persisted with a public key and that authentication updates its timestamp
- [x] 2.5 Verify the round-trip test actually catches the defect by temporarily reverting the task 1.1 override, confirming it fails with `NoSuchMethodError`, then restoring the override

## 3. Verification beyond the webauthn4j path

- [x] 3.1 Run `./gradlew test` and verify the whole suite passes, including the pre-existing registration and authentication tests
- [x] 3.2 Start the application with `./gradlew bootRun` and verify `POST /attestation/options` returns a well-formed options envelope, confirming Spring's own request deserialization and response serialization still work under the raised Jackson, and that the log holds no `NoSuchMethodError` or `NoClassDefFoundError`

## Workflow follow-up

- Archive this change after the project's review requirements are satisfied, then resume `harden-ceremony-verification`, which is blocked on this fix.
