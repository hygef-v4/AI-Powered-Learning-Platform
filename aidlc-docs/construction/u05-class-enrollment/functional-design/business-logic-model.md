# U05 Class & Enrollment — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U05 Class & Enrollment thuộc Iter 1, người code: Người 5; UC 20–30. Tách phần lớp và ghi danh từ U04 cũ; mã `BR-U05-xx` ứng với `BR-U04-xx` cũ cùng số. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Lớp đang học | UC 20 | Class Dashboard (Student) |
| F2 Chi tiết lớp đang học | UC 21 | Student Class Detail |
| F3 Lớp được giao dạy | UC 22 | Class Dashboard (Teacher, Subject Manager) |
| F4 Chi tiết lớp được giao dạy | UC 23 | Teacher Class Detail, popup Student Detail |
| F5 Môn được giao quản lý | UC 30 | Manager Dashboard, My Subject |
| F6 Lớp của môn | UC 24 | Class List |
| F7 Tạo lớp | UC 26 | Class List (popup Tạo lớp) |
| F8 Chi tiết lớp của môn | UC 25 | Class Detail |
| F9 Gán giảng viên | UC 27 | Class Detail (popup Phân công) |
| F10 Thêm từng sinh viên | UC 28 | Class Detail |
| F11 Thêm sinh viên theo danh sách | UC 28 | Class Detail |
| F12 Gỡ sinh viên | UC 28 | Class Detail |
| F13 Sửa thông tin lớp, phân bố điểm | UC 29 | Class Detail |
| F14 Đổi trạng thái lớp | UC 29 | Class Detail |
| F15 Phạm vi lớp cho unit khác | Mọi UC cần R3/R4/R5 | - |

## 2. Sinh viên

### F1 - Lớp đang học (UC 20)
1. Student vào Class Dashboard sau đăng nhập; frontend gọi `GET /api/v1/me/classes`.
2. Lấy ghi danh `ACTIVE` của sinh viên; bỏ lớp `DRAFT`.
3. Chia "Đang học" (`OPEN`) và "Đã kết thúc" (`ARCHIVED`); mỗi thẻ có mã môn, tên lớp, giảng viên (BR-U05-40).
4. Thanh bên có lối vào do unit khác đăng ký: Student Assignments (U08), Quiz Practice History (U11), Class Announcements (U14).

### F2 - Chi tiết lớp đang học (UC 21)
1. Student bấm một lớp; frontend gọi `GET /api/v1/me/classes/{id}`.
2. Kiểm ghi danh `ACTIVE` và lớp `OPEN`; không đạt thì trả "không tìm thấy" (BR-U05-41, 42).
3. Trả thông tin lớp, môn, giảng viên.
4. Trang hiện các phần do unit khác gắn vào: học liệu (U13), bài tập (U08), thông báo (U14), nhóm của tôi (U12); mỗi phần gọi API của unit đó.

## 3. Giảng viên

### F3 - Lớp được giao dạy (UC 22)
1. Teacher hoặc Subject Manager mở Class Dashboard; frontend gọi `GET /api/v1/teaching/classes`.
2. Lấy lớp có `teacher_id` là người đó; chia "Sắp mở", "Đang dạy", "Đã kết thúc" (BR-U05-45).

### F4 - Chi tiết lớp được giao dạy (UC 23)
1. Bấm một lớp; frontend gọi `GET /api/v1/teaching/classes/{id}`.
2. Kiểm người gọi là giảng viên chính; không phải thì trả "không tìm thấy" và ghi audit (BR-U05-46, 51).
3. Tab Class Detail: thông tin lớp chỉ đọc.
4. Tab Students: `GET /api/v1/teaching/classes/{id}/students` trả sinh viên `ACTIVE` (email, tên, ngày ghi danh); phần nhóm của U12 gắn vào tab này.
5. Bấm một sinh viên mở popup Student Detail: email, tên, ngày ghi danh; phần nhóm do U12 gắn vào (BR-U05-47).
6. Tab Materials, Evals do U13, U07, U09, U10 gắn vào.

