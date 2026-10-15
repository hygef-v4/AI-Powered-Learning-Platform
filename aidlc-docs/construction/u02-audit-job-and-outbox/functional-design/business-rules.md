# U02 Audit — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U02 Audit thuộc Iter 1, người code: Người 2; phạm vi UC 13 giữ như U02 cũ (chỉ audit; việc nền đã thuộc U03). 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 13. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: snapshot. Folder `u02-audit-job-and-outbox` giữ tên cũ để không phá liên kết, là thiết kế của U02 Audit hiện hành.

Việc nền, worker và sự kiện thông báo đã chuyển sang U03 (quyết định 2026-10-04); U02 chỉ còn audit.

## 1. Audit

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U02-01 | Audit chỉ được thêm, không có API hay thao tác ứng dụng nào sửa hoặc xóa. | US-AUD-001 S2 |
| BR-U02-02 | Mọi unit ghi audit bằng một hàm `AuditPort.record`; U02 chọn transaction theo `result`. `SUCCESS` ghi thẳng vào `audit_logs` **trong transaction của unit gọi**: thao tác nghiệp vụ commit thì audit có, rollback thì audit không có. `DENIED` và `FAILURE` được ghi trong transaction riêng để không mất khi nghiệp vụ rollback. Không đi qua RabbitMQ. | Quyết định 2026-09-26 |
| BR-U02-03 | `id` do unit gọi tạo là khóa chính; ghi lại cùng `id` không tạo bản ghi thứ hai. | Thiết kế |
| BR-U02-04 | Ghi audit lỗi (ví dụ DB lỗi) thì thao tác nghiệp vụ cùng transaction cũng rollback; audit bắt buộc không bị mất âm thầm. | Quyết định 2026-09-26 |
| BR-U02-05 | `details` không chứa mật khẩu, OTP, token, số điện thoại; unit gọi chịu trách nhiệm che, U02 từ chối lưu nếu phát hiện khóa thuộc danh sách cấm (`password`, `otp`, `token`, `secret`, `phone`); danh sách và `ForbiddenKeyGuard` nằm ở `shared/` (khung dự án) để U03 dùng chung cho payload việc nền. | SEC-005 |
| BR-U02-06 | Audit giữ vĩnh viễn. | Câu 6 |
| BR-U02-07 | Chỉ `ADMIN` được tra cứu audit; mọi lần tra cứu cũng được audit. | Câu 7, UC 13 |
| BR-U02-08 | Tra cứu lọc theo người thực hiện, action, đối tượng, result, khoảng thời gian; sắp xếp mới nhất trước; mỗi trang tối đa 100 bản ghi. Lọc người thực hiện theo `actorId`; màn Audit Log cho nhập email rồi đổi sang `accountId` bằng API danh sách tài khoản của U01. | US-AUD-001 S1, UC 13 |
| BR-U02-09 | Kết quả tra cứu hiện email người thực hiện, lấy qua `AccountLookupPort` của U01 lúc đọc (không lưu email vào `audit_logs`); `actor_id` rỗng là hệ thống hoặc worker. | UC 13, contract `audit.yaml` (`actorEmail`) |

## 2. Lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U02-50 | Không kiểm được quyền khi đọc audit thì từ chối. | SEC-002 |
| BR-U02-51 | Lỗi trả client an toàn, không lộ chi tiết queue hay database. | SEC-006 |

## Quy tắc tích hợp revision hiện hành

- Audit append-only; chỉ Admin query, không sửa/xóa. Ghi actor/action/object/result/occurredAt/correlationId; payload sanitize.
- Success ghi cùng transaction owner; denied/failure dùng transaction riêng khi nghiệp vụ rollback. Audit id do caller cung cấp, dedup khóa chính.
- Thêm actions AI_CONNECTION_CHANGED, QUOTA_CHANGED, MCP_REVOKED, BACKUP_COMPLETED/FAILED, RESTORE_VERIFIED và activity policy changed; không log secrets/raw prompt/nội dung nhạy cảm.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. Decided 2026-10-15: D01 Quiz Practice only, D02 Group Graded only, D03 dropped (no student AI for group work), D04 daily quota plus global cap. Backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Id lặp không trùng; rollback denied vẫn ghi; field secrets/prompt bị loại; filter malformed từ chối; không Admin denied.
