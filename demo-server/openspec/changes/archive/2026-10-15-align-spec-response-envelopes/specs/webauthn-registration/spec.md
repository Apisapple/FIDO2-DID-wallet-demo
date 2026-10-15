# Spec Delta

## ADDED Requirements

### Requirement: Response envelope shapes

The two endpoints SHALL answer with asymmetric envelopes. A successful options request SHALL carry `status: "ok"`, an empty `errorMessage`, and the ceremony options under `options`. A successful result, and every handled failure of either endpoint, SHALL carry `success` and a `message`. A handled failure SHALL be returned with HTTP 200 and `success: false`, so a client SHALL NOT judge the outcome by status code alone.

#### Scenario: Options success carries the status envelope

- **WHEN** a client POSTs a valid registration options request
- **THEN** the response carries `status: "ok"`, an empty `errorMessage`, and the options under `options`

#### Scenario: Result success carries the success envelope

- **WHEN** a client POSTs an attestation that verifies
- **THEN** the response carries `success: true` and a `message`

#### Scenario: A handled failure answers HTTP 200

- **WHEN** a verification fails for a handled reason, such as an unknown challenge or a duplicate credential
- **THEN** the response is HTTP 200 carrying `success: false` and a non-empty `message`, and a client reading only the status code would wrongly read it as success

### Requirement: Unreadable request bodies answer with the failure envelope

A request whose body cannot be read — malformed JSON, or a body omitting a field the endpoint requires — SHALL be answered with the same failure envelope as any other handled failure: `success: false` and a non-empty `message`. The response SHALL NOT expose a framework error page, and SHALL NOT include a stack trace or other internal diagnostic detail.

#### Scenario: Options request omits the username field entirely

- **WHEN** a client POSTs a registration options request whose body omits the `username` field
- **THEN** the system answers with `success: false` and a non-empty `message`, no challenge is generated, and the response carries no stack trace

#### Scenario: Options request carries malformed JSON

- **WHEN** a client POSTs a registration options request whose body is not valid JSON
- **THEN** the system answers with `success: false` and a non-empty `message`, and the response carries no stack trace

## MODIFIED Requirements

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

- **WHEN** a client POSTs a registration options request without a usable username, whether the field is blank or absent from the body
- **THEN** the system returns an error envelope with `success: false` and a non-empty `message`, and does not generate a challenge

### Requirement: Verify registration result

The system SHALL provide an endpoint that verifies a client's attestation response against a previously issued challenge. Verification SHALL confirm the challenge matches a stored, unexpired challenge; the client data type is `webauthn.create`; the origin matches the configured relying party origin; and the RP ID hash matches the configured relying party. On successful verification the system SHALL persist the new credential and return a success envelope.

The stored challenge SHALL be single-use: it SHALL be consumed (invalidated) once a verification attempt completes, whether it succeeds or fails, so it cannot be replayed.

The system SHALL support `attestation=none`; verification of full attestation trust chains against metadata is out of scope for this capability.

#### Scenario: Valid attestation is registered

- **WHEN** a client POSTs a valid attestation response referencing an issued, unexpired challenge
- **THEN** the system verifies the response, persists a credential (credential ID, public key, signature algorithm, sign counter, AAGUID, transports) associated with the relying party and user, and returns HTTP 200 with `success: true`

#### Scenario: Challenge is unknown or expired

- **WHEN** a client POSTs an attestation response whose challenge is not found server-side or has expired
- **THEN** the system returns an error envelope with `success: false` and a non-empty `message`, and no credential is persisted

#### Scenario: Origin or RP ID mismatch

- **WHEN** the attestation's client data origin or RP ID hash does not match the configured relying party
- **THEN** the system rejects the request with `success: false` and a non-empty `message`, and no credential is persisted

#### Scenario: Duplicate credential

- **WHEN** a client submits an attestation whose credential ID is already registered
- **THEN** the system rejects the registration with `success: false` and a non-empty `message`, and does not create a duplicate credential

#### Scenario: Challenge cannot be replayed

- **WHEN** a challenge has already been used in a verification attempt
- **THEN** a subsequent verification attempt referencing the same challenge is rejected with `success: false`

### Requirement: Enforce user presence during registration

Registration verification SHALL require the user presence flag in the authenticator data to be set. A registration whose authenticator data does not assert user presence SHALL be rejected and no credential persisted.

#### Scenario: User presence asserted

- **WHEN** a client POSTs an otherwise valid attestation whose authenticator data has the user presence flag set
- **THEN** verification continues and, on success, the credential is persisted

#### Scenario: User presence not asserted

- **WHEN** a client POSTs an attestation whose authenticator data does not have the user presence flag set
- **THEN** the system rejects the registration with `success: false` and a non-empty `message`, and no credential is persisted

### Requirement: Enforce the advertised user verification requirement

The user verification requirement advertised in the registration options SHALL be stored alongside the challenge and enforced at verification time. When the advertised requirement was `required`, a registration whose authenticator data does not assert user verification SHALL be rejected. When it was `preferred` or `discouraged`, the user verification flag SHALL NOT cause rejection.

#### Scenario: Required user verification is satisfied

- **GIVEN** registration options advertised `userVerification: "required"`
- **WHEN** a client POSTs an attestation whose authenticator data asserts user verification
- **THEN** verification continues and, on success, the credential is persisted

#### Scenario: Required user verification is absent

- **GIVEN** registration options advertised `userVerification: "required"`
- **WHEN** a client POSTs an attestation whose authenticator data does not assert user verification
- **THEN** the system rejects the registration with `success: false` and a non-empty `message`, and no credential is persisted

#### Scenario: Preferred user verification is absent

- **GIVEN** registration options advertised `userVerification: "preferred"`
- **WHEN** a client POSTs an attestation whose authenticator data does not assert user verification
- **THEN** the user verification flag does not cause rejection and, on success, the credential is persisted
