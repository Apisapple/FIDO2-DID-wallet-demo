# webauthn-authentication Specification

## Purpose
Defines how the server issues a WebAuthn authentication challenge and verifies the authenticator's assertion response to authenticate a user with registered FIDO2 credentials.

## Requirements

### Requirement: Issue authentication options

The system SHALL provide an endpoint that returns `PublicKeyCredentialRequestOptions` for an authentication ceremony. The response SHALL include a freshly generated challenge, the configured relying party ID, user verification requirements, and optionally a list of allowed credentials matching the requested user.

The generated challenge SHALL be at least 16 bytes of cryptographically random data, base64url-encoded, and SHALL be stored server-side with a bounded time-to-live (e.g., 60 seconds) so it can be verified later. Each call SHALL produce a new, unique challenge.

The response envelope SHALL carry a `status` of `"ok"` and an empty `errorMessage` on success.

#### Scenario: Existing user requests authentication options
- **WHEN** a client POSTs an authentication options request with a known username
- **THEN** the system returns HTTP 200 with `status: "ok"`, the configured RP ID, a unique base64url challenge, and `allowCredentials` containing the user's registered credential IDs and transport hints
- **AND** the authentication challenge context is stored server-side for the duration of its TTL

#### Scenario: User with no credentials requests options
- **WHEN** a client POSTs an authentication options request for a username with no registered credentials
- **THEN** the system returns an error envelope with `status: "failed"` and a non-empty `errorMessage` (or empty `allowCredentials` depending on policy), and does not persist an active challenge

#### Scenario: Userless authentication options request
- **WHEN** a client POSTs an authentication options request without specifying a username
- **THEN** the system returns HTTP 200 with `status: "ok"`, a unique challenge, the RP ID, and `allowCredentials` as null/empty to support discoverable credentials (resident keys)
- **AND** the authentication challenge context is stored server-side with an unassigned user for the duration of its TTL

### Requirement: Verify authentication assertion

The system SHALL provide an endpoint that verifies a client's assertion response against a previously issued challenge. Verification SHALL confirm:
1. The challenge matches a stored, unexpired authentication challenge.
2. The client data type is `webauthn.get`.
3. The origin matches the configured relying party origin.
4. The RP ID hash matches the configured relying party.
5. The signature is valid against the stored public key of the referenced credential ID.
6. The authenticator user verification flag satisfies the RP policy if required.
7. The signature counter is strictly greater than the stored `signCounter` (unless the stored counter is 0 and authenticator does not increment).

The stored challenge SHALL be single-use: it SHALL be consumed (invalidated) once a verification attempt completes, whether it succeeds or fails, to prevent replay attacks.

On successful verification, the system SHALL update the stored credential's `signCounter` and return a success envelope.

#### Scenario: Valid assertion is authenticated
- **WHEN** a client POSTs a valid assertion response referencing an issued, unexpired challenge and registered credential
- **THEN** the system verifies the signature and client data, updates the credential's `signCounter`, and returns HTTP 200 with `status: "ok"`

#### Scenario: Challenge is unknown or expired
- **WHEN** a client POSTs an assertion response whose challenge is not found server-side or has expired
- **THEN** the system returns an error envelope with `status: "failed"` and a non-empty `errorMessage`, and no credential state is modified

#### Scenario: Challenge cannot be replayed
- **WHEN** a challenge has already been used in an assertion verification attempt
- **THEN** a subsequent verification attempt referencing the same challenge is rejected with `status: "failed"`

#### Scenario: Origin or RP ID mismatch
- **WHEN** the assertion's client data origin or authenticator data RP ID hash does not match the configured relying party
- **THEN** the system rejects the request with `status: "failed"` and a non-empty `errorMessage`

#### Scenario: Unknown credential ID
- **WHEN** a client submits an assertion with a credential ID not registered in the system
- **THEN** the system rejects the request with `status: "failed"` and a non-empty `errorMessage`

#### Scenario: Sign counter regression detected
- **WHEN** a client submits an assertion with a `signCount` less than or equal to the stored `signCounter` (when stored counter > 0)
- **THEN** the system rejects the request with an error indicating possible authenticator cloning or replay attack
