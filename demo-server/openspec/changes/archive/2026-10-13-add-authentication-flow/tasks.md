## 1. Challenge & Context Management

- [x] 1.1 Create `PendingAuthenticationContext` class and extend `ChallengeStore` interface with authentication challenge save and consume methods
- [x] 1.2 Implement authentication challenge operations in `RedisChallengeStore` with Redis key prefix and TTL, and verify with unit tests in `RedisChallengeStoreTest`

## 2. Authentication Domain & Service

- [x] 2.1 Define authentication domain exceptions (`AuthenticationException`, `AuthenticationChallengeNotFoundException`, `CredentialNotFoundException`, `SignCounterRegressionException`)
- [x] 2.2 Implement `AuthenticationService.createOptions` to generate challenge, resolve user credentials for `allowCredentials`, and persist pending authentication context in Redis
- [x] 2.3 Implement `AuthenticationService.verify` to validate assertion with `WebAuthnManager`, verify public key signature, check sign counter progression, and update `UserKeyEntity.signCounter`
- [x] 2.4 Add unit tests in `AuthenticationServiceTest` verifying options generation and assertion verification logic

## 3. Web Layer & Exception Handling

- [x] 3.1 Implement `AuthenticationController` exposing `POST /assertion/options` and `POST /assertion/result`
- [x] 3.2 Implement `AuthenticationExceptionHandler` (`@RestControllerAdvice`) returning standardized FIDO error response envelopes
- [x] 3.3 Add unit tests in `AuthenticationControllerTest` covering request validation, success envelopes, and error handling

## 4. Verification & Testing

- [x] 4.1 Run full build and test suite via `./gradlew test` to verify no regressions in existing registration flow and all authentication tests pass
