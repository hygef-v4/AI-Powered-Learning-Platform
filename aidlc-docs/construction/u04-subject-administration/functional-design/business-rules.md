# U04 Subject Administration — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U04 Subject Administration thuộc Iter 1, người code: Người 4; UC 16–19. Tách phần môn học từ U04 cũ (Subject, Class & Enrollment); phần lớp và ghi danh thuộc U05. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

Mỗi rule có mã `BR-U04-xx` để truy vết sang test. Mã giữ như U04 cũ cho các rule về môn.

## 1. Môn học

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U04-01 | Chỉ `ADMIN` đang `ACTIVE` xem danh sách môn (UC 16), thêm môn (UC 17), xem chi tiết môn (UC 18) và sửa môn (UC 19). Role khác bị từ chối và ghi audit. | UC 16–19 |
| BR-U04-02 | `code` môn duy nhất toàn hệ thống, tối đa 20 ký tự, chỉ chữ hoa, số và `-`; không đổi sau khi tạo. | UC 17, 19 |
| BR-U04-03 | Mỗi môn có tối đa một Chủ nhiệm môn, phải là tài khoản `SUBJECT_MANAGER` đang `ACTIVE`; Admin không được gán. Gán khi thêm môn hoặc khi sửa môn; gán người mới thay người cũ; được để trống. Môn `ARCHIVED` không có Chủ nhiệm môn: không gán được tới khi mở lại. `subjects.manager_id` hiện thời là căn cứ duy nhất của quyền R2. | FR-002, FR-003 |
| BR-U04-04 | Lưu trữ và mở lại là thao tác trong UC 19. Chỉ lưu trữ khi mọi lớp của môn đã `ARCHIVED`; không kiểm được số lớp (U05 không trả lời) thì từ chối. Mở lại đưa môn về `ACTIVE`. Không xóa môn. | UC 19 |
| BR-U04-05 | Subject Detail của Admin (UC 18) hiện thông tin môn, Chủ nhiệm môn và danh sách lớp của môn chỉ đọc (mã, tên, học kỳ, trạng thái, giảng viên, số sinh viên). Admin không mở chi tiết lớp và không sửa lớp. | UC 18 |
| BR-U04-06 | `name` bắt buộc, tối đa 200 ký tự; `description` tùy chọn, tối đa 2.000 ký tự. Sửa phải gửi kèm `version`; lệch thì trả `409`. | UC 17, 19 |
| BR-U04-07 | Môn `ARCHIVED`: không tạo lớp mới (U05 kiểm), tài nguyên cấp môn chỉ đọc (U07, U10, U13 kiểm qua `ISubjectQuery.isActive`); lịch sử giữ nguyên. | UC 19 |

## 2. Phạm vi cho phân quyền

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U04-10 | `isSubjectManager(accountId, subjectId)` đọc `manager_id` hiện thời mỗi lần gọi, không cache; đổi hoặc bỏ Chủ nhiệm môn có hiệu lực ở request tiếp theo. | BR-U01-62, 64 |
| BR-U04-11 | Lưu trữ môn thì gỡ Chủ nhiệm môn trong cùng transaction, cùng một lần tăng `version`; ghi audit `SUBJECT_ARCHIVED` và `SUBJECT_MANAGER_CHANGED`. Mở lại môn thì môn chưa có Chủ nhiệm, Admin gán lại. | BR-U04-07, Q14 |
| BR-U04-12 | Tài khoản bị vô hiệu hóa (BR-U01-71) thì bị gỡ khỏi Chủ nhiệm của mọi môn trong transaction vô hiệu hóa của U01: U01 gọi `ISubjectScope.releaseManagerAssignments`, U04 khóa từng môn, gỡ, tăng `version`, ghi audit `SUBJECT_MANAGER_CHANGED` với người thực hiện là Admin. Lỗi thì vô hiệu hóa rollback. Mở lại tài khoản không tự gán lại. | Q14 |

## 3. Danh sách, audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U04-50 | Subject List tìm theo mã hoặc tên, lọc theo trạng thái, sắp theo mã, mỗi trang tối đa 100 dòng; mỗi dòng có Chủ nhiệm môn và số lớp. | UC 16 |
| BR-U04-51 | Người không phải Admin gọi API quản trị môn nhận `403` và ghi audit `ACCESS_DENIED`. | SEC-002 |
| BR-U04-52 | Audit: `SUBJECT_CREATED`, `SUBJECT_UPDATED` (tên trường đã đổi), `SUBJECT_MANAGER_CHANGED` (trước/sau), `SUBJECT_ARCHIVED`, `SUBJECT_REOPENED`. | FR-014, SEC-005 |
| BR-U04-53 | Dữ liệu sai (mã trùng, mã sai định dạng, Chủ nhiệm môn không tồn tại, sai role hoặc không `ACTIVE`) thì từ chối toàn bộ thao tác, báo cách sửa, không lộ dữ liệu người khác. | US-CAT-001 S2 |
| BR-U04-54 | Không kiểm được quyền (U01 lỗi) thì từ chối. | BR-U01-93 |

## 4. Quy tắc chung

- Quyền kiểm phía server ở mọi request, mặc định từ chối; route đổi trạng thái có chống CSRF.
- Repository của unit là riêng; unit khác đọc môn qua `ISubjectQuery`, không đọc bảng `subjects`.
