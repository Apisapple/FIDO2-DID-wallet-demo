# Proposal

## Why

The registration and authentication ceremonies currently accept assertions that the FIDO2 specification requires us to reject: user presence and user verification flags are never checked, the `userHandle` returned by a discoverable credential is never bound to the stored user, and user handles are a reversible encoding of the username. Separately, the relying party is configured with a single origin string, so the planned Android client — whose origin is `android:apk-key-hash:<hash>` rather than an HTTPS URL — cannot authenticate against the same server instance as a browser.

Both problems block the demo from being a faithful illustration of FIDO2: the first makes the server's "verified" result weaker than it claims, the second makes the Android half of the FIDO2-DID wallet demo unreachable.

## What Changes

**Ceremony verification**

- Enforce user presence on registration and authentication. `userPresence` is currently passed as `false` to both `RegistrationParameters` and `AuthenticationParameters`; it becomes always required.
- Enforce the user verification requirement the server itself advertised. The requested `userVerification` value is carried in the pending challenge context and used to drive the `userVerificationRequired` flag at verification time, instead of being hardcoded to `false`.
- Bind `userHandle` to the stored credential during authentication. For discoverable-credential (usernameless) ceremonies the assertion's `userHandle` must equal the stored `userId`; a mismatch is rejected.
- Generate user handles from `SecureRandom` instead of base64-encoding the username. **BREAKING** — credentials registered before this change carry a username-derived handle and will not match the new generation scheme. The H2 database is in-memory, so this only affects a server restart boundary, not stored data.
- Record the real attestation type and resident-key state. `attestationType` is currently hardcoded to `NONE` regardless of the requested `attestation` preference; it is derived from the verified attestation statement. `rk` is populated from the `credProps` extension output, which the options must request and which a client may legitimately omit — no authenticator data flag conveys discoverability.
- Resolve the sign-counter regression contract. `SignCounterRegressionException` is currently unreachable because webauthn4j detects regression internally and the failure surfaces as a generic verification error; the regression case gets an explicit, distinguishable rejection path.

**Relying party origins**

- Replace the single `webauthn.rp.origin` string with a set of accepted origins, supplied to `ServerProperty` as a collection. **BREAKING** — the `webauthn.rp.origin` configuration key becomes `webauthn.rp.origins` (a list).
- Accept Android origins (`android:apk-key-hash:<base64url-sha256>`) alongside HTTPS/localhost browser origins in that set.
- Serve Digital Asset Links at `/.well-known/assetlinks.json` from configuration so an Android client can be associated with the relying party ID.

## Capabilities

### New Capabilities

- `webauthn-relying-party`: Owns relying-party identity — the RP record, the set of accepted client origins (browser and Android), and the Digital Asset Links association document. Both ceremonies depend on this capability; neither owns it.

### Modified Capabilities

- `webauthn-registration`: Adds requirements for user presence/verification enforcement, unpredictable user handle generation, and recording the attestation type and resident-key state of the verified credential. Removes the `Relying party configuration` requirement, which moves to `webauthn-relying-party`.
- `webauthn-authentication`: Adds requirements for user presence/verification enforcement, `userHandle`-to-credential binding, and an explicit sign-counter regression rejection.

## Impact

**Code**

- `registration/RegistrationService.kt` — verification flags, user handle generation, attestation type and `rk` persistence.
- `authentication/AuthenticationService.kt` — verification flags, `userHandle` binding, sign-counter regression handling.
- `registration/challenge/PendingRegistrationContext.kt`, `authentication/challenge/PendingAuthenticationContext.kt` — carry the advertised `userVerification` through the challenge round trip.
- `config/WebAuthnRpProperties.kt` — `origin: String` becomes `origins: Set<String>`; adds Digital Asset Links fields.
- New controller serving `/.well-known/assetlinks.json`.
- `authentication/exception/AuthenticationException.kt` — `SignCounterRegressionException` becomes reachable.

**Configuration**

- `application.yml`: `webauthn.rp.origin` → `webauthn.rp.origins` (list), plus Digital Asset Links package name and certificate fingerprints.

**Tests**

- Existing service tests mock `WebAuthnManager`, so they never execute the parse/verify path these requirements govern. Covering the new behavior requires real authenticator fixtures via `com.webauthn4j:webauthn4j-test` (`EmulatorUtil`) — a new test dependency.

**Out of scope**

- Attestation trust-chain validation against FIDO MDS metadata.
- Session or token issuance after a successful ceremony.
- Credential listing and revocation endpoints.
