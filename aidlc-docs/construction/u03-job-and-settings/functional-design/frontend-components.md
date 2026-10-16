# U03 Job and Settings — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U03 Job and Settings thuộc Iter 1, người code: Người 3. Viết lại từ thiết kế chi tiết 2026-10-09 theo phạm vi 66 UC: bỏ avatar và việc nền của Code Lab, PayOS, tóm tắt học liệu; thêm backup và các nhóm cài đặt mới. Chỉ sửa tài liệu; unit chưa có code (khung code tạo lại 2026-10-16).

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

U03 có 2 màn Settings (UC 14, 15) và các component dùng chung. Ẩn hiện UI chỉ hỗ trợ trải nghiệm; quyền thật do backend quyết định.

## 1. Màn hình theo screen flow

| Màn | Component | UC | Mở từ |
|---|---|---|---|
| Setting List | `SettingListPage` | 14 | Admin Dashboard |
| Setting Detail | `SettingDetailPage` (mục khóa–giá trị) hoặc panel do unit sở hữu đăng ký (API AI chung, quota) | 14, 15 | Setting List |

## 2. Cây component

```
app/admin/settings/            SettingListPage     màn Setting List
  SettingGroupTable                                 nhóm Tệp, Backup, Hoạt động làm bài
  DelegatedSectionCard                              API AI chung, quota (tóm tắt đã che, hoặc "không khả dụng")
app/admin/settings/[key]/      SettingDetailPage   màn Setting Detail cho mục khóa-giá trị
  SettingValueForm                                  ô nhập theo kiểu, kèm version
app/admin/settings/sections/[sectionKey]/
  SettingsSectionHost                               hiện panel do unit sở hữu đăng ký
shared/settings/
  settingsSectionRegistry                           U06 đăng ký AiConnectionsPanel, TeacherQuotaPanel
shared/files/
  FileUploader, useFileUpload, FileLink, purposes
shared/status/
  StatusBadge, usePollStatus
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `SettingListPage` | `GET /api/v1/admin/settings` | Mỗi nhóm một bảng: tên mục, giá trị hiện hành ("chưa cấu hình" nếu rỗng), người sửa, thời điểm sửa; phần ủy quyền hiện tóm tắt hoặc "không khả dụng" (UC 14) |
| `SettingDetailPage` | `GET /api/v1/admin/settings/{key}` | Mô tả, giá trị hiện hành, giới hạn hợp lệ, lần sửa gần nhất |
| `SettingValueForm` | `PATCH /api/v1/admin/settings/{key}` | Ô nhập theo kiểu (số, bật/tắt, giờ:phút, chọn một, chọn nhiều); kiểm sơ bộ phía client; `400` hiện lý do; `409` báo dữ liệu đã đổi và tải lại; lỗi không xóa nội dung đang nhập (UC 15) |
| `SettingsSectionHost` | API của unit sở hữu panel | Không có panel đăng ký cho `sectionKey` thì báo không khả dụng |
| `FileUploader` | `GET /api/v1/files/policies`, `POST /api/v1/files` | Chọn hoặc kéo thả, kiểm sơ bộ đuôi và dung lượng theo giới hạn hiện hành, thanh tiến trình, nút Hủy, hiện lỗi an toàn từ backend |
| `FileLink` | API của unit sở hữu | Khi bấm mới lấy URL tải (token 5 phút) rồi mở; không lưu URL |
| `usePollStatus`, `StatusBadge` | API của unit sở hữu | Poll mỗi 3 giây, dừng ở trạng thái cuối hoặc khi rời trang; nhãn tiếng Việt do unit truyền vào |

Route dưới `/admin` nên `RoleGuard` của U01 chỉ cho `ADMIN` vào; API vẫn tự kiểm quyền.

Trạng thái backup hiện trên Admin Dashboard do U15 làm (đọc qua `IBackupStatus`); U03 không có màn backup riêng. Không có màn quản lý hay chạy lại việc nền.

Unit dùng component tệp: U13 (`MATERIAL`), U07, U08, U12 (`DOCUMENT_IMAGE` trong trình soạn tài liệu).
