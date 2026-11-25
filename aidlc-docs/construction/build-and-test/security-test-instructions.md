# Security verification instructions

Baseline staging@794a4053; document sync does not certify runtime controls. Retain SECURITY-03/04/05/08/09/12/15; other controls outside approved project scope N/A, PBT disabled.

- Check current ACTIVE role/session/resource permission server-side for web and MCP. Admin MCP denied, including existing tokens. Student MCP supports own submitted PRACTICE feedback with the same ownership/enrollment/hash/schema/pending/daily-limit rules as personal APIs; no grading/authoring/hidden Quiz answers. Role/scope changes apply next call.
- Reject wrong owner, expired/revoked credential. Since 2026-11-23 there is no token reveal: `GET /api/v1/me/mcp-credentials/{id}/token` must not exist, `mcp_credentials` keeps only `token_hash`/`token_hint`, and a READ_ONLY credential cannot be raised to READ_WRITE (`MCP_ACCESS_UPGRADE_NOT_ALLOWED`). Check CSRF, cache headers, bounds/rate limits, output schema/version and error redaction.
- API keys are write-only (D15 closed 2026-11-11), including institutional keys owned by Admin. Lists and connection responses must not expose raw keys; obsolete API-key reveal routes must not return credentials. Verify direct owner/Admin requests and denied cross-account requests, and ensure error/audit output never contains the submitted key.
- MCP tokens are shown once at creation and are never stored in a decryptable form; hash still authenticates. Historical `MCP_TOKEN_REVEALED` audit rows stay readable. No secret values should be captured as test evidence.
- Recheck scope on module/Quiz attachment and material read, hidden results, grading/submission snapshots, private download tokens and backup metadata.

Use isolated fixtures and record exact commands/results/commit. Existing role/contract/architecture tests and dated manual reports are evidence only for their stated scope/date; not automatically passing at this source baseline. See [decision register](../../inception/reverse-engineering/decision-register.md).
