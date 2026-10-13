## Purpose

Defines how the server issues a WebAuthn registration challenge and verifies the authenticator's attestation response so that a new FIDO2 credential can be registered for a relying party.

## ADDED Requirements

### Requirement: Issue registration options

The system SHALL provide an endpoint that returns `PublicKeyCredentialCreationOptions` for a requested user, following the FIDO server response format. The response SHALL include the configured relying party, a user entity, a freshly generated challenge, and the supported public-key credential parameters.

The generated challenge SHALL be at least 16 bytes of cryptographically random data, base64url-encoded, and SHALL be stored server-side with a bounded time-to-live so it can be verified later. Each call SHALL produce a new, unique challenge.

The response envelope SHALL carry a `status` of `"ok"` and an empty `errorMessage` on success.

#### Scenario: New user requests options

- **WHEN** a client POSTs a registration options request with a username (and optional display name)
- **THEN** the system returns HTTP 200 with `status: "ok"`, the configured RP (`id`, `name`), a user entity (`id`, `name`, `displayName`), a unique base64url challenge, and `pubKeyCredParams` containing at least ES256
- **AND** the challenge is retrievable server-side for the duration of its TTL

#### Scenario: Existing credentials are excluded

- **WHEN** a client requests options for a username that already has one or more registered credentials
- **THEN** the response `excludeCredentials` lists the credential IDs already registered for that user under the relying party

#### Scenario: Missing username

- **WHEN** a client POSTs a registration options request without a username
- **THEN** the system returns an error envelope with `status: "failed"` and a non-empty `errorMessage`, and does not generate a challenge

### Requirement: Verify registration result

The system SHALL provide an endpoint that verifies a client's attestation response against a previously issued challenge. Verification SHALL confirm the challenge matches a stored, unexpired challenge; the client data type is `webauthn.create`; the origin matches the configured relying party origin; and the RP ID hash matches the configured relying party. On successful verification the system SHALL persist the new credential and return a success envelope.

The stored challenge SHALL be single-use: it SHALL be consumed (invalidated) once a verification attempt completes, whether it succeeds or fails, so it cannot be replayed.

The system SHALL support `attestation=none`; verification of full attestation trust chains against metadata is out of scope for this capability.

#### Scenario: Valid attestation is registered

- **WHEN** a client POSTs a valid attestation response referencing an issued, unexpired challenge
- **THEN** the system verifies the response, persists a credential (credential ID, public key, signature algorithm, sign counter, AAGUID, transports) associated with the relying party and user, and returns HTTP 200 with `status: "ok"`

#### Scenario: Challenge is unknown or expired

- **WHEN** a client POSTs an attestation response whose challenge is not found server-side or has expired
- **THEN** the system returns an error envelope with `status: "failed"` and a non-empty `errorMessage`, and no credential is persisted

#### Scenario: Origin or RP ID mismatch

- **WHEN** the attestation's client data origin or RP ID hash does not match the configured relying party
- **THEN** the system rejects the request with `status: "failed"` and a non-empty `errorMessage`, and no credential is persisted

#### Scenario: Duplicate credential

- **WHEN** a client submits an attestation whose credential ID is already registered
- **THEN** the system rejects the registration with `status: "failed"` and a non-empty `errorMessage`, and does not create a duplicate credential

#### Scenario: Challenge cannot be replayed

- **WHEN** a challenge has already been used in a verification attempt
- **THEN** a subsequent verification attempt referencing the same challenge is rejected with `status: "failed"`

### Requirement: Relying party configuration

The system SHALL derive the relying party identity (`id`, `name`, `origin`) from application configuration, and SHALL ensure a corresponding relying party record exists before registration ceremonies are served.

#### Scenario: RP available at startup

- **WHEN** the server starts with relying party configuration present
- **THEN** a relying party record matching the configured `id` and `name` exists and is used to build registration options
