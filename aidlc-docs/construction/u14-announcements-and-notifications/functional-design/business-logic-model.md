# U14 Announcements & Notifications — business-logic-model

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

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
1. Người dùng mở Class Announcements từ mục Thông báo lớp trên menu bên (U14 đăng ký `NavEntry`) hoặc từ phần Thông báo trên Student Class Detail (`StudentClassSection`, lọc sẵn lớp); frontend gọi `GET /api/v1/me/announcements` kèm lớp.
2. Lấy các lớp `OPEN` người dùng đang học hoặc đang dạy (`IClassAccess`); trả thông báo chưa xóa của các lớp đó, mới nhất trước, lọc theo lớp (BR-U14-06).
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
2. Xác định người nhận theo BR-U14-11: người học đang ghi danh qua `IClassAccess.listActiveStudents` (bài, Quiz của môn thì qua `listOpenClassesOfSubject`), giảng viên qua `getClassRef`, Admin qua `IAccountLookup`; người nhận đi kèm sự kiện thì dùng luôn.

3. Với mỗi người nhận: chèn thông báo, trùng (sự kiện, người nhận, loại) thì bỏ qua (BR-U14-10).
4. Loại có email: tạo bản ghi email (F6) theo tùy chọn (BR-U14-21).
5. Sau commit đẩy sự kiện qua `ISseHub` tới kênh của từng người nhận.

Refinement B2 được duyệt 2026-10-25: `ASSIGNMENT_OPENED` phân giải thành các nhóm theo lớp `OPEN`, giữ tài khoản sinh viên `ACTIVE` và ghi danh hiện hành. Mỗi nhóm dùng `/my-assignments/{assignmentId}?classId={classId}`; bài của môn giữ ngữ cảnh từng lớp. Xử lý lớp theo classId ổn định và loại người nhận trùng trong cùng sự kiện, giữ sourceEventId gốc và unique key hiện có.

### F5 - Xem, đánh dấu đã đọc (UC 65)
1. `NotificationBell` trên thanh điều hướng (U14 gắn qua slot `HeaderWidget` của U01) gọi `GET /api/v1/me/notifications/unread-count` và mở kênh `GET /api/v1/me/notifications/events`.
2. Mở popup: `GET /api/v1/me/notifications` (phân trang, mới nhất trước).
3. Bấm một thông báo: `POST /api/v1/me/notifications/{id}:read` rồi mở đường dẫn; màn đích kiểm quyền hiện hành (BR-U14-12).
4. "Đánh dấu tất cả đã đọc": `POST /api/v1/me/notifications:read-all`.

## 4. Email và nhắc hạn

### F6 - Gửi email
1. Bản ghi email tạo cùng transaction với thông báo: người nhận tắt loại đó thì `SKIPPED`; đã chạm trần trong ngày thì `DEFERRED`; còn lại `PENDING` và gửi job `EMAIL_SEND` sau commit (BR-U14-22).
2. Worker nhận job, đọc bản ghi; đã `SENT` thì bỏ qua; lấy địa chỉ qua `IAccountLookup.getContact`; dựng nội dung tối thiểu kèm link; gửi qua `IMail` của U01.
3. Thành công thì `SENT`; lỗi tạm thì U03 thử lại; hết lượt thì `onFailed` ghi `FAILED` với lỗi đã làm sạch.

Refinement B2: trước gửi email nhắc hạn, parse UUID từ đường dẫn nội bộ mới `/my-assignments/` hoặc đường dẫn cũ `/assignments/`; link sai cấu trúc thì `SKIPPED` với mã lỗi cố định, không retry như lỗi dependency. Bài vẫn phải `OPEN` và giữ nguyên hạn tương ứng `expiresAt`. Không chuyển đổi các link lịch sử đã lưu/gửi trong bản vá này.

### F7 - Nhắc hạn nộp
1. `IScheduledScanner` của U14 chạy mỗi phút: lấy bài có `closesAt` trong 24 giờ tới qua `IAssignmentQuery.listClosingBetween` của U07.
2. Với mỗi (bài, lớp, `closesAt`) chưa xử lý: bỏ qua nếu bài không còn `OPEN` hoặc mở sau thời điểm nhắc (BR-U14-32).
3. Lớp của bài cá nhân hoặc nhóm: lớp của bài; bài của môn: mọi lớp `OPEN` thuộc môn (`listOpenClassesOfSubject`).
4. Lấy người chưa nộp qua `ISubmissionQuery.listStudentsWithoutSubmission(assignmentId, classId)` của U08 (bài nhóm: U08 dựa vào trạng thái nhóm từ U12).
5. Tạo thông báo `DEADLINE_REMINDER` và email như F4 bước 3–5; đánh dấu (bài, lớp, `closesAt`) đã nhắc.

Refinement B8 được duyệt 2026-10-25: mỗi lớp xử lý trong transaction riêng có timeout 15 giây. Bắt lỗi bên ngoài transaction để marker, notification và email cùng rollback; tiếp tục lớp sau, đồng thời cô lập lỗi đọc bài/liệt kê lớp để tiếp tục bài sau. Log scanner, assignmentId/classId, scanTime và loại lỗi, không log thông tin người nhận hoặc exception message. Giữ nguyên cửa sổ nhắc 5 phút và khóa chống trùng; không bảo đảm phục hồi sau outage vượt cửa sổ.

### F8 - Tùy chọn email (UC 65)
1. Người học mở phần Cài đặt email trong popup: `GET /api/v1/me/notification-preferences`.
2. Bật, tắt từng loại học tập: `PUT /api/v1/me/notification-preferences`; áp cho email tạo sau đó (BR-U14-21).
3. Giảng viên, Admin không thấy phần này vì không có loại email học tập gửi cho họ.

### F9 - Dọn dữ liệu, gửi email đã hoãn
1. Scanner mỗi phút: sang ngày mới (giờ Việt Nam) thì chuyển email `DEFERRED` sang `PENDING` theo thứ tự ưu tiên tới trần ngày; nhắc hạn đã qua hạn thì `SKIPPED` (BR-U14-24).
2. Xóa theo lô thông báo cũ hơn 180 ngày (BR-U14-14).
