# U14 Announcements & Notifications — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U14 Announcements & Notifications thuộc Iter 3, người code: Người 3; UC 63–65. Lấy thông báo lớp của U05 cũ và thông báo, email, nhắc hạn của U16 cũ. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Thông báo lớp (UC 63, 64)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U14-01 | Giảng viên chính của lớp (R3/R4) tạo, sửa, xóa thông báo của lớp `OPEN`; người học đang ghi danh và giảng viên của lớp đọc. Người học không đăng; Admin không vào lớp. | FR-023, UC 63, 64 |
| BR-U14-02 | Thông báo có tiêu đề văn bản thuần (≤ 200) và nội dung Markdown (≤ 5.000 ký tự đầu vào). Tạo/sửa loại thẻ HTML, thuộc tính sự kiện và nội dung script/style bằng parser trước lưu, giữ Markdown hợp lệ; từ chối nếu rỗng sau làm sạch. Hiển thị tiếp tục chặn HTML thô, ảnh và URL nguy hiểm. Không tệp đính kèm, không bình luận. Phương án A được duyệt ngày 2026-10-23. | FR-023, SEC-003 |
| BR-U14-03 | Khi tạo, giảng viên có thể đánh dấu quan trọng và chọn gửi email. Tạo mới thì phát `class.announcement-posted` sau commit: người học đang ghi danh nhận thông báo trong app; thông báo quan trọng có chọn email thì gửi email theo tùy chọn của người nhận. | UC 64, FR-011 |
| BR-U14-04 | Sửa kiểm `version`, giữ tác giả và thời điểm gốc, ghi người sửa và audit trước, sau; không gửi lại thông báo hay email. | US-CNT-004 S4 |
| BR-U14-05 | Xóa là xóa mềm: loại khỏi danh sách, giữ người xóa, thời điểm, tham chiếu và audit. | US-CNT-004 S5 |
| BR-U14-06 | Class Announcements mở từ Class Dashboard: thông báo của mọi lớp `OPEN` mình học hoặc dạy, mới nhất trước, lọc theo lớp; mở từ một lớp thì lọc sẵn lớp đó. Student Class Detail hiện vài thông báo mới nhất của lớp. | US-CNT-004 S2 |

## 2. Thông báo trong app (UC 65)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U14-10 | Thông báo tạo từ sự kiện sau khi nghiệp vụ đã commit; U14 lỗi không làm rollback nghiệp vụ. Mỗi (sự kiện, người nhận, loại) tạo đúng một thông báo. | US-NTF-001 S1, S2 |
| BR-U14-11 | Loại và người nhận: `ENROLLED` (người được ghi danh); `ASSIGNMENT_OPENED` (người học đang ghi danh của lớp, hoặc của mọi lớp `OPEN` thuộc môn với bài của môn); `QUIZ_PUBLISHED` (như trên); `GRADE_PUBLISHED` (người học vừa có điểm); `DEADLINE_REMINDER` (F7); `ANNOUNCEMENT` (người học của lớp, trừ người đăng); `LEADER_CHANGE_REQUESTED` (giảng viên của lớp); `LEADER_CHANGED`, `LEADER_REQUEST_REJECTED`, `GROUP_MEMBERSHIP_CHANGED`, `GROUP_SUBMITTED` (thành viên liên quan); `BACKUP_FAILED` (mọi Admin đang hoạt động). | FR-011 |
| BR-U14-12 | Nội dung chỉ về chính người nhận; không ghi điểm, bài nộp, secret; chỉ báo "có điểm mới" và đường dẫn. Mở đường dẫn không cấp quyền: màn đích kiểm quyền hiện hành. | US-NTF-001 S3 |
| BR-U14-13 | Chuông và popup trên thanh điều hướng của mọi vai trò: số chưa đọc, danh sách phân trang, đánh dấu đã đọc từng cái hoặc tất cả; cập nhật realtime qua `ISseHub` của U12 (kênh theo tài khoản). Người dùng chỉ xem, sửa thông báo của mình. | UC 65 |
| BR-U14-14 | Giữ thông báo 180 ngày rồi xóa. | Thiết kế |

