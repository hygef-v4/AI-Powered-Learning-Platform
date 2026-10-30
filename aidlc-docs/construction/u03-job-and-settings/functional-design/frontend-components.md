# U03 Job and Settings — frontend-components

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Code frontend U03 (Settings, tệp, khung trang, `shared/ui`) đã làm theo bản này.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

U03 có 2 màn Settings (UC 14, 15) và các component dùng chung. Ẩn hiện UI chỉ hỗ trợ trải nghiệm; quyền thật do backend quyết định.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Mở từ |
|---|---|---|---|---|
| 9 | Setting List (`a15`) | `SettingListPage` | 14 | Mục "Cài đặt" trên menu bên của Admin (U03 đăng ký `NavEntry`); nút "Cài đặt backup" trên Admin Dashboard |
| 10 | Setting Detail, mục khóa–giá trị (`a14b`) | `SettingDetailPage` | 14, 15 | Bấm một dòng trên Setting List |
| 10 | Setting Detail, phần do unit khác lưu (`a14`, `a14c`: API AI của trường và hạn mức, U06 vẽ) | `SettingsSectionHost` | 14, 15 | Bấm thẻ phần ủy quyền trên Setting List |

U03 còn giữ bộ giao diện dùng chung (mục 4), khung trang `AppShell` (cùng U01, xem frontend-components của U01) và component tệp, trạng thái cho các unit khác.

## 2. Cây component

