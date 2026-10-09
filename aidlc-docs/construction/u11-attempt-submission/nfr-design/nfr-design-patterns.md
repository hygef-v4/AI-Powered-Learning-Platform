# U11 Attempt & Submission - NFR Design Patterns

**Bản tài liệu 2026-10-09**: UC 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); danh sách quiz của học liệu trên Learning Material (UC 15, màn của U05); primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Bắt đầu lượt an toàn đồng thời
- Transaction: kiểm ghi danh và phạm vi lớp (BR-U11-01, 05) → `pg_advisory_xact_lock(hash(assignmentId, accountId))` → có `IN_PROGRESS` thì trả lại → đếm lượt đã có, so `maxAttempts` (quiz không giới hạn thì bỏ qua) → INSERT (`attemptNo = count + 1`, `class_id`) + nội dung rỗng; `deadline_at` tính sẵn để scanner tự nộp, rỗng với quiz không giới hạn giờ (NFR-U11-10).
- Partial unique index là chốt chặn cuối.

## P2 - Lưu nháp tối ưu
- `UPDATE attempts SET content = ?, content_version = content_version + 1 WHERE id = ? AND content_version = ?` và điều kiện lượt `IN_PROGRESS`, `deadline_at IS NULL OR now <= deadline_at + 30s`; 0 dòng → phân biệt `409` (lệch version) hay `410` (hết hạn/đã nộp) bằng một SELECT (NFR-U11-11).
- Kiểm tài liệu (`validateForSave`) trước UPDATE; request gzip giải nén qua filter giới hạn 10 MB sau giải nén (NFR-U11-02).

## P3 - Một đường nộp
- `AttemptSubmitter.submit(attemptId, mode)` dùng cho nộp tay và scanner (hết giờ, hết hạn, ngưng giao):
  1. `UPDATE attempts SET status = 'SUBMITTED', submitted_at = now(), submit_mode = COALESCE(submit_mode, ?), is_late = ? WHERE id = ? AND status = 'IN_PROGRESS'`.
  2. 0 dòng → đã nộp trước đó, trả biên nhận cũ (idempotent) (NFR-U11-12).
  3. Tính `receiptHash`; bài tập `GRADED` gọi `SubmissionSubmittedPort.onSubmitted(attemptId)` (U15 tạo dòng `evaluations` `PENDING`) trong cùng transaction. Quiz gọi `PracticeResultPort.scoreQuiz` (U15 chấm và ghi `evaluations` `kind = PRACTICE`); Practice Code Lab gọi `CodeRunPort.grade` (U13 chạy test sau commit); Practice Text/Diagram Essay **không** gọi AI khi nộp: Student bấm "Chấm với AI" trên Submission History, U11 mới gọi `PracticeGradingPort` (BR-U11-35).
- Nộp tay: `validateForSubmit` trước bước 1; tự nộp: bỏ qua kiểm, lưu kết quả kiểm thành `warnings`.

## P4 - Bất biến sau nộp
- Trigger `BEFORE UPDATE ON attempts`: nếu dòng cũ đã `SUBMITTED` và `content` đổi → `RAISE EXCEPTION` (NFR-U11-13).

## P5 - Tự nộp theo lịch và khi ngưng giao
- `AttemptDeadlineScanner` mỗi phút chọn lượt `IN_PROGRESS` có `deadline_at IS NOT NULL AND deadline_at + 30 s <= now()` (chờ lần lưu cuối trong ân hạn), gọi P3 theo lô 100.
- Ngưng giao bài, ngưng quiz hoặc phát hành version mới của quiz: `AssignmentLifecycleAdapter.onRetired` đặt `deadline_at = now() - 30 s`, `submit_mode = 'AUTO_RETIRED'` cho mọi lượt `IN_PROGRESS` của bài trong transaction của U08; scanner nộp ở lượt kế tiếp.

## P6 - Rate limit lưu
- Bucket4j Redis `ratelimit:attempt-save:{accountId}` 30/phút; vượt → `429`, client lùi 10 giây (NFR-U11-23).

## P7 - Client tự lưu
- `useAutosave`: debounce 10 s; `navigator.sendBeacon` không dùng được với PUT nên khi rời trang gọi `fetch(..., {keepalive: true})`; nén gzip; hàng đợi một yêu cầu (không gửi chồng); `409` → dừng tự lưu, hộp thoại tải lại.

## P8 - Danh sách gộp theo lớp
- Student Assignments và Quiz Practice History: lấy lớp `OPEN` mình ghi danh một lần (`listOpenClassesOf`), gọi `listForClass`/truy vấn lượt theo tập lớp; ghép lượt gần nhất bằng một truy vấn `DISTINCT ON (assignment_id)` trên `attempts` theo `account_id`, chỉ chọn cột metadata; phân trang 50 dòng (NFR-U11-05).
- Lọc `classId` ngoài tập lớp của mình → `404`, không lộ tên lớp.
