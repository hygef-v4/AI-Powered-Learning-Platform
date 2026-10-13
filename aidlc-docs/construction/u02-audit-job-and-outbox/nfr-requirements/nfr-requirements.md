# U02 Audit — nfr-requirements

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 73. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u02-audit-job-and-outbox` không quyết định chức năng hiện hành.

Việc nền, RabbitMQ và worker chuyển sang U03 (2026-10-04); yêu cầu của chúng nằm ở NFR-U03-40…55.

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-01 | `AuditPort.record` chỉ thêm một câu INSERT vào transaction của unit gọi. | NFR-003, BR-U02-02 |
| NFR-U02-03 | Tra cứu audit p95 ≤ 500 ms với tới 1 triệu bản ghi. Có index theo `occurred_at`, `(actor_id, occurred_at)`, `(object_type, object_id)`, `action`. | NFR-003, BR-U02-08 |

## 2. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-30 | User database của ứng dụng không có quyền `UPDATE`/`DELETE` trên `audit_logs`. | BR-U02-01, SEC-005 |
| NFR-U02-31 | Tra cứu audit chỉ cho `ADMIN`. | BR-U02-07 |

## 3. Khả dụng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-40 | Audit ghi trong PostgreSQL cùng transaction nghiệp vụ nên không phụ thuộc RabbitMQ; ghi audit lỗi thì thao tác rollback. | BR-U02-04 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-50 | Unit test cho mọi `BR-U02-xx`. Integration test với PostgreSQL bằng Testcontainers: audit trùng `id`, rollback vẫn còn audit `DENIED`/`FAILURE`, user `app` không UPDATE/DELETE được `audit_logs`. | NFR-004 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

## Quy tắc tích hợp revision hiện hành

- Audit append-only; chỉ Admin query, không sửa/xóa. Ghi actor/action/object/result/occurredAt/correlationId; payload sanitize.
- Success ghi cùng transaction owner; denied/failure dùng transaction riêng khi nghiệp vụ rollback. Audit id do caller cung cấp, dedup khóa chính.
- Thêm actions AI_CONNECTION_CHANGED, QUOTA_CHANGED, MCP_REVOKED, BACKUP_COMPLETED/FAILED, RESTORE_VERIFIED và activity policy changed; không log secrets/raw prompt/nội dung nhạy cảm.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Id lặp không trùng; rollback denied vẫn ghi; field secrets/prompt bị loại; filter malformed từ chối; không Admin denied.
