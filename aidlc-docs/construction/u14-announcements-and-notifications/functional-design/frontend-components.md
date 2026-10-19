# U14 Announcements & Notifications — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U14 Announcements & Notifications thuộc Iter 3, người code: Người 3; UC 63–65. Lấy thông báo lớp của U05 cũ và thông báo, email, nhắc hạn của U16 cũ. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 31 | Class Announcements: giảng viên (`t08`), popup Sửa thông báo (`t08b`), Xóa thông báo (`t08c`); sinh viên (`s06`) | `ClassAnnouncementsPage`, `AnnouncementEditorDialog`, `DeleteAnnouncementDialog` | 63, 64 | Student, Teacher, Subject Manager của lớp |
| 36 | Student Class Detail, phần Thông báo lớp (`s02`) | `ClassAnnouncementsCard` | 63 | Student |
| – | Popup Thông báo trên thanh trên cùng (`c14`) | `NotificationBell`, `NotificationPopup`, `EmailPreferencePanel` | 65 | Mọi vai trò |

U14 đăng ký `NavEntry` "Thông báo lớp" (Student, Teacher; Subject Manager trong nhóm "Giảng dạy" cho lớp được giao dạy) và `HeaderWidget` (chuông thông báo).

## 2. Cây component

```
app/announcements/                 ClassAnnouncementsPage   #31 Class Announcements
  ClassFilter                                               nhãn chọn lớp (giảng viên) hoặc lọc lớp (sinh viên)
  AnnouncementComposer                                      khung Thông báo mới của giảng viên
  AnnouncementFeed
  AnnouncementEditorDialog                                  sửa (giảng viên)
  DeleteAnnouncementDialog
(StudentClassSection của U05)  ClassAnnouncementsCard
(HeaderWidget của U01)
  NotificationBell                                          số chưa đọc, kết nối SSE
    NotificationPopup                                       danh sách, đánh dấu đã đọc, tất cả đã đọc
      EmailPreferencePanel                                  chỉ hiện với Student
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `ClassAnnouncementsPage`, `ClassFilter`, `AnnouncementFeed` | `GET /api/v1/me/announcements` | "Thông báo lớp". Giảng viên: "Đăng, sửa và xóa thông báo cho từng lớp bạn dạy", nhãn chọn lớp mình dạy. Sinh viên: "Thông báo của giảng viên ở các lớp bạn đang học", nhãn lọc Tất cả lớp hoặc từng lớp; mở từ một lớp thì lọc sẵn lớp đó. Danh sách mới nhất trước: avatar, tác giả, thời điểm, lớp, nhãn Quan trọng, tiêu đề, nội dung markdown đã làm sạch; giảng viên có nút Sửa, Xóa |
| `AnnouncementComposer` | `POST /api/v1/classes/{classId}/announcements` | "Thông báo mới · lớp": Tiêu đề (tối đa 200 ký tự), Nội dung (Markdown, tối đa 5.000 ký tự) "gửi tới n sinh viên"; ô Thông báo quan trọng (gắn nhãn Quan trọng, không ghim); ô Gửi email cho sinh viên (chỉ chọn được khi đánh dấu quan trọng; email gửi theo tùy chọn của người nhận, BR-U14-03); "Hai lựa chọn này chỉ có khi tạo, không đổi được khi sửa."; nút Đăng thông báo |
| `AnnouncementEditorDialog` | `PATCH /api/v1/announcements/{id}` | "Sửa thông báo": Tiêu đề*, Nội dung (Markdown)*; "Không đổi được lựa chọn Quan trọng / Gửi email sau khi đăng. Sinh viên không nhận thông báo mới khi bạn sửa."; Hủy, Lưu thay đổi; `409` báo tải lại |
| `DeleteAnnouncementDialog` | `DELETE /api/v1/announcements/{id}` | "Xóa thông báo?": "“X” sẽ bị xóa khỏi lớp …"; Hủy, Xóa (xóa mềm phía hệ thống) |
| `ClassAnnouncementsCard` | `GET /api/v1/me/announcements?classId=` | "Thông báo lớp" trên Student Class Detail: vài thông báo mới nhất (tiêu đề, nhãn Quan trọng, tác giả · thời điểm); link Xem tất cả mở Class Announcements lọc lớp |
| `NotificationBell` | `GET /api/v1/me/notifications/unread-count`, `.../events` | Chuông trên thanh trên cùng, chấm đỏ kèm số chưa đọc; SSE; mất kết nối thì tải lại số chưa đọc |
| `NotificationPopup` | `GET /api/v1/me/notifications`, `POST .../{id}:read`, `POST .../notifications:read-all` | "Thông báo", nút Đánh dấu tất cả đã đọc; tab Tất cả, Chưa đọc (kèm số); mỗi thông báo: tiêu đề, lớp hoặc nhóm · thời điểm tương đối, chấm chưa đọc; bấm thì đánh dấu đã đọc rồi mở link; tải thêm khi cuộn (không có trang thông báo riêng) |
| `EmailPreferencePanel` | `GET`, `PUT /api/v1/me/notification-preferences` | Chỉ Student, cuối popup: "Gửi email cho tôi khi có" với công tắc từng loại: Ghi danh vào lớp, Bài tập mới mở, Nhắc hạn nộp, Công bố điểm, Thông báo quan trọng của lớp (BR-U14-20, 21); tắt thì vẫn có thông báo trong app |

Route `/announcements` cho Student, Teacher, Subject Manager (dùng `RoleGuard` của U01); chuông hiện cho mọi vai trò. Backend vẫn kiểm quyền thật.
