# FIDO2 Demo Server — API

WebAuthn registration and authentication over HTTP. Base URL in development is
`http://localhost:8080`.

Every field carrying binary data — challenges, credential IDs, user handles, client data,
attestation objects, authenticator data, signatures — is **base64url without padding**, in both
directions.

No authentication is required on any endpoint. There is no session or token: a successful
ceremony returns a result envelope and nothing else.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/attestation/options` | Start registration — returns `PublicKeyCredentialCreationOptions` |
| POST | `/attestation/result` | Finish registration — verifies the attestation, stores the credential |
| POST | `/assertion/options` | Start authentication — returns `PublicKeyCredentialRequestOptions` |
| POST | `/assertion/result` | Finish authentication — verifies the assertion, updates the sign counter |
| GET | `/.well-known/assetlinks.json` | Digital Asset Links statement list for Android clients |

A browser test page is served at `/`, and the H2 console at `/h2-console/`.

## Response envelopes

Two shapes are in use, and **both can report failure with HTTP 200** — never judge an outcome by
status code alone.

**Options success** — `status` is `"ok"` and `options` holds the ceremony options:

```json
{ "status": "ok", "errorMessage": "", "options": { } }
```

**Result, and every handled failure** — check `success`:

```json
{ "success": true, "message": "Registration successful" }
```

```json
{ "success": false, "message": "Challenge not found or expired: ..." }
```

> The two envelopes are not symmetric: options answer with `status`, results with `success`, and a
> failed options request answers with the `success` shape rather than `status: "failed"`. This is
> the specified contract — see the `webauthn-registration` and `webauthn-authentication` specs.

## POST /attestation/options

Request:

| Field | Type | Required | Notes |
|---|---|---|---|
| `username` | string | yes | Must not be blank. |
| `displayName` | string | no | Defaults to `username`. |
| `attestation` | string | no | `none` (default), `indirect`, `direct`, `enterprise`. |
| `authenticatorSelection` | object | no | `authenticatorAttachment`, `requireResidentKey`, `residentKey`, `userVerification`. |

```json
{
  "username": "alice",
  "displayName": "Alice",
  "attestation": "direct",
  "authenticatorSelection": { "userVerification": "required", "residentKey": "required" }
}
```

Response — `options`:

| Field | Type | Notes |
|---|---|---|
| `rp` | object | `{ id, name }` from configuration. |
| `user` | object | `{ id, name, displayName }`. `id` is the user handle: 32 random bytes, base64url. A user who already has a credential keeps their handle. |
| `challenge` | string | 32 random bytes. Single-use, 60-second TTL. |
| `pubKeyCredParams` | array | `[{type:"public-key",alg:-7}, {type:"public-key",alg:-257}]` — ES256 and RS256. |
| `timeout` | number | 60000. |
| `excludeCredentials` | array | Omitted when the user has none; otherwise one `{id, type, transports}` per registered credential. |
| `authenticatorSelection` | object | Echoes the request, with `userVerification` always present. |
| `attestation` | string | Echoes the request. |
| `extensions` | object | `{"credProps": true}` — asks the client to report whether the credential is discoverable. |

```json
{
  "status": "ok",
  "errorMessage": "",
  "options": {
    "rp": { "id": "localhost", "name": "FIDO2 Demo Server" },
    "user": { "id": "pREnle-M6jw48NdeW1A75jxxwaTJsYwMleVXHtjPk6Q", "displayName": "Alice", "name": "alice" },
    "challenge": "KOaqX-9kiE71QKjcIw2FqNBHwApWNgXm-0SewLVMsDo",
    "pubKeyCredParams": [ { "type": "public-key", "alg": -7 }, { "type": "public-key", "alg": -257 } ],
    "timeout": 60000,
    "authenticatorSelection": { "residentKey": "required", "userVerification": "required" },
    "attestation": "direct",
    "extensions": { "credProps": true }
  }
}
```

Failures:

| Condition | HTTP | Body |
|---|---|---|
| Blank or absent `username` | 400 | `{"success": false, "message": "username is required"}` |
| Body is not readable JSON | 400 | `{"success": false, "message": "Request body could not be read: ..."}` |

## POST /attestation/result

Request:

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | string | yes | Credential ID. |
| `rawId` | string | no | Preferred over `id` when present. |
| `type` | string | no | `public-key`. |
| `response.clientDataJSON` | string | yes | Also accepted as `clientDataJson`. |
| `response.attestationObject` | string | yes | |
| `response.transports` | array | no | e.g. `["internal","hybrid"]`. Stored with the credential. |
| `clientExtensionOutput` | object | no | Also accepted as `clientExtensionOutputs` or `extensions`. Carries `credProps`. |

```json
{
  "id": "a6ywEWk66eUKNhL1...",
  "rawId": "a6ywEWk66eUKNhL1...",
  "type": "public-key",
  "response": {
    "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIi...",
    "attestationObject": "o2NmbXRkbm9uZWdhdHRTdG10oGh...",
    "transports": ["internal"]
  },
  "clientExtensionOutput": { "credProps": { "rk": true } }
}
```

Verification checks the challenge is known and unexpired, the client data type is
`webauthn.create`, the origin is one of the configured origins, the RP ID hash matches, **user
presence is asserted**, and — when the options advertised `userVerification: "required"` — that
user verification is asserted. The challenge is consumed once it has been looked up, whether
verification then succeeds or fails. The duplicate-credential check runs *before* that lookup, so
a rejected duplicate leaves the challenge unconsumed and still usable until its TTL expires.

On success the credential is stored with its public key, signature algorithm, sign counter,
AAGUID, transports, the attestation type derived from the verified statement, and the
resident-key state reported in `credProps` (recorded as unknown when the client omits it).

Response: `{"success": true, "message": "Registration successful"}`

Failures — all **HTTP 200** with `success: false`, except as noted:

| Condition | `message` |
|---|---|
| Credential ID already registered | `Credential already registered: <id>` |
| Challenge unknown or expired | `Challenge not found or expired: <challenge>` |
| Malformed attestation | `Failed to parse attestation: ...` |
| Origin, RP ID, UP or UV check failed | `Attestation verification failed: ...` |
| Invalid argument (HTTP 400) | handler-supplied message |

## POST /assertion/options

Request — the body may be omitted entirely:

| Field | Type | Required | Notes |
|---|---|---|---|
| `username` | string | no | Omit or send `null` for a usernameless (discoverable credential) ceremony. |
| `userVerification` | string | no | `preferred` (default), `required`, `discouraged`. |

Response — `options`:

| Field | Type | Notes |
|---|---|---|
| `challenge` | string | 32 random bytes. Single-use, 60-second TTL. |
| `rpId` | string | Configured relying party ID. |
| `allowCredentials` | array | One `{id, type, transports}` per credential of the named user. Omitted for a usernameless ceremony. |
| `timeout` | number | 60000. |
| `userVerification` | string | Echoes the request; enforced at verification time. |

```json
{
  "status": "ok",
  "errorMessage": "",
  "options": {
    "challenge": "g_sg6voFnSk8Sa0jllNaM9kDZfK1blz46CkbcjleJyM",
    "rpId": "localhost",
    "allowCredentials": [ { "id": "a6ywEWk66eUKNhL1...", "type": "public-key", "transports": ["internal"] } ],
    "timeout": 60000,
    "userVerification": "required"
  }
}
```

Failure — **HTTP 200** with
`{"success": false, "message": "Credential not found: No credentials registered for user: <username>"}`
when a named user has no credentials. A usernameless request always succeeds.

## POST /assertion/result

Request:

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | string | yes | Credential ID. |
| `rawId` | string | no | Preferred over `id` when present. |
| `type` | string | no | `public-key`. |
| `response.clientDataJSON` | string | yes | Also accepted as `clientDataJson`. |
| `response.authenticatorData` | string | yes | |
| `response.signature` | string | yes | |
| `response.userHandle` | string | conditional | **Required** when the ceremony was started without a username. Optional otherwise; when present it must match the credential's stored handle. |
| `clientExtensionOutput` | object | no | |

```json
{
  "id": "a6ywEWk66eUKNhL1...",
  "rawId": "a6ywEWk66eUKNhL1...",
  "type": "public-key",
  "response": {
    "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uZ2V0Ii...",
    "authenticatorData": "SZYN5YgOjGh0NBcPZHZgW4_krrmi...",
    "signature": "MEUCIQDa9H8l...",
    "userHandle": "pREnle-M6jw48NdeW1A75jxxwaTJsYwMleVXHtjPk6Q"
  }
}
```

Verification checks the challenge is known and unexpired, the client data type is `webauthn.get`,
the origin is one of the configured origins, the RP ID hash matches, the credential is registered
and within `allowCredentials` when one was issued, the user handle binds to the credential, the
signature validates against the stored public key, **user presence is asserted**, user
verification is asserted when advertised as `required`, and the sign counter advanced. The
challenge is consumed whether verification succeeds or fails.

On success the stored sign counter and last-authenticated timestamp are updated.

Response: `{"success": true, "message": "Authentication successful"}`

Failures — all **HTTP 200** with `success: false`, except as noted:

| Condition | `message` |
|---|---|
| Challenge unknown or expired | `Challenge not found or expired: <challenge>` |
| Credential not registered | `Credential not found: <id>` |
| User handle mismatch | `Asserted userHandle does not match the credential: <id>` |
| Usernameless ceremony with no user handle | `Assertion for a usernameless ceremony must carry a userHandle` |
| Credential outside `allowCredentials` | `Credential ID <id> is not in allowed credentials` |
| Sign counter did not advance | `Sign counter regressed: stored <n>, received <m>` |
| Malformed assertion | `Failed to parse assertion: ...` |
| Signature, origin, RP ID, UP or UV check failed | `Assertion verification failed: ...` |
| Invalid argument (HTTP 400) | handler-supplied message |

## GET /.well-known/assetlinks.json

Digital Asset Links statement list, served as `application/json`, built from configuration.
Fetched anonymously by the Android platform; requires no credential. Returns `200` with an empty
array when no Android application is configured.

```json
[
  {
    "relation": ["delegate_permission/common.get_login_creds"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.raonsecure.wallet",
      "sha256_cert_fingerprints": ["AA:BB:CC:..."]
    }
  }
]
```

Field names are snake_case, as the Digital Asset Links format requires.

## Configuration

```yaml
webauthn:
  rp:
    id: localhost
    name: FIDO2 Demo Server
    origins:                                  # non-empty; every accepted client origin
      - http://localhost:8080
      - android:apk-key-hash:<base64url-sha256>
    android-apps:                             # optional; published at /.well-known/assetlinks.json
      - package-name: com.raonsecure.wallet
        sha256-cert-fingerprints: ["AA:BB:CC:..."]
```

`id` must not be blank and `origins` must not be empty or contain a blank entry; the application
fails to start otherwise. A ceremony is accepted only when the client data origin matches a
member of `origins` exactly, which is what lets one instance serve both browser and Android
clients.

## Notes and limits

- **Challenges** are single-use with a 60-second TTL, held in Redis (embedded in development).
- **Credential IDs** are stored up to 1400 base64url characters, covering the 1023-byte WebAuthn
  maximum.
- **Supported algorithms** are ES256 (-7) and RS256 (-257).
- **No session is issued.** A successful assertion returns only the result envelope, so a client
  cannot carry proof of authentication to a subsequent request.
- **No credential management.** There is no endpoint to list or revoke registered credentials.
- **Attestation trust chains are not validated** against FIDO MDS metadata; the attestation type
  is recorded but not verified against authenticator metadata.
- `WebAuthnVerificationResponse` declares `userId` and `username` fields, but no endpoint
  currently populates them.
