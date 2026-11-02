# U15 Admin Dashboard — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U15 Admin Dashboard thuộc Iter 3, người code: Người 5; UC 66. Lấy phần Admin Dashboard của U16 cũ; thêm trạng thái backup của U03. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 8 | Admin Dashboard (`a01`), popup Lịch sử backup (`a01b`) | `AdminDashboardPage`, `BackupHistoryDialog` | 66 | Admin |

U15 đăng ký `NavEntry` "Tổng quan" (Admin); các màn quản trị khác mở từ menu bên, không có khối lối tắt riêng.

## 2. Cây component

```
app/admin/                        AdminDashboardPage   #8 Admin Dashboard
  CountCard                                            tài khoản, môn, lớp, ghi danh; "không khả dụng" khi lỗi
  BackupSection                                        lần chạy gần nhất, lịch, lỗi 7 ngày qua, ba lần gần nhất
    BackupHistoryDialog                                lịch sử phân trang
  StatusBreakdown                                      tài khoản theo vai trò × trạng thái; môn, lớp theo trạng thái
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `AdminDashboardPage`, `CountCard` | `GET /api/v1/admin/dashboard` | "Tổng quan hệ thống", "Số liệu tính lúc …"; thẻ Tài khoản (tổng, số đang hoạt động), Môn học (tổng; hoạt động · lưu trữ), Lớp học (tổng; đang mở · nháp · lưu trữ), Ghi danh đang hoạt động (tổng; trên n lớp đang mở); tải lại khi mở trang; từng thẻ hiện riêng trạng thái lỗi |
| `BackupSection` | `GET /api/v1/admin/dashboard` | "Sao lưu dữ liệu": "Không tải xuống hay khôi phục từ giao diện; khôi phục làm tay theo runbook."; nút Lịch sử backup, Cài đặt backup (mở Setting List). Lần chạy gần nhất: kết quả, ngày, Bắt đầu, Kết thúc, Dung lượng, Lỗi đã làm sạch. Lịch: bật hay tắt, giờ chạy hằng ngày (giờ Việt Nam), lần tiếp theo (tính từ cài đặt Backup của U03 qua `ISettings`; chưa bật thì ghi rõ). "Bản sao lưu giữ cố định 7 ngày, sau đó tự xóa. Chỉ sao lưu cơ sở dữ liệu." Lỗi 7 ngày qua: số lần và lỗi gần nhất. Bảng ba lần gần nhất (Ngày chạy, Bắt đầu, Kết thúc, Dung lượng, Kết quả, Lỗi) kèm link Xem lịch sử backup. Không có nút tải hay khôi phục; lỗi nguồn không hiện thành công |
| `BackupHistoryDialog` | `GET /api/v1/admin/dashboard/backups` | "Lịch sử backup", "Mỗi lần chạy theo lịch hằng ngày · bản thành công được giữ 7 ngày"; bảng Ngày chạy, Bắt đầu, Kết thúc, Dung lượng, Kết quả (Thành công, Lỗi, "Đã xóa (quá 7 ngày)" khi bản đã dọn), Lỗi; phân trang; Đóng |
| `StatusBreakdown` | `GET /api/v1/admin/dashboard` | "Chi tiết theo trạng thái", "Tính theo yêu cầu · không chứa dữ liệu cá nhân": bảng Tài khoản theo vai trò × trạng thái (Hoạt động, Chờ kích hoạt, Vô hiệu hóa), Môn học theo trạng thái (Hoạt động, Lưu trữ), Lớp học theo trạng thái (Nháp, Đang mở, Lưu trữ) |

Route `/admin` chỉ cho `ADMIN` (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.
