# Build and test evidence — consolidated source baseline

**Current source inventory: `f68a6ec5`, 2026-11-07**, after the staging merge. This documentation-only sync performs no application run. All 15 units have source; full platform acceptance and NFR certification remain pending. The dated evidence below belongs to its recorded source/scope, not automatically to current HEAD.

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

Fresh full application verification after merge `f68a6ec5` is not established by this documentation update. Source inventory and link checks are separate from runtime/test acceptance. CI commands and the four required PostgreSQL suites are specified in [build instructions](build-instructions.md) and the guard script.

| Recorded scope | Evidence | Result and limit |
|---|---|---|
| U09 move/rename, 06/11; delivered in `1cddff35` before the staging merge | [Full verification](2026-11-06-u09-structure-full-verification.md) | Backend 1,830 passed/0 skipped; all four required PostgreSQL suites passed; frontend 799 passed/116 files; lint without warnings, typecheck and production build passed. Not a new run on `f68a6ec5`. |
| U12 sinh viên tự chia nhóm, 11/11, nhánh `feat/student-self-grouping` chưa commit | [Kết quả](2026-11-11-student-self-grouping-results.md) | Backend 2.203 test qua (`mvn -B verify`, PostgreSQL Testcontainers); frontend lint, typecheck, 978/978 test, build qua; thử hai vai trò trên trình duyệt ở stack riêng. Chưa xem trên giao diện: duyệt nhiều, hạn chót, giao diện điện thoại. |
| Đối chiếu tài liệu với `staging@062a43f5`, 10/11 | [Doc/code sync](2026-11-10-aidlc-code-sync-gaps.md) | Chỉ sửa Markdown cho U01, U04–U10 và các bảng kê; không chạy application test, không phải acceptance. |
| Move-only refactor, 30/10 | [Package/file moves](2026-10-30-package-moves-only-results.md) | Historical 1,611 backend/599 frontend; no conversion into current HEAD acceptance. |
| Remediation, 30/10 | [Remediation result](2026-10-30-remediation-student-mcp-results.md) | Historical 1,611 backend/599 frontend and two lint warnings; original scope/limits retained. |

CI execution status, browser/provider/restore/security/performance acceptance are not inferred from local logs. Disposable PostgreSQL migrations in dated reports do not certify a live migration or restore.

## Runbooks

[Build](build-instructions.md), [automated tests](unit-test-instructions.md), [integration](integration-test-instructions.md), [E2E](e2e-test-instructions.md), [security](security-test-instructions.md), [performance](performance-test-instructions.md). CI tests the deploy script; VPS pulls successful main builds using the [deployment runbook](../../../docs/runbooks/deploy-vps.md). This document does not assert live VPS state. Historical instructions/results retain their dates.
