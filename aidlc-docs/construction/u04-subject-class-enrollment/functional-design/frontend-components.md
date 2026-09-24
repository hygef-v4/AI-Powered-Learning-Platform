# U04 Subject, Class, Enrollment & Learning Access - Frontend Components

```
app/admin/subjects/          SubjectListPage, SubjectFormDialog, AssignManagerDialog
app/teaching/classes/        ClassListPage, ClassFormDialog (ADMIN tạo), ClassDetailPage
  ClassDetailPage
    ClassInfoTab             sửa name/description/term, ClassStateActions
    EnrollmentTab            EnrollmentTable, AddLearnerSearch, AddLearnersListDialog, EnrollmentResultTable
    InviteCodeTab            InviteCodePanel
    AssignInstructorDialog   (chỉ ADMIN)
app/learn/                   MyClassesPage, LearnerClassPage, JoinByCodeDialog
```

| Component | Hành vi | API |
|---|---|---|
| `SubjectListPage` | Bảng môn, tìm, lọc trạng thái, phân trang | `GET /api/v1/subjects` |
| `SubjectFormDialog` | Tạo/sửa; `code` khóa khi sửa; kiểm định dạng phía client | `POST`, `PATCH /api/v1/subjects/{id}` |
| `AssignManagerDialog` | Tìm tài khoản role `SUBJECT_MANAGER` | `PUT /api/v1/subjects/{id}/manager` |
| `ClassListPage` | Danh sách lớp theo phạm vi, lọc môn/trạng thái/học kỳ | `GET /api/v1/classes` |
| `ClassStateActions` | Nút Mở / Lưu trữ / Mở lại theo trạng thái; hộp xác nhận; hiện danh sách người vướng khi mở lại bị từ chối | `POST /api/v1/classes/{id}/state` |
| `AddLearnerSearch` | Ô tìm theo email/tên, debounce 300 ms | `GET /api/v1/classes/{id}/learner-candidates?q=` |
| `AddLearnersListDialog` | Dán email hoặc chọn CSV; đếm dòng, chặn > 200 | `POST /api/v1/classes/{id}/enrollments:bulk` |
| `EnrollmentResultTable` | Kết quả từng dòng với nhãn tiếng Việt | - |
| `EnrollmentTable` | Người học `ACTIVE`/`REMOVED`, nút Gỡ (xác nhận), Ghi danh lại | `GET`, `DELETE`, `POST .../enrollments` |
| `InviteCodePanel` | Hiện mã, hạn, bật/tắt, đổi mã, sao chép | `PUT /api/v1/classes/{id}/invite` |
| `MyClassesPage` | Hai mục "Đang học" / "Đã kết thúc"; nút "Tham gia bằng mã" | `GET /api/v1/me/classes` |
| `JoinByCodeDialog` | Ô 8 ký tự, tự viết hoa; lỗi chung | `POST /api/v1/me/classes:join` |
| `LearnerClassPage` | Thông tin lớp và nội dung đã phát hành; chỗ trống cho assignment (U08) | `GET /api/v1/me/classes/{id}` |
