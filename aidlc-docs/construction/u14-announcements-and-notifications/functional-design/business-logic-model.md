# U14 Announcements & Notifications — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U14 Announcements & Notifications thuộc Iter 3, người code: Người 3; UC 63–65. Lấy thông báo lớp của U05 cũ và thông báo, email, nhắc hạn của U16 cũ. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Xem thông báo lớp | UC 63 | Class Announcements; phần Thông báo trên Student Class Detail |
| F2 Tạo thông báo lớp | UC 64 | Class Announcements |
| F3 Sửa, xóa thông báo lớp | UC 64 | Class Announcements |
| F4 Tạo thông báo từ sự kiện | - | - |
| F5 Xem, đánh dấu đã đọc | UC 65 | Chuông và popup thông báo |
| F6 Gửi email | - | - |
| F7 Nhắc hạn nộp | - | - |
| F8 Tùy chọn email | UC 65 | Popup thông báo |
| F9 Dọn dữ liệu, gửi email đã hoãn | - | - |

## 2. Thông báo lớp

### F1 - Xem thông báo lớp (UC 63)
1. Người dùng mở Class Announcements từ thanh bên Class Dashboard (U14 đăng ký `DashboardNavEntry`) hoặc từ phần Thông báo trên Student Class Detail (`StudentClassSection`, lọc sẵn lớp); frontend gọi `GET /api/v1/me/announcements` kèm lớp.
2. Lấy các lớp `OPEN` người dùng đang học hoặc đang dạy (`ClassAccessPort`); trả thông báo chưa xóa của các lớp đó, mới nhất trước, lọc theo lớp (BR-U14-06).
3. Thông báo của lớp ngoài quyền trả "không tìm thấy".

### F2 - Tạo thông báo lớp (UC 64)
1. Giảng viên bấm Tạo trên Class Announcements, chọn lớp mình dạy, nhập tiêu đề, nội dung, đánh dấu quan trọng và chọn gửi email nếu muốn; frontend gọi `POST /api/v1/classes/{classId}/announcements`.
2. Kiểm giảng viên chính của lớp, lớp `OPEN`, giới hạn nội dung (BR-U14-01, 02).
3. Lưu thông báo; audit `ANNOUNCEMENT_CREATED`; sau commit phát `class.announcement-posted` (lớp, thông báo, người đăng, cờ quan trọng, chọn email).

### F3 - Sửa, xóa thông báo lớp (UC 64)
1. Sửa: `PATCH /api/v1/announcements/{id}` kèm `version`; kiểm giảng viên chính của lớp; lưu người sửa, thời điểm; audit trước, sau; không phát sự kiện (BR-U14-04).
2. Xóa: xác nhận, `DELETE /api/v1/announcements/{id}`; chuyển `DELETED`, ghi người xóa, thời điểm; audit (BR-U14-05).

## 3. Thông báo trong app

### F4 - Tạo thông báo từ sự kiện
1. Listener của U14 nhận sự kiện trên queue `jobs.notification` (bind `platform.events` của U03); bỏ qua phiên bản sự kiện không hỗ trợ.
2. Xác định người nhận theo BR-U14-11: người học đang ghi danh qua `ClassAccessPort.listActiveStudents` (bài, Quiz của môn thì qua `listOpenClassesOfSubject`), giảng viên qua `getClassRef`, Admin qua `AccountLookupPort`; người nhận đi kèm sự kiện thì dùng luôn.
3. Với mỗi người nhận: chèn thông báo, trùng (sự kiện, người nhận, loại) thì bỏ qua (BR-U14-10).
4. Loại có email: tạo bản ghi email (F6) theo tùy chọn (BR-U14-21).
5. Sau commit đẩy sự kiện qua `SseHub` tới kênh của từng người nhận.

### F5 - Xem, đánh dấu đã đọc (UC 65)
1. `NotificationBell` trên thanh điều hướng (U14 gắn qua slot `HeaderWidget` của U01) gọi `GET /api/v1/me/notifications/unread-count` và mở kênh `GET /api/v1/me/notifications/events`.
2. Mở popup: `GET /api/v1/me/notifications` (phân trang, mới nhất trước).
3. Bấm một thông báo: `POST /api/v1/me/notifications/{id}:read` rồi mở đường dẫn; màn đích kiểm quyền hiện hành (BR-U14-12).
4. "Đánh dấu tất cả đã đọc": `POST /api/v1/me/notifications:read-all`.

## 4. Email và nhắc hạn

### F6 - Gửi email
1. Bản ghi email tạo cùng transaction với thông báo: người nhận tắt loại đó thì `SKIPPED`; đã chạm trần trong ngày thì `DEFERRED`; còn lại `PENDING` và gửi job `EMAIL_SEND` sau commit (BR-U14-22).
2. Worker nhận job, đọc bản ghi; đã `SENT` thì bỏ qua; lấy địa chỉ qua `AccountLookupPort.getContact`; dựng nội dung tối thiểu kèm link; gửi qua `MailPort` của U01.
3. Thành công thì `SENT`; lỗi tạm thì U03 thử lại; hết lượt thì `onFailed` ghi `FAILED` với lỗi đã làm sạch.

### F7 - Nhắc hạn nộp
1. `ScheduledScanner` của U14 chạy mỗi phút: lấy bài có `closesAt` trong 24 giờ tới qua `AssignmentQueryPort.listClosingBetween` của U07.
2. Với mỗi (bài, lớp, `closesAt`) chưa xử lý: bỏ qua nếu bài không còn `OPEN` hoặc mở sau thời điểm nhắc (BR-U14-32).
3. Lớp của bài cá nhân hoặc nhóm: lớp của bài; bài của môn: mọi lớp `OPEN` thuộc môn (`listOpenClassesOfSubject`).
4. Lấy người chưa nộp qua `SubmissionQueryPort.listStudentsWithoutSubmission(assignmentId, classId)` của U08 (bài nhóm: U08 dựa vào trạng thái nhóm từ U12).
5. Tạo thông báo `DEADLINE_REMINDER` và email như F4 bước 3–5; đánh dấu (bài, lớp, `closesAt`) đã nhắc.

### F8 - Tùy chọn email (UC 65)
1. Người học mở phần Cài đặt email trong popup: `GET /api/v1/me/notification-preferences`.
2. Bật, tắt từng loại học tập: `PUT /api/v1/me/notification-preferences`; áp cho email tạo sau đó (BR-U14-21).
3. Giảng viên, Admin không thấy phần này vì không có loại email học tập gửi cho họ.

### F9 - Dọn dữ liệu, gửi email đã hoãn
1. Scanner mỗi phút: sang ngày mới (giờ Việt Nam) thì chuyển email `DEFERRED` sang `PENDING` theo thứ tự ưu tiên tới trần ngày; nhắc hạn đã qua hạn thì `SKIPPED` (BR-U14-24).
2. Xóa theo lô thông báo cũ hơn 180 ngày (BR-U14-14).
