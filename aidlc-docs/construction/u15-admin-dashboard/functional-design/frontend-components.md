# U15 Admin Dashboard — frontend-components

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
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

## Source behavior delta — 2026-10-30

Admin Dashboard có counts, backup metadata/history và GET /activity. Activity gồm biến động ACTIVE accounts hôm nay, số Assignment 7 ngày (individual/group/practice), tối đa 50 security events trong 30 ngày; ngày Asia/Ho_Chi_Minh. Các nguồn lỗi/timeout trả từng Section UNAVAILABLE/data null; no-store, chỉ ADMIN, query lạ bị từ chối. Không lộ archive/secrets hoặc cấp quyền học vụ.

[Source/API/schema/test inventory](../code/as-built-baseline.md). Các rule và test cũ trái delta này là thiết kế trước baseline; không tiếp tục áp điều kiện cũ vào AS-BUILT.
