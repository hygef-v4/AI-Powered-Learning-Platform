# U04 Subject, Class, Enrollment & Learning Access - NFR Design Patterns

**Bản tài liệu 2026-10-09**: UC 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Kiểm phạm vi bằng query có index
- `isTeacherOf`: `course_classes(id, teacher_id)`; `isSubjectManager`: `subjects(id, manager_id)`; `isActiveStudent`: khóa chính `enrollments(class_id, account_id)` + lọc `status = 'ACTIVE'`.
- Không cache (NFR-U04-01). Index thêm: `course_classes(subject_id, status)`, `course_classes(teacher_id)`, `subjects(manager_id)`, `enrollments(account_id, status)`.

## P2 - Khóa theo người học và môn
- Trước khi ghi danh (thêm, thêm lại): `pg_advisory_xact_lock(hash(studentId, subjectId))` trong transaction, rồi kiểm BR-U04-22 (NFR-U04-11).
- Hàng chưa tồn tại không khóa được bằng `FOR UPDATE`, nên dùng advisory lock; khóa tự nhả khi transaction kết thúc.
- Mở lại lớp: lấy khóa cho mọi người học `ACTIVE` của lớp theo thứ tự `studentId` tăng dần để tránh deadlock, rồi kiểm BR-U04-15.

## P3 - Ghi danh danh sách, mỗi dòng một transaction
1. Chuẩn hóa, bỏ trùng, kiểm ≤ 200 dòng và ≤ 100 KB.
2. Gọi `AccountLookupPort.findByEmails(list)` **một lần**.
3. Mỗi dòng chạy trong `TransactionTemplate` riêng (P2); lỗi một dòng không ảnh hưởng dòng khác.
4. Gom kết quả trả về (NFR-U04-03).

## P4 - Khóa lạc quan cho lớp và môn
- Cột `version` (`@Version`); client gửi `version` khi sửa/đổi trạng thái; lệch → `409` (NFR-U04-12).

## P5 - Event sau commit
- `enrollment.activated {classId, accountId}` gửi qua `EventPublisherPort` (U03, gửi sau commit). Mở lớp `DRAFT → OPEN` gửi một event cho mỗi ghi danh `ACTIVE` (NFR-U04-13).

## P6 - Mã mời
- Bỏ ngày 2026-10-09 cùng chức năng tự ghi danh bằng mã mời.

## P7 - Che giấu đối tượng ngoài quyền
- Mọi truy vấn theo ID đi qua một hàm `loadForActor(actor, classId)`: không thấy hoặc không có quyền → cùng `404` (BR-U04-42, 51).

## P8 - Fail closed
- U01 (`AuthorizationPort`, `AccountLookupPort`) lỗi hoặc hết timeout → từ chối, trả "tạm thời không khả dụng".
- `PublishedContentPort` (U05) lỗi → trang lớp vẫn trả thông tin lớp, phần nội dung báo "chưa tải được".
