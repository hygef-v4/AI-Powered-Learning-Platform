# U12 Group Assignment — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Gộp U12 cũ (nhóm và trưởng nhóm) và U14 cũ (tài liệu nhóm và nộp). Soạn đề, rubric dùng U07; chấm dùng U09. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Quản lý nhóm | UC 57 | Teacher Class Detail, tab Students |
| F2 Chia ngẫu nhiên | UC 57 | Tab Students |
| F3 Duyệt yêu cầu đổi trưởng nhóm | UC 57 | Tab Students |
| F4 Nhóm của tôi | UC 54 | Student Class Detail (My Group) |
| F5 Gửi, hủy yêu cầu đổi trưởng nhóm | UC 55 | Student Class Detail (My Group) |
| F6 Soạn và phát hành bài nhóm | UC 53 | Assignment Form (U07) |
| F7 Tạo tài liệu nhóm | - | - |
| F8 Mở workspace nhóm | UC 56 | Assignment Detail, Assignment Workspace (chế độ nhóm) |
| F9 Giao, nhận mục | UC 56 | Assignment Workspace |
| F10 Làm mục và Xong | UC 56 | Popup làm mục |
| F11 Nhả khóa | UC 56 | Assignment Workspace, tab Students |
| F12 Nộp bài nhóm | UC 56 | Assignment Workspace |
| F13 Tự nộp | - | - |
| F14 Đổi thành viên khi có bài đang mở | UC 57 | Tab Students |
| F15 Port cho unit khác, realtime | UC của U08, U09, U14 | - |

## 2. Nhóm và trưởng nhóm

### F1 - Create/Update/Delete Class Group (UC 57)
1. Giảng viên mở tab Students; phần Nhóm do U12 gắn qua `TeacherClassTab`; frontend gọi `GET /api/v1/classes/{classId}/groups`.
2. Kiểm giảng viên chính của lớp (BR-U12-01); trả nhóm, thành viên (người đã rời lớp có nhãn), trưởng nhóm, sinh viên chưa có nhóm, yêu cầu đang chờ, `version`.
3. Giảng viên tạo nhóm, thêm, chuyển, gỡ thành viên, chọn trưởng nhóm trên bản sửa ở trình duyệt.
4. Lưu: `PUT /api/v1/classes/{classId}/groups` kèm cả bộ nhóm và `version`.
5. Kiểm lớp `DRAFT` hoặc `OPEN`, thành viên ghi danh `ACTIVE` (`ClassAccessPort`), mỗi người một nhóm, mỗi nhóm có trưởng nhóm là thành viên, nhóm bị xóa chưa có tài liệu có nội dung (BR-U12-02, 05, 06); lỗi thì trả từng lỗi, không lưu gì.
6. Một transaction: ghi nhóm; với thay đổi khi có bài nhóm đang mở chạy F14; audit; sau commit phát `group.membership-changed` và `group.leader-changed` (kèm người nhận).

### F2 - Chia ngẫu nhiên (UC 57)
1. Bấm Chia ngẫu nhiên, nhập sĩ số tối đa 2–20; frontend gọi `POST /api/v1/classes/{classId}/groups:random-preview`.
2. Lấy sinh viên đang học chưa có nhóm; chia thành nhóm chênh nhau tối đa một người; chọn ngẫu nhiên trưởng nhóm (BR-U12-03).
3. Trả bản xem trước gộp với nhóm hiện có; giảng viên sửa rồi lưu như F1 bước 4–6.

### F3 - Duyệt yêu cầu đổi trưởng nhóm (UC 57)
1. Tab Students hiện yêu cầu đang chờ; giảng viên bấm Duyệt hoặc Từ chối.
2. Duyệt: chọn trưởng nhóm mới (mặc định người được đề cử); `POST /api/v1/leader-requests/{id}:approve`. Từ chối: nhập lý do; `POST .../{id}:reject`.
3. Kiểm giảng viên chính của lớp, yêu cầu còn chờ, người mới là thành viên hiệu lực (BR-U12-11).
4. Một transaction: cập nhật yêu cầu, đổi trưởng nhóm nếu duyệt (không đổi mục ai đang giữ, BR-U12-13); audit.
5. Sau commit phát `group.leader-changed` hoặc `group.leader-request-rejected` cho U14.

