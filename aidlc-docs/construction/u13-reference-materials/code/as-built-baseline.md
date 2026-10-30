# U13 Reference Materials — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

Module cơ bản của môn đã triển khai. Học liệu môn/lớp bắt buộc moduleId cùng môn; dữ liệu cũ backfill Module 1. Manager quản lý module; class viewers đọc theo scope. Quiz nằm tối đa một module, attach khác module chuyển link; phải kiểm quyền với Quiz. Delete chặn active materials; archived materials chuyển sang module khác, không xóa module cuối nếu cần giữ chúng. Version/reorder kiểm server; module không đồng nghĩa Lesson/RAG.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [materials/controller](../../../../backend/src/main/java/edu/aiplatform/materials/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [materials/service](../../../../backend/src/main/java/edu/aiplatform/materials/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [materials/repository](../../../../backend/src/main/java/edu/aiplatform/materials/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [materials/model](../../../../backend/src/main/java/edu/aiplatform/materials/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [materials/integration](../../../../backend/src/main/java/edu/aiplatform/materials/integration) | Adapter hạ tầng/provider, listener và sender. |

| Feature | API | Model | Pages | Components | Hooks |
|---|---|---|---|---|---|
| [materials](../../../../frontend/src/features/materials) | [api](../../../../frontend/src/features/materials/api) | [model](../../../../frontend/src/features/materials/model) | [pages](../../../../frontend/src/features/materials/pages) | [components](../../../../frontend/src/features/materials/components) | [hooks](../../../../frontend/src/features/materials/hooks) |

Tests: [materials](../../../../backend/src/test/java/edu/aiplatform/materials).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/u13-verification.md](../../build-and-test/u13-verification.md)
