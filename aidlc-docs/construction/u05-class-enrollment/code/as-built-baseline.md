# U05 Class & Enrollment — AS-BUILT source snapshot 2026-11-07

**Convention delta 2026-11-05:** concrete actions use verb-object names (`loadClass`, `listStudents`, `createClass`, `enrollStudents`); dependency fields/constructor arguments identify their service/repository roles. `transactionTemplate` and `lineTransactionTemplate` remain separate; the latter retains per-line REQUIRES_NEW behavior. Frontend API actions use explicit names, component modules are `AcademicsUi.tsx`, `ClassNavContexts.tsx`, `ClassRoutes.tsx`, with all imports/mocks updated. Java wildcard imports retained per the user's follow-up preference; explicit imports are allowed where names collide. DTO/record fields, contract overrides, routes, parameter bindings and behavior remain unchanged. [Naming verification and exceptions](../../build-and-test/2026-11-05-u05-u09-u15-conventions-results.md).

**Follow-up delta 2026-11-05:** moved `formatDate`/`operationError` from UI components to `model/format.ts`, updating callers. Vietnam dates, invalid-date fallback, conflict copy and suppression of unsafe API details remain unchanged. Removed one unused controller repository import; no authorization, lock, contract or schema change. [Current verification and limits](../../build-and-test/2026-11-05-u05-u09-u15-followup-results.md).

**Cleanup delta 2026-11-04:** removed one consecutive duplicate manager-scope check in class creation. Checks before account locking and after subject locking remain; no authorization/transaction or schema change. [Evidence](../../build-and-test/2026-11-04-u05-u09-u15-cleanup-results.md).

Source snapshot `f68a6ec5889074de80a1f1f0faf7bcb7e7054ec8`; current source paths are catalogued in [current-code-baseline](../../../inception/reverse-engineering/current-code-baseline.md). Dated deltas/test results below retain their original scope; this update is not runtime acceptance.

**Contract delta 2026-11-01:** U05 no longer exposes the manager class-wide distribution mutation. Historical `course_classes.show_grade_distribution` and the retained `showGradeDistribution` DTO/`ClassRef` field remain dormant; no true flags migrate to the new settings. U09 owns assignment-class visibility and never uses this field as a gate; U05 supplies current teaching scope and active enrollment. Verification for this delta belongs to the current change, not the older reports below.

CourseClass.create tạo DRAFT. OPEN không bắt buộc teacher: domain, AcademicsService và migration 27/10 đã bỏ guard. Nếu có teacher thì account vẫn phải ACTIVE và đúng role. Scope teaching dựa teacher_id hiện hành, manager dựa subject ownership, Student dựa enrollment/status; không tự cấp teaching cho manager chưa được giao.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [academics/controller](../../../../backend/src/main/java/edu/aiplatform/academics/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [academics/service](../../../../backend/src/main/java/edu/aiplatform/academics/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [academics/repository](../../../../backend/src/main/java/edu/aiplatform/academics/repository) | Repository interface/implementation, Spring JDBC/SQL và persistence mapping. |
| [academics/model](../../../../backend/src/main/java/edu/aiplatform/academics/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [academics/dto](../../../../backend/src/main/java/edu/aiplatform/academics/dto) | Kiểu dữ liệu request/response riêng. |
| [academics/contract](../../../../backend/src/main/java/edu/aiplatform/academics/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [academics](../../../../frontend/src/features/academics) | [api](../../../../frontend/src/features/academics/api) | [model](../../../../frontend/src/features/academics/model) | [pages](../../../../frontend/src/features/academics/pages) | [components](../../../../frontend/src/features/academics/components) | [hooks](../../../../frontend/src/features/academics/hooks) | [styles](../../../../frontend/src/features/academics/styles) | [tests](../../../../frontend/tests/features/academics) |

Tests: [academics](../../../../backend/src/test/java/edu/aiplatform/academics).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-audit-report.md](../../build-and-test/2026-10-25-u05-u15-audit-report.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-manual-test-guide.md](../../build-and-test/2026-10-25-u05-u15-manual-test-guide.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u05-u15-followup-report.md](../../build-and-test/2026-10-26-u05-u15-followup-report.md)

## Delta working tree — 2026-11-09

EnrollmentPanel đọc Excel .xlsx một cột email qua enrollmentWorkbook, tối đa 100 KB/200 dòng, mẫu enrollment-import.xlsx. Vẫn cho dán email và dùng API danh sách text; server kiểm lại quyền và từng dòng.

Delta này bổ sung snapshot commit có ngày phía trên, chưa tạo commit mới và không sửa lại evidence lịch sử. [Kết quả và giới hạn](../../build-and-test/2026-11-09-excel-import-results.md); [đối chiếu tài liệu](../../build-and-test/2026-11-09-aidlc-doc-sync.md).
