# webauthn-relying-party Specification

## Purpose
Defines the relying party's identity for WebAuthn ceremonies: the relying party record, the set of client origins accepted during verification, and the Digital Asset Links document that associates a native Android client with the relying party ID.

## Requirements

### Requirement: Relying party configuration

The system SHALL derive the relying party identity (`id`, `name`) from application configuration, and SHALL ensure a corresponding relying party record exists before any ceremony is served.

#### Scenario: RP available at startup

- **WHEN** the server starts with relying party configuration present
- **THEN** a relying party record matching the configured `id` and `name` exists and is used to build ceremony options

#### Scenario: Relying party ID is absent from configuration

- **WHEN** the server starts without a configured relying party `id`
- **THEN** startup fails with an error naming the missing configuration, rather than serving ceremonies against an undefined relying party

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

### Requirement: Digital Asset Links association

The system SHALL serve a Digital Asset Links statement list at `GET /.well-known/assetlinks.json` describing the Android applications permitted to act as clients for the relying party ID. The document SHALL be built from configuration, SHALL be served as `application/json`, and SHALL declare the `delegate_permission/common.get_login_creds` relation for each configured application.

#### Scenario: Association document is served

- **WHEN** a client issues `GET /.well-known/assetlinks.json`
- **THEN** the system returns HTTP 200 with content type `application/json` and a statement list in which each entry declares `delegate_permission/common.get_login_creds` for a configured Android package name and its SHA-256 signing certificate fingerprints

#### Scenario: No Android application is configured

- **WHEN** no Android application is configured and a client issues `GET /.well-known/assetlinks.json`
- **THEN** the system returns HTTP 200 with an empty statement list, so the endpoint remains well-formed for clients that probe it

#### Scenario: Association document requires no authentication

- **WHEN** an unauthenticated client issues `GET /.well-known/assetlinks.json`
- **THEN** the document is returned without a credential challenge, since the Android platform fetches it anonymously
