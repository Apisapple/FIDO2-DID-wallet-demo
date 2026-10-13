## Context

See `proposal.md` for motivation and background.
The registration flow is fully operational using `webauthn4j-core` (0.31.10.RELEASE), Spring Data JPA with `UserKeyEntity`, and `RedisChallengeStore` for 1-time challenge lifecycle management.
DTO definitions for authentication options and assertion responses are already partially defined in `com.raonsecure.demoserver.common.data.AuthenticationDto.kt`.

## Goals / Non-Goals

**Goals:**
- Provide `POST /assertion/options` to issue FIDO2 WebAuthn authentication challenges with optional user scoping and `allowCredentials` population.
- Provide `POST /assertion/result` to verify assertion signatures against stored public keys using `webauthn4j` and update credential `signCounter`.
- Maintain challenge state in Redis with single-use semantics and TTL (60s), mirroring the registration challenge store pattern.
- Handle error conditions cleanly (missing challenge, credential not found, invalid signature, sign counter regression) with standard demo response envelopes.

**Non-Goals:**
- User session / JWT issuance after authentication (can be layered later or integrated into client DID wallet workflow).
- FIDO MDS / Attestation trust verification during assertion (assertion uses public key directly; MDS was not validated in registration either).
- Multi-factor Step-up or conditional UI mediation (keep standard WebAuthn assertion ceremony for demo).

## Decisions

### 1. Challenge Store Structure
- **Decision**: Extend `ChallengeStore` / `RedisChallengeStore` with specific methods for authentication (`saveAuthentication(challenge, context, ttl)` and `consumeAuthentication(challenge)`) and define a `PendingAuthenticationContext(username: String?, challenge: String, credentialIds: List<String>)`.
- **Rationale**: Isolates registration contexts from authentication contexts in Redis (using key prefix `auth:challenge:` vs `reg:challenge:`) while reusing Redis template and TTL infrastructure.
- **Alternatives considered**:
  - Store raw challenge only without context: Would lose binding to requested username/credentials for assertion validation.
  - Single generic store with polymorphic payload: Introduces Jackson polymorphism deserialization complexity without tangible benefit for two flows.

### 2. Controller & Package Organization
- **Decision**: Place authentication code under `com.raonsecure.demoserver.authentication`:
  - `AuthenticationController`: handles `/assertion/options` and `/assertion/result`.
  - `AuthenticationService`: orchestrates challenge generation, DB credential lookup, WebAuthn4J verification, and sign counter persistence.
  - `AuthenticationExceptionHandler`: maps authentication-specific domain exceptions to JSON error envelopes.
  - `challenge/PendingAuthenticationContext`: contextual state saved in Redis.
- **Rationale**: Clean separation of concerns mirroring the existing `com.raonsecure.demoserver.registration` package.

### 3. Assertion Verification with WebAuthn4J
- **Decision**: Reconstruct `webauthn4j.data.Authenticator` from `UserKeyEntity` stored fields (`credentialId`, `publicKey`, `signatureAlgorithm`, `signCounter`) to feed into `WebAuthnManager.verify(AuthenticationRequest, AuthenticationParameters)`.
- **Rationale**: `UserKeyEntity` stores raw public key bytes (Base64) and algorithm ID (e.g. -7 for ES256, -257 for RS256). WebAuthn4J expects a `CredentialRecord` or `Authenticator` instance. Parsing the public key into an `AttestedCredentialData` / `COSEKey` or building `AuthenticatorImpl` allows native WebAuthn4J signature validation and counter checks.
- **Alternatives considered**:
  - Manual signature verification using `java.security.Signature`: Re-invents WebAuthn authenticatorData parsing, flags check (UP/UV), and SHA-256 clientDataHash handling, which is error-prone.

### 4. Cloned Authenticator / Counter Regression Check
- **Decision**: WebAuthn4J automatically verifies that assertion `signCount > stored signCounter` if stored counter > 0. If regression occurs, verification throws an exception and fails the request. If both are 0, verification succeeds (supporting authenticators without hardware counters).
- **Rationale**: Adheres to W3C WebAuthn Level 2 recommendation for replay/clone detection.

## Risks / Trade-offs

- **[Risk] Public key format reconstruction in WebAuthn4J**
  → *Mitigation*: Unit test decoding and reconstructing the `COSEKey` / `PublicKey` from the Base64-encoded `publicKey` stored in `UserKeyEntity` to ensure compatibility across supported algorithms (ES256, RS256).
- **[Risk] Unregistered / empty user authentication request**
  → *Mitigation*: If `username` is provided but has no credentials, return failure early with descriptive message ("No credentials registered for user") rather than issuing a challenge that cannot be satisfied.
- **[Risk] Redis concurrency on challenge consume**
  → *Mitigation*: Redis `GETDEL` (or get + delete in single transaction / script) ensures challenge single-use even under concurrent duplicate submissions.