## 4. Chủ nhiệm môn

### F5 - Môn được giao quản lý (UC 30)
1. Subject Manager đăng nhập vào Manager Dashboard; frontend gọi `GET /api/v1/manager/subjects`.
2. Lấy các môn qua `SubjectQueryPort.listManagedSubjects` (U04); đếm lớp theo trạng thái của từng môn.
3. Hiện thẻ môn; lối vào My Subject, Class List và các màn của unit khác (Material List, Question List, Quiz List, Assignment List); nút sang Class Dashboard nếu có lớp đang dạy (BR-U05-48).
4. Bấm một môn mở My Subject: `GET /api/v1/manager/subjects/{id}` kiểm người gọi là Chủ nhiệm môn; trả thông tin môn và số lớp theo trạng thái.

### F6 - Lớp của môn (UC 24)
1. Mở Class List; frontend gọi `GET /api/v1/manager/classes` kèm môn, trạng thái, học kỳ, từ khóa.
2. Chỉ trả lớp của các môn người gọi đang quản lý; tối đa 100 dòng (BR-U05-50).

### F7 - Tạo lớp (UC 26)
1. Trên Class List bấm "Tạo lớp"; popup mở.
2. Chọn môn, nhập mã, tên, mô tả, học kỳ; tùy chọn chọn giảng viên (ô tìm gọi `GET /api/v1/manager/teacher-candidates`; U05 gọi `AccountLookupPort.findAccounts` với role `TEACHER`, `SUBJECT_MANAGER` và chỉ giữ người `ACTIVE`).
3. Kiểm người gọi là Chủ nhiệm môn của môn và môn `ACTIVE` (BR-U05-10).
4. Kiểm mã trong môn (BR-U05-11); kiểm giảng viên nếu có (BR-U05-12).
5. Tạo lớp `DRAFT`; ghi audit `CLASS_CREATED` (và `CLASS_TEACHER_CHANGED` nếu có giảng viên).

### F8 - Chi tiết lớp của môn (UC 25)
1. Bấm một lớp trên Class List; frontend gọi `GET /api/v1/manager/classes/{id}`.
2. Kiểm người gọi là Chủ nhiệm môn của môn chứa lớp; không phải thì "không tìm thấy" và audit.
3. Trả thông tin lớp, giảng viên chính, danh sách sinh viên (`ACTIVE` và `REMOVED`), `version`; các nút F9–F14 nằm trên màn này.

### F9 - Gán giảng viên (UC 27)
1. Bấm "Phân công giảng viên", tìm và chọn người; frontend gọi `PUT /api/v1/manager/classes/{id}/teacher` kèm `version`.
2. Kiểm quyền Chủ nhiệm môn và lớp chưa lưu trữ (BR-U05-16); người mới phải `TEACHER` hoặc `SUBJECT_MANAGER` đang `ACTIVE` (BR-U05-12).
3. Lớp `OPEN` không được bỏ trống giảng viên.
4. Cập nhật có điều kiện `version`; ghi audit `CLASS_TEACHER_CHANGED` (người cũ, người mới).
5. Từ request tiếp theo, người cũ mất quyền dạy lớp.

### F10 - Thêm từng sinh viên (UC 28)
1. Gõ email hoặc tên; frontend gọi `GET /api/v1/manager/classes/{id}/student-candidates?q=`, U05 tra `AccountLookupPort.findStudents` (tối đa 20).
2. Chọn sinh viên; frontend gọi `POST /api/v1/manager/classes/{id}/enrollments`.
3. Kiểm quyền Chủ nhiệm môn, lớp `DRAFT` hoặc `OPEN`, role `STUDENT`, trạng thái `ACTIVE` hoặc `PENDING` (BR-U05-20).
4. Khóa hàng theo (sinh viên, môn), kiểm chưa `ACTIVE` ở lớp chưa lưu trữ khác cùng môn (BR-U05-22).
5. Tạo ghi danh `ACTIVE` hoặc khôi phục bản `REMOVED` (BR-U05-21); ghi audit `STUDENT_ENROLLED`.
6. Lớp `OPEN` thì phát `enrollment.activated` sau commit (BR-U05-26).