### F4 - Nhóm của tôi (UC 54)
1. Student Class Detail có phần My Group do U12 gắn qua `StudentClassSection`; frontend gọi `GET /api/v1/me/classes/{classId}/group`.
2. Kiểm R5 với lớp; không có nhóm thì báo "chưa có nhóm".
3. Trả theo BR-U12-50: nhóm, thành viên, trưởng nhóm, tài liệu nhóm của các bài nhóm đã mở kèm tiến độ, trạng thái nộp, yêu cầu gần nhất của mình.
4. Bấm tài liệu mở Assignment Detail của bài nhóm (U08).

### F5 - Gửi, hủy yêu cầu đổi trưởng nhóm (UC 55)
1. Trên My Group bấm "Đề nghị đổi trưởng nhóm", nhập lý do, tùy chọn đề cử; frontend gọi `POST /api/v1/groups/{groupId}/leader-requests`.
2. Kiểm người gửi là thành viên hiệu lực, lớp `OPEN`, lý do 10–1.000 ký tự, người được đề cử là thành viên khác trưởng nhóm, nhóm chưa có yêu cầu đang chờ (BR-U12-10).
3. Tạo yêu cầu `PENDING`; audit; sau commit phát `group.leader-change-requested` cho giảng viên của lớp.
4. Hủy khi còn chờ: `POST /api/v1/leader-requests/{id}:cancel` (chỉ người gửi).

## 3. Bài nhóm và tài liệu nhóm

### F6 - Soạn và phát hành bài nhóm (UC 53)
1. Giảng viên tạo bài trên tab Evals (U07), chọn `GROUP` (chỉ hiện khi U12 có), bật hay tắt sơ đồ; chế độ khóa `GRADED`.
2. Soạn đề, khung, rubric trên Assignment Form của U07; phần Nhóm của U12 (slot `AssignmentFormSection`) hiện số nhóm, kết quả kiểm sẵn sàng, lối vào tab Students.
3. Khi duyệt và phát hành, U07 gọi `GroupAssignmentSupport.checkReadiness(classId)`; U12 trả lỗi chặn và cảnh báo theo BR-U12-22.
4. Có cảnh báo `STUDENT_WITHOUT_GROUP` thì giảng viên xác nhận để phát hành.

### F7 - Tạo tài liệu nhóm
1. U07 mở bài nhóm và gọi `onOpened(assignmentId)` trong transaction mở bài; U12 gửi job `GROUP_DOC_CREATE` qua `JobPort` sau commit.
2. Worker lấy khung và phần (`AssignmentQueryPort.getContent`), tạo một tài liệu cho mỗi nhóm hợp lệ của lớp: mục theo `partId`, phần chung chỉ đọc (BR-U12-30); làm lại không tạo trùng.
3. Người học mở trước khi job chạy xong thì tài liệu được tạo ngay khi truy cập.
4. Nhóm mới tạo khi bài đang mở cũng được tạo tài liệu (F1 bước 6).

### F8 - Mở workspace nhóm (UC 56)
1. Trên Assignment Detail bấm "Mở bài nhóm" (U12 gắn qua `GroupWorkEntry`); frontend gọi `GET /api/v1/assignments/{assignmentId}/my-group-doc` kèm `classId`.
2. Kiểm thành viên hiệu lực của một nhóm trong lớp; trả tài liệu (phần chung, mục kèm trạng thái, người giữ, nội dung đã đăng), quyền của mình (thành viên hay trưởng nhóm), `version`.
3. Mở kênh SSE `GET /api/v1/group-docs/{id}/events`; bài bật theo dõi thì `useActivityTracker` gửi sự kiện tới `POST /api/v1/group-docs/{id}/activity` (F15).
4. Workspace hiện toàn bộ tài liệu và danh sách mục với thao tác hợp lệ cho từng người.

### F9 - Giao, nhận mục (UC 56)
1. Thành viên bấm Nhận ở mục `OPEN` hoặc `DONE`: `POST /api/v1/group-docs/{id}/sections/{sectionId}:claim`.
2. Trưởng nhóm bấm Giao, chọn thành viên: `POST .../sections/{sectionId}:assign`; mục đang do người khác giữ thì cảnh báo và nhả trước (F11).
3. Kiểm quyền, tài liệu `IN_PROGRESS`, bài còn nhận bài (BR-U12-32); khóa dòng mục; cập nhật `CLAIMED`, người giữ, người giao; audit với giao mục.
4. Đẩy sự kiện SSE cho mọi người đang mở tài liệu.

