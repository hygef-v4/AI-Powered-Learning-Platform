# U04 Subject, Class, Enrollment & Learning Access - Business Rules

**Bản tài liệu 2026-10-09**: UC 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Môn học (Admin)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U04-01 | Chỉ ADMIN xem danh sách môn (UC 64), thêm môn (UC 65), xem chi tiết môn (UC 66) và sửa môn (UC 67). | UC 64–67 |
| BR-U04-02 | `code` môn duy nhất, chỉ chữ hoa/số/`-`, không đổi sau khi tạo. | UC 65, 67 |
| BR-U04-03 | Mỗi môn tối đa một Chủ nhiệm môn, phải là tài khoản `SUBJECT_MANAGER` đang `ACTIVE`; Admin không được gán. Gán khi thêm môn (UC 65) hoặc khi sửa môn (UC 67); gán mới thay người cũ và audit. `subjects.manager_id` hiện thời cấp quyền R2. | FR-002, FR-003; người dùng chốt 2026-10-09 |
| BR-U04-04 | Lưu trữ và mở lại môn là thao tác trong UC 67: chỉ lưu trữ khi mọi lớp của môn đã `ARCHIVED`; môn `ARCHIVED` không tạo lớp mới; mở lại về `ACTIVE`. Không xóa môn. | Câu 12; người dùng chốt 2026-10-09 |
| BR-U04-05 | Subject Detail của Admin (UC 66) hiện thông tin môn, Chủ nhiệm môn và danh sách lớp của môn chỉ đọc (mã, tên, học kỳ, trạng thái, giảng viên, số sinh viên). Admin không mở chi tiết lớp và không sửa lớp. | UC 66; người dùng chốt 2026-10-09 |

## 2. Lớp học (Subject Manager)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U04-10 | Chỉ Chủ nhiệm môn của môn tạo lớp (UC 49) trên Class List; lớp thuộc đúng 1 môn `ACTIVE`, bắt đầu ở `DRAFT`. | UC 49 |
| BR-U04-11 | `code` lớp duy nhất trong môn, không đổi; `subjectId` không đổi. | Thiết kế |
| BR-U04-12 | Giảng viên chính là tài khoản `TEACHER` hoặc `SUBJECT_MANAGER` đang `ACTIVE`. Chỉ Chủ nhiệm môn của môn gán/đổi (UC 50); lớp `OPEN` không được thiếu giảng viên. `teacher_id` hiện thời cấp quyền R3/R4. | FR-003, UC 50 |
| BR-U04-13 | Chỉ Chủ nhiệm môn của môn sửa `name`, `description`, `term`, mở/lưu trữ/mở lại lớp và bật/tắt phân bố điểm (UC 52 Edit Class Information). Teacher được giao dạy chỉ xem thông tin lớp và danh sách sinh viên. | FR-003, UC 52; người dùng chốt 2026-10-09 |
| BR-U04-14 | Chuyển trạng thái hợp lệ: `DRAFT→OPEN`, `DRAFT→ARCHIVED`, `OPEN→ARCHIVED`, `ARCHIVED→OPEN`. `OPEN` cần có giảng viên và môn `ACTIVE`. | Câu 1, UC 52 |
| BR-U04-15 | Mở lại lớp `ARCHIVED` bị từ chối nếu có người học `ACTIVE` của lớp đang ở lớp chưa lưu trữ khác cùng môn; trả danh sách người vướng. | BR-U04-22 |
| BR-U04-16 | Lưu trữ lớp giữ nguyên ghi danh và dữ liệu học tập; lớp `ARCHIVED` chỉ đọc. | UC 52 |
| BR-U04-17 | `showGradeDistribution` là cài đặt lớp trong UC 52, chỉ Chủ nhiệm môn bật/tắt, mặc định tắt. U16 chỉ trả phân bố ẩn danh trên Student Assignments khi đủ mẫu. | FR-024, UC 52 |

## 3. Ghi danh (Subject Manager, UC 51)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U04-20 | Chỉ Chủ nhiệm môn của môn thêm và gỡ sinh viên (UC 51 Add/Remove Class Students); Teacher không thêm/gỡ. Ghi danh chỉ vào lớp `DRAFT` hoặc `OPEN`; người được ghi danh phải có role `STUDENT`, trạng thái `ACTIVE` hoặc `PENDING`. | UC 51; người dùng chốt 2026-10-09 |
| BR-U04-21 | Mỗi `(lớp, người học)` một bản ghi. Đã `ACTIVE` → không làm gì, báo "đã ghi danh". Đang `REMOVED` → chuyển lại `ACTIVE`. | Câu 8, US-CAT-003 S2 |
| BR-U04-22 | Một người học chỉ `ACTIVE` ở tối đa 1 lớp chưa lưu trữ của mỗi môn. | Câu 10 |
| BR-U04-23 | Thêm từng người: tìm theo email/tên (≤ 20 kết quả, chỉ `STUDENT`). Thêm theo danh sách: dán email hoặc CSV 1 cột, ≤ 200 dòng; xử lý từng dòng, dòng hợp lệ vẫn được ghi danh. | Câu 4 |
| BR-U04-24 | Kết quả từng dòng: `ENROLLED`, `RESTORED`, `ALREADY_ENROLLED`, `NOT_FOUND`, `NOT_STUDENT`, `DISABLED`, `IN_OTHER_CLASS`, `INVALID_EMAIL`, `DUPLICATE_IN_LIST`. | Câu 4 |
| BR-U04-25 | Gỡ sinh viên (UC 51): chuyển `REMOVED`, giữ lịch sử; quyền bị thu hồi ngay. Cần xác nhận trên UI. | US-CAT-003 S3 |
| BR-U04-26 | Khi ghi danh có hiệu lực với người học (ghi danh vào lớp `OPEN`, hoặc lớp `DRAFT` chuyển `OPEN` thì với mọi ghi danh `ACTIVE`), phát event `enrollment.activated`; U16 gửi thông báo trong app **và** email. | Câu 6 |

