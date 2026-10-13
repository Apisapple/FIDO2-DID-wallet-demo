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

### Requirement: Enforce user presence during authentication

Assertion verification SHALL require the user presence flag in the authenticator data to be set. An assertion whose authenticator data does not assert user presence SHALL be rejected and no credential state modified.

#### Scenario: User presence asserted

- **WHEN** a client POSTs an otherwise valid assertion whose authenticator data has the user presence flag set
- **THEN** verification continues and, on success, the credential's sign counter is updated

#### Scenario: User presence not asserted

- **WHEN** a client POSTs an assertion whose authenticator data does not have the user presence flag set
- **THEN** the system rejects the request with `status: "failed"` and a non-empty `errorMessage`, and no credential state is modified

### Requirement: Enforce the advertised user verification requirement

The user verification requirement advertised in the authentication options SHALL be stored alongside the challenge and enforced at verification time. When the advertised requirement was `required`, an assertion whose authenticator data does not assert user verification SHALL be rejected. When it was `preferred` or `discouraged`, the user verification flag SHALL NOT cause rejection.

#### Scenario: Required user verification is satisfied

- **GIVEN** authentication options advertised `userVerification: "required"`
- **WHEN** a client POSTs an assertion whose authenticator data asserts user verification
- **THEN** verification continues and, on success, the credential's sign counter is updated

#### Scenario: Required user verification is absent

- **GIVEN** authentication options advertised `userVerification: "required"`
- **WHEN** a client POSTs an assertion whose authenticator data does not assert user verification
- **THEN** the system rejects the request with `status: "failed"` and a non-empty `errorMessage`, and no credential state is modified

#### Scenario: Preferred user verification is absent

- **GIVEN** authentication options advertised `userVerification: "preferred"`
- **WHEN** a client POSTs an assertion whose authenticator data does not assert user verification
- **THEN** the user verification flag does not cause rejection and, on success, the credential's sign counter is updated

### Requirement: Bind the asserted user handle to the credential

When an assertion carries a `userHandle`, verification SHALL reject the assertion unless the handle equals the user handle stored for the referenced credential. When the authentication ceremony was initiated without a username, the assertion SHALL carry a `userHandle`; an assertion without one SHALL be rejected, because the server has no other means of establishing which user is authenticating.

#### Scenario: User handle matches the credential

- **WHEN** a client POSTs an assertion whose `userHandle` equals the user handle stored for the referenced credential
- **THEN** verification continues and, on success, the credential's sign counter is updated

#### Scenario: User handle belongs to a different user

- **WHEN** a client POSTs an assertion whose `userHandle` is a valid handle for some other user, not the one stored for the referenced credential
- **THEN** the system rejects the request with `status: "failed"` and a non-empty `errorMessage`, and no credential state is modified

#### Scenario: Usernameless ceremony omits the user handle

- **GIVEN** the authentication options were requested without a username
- **WHEN** a client POSTs an assertion carrying no `userHandle`
- **THEN** the system rejects the request with `status: "failed"` and a non-empty `errorMessage`, and no credential state is modified

#### Scenario: Named ceremony omits the user handle

- **GIVEN** the authentication options were requested with a username and returned a non-empty `allowCredentials`
- **WHEN** a client POSTs an assertion carrying no `userHandle`
- **THEN** the absent handle does not cause rejection, and the user is established from the referenced credential

### Requirement: Sign counter regression is distinguishable

A rejection caused by a sign counter that did not advance SHALL be reported as a distinct condition from other verification failures, so that a possible cloned or replayed authenticator can be told apart from a malformed or unauthentic assertion.

#### Scenario: Regressed counter is reported as such

- **GIVEN** a credential whose stored sign counter is greater than zero
- **WHEN** a client POSTs an assertion that is otherwise valid but whose `signCount` is less than or equal to the stored counter
- **THEN** the system rejects the request with `status: "failed"` and an `errorMessage` identifying a sign counter regression, and no credential state is modified

#### Scenario: Other failures are not reported as regression

- **WHEN** a client POSTs an assertion rejected for a reason other than the sign counter, such as an invalid signature
- **THEN** the `errorMessage` does not identify a sign counter regression

#### Scenario: Authenticator that does not increment its counter

- **GIVEN** a credential whose stored sign counter is zero
- **WHEN** a client POSTs a valid assertion whose `signCount` is zero
- **THEN** the assertion is accepted, since an authenticator that does not maintain a counter reports zero on every ceremony
