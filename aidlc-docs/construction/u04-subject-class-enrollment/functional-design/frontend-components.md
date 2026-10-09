# U04 Subject, Class, Enrollment & Learning Access - Frontend Components

**Bản tài liệu 2026-10-09**: UC 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Class Dashboard | `ClassDashboardPage` | 13, 31 | Student, Teacher, Subject Manager |
| Student Class Detail | `StudentClassPage` | 14 | Student |
| Teacher Class Detail | `TeacherClassPage` | 32 | Teacher, Subject Manager được giao dạy |
| Student Detail (popup) | `StudentDetailDialog` | 32 | Teacher, Subject Manager được giao dạy |
| Manager Dashboard | `ManagerDashboardPage` | 53 | Subject Manager |
| Subject Detail (Subject Manager) | `ManagedSubjectPage` | 53 | Subject Manager |
| Class List | `ClassListPage` | 47, 49 | Subject Manager |
| Class Detail (Subject Manager) | `ManagedClassPage` | 48, 50, 51, 52 | Subject Manager |
| Subject List | `SubjectListPage` | 64, 65 | Admin |
| Subject Detail (Admin) | `SubjectDetailPage` | 66, 67 | Admin |

## 2. Cây component

```
app/classes/                              ClassDashboardPage   màn Class Dashboard
app/classes/[id]/                         StudentClassPage     màn Student Class Detail
app/classes/[id]/teaching/                TeacherClassPage     màn Teacher Class Detail
  ClassInfoTab                            thông tin lớp chỉ đọc
  StudentsTab                             danh sách sinh viên ACTIVE; chỗ gắn ClassStudentsGroupsPanel (U12)
    StudentDetailDialog                   popup Student Detail (bấm một sinh viên)
app/manager/                              ManagerDashboardPage màn Manager Dashboard
app/manager/subjects/[id]/                ManagedSubjectPage   màn Subject Detail của Subject Manager
app/manager/classes/                      ClassListPage        màn Class List
  ClassFormDialog                         tạo lớp (UC 49)
app/manager/classes/[id]/                 ManagedClassPage     màn Class Detail của Subject Manager
  ClassInfoForm                           sửa name/description/term (UC 52)
  ClassStateActions                       Mở / Lưu trữ / Mở lại (UC 52)
  GradeDistributionToggle                 bật/tắt phân bố điểm (UC 52)
  AssignTeacherDialog                     gán/đổi giảng viên (UC 50)
  EnrollmentTable                         danh sách sinh viên, nút Gỡ (UC 51)
  AddStudentSearch                        thêm từng người (UC 51)
  AddStudentsListDialog                   thêm theo danh sách (UC 51)
  EnrollmentResultTable                   kết quả từng dòng
app/admin/subjects/                       SubjectListPage      màn Subject List
  SubjectFormDialog                       thêm môn, chọn Chủ nhiệm môn (UC 65)
app/admin/subjects/[id]/                  SubjectDetailPage    màn Subject Detail của Admin
  SubjectEditForm                         sửa môn, đổi Chủ nhiệm môn, lưu trữ/mở lại (UC 67)
  SubjectClassesTable                     lớp của môn, chỉ đọc (UC 66)
```

