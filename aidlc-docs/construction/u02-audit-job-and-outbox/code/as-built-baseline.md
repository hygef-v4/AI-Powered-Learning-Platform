# U02 Audit — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 13; FR-014.

Audit append-only và Admin query có source; AuditQueryService/IAuditQuery/IAuditStats đã phục vụ filter, label và tổng hợp U15. Actor/object labels là projection; không cấp quyền đọc học vụ. Các kết quả truy vấn/audit trước 30/10 là historical evidence.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/audit/application/AuditQueryService.java](../../../../backend/src/main/java/edu/aiplatform/audit/application/AuditQueryService.java)
- [backend/src/main/java/edu/aiplatform/audit/web/AuditController.java](../../../../backend/src/main/java/edu/aiplatform/audit/web/AuditController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/e2e-u02-u06-u12-2026-10-24.md](../../build-and-test/e2e-u02-u06-u12-2026-10-24.md)
