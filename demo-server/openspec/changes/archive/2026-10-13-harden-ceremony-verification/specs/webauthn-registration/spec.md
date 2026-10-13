# Spec Delta

## ADDED Requirements

### Requirement: Enforce user presence during registration

Registration verification SHALL require the user presence flag in the authenticator data to be set. A registration whose authenticator data does not assert user presence SHALL be rejected and no credential persisted.

#### Scenario: User presence asserted

- **WHEN** a client POSTs an otherwise valid attestation whose authenticator data has the user presence flag set
- **THEN** verification continues and, on success, the credential is persisted

#### Scenario: User presence not asserted

- **WHEN** a client POSTs an attestation whose authenticator data does not have the user presence flag set
- **THEN** the system rejects the registration with `status: "failed"` and a non-empty `errorMessage`, and no credential is persisted

### Requirement: Enforce the advertised user verification requirement

The user verification requirement advertised in the registration options SHALL be stored alongside the challenge and enforced at verification time. When the advertised requirement was `required`, a registration whose authenticator data does not assert user verification SHALL be rejected. When it was `preferred` or `discouraged`, the user verification flag SHALL NOT cause rejection.

#### Scenario: Required user verification is satisfied

- **GIVEN** registration options advertised `userVerification: "required"`
- **WHEN** a client POSTs an attestation whose authenticator data asserts user verification
- **THEN** verification continues and, on success, the credential is persisted

#### Scenario: Required user verification is absent

- **GIVEN** registration options advertised `userVerification: "required"`
- **WHEN** a client POSTs an attestation whose authenticator data does not assert user verification
- **THEN** the system rejects the registration with `status: "failed"` and a non-empty `errorMessage`, and no credential is persisted

#### Scenario: Preferred user verification is absent

- **GIVEN** registration options advertised `userVerification: "preferred"`
- **WHEN** a client POSTs an attestation whose authenticator data does not assert user verification
- **THEN** the user verification flag does not cause rejection and, on success, the credential is persisted

### Requirement: User handles are unpredictable

The user handle issued in registration options SHALL be generated from at least 32 bytes of cryptographically random data, SHALL NOT be derived from the username or any other user-identifying value, and SHALL NOT exceed 64 bytes. A user who already has a registered credential SHALL keep their existing user handle so that credentials for one user share a single handle.

#### Scenario: New user receives a random handle

- **WHEN** a client requests registration options for a username with no registered credentials
- **THEN** the returned user `id` is a base64url value of at least 32 random bytes from which the username cannot be recovered

#### Scenario: Two users receive unrelated handles

- **WHEN** registration options are requested for two different usernames with no registered credentials
- **THEN** neither user handle can be derived from the other or from either username

#### Scenario: Returning user keeps their handle

- **WHEN** a client requests registration options for a username that already has a registered credential
- **THEN** the returned user `id` equals the user handle stored for that user's existing credentials

### Requirement: Record the verified attestation type

On successful registration the system SHALL persist the attestation type derived from the verified attestation statement, rather than a fixed value.

#### Scenario: Self attestation is recorded

- **WHEN** a credential is registered whose verified attestation statement is self attestation
- **THEN** the persisted credential records an attestation type of self attestation, not `none`

#### Scenario: Basic attestation is recorded

- **WHEN** a credential is registered whose verified attestation statement carries a basic attestation certificate
- **THEN** the persisted credential records an attestation type of basic attestation

#### Scenario: No attestation is recorded

- **WHEN** a credential is registered from an attestation statement conveying no attestation
- **THEN** the persisted credential records an attestation type of `none`

### Requirement: Record the reported resident-key state

Registration options SHALL request the credential properties extension, and on successful registration the system SHALL persist the resident-key state the client reports in that extension's output. The state SHALL be recorded as unknown when the client does not return the extension, since no authenticator data flag conveys it and a client is not obliged to report it.

#### Scenario: Client reports a discoverable credential

- **WHEN** a credential is registered and the client returns a credential properties extension output reporting a resident key
- **THEN** the persisted credential records that it is a resident key

#### Scenario: Client reports a non-discoverable credential

- **WHEN** a credential is registered and the client returns a credential properties extension output reporting no resident key
- **THEN** the persisted credential records that it is not a resident key

#### Scenario: Client omits the extension

- **WHEN** a credential is registered and the client returns no credential properties extension output
- **THEN** the persisted credential records the resident-key state as unknown, and registration still succeeds

## REMOVED Requirements

### Requirement: Relying party configuration

**Reason**: Relying party identity is no longer owned by the registration capability. It now covers a set of accepted origins (web and Android) and a Digital Asset Links association document, which both ceremonies depend on, so it moves to the `webauthn-relying-party` capability.

**Migration**: The requirement is carried over unchanged in substance as `Requirement: Relying party configuration` in `webauthn-relying-party`, which additionally specifies the accepted-origin set and the association document. No behavior is dropped.