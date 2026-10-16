# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FIDO2 WebAuthn demo server — a Spring Boot backend implementing FIDO2 credential registration and authentication flows. Part of a larger FIDO2-DID Wallet demo (the planned client is Android/Kotlin with DID/ZKP integration).

## Build & Run Commands

```bash
# Build
./gradlew build

# Run server (port 8080). The relying party identity has no defaults, so a run with no
# webauthn.rp.* configuration fails at startup by design; the rp-defaults profile carries
# the demo identity for local development.
./gradlew bootRun --args='--spring.profiles.active=rp-defaults'

# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "com.raonsecure.demoserver.DemoServerApplicationTests"

# Run a specific test method
./gradlew test --tests "com.raonsecure.demoserver.DemoServerApplicationTests.contextLoads"

# Lint check (ktlint)
./gradlew ktlintCheck

# Auto-format Kotlin code (ktlint)
./gradlew ktlintFormat
```

## Tech Stack

- **Kotlin** + **Spring Boot 4.0.3**, Java 21
- **H2** in-memory database (auto-configured, H2 console at `http://localhost:8080/h2-console`)
- **Embedded Redis** — no external Redis needed; dependency is architecture-aware (`codemonstur/embedded-redis` on aarch64/Apple Silicon, `kstyrc/embedded-redis` on x86)
- **Spring Data JPA** with Hibernate (`ddl-auto: update`)
- **Spring MVC** (servlet stack, not reactive)

## Relying Party Configuration

`WebAuthnRpProperties` (`webauthn.rp.*`) carries **no defaults** for `id`, `name` or `origins` — a
missing property is rejected by name at startup rather than binding to a fallback, so the server
cannot serve ceremonies against an undefined relying party (see the `webauthn-relying-party` spec).

The demo identity therefore lives in the `rp-defaults` profile
(`src/main/resources/application-rp-defaults.yml`), not in `application.yml`. Any test that starts a
context needs `@ActiveProfiles("rp-defaults")` or its own `webauthn.rp.*` property values.
`androidApps` keeps its `emptyList()` default: it is genuinely optional.

## Package Structure

All source lives under `com.raonsecure.demoserver`:

| Package | Purpose |
|---|---|
| `base/entity` | JPA entities for persisted FIDO2 data |
| `base/repository` | Spring Data JPA repositories |
| `common/crypto` | Low-level crypto utilities (`Digests`) |
| `common/data` | Shared FIDO2 data types (not persisted) |

## Domain Model

The core FIDO2 domain is:

- **`RpEntity`** — Relying Party; identified by string `id` (origin/domain). Parent of all credentials.
- **`UserKeyEntity`** — A registered FIDO2 credential. Holds `credentialId`, `publicKey` (TEXT/Base64), `signatureAlgorithm` (COSE algorithm int), `signCounter`, `attestationType`, `aaguid`, and optional `rk`/`credProtect` extension values. Uses a Builder pattern.
- **`AuthenticatorTransportEntity`** — Transport hints (`usb`, `nfc`, `ble`, etc.) associated with a `UserKeyEntity` via `@ManyToOne`.
- **`MetadataEntity`** — FIDO2 MDS3 metadata blob keyed by AAGUID.
- **`MetadataTocEntity`** — FIDO2 MDS TOC (Table of Contents) with version tracking (`no` field) per `metadataSource`.
- **`MetadataYubicoEntity`** — Yubico-specific metadata blob.

`UserKeyEntity` → `RpEntity` is `@ManyToOne` (many credentials per RP).  
`AuthenticatorTransportEntity` → `UserKeyEntity` is `@ManyToOne` with `CascadeType.ALL`.

## Coding Conventions

- Entity `id` fields use `var` (not `val`) to satisfy JPA proxy requirements.
- `UserKeyEntity` exposes a nested `Builder` class — use it when constructing instances with many fields.
- Kotlin compiler flags: `-Xjsr305=strict` (null-safety for Spring annotations) and `-Xannotation-default-target=param-property`.
- Code formatting & linting: Ktlint via `./gradlew ktlintFormat` and `./gradlew ktlintCheck`.
