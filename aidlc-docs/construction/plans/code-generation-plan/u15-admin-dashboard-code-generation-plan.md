# U15 Admin Dashboard — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U15 Admin Dashboard thuộc Iter 3, người code: Người 5; UC 66. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Thiết kế: [business-logic-model](../../u15-admin-dashboard/functional-design/business-logic-model.md), [business-rules](../../u15-admin-dashboard/functional-design/business-rules.md), [domain-entities](../../u15-admin-dashboard/functional-design/domain-entities.md), [frontend-components](../../u15-admin-dashboard/functional-design/frontend-components.md), [NFR](../../u15-admin-dashboard/nfr-requirements/nfr-requirements.md), [infrastructure](../../u15-admin-dashboard/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/admindashboard/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../admindashboard/contract/` | Trống: unit không cung cấp interface mới, chỉ cài interface của unit trước |
| `frontend/src/features/admindashboard/` | Chỉ có `index.ts` trống |

## Checklist

- [ ] **Bước 1 — Service:** `AdminDashboardService` gọi song song các port có timeout, ghép kết quả theo P1, P2 (F1, F2).
- [ ] **Bước 2 — API:** `AdminDashboardController` (`GET /api/v1/admin/dashboard`, `GET .../backups`); vai trò khác trả `403`; `contracts/openapi/admin-dashboard.yaml`.
- [ ] **Bước 3 — Frontend:** `AdminDashboardPage`, `CountCard`, `BackupSection`, `BackupHistoryDialog`, `StatusBreakdown`; đăng ký `NavEntry` mục Tổng quan; đổi `homePath` tạm của Admin (Account List) về `/admin`.
- [ ] **Bước 4 — Unit test (người code viết):** chỉ `ADMIN`; ghép số liệu; một nguồn lỗi hoặc hết giờ thì phần đó `UNAVAILABLE`; U03 lỗi không hiện thành công; response không có tên, email, ID; component frontend. Integration test do tester viết.
- [ ] **Bước 5 — Review:** đối chiếu UC 66, US-RPT-002, BR-U15 với code; người dùng duyệt.

## Integration test cases (tester viết)

- Vai trò khác gọi API: `403`, không có số liệu.
- Tắt một nguồn (ví dụ U05 lỗi): các thẻ khác vẫn hiện.
- Backup lỗi hoặc chưa bật: mục Backup hiện đúng trạng thái.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
