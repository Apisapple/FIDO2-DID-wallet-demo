## Why

The server can register FIDO2 credentials but has no way to authenticate a user with them. The authentication ceremony — challenge issuance and assertion verification — is required to complete the FIDO2 demo flow end-to-end.

## What Changes

- Add `POST /assertion/options` endpoint that returns a `PublicKeyCredentialRequestOptions` challenge for a given username, following the FIDO server spec response envelope.
- Add `POST /assertion/result` endpoint that verifies the client's assertion response using webauthn4j and returns a success/failure envelope.
- Challenge state between the two calls is stored in Redis with a bounded TTL and consumed on first use (single-use, same pattern as registration).
- On successful verification, the credential's `signCounter` is updated and the authentication event is considered complete.

## Capabilities

### New Capabilities

- `webauthn-authentication`: WebAuthn authentication ceremony — assertion challenge issuance and assertion verification against stored credentials.

### Modified Capabilities

(none)

## Impact

- **New endpoints**: `POST /assertion/options`, `POST /assertion/result`
- **Existing code reused**: `ChallengeStore` / `RedisChallengeStore` extended with an authentication context; `UserKeyEntity` / `UserKeyRepository` read for credential lookup and sign-counter update; `WebAuthnRpProperties` and `WebAuthnManager` bean reused unchanged.
- **No breaking changes** to existing registration endpoints or entities.
