# Build and test evidence — consolidated source baseline

**Documentation baseline 2026-10-30: staging@794a405304bf02dd2593a9fa2c88662d7cd52845**. The initial documentation-only sync performed no application run; the subsequent approved remediation now has fresh automated verification below. All 15 units have source; full platform acceptance and NFR certification remain pending.

## Evidence matrix

| Scope/date | Evidence | Status and limit |
|---|---|---|
| U14 21/10 | [u14-verification.md](u14-verification.md) | historical-test-evidence; prior checkout/schema/Docker assumptions, not current blockers |
| U02/U06/U12 24/10 | [e2e-u02-u06-u12-2026-10-24.md](e2e-u02-u06-u12-2026-10-24.md) | historical-test-evidence; exact fixture/failure/fix/rerun scope inside report |
| U04/U13/U14 remediation 24/10 | [u04-u13-u14-remediation-2026-10-24.md](../remediation/u04-u13-u14/verification/u04-u13-u14-remediation-2026-10-24.md) | historical-test-evidence, not fresh HEAD acceptance |
| B2/B8 UI + Mailpit 25/10 | [2026-10-25-b2-ui-smtp-acceptance-results.md](2026-10-25-b2-ui-smtp-acceptance-results.md) | historical local UI/SMTP evidence; not real delivery/inbox certification |
| U09 structured feedback 26/10 | [2026-10-26-u09-structured-feedback-report.md](2026-10-26-u09-structured-feedback-report.md) | historical source/automated verification; scope and manual limits in report |
| U05/U15 followup 26/10 | [2026-10-26-u05-u15-followup-report.md](2026-10-26-u05-u15-followup-report.md) | historical scoped fixes/verification |
| MCP staff expansion 26/10 | [2026-10-26-mcp-staff-expansion-results.md](2026-10-26-mcp-staff-expansion-results.md) | historical full/focused/isolated PostgreSQL runs; 91 frontend files/560 tests recorded for that run, not HEAD |
| Merged MCP/staging 26/10 | [audit.md](../../audit.md) | historical integration entry: 92 files/565 frontend and 101 focused backend; a separate run, not added to previous totals |
| Code/docs 30/10 | [current-code-baseline.md](../../inception/reverse-engineering/current-code-baseline.md) | source-observed only; Markdown/OpenAPI/XML/export verification has separate sync report |

## Current gates

Current move-only working tree at parent HEAD `5a76e428`: backend 1,611 passed/0 skipped and required DB guard passed; frontend 599 passed/94 files, typecheck/build passed, lint exit 0 with two existing warnings. All original source/resource files retained; broader refactor edits withdrawn. See [package/file move results](2026-10-30-package-moves-only-results.md). No stage, commit, push or history rewrite.

Fresh local automated verification: backend 1,611 passed/0 skipped, required DB guard passed, frontend 599 passed/94 files, lint/typecheck/build passed (2 existing lint warnings). See [2026-10-30-remediation-student-mcp-results.md](2026-10-30-remediation-student-mcp-results.md). CI, browser/runtime/provider/restore/security/performance acceptance remains pending. Docker and disposable PostgreSQL migrations were exercised in the remediation; the older “Docker stopped”, skipped-suite and missing-schema statements describe earlier runs. No live application database migration, provider call, email/grade publication or deployment was performed.

## Runbooks

[Build](build-instructions.md), [automated tests](unit-test-instructions.md), [integration](integration-test-instructions.md), [E2E](e2e-test-instructions.md), [security](security-test-instructions.md), [performance](performance-test-instructions.md). CI source and required DB-suite guard are linked in build instructions; deploy job remains disabled. Historical instructions and results are preserved with dates.
