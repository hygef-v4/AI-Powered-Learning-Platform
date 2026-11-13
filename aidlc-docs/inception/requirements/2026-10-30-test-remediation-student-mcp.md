# Test remediation and Student MCP restoration — 2026-10-30

User authorizes fixing the issues identified in the unit-test/BR audit, removing MCP from Admin and restoring Student MCP. Follow-up clarification: MCP must support the same Student AI business operations as the existing personal-provider API. Legacy UC41 is submitted Practice Assignment feedback; UC52 is deterministic Quiz result/review, not a new AI explanation service.

## Approved behavior and execution interpretation

- MCP credential management/authentication: ACTIVE Student/Teacher/Subject Manager, not Admin. Existing Admin credentials stop working through current-role checks; historical usage/credentials are retained, no data deletion/migration needed.
- Remove Admin MCP navigation/route/tab and report-tool availability. Keep normal Admin Settings/AI key/quota/usage management through authorized web APIs.
- Restore Student read_own_work and record_private_feedback using existing PracticeFeedbackService eligibility/hash/schema/daily-limit/ownership rules, plus scoped assignment/material context where the existing legacy business flow requires it. No Student grading, shared quota, official submission/publication or hidden Quiz answers.
- Student PERSONAL provider API and MCP agent are separate transports for the same submitted-own-PRACTICE feedback business operation. MCP agent runs its own model; ALAP validates/stores feedback, it does not become a provider proxy.
- Fix worker ACK/terminal persistence, backup CAS/error sanitization, Quiz result retry eligibility and test false positives. Fill focused test gaps in lifecycle/reveal/module/group/email/dashboard and correct stale doc or impossible fixtures.
- Preserve intentional fail-closed authorization and existing privacy/version/state restrictions. Current UI choices documented by tests stay as source behavior unless a real defect is demonstrated; do not resurrect removed screens solely from stale prose.

## Acceptance

Regression tests fail against the pre-fix baseline before implementation, then pass. Student MCP works through credential/auth/catalog/call and actual scoped service, Admin fails across those boundaries. Non-owner/unsubmitted/GRADED/removed enrollment/stale hash feedback is refused without mutation. Remaining audit findings are individually resolved or explicitly recorded with evidence. Full frontend/backend checks and required DB guard results are reported separately; no live deployment/DB mutation, commit or push.
