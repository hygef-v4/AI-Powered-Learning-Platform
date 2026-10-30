# U05 Class & Enrollment — frontend-components

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 17 | Manager Dashboard (`m11`) | `ManagerDashboardPage` | 30 | Subject Manager |
| 18 | My Subject (`m01`) | `MySubjectPage` | 30 | Subject Manager |
| 28 | Class List (`m03`), popup Tạo lớp (`m04`) | `ClassListPage`, `ClassFormDialog` | 24, 26 | Subject Manager |
| 29 | Class Detail: tab Thông tin (`m05`), tab Sinh viên (`m05b`), popup Gỡ sinh viên (`m05d`), Phân công giảng viên (`m06`), Mở lại bị từ chối (`m05c`) | `ManagedClassPage` và các popup | 25, 27, 28, 29 | Subject Manager |
| 30 | Class Dashboard: Giảng viên (`t01`), Sinh viên (`s01`) | `ClassDashboardPage` | 20, 22 | Student, Teacher, Subject Manager |
| 32 | Teacher Class Detail: khung, tab Thông tin lớp (`t18`), danh sách sinh viên của tab Sinh viên (`t02c`) | `TeacherClassPage`, `ClassInfoTab`, `StudentsTab` | 23 | Teacher, Subject Manager được giao dạy |
| 33 | Student Detail, popup (`t19`) | `StudentDetailDialog` | 23 | Teacher, Subject Manager được giao dạy |
| 36 | Student Class Detail (`s02`) | `StudentClassPage` | 21 | Student |

U05 đăng ký `NavEntry` cho các mục menu bên: Tổng quan, Môn học của tôi, Lớp học (Subject Manager); Lớp được phân công (Teacher, Subject Manager); Lớp của tôi (Student).

## 2. Cây component

```
app/manager/                    ManagerDashboardPage #17 Manager Dashboard
  TeachingNotice                                     "Bạn đang dạy N lớp…", nút Mở Lớp được phân công
  SubjectCard                                        mã, tên, số lớp theo trạng thái, vùng SubjectSummarySection
app/manager/subjects/[id]/      MySubjectPage        #18 My Subject; /manager/subjects mở môn đầu tiên
  SubjectSwitcher                                    nút chọn môn (dùng chung cho các màn /manager/*)
  SubjectInfoCard, ClassCountTable                   (vùng SubjectSummarySection: thẻ số liệu)
app/manager/classes/            ClassListPage        #28 Class List
  ClassFormDialog                                    popup Tạo lớp (UC 26)
app/manager/classes/[id]/       ManagedClassPage     #29 Class Detail
  ClassStateActions                                  Mở lớp / Lưu trữ lớp / Mở lại lớp (UC 29)
  ReopenRejectedDialog                               popup Mở lại bị từ chối
  ClassInfoForm                                      tab Thông tin: sửa học kỳ, tên, mô tả (UC 29)
  TeacherCard, AssignTeacherDialog                   thẻ Giảng viên, popup Phân công (UC 27)
  GradeDistributionToggle                            thẻ Quyền xem (UC 29)
  StudentsTabManaged                                 tab Sinh viên (UC 28)
    AddStudentsPanel                                 Thêm từng người / Thêm theo danh sách, kết quả từng dòng
    EnrollmentTable                                  Đang học / Đã gỡ; Gỡ, Thêm lại
    RemoveStudentDialog                              popup Gỡ sinh viên
app/classes/                    ClassDashboardPage   #30 Class Dashboard
  ClassCard                                          vùng ClassCardSection
app/classes/[id]/               StudentClassPage     #36 Student Class Detail; vùng StudentClassSection
app/teaching/classes/[id]/      TeacherClassPage     #32 Teacher Class Detail: tab Thông tin lớp, Sinh viên, Học liệu, Đánh giá, Sổ điểm
  ClassInfoTab                                       thông tin lớp chỉ đọc; vùng TeacherClassTab "info"
  StudentsTab                                        sinh viên ACTIVE; vùng TeacherClassTab "students" (U12)
    StudentDetailDialog                              #33 popup; vùng StudentDetailSection
  (tab Học liệu, Đánh giá, Sổ điểm từ TeacherClassTab)
features/academics/slots.ts                          StudentClassSection, TeacherClassTab, StudentDetailSection,
                                                     SubjectSummarySection, ClassCardSection
```

## 3. Component và API

### Subject Manager

