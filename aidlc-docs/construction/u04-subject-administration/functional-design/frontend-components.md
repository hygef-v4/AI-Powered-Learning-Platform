# U04 Subject Administration — frontend-components

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

**Quyết định 2026-11-07 — D-U04-04:** Bỏ học kỳ lớp khỏi UI, request/response, domain và dữ liệu hiện hành; xem [quyết định đã duyệt](../../../inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md). Migration mới `V20261107_2200__remove_class_term.sql` xóa cột cũ khi áp dụng; không sửa migration hoặc bằng chứng lịch sử. Các quy tắc khác giữ nguyên phạm vi đã chốt.

**Yêu cầu đã chốt 2026-11-08:** Áp dụng các [quyết định U04/U13/U14](../../../inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md). Đây là baseline yêu cầu để thiết kế test; không phải xác nhận code/runtime hoặc coverage đã đầy đủ. Các snapshot và số kiểm thử có ngày cũ là lịch sử.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 15 | Subject List (`a05`), popup Thêm môn học (`a06`) | `SubjectListPage`, `SubjectFormDialog` | 16, 17 | Admin |
| 16 | Subject Detail: tab Thông tin (`a07`), tab Lớp học (`a07b`), popup Đổi / bỏ chủ nhiệm (`a07c`) | `SubjectDetailPage`, `ManagerChangeDialog` | 18, 19 | Admin |

Subject List mở từ mục "Môn học" trên menu bên của Admin (U04 đăng ký `NavEntry`). Subject Detail của Subject Manager (My Subject) và Manager Dashboard (UC 30) thuộc U05.

## 2. Cây component

```
app/admin/subjects/                SubjectListPage     #15 Subject List
  SubjectFilters                                       tab Đang hoạt động / Đã lưu trữ (kèm số), ô tìm mã hoặc tên
  SubjectTable                                         mã, tên, Chủ nhiệm môn, trạng thái
  SubjectFormDialog                                    popup Thêm môn học (UC 17)
    ManagerPicker                                      tìm Subject Manager đang ACTIVE
app/admin/subjects/[id]/           SubjectDetailPage   #16 Subject Detail của Admin
  SubjectStatusActions                                 nút Lưu trữ môn / Mở lại môn
  SubjectInfoTab
    SubjectEditForm                                    mã chỉ đọc; sửa tên, mô tả (UC 19)
    ManagerCard                                        Chủ nhiệm môn hiện tại, nút Đổi chủ nhiệm
    SubjectMetaCard                                    trạng thái, ngày tạo
  SubjectClassesTab
    SubjectClassesTable                                lớp của môn, chỉ đọc (UC 18)
  ManagerChangeDialog                                  popup Đổi / bỏ chủ nhiệm (a07c)
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `SubjectListPage` | `GET /api/v1/admin/subjects` | "Môn học" kèm tổng số; nút Thêm môn học; tab Đang hoạt động, Đã lưu trữ (kèm số); ô "Tìm theo mã hoặc tên môn"; cột Mã môn, Tên môn, Chủ nhiệm môn ("Chưa phân công" khi trống), Trạng thái, nút mở chi tiết; phân trang "Hiển thị a – b trong N môn"; không có cột số lớp |
| `SubjectFormDialog` | `POST /api/v1/admin/subjects` | Mã môn* (tự viết hoa; chỉ chữ hoa, số, "-"; tối đa 20 ký tự; không đổi sau khi tạo), Tên môn*, Chủ nhiệm môn (tùy chọn, mặc định "Chưa phân công", gợi ý "Chỉ hiện người có vai trò Chủ nhiệm môn đang hoạt động. Có thể phân công sau."), Mô tả (tùy chọn); đóng bằng X/Escape, không có nút Hủy riêng; Thêm môn học; lỗi trùng mã hiện dưới ô mã |
| `ManagerPicker` | `GET /api/v1/admin/accounts?role=SUBJECT_MANAGER&status=ACTIVE&q=` (U01) | Tìm theo tên hoặc email, tối đa 20 kết quả; có lựa chọn bỏ trống |
| `SubjectDetailPage` | `GET /api/v1/admin/subjects/{id}` | Nút quay lại; tên môn kèm nhãn trạng thái; đường dẫn "Môn học › mã môn"; nút Lưu trữ môn hoặc Mở lại môn; tab Thông tin, Lớp học (kèm tổng số lớp mọi trạng thái; hiển thị 0, ẩn số nếu không khả dụng) |
| `SubjectEditForm` | `PATCH /api/v1/admin/subjects/{id}` | Mã môn chỉ đọc ("Không đổi được"), Tên môn học*, Mô tả (tùy chọn); Hủy, Lưu thay đổi; `409` báo dữ liệu đã đổi và tải lại |
| `ManagerCard`, `ManagerChangeDialog` | `PUT /api/v1/admin/subjects/{id}/manager` | Thẻ "Chủ nhiệm môn": avatar, họ tên, email, nút Đổi chủ nhiệm. Popup: Chủ nhiệm hiện tại; chọn "Phân công người khác" (dùng `ManagerPicker`) hoặc "Bỏ chủ nhiệm" (môn chưa có chủ nhiệm thì chỉ Admin quản lý cho tới khi phân công lại); Hủy, Xác nhận |
| `SubjectMetaCard` | - | Trạng thái, ngày tạo, nút Lưu trữ/Mở lại; không thêm bộ đếm lớp vào thẻ |
| `SubjectStatusActions` | `PUT /api/v1/admin/subjects/{id}/status` | Lưu trữ, Mở lại có xác nhận. Popup lưu trữ báo mọi lớp chưa lưu trữ của môn sẽ được lưu trữ theo và Chủ nhiệm môn bị gỡ (BR-U04-04, BR-U04-11). Design `a07d` (lưu trữ bị từ chối) không còn dùng |
| `SubjectClassesTable` | `GET /api/v1/admin/subjects/{id}/classes` | Lọc trạng thái (Tất cả, Nháp, Đang mở, Lưu trữ); cột Mã lớp, Giảng viên ("Chưa phân công"), Trạng thái; ghi chú "Chỉ xem. Lớp do Chủ nhiệm môn quản lý."; không có link mở lớp |

Route dưới `/admin` nên `RoleGuard` của U01 chỉ cho `ADMIN`; API vẫn tự kiểm quyền.
