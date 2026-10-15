# U05 Class & Enrollment — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U05 Class & Enrollment thuộc Iter 1, người code: Người 5; UC 20–30. Tách phần lớp và ghi danh từ U04 cũ; mã `BR-U05-xx` ứng với `BR-U04-xx` cũ cùng số. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Class Dashboard | `ClassDashboardPage` | 20, 22 | Student, Teacher, Subject Manager |
| Student Class Detail | `StudentClassPage` | 21 | Student |
| Teacher Class Detail | `TeacherClassPage` | 23 | Teacher, Subject Manager được giao dạy |
| Student Detail (popup) | `StudentDetailDialog` | 23 | Teacher, Subject Manager được giao dạy |
| Manager Dashboard | `ManagerDashboardPage` | 30 | Subject Manager |
| My Subject | `MySubjectPage` | 30 | Subject Manager |
| Class List | `ClassListPage`, popup `ClassFormDialog` | 24, 26 | Subject Manager |
| Class Detail | `ManagedClassPage` | 25, 27, 28, 29 | Subject Manager |

## 2. Cây component

```
app/classes/                    ClassDashboardPage   Class Dashboard; thanh bên lấy từ DashboardNavEntry
app/classes/[id]/               StudentClassPage     Student Class Detail; vùng StudentClassSection
app/teaching/classes/[id]/      TeacherClassPage     Teacher Class Detail
  ClassInfoTab                                       thông tin lớp chỉ đọc
  StudentsTab                                        sinh viên ACTIVE; vùng TeacherClassTab của U12
    StudentDetailDialog                              popup Student Detail; vùng StudentDetailSection
  (tab Materials, Evals từ TeacherClassTab)
app/manager/                    ManagerDashboardPage Manager Dashboard; lối vào từ DashboardNavEntry
app/manager/subjects/[id]/      MySubjectPage        My Subject
app/manager/classes/            ClassListPage        Class List
  ClassFormDialog                                    popup Tạo lớp (UC 26)
app/manager/classes/[id]/       ManagedClassPage     Class Detail
  ClassInfoForm                                      sửa tên, mô tả, học kỳ (UC 29)
  ClassStateActions                                  Mở / Lưu trữ / Mở lại (UC 29)
  GradeDistributionToggle                            phân bố điểm (UC 29)
  AssignTeacherDialog                                popup Phân công (UC 27)
  EnrollmentTable                                    sinh viên, nút Gỡ / Thêm lại (UC 28)
  AddStudentSearch                                   thêm từng người (UC 28)
  AddStudentsListDialog                              thêm theo danh sách (UC 28)
  EnrollmentResultTable                              kết quả từng dòng
shared/class-slots/                                  registry StudentClassSection, TeacherClassTab,
                                                     StudentDetailSection, DashboardNavEntry
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `ClassDashboardPage` | `GET /api/v1/me/classes` hoặc `GET /api/v1/teaching/classes` | Student: "Đang học" / "Đã kết thúc". Teacher, Subject Manager: "Sắp mở" / "Đang dạy" / "Đã kết thúc" |
| `StudentClassPage` | `GET /api/v1/me/classes/{id}` | Thông tin lớp, giảng viên; các phần do unit khác đăng ký |
| `TeacherClassPage`, `StudentsTab` | `GET /api/v1/teaching/classes/{id}`, `.../students` | Tab do U05 và tab do unit khác đăng ký |
| `StudentDetailDialog` | `GET .../students/{accountId}` | Chỉ đọc |
| `ManagerDashboardPage`, `MySubjectPage` | `GET /api/v1/manager/subjects`, `.../{id}` | Thẻ môn, số lớp theo trạng thái |
| `ClassListPage`, `ClassFormDialog` | `GET`, `POST /api/v1/manager/classes` | Lọc môn, trạng thái, học kỳ; mã lớp kiểm phía client |
| `AssignTeacherDialog` | `GET /api/v1/manager/teacher-candidates`, `PUT .../teacher` | Tìm `TEACHER`, `SUBJECT_MANAGER` đang `ACTIVE` |
| `AddStudentSearch` | `GET .../student-candidates?q=` | Debounce 300 ms |
| `AddStudentsListDialog` | `POST .../enrollments:batch` | Chặn quá 200 dòng; hiện `EnrollmentResultTable` |
| `EnrollmentTable` | `PUT .../enrollments/{accountId}/status` | Gỡ có xác nhận; Thêm lại |
| `ClassInfoForm`, `GradeDistributionToggle`, `ClassStateActions` | `PATCH`, `PUT .../grade-distribution`, `PUT .../status` | `409` báo tải lại; mở lại bị từ chối thì hiện người vướng |

Route theo `RoleGuard` của U01: `/classes/*` cho Student; `/teaching/*` cho Teacher, Subject Manager; `/manager/*` cho Subject Manager. Admin không có route lớp. Backend vẫn kiểm quan hệ thật.