### F10 - Làm mục và Xong (UC 56)
1. Người giữ bấm Làm, mở popup làm mục (BR-U12-34) dùng `DocumentEditor` của U07.
2. 10 giây sau lần sửa cuối: `PUT /api/v1/group-docs/{id}/sections/{sectionId}/draft` kèm `version` của mục; kiểm người giữ, còn hạn (cộng 30 giây), `validateForSave`; lệch `version` thì `409`.
3. Bấm Xong: `POST .../sections/{sectionId}:done`; kiểm block; nội dung nháp thành nội dung đã đăng; thêm bản lịch sử; mục `DONE`, bỏ khóa (BR-U12-35).
4. Đóng popup; đẩy sự kiện SSE; workspace của mọi người tải nội dung mới của mục.

### F11 - Nhả khóa (UC 56)
1. Người giữ, trưởng nhóm (trên workspace) hoặc giảng viên (trên tab Students) bấm Nhả, xác nhận cảnh báo; frontend gọi `POST .../sections/{sectionId}:release`.
2. Kiểm quyền; xóa bản nháp; mục về `DONE` nếu đã có nội dung đăng, không thì `OPEN` (BR-U12-36); audit khi trưởng nhóm hoặc giảng viên nhả.
3. Đẩy sự kiện SSE.

### F12 - Nộp bài nhóm (UC 56)
1. Trưởng nhóm bấm Nộp; hệ thống báo mục chưa Xong; frontend gọi `POST /api/v1/group-docs/{id}/submit` kèm `version`.
2. Kiểm trưởng nhóm hiện tại, tài liệu `IN_PROGRESS`, bài còn nhận bài; `validateForSubmit`; lỗi thì trả danh sách cần sửa (BR-U12-40).
3. Một transaction: chụp bản nộp (phần chung, nội dung đã đăng, tác giả từng mục), `receiptHash`, `late`, `MANUAL`; gọi `GroupEvaluationPort.onGroupSubmitted` (U09); audit.
4. Sau commit phát `group.submitted`; đẩy SSE; trả biên nhận. Trưởng nhóm nộp lại được trước hạn (BR-U12-42).

### F13 - Tự nộp
1. `ScheduledScanner` của U12 chạy mỗi phút: tài liệu `IN_PROGRESS` của bài đã quá hạn cuối 30 giây.
2. U07 ngưng giao bài thì gọi `onRetired`: U12 đánh dấu các tài liệu của bài để tự nộp ngay trong cùng transaction.
3. Với từng tài liệu có thay đổi sau lần nộp cuối: chốt mục `CLAIMED` bằng bản nháp đã lưu, chụp bản nộp `AUTO_DEADLINE` hoặc `AUTO_RETIRED`, gọi `onGroupSubmitted`; tài liệu `CLOSED` (BR-U12-43).
4. Đẩy SSE: client khóa trang "Đang nộp…" rồi chuyển sang thẻ Bản nộp; câu lệnh có điều kiện nên chạy lại không nộp hai lần.

### F14 - Đổi thành viên khi có bài đang mở
1. Trong transaction lưu nhóm (F1 bước 6), với người bị gỡ hoặc chuyển nhóm: nhả mục họ đang giữ (bỏ bản nháp), đóng kênh SSE của họ với tài liệu cũ.
2. Nhóm mới: tạo tài liệu cho mọi bài nhóm đang mở của lớp.
3. Nội dung đã Xong giữ tên tác giả; bản nộp đã có không đổi (BR-U12-05).

## 4. Port cho unit khác

### F15 - Port cho unit khác, realtime
1. U08 hỏi `GroupWorkStatusPort.statusFor` cho Student Assignments; Assignment Detail nhúng nút của U12 qua `GroupWorkEntry`.
2. U09 đọc bản nộp và tiến độ qua `GroupSubmissionSource`; agent của giảng viên đọc bản nộp qua tool `read_authorized_group_submission` (kiểm giảng viên chính của lớp).
3. Ghi dữ liệu hoạt động: U12 kiểm thành viên hiệu lực, tài liệu `IN_PROGRESS`, chính sách đã chụp khi bài mở, rồi gọi `ActivityWritePort.record` loại `GROUP_DOCUMENT`.
4. `SseHub` gửi sự kiện tới mọi instance backend qua exchange `platform.realtime`; U14 dùng `SseHub` cho chuông thông báo.