Refinement approved 2026-10-24: mô tả trong chuông dùng lời dẫn hành động theo loại thông báo và đối tượng nhận hiện hành; không yêu cầu người đã đăng nhập đăng nhập lại. Thông báo cũ còn câu mặc định được chuyển mô tả khi trả feed, giữ nguyên lịch sử, đường dẫn và trạng thái đã đọc. Email dùng mẫu tối thiểu yêu cầu đăng nhập riêng theo BR-U14-23. BR-U14-12 tiếp tục áp dụng; không đưa điểm/nhận xét hoặc payload riêng tư vào mô tả.

## 3. Email

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U14-20 | Loại có email: `ENROLLED`, `ASSIGNMENT_OPENED`, `GRADE_PUBLISHED`, `DEADLINE_REMINDER`, `ANNOUNCEMENT_IMPORTANT` (thông báo lớp quan trọng có chọn email), `BACKUP_FAILED` (Admin). Các loại khác chỉ trong app. | FR-011 |
| BR-U14-21 | Người học bật, tắt email từng loại học tập trong popup thông báo; tắt thì email `SKIPPED`, thông báo trong app vẫn có. Email `BACKUP_FAILED` cho Admin luôn gửi. | US-NTF-001, FR-011 |
| BR-U14-22 | Trần 300 email thông báo mỗi ngày (giờ Việt Nam, đặt bằng biến môi trường); vượt thì `DEFERRED` sang ngày sau theo thứ tự ưu tiên `BACKUP_FAILED` > `DEADLINE_REMINDER` > `ANNOUNCEMENT_IMPORTANT` > `ASSIGNMENT_OPENED` > `GRADE_PUBLISHED` > `ENROLLED`. Email OTP của U01 không tính. | FR-011 |
| BR-U14-23 | Gửi qua job `EMAIL_SEND` (queue `jobs.email` của U03) bằng `IMail` của U01; lỗi thử lại theo U03 tối đa 5 lần rồi `FAILED`; không gửi trùng. `SENT` chỉ nghĩa là nhà cung cấp đã nhận. Nội dung tối thiểu kèm link cần đăng nhập. | US-NTF-001 S2, S4 |
| BR-U14-24 | Email nhắc hạn đã bị hoãn mà tới lúc gửi đã qua hạn thì `SKIPPED`. | Thiết kế |

## 4. Nhắc hạn nộp (FR-019)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U14-30 | Tự nhắc một lần 24 giờ trước `closesAt` của Assignment; Quiz không nhắc; giảng viên không nhắc tay. | FR-019, US-RPT-001 S2 |
| BR-U14-31 | Người nhận: người học đang ghi danh chưa có bài nộp (bài cá nhân) hoặc thành viên của nhóm chưa nộp (bài nhóm), lấy qua `listStudentsWithoutSubmission` của U08; bài của môn tính trên mọi lớp `OPEN` của môn. | US-RPT-001 S2 |
| BR-U14-32 | Bài đã ngưng giao, đã đóng thì không nhắc; đổi hạn thì nhắc theo hạn mới (mỗi hạn một lần); bài mở sau thời điểm nhắc thì không nhắc. | US-RPT-001 S2 |

## 5. Audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U14-40 | Mọi API kiểm quyền phía server; ngoài phạm vi trả `404` và ghi audit `ACCESS_DENIED`. | SEC-002 |
| BR-U14-41 | Audit: `ANNOUNCEMENT_CREATED`, `ANNOUNCEMENT_UPDATED` (trước, sau), `ANNOUNCEMENT_DELETED`. Không audit từng thông báo, từng email. | FR-014 |
| BR-U14-42 | U01, U05 không trả lời khi xử lý sự kiện thì thử lại theo U03; mất sự kiện chỉ mất thông báo, không ảnh hưởng nghiệp vụ. | BR-U03-70 |
