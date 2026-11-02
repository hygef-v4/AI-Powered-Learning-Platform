# U04 Subject Administration — business-logic-model

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Xem danh sách môn | UC 16 | Subject List |
| F2 Thêm môn | UC 17 | Subject List (popup Thêm môn học) |
| F3 Xem chi tiết môn | UC 18 | Subject Detail (Admin) |
| F4 Sửa thông tin môn | UC 19 | Subject Detail (Admin) |
| F5 Đổi Chủ nhiệm môn | UC 17, 19 | Subject List (popup), Subject Detail |
| F6 Lưu trữ và mở lại môn | UC 19 | Subject Detail |
| F7 Trả phạm vi môn cho phân quyền | Mọi UC cần R2 | - |
| F8 Cung cấp thông tin môn cho unit khác | UC 26, 30 của U05; Admin Dashboard (UC 66) | - |

## 2. Luồng

### F1 - Xem danh sách môn (UC 16)
1. Admin mở Subject List từ Admin Dashboard; frontend gọi `GET /api/v1/admin/subjects` kèm từ khóa, trạng thái, trang.
2. Kiểm quyền `ADMIN` qua `IAuthorization`; không phải thì `403` và audit (BR-U04-01, 51).
3. Truy vấn `subjects` theo mã hoặc tên, lọc trạng thái, sắp theo mã, tối đa 100 dòng (BR-U04-50).
4. Lấy tên Chủ nhiệm môn của các dòng qua `IAccountLookup.getContact`.
5. Lấy số lớp của các môn trong trang qua `ISubjectClasses.countClasses`; U05 chưa trả lời thì cột số lớp hiện "không khả dụng".
6. Trả danh sách; bấm một dòng mở Subject Detail.

### F2 - Thêm môn (UC 17)
1. Trên Subject List, Admin bấm "Thêm môn học"; popup mở.
2. Admin nhập mã, tên, mô tả; tùy chọn chọn Chủ nhiệm môn từ ô tìm (frontend gọi API tài khoản của U01, lọc role `SUBJECT_MANAGER`, trạng thái `ACTIVE`).
3. Backend kiểm quyền `ADMIN`.
4. Chuẩn hóa và kiểm mã: định dạng, độ dài, chưa tồn tại (BR-U04-02); kiểm tên, mô tả (BR-U04-06).
5. Có Chủ nhiệm môn thì kiểm qua `IAccountLookup.getContact`: phải `SUBJECT_MANAGER` và `ACTIVE` (BR-U04-03).
6. Lưu môn trạng thái `ACTIVE`, `version` 1.
7. Ghi audit `SUBJECT_CREATED` (kèm Chủ nhiệm môn nếu có) trong cùng transaction.
8. Đóng popup, danh sách tải lại.

### F3 - Xem chi tiết môn (UC 18)
1. Admin mở Subject Detail; frontend gọi `GET /api/v1/admin/subjects/{id}`.
2. Kiểm quyền `ADMIN`; môn không tồn tại thì `404`.
3. Trả mã, tên, mô tả, trạng thái, Chủ nhiệm môn (tên, email), `version`.
4. Tab Lớp học gọi `GET /api/v1/admin/subjects/{id}/classes`; U04 lấy danh sách qua `ISubjectClasses.listClassesOfSubject` và trả chỉ đọc (BR-U04-05). U05 không trả lời thì báo "không khả dụng".

### F4 - Sửa thông tin môn (UC 19)
1. Trên tab Thông tin, Admin sửa tên hoặc mô tả, lưu kèm `version`; frontend gọi `PATCH /api/v1/admin/subjects/{id}`.
2. Kiểm quyền `ADMIN`; mã môn không sửa được.
3. Kiểm tên, mô tả (BR-U04-06).
4. Cập nhật có điều kiện `version` khớp; lệch thì `409`, frontend báo tải lại.
5. Ghi audit `SUBJECT_UPDATED` với tên trường đã đổi.

### F5 - Đổi Chủ nhiệm môn (UC 17, 19)
1. Admin bấm "Đổi chủ nhiệm", chọn người hoặc chọn bỏ trống; frontend gọi `PUT /api/v1/admin/subjects/{id}/manager` kèm `managerId` (hoặc rỗng) và `version`.
2. Kiểm quyền `ADMIN`.
3. Có người mới thì kiểm `SUBJECT_MANAGER` và `ACTIVE` (BR-U04-03).
4. Cập nhật `manager_id` có điều kiện `version`.
5. Ghi audit `SUBJECT_MANAGER_CHANGED` với người cũ và người mới.
6. Từ request tiếp theo, người cũ mất quyền R2 với môn này, người mới có quyền (BR-U04-10).

### F6 - Lưu trữ và mở lại môn (UC 19)
**Lưu trữ**:
1. Admin bấm Lưu trữ, xác nhận; frontend gọi `PUT /api/v1/admin/subjects/{id}/status` với `ARCHIVED` và `version`.
2. Kiểm quyền `ADMIN`.
3. Gọi `ISubjectClasses.archiveClassesOfSubject` trong cùng transaction: U05 lưu trữ mọi lớp `DRAFT`, `OPEN` của môn, giữ nguyên ghi danh, ghi audit `CLASS_STATUS_CHANGED` từng lớp; U05 không trả lời thì từ chối và không lưu trữ gì (BR-U04-04).
4. Chuyển `ARCHIVED` và gỡ Chủ nhiệm môn có điều kiện `version` (một lần tăng `version`); ghi audit `SUBJECT_ARCHIVED`, có Chủ nhiệm thì thêm `SUBJECT_MANAGER_CHANGED` (BR-U04-11).

**Mở lại**:
1. Admin bấm Mở lại; gửi `ACTIVE` và `version`.
2. Kiểm quyền, chuyển `ACTIVE`, ghi audit `SUBJECT_REOPENED`. Môn mở lại chưa có Chủ nhiệm; Admin gán lại theo F5.

### F7 - Phạm vi môn cho phân quyền
1. U01 gọi `ISubjectScope.isSubjectManager(accountId, subjectId)` khi một hành động cần R2.
2. U04 đọc `manager_id` hiện thời của môn, không cache; môn không tồn tại thì trả `false` (BR-U04-10).
3. Khi U01 chặn đổi role (BR-U01-64), gọi `listManagedSubjects(accountId)` để biết người đó còn quản lý môn nào.
4. Khi U01 vô hiệu hóa tài khoản (BR-U01-71), gọi `releaseManagerAssignments(accountId, actor)` trong cùng transaction; U04 khóa các môn người đó quản lý theo thứ tự id, gỡ Chủ nhiệm, tăng `version`, ghi audit từng môn (BR-U04-12).

### F8 - Thông tin môn cho unit khác
1. U05 gọi `ISubjectQuery.isActive(subjectId)` trước khi tạo lớp, và `listManagedSubjects(accountId)` để dựng Manager Dashboard và Subject Detail của Subject Manager (UC 30).
2. U07, U10, U13 gọi `isActive` trước khi cho sửa tài nguyên cấp môn (BR-U04-07).
3. U15 gọi `countSubjectsByStatus()` cho Admin Dashboard.
