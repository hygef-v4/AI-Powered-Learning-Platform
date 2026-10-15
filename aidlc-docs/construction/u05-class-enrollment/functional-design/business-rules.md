# U05 Class & Enrollment — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U05 Class & Enrollment thuộc Iter 1, người code: Người 5; UC 20–30. Tách phần lớp và ghi danh từ U04 cũ; mã `BR-U05-xx` ứng với `BR-U04-xx` cũ cùng số. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

## 1. Lớp học (Subject Manager)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U05-10 | Chỉ Chủ nhiệm môn của môn tạo lớp (UC 26) trên Class List; môn phải `ACTIVE` (hỏi U04); lớp bắt đầu ở `DRAFT`. | UC 26 |
| BR-U05-11 | `code` lớp duy nhất trong môn, tối đa 30 ký tự, không đổi; `subjectId` không đổi. | Thiết kế |
| BR-U05-12 | Giảng viên chính là tài khoản `TEACHER` hoặc `SUBJECT_MANAGER` đang `ACTIVE`. Chỉ Chủ nhiệm môn của môn gán hoặc đổi (UC 27); lớp `OPEN` không được thiếu giảng viên. `teacher_id` hiện thời là căn cứ duy nhất của quyền R3/R4; đổi giảng viên thì người cũ mất quyền ở request tiếp theo. | FR-003, UC 27 |
| BR-U05-13 | Chỉ Chủ nhiệm môn của môn sửa `name`, `description`, `term`, mở, lưu trữ, mở lại lớp và bật/tắt phân bố điểm (UC 29). Giảng viên được giao chỉ xem thông tin lớp và danh sách sinh viên. | FR-003, UC 29 |
| BR-U05-14 | Chuyển trạng thái hợp lệ: `DRAFT→OPEN`, `DRAFT→ARCHIVED`, `OPEN→ARCHIVED`, `ARCHIVED→OPEN`. Mở hoặc mở lại cần có giảng viên và môn `ACTIVE`. | UC 29 |
| BR-U05-15 | Mở lại lớp `ARCHIVED` bị từ chối nếu có sinh viên `ACTIVE` của lớp đang ở lớp chưa lưu trữ khác cùng môn; trả danh sách người vướng. | BR-U05-22 |
| BR-U05-16 | Lưu trữ lớp giữ nguyên ghi danh và dữ liệu học tập; lớp `ARCHIVED` chỉ đọc. | UC 29 |
| BR-U05-17 | `showGradeDistribution` mặc định tắt, chỉ Chủ nhiệm môn bật/tắt trong UC 29. U09 đọc cờ qua `ClassAccessPort.getClassRef` và chỉ trả phân bố ẩn danh trên Student Assignments khi đủ mẫu. | FR-024, UC 29, US-RPT-001 |

## 2. Ghi danh (Subject Manager, UC 28)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U05-20 | Chỉ Chủ nhiệm môn của môn thêm và gỡ sinh viên; giảng viên không thêm/gỡ. Chỉ ghi danh vào lớp `DRAFT` hoặc `OPEN`; người được ghi danh phải có role `STUDENT`, trạng thái `ACTIVE` hoặc `PENDING`. | UC 28 |
| BR-U05-21 | Mỗi `(lớp, sinh viên)` một bản ghi. Đã `ACTIVE` thì báo "đã ghi danh"; đang `REMOVED` thì chuyển lại `ACTIVE`. | US-CAT-003 S2 |
| BR-U05-22 | Một sinh viên chỉ `ACTIVE` ở tối đa 1 lớp chưa lưu trữ của mỗi môn. | Thiết kế |
| BR-U05-23 | Thêm từng người: tìm theo email hoặc tên, tối đa 20 kết quả, chỉ `STUDENT`. Thêm theo danh sách: dán email hoặc CSV một cột, tối đa 200 dòng và 100 KB; xử lý từng dòng, dòng hợp lệ vẫn được ghi danh. | UC 28 |
| BR-U05-24 | Kết quả từng dòng: `ENROLLED`, `RESTORED`, `ALREADY_ENROLLED`, `NOT_FOUND`, `NOT_STUDENT`, `DISABLED`, `IN_OTHER_CLASS`, `INVALID_EMAIL`, `DUPLICATE_IN_LIST`. | UC 28 |
| BR-U05-25 | Gỡ sinh viên: chuyển `REMOVED`, giữ lịch sử bài làm và điểm; quyền R5 bị thu hồi ở request tiếp theo. Cần xác nhận trên UI. | US-CAT-003 S3 |
| BR-U05-26 | Khi ghi danh có hiệu lực với sinh viên (ghi danh vào lớp `OPEN`, hoặc lớp `DRAFT` chuyển `OPEN` thì với mọi ghi danh `ACTIVE`), phát sự kiện `enrollment.activated` sau commit; U14 gửi thông báo trong app và email. | US-NTF-001 |

