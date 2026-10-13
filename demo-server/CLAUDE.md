# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FIDO2 WebAuthn demo server — a Spring Boot backend implementing FIDO2 credential registration and authentication flows. Part of a larger FIDO2-DID Wallet demo (the planned client is Android/Kotlin with DID/ZKP integration).

## Build & Run Commands

```bash
# Build
./gradlew build

# Run server (port 8080)
./gradlew bootRun

# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "com.raonsecure.demoserver.DemoServerApplicationTests"

# Run a specific test method
./gradlew test --tests "com.raonsecure.demoserver.DemoServerApplicationTests.contextLoads"
```

## Tech Stack

- **Kotlin** + **Spring Boot 4.0.3**, Java 21
- **H2** in-memory database (auto-configured, H2 console at `http://localhost:8080/h2-console`)
- **Embedded Redis** — no external Redis needed; dependency is architecture-aware (`codemonstur/embedded-redis` on aarch64/Apple Silicon, `kstyrc/embedded-redis` on x86)
- **Spring Data JPA** with Hibernate (`ddl-auto: update`)
- **Spring MVC** (servlet stack, not reactive)

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
