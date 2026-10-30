# U02 Audit — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

Audit append-only và Admin query có source; AuditQueryService/IAuditQuery/IAuditStats đã phục vụ filter, label và tổng hợp U15. Actor/object labels là projection; không cấp quyền đọc học vụ. Các kết quả truy vấn/audit trước 30/10 là historical evidence.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [audit/controller](../../../../backend/src/main/java/edu/aiplatform/audit/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [audit/service](../../../../backend/src/main/java/edu/aiplatform/audit/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [audit/repository](../../../../backend/src/main/java/edu/aiplatform/audit/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [audit/model](../../../../backend/src/main/java/edu/aiplatform/audit/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [audit/contract](../../../../backend/src/main/java/edu/aiplatform/audit/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks |
|---|---|---|---|---|---|
| [audit](../../../../frontend/src/features/audit) | [api](../../../../frontend/src/features/audit/api) | [model](../../../../frontend/src/features/audit/model) | [pages](../../../../frontend/src/features/audit/pages) | [components](../../../../frontend/src/features/audit/components) | — |

Tests: [audit](../../../../backend/src/test/java/edu/aiplatform/audit).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/e2e-u02-u06-u12-2026-10-24.md](../../build-and-test/e2e-u02-u06-u12-2026-10-24.md)
