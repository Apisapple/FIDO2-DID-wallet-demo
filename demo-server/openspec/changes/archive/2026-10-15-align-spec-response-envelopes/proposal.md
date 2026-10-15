# Proposal

## Why

The three webauthn specs describe a failure envelope the server has never sent. In 18 places they
require `status: "failed"` with a non-empty `errorMessage`; every handled failure actually returns
`{"success": false, "message": "..."}`. `docs/api.md` already documents the real shape and flags it
as unintended, and the controller slice tests assert `$.success` — so the specs are the only
artifact still describing the old contract, and they cannot be used to review the code.

Two smaller defects compound it: the authentication spec offers a choice of two behaviours for the
same scenario, which makes that requirement unverifiable, and a registration options request with
no `username` field at all escapes both envelopes and returns a Spring error page carrying a stack
trace.

## What Changes

- Rewrite the failure envelope in all three specs to the implemented shape: `success: false` with a
  non-empty `message`. No production code changes for this part — it records the contract that
  `docs/api.md` and the controller tests already encode.
- State explicitly in each spec that a handled failure answers **HTTP 200** with `success: false`,
  and that the success envelopes are asymmetric: options answer with `status`/`options`, results
  and all handled failures answer with `success`/`message`. The specs are currently silent on
  status codes, which is why the mismatch went unnoticed.
- Resolve the self-contradiction in `webauthn-authentication`. The "User with no credentials
  requests options" scenario currently permits either a failure **or** an empty `allowCredentials`
  "depending on policy". Fix the policy as the implemented one — reject with `success: false` — so
  the requirement is testable.
- Require that a malformed or incomplete request body is answered with the same failure envelope
  rather than a framework error page. This is the one part with observable behaviour change:
  `POST /attestation/options` with `{}` currently returns a Spring error page including a
  `trace` field. **BREAKING** for any client parsing that body, though no such client exists yet.
- Restate the sign counter requirement so the rule and its exception read as one sentence instead
  of a requirement body contradicted by a later scenario. No behaviour change; webauthn4j already
  implements the accepted semantics.

## Capabilities

### New Capabilities

None. This change corrects existing specs; it introduces no new system behaviour.

### Modified Capabilities

- `webauthn-registration`: failure envelope in 7 scenarios; add the HTTP status and envelope-shape
  requirement; require the envelope for an unreadable request body (the missing-`username` case).
- `webauthn-authentication`: failure envelope in 10 scenarios; add the HTTP status and
  envelope-shape requirement; fix the no-credentials scenario to a single policy; restate the sign
  counter requirement so body and scenarios agree.
- `webauthn-relying-party`: failure envelope in the unlisted-origin scenario.

## Impact

- **Specs**: all three files under `openspec/specs/`. This is the bulk of the change.
- **Code**: one addition only — a handler for an unreadable request body, so a malformed payload
  answers with the project's envelope instead of Spring's default error response. The envelope
  realignment itself requires no code change.
- **Tests**: a slice test for the malformed-body path. Existing controller tests already assert the
  envelope this change writes into the specs and need no edits.
- **Docs**: `docs/api.md` can drop its note disclaiming the envelope as "current behaviour, not an
  intended contract", since the specs will then agree with it. Its description of the two shapes is
  already accurate.
- **Not in scope**: unifying the two envelopes into one shape. That would change the success path
  and every client, and is a larger decision than correcting documentation that is simply wrong.