Route theo `RoleGuard` của U01: `/classes/*` cho Student, Teacher, Subject Manager; `/manager/*` cho Subject Manager; `/admin/*` cho Admin. Backend vẫn kiểm quan hệ thật (ghi danh, giảng viên chính, Chủ nhiệm môn).

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `ClassDashboardPage` | Student: "Đang học" / "Đã kết thúc" (UC 13). Teacher, Subject Manager: "Sắp mở" / "Đang dạy" / "Đã kết thúc" (UC 31). Có lối vào Student Assignments, Quiz Practice History (U11), Class Announcements (U05), My Credit Package và Public Credit Packages (U07) | `GET /api/v1/me/classes`, `GET /api/v1/classes?teacher=me` |
| `StudentClassPage` | Thông tin lớp, giảng viên, module và học liệu `ACTIVE` (gắn `ModuleList` chỉ xem + Learning Material của U05); lối vào Student Assignments (U11) và Class Announcements (U05) lọc sẵn lớp; phần Nhóm của tôi do U12 gắn vào (UC 14) | `GET /api/v1/me/classes/{id}` |
| `TeacherClassPage` | Tab Class Detail (chỉ đọc), tab Students (U04, nhóm của U12); tab Evals (U08: bài, quiz, bài nộp và sổ điểm của U15) và tab Materials (U05) gắn vào (UC 32) | `GET /api/v1/classes/{id}`, `GET /api/v1/classes/{id}/enrollments` |
| `StudentDetailDialog` | Popup chỉ đọc: email, tên hiển thị, ngày ghi danh; nhóm lấy từ dữ liệu nhóm của U12 đã tải ở tab Students | `GET /api/v1/classes/{id}/students/{accountId}` |
| `ManagerDashboardPage` | Danh sách môn được giao; lối vào Subject Detail, Class List, Material List, Question List, Quiz List, Assignment List; nút sang Class Dashboard nếu có dạy lớp (UC 53) | `GET /api/v1/subjects?manager=me` |
| `ManagedSubjectPage` | Thông tin môn, số lớp theo trạng thái (UC 53) | `GET /api/v1/subjects/{id}` |
| `ClassListPage` | Lớp của môn được giao, lọc môn/trạng thái/học kỳ, phân trang (UC 47); nút Tạo lớp | `GET /api/v1/classes?subjectId=` |
| `ClassFormDialog` | Tạo lớp trong môn mình quản lý; `code` kiểm định dạng phía client (UC 49) | `POST /api/v1/classes` |
| `ClassInfoForm` | Sửa name/description/term kèm `version`; `409` báo tải lại (UC 52) | `PATCH /api/v1/classes/{id}` |
| `ClassStateActions` | Nút Mở / Lưu trữ / Mở lại theo trạng thái; hộp xác nhận; hiện danh sách người vướng khi mở lại bị từ chối (UC 52) | `POST /api/v1/classes/{id}/state` |
| `GradeDistributionToggle` | Hiện trạng thái, xác nhận khi bật (UC 52) | `PATCH /api/v1/classes/{id}/grade-distribution` |
| `AssignTeacherDialog` | Tìm tài khoản `TEACHER`/`SUBJECT_MANAGER` `ACTIVE` (UC 50) | `PUT /api/v1/classes/{id}/teacher` |
| `EnrollmentTable` | Sinh viên `ACTIVE`/`REMOVED`, nút Gỡ (xác nhận), Thêm lại (UC 51) | `GET`, `DELETE`, `POST .../enrollments` |
| `AddStudentSearch` | Ô tìm theo email/tên, debounce 300 ms (UC 51) | `GET /api/v1/classes/{id}/student-candidates?q=` |
| `AddStudentsListDialog` | Dán email hoặc chọn CSV; đếm dòng, chặn > 200 (UC 51) | `POST /api/v1/classes/{id}/enrollments:bulk` |
| `EnrollmentResultTable` | Kết quả từng dòng với nhãn tiếng Việt | - |
| `SubjectListPage` | Bảng môn, tìm, lọc trạng thái, phân trang (UC 64) | `GET /api/v1/subjects` |
| `SubjectFormDialog` | Thêm môn, chọn Chủ nhiệm môn (tài khoản `SUBJECT_MANAGER` `ACTIVE`) (UC 65) | `POST /api/v1/subjects` |
| `SubjectDetailPage` | Thông tin môn, Chủ nhiệm môn, lớp của môn chỉ đọc (UC 66) | `GET /api/v1/subjects/{id}`, `GET /api/v1/classes?subjectId=` |
| `SubjectEditForm` | Sửa name/description, đổi Chủ nhiệm môn, lưu trữ/mở lại; `code` khóa (UC 67) | `PATCH /api/v1/subjects/{id}`, `PUT /api/v1/subjects/{id}/manager` |

Không còn popup Join Class và mã mời (bỏ 2026-10-09). Admin không có màn lớp riêng; Teacher chỉ xem thông tin lớp và danh sách sinh viên.
