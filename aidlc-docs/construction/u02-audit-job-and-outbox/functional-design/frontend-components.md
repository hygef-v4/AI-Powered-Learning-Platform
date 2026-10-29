# U02 Audit — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U02 Audit thuộc Iter 1, người code: Người 2; phạm vi UC 13 giữ như U02 cũ (chỉ audit; việc nền đã thuộc U03). 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 13. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md). Baseline cũ: snapshot. Folder `u02-audit-job-and-outbox` giữ tên cũ để không phá liên kết, là thiết kế của U02 Audit hiện hành.

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC |
|---|---|---|---|
| 14 | Audit Log (`a13`), popup Chi tiết bản ghi (`a13b`), trạng thái email người thực hiện không khớp (`a13c`) | `AuditLogPage`, `AuditDetailDialog` | 13 |

Mở từ mục "Nhật ký audit" trên menu bên của Admin (U02 đăng ký `NavEntry`) hoặc từ link "Mở Nhật ký audit đã lọc theo tài khoản" trên Account Detail (U01).

## 2. Cây component

```
app/admin/audit/
  AuditLogPage             #14 Audit Log (UC 13)
    AuditFilters
    AuditTable
    AuditDetailDialog      popup Chi tiết bản ghi
```

Component trạng thái việc nền (`usePollStatus`, `StatusBadge`) thuộc U03.

## 3. AuditLogPage

| Component | Nội dung | API |
|---|---|---|
| Đầu trang | "Nhật ký audit"; "Chỉ đọc · mặc định 7 ngày gần nhất · không bản ghi nào sửa hoặc xóa được" | - |
| `AuditFilters` | Người thực hiện (nhập email), Hành động, Loại đối tượng, Mã đối tượng, Kết quả (Thành công, Từ chối, Thất bại), Thời gian (mặc định 7 ngày gần nhất); nút Xóa lọc, Lọc. Mở từ Account Detail thì điền sẵn loại và mã đối tượng từ tham số trên URL | `GET /api/v1/admin/accounts?q=<email>` (U01) rồi chọn tài khoản có email trùng để đổi thành `actorId`; email không khớp tài khoản nào thì hiện lỗi dưới ô "Không tìm thấy người thực hiện với email này." và không tra cứu |
| `AuditTable` | Cột Thời gian, Người thực hiện (email, hoặc "Hệ thống" khi actor rỗng), Hành động, Đối tượng (loại · mã rút gọn), Kết quả (nhãn màu), Lý do; 50 dòng mỗi trang, tối đa 100; "Hiển thị a–b trong N bản ghi"; không có dòng: "Không có bản ghi nào khớp bộ lọc."; bấm dòng mở chi tiết | `GET /api/v1/admin/audit-logs` |
| `AuditDetailDialog` | Mã, thời điểm, người thực hiện, hành động, đối tượng (loại · mã đầy đủ), kết quả, lý do; "Chi tiết (JSON, chỉ đọc)" gồm dữ liệu trước/sau đã che, loại actor (`USER`, `WORKER`, `SYSTEM`), `correlationId`; ghi chú "Bản ghi không sửa hoặc xóa được"; nút Đóng. Không có nút sửa/xóa | - |

Route nằm dưới `/admin` nên `RoleGuard` của U01 chỉ cho `ADMIN` vào; API vẫn tự kiểm quyền.

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
