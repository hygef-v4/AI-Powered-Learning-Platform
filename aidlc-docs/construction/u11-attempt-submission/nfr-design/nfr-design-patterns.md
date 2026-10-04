# U11 Attempt & Submission - NFR Design Patterns

## P1 - Bắt đầu lượt an toàn đồng thời
- Transaction: `pg_advisory_xact_lock(hash(assignmentId, accountId))` → có `IN_PROGRESS` thì trả lại → đếm lượt đã có, so giới hạn → INSERT (`attemptNo = count + 1`) + nội dung rỗng; `deadline_at` tính sẵn để scanner tự nộp (NFR-U11-10).
- Partial unique index là chốt chặn cuối.

## P2 - Lưu nháp tối ưu
- `UPDATE attempts SET content = ?, content_version = content_version + 1 WHERE id = ? AND content_version = ?` và điều kiện lượt `IN_PROGRESS`, `now <= deadline_at + 30s`; 0 dòng → phân biệt `409` (lệch version) hay `410` (hết hạn/đã nộp) bằng một SELECT (NFR-U11-11).
- Kiểm tài liệu (`validateForSave`) trước UPDATE; request gzip giải nén qua filter giới hạn 10 MB sau giải nén (NFR-U11-02).

## P3 - Một đường nộp
- `AttemptSubmitter.submit(attemptId, mode)` dùng cho nộp tay và scanner (hết hạn, ngừng giao):
  1. `UPDATE attempts SET status = 'SUBMITTED', submitted_at = now(), submit_mode = COALESCE(submit_mode, ?), is_late = ? WHERE id = ? AND status = 'IN_PROGRESS'`.
  2. 0 dòng → đã nộp trước đó, trả biên nhận cũ (idempotent) (NFR-U11-12).
  3. Tính `receiptHash`; nếu `GRADED`, gọi `SubmissionSubmittedPort.onSubmitted(attemptId)` (U15 tạo dòng `evaluations` `PENDING`) trong cùng transaction. Nếu `PRACTICE`: Quiz gọi `PracticeResultPort.scoreQuiz` (U15 chấm và ghi `evaluations` `kind = PRACTICE`), Code Lab gọi `CodeRunPort.grade` (U13 chạy test sau commit), Text/Diagram Essay **không** gọi AI khi nộp: Student bấm "Chấm với AI" sau đó, U11 mới gọi `PracticeGradingPort` (BR-U11-35).
- Nộp tay: `validateForSubmit` trước bước 1; tự nộp: bỏ qua kiểm, lưu kết quả kiểm thành `warnings`.

## P4 - Bất biến sau nộp
- Trigger `BEFORE UPDATE ON attempts`: nếu dòng cũ đã `SUBMITTED` và `content` đổi → `RAISE EXCEPTION` (NFR-U11-13).

## P5 - Tự nộp theo lịch và khi ngưng giao
- `AttemptDeadlineScanner` mỗi phút chọn lượt `IN_PROGRESS` có `deadline_at + 30 s <= now()` (chờ lần lưu cuối trong ân hạn), gọi P3 theo lô 100.
- Ngưng giao: `AssignmentLifecycleAdapter.onRetired` đặt `deadline_at = now() - 30 s`, `submit_mode = 'AUTO_RETIRED'` cho mọi lượt `IN_PROGRESS` của bài trong transaction của U08; scanner nộp ở lượt kế tiếp.

## P6 - Rate limit lưu
- Bucket4j Redis `ratelimit:attempt-save:{accountId}` 30/phút; vượt → `429`, client lùi 10 giây (NFR-U11-23).

## P7 - Client tự lưu
- `useAutosave`: debounce 10 s; `navigator.sendBeacon` không dùng được với PUT nên khi rời trang gọi `fetch(..., {keepalive: true})`; nén gzip; hàng đợi một yêu cầu (không gửi chồng); `409` → dừng tự lưu, hộp thoại tải lại.
