# U03 Job and Settings — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

## 1. Thành phần

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

## 2. Cấu hình

| Biến | Ý nghĩa |
|---|---|
| `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY`, `GOOGLE_SHARED_DRIVE_ID` | Drive cho tệp đang dùng |
| `GOOGLE_BACKUP_FOLDER_ID` | Thư mục backup riêng |
| `U03_FILEREF_SECRET` | Khóa ký `FileRef` |
| `U03_REQUIRE_DRIVE` | Production bắt buộc có Drive |
| `U03_JOB_HANDLER_TIMEOUT` | Timeout mặc định của handler |
| `U03_SETTINGS_CACHE_TTL` | 30 giây |
| `U03_BACKUP_PUBLIC_KEY` | Public key RSA bọc khóa archive |
| `BACKUP_DB_PASSWORD` | Mật khẩu role `backup_reader` |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
