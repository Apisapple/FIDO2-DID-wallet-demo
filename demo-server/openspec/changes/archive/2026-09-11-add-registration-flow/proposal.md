## Why

The demo server currently has the full FIDO2 data model (entities, repositories, DTOs) but no runnable API — there is no way for a client to register a credential. To exercise the WebAuthn registration ceremony end-to-end, the server needs endpoints that issue a registration challenge and verify the authenticator's attestation response.

## What Changes

- Add a **registration options** endpoint (`POST /attestation/options`) that builds a `PublicKeyCredentialCreationOptions` response (RP, user, challenge, `pubKeyCredParams`, `excludeCredentials`) following the FIDO server request/response format.
- Add a **registration result** endpoint (`POST /attestation/result`) that verifies the client's attestation response with webauthn4j and, on success, persists a `UserKeyEntity` for the RP.
- Introduce a **service layer** that owns challenge generation, challenge lookup/verification, attestation validation (`attestation=none` focus), and credential persistence.
- Store the pending challenge (plus associated user handle / options) in **Redis** with a short TTL, keyed per registration ceremony, and consume it on `/attestation/result`.
- Seed the `RpEntity` at startup from **`application.yml`** RP configuration (`webauthn.rp.id`, `name`, `origin`).
- Return FIDO-style envelopes: success responses carry `status: "ok"` / `errorMessage: ""`; failures carry `status: "failed"` / `errorMessage: "<reason>"`.

## Capabilities

### New Capabilities
- `webauthn-registration`: Issuing a registration challenge and verifying the authenticator attestation response to register a new FIDO2 credential for a relying party.

### Modified Capabilities
<!-- None — no existing spec-level behavior changes. -->

## Impact

- **New code**: a REST controller, a registration service, a challenge store abstraction (Redis-backed), RP configuration properties, and a startup RP seeder.
- **Existing code**: reuses `common/data` DTOs, `base/entity` (`RpEntity`, `UserKeyEntity`, `AuthenticatorTransportEntity`), and `base/repository` (`RpRepository`, `UserKeyRepository`); may add finder/save usage. DTOs may gain a FIDO-style status envelope wrapper.
- **Dependencies**: uses the already-present `webauthn4j-core` and `spring-boot-starter-data-redis`; no new dependencies expected.
- **Config**: `application.yml` gains a `webauthn.rp` block; Redis (embedded) is now functionally exercised.
- **APIs**: two new public endpoints under `/attestation/*`.