```
app/admin/layout.tsx                               khung AppShell cho mọi màn /admin/*
app/admin/settings/            SettingListPage     #9 Setting List
  DelegatedSectionCard                              thẻ phần ủy quyền (API AI của trường, hạn mức), hai cột, trên cùng
  SettingGroupTable                                 mỗi nhóm một thẻ: Tệp, Backup, Hoạt động làm bài
app/admin/settings/[key]/      SettingDetailPage   #10 Setting Detail (mục khóa–giá trị)
  SettingValueForm                                  ô nhập theo kiểu, kèm version
app/admin/settings/sections/[sectionKey]/
  SettingsSectionHost                               hiện panel do unit sở hữu đăng ký; panel tự vẽ đầu trang
features/settings/slots.ts     settingsSectionRegistry  U06 đăng ký panel API AI của trường, hạn mức (kèm icon, màu thẻ)
features/files/                FileUploader, useFileUpload, FileLink, purposes
shared/status/                 StatusBadge, usePollStatus
shared/ui/                     bộ giao diện dùng chung theo design (mục 4)
shared/layout/                 AppShell, SignOutDialog, menu bên (U01 nối phiên thật)
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `SettingListPage` | `GET /api/v1/admin/settings` | Đầu trang "Cài đặt", "Bấm một dòng để xem và sửa · mỗi lần lưu được ghi vào Nhật ký audit". Thẻ phần ủy quyền: icon, tên, tóm tắt đã che do unit sở hữu trả, mũi tên mở trang; unit không trả lời thì nhãn "Không khả dụng, thử lại sau" và không mở được (UC 14) |
| `SettingGroupTable` | - | Mỗi nhóm một thẻ có icon, tên, mô tả: Tệp ("Giới hạn khi tải học liệu và ảnh trong tài liệu"), Backup ("Sao lưu cơ sở dữ liệu lên Google Drive · giữ 7 ngày (cố định)"), Hoạt động làm bài ("Dữ liệu rời trang, ẩn tab khi bài bật Ghi nhận hoạt động"). Cột Mục, Giá trị hiện hành ("Chưa cấu hình" nếu rỗng; bật/tắt là nhãn màu; số kèm đơn vị, ví dụ "50 MB"), Người sửa (họ tên), Thời điểm sửa (dd/MM HH:mm giờ Việt Nam); bấm dòng mở Setting Detail |
| `SettingDetailPage` | `GET /api/v1/admin/settings/{key}` | Nút quay lại; tên mục; đường dẫn "Cài đặt › nhóm › mục". Thẻ "Giá trị": kiểu (số nguyên, bật / tắt, giờ:phút, chọn một, chọn nhiều), mô tả, Giá trị hiện hành, Giới hạn hợp lệ, Sửa gần nhất (người · thời điểm, hoặc "Chưa sửa, đang dùng giá trị khởi tạo") |
| `SettingValueForm` | `PATCH /api/v1/admin/settings/{key}` | Ô "Giá trị mới" theo kiểu: số kèm đơn vị, công tắc, giờ:phút, chọn một, chọn nhiều bằng nhãn bật/tắt. Kiểm ngay khi sửa, lỗi hiện dưới ô; nút Lưu khóa khi giá trị sai hoặc chưa đổi; Hủy trả về giá trị hiện hành; Bỏ trống với mục không có mặc định. `400` hiện lý do dưới ô; `409` hiện hộp "Người khác vừa sửa" kèm nút Tải lại; lỗi không xóa nội dung đang nhập (UC 15) |
| `SettingsSectionHost` | API của unit sở hữu panel | Panel tự vẽ đầu trang có nút quay lại về Setting List; không có panel đăng ký cho `sectionKey` thì báo "Phần cài đặt này hiện không khả dụng." |
| `FileUploader` | `GET /api/v1/files/policies`, `POST /api/v1/files` | Chưa chọn tệp: ô kéo thả có nút "Chọn tệp". Đang tải: ô tệp (tên, phần trăm), thanh tiến trình, nút Hủy. Xong: ô tệp có dấu xong và dung lượng, nút "Chọn tệp khác". Dòng giới hạn hiện hành (loại, dung lượng) dưới ô; kiểm sơ bộ đuôi và dung lượng; lỗi an toàn từ backend hiện dưới ô |
| `FileLink` | API của unit sở hữu | Khi bấm mới lấy URL tải (token 5 phút) rồi mở; không lưu URL |
| `usePollStatus`, `StatusBadge` | API của unit sở hữu | Poll mỗi 3 giây, dừng ở trạng thái cuối hoặc khi rời trang; nhãn bo tròn theo màu (trung tính, đang chạy, thành công, cảnh báo, lỗi), chữ tiếng Việt do unit truyền vào |

Route dưới `/admin` nên `RoleGuard` của U01 chỉ cho `ADMIN` vào; API vẫn tự kiểm quyền.

Trạng thái backup hiện trên Admin Dashboard do U15 làm (đọc qua `IBackupStatus`); U03 không có màn backup riêng. Không có màn quản lý hay chạy lại việc nền.

Unit dùng component tệp: U13 (`MATERIAL`), U07, U08, U12 (`DOCUMENT_IMAGE` trong trình soạn tài liệu).

## 4. Giao diện dùng chung (`shared/ui`)

U03 giữ bộ giao diện theo design; mọi unit dùng lại, không tự vẽ lại.

| Thành phần | Nội dung |
|---|---|
| Màu, chữ | Font Be Vietnam Pro (tự host khi build); chữ chính `#0F172A`, chữ phụ `#475069`, chữ mờ `#64708A`; viền `#E8EBF2`; nền vùng nội dung `#F7F9FE`; màu chính `#2F45B5`; xanh lá, vàng, đỏ, xanh dương, tím nhạt cho trạng thái |
| `PageHeader` | Nút quay lại, tiêu đề (24px, đậm), đường dẫn hoặc mô tả ngắn, nút thao tác bên phải |
| `Section` | Thẻ nền trắng bo 16px, tiêu đề kèm ô icon màu, mô tả nhỏ |
| `TableFrame`, `Th`, `Td`, `ClickableRow` | Bảng bo 12px, tiêu đề cột chữ hoa nhỏ, dòng bấm được |
| `Button`, `ButtonLink` | Kiểu chính, phụ, nguy hiểm, nhẹ; cỡ thường và nhỏ; có icon |
| `Field`, `TextInput`, `Select`, `Switch`, `ChipToggleGroup` | Trường có nhãn, dấu * khi bắt buộc, gợi ý, lỗi dưới ô; ô số kèm đơn vị; công tắc; chọn nhiều bằng nhãn |
| `Pill`, `IconTile`, `Alert` | Nhãn trạng thái bo tròn; ô icon màu; hộp cảnh báo, lỗi, thông tin kèm nút bên phải |
| `Dialog` | Popup giữa màn hình, đầu popup có ô icon, Esc hoặc bấm ra ngoài để đóng, chân popup chứa nút |
| `Icon` | Icon Material Symbols dạng SVG, không tải font ngoài |
