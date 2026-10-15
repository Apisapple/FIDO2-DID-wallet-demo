# Spec Delta

## MODIFIED Requirements

### Requirement: Accepted client origins

The relying party SHALL be configured with a set of accepted client origins, and SHALL accept a ceremony only when the client data origin matches a member of that set exactly. The set SHALL accept both web origins (scheme, host and port) and native Android origins of the form `android:apk-key-hash:<base64url-encoded SHA-256 of the signing certificate>`. The set SHALL NOT be empty.

#### Scenario: Browser origin is accepted

- **WHEN** a ceremony is verified whose client data origin is a web origin present in the configured set
- **THEN** the origin check passes and verification continues

#### Scenario: Android origin is accepted

- **WHEN** a ceremony is verified whose client data origin is an `android:apk-key-hash:` origin present in the configured set
- **THEN** the origin check passes and verification continues

#### Scenario: Browser and Android clients share one server instance

- **GIVEN** the configured set contains both a web origin and an `android:apk-key-hash:` origin
- **WHEN** a browser client and an Android client each complete a ceremony against the same server instance
- **THEN** both are verified without reconfiguration between them

#### Scenario: Unlisted origin is rejected

- **WHEN** a ceremony is verified whose client data origin is not a member of the configured set
- **THEN** the system rejects the ceremony with HTTP 200 and `success: false` and a non-empty `message`, and no credential is persisted or updated

#### Scenario: Origin set is empty

- **WHEN** the server starts with an empty or absent accepted-origin set
- **THEN** startup fails with an error naming the missing configuration