| Component | API | Hành vi |
|---|---|---|
| `ManagerDashboardPage` | `GET /api/v1/manager/subjects` | "Tổng quan", "Các môn bạn phụ trách". Khi đang dạy lớp: hộp "Bạn đang dạy N lớp: … Bài nộp và chấm điểm của lớp nằm ở mục Lớp được phân công." kèm nút mở Class Dashboard. Thẻ môn: mã, tên, số lớp Nháp / Đang mở / Lưu trữ, dòng số học liệu, câu hỏi, Quiz, bài tập (U13, U10, U07 gắn qua `SubjectSummarySection`), link "Truy cập quản lý môn" sang My Subject |
| `MySubjectPage` | `GET /api/v1/manager/subjects/{id}` | Nút chọn môn; "Thông tin môn": mã, tên, trạng thái, mô tả, ghi chú "Tên và mô tả môn do Quản trị viên quản lý."; "Lớp của môn": bảng Trạng thái lớp, Số lớp kèm link Mở danh sách lớp; thẻ số liệu Học liệu, Câu hỏi trắc nghiệm, Quiz, Bài tập của môn (qua `SubjectSummarySection`) |
| `ClassListPage` | `GET /api/v1/manager/classes` | "Lớp của môn", nút chọn môn, Tạo lớp; tab Tất cả, Nháp, Đang mở, Lưu trữ (kèm số); lọc học kỳ; ô "Tìm mã lớp, tên, giảng viên"; cột Mã lớp, Tên lớp, Học kỳ, Giảng viên ("(bạn)" khi là mình, "Chưa phân công"), Sinh viên, Trạng thái; bấm dòng mở Class Detail |
| `ClassFormDialog` | `POST /api/v1/manager/classes` | Môn đang chọn; Mã lớp* (duy nhất trong môn, không đổi sau khi tạo; kiểm phía client), Học kỳ*, Tên lớp*, Mô tả (tùy chọn), Giảng viên (tùy chọn; bắt buộc trước khi mở lớp; chỉ Giảng viên và Chủ nhiệm môn đang hoạt động); ghi chú "Lớp mới ở trạng thái Nháp: sinh viên chưa thấy lớp cho tới khi bạn bấm Mở lớp."; Hủy, Tạo lớp |
| `ManagedClassPage` | `GET /api/v1/manager/classes/{id}` | Nút quay lại; "mã môn · mã lớp" kèm nhãn trạng thái; đường dẫn "Lớp của môn › …"; nút Phân công giảng viên và Mở lớp (Nháp), Lưu trữ lớp (Đang mở) hoặc Mở lại lớp (Lưu trữ); tab Thông tin, Sinh viên (kèm số) |
| `ClassInfoForm` | `PATCH /api/v1/manager/classes/{id}` | Mã lớp (chỉ đọc), Học kỳ*, Tên lớp*, Mô tả (tùy chọn); Sinh viên "n đang học · n đã gỡ"; Ngày tạo; Hủy, Lưu thay đổi; `409` báo tải lại |
| `TeacherCard`, `AssignTeacherDialog` | `GET /api/v1/manager/teacher-candidates`, `PUT .../teacher` | Thẻ "Giảng viên": avatar, tên, email, nút Đổi giảng viên. Popup "Phân công giảng viên": ô "Tìm theo tên hoặc email"; danh sách chọn một (avatar, tên, email, vai trò; người đang dạy ghi "hiện tại"); "Chỉ hiện người có vai trò Giảng viên hoặc Chủ nhiệm môn đang hoạt động."; Hủy, Phân công |
| `GradeDistributionToggle` | `PUT .../grade-distribution` | Thẻ "Quyền xem": công tắc "Cho sinh viên xem phân bố điểm", ghi chú "Sinh viên thấy biểu đồ phân bố điểm của bài đã công bố, không thấy điểm người khác." |
| `ClassStateActions`, `ReopenRejectedDialog` | `PUT .../status` | Mở, lưu trữ, mở lại có xác nhận; `409` báo tải lại. Mở lại bị từ chối: popup "Không mở lại được lớp" (mã lớp · học kỳ), "Một số người trong lớp không còn hoạt động. Hãy đổi giảng viên hoặc gỡ sinh viên bị vô hiệu hóa rồi thử lại.", bảng Người, Vai trò trong lớp, Lý do; Đóng |
| `AddStudentsPanel` | `GET .../student-candidates?q=`, `POST .../enrollments:batch` | Thẻ "Thêm sinh viên", hai cách: Thêm từng người (tìm theo tên hoặc email, debounce 300 ms) và Thêm theo danh sách (dán email, mỗi dòng một, tối đa 200 dòng); bảng kết quả từng dòng Email, Kết quả (Đã thêm, Đã có trong lớp, Tài khoản vô hiệu hóa, Không phải sinh viên…); nút Thêm vào lớp |
| `EnrollmentTable`, `RemoveStudentDialog` | `PUT .../enrollments/{accountId}/status` | Ô tìm tên hoặc email; tab Đang học, Đã gỡ (kèm số); cột Sinh viên (avatar, tên, email), Trạng thái, Ngày vào lớp, nút Gỡ hoặc Thêm lại; phân trang. Popup "Gỡ sinh viên khỏi lớp?": "X sẽ không còn thấy lớp. Bài đã nộp và điểm vẫn được giữ; bạn có thể Thêm lại sau."; Hủy, Gỡ khỏi lớp |

