# U03 Job and Settings — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

files/jobs/settings/backup có source và Flyway schema. Settings hiện hành lấy từ SettingDefinition trong source, không từ danh sách cũ; activity.retentionDays không còn được khai báo. Drive private upload/download và backup encryption cần runtime secrets; không chứng nhận restore từ việc có script. CI deploy vẫn disabled.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [files/controller](../../../../backend/src/main/java/edu/aiplatform/files/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [files/service](../../../../backend/src/main/java/edu/aiplatform/files/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [files/model](../../../../backend/src/main/java/edu/aiplatform/files/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [files/integration](../../../../backend/src/main/java/edu/aiplatform/files/integration) | Adapter hạ tầng/provider, listener và sender. |
| [files/config](../../../../backend/src/main/java/edu/aiplatform/files/config) | Properties, cấu hình bean và topology. |
| [files/contract](../../../../backend/src/main/java/edu/aiplatform/files/contract) | Public interface/DTO xuyên unit; giữ nguyên. |
| [jobs/service](../../../../backend/src/main/java/edu/aiplatform/jobs/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [jobs/integration](../../../../backend/src/main/java/edu/aiplatform/jobs/integration) | Adapter hạ tầng/provider, listener và sender. |
| [jobs/config](../../../../backend/src/main/java/edu/aiplatform/jobs/config) | Properties, cấu hình bean và topology. |
| [jobs/contract](../../../../backend/src/main/java/edu/aiplatform/jobs/contract) | Public interface/DTO xuyên unit; giữ nguyên. |
| [settings/controller](../../../../backend/src/main/java/edu/aiplatform/settings/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [settings/service](../../../../backend/src/main/java/edu/aiplatform/settings/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [settings/repository](../../../../backend/src/main/java/edu/aiplatform/settings/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [settings/model](../../../../backend/src/main/java/edu/aiplatform/settings/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [settings/contract](../../../../backend/src/main/java/edu/aiplatform/settings/contract) | Public interface/DTO xuyên unit; giữ nguyên. |
| [backup/service](../../../../backend/src/main/java/edu/aiplatform/backup/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [backup/repository](../../../../backend/src/main/java/edu/aiplatform/backup/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [backup/model](../../../../backend/src/main/java/edu/aiplatform/backup/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [backup/integration](../../../../backend/src/main/java/edu/aiplatform/backup/integration) | Adapter hạ tầng/provider, listener và sender. |
| [backup/config](../../../../backend/src/main/java/edu/aiplatform/backup/config) | Properties, cấu hình bean và topology. |
| [backup/contract](../../../../backend/src/main/java/edu/aiplatform/backup/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks |
|---|---|---|---|---|---|
| [files](../../../../frontend/src/features/files) | [api](../../../../frontend/src/features/files/api) | [model](../../../../frontend/src/features/files/model) | — | [components](../../../../frontend/src/features/files/components) | [hooks](../../../../frontend/src/features/files/hooks) |
| [settings](../../../../frontend/src/features/settings) | [api](../../../../frontend/src/features/settings/api) | [model](../../../../frontend/src/features/settings/model) | [pages](../../../../frontend/src/features/settings/pages) | [components](../../../../frontend/src/features/settings/components) | — |

Tests: [files](../../../../backend/src/test/java/edu/aiplatform/files), [jobs](../../../../backend/src/test/java/edu/aiplatform/jobs), [settings](../../../../backend/src/test/java/edu/aiplatform/settings), [backup](../../../../backend/src/test/java/edu/aiplatform/backup).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