## 3. Phạm vi cho phân quyền

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U05-30 | `isTeacherOf` đọc `teacher_id` hiện thời, `isEnrolledStudent` đọc ghi danh `ACTIVE` hiện thời, mỗi lần gọi, không cache; đổi giảng viên hoặc gỡ sinh viên có hiệu lực ở request tiếp theo. | BR-U05-12, 25 |
| BR-U05-31 | Phạm vi trả đúng quan hệ cả khi lớp `DRAFT` hoặc `ARCHIVED`; unit sở hữu tài nguyên tự chặn thao tác theo trạng thái lớp đọc từ `ClassAccessPort.getClassRef`. | BR-U05-16 |

## 4. Xem lớp

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U05-40 | Class Dashboard của Student (UC 20) hiện lớp có ghi danh `ACTIVE`: lớp `OPEN` ở "Đang học", lớp `ARCHIVED` ở "Đã kết thúc" (chỉ thông tin lớp); lớp `DRAFT` không hiện. | UC 20 |
| BR-U05-41 | Student Class Detail (UC 21) chỉ trả khi ghi danh `ACTIVE` và lớp `OPEN`. U05 trả thông tin lớp và giảng viên; các phần học liệu (U13), bài tập (U08), thông báo (U14), nhóm (U12) gắn vào qua `StudentClassSection`, mỗi unit tự kiểm quyền R5. | UC 21, FR-005 |
| BR-U05-42 | Không ghi danh, đã bị gỡ, hoặc lớp không mở thì trả "không tìm thấy", không lộ tên hay thông tin lớp. | US-LRN-001 S2 |
| BR-U05-44 | Không lưu tiến độ, lộ trình hay trạng thái hoàn thành bài học. | Phạm vi |
| BR-U05-45 | Class Dashboard của Teacher và Subject Manager (UC 22) hiện lớp mình là giảng viên chính: `DRAFT` ở "Sắp mở", `OPEN` ở "Đang dạy", `ARCHIVED` ở "Đã kết thúc". | UC 22 |
| BR-U05-46 | Teacher Class Detail (UC 23) chỉ mở được khi là giảng viên chính của lớp. U05 cung cấp tab Class Detail (thông tin chỉ đọc) và tab Students (sinh viên `ACTIVE`); tab Materials (U13), Evals (U07, U09, U10) và phần nhóm trong tab Students (U12) gắn vào qua `TeacherClassTab`. | UC 23 |
| BR-U05-47 | Student Detail (popup từ tab Students) chỉ hiện email, tên hiển thị, ngày ghi danh và nhóm trong lớp; phần nhóm do U12 gắn vào qua `StudentDetailSection`. Chỉ đọc. | UC 23; người dùng chốt 2026-10-09 |
| BR-U05-48 | Manager Dashboard (UC 30) là trang đích của Subject Manager: các môn mình quản lý (lấy từ U04) kèm số lớp theo trạng thái; lối vào My Subject, Class List và các màn của unit khác (Material List, Question List, Quiz List, Assignment List) đăng ký qua `DashboardNavEntry`; nút sang Class Dashboard nếu có dạy lớp. My Subject hiện thông tin môn và số lớp theo trạng thái. | UC 30, BR-U01-48 |
| BR-U05-49 | Managed Class Detail (UC 25) hiện thông tin lớp, giảng viên chính và danh sách sinh viên; thao tác UC 27–29 nằm trên màn này. | UC 25 |

## 5. Phân quyền, audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U05-50 | Chủ nhiệm môn thấy lớp của các môn mình quản lý (UC 24); giảng viên thấy lớp mình dạy (UC 22); sinh viên thấy lớp đã ghi danh (UC 20). Trang tối đa 100 dòng, tìm theo mã hoặc tên. | UC 20, 22, 24 |
| BR-U05-51 | Truy cập lớp ngoài phạm vi trả "không tìm thấy" và ghi audit `ACCESS_DENIED`. | US-CAT-002 S3 |
| BR-U05-52 | Audit: `CLASS_CREATED`, `CLASS_UPDATED`, `CLASS_STATUS_CHANGED`, `GRADE_DISTRIBUTION_CHANGED`, `CLASS_TEACHER_CHANGED` (trước/sau), `STUDENT_ENROLLED`, `STUDENT_REMOVED` (mỗi người một sự kiện). | FR-014, SEC-005 |
| BR-U05-53 | Dữ liệu sai (lớp, môn hoặc người không tồn tại, sai role) thì từ chối toàn bộ thao tác đó, báo cách sửa, không lộ dữ liệu người khác. | US-CAT-001 S2 |
| BR-U05-54 | U01 hoặc U04 không trả lời thì từ chối (fail closed). | BR-U01-93 |
| BR-U05-55 | Admin không có màn hay API lớp; Admin chỉ xem danh sách lớp của một môn qua Subject Detail của U04. | BR-U01-60 |
