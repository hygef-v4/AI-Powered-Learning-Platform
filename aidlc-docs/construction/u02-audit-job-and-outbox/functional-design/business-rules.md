# U02 Audit - Business Rules

**Bản tài liệu 2026-10-08**: UC 70; primary stories: US-AUD-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Việc nền, worker và sự kiện thông báo đã chuyển sang U03 (quyết định 2026-10-04); U02 chỉ còn audit.

## 1. Audit

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U02-01 | Audit chỉ được thêm, không có API hay thao tác ứng dụng nào sửa hoặc xóa. | US-AUD-001 S2 |
| BR-U02-02 | `AuditPort.record` ghi thẳng vào `audit_logs` **trong transaction của unit gọi**: thao tác nghiệp vụ commit thì audit có, rollback thì audit không có. Sự kiện bị từ chối (`DENIED`) và lỗi (`FAILURE`) được ghi trong transaction riêng để không mất khi nghiệp vụ rollback. Không đi qua RabbitMQ. | Quyết định 2026-09-26 |
| BR-U02-03 | `id` do unit gọi tạo là khóa chính; ghi lại cùng `id` không tạo bản ghi thứ hai. | Thiết kế |
| BR-U02-04 | Ghi audit lỗi (ví dụ DB lỗi) thì thao tác nghiệp vụ cùng transaction cũng rollback; audit bắt buộc không bị mất âm thầm. | Quyết định 2026-09-26 |
| BR-U02-05 | `details` không chứa mật khẩu, OTP, token, số điện thoại; unit gọi chịu trách nhiệm che, U02 từ chối lưu nếu phát hiện khóa thuộc danh sách cấm (`password`, `otp`, `token`, `secret`, `phone`); danh sách và `ForbiddenKeyGuard` nằm ở `shared/` (khung dự án) để U03 dùng chung cho payload việc nền. | SEC-005 |
| BR-U02-06 | Audit giữ vĩnh viễn. | Câu 6 |
| BR-U02-07 | Chỉ `ADMIN` được tra cứu audit; mọi lần tra cứu cũng được audit. | Câu 7, UC 70 |
| BR-U02-08 | Tra cứu lọc theo actor, action, đối tượng, result, khoảng thời gian; sắp xếp mới nhất trước; mỗi trang tối đa 100 bản ghi. | US-AUD-001 S1 |

## 2. Lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U02-50 | Không kiểm được quyền khi đọc audit thì từ chối. | SEC-002 |
| BR-U02-51 | Lỗi trả client an toàn, không lộ chi tiết queue hay database. | SEC-006 |
