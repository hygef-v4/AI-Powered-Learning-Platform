# U08 Assignment Work — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

assignmentwork có start/save/submit/history, late/auto-submit, practice feedback và activity. ActivityService.RETENTION = 1 ngày; scanner xóa theo hằng này, không Settings activity.retentionDays 1–365. Theo dõi hoạt động không tự trừ điểm. Feedback Practice gắn submission/version và không vào gradebook.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [assignmentwork/controller](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [assignmentwork/service](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [assignmentwork/repository](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [assignmentwork/model](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [assignmentwork/dto](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/dto) | Kiểu dữ liệu request/response riêng. |
| [assignmentwork/contract](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [assignmentwork](../../../../frontend/src/features/assignmentwork) | [api](../../../../frontend/src/features/assignmentwork/api) | [model](../../../../frontend/src/features/assignmentwork/model) | [pages](../../../../frontend/src/features/assignmentwork/pages) | [components](../../../../frontend/src/features/assignmentwork/components) | [hooks](../../../../frontend/src/features/assignmentwork/hooks) | — | [tests](../../../../frontend/tests/features/assignmentwork) |

Tests: [assignmentwork](../../../../backend/src/test/java/edu/aiplatform/assignmentwork).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