## 4. Mã mời (đã bỏ)

Tự ghi danh bằng mã mời đã bỏ ngày 2026-10-09 (người dùng chốt; không có trong 73 UC). Các quy tắc BR-U04-30…34 không còn dùng; sinh viên chỉ vào lớp khi Chủ nhiệm môn thêm (UC 51).

## 5. Xem lớp (Student, Teacher, Subject Manager)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U04-40 | Class Dashboard của Student (UC 13) hiện lớp mình đang ghi danh `ACTIVE`: lớp `OPEN` ở mục "Đang học", lớp `ARCHIVED` ở mục "Đã kết thúc" (chỉ thông tin lớp); lớp `DRAFT` không hiện. | UC 13, US-LRN-001 |
| BR-U04-41 | Student Class Detail (UC 14) chỉ trả khi ghi danh `ACTIVE` và lớp `OPEN`: thông tin lớp, giảng viên, module `ACTIVE` của môn kèm học liệu đang hiển thị của môn và của lớp (qua `PublishedContentPort`), lối vào bài tập, thông báo và nhóm của mình. | UC 14, FR-005 |
| BR-U04-42 | Không ghi danh, bị gỡ, hoặc lớp không mở → trả "không tìm thấy", không lộ tên hay metadata lớp. | US-LRN-001 S2 |
| BR-U04-43 | Thanh toán không ảnh hưởng quyền vào lớp; U04 không gọi U07. | Câu 7 |
| BR-U04-44 | Không lưu tiến độ, lộ trình hay trạng thái hoàn thành bài học. | unit-of-work.md |
| BR-U04-45 | Class Dashboard của Teacher và Subject Manager (UC 31) hiện các lớp mình là giảng viên chính: `DRAFT` ở mục "Sắp mở", `OPEN` ở "Đang dạy", `ARCHIVED` ở "Đã kết thúc". | UC 31 |
| BR-U04-46 | Teacher Class Detail (UC 32) chỉ mở được khi là giảng viên chính của lớp: tab Class Detail (thông tin lớp chỉ đọc) và tab Students (danh sách sinh viên `ACTIVE`) do U04 cung cấp; các tab và lối vào khác (Evals, Materials, quiz, bài tập) do unit sở hữu cung cấp. | UC 32 |
| BR-U04-47 | Student Detail (mở từ tab Students, thuộc UC 32) chỉ hiện thông tin cơ bản: email, tên hiển thị, nhóm trong lớp (U12 cung cấp), ngày ghi danh; chỉ đọc. | UC 32; người dùng chốt 2026-10-09 |
| BR-U04-48 | Manager Dashboard (UC 53) là trang đích của Subject Manager: danh sách môn được giao, lối vào Subject Detail, Class List, Material List, Question List, Quiz List, Assignment List (lọc theo môn, nội dung do unit sở hữu), và nút sang Class Dashboard nếu có dạy lớp. Subject Detail của Subject Manager hiện thông tin môn và số lớp theo trạng thái. | UC 53, BR-U01-48 |
| BR-U04-49 | Managed Class Detail (UC 48) hiện thông tin lớp, giảng viên chính và danh sách sinh viên; các thao tác UC 50–52 nằm trên màn này. | UC 48 |

## 6. Phân quyền, audit và lỗi

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U04-50 | Danh sách: ADMIN thấy mọi môn (UC 64) và không có danh sách lớp riêng; Chủ nhiệm môn thấy môn mình và lớp của các môn đó (UC 47, 53); giảng viên thấy lớp mình dạy (UC 31); người học thấy lớp đã ghi danh (UC 13). Trang ≤ 100, tìm theo mã/tên. | UC 13, 31, 47, 53, 64 |
| BR-U04-51 | Truy cập ngoài phạm vi → "không tìm thấy" và audit `ACCESS_DENIED`. | US-CAT-002 S3 |
| BR-U04-52 | Audit: tạo/sửa/lưu trữ/mở lại môn, gán Chủ nhiệm môn, tạo/sửa/đổi trạng thái lớp, bật/tắt phân bố điểm, gán giảng viên, ghi danh/gỡ (mỗi người một sự kiện). | SEC-005, FR-003 |
| BR-U04-53 | Lỗi cấu hình (môn/lớp/người không tồn tại, sai role) → từ chối toàn bộ thao tác đó, thông báo cách sửa, không lộ dữ liệu người khác. | US-CAT-001 S2 |
| BR-U04-54 | U01 không gọi được → từ chối (fail closed). | BR-U01-93 |
