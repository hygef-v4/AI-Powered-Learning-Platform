# U14 Announcements & Notifications — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

announcements/notifications có CRUD, recipients, preference, in-app/SSE/email/reminder/retention. OTP gửi qua U01; U14 xử lý email học tập/cảnh báo. Remediation 23–26/10 giữ historical evidence; SENT chỉ là SMTP chấp nhận, không bảo đảm exactly-once hoặc inbox. Scope/privacy/version vẫn do backend kiểm.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [announcements/controller](../../../../backend/src/main/java/edu/aiplatform/announcements/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [announcements/service](../../../../backend/src/main/java/edu/aiplatform/announcements/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [announcements/repository](../../../../backend/src/main/java/edu/aiplatform/announcements/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [announcements/model](../../../../backend/src/main/java/edu/aiplatform/announcements/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [announcements/dto](../../../../backend/src/main/java/edu/aiplatform/announcements/dto) | Kiểu dữ liệu request/response riêng. |
| [notifications/controller](../../../../backend/src/main/java/edu/aiplatform/notifications/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [notifications/service](../../../../backend/src/main/java/edu/aiplatform/notifications/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [notifications/repository](../../../../backend/src/main/java/edu/aiplatform/notifications/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [notifications/model](../../../../backend/src/main/java/edu/aiplatform/notifications/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [notifications/integration](../../../../backend/src/main/java/edu/aiplatform/notifications/integration) | Adapter hạ tầng/provider, listener và sender. |
| [notifications/config](../../../../backend/src/main/java/edu/aiplatform/notifications/config) | Properties, cấu hình bean và topology. |

| Feature | API | Model | Pages | Components | Hooks |
|---|---|---|---|---|---|
| [announcements](../../../../frontend/src/features/announcements) | [api](../../../../frontend/src/features/announcements/api) | [model](../../../../frontend/src/features/announcements/model) | [pages](../../../../frontend/src/features/announcements/pages) | [components](../../../../frontend/src/features/announcements/components) | — |
| [notifications](../../../../frontend/src/features/notifications) | [api](../../../../frontend/src/features/notifications/api) | [model](../../../../frontend/src/features/notifications/model) | — | [components](../../../../frontend/src/features/notifications/components) | — |

Tests: [announcements](../../../../backend/src/test/java/edu/aiplatform/announcements), [notifications](../../../../backend/src/test/java/edu/aiplatform/notifications).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/u14-build-and-test-plan.md](../../build-and-test/u14-build-and-test-plan.md)
- [aidlc-docs/construction/build-and-test/u14-verification.md](../../build-and-test/u14-verification.md)
