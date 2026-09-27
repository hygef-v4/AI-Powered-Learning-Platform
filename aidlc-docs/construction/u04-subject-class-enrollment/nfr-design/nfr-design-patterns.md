# U04 Subject, Class, Enrollment & Learning Access - NFR Design Patterns

## P1 - Kiểm phạm vi bằng query có index
- `isInstructorOf`: `classes(id, instructor_account_id)`; `isSubjectManager`: `subjects(id, manager_account_id)`; `isActiveLearner`: unique `(class_id, learner_account_id)` + lọc `status = 'ACTIVE'`.
- Không cache (NFR-U04-01). Index thêm: `classes(subject_id, status)`, `classes(instructor_account_id)`, `subjects(manager_account_id)`, `enrollments(learner_account_id, status)`.

## P2 - Khóa theo người học và môn
- Trước khi ghi danh (thêm, khôi phục, mã mời): `pg_advisory_xact_lock(hash(learnerId, subjectId))` trong transaction, rồi kiểm BR-U04-22 (NFR-U04-11).
- Hàng chưa tồn tại không khóa được bằng `FOR UPDATE`, nên dùng advisory lock; khóa tự nhả khi transaction kết thúc.
- Mở lại lớp: lấy khóa cho mọi người học `ACTIVE` của lớp theo thứ tự `learnerId` tăng dần để tránh deadlock, rồi kiểm BR-U04-15.

## P3 - Ghi danh danh sách, mỗi dòng một transaction
1. Chuẩn hóa, bỏ trùng, kiểm ≤ 200 dòng và ≤ 100 KB.
2. Gọi `AccountLookupPort.findByEmails(list)` **một lần**.
3. Mỗi dòng chạy trong `TransactionTemplate` riêng (P2); lỗi một dòng không ảnh hưởng dòng khác.
4. Gom kết quả trả về (NFR-U04-03).

## P4 - Khóa lạc quan cho lớp và môn
- Cột `version` (`@Version`); client gửi `version` khi sửa/đổi trạng thái; lệch → `409` (NFR-U04-12).

## P5 - Event sau commit
- `ENROLLMENT_ACTIVATED {enrollmentId, classId, learnerAccountId}` gửi qua `EventPublisherPort` (U02, gửi sau commit). Mở lớp `DRAFT → OPEN` gửi một event cho mỗi ghi danh `ACTIVE` (NFR-U04-13).

## P6 - Mã mời
- Sinh 8 ký tự từ bảng chữ BR-U04-30 bằng `SecureRandom`; trùng unique → sinh lại, tối đa 3 lần.
- Rate limit Bucket4j, khóa Redis `ratelimit:invite-code:{accountId}`, 10 token/giờ; **chỉ trừ khi nhập sai**; hết token → `429` và audit. Redis lỗi → từ chối (NFR-U04-31).
- Mã so sánh sau khi viết hoa, bỏ khoảng trắng.

## P7 - Che giấu đối tượng ngoài quyền
- Mọi truy vấn theo ID đi qua một hàm `loadForActor(actor, classId)`: không thấy hoặc không có quyền → cùng `404` (BR-U04-42, 51).

## P8 - Fail closed
- U01 (`AuthorizationPort`, `AccountLookupPort`) lỗi hoặc hết timeout → từ chối, trả "tạm thời không khả dụng".
- `PublishedContentPort` (U05) lỗi → trang lớp vẫn trả thông tin lớp, phần nội dung báo "chưa tải được".
