# Build and test evidence — consolidated source baseline

**Source checked:** `staging@7e0e8375`, 2026-11-15. Each result below applies to its recorded source and scope. This documentation update ran static checks. Full platform acceptance and NFR certification remain pending.

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

The [build instructions](build-instructions.md) and [guard script](../../../scripts/assert-required-backend-tests.ps1) specify the current CI commands and five required PostgreSQL suites. The results below record the source, scope and limits of each run.

| Recorded scope | Evidence | Result and limit |
|---|---|---|
| U04/U13/U14 chuyển JPA sang JDBC, 18/11, nhánh `refactor/jdbc-u04-u13-u14` (commit cục bộ `a4bd6934`) | [Kết quả](2026-11-18-u04-u13-u14-jdbc-results.md) | Backend `mvn -B verify` 2.572 test, 0 thất bại, 0 lỗi, 1 bỏ qua, gồm test khóa/CAS mới trên PostgreSQL Testcontainers. Frontend không đổi nên không chạy. E2E API `u04-u13-u14` 40/40 trên stack cô lập `jdbc1118`, cần bản vá tạm cho lỗi RabbitMQ có sẵn trên staging (`3ed52391`); chưa thử trên staging. |
| Ngày tháng/AI/đóng nhận bài, 15/11, working tree `fix/date-ai-grading-window` | [Kết quả](2026-11-15-date-ai-grading-window.md) | Frontend 1135/1135, lint, kiểm kiểu src/tests và build qua. Backend theo phạm vi và 282 test tích hợp qua; guard 5 suite PostgreSQL/58 kịch bản qua. Chưa gọi provider thật/nghiệm thu browser; không phải full backend suite. |
| U09 giữ bản nháp khi refresh lỗi, 15/11, bản sửa trong `c376c06a` | [Kết quả](2026-11-15-member-draft-refresh-fix.md) | Vitest 306/306 theo phạm vi, 65/65 khi kiểm trước commit. Typecheck toàn src/tests bằng cấu hình tạm loại cache Next cũ. |
| U04/U13/U14 quy ước mã nguồn, 15/11, working tree nhánh `fix/code-conventions-u04-u13-u14` trước commit | [Kết quả](2026-11-15-u04-u13-u14-code-conventions-results.md) | Backend 2.426 test, đủ 5 suite PostgreSQL. Frontend 1.075/1.092 lần chạy đầu, các test timeout qua khi chạy lại. E2E phạm vi này 40/40. |
| U05 học kỳ, 15/11, nhánh `fix/term` | [Kết quả](2026-11-15-class-term-results.md) | Backend 2.426 test. Frontend 1.082/1.092 lần chạy đầu, các test timeout qua khi chạy lại. API 21/21 và kiểm tra trình duyệt trên stack riêng. |
| U01 MSSV, 14/11, nhánh `fix/student-code` | [Kết quả](2026-11-14-student-code-results.md) | Sau merge staging vào nhánh tính năng, backend 2.373 test. Frontend 1.062/1.066 lần chạy đầu, các file có timeout qua khi chạy lại. |
| U07 nhập đề ảnh/PDF scan, 11/11, nhánh `feat/exam-image-import` | [Kết quả](2026-11-11-exam-image-import-verification.md) | Backend 2.201 test, frontend 969/969, thử trình duyệt với Gemini. |
| U09 move/rename, 06/11; delivered in `1cddff35` before the staging merge | [Full verification](2026-11-06-u09-structure-full-verification.md) | Backend 1,830 passed/0 skipped; all four required PostgreSQL suites passed; frontend 799 passed/116 files; lint without warnings, typecheck and production build passed. Not a new run on `f68a6ec5`. |
| U12 sinh viên tự chia nhóm, 11/11, nhánh `feat/student-self-grouping` chưa commit | [Kết quả](2026-11-11-student-self-grouping-results.md) | Backend 2.203 test qua (`mvn -B verify`, PostgreSQL Testcontainers); frontend lint, typecheck, 978/978 test, build qua; thử hai vai trò trên trình duyệt ở stack riêng. Chưa xem trên giao diện: duyệt nhiều, hạn chót, giao diện điện thoại. |
| Đối chiếu tài liệu với `staging@062a43f5`, 10/11 | [Doc/code sync](2026-11-10-aidlc-code-sync-gaps.md) | Chỉ sửa Markdown cho U01, U04–U10 và các bảng kê; không chạy application test, không phải acceptance. |
| Move-only refactor, 30/10 | [Package/file moves](2026-10-30-package-moves-only-results.md) | Historical 1,611 backend/599 frontend; no conversion into current HEAD acceptance. |
| Remediation, 30/10 | [Remediation result](2026-10-30-remediation-student-mcp-results.md) | Historical 1,611 backend/599 frontend and two lint warnings; original scope/limits retained. |

CI execution status, browser/provider/restore/security/performance acceptance are not inferred from local logs. Disposable PostgreSQL migrations in dated reports do not certify a live migration or restore.

## Runbooks

[Build](build-instructions.md), [automated tests](unit-test-instructions.md), [integration](integration-test-instructions.md), [E2E](e2e-test-instructions.md), [security](security-test-instructions.md), [performance](performance-test-instructions.md). CI tests the deploy script; VPS pulls successful main builds using the [deployment runbook](../../../docs/runbooks/deploy-vps.md). This document does not assert live VPS state. Historical instructions/results retain their dates.
