# U04 Subject, Class & Enrollment — nfr-design-patterns

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67. **Primary stories:** US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001. **Requirements:** FR-002/003/005/027.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u04-subject-class-enrollment` không quyết định chức năng hiện hành.

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

## Quy tắc tích hợp revision hiện hành

- Admin CRUD/archive subjects và gán Subject Manager ACTIVE, chỉ xem class list metadata; không teacher hoặc manager assignment cho Admin.
- Subject Manager của môn R2 tạo/lifecycle lớp, phân Teacher/Subject Manager ACTIVE và thêm/gỡ Student; Teacher chỉ xem lớp được giao, không enrollment/lifecycle.
- Enrollment ACTIVE cấp R5, khi gỡ không xóa lịch sử bài/điểm nhưng deny thao tác tiếp theo. Mỗi sinh viên tối đa một lớp hiệu lực của một môn theo baseline.
- Scope reader cho API/MCP/activity cùng current authority; group membership và teacher assignment kiểm mỗi call, không role-only.
- Archived resource chỉ đọc lịch sử theo quyền; không tiếp tục edit/publish/enroll.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Admin không dạy/chấm, manager không assigned teacher không được chấm, Teacher không enrollment edit.
- Concurrent enrollment/assignment update có version; revoke enrollment deny next API/MCP/activity call; giữ lịch sử.
