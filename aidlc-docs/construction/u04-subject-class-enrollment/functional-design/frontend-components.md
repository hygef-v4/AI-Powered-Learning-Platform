# U04 Subject, Class, Enrollment & Learning Access - Frontend Components

**Bản tài liệu 2026-10-08**: UC 12, 13, 27, 28, 45, 46, 47, 48, 49, 50, 63, 64, 65, 66; primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-CAT-005, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
app/admin/subjects/          SubjectListPage (màn Subject List), SubjectFormDialog, AssignManagerDialog
app/admin/subjects/[id]/     SubjectDetailPage (Admin: thông tin môn, Chủ nhiệm môn, trạng thái)
app/teaching/subjects/[id]/  SubjectHubPage (Subject Detail của Chủ nhiệm môn: phần Học liệu gắn ModuleList U05 chế độ môn (module và học liệu của môn); liên kết Template U10, Lớp U04)
app/teaching/                TeacherMenuPage (Assigned Classes, đích sau đăng nhập của Teacher/Subject Manager: lớp đang dạy, chọn một lớp mở thẳng Class Detail; lối vào Class Question Bank U06; Subject Manager thấy thêm lối vào điều hướng môn được phân công)
app/teaching/subjects/       SubjectManagerMenuPage (điều hướng môn được phân công: môn được phân công, chọn một môn mở Subject Detail)
app/admin/classes/           ClassListPage, ClassDetailPage (Class List, Class Detail cấu trúc của Admin, mở từ Admin Sidebar; dùng chung component với app/teaching/classes/)
app/teaching/classes/        ClassListPage, ClassFormDialog (ADMIN hoặc Chủ nhiệm môn của môn tạo), ClassDetailPage
app/teaching/subjects/[id]/classes/  ClassListPage lọc theo môn, mở từ Subject Detail của Chủ nhiệm môn; cùng ClassDetailPage
  ClassDetailPage
    ClassInfoTab             sửa name/description/term, ClassStateActions
    MaterialsTab             ModuleList của U05 chế độ lớp: module của môn chỉ đọc; giảng viên tải học liệu của lớp bằng nút của từng module
    EnrollmentTab            EnrollmentTable, AddStudentSearch, AddStudentsListDialog, EnrollmentResultTable; nhúng ClassStudentsGroupsPanel, LeaderRequestsPanel của U12
    InviteCodeTab            InviteCodePanel
    GradeDistributionToggle  người quản lý lớp bật hoặc tắt phân bố điểm ẩn danh; mặc định tắt
    AssignTeacherDialog   (ADMIN hoặc Chủ nhiệm môn của môn)
    ClassNavLinks         lối vào Class Announcements (U05), Assignment List (U08), Gradebook (U15) của lớp
app/learning/                   MyClassesPage (My Classes), StudentClassPage (Class Detail của Student; gắn ModuleList (chỉ xem) + ViewLearningMaterialDialog U05 (Learning Material); điểm xem trên danh sách bài theo loại của U11 (UC 17)), JoinByCodeDialog (popup Join Class trên My Classes)
app/learning/classes/[id]/group/  MyGroupPage (màn My Group: khung trang trong lớp, nội dung là MyGroupPanel của U12)
```

| Component | Hành vi | API |
|---|---|---|
| `SubjectListPage` | Bảng môn, tìm, lọc trạng thái, phân trang | `GET /api/v1/subjects` |
| `SubjectDetailPage` | Admin xem/sửa môn, gán Chủ nhiệm môn, lưu trữ/mở lại | `GET`, `PATCH /api/v1/subjects/{id}` |
| `SubjectHubPage` | Chủ nhiệm môn chỉ mở môn được phân công; phần Học liệu (module, học liệu của môn), lối vào Template, Subject Classes | `GET /api/v1/subjects/{id}` |
| `SubjectFormDialog` | Tạo/sửa; `code` khóa khi sửa; kiểm định dạng phía client | `POST`, `PATCH /api/v1/subjects/{id}` |
| `AssignManagerDialog` | Tìm tài khoản ACTIVE role SUBJECT_MANAGER hoặc ADMIN | `PUT /api/v1/subjects/{id}/manager` |
| `ClassListPage` | Danh sách lớp theo phạm vi, lọc môn/trạng thái/học kỳ; Chủ nhiệm môn mở theo môn từ Subject Detail | `GET /api/v1/classes?subjectId=` |
| `ClassStateActions` | Nút Mở / Lưu trữ / Mở lại theo trạng thái; hộp xác nhận; hiện danh sách người vướng khi mở lại bị từ chối | `POST /api/v1/classes/{id}/state` |
| `AddStudentSearch` | Ô tìm theo email/tên, debounce 300 ms | `GET /api/v1/classes/{id}/student-candidates?q=` |
| `AddStudentsListDialog` | Dán email hoặc chọn CSV; đếm dòng, chặn > 200 | `POST /api/v1/classes/{id}/enrollments:bulk` |
| `EnrollmentResultTable` | Kết quả từng dòng với nhãn tiếng Việt | - |
| `EnrollmentTable` | Người học `ACTIVE`/`REMOVED`, nút Gỡ (xác nhận), Ghi danh lại | `GET`, `DELETE`, `POST .../enrollments` |
| `InviteCodePanel` | Hiện mã, hạn, bật/tắt, đổi mã, sao chép | `PUT /api/v1/classes/{id}/invite` |
| `GradeDistributionToggle` | Hiện trạng thái và xác nhận khi bật; chỉ người quản lý lớp thao tác | `PATCH /api/v1/classes/{id}/grade-distribution` |
| `MyClassesPage` | Hai mục "Đang học" / "Đã kết thúc"; nút "Tham gia bằng mã" | `GET /api/v1/me/classes` |
| `JoinByCodeDialog` | Ô 8 ký tự, tự viết hoa; lỗi chung | `POST /api/v1/me/classes:join` |
| `TeacherMenuPage` | Lớp đang dạy (giảng viên chính); Subject Manager thấy thêm nút điều hướng môn được phân công | `GET /api/v1/classes?teacher=me` |
| `SubjectManagerMenuPage` | Môn được phân công làm Chủ nhiệm môn | `GET /api/v1/subjects?manager=me` |
| `StudentClassPage` | Thông tin lớp, giảng viên, module và học liệu `ACTIVE`; lối vào danh sách bài theo loại (U11, UC 17), Class Announcements (U05), My Group | `GET /api/v1/me/classes/{id}` |

## Hành động trên Class Detail
ClassInfoTab/ClassStateActions/AssignTeacherDialog chỉ quyền cấu trúc (Admin Full hoặc Subject Manager đúng môn). Teacher/vai trò kế thừa được giao dạy chỉ thao tác roster/mã mời/phân bố và giảng dạy được cấp. Gradebook, bank, submissions/học liệu lớp kiểm R3/R4 dù đã mở được cấu trúc lớp.
