# Code generation plan — audit fixes and Student MCP

Spec: [requirements and clarification](../../../inception/requirements/2026-10-30-test-remediation-student-mcp.md). Baseline staging@794a4053 plus preserved uncommitted documentation sync/review. User explicitly requested execution of the concrete audit fixes and MCP role correction; no additional stage approval inferred necessary for that scope. Work directly in the existing checkout to preserve this session's uncommitted artifacts.

- [x] Read current workflow, audit findings and legacy UC/source; confirm UC41 Practice feedback vs UC52 deterministic Quiz review. User clarified API/MCP business parity.
- [x] Write regression tests for U03 durable ACK/failed state/retry, backup CAS/error redaction.
- [x] Repair false-positive fixtures in U05/U09/U10; add Quiz publication/reopen guards.
- [x] Test/fix U11 retry eligibility; add U12 submission, U13 module, U14 recipient flags and U15 activity HTTP guards.
- [x] Restore Student/remove Admin MCP across backend authorization/credential/catalog/tool dispatch and frontend navigation/routes/credentials UI. Add direct negative reveal/multi-key boundary tests where feasible.
- [x] Run the new tests against baseline and retain actual RED evidence before implementation; use sequential Maven runs to avoid shared build output races.
- [x] Implement fixes and run focused GREEN checks; all final integration/review work performed directly after user requested no subagents.
- [x] Update current BR/UC/docs/guides/contracts to corrected implementation and explicit current MCP roles; historical reports/audits remain historical.
- [x] Run full backend/frontend tests, lint/typecheck/build and DB guard; report environment-gated cases accurately.
- [x] Review final diff, fix substantive findings, update state/audit/checkboxes and deliver changed behavior plus evidence. No commit/push/deploy.

Execution preference: the user subsequently requested direct execution without subagents. Existing workers were stopped; all remaining implementation, integration, self-review, documentation and verification are performed directly. Existing completed edits are preserved and checked.

Final evidence: [remediation report](../../build-and-test/2026-10-30-remediation-student-mcp-results.md). Focused104pass; backend1611pass/0skip; frontend599pass/94files; required DBguard and frontend lint/typecheck/build pass. Broader acceptance gaps and migration/backup tradeoffs are explicit in the report.
