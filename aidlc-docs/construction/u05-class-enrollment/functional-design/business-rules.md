# U05 Class & Enrollment — business-rules

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

**Quyết định 2026-11-07 — D-U04-04:** Bỏ học kỳ lớp khỏi UI, request/response, domain và dữ liệu hiện hành; xem [quyết định đã duyệt](../../../inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md). Migration mới `V20261107_2200__remove_class_term.sql` xóa cột cũ khi áp dụng; không sửa migration hoặc bằng chứng lịch sử. Các quy tắc khác giữ nguyên phạm vi đã chốt.

## 1. Lớp học (Subject Manager)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U05-10 | Chỉ Chủ nhiệm môn của môn tạo lớp (UC 26) trên Class List; môn phải `ACTIVE` (hỏi U04); lớp bắt đầu ở `DRAFT`. | UC 26 |
| BR-U05-11 | `code` lớp duy nhất trong môn, tối đa 30 ký tự, không đổi; `subjectId` không đổi. | Thiết kế |
| BR-U05-12 | Teacher được gán phải ACTIVE và role TEACHER/SUBJECT_MANAGER; chỉ Chủ nhiệm môn của môn gán/đổi. OPEN được phép thiếu teacher. teacher_id hiện hành là căn cứ teaching scope; chưa gán thì không có educator nhận quyền từ phân công đó. Đổi teacher mất quyền ở request tiếp theo. | FR-003, UC 27 |
| BR-U05-13 | Chỉ Chủ nhiệm môn của môn sửa `name`, `description`, mở, lưu trữ, mở lại lớp (UC 29). Giảng viên được giao chỉ xem thông tin lớp và danh sách sinh viên trong U05; quyền cài đặt phân bố theo bài–lớp thuộc U09. | FR-003, UC 29 |
| BR-U05-14 | Chuyển DRAFT→OPEN, DRAFT→ARCHIVED, OPEN→ARCHIVED, ARCHIVED→OPEN. Create vẫn DRAFT. Mở/mở lại cần môn ACTIVE và kiểm enrollment conflict; teacher có thể null, nếu đã gán phải còn hợp lệ. Source-observed thay quy tắc bắt buộc teacher trước 27/10. | UC 29 |
| BR-U05-15 | Mở lại lớp `ARCHIVED` bị từ chối nếu có sinh viên `ACTIVE` của lớp đang ở lớp chưa lưu trữ khác cùng môn; trả danh sách người vướng. | BR-U05-22 |
| BR-U05-16 | Lưu trữ lớp giữ nguyên ghi danh và dữ liệu học tập; lớp `ARCHIVED` chỉ đọc. Khi U04 lưu trữ môn, U05 lưu trữ mọi lớp `DRAFT`, `OPEN` của môn qua `ISubjectClasses.archiveClassesOfSubject` (BR-U04-04), ghi audit từng lớp. | UC 29, BR-U04-04 |
| BR-U05-17 | Phân bố điểm không thuộc UC 29; U09 sở hữu cài đặt mặc định tắt theo bài–lớp, chỉ giảng viên được giao dạy lớp bật/tắt. Cột lịch sử `course_classes.show_grade_distribution` và field `showGradeDistribution` còn giữ dormant, không dùng làm quyền hay chuyển true cũ sang cài đặt mới. | FR-024, US-RPT-001 |

## 2. Ghi danh (Subject Manager, UC 28)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U05-20 | Chỉ Chủ nhiệm môn của môn thêm và gỡ sinh viên; giảng viên không thêm/gỡ. Chỉ ghi danh vào lớp `DRAFT` hoặc `OPEN`; người được ghi danh phải có role `STUDENT`, trạng thái `ACTIVE` hoặc `PENDING`. Thêm thủ công, khôi phục và từng dòng nhập danh sách đều kiểm quyền trước, rồi khóa tài khoản đích trước khóa môn, giữ role/trạng thái hợp lệ tới commit; thay đổi tài khoản đồng thời phải chờ khóa hoặc được thấy trước khi kiểm điều kiện. | UC 28, KAN-155 |
| BR-U05-21 | Mỗi `(lớp, sinh viên)` một bản ghi. Đã `ACTIVE` thì báo "đã ghi danh"; đang `REMOVED` thì chuyển lại `ACTIVE`. | US-CAT-003 S2 |
| BR-U05-22 | Một sinh viên chỉ `ACTIVE` ở tối đa 1 lớp chưa lưu trữ của mỗi môn. | Thiết kế |
| BR-U05-23 | Thêm từng người: tìm theo email hoặc tên, tối đa 20 kết quả, chỉ `STUDENT`. Thêm theo danh sách: dán email hoặc tải Excel `.xlsx` một cột `email` ở sheet đầu, tối đa 200 dòng dữ liệu và 100 KB; frontend chuyển workbook về danh sách text cho API hiện hữu; xử lý từng dòng, dòng hợp lệ vẫn được ghi danh. | UC 28 |
| BR-U05-24 | Kết quả từng dòng: `ENROLLED`, `RESTORED`, `ALREADY_ENROLLED`, `NOT_FOUND`, `NOT_STUDENT`, `DISABLED`, `IN_OTHER_CLASS`, `INVALID_EMAIL`, `DUPLICATE_IN_LIST`. | UC 28 |
| BR-U05-25 | Gỡ sinh viên: chuyển `REMOVED`, giữ lịch sử bài làm và điểm; quyền R5 bị thu hồi ở request tiếp theo. Cần xác nhận trên UI. | US-CAT-003 S3 |
| BR-U05-26 | Khi ghi danh có hiệu lực với sinh viên (ghi danh vào lớp `OPEN`, hoặc lớp `DRAFT` chuyển `OPEN` hay lớp `ARCHIVED` mở lại thành `OPEN` thì với mọi ghi danh `ACTIVE`), phát sự kiện `enrollment.activated` sau commit; U14 gửi thông báo trong app và email. | US-NTF-001 |

