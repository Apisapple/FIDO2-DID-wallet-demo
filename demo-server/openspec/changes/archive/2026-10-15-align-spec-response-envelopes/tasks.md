# Tasks

## 1. Realign the specs with the implemented envelope

- [x] 1.1 Sync the three delta specs into `openspec/specs/` and verify `grep -rc 'status: "failed"' openspec/specs/` reports 0 in all three files
- [x] 1.2 Verify the synced specs still describe the running server: start it, POST an attestation result with an unknown challenge, and confirm the response is HTTP 200 with `success: false` and a non-empty `message` as the specs now state
- [x] 1.3 Drop the disclaimer in `docs/api.md` that calls the envelope "current behaviour, not an intended contract", keeping its description of the two shapes, and verify no remaining sentence in that file contradicts the synced specs

## 2. Route malformed request bodies through the project envelope

- [x] 2.1 Check for other throwers of `IllegalArgumentException` outside the registration package (`grep -rn "IllegalArgumentException\|require(\|requireNotNull" src/main/kotlin --include=*.kt`) and record whether any reachable endpoint relies on the registration advice's current global reach — if one does, stop and raise it before narrowing
- [x] 2.2 Scope `RegistrationExceptionHandler` to its own package with `basePackageClasses`, matching `AuthenticationExceptionHandler`, and verify the full suite still passes
- [x] 2.3 Add an application-wide advice handling `HttpMessageNotReadableException` that answers with `success: false` and a message naming an unreadable body, carrying no parser output, class name or stack trace
- [x] 2.4 Make `RegistrationOptionsRequest.username` nullable so an absent field reaches the controller's existing blank check, and verify `POST /attestation/options` with `{}` answers with the envelope rather than a Spring error page
- [x] 2.5 Add slice tests for both malformed-body scenarios — body omitting `username`, and body that is not valid JSON — asserting `$.success` is false, `$.message` is non-empty, and the response carries no `trace` field
- [x] 2.6 Add a slice test that an absent and a blank `username` produce the same envelope, covering the single "Missing username" scenario the registration spec now states

## 3. Verify the whole change

- [x] 3.1 Run `./gradlew test` and verify the suite passes with no regressions in the existing controller, ceremony and config tests
- [x] 3.2 Run `openspec validate --specs` and verify the synced specs parse with no errors
- [x] 3.3 Start the server and walk one full registration and one full authentication ceremony through `/` to verify neither the narrowed advice nor the nullable DTO broke a success path

## Workflow follow-up

- Archive the change once the work is reviewed.
