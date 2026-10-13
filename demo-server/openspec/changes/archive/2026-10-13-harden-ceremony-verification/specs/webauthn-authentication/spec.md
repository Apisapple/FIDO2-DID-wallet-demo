# Spec Delta

## ADDED Requirements

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
