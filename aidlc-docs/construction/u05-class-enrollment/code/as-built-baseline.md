# U05 Class & Enrollment — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

CourseClass.create tạo DRAFT. OPEN không bắt buộc teacher: domain, AcademicsService và migration 27/10 đã bỏ guard. Nếu có teacher thì account vẫn phải ACTIVE và đúng role. Scope teaching dựa teacher_id hiện hành, manager dựa subject ownership, Student dựa enrollment/status; không tự cấp teaching cho manager chưa được giao.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [academics/controller](../../../../backend/src/main/java/edu/aiplatform/academics/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [academics/service](../../../../backend/src/main/java/edu/aiplatform/academics/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [academics/repository](../../../../backend/src/main/java/edu/aiplatform/academics/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [academics/model](../../../../backend/src/main/java/edu/aiplatform/academics/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [academics/dto](../../../../backend/src/main/java/edu/aiplatform/academics/dto) | Kiểu dữ liệu request/response riêng. |
| [academics/contract](../../../../backend/src/main/java/edu/aiplatform/academics/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks |
|---|---|---|---|---|---|
| [academics](../../../../frontend/src/features/academics) | [api](../../../../frontend/src/features/academics/api) | [model](../../../../frontend/src/features/academics/model) | [pages](../../../../frontend/src/features/academics/pages) | [components](../../../../frontend/src/features/academics/components) | [hooks](../../../../frontend/src/features/academics/hooks) |

Tests: [academics](../../../../backend/src/test/java/edu/aiplatform/academics).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-audit-report.md](../../build-and-test/2026-10-25-u05-u15-audit-report.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-manual-test-guide.md](../../build-and-test/2026-10-25-u05-u15-manual-test-guide.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u05-u15-followup-report.md](../../build-and-test/2026-10-26-u05-u15-followup-report.md)