### F11 - Thêm sinh viên theo danh sách (UC 28)
1. Dán email hoặc chọn CSV một cột; frontend chặn quá 200 dòng.
2. `POST /api/v1/manager/classes/{id}/enrollments:batch`; kiểm quyền và giới hạn kích thước.
3. Chuẩn hóa chữ thường, đánh dấu trùng trong danh sách; tra U01 một lần bằng `findByEmails`.
4. Mỗi dòng chạy F10 bước 3–6 trong transaction riêng và ghi kết quả (BR-U05-24); dòng lỗi không làm hỏng dòng khác.
5. Trả bảng kết quả; không lưu lô.

### F12 - Gỡ sinh viên (UC 28)
1. Bấm Gỡ ở dòng sinh viên, xác nhận; frontend gọi `PUT /api/v1/manager/classes/{id}/enrollments/{accountId}/status` với `REMOVED`.
2. Kiểm quyền và lớp chưa lưu trữ; chuyển `REMOVED`, ghi `removedAt`; ghi audit `STUDENT_REMOVED` (BR-U05-25).
3. Từ request tiếp theo, sinh viên mất quyền vào lớp; bài làm và điểm giữ nguyên.
4. Thêm lại người đã gỡ: cùng API với `ACTIVE`, chạy F10 bước 3–6.

### F13 - Sửa thông tin lớp và phân bố điểm (UC 29)
1. Sửa tên, mô tả, học kỳ kèm `version`: `PATCH /api/v1/manager/classes/{id}`; mã lớp và môn không sửa được.
2. Bật hoặc tắt phân bố điểm: `PUT /api/v1/manager/classes/{id}/grade-distribution` kèm `version`, xác nhận khi bật.
3. Kiểm quyền Chủ nhiệm môn và lớp chưa lưu trữ (BR-U05-16); cập nhật có điều kiện `version`, lệch thì `409`.
4. Ghi audit `CLASS_UPDATED` hoặc `GRADE_DISTRIBUTION_CHANGED`.

### F14 - Đổi trạng thái lớp (UC 29)
1. Bấm Mở, Lưu trữ hoặc Mở lại, xác nhận; frontend gọi `PUT /api/v1/manager/classes/{id}/status` kèm trạng thái mới và `version`.
2. Kiểm quyền Chủ nhiệm môn và chuyển hợp lệ (BR-U05-14).
3. Mở hoặc mở lại: cần giảng viên và môn `ACTIVE` (hỏi U04); mở lại kiểm BR-U05-15, vướng thì trả danh sách người vướng.
4. Cập nhật có điều kiện `version`; ghi audit `CLASS_STATUS_CHANGED`.
5. `DRAFT→OPEN`: phát `enrollment.activated` cho mọi ghi danh `ACTIVE` sau commit (BR-U05-26).
6. Lưu trữ: lớp chỉ đọc; dữ liệu giữ nguyên (BR-U05-16).

## 5. Phạm vi cho unit khác

### F15 - Trả phạm vi lớp
1. U01 gọi `ClassScopePort.isTeacherOf` (R3/R4) hoặc `isEnrolledStudent` (R5) khi quyết định quyền; U05 đọc `teacher_id` và ghi danh hiện thời, không cache (BR-U05-30).
2. U01 gọi `listTaughtClasses` khi chặn hạ role người đang dạy lớp.
3. U04 gọi `SubjectClassesPort` để hiện lớp của môn và chặn lưu trữ môn.
4. U07–U15 gọi `ClassAccessPort`: thông tin lớp, sinh viên `ACTIVE`, Chủ nhiệm môn của lớp, lớp `OPEN` của một người hoặc của một môn, số đếm cho Admin Dashboard.