## 3. Phạm vi cho phân quyền

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U05-30 | `isTeacherOf` đọc `teacher_id` hiện thời, `isEnrolledStudent` đọc ghi danh `ACTIVE` hiện thời, mỗi lần gọi, không cache; đổi giảng viên hoặc gỡ sinh viên có hiệu lực ở request tiếp theo. | BR-U05-12, 25 |
| BR-U05-31 | Phạm vi trả đúng quan hệ cả khi lớp `DRAFT` hoặc `ARCHIVED`; unit sở hữu tài nguyên tự chặn thao tác theo trạng thái lớp đọc từ `IClassAccess.getClassRef`. | BR-U05-16 |

## 4. Xem lớp

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U05-40 | Class Dashboard của Student (UC 20) hiện lớp có ghi danh `ACTIVE`: lớp `OPEN` ở "Đang học", lớp `ARCHIVED` ở "Đã kết thúc" (chỉ thông tin lớp); lớp `DRAFT` không hiện. | UC 20 |
| BR-U05-41 | Student Class Detail (UC 21) chỉ trả khi ghi danh `ACTIVE` và lớp `OPEN`. U05 trả thông tin lớp và giảng viên; các phần học liệu (U13), bài tập (U08), thông báo (U14), nhóm (U12) gắn vào qua `StudentClassSection`, mỗi unit tự kiểm quyền R5. | UC 21, FR-005 |
| BR-U05-42 | Không ghi danh, đã bị gỡ, hoặc lớp không mở thì trả "không tìm thấy", không lộ tên hay thông tin lớp. | US-LRN-001 S2 |
| BR-U05-44 | Không lưu tiến độ, lộ trình hay trạng thái hoàn thành bài học. | Phạm vi |
| BR-U05-45 | Class Dashboard của Teacher và Subject Manager (UC 22) hiện lớp mình là giảng viên chính: `DRAFT` ở "Sắp mở", `OPEN` ở "Đang dạy", `ARCHIVED` ở "Đã kết thúc". | UC 22 |
| BR-U05-46 | Teacher Class Detail (UC 23) chỉ mở được khi là giảng viên chính của lớp. U05 cung cấp tab Class Detail (thông tin chỉ đọc) và tab Students (sinh viên `ACTIVE`); tab Materials (U13), Evals (U07, U09, U10) và phần nhóm trong tab Students (U12) gắn vào qua `TeacherClassTab`. | UC 23 |
| BR-U05-47 | Teacher Class Detail hiển thị roster chỉ đọc; trang hiện tại không mở Student Detail popup khi bấm sinh viên. Backend scoped student-detail endpoint vẫn kiểm active student/class. Không biến popup cũ chưa nối vào UI thành acceptance đã có. | UC 23; người dùng chốt 2026-10-09 |
| BR-U05-48 | Manager Dashboard (UC 30) là trang đích của Subject Manager: các môn mình quản lý (lấy từ U04) kèm số lớp theo trạng thái và số học liệu, câu hỏi, Quiz, bài tập do U13, U10, U07 gắn qua `SubjectSummarySection`; lối vào My Subject; menu bên (`NavEntry` của U01) có My Subject, Class List và màn của unit khác (Material List, Question List, Quiz List, Assignment List); nút sang Class Dashboard nếu có dạy lớp. My Subject hiện thông tin môn, số lớp theo trạng thái và các số liệu đó. | UC 30, BR-U01-48 |
| BR-U05-49 | Managed Class Detail (UC 25) hiện thông tin lớp, giảng viên chính và danh sách sinh viên; thao tác UC 27–29 nằm trên màn này. | UC 25 |

