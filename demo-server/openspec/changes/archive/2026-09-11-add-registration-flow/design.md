## Context

See proposal.md — Why. The data model (`RpEntity`, `UserKeyEntity`, `AuthenticatorTransportEntity`), repositories, and request/response DTOs (`common/data`) already exist. Missing pieces are the web layer, the registration service, challenge storage, and RP bootstrapping. Constraints from CLAUDE.md: Kotlin + Spring Boot 4.0.3 (servlet MVC), H2 + JPA (`ddl-auto: update`), embedded Redis, `webauthn4j-core 0.31.10`. Entity `id` fields are `var`; `UserKeyEntity` uses a Builder.

## Goals / Non-Goals

**Goals:**
- A thin `@RestController` exposing `POST /attestation/options` and `POST /attestation/result`.
- A `RegistrationService` that owns challenge issuance, attestation verification (via webauthn4j), and persistence.
- A Redis-backed, single-use, TTL-bounded challenge store keyed per ceremony.
- RP identity sourced from `application.yml` and seeded into `RpEntity` at startup.
- FIDO-style status envelope (`status`, `errorMessage`) on both endpoints.

**Non-Goals:**
- Authentication/assertion flow (separate change).
- Full attestation trust-chain / MDS metadata verification (`attestation=none` focus).
- Multi-RP support — a single configured RP is sufficient for the demo.
- Production hardening (rate limiting, auth on endpoints, CSRF).

## Decisions

**1. webauthn4j for attestation verification.** Use `WebAuthnManager.createNonStrictWebAuthnManager()` (lenient, suitable for `attestation=none` demos) over hand-rolling CBOR/COSE parsing. It handles clientDataJSON, authenticatorData, and attestationObject decoding plus the standard verification checks. We pass a `ServerProperty(origin, rpId, challenge, tokenBinding=null)` built from configured RP + the stored challenge. Alternative — strict manager with attestation statement validators — rejected as out of scope.

**2. Challenge store as an interface with a Redis implementation.** Define `ChallengeStore` (put with TTL / consume) and back it with `StringRedisTemplate`. Key = `reg:challenge:{challenge}` (or a ceremony id) holding the serialized pending-registration context (username, userHandle/userId, requested options). This keeps the service testable (swap for an in-memory fake) and isolates Redis. TTL defaults to the options `timeout` (60s). Consume = `GET` + `DELETE` to enforce single use regardless of verification outcome. Alternative — HTTP session — rejected: the Android client is stateless and won't carry a session cookie.

**3. RP config via `@ConfigurationProperties`.** Bind `webauthn.rp.{id,name,origin}` to a `WebAuthnRpProperties` class. An `ApplicationRunner` (or `@PostConstruct` seeder) ensures an `RpEntity` with the configured id exists (`RpRepository.findById` → save if absent). Alternative — hardcoded constants — rejected as it defeats the config-driven decision.

**4. Status envelope.** Add a generic FIDO-style wrapper (e.g. `status: "ok" | "failed"`, `errorMessage: String`) that the existing `RegistrationDataResponse` / `WebAuthnVerificationResponse` are returned within (or extended to carry). A `@RestControllerAdvice` maps thrown domain exceptions (`ChallengeNotFoundException`, `VerificationException`, duplicate-credential, bad request) to HTTP 200/4xx with `status: "failed"` and the reason, keeping the happy path clean.

**5. Persistence mapping.** On success, build `UserKeyEntity` via its Builder: `credentialId` (base64url), `publicKey` (COSE key, Base64/TEXT), `signatureAlgorithm` (COSE alg int), `signCounter`, `aaguid`, `attestationType = NONE`, `registeredTimestamp = now`, transports mapped to `AuthenticatorTransportEntity` (cascade ALL). Associate the resolved `RpEntity` and the user handle from the pending context.

## Risks / Trade-offs

- **[Non-strict verification accepts weak attestations]** → Acceptable for a demo; documented as a Non-Goal. A follow-up change can introduce strict validation + MDS.
- **[Redis unavailable at runtime]** → Embedded Redis is started with the app; if it fails to bind, challenge issuance fails fast with a clear error. Mitigation: verify embedded Redis config on startup.
- **[webauthn4j 0.31.x API surface may differ from older docs]** → Confirm exact class/method names (`WebAuthnManager`, `RegistrationRequest`, `RegistrationParameters`, `ServerProperty`, `verify`/`validate`) against the resolved dependency during implementation; adjust the service accordingly.
- **[Challenge stored keyed by challenge value]** → Two ceremonies can't collide because challenges are random and unique; consuming on any verification attempt prevents replay.
- **[User handle stability]** → `userId` (user handle) must be generated once per username and reused across ceremonies so `excludeCredentials` and credential lookup stay consistent. Derive it deterministically from existing credentials, or generate and persist on first registration.

## Open Questions

- Exact envelope shape (wrap existing DTOs vs. add `status`/`errorMessage` fields to them) — cosmetic, resolved during implementation without affecting spec behavior.
