# Design

## Context

See `proposal.md` — Why.

Constraints that shape the approach:

- **Spring Boot's dependency management wins over a plain BOM.** The `io.spring.dependency-management` plugin overrides a `platform(...)` constraint declared in `dependencies`. Verified directly: adding `implementation platform('tools.jackson:jackson-bom:3.2.1')` left the tree at `3.2.1 -> 3.0.4`. The supported lever is overriding the version property Spring Boot's own BOM uses.
- **Spring Boot 4.0.3 manages two Jackson BOMs.** Its `spring-boot-dependencies` POM declares `jackson-bom.version` (3.0.4, the `tools.jackson` artifacts) and `jackson-2-bom.version` (2.20.2, the `com.fasterxml.jackson` annotations). Jackson 3 still depends on Jackson 2's annotations artifact, so the two move together.
- **The artifact version and the BOM version differ.** `jackson-annotations` publishes `2.22`, but there is no `jackson-bom:2.22` — the BOM line is `2.22.x`. Setting `jackson-2-bom.version` to `2.22` fails resolution outright.
- **Raising Jackson is not confined to webauthn4j.** Spring's HTTP message conversion uses the same databind, so a version bump has to be verified against the application's own serialization, not just the CBOR path.

## Goals / Non-Goals

**Goals**

- Make `WebAuthnManager.verify` executable at runtime.
- Leave a regression test that fails if the conflict returns.

**Non-Goals**

- Changing what verification checks. That is `harden-ceremony-verification`.
- Upgrading Jackson for its own sake, or pinning any other managed dependency.
- Replacing the existing mock-based service tests.

## Decisions

### Override both Jackson BOM version properties

```groovy
ext['jackson-bom.version'] = '3.2.1'
ext['jackson-2-bom.version'] = '2.22.3'
```

Why both: pinning only `jackson-bom.version` resolved databind to 3.2.1 but left annotations at 2.20, which moved the failure rather than fixing it —

```
java.lang.NoClassDefFoundError: com/fasterxml/jackson/annotation/JsonSerializeAs
  at tools.jackson.databind.introspect.JacksonAnnotationIntrospector.<clinit>
```

`JsonSerializeAs` is absent from `jackson-annotations` 2.20 and present in 2.22 (confirmed by inspecting both jars), and databind 3.2.1 declares 2.22. Overriding `jackson-2-bom.version` to the `2.22.3` BOM line brings annotations to 2.22.

Alternatives considered:

- **`platform(...)` constraint in `dependencies`** — does not take effect; Spring's plugin overrides it.
- **`resolutionStrategy.force`** — would work but applies a blunt override outside Spring's dependency management, so Spring's own BOM and the forced version could disagree silently on a Boot upgrade. The `ext[...]` property is the mechanism the plugin documents for exactly this.
- **Downgrading webauthn4j to a release built against databind 3.0.x** — leaves the project on an older FIDO2 library to accommodate a transitive version, and the 0.31.x line targets Jackson 3.2.

### Make the verification path executable by tests, not just correct in configuration

A configuration fix alone would leave the suite unable to tell whether verification works. The test fixture drives webauthn4j's `ClientPlatform` and `PackedAuthenticator` emulator against a real `WebAuthnManager`, through the actual `RegistrationService` and `AuthenticationService`, with an in-memory `ChallengeStore` and a list-backed `UserKeyRepository` fake.

Using a real `WebAuthnManager` is the point: the original defect is invisible to any test that mocks it. The in-memory store also exercises the real consume-once challenge behavior rather than stubbing it.

Hand-crafting CBOR attestation blobs was rejected as unmaintainable.

### Verify the bump against the application's own JSON, not only webauthn4j

Because Spring shares the databind, verification includes starting the application and exercising an endpoint end to end, in addition to the ceremony round trip. A green `./gradlew test` is necessary but not sufficient — the existing suite was green while the server was broken.

## Risks / Trade-offs

- **Jackson 3.2.1 is ahead of what Spring Boot 4.0.3 was tested against** → Verified by running the application and exercising request deserialization and response serialization, plus the full suite. Residual risk is a Spring code path neither the suite nor the smoke check reaches; the mitigation if that surfaces is pinning Jackson back and downgrading webauthn4j instead.
- **A future Spring Boot upgrade may change these property names or ship a compatible Jackson** → The override is commented with the reason, so it can be removed when the managed version satisfies webauthn4j. Leaving a stale override is the failure mode to watch.
- **The emulator exercises one authenticator model (packed, ES256)** → It covers the CBOR extension-output path that broke. Broader authenticator and algorithm coverage is worth adding but is not what this defect requires.

## Migration Plan

No configuration or data changes; the override affects dependency resolution only. Rollback is reverting the commit, which restores the managed Jackson version and the original failure.
