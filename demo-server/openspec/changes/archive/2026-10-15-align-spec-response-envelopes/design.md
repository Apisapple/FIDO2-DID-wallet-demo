# Design

## Context

See proposal.md — Why. Two facts shape the approach:

The envelope realignment is a documentation correction. `WebAuthnVerificationResponse`
(`success`/`message`) is what every handler already returns, `docs/api.md` already describes it,
and the controller slice tests already assert `$.success`. Nothing in the running system changes.

The malformed-body requirement is the one behavioural change, and it lands on an asymmetry:
`AuthenticationExceptionHandler` is scoped with
`@RestControllerAdvice(basePackageClasses = [AuthenticationController::class])`, while
`RegistrationExceptionHandler` is a bare `@RestControllerAdvice` and therefore global. Both declare
an `IllegalArgumentException` handler, so the registration advice already answers for exceptions
raised anywhere in the application, including `AssetLinksController`.

## Goals / Non-Goals

**Goals:**

- Leave the specs reviewable against the code: a reader comparing them should find no disagreement.
- Answer an unreadable request body with the project's envelope, and without a stack trace.
- Resolve the handler-scope asymmetry rather than build on top of it.

**Non-Goals:**

- Unifying the two envelopes. Out of scope per the proposal, and it would change the success path.
- Reworking the exception hierarchy or adding `@Valid`/Bean Validation. The endpoints validate by
  hand today; introducing a second validation mechanism for one field is not warranted.
- Changing which HTTP status a handled failure carries. The specs will record HTTP 200 as the
  contract. Whether that is a good contract is a separate question from whether it is documented.

## Decisions

**Handle `HttpMessageNotReadableException` in one shared advice, not per package.**

A malformed body is a transport-level failure identical for both endpoints, so duplicating the
handler in two package-scoped advices would mean two copies of the same mapping. Instead, scope the
registration advice to its own package — matching what the authentication advice already does — and
add a single application-wide advice holding the body-parse mapping. This removes the existing
accidental global reach of `RegistrationExceptionHandler` in the same step, which is what makes the
narrowing safe to do here rather than deferring it.

*Alternative considered:* add the mapping to `RegistrationExceptionHandler` and leave it global.
Rejected — it keeps a registration-named class answering for the whole application and leaves the
authentication endpoint's malformed-body behaviour depending on the registration package.

*Alternative considered:* a global `ProblemDetail`/RFC 7807 response. Rejected — it introduces a
third envelope shape when the complaint being fixed is that there are already two.

**Make `username` nullable in the DTO and reject it in one place.**

`RegistrationOptionsRequest.username` is non-null, so Jackson throws before any application code
runs — which is why the field being absent and the field being blank fail along different paths and
produce different responses. Making it nullable moves both cases to the existing blank check in
`RegistrationController`, which already returns the right envelope. The spec delta states the
absent and blank cases as one scenario because, after this, they are one code path.

*Alternative considered:* catch the Jackson failure only. Rejected — it would answer with the right
envelope while leaving two paths for one condition, and the message would be derived from a parse
error rather than naming the missing field.

**Keep the message free of internal detail.**

The current Spring error page includes a `trace` field exposing the exception chain and DTO class
name. The replacement message names the problem (an unreadable body) without echoing the parser's
output, so the response does not leak class names or stack frames.

## Risks / Trade-offs

**Narrowing `RegistrationExceptionHandler` changes behaviour for non-registration endpoints** →
Today an `IllegalArgumentException` from `AssetLinksController` is answered by the registration
advice with a `success: false` body. After narrowing, it would fall through to Spring's default. In
practice `AssetLinksController` throws no such exception — its only inputs come from validated
configuration — so there is no live path. The tasks include a check for other throwers before the
narrowing lands, and `AssetLinksControllerTest` covers the endpoint's happy path.

**The specs will record HTTP 200 for failures, making an odd contract look sanctioned** → Mitigated
by stating it as an explicit observation with its consequence — that a client cannot judge the
outcome by status code — rather than as a neutral detail. A later change may revisit it; it will
then be changing a contract that is written down rather than discovering it.

**Scenario count grows, and a hand-edited spec can drift again** → The new scenarios are written to
be directly testable, and the tasks pair the malformed-body scenarios with slice tests. The
envelope scenarios are already covered by the existing controller tests.

## Migration Plan

No data migration and no deployment coordination: the server is a demo with an in-memory database,
and the only behavioural change is the response body for a request that currently fails anyway.

Rollback is reverting the commit. The spec edits and the code change are separable — the spec
realignment alone is inert with respect to the running system, so if the handler change proves
troublesome it can be reverted on its own while the corrected specs stand.
