# U08 Assessment Core & Publication - NFR Design Patterns

## P1 - Aggregate có khóa trạng thái
- `Assignment` là aggregate root; mọi thay đổi câu đi qua `assignment.editQuestions(...)`, ném `AssignmentLockedException` khi không phải `DRAFT` (NFR-U08-20).
- Mỗi thao tác sửa khóa dòng `assignments` (`SELECT ... FOR UPDATE`) rồi kiểm `DRAFT`; không còn `DRAFT` → `409` (NFR-U08-21).

## P2 - Lịch bằng scanner
- `AssignmentScheduleScanner` đăng ký với U03, chạy mỗi phút: `UPDATE assignments SET status = 'OPEN' WHERE status = 'SCHEDULED' AND opens_at <= now() RETURNING id, class_id` rồi gọi `AssignmentLifecyclePort.onOpened` cho từng bài trong cùng transaction; tương tự đóng bài khi `COALESCE(late_until, closes_at) <= now()` (NFR-U08-23).
- Đổi lịch chỉ sửa cột; lần quét sau dùng lịch mới. Độ trễ ≤ 1 phút (NFR-U08-03).

## P3 - Trạng thái nộp tính tại chỗ
- `isSubmissionOpen(assignment, now)`: `RETIRED` hoặc `now < opens_at` → `CLOSED`; `now ≤ closes_at` → `ON_TIME`; `late_until` có và `now ≤ late_until` → `LATE`; còn lại `CLOSED`. Không phụ thuộc scanner (scanner chỉ để hiển thị và phát event) (NFR-U08-02, 11).

## P4 - Chuyển đổi hiển thị cho người học
- `StudentAssignmentView` dựng từ `assignment_questions`: mọi câu (ngân hàng hay riêng của bài) đọc qua `BankQueryPort.getStudentView` (NFR-U08-30).

## P5 - Kiểm duyệt có thể mở rộng
- `ReviewValidator` chạy danh sách `ReviewCheck`: kiểm của U08 (câu, điểm, loại khớp) + `TypeConfigPort` (U09, `C`: cấu hình, khung, rubric từng câu/phần) + `CodeLabCheckPort` cho `CODE_LAB` (U13, `C`); khi chưa có U09/U13 hai kiểm này mặc định đạt.
- Phát hành bài `GROUP_ASSIGNMENT` gọi thêm `GroupReadinessPort` (U12).

## P6 - Event sau commit
- Mở bài và ngưng giao gọi `AssignmentLifecyclePort.onOpened/onRetired(assignmentId)` trong cùng transaction; các cài đặt (U11, U14) ghi dòng hoặc gửi việc của mình nên không mất phản ứng.
- Sau commit phát `assignment.opened` `{assignmentId, classId}` qua `EventPublisherPort` (U03) chỉ cho thông báo (NFR-U08-24).
