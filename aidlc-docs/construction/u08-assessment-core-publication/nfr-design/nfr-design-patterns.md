# U08 Assessment Core & Publication - NFR Design Patterns

## P1 - Aggregate có khóa trạng thái
- `Assignment` là aggregate root; mọi thay đổi thành phần đi qua `assignment.editComponents(...)`, ném `AssignmentLockedException` khi không phải `DRAFT` (NFR-U08-20).
- `@Version` trên `Assignment` và `Publication`; lệch → `409` (NFR-U08-21).

## P2 - Lịch bằng job U02
- Phát hành: tạo hai job `U08_PUBLICATION_OPEN` (`next_attempt_at = opensAt`) và `U08_PUBLICATION_CLOSE` (`next_attempt_at = lateUntil ?? closesAt`), payload `{publicationId, expectedAt}`.
- Handler: `UPDATE publications SET status = 'OPEN' WHERE id = ? AND status = 'SCHEDULED' AND opens_at = :expectedAt` (tương tự cho đóng); 0 dòng → bỏ qua (job cũ sau khi đổi lịch hoặc chạy lại) (NFR-U08-23).
- Đổi lịch tạo job mới với `expectedAt` mới; không cần xóa job cũ.
- Độ trễ: U02 quét mỗi phút → ≤ 1 phút (NFR-U08-03).

## P3 - Trạng thái nộp tính tại chỗ
- `isSubmissionOpen(publication, now)`: `RETIRED` hoặc `now < opensAt` → `CLOSED`; `now ≤ closesAt` → `ON_TIME`; `allowLate && now ≤ lateUntil` → `LATE`; còn lại `CLOSED`. Không phụ thuộc job (job chỉ để hiển thị và phát event) (NFR-U08-02, 11).

## P4 - Chuyển đổi hiển thị cho người học
- `LearnerAssignmentView` dựng từ thành phần: câu ngân hàng qua `BankQueryPort.getLearnerView`, câu riêng qua cùng bộ lọc (`LearnerViewMapper`) (NFR-U08-30).

## P5 - Kiểm duyệt có thể mở rộng
- `ReviewValidator` chạy danh sách `ReviewCheck`: kiểm của U08 (thành phần, điểm, loại khớp) + `TypeConfigCheckPort` (U09, `C`); mặc định khi chưa có U09: đạt.

## P6 - Event sau commit
- `ASSIGNMENT_OPENED`, `ASSIGNMENT_CLOSED`, `ASSIGNMENT_RETIRED` `{publicationId, assignmentId, classId}` qua `EventPublisherPort` (U02) (NFR-U08-24).
