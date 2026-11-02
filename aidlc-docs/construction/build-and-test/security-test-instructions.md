# Security verification instructions

Baseline staging@794a4053; document sync does not certify runtime controls. Retain SECURITY-03/04/05/08/09/12/15; other controls outside approved project scope N/A, PBT disabled.

- Check current ACTIVE role/session/resource permission server-side for web and MCP. Admin MCP denied, including existing tokens. Student MCP supports own submitted PRACTICE feedback with the same ownership/enrollment/hash/schema/pending/daily-limit rules as personal APIs; no grading/authoring/hidden Quiz answers. Role/scope changes apply next call.
- Reject wrong owner, expired/revoked credential; legacy missing ciphertext cannot reveal. Check CSRF, cache headers, bounds/rate limits, output schema/version and error redaction.
- API-key list stays masked. Source exception: Admin institutional-key reveal for non-REVOKED connection returns raw key and records AI_CONNECTION_KEY_REVEALED. This supersedes the blanket old no-readback description; security design review of that exception remains pending. Personal keys are not thereby readable by Admin.
- Owner ACTIVE MCP credential reveal when ciphertext exists records MCP_TOKEN_REVEALED; hash still authenticates, encryption key must stay out of doc/logs. No secret values should be captured as test evidence.
- Recheck scope on module/Quiz attachment and material read, hidden results, grading/submission snapshots, private download tokens and backup metadata.

Use isolated fixtures and record exact commands/results/commit. Existing role/contract/architecture tests and dated manual reports are evidence only for their stated scope/date; not automatically passing at this source baseline. See [decision register](../../inception/reverse-engineering/decision-register.md).
