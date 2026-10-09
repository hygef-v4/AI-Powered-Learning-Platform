# U03 File, Job & Event - Frontend Components

**Bản tài liệu 2026-10-09**: UC 70, 71 (Settings, người dùng chốt U03 giữ ngày 2026-10-09) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-SET-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

U03 có 2 màn Settings (UC 70–71) và các component dùng chung cho các unit.

## Màn Settings (UC 70–71)

```
app/admin/settings/          SettingListPage      màn Setting List, mở từ Admin Dashboard
  SettingGroupTable                               bảng theo nhóm Tệp, Credit, AI
app/admin/settings/[key]/    SettingDetailPage    màn Setting Detail
  SettingValueForm                                ô nhập theo kiểu của mục, kèm version
```

| Component | API | Hành vi |
|---|---|---|
| `SettingListPage` | `GET /api/v1/admin/settings` | Mỗi nhóm một bảng: tên mục, giá trị hiện hành, người sửa, thời điểm sửa; bấm một dòng mở Setting Detail (UC 70) |
| `SettingDetailPage` | `GET /api/v1/admin/settings/{key}` | Hiện mô tả, giá trị hiện hành, giới hạn hợp lệ, lần sửa gần nhất |
| `SettingValueForm` | `PATCH /api/v1/admin/settings/{key}` | Ô nhập theo kiểu (số, bật/tắt, chọn một, chọn nhiều); kiểm giới hạn phía client; `400` hiện lý do; `409` báo dữ liệu đã đổi và tải lại (UC 71) |

Route nằm dưới `/admin` nên `RoleGuard` của U01 chỉ cho `ADMIN`; API vẫn tự kiểm quyền.

## Component dùng chung

```
shared/files/
  FileUploader        (props: purpose, accept, maxBytes theo purpose, onUploaded)
  FileLink            (props: getDownloadUrl, fileName)
  useFileUpload       (hook: tiến trình, hủy, lỗi)
shared/status/
  StatusBadge
  usePollStatus       (hook poll trạng thái của dòng nghiệp vụ)
```

| Component | Hành vi |
|---|---|
| `FileUploader` | Chọn hoặc kéo thả file; kiểm sơ bộ đuôi và dung lượng theo giới hạn hiện hành lấy từ `GET /api/v1/files/policies`; hiện thanh tiến trình; nút Hủy dừng request; lỗi từ backend hiện nguyên thông điệp an toàn |
| `useFileUpload` | Gửi `POST /api/v1/files` multipart với `purpose`; trả `fileRef` khi thành công; hủy giữa chừng thì backend không giữ lại gì |
| `FileLink` | Khi bấm mới gọi API của unit sở hữu để lấy URL tải (token 5 phút), rồi mở URL; không lưu URL lâu |

Unit dùng: U05 (`MATERIAL`), U06/U09/U11/U14 (`DOCUMENT_IMAGE` qua trình soạn tài liệu).

## Trạng thái việc nền dùng chung

| Component | Props | Hành vi |
|---|---|---|
| `usePollStatus(url)` | `url` API của unit sở hữu, `intervalMs = 3000` | Poll; dừng khi trạng thái là trạng thái cuối do unit khai báo; dừng khi rời trang |
| `StatusBadge` | `status`, `labels` | Nhãn tiếng Việt do unit truyền vào (ví dụ Đang quét, Đã lập chỉ mục, Lỗi) |

Unit khác (U05, U13, U14...) dùng hai component này trên màn hình của mình.

Không có màn quản lý hay chạy lại việc nền (BR-U03-57).
