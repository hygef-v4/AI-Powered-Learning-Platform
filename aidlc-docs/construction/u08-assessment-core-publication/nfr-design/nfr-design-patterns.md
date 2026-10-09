# U08 Assessment Core & Publication - NFR Design Patterns

**Bản tài liệu 2026-10-09**: UC 41 và vòng đời (tạo, xóa, duyệt, phát hành, lịch, ngưng giao, nhân bản, version) của UC 35, 42, 43, 44, 45 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Aggregate có khóa trạng thái
- `Assignment` là aggregate root; mọi thay đổi câu đi qua `assignment.editQuestions(...)`, ném `AssignmentLockedException` khi không phải `DRAFT` (NFR-U08-20).
- Mỗi thao tác sửa khóa dòng `assignments` (`SELECT ... FOR UPDATE`) rồi kiểm `DRAFT`; không còn `DRAFT` → `409` (NFR-U08-21).

## P2 - Lịch bằng scanner
- `AssignmentScheduleScanner` đăng ký với U03, chạy mỗi phút: `UPDATE assignments SET status = 'OPEN' WHERE status = 'SCHEDULED' AND opens_at <= now() RETURNING id, class_id` rồi gọi `AssignmentLifecyclePort.onOpened` cho từng bài trong cùng transaction; tương tự đóng bài tập khi `COALESCE(late_until, closes_at) <= now()`; quiz không có `closes_at` nên không bị đóng (NFR-U08-23).
- Đổi lịch chỉ sửa cột; lần quét sau dùng lịch mới. Độ trễ ≤ 1 phút (NFR-U08-03).

## P3 - Trạng thái nộp tính tại chỗ
- `isSubmissionOpen(assignment, now)`: quiz `OPEN` → `ON_TIME`; `RETIRED` hoặc `now < opens_at` → `CLOSED`; `now ≤ closes_at` → `ON_TIME`; `late_until` có và `now ≤ late_until` → `LATE`; còn lại `CLOSED`. Không phụ thuộc scanner (scanner chỉ để hiển thị và phát event) (NFR-U08-02, 11).

## P4 - Chuyển đổi hiển thị cho người học
- `StudentAssignmentView` dựng từ `assignment_questions`: mọi câu (ngân hàng hay riêng của bài) đọc qua `BankQueryPort.getStudentView`; khung Diagram Essay/bài nhóm lấy từ U09 (NFR-U08-30).

## P5 - Kiểm duyệt có thể mở rộng
- `ReviewValidator` chạy danh sách `ReviewCheck`: kiểm của U08 (câu hợp lệ khớp dạng, điểm; quiz: học liệu còn `ACTIVE` qua `ContentRefPort`) + `TypeConfigPort` (U09, `C`: nội dung dạng bài, khung, rubric đã điền) + `CodeLabCheckPort` cho `CODE_LAB` (U13, `C`); khi chưa có U09/U13 hai kiểm này mặc định đạt.
- Phát hành bài `GROUP_ASSIGNMENT` gọi thêm `GroupReadinessPort` (U12).

## P6 - Event sau commit
- Mở bài và ngưng giao gọi `AssignmentLifecyclePort.onOpened/onRetired(assignmentId)` trong cùng transaction; các cài đặt (U11, U14) ghi dòng hoặc gửi việc của mình nên không mất phản ứng.
- Sau commit phát `assignment.opened` `{assignmentId, classId | subjectId, type}` qua `EventPublisherPort` (U03) chỉ cho thông báo; bài của môn thì U16 tự lấy các lớp `OPEN` của môn (NFR-U08-24).

## P7 - Khóa rubric khi phát hành
- `ScheduleService.publish` khóa dòng `assignments`, ghi lịch, gọi `RubricPort.lockForAssignment(assignmentId)` rồi đổi trạng thái trong cùng transaction; U06 ném lỗi kèm danh sách câu/phần có rubric trống → rollback, trả `422` cho frontend hiện danh sách (BR-U08-33, BR-U06-37).
- Quiz phát hành: đổi `OPEN`, đặt `opens_at = now()`, chuyển version cũ cùng nguồn sang `RETIRED` trong cùng transaction (BR-U08-64).

## P8 - Copy nguyên khối
- `AssignmentCopier.copy(source, targetClass, actor)` trong một `@Transactional`:
  1. Khóa và kiểm quyền cả lớp nguồn và lớp đích (NFR-U08-25).
  2. Tạo dòng `assignments` `DRAFT` ở lớp đích, `source_assignment_id`, `config.origin = CLASS_COPY`.
  3. Câu ngân hàng giữ nguyên `question_id`; câu riêng của bài nguồn sao bằng `InlineQuestionPort.copyToAssignment`.
  4. `TypeConfigPort.copy` (U09) sao cấu hình, khung và nhân bản rubric.
  5. Audit; lỗi ở bất kỳ bước nào thì rollback toàn bộ.
