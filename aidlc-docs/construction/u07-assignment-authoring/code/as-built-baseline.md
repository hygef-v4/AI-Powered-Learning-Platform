# U07 Assignment Authoring — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

assignments sở hữu document/rubric/authoring/AI draft/publication/copy. UI editor và service copy hiện có; không phân loại sample/general rubric/flat-page hoàn thành chỉ từ nhãn feat(ui). Nguồn AI qua U06, kết quả chỉ proposal/draft; version và scoped destination vẫn phải kiểm server. Hợp nhất delta AI authoring 24/10 và summary có ngày vào truy vết.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [assignments/controller](../../../../backend/src/main/java/edu/aiplatform/assignments/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [assignments/service](../../../../backend/src/main/java/edu/aiplatform/assignments/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [assignments/repository](../../../../backend/src/main/java/edu/aiplatform/assignments/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [assignments/model](../../../../backend/src/main/java/edu/aiplatform/assignments/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [assignments/dto](../../../../backend/src/main/java/edu/aiplatform/assignments/dto) | Kiểu dữ liệu request/response riêng. |
| [assignments/contract](../../../../backend/src/main/java/edu/aiplatform/assignments/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks |
|---|---|---|---|---|---|
| [assignments](../../../../frontend/src/features/assignments) | [api](../../../../frontend/src/features/assignments/api) | [model](../../../../frontend/src/features/assignments/model) | [pages](../../../../frontend/src/features/assignments/pages) | [components](../../../../frontend/src/features/assignments/components) | [hooks](../../../../frontend/src/features/assignments/hooks) |

Tests: [assignments](../../../../backend/src/test/java/edu/aiplatform/assignments).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
