# U01 Account & Access — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

Tài khoản trường cấp, OTP/kích hoạt/khôi phục, JWT/session, hồ sơ và Admin CRUD/status có controller/service. Quyền current actor/session không thay bằng hard-code UI; xem identity contract và tests. SMTP thực tế vẫn cần cấu hình, không kết luận email đã đến inbox.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [identity/controller](../../../../backend/src/main/java/edu/aiplatform/identity/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [identity/service](../../../../backend/src/main/java/edu/aiplatform/identity/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [identity/repository](../../../../backend/src/main/java/edu/aiplatform/identity/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [identity/model](../../../../backend/src/main/java/edu/aiplatform/identity/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [identity/integration](../../../../backend/src/main/java/edu/aiplatform/identity/integration) | Adapter hạ tầng/provider, listener và sender. |
| [identity/config](../../../../backend/src/main/java/edu/aiplatform/identity/config) | Properties, cấu hình bean và topology. |
| [identity/contract](../../../../backend/src/main/java/edu/aiplatform/identity/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks |
|---|---|---|---|---|---|
| [identity](../../../../frontend/src/features/identity) | [api](../../../../frontend/src/features/identity/api) | [model](../../../../frontend/src/features/identity/model) | [pages](../../../../frontend/src/features/identity/pages) | [components](../../../../frontend/src/features/identity/components) | [hooks](../../../../frontend/src/features/identity/hooks) |

Tests: [identity](../../../../backend/src/test/java/edu/aiplatform/identity).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