### Class Dashboard và Class Detail

| Component | API | Hành vi |
|---|---|---|
| `ClassDashboardPage` (Student) | `GET /api/v1/me/classes` | "Lớp của tôi"; tab Đang học, Đã kết thúc; thẻ lớp: mã môn, tên môn, "GV tên", vùng `ClassCardSection` (U08: số bài sắp đến hạn hoặc đang mở, "Chưa có bài"); bấm thẻ mở Student Class Detail |
| `ClassDashboardPage` (Teacher, Subject Manager) | `GET /api/v1/teaching/classes` | "Lớp được phân công"; tab Sắp mở, Đang dạy, Đã kết thúc (kèm số); thẻ lớp: mã môn, tên môn, "Lớp mã · Học kỳ", số sinh viên; bấm thẻ mở Teacher Class Detail |
| `StudentClassPage` | `GET /api/v1/me/classes/{id}` | Nút quay lại; tên môn; đường dẫn "Lớp của tôi › mã môn · mã lớp · GV"; các phần do unit khác gắn qua `StudentClassSection`, theo thứ tự: Học liệu (U13), Bài tập sắp đến hạn (U08), Thông báo lớp (U14), Nhóm của tôi (U12) |
| `TeacherClassPage` | `GET /api/v1/teaching/classes/{id}` | Nút quay lại; tên môn kèm nhãn trạng thái; đường dẫn "Lớp được phân công › mã môn · mã lớp"; tab Thông tin lớp, Sinh viên (kèm số), Học liệu (U13), Đánh giá (U07, U10), Sổ điểm (U09) |
| `ClassInfoTab` | - | Thẻ "Thông tin lớp": Môn, Mã lớp, Học kỳ, Giảng viên, Trạng thái, Mô tả; thẻ số Sinh viên đang học; phần do unit khác gắn qua `TeacherClassTab` "info": Nhóm và số sinh viên chưa có nhóm (U12), Bài đang mở, Chờ chấm, Tiến độ nộp bài, Phân bố điểm (U09) |
| `StudentsTab` | `GET /api/v1/teaching/classes/{id}/students` | Ô tìm theo tên hoặc email; bảng #, Họ tên (avatar), Email, Nhóm (U12); phân trang. Lớp đã chia nhóm thì phần nhóm của U12 hiện thay bảng |
| `StudentDetailDialog` | `GET .../students/{accountId}` | Chỉ đọc: avatar, họ tên, "Sinh viên · mã môn · mã lớp", email; phần do unit khác gắn qua `StudentDetailSection`: Nhóm (U12), số bài đã nộp và nộp trễ, bảng Bài nộp trong lớp (U09); nút Đóng |

Route theo `RoleGuard` của U01: `/classes/*` cho Student; `/teaching/*` cho Teacher, Subject Manager; `/manager/*` cho Subject Manager. Admin không có route lớp. Backend vẫn kiểm quan hệ thật.

### Slot U05 khai báo cho frontend

| Slot | Unit gắn | Dùng để |
|---|---|---|
| `StudentClassSection` | U13 (Học liệu), U08 (Bài tập sắp đến hạn), U14 (Thông báo lớp), U12 (Nhóm của tôi) | Phần nội dung trên Student Class Detail |
| `TeacherClassTab` | "info": U12, U09; "students": U12; "materials": U13; "evals": U07, U10 (U09 gắn cột Bài nộp qua slot của U07); "gradebook": U09 | Tab và phần trên Teacher Class Detail |
| `StudentDetailSection` | U12 (nhóm), U09 (bài nộp, điểm trong lớp) | Phần trong popup Student Detail |
| `SubjectSummarySection` | U13 (học liệu), U10 (câu hỏi, Quiz), U07 (bài tập của môn) | Số liệu theo môn trên Manager Dashboard và My Subject |
| `ClassCardSection` | U08 (bài sắp đến hạn của Student) | Dòng tóm tắt trên thẻ lớp ở Class Dashboard |

## Source behavior delta — 2026-10-30

CourseClass.create tạo DRAFT. OPEN không bắt buộc teacher: domain, AcademicsService và migration 27/10 đã bỏ guard. Nếu có teacher thì account vẫn phải ACTIVE và đúng role. Scope teaching dựa teacher_id hiện hành, manager dựa subject ownership, Student dựa enrollment/status; không tự cấp teaching cho manager chưa được giao.

[Source/API/schema/test inventory](../code/as-built-baseline.md). Các rule và test cũ trái delta này là thiết kế trước baseline; không tiếp tục áp điều kiện cũ vào AS-BUILT.
