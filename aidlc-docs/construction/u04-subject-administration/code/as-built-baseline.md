# U04 Subject Administration — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

subjects có quản trị danh sách/chi tiết/create/update, manager assignment, archive/reopen và version guards. UI tại admin/subjects; Subject Detail của Admin không cấp quyền chấm hoặc sửa lớp. Các handoff raw paths bị mất được thay bằng baseline này, không tạo lại nội dung approval giả.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [subjects/controller](../../../../backend/src/main/java/edu/aiplatform/subjects/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [subjects/service](../../../../backend/src/main/java/edu/aiplatform/subjects/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [subjects/repository](../../../../backend/src/main/java/edu/aiplatform/subjects/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [subjects/model](../../../../backend/src/main/java/edu/aiplatform/subjects/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [subjects/dto](../../../../backend/src/main/java/edu/aiplatform/subjects/dto) | Kiểu dữ liệu request/response riêng. |
| [subjects/contract](../../../../backend/src/main/java/edu/aiplatform/subjects/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks |
|---|---|---|---|---|---|
| [subjects](../../../../frontend/src/features/subjects) | [api](../../../../frontend/src/features/subjects/api) | [model](../../../../frontend/src/features/subjects/model) | [pages](../../../../frontend/src/features/subjects/pages) | [components](../../../../frontend/src/features/subjects/components) | [hooks](../../../../frontend/src/features/subjects/hooks) |

Tests: [subjects](../../../../backend/src/test/java/edu/aiplatform/subjects).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/2026-10-25-b2-b8-u04-results.md](../../build-and-test/2026-10-25-b2-b8-u04-results.md)
