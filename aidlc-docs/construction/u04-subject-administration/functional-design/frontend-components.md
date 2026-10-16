# U04 Subject Administration — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U04 Subject Administration thuộc Iter 1, người code: Người 4; UC 16–19. Tách phần môn học từ U04 cũ (Subject, Class & Enrollment); phần lớp và ghi danh thuộc U05. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Subject List | `SubjectListPage`, popup `SubjectFormDialog` | 16, 17 | Admin |
| Subject Detail (Admin) | `SubjectDetailPage` | 18, 19 | Admin |

Subject Detail của Subject Manager và Manager Dashboard (UC 30) thuộc U05.

## 2. Cây component

```
app/admin/subjects/                SubjectListPage     màn Subject List
  SubjectFilters                                       tìm mã/tên, tab Đang hoạt động / Đã lưu trữ
  SubjectTable                                         mã, tên, Chủ nhiệm môn, số lớp, trạng thái
  SubjectFormDialog                                    popup Thêm môn học (UC 17)
    ManagerPicker                                      tìm Subject Manager đang ACTIVE
app/admin/subjects/[id]/           SubjectDetailPage   màn Subject Detail của Admin
  SubjectInfoTab
    SubjectEditForm                                    sửa tên, mô tả (UC 19)
    ManagerCard                                        Chủ nhiệm môn hiện tại, nút Đổi chủ nhiệm
    SubjectStatusActions                               Lưu trữ / Mở lại
  SubjectClassesTab
    SubjectClassesTable                                lớp của môn, chỉ đọc (UC 18)
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `SubjectListPage` | `GET /api/v1/admin/subjects` | Tìm, lọc, phân trang; số lớp "không khả dụng" khi U05 chưa trả lời |
| `SubjectFormDialog` | `POST /api/v1/admin/subjects` | Mã tự viết hoa, kiểm định dạng phía client; lỗi trùng mã hiện dưới ô mã |
| `ManagerPicker` | `GET /api/v1/admin/accounts?role=SUBJECT_MANAGER&status=ACTIVE&q=` (U01) | Tối đa 20 kết quả; có lựa chọn bỏ trống |
| `SubjectEditForm` | `PATCH /api/v1/admin/subjects/{id}` | Mã khóa, không sửa; `409` báo dữ liệu đã đổi và tải lại |
| `ManagerCard` | `PUT /api/v1/admin/subjects/{id}/manager` | Đổi hoặc bỏ Chủ nhiệm môn, hỏi xác nhận |
| `SubjectStatusActions` | `PUT /api/v1/admin/subjects/{id}/status` | Lưu trữ bị từ chối thì hiện danh sách lớp chưa lưu trữ |
| `SubjectClassesTable` | `GET /api/v1/admin/subjects/{id}/classes` | Chỉ đọc, không có link mở lớp |

Route dưới `/admin` nên `RoleGuard` của U01 chỉ cho `ADMIN`; API vẫn tự kiểm quyền.