## 5. Phân quyền, audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U05-50 | Chủ nhiệm môn thấy lớp của các môn mình quản lý (UC 24); giảng viên thấy lớp mình dạy (UC 22); sinh viên thấy lớp đã ghi danh (UC 20). Trang tối đa 100 dòng, tìm theo mã hoặc tên. | UC 20, 22, 24 |
| BR-U05-51 | Truy cập lớp ngoài phạm vi trả "không tìm thấy" và ghi audit `ACCESS_DENIED`. | US-CAT-002 S3 |
| BR-U05-52 | Audit: `CLASS_CREATED`, `CLASS_UPDATED`, `CLASS_STATUS_CHANGED`, `CLASS_TEACHER_CHANGED` (trước/sau), `STUDENT_ENROLLED`, `STUDENT_REMOVED` (mỗi người một sự kiện). Cài đặt phân bố do U09 audit theo bài–lớp. | FR-014, SEC-005 |
| BR-U05-53 | Dữ liệu sai (lớp, môn hoặc người không tồn tại, sai role) thì từ chối toàn bộ thao tác đó, báo cách sửa, không lộ dữ liệu người khác. | US-CAT-001 S2 |
| BR-U05-54 | U01 hoặc U04 không trả lời thì từ chối (fail closed). | BR-U01-93 |
| BR-U05-55 | Admin không có màn hay API lớp; Admin chỉ xem danh sách lớp của một môn qua Subject Detail của U04. | BR-U01-60 |

## 6. Mã số sinh viên (MSSV) — 2026-11-13

Nguồn: [yêu cầu SC-09, SC-10, SC-11](../../../inception/requirements/2026-11-13-student-code.md) và [FD Q5, Q6](../../plans/functional-design-plan/2026-11-13-u01-student-code-functional-design-plan.md). U01 sở hữu MSSV; xem BR-U01-100…115.

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U05-56 | Ghi danh theo danh sách nhận mỗi dòng là một email hoặc một MSSV, và một danh sách được trộn cả hai loại. Hệ thống nhận biết từng dòng như sau: dòng có `@` là email (chuẩn hóa chữ thường như cũ). Dòng còn lại được đổi sang in hoa; nếu khớp `^[A-Z]{2}[0-9]{6}$` thì là MSSV. Không khớp loại nào thì báo `INVALID_IDENTIFIER`. Mã `INVALID_IDENTIFIER` thay cho `INVALID_EMAIL` cũ (FD Q5). Header ở dòng đầu là tùy chọn và nhận một trong các giá trị `email`, `student_code`, `mssv`, không phân biệt hoa thường. Giới hạn 200 dòng và 100 KB giữ nguyên. Tệp Excel tải lên dùng một cột với một trong các header trên. | SC-09, FD Q5 |
| BR-U05-57 | Một giá trị đã chuẩn hóa xuất hiện lần thứ hai trong danh sách thì báo `DUPLICATE_IN_LIST`. Hệ thống tra email bằng `findByEmails` và tra MSSV bằng `findByStudentCodes`, mỗi loại tra một lần. Giá trị không thuộc tài khoản nào thì báo `NOT_FOUND`, giống email không tìm thấy hiện nay. Nếu một dòng email và một dòng MSSV cùng chỉ tới một sinh viên, dòng sau nhận kết quả `ALREADY_ENROLLED` theo BR-U05-21. Mỗi dòng vẫn chạy trong transaction riêng như hiện nay. | SC-09, FD Q5 |
| BR-U05-58 | Kết quả của mỗi dòng hiện đúng giá trị người dùng đã nhập (email hoặc MSSV). Bộ kết quả đầy đủ của BR-U05-24 trở thành: `ENROLLED`, `RESTORED`, `ALREADY_ENROLLED`, `NOT_FOUND`, `NOT_STUDENT`, `DISABLED`, `IN_OTHER_CLASS`, `INVALID_IDENTIFIER`, `DUPLICATE_IN_LIST`. | SC-09 |
| BR-U05-59 | Ô tìm khi thêm từng sinh viên (BR-U05-23) tìm được theo email, tên hoặc MSSV. Giới hạn 20 kết quả và chỉ trả `STUDENT`, như cũ. | SC-09 |
| BR-U05-60 | DTO `Person` của U05 có thêm trường `studentCode`, có thể rỗng. Trường này chỉ hiện ở những màn vốn đã hiện người đó: danh sách sinh viên của lớp cho nhân sự (Chủ nhiệm môn, giảng viên lớp, Admin), danh sách thành viên lớp cho sinh viên (SC-11), và kết quả tìm ứng viên. Quyền xem danh sách không đổi. | SC-10, SC-11 |

**Giao diện**:

- `EnrollmentPanel`: ô dán đổi chữ gợi ý thành "Mỗi dòng một email hoặc MSSV". Tệp mẫu Excel dùng header `email` và có thêm ví dụ MSSV. Cột kết quả hiện giá trị đã nhập. Câu báo cho `INVALID_IDENTIFIER` là "Không phải email hay MSSV hợp lệ".
- `PersonPicker`: dòng phụ của mỗi kết quả là `SE123456 · email`.
- Danh sách lớp phía nhân sự (`ManagedClassPage`) có cột "MSSV" đứng trước cột họ tên.
- Thành viên lớp phía sinh viên (`StudentClassPage`) hiện MSSV ở dòng phụ dưới tên.
