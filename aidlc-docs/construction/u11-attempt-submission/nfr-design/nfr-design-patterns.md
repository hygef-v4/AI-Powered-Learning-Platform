# U11 Attempt & Submission - NFR Design Patterns

## P1 - Bắt đầu lượt an toàn đồng thời
- Transaction: `pg_advisory_xact_lock(hash(publicationId, learnerId))` → có `IN_PROGRESS` thì trả lại → đếm lượt đã có, so giới hạn → INSERT (`attemptNo = count + 1`) + nội dung rỗng → tạo job tự nộp (NFR-U11-10).
- Partial unique index là chốt chặn cuối.

## P2 - Lưu nháp tối ưu
- `UPDATE submissions SET content = ?, content_version = content_version + 1 WHERE id = ? AND content_version = ?` và điều kiện lượt `IN_PROGRESS`, `now <= deadline_at + 30s`; 0 dòng → phân biệt `409` (lệch version) hay `410` (hết hạn/đã nộp) bằng một SELECT (NFR-U11-11).
- Kiểm tài liệu (`validateForSave`) trước UPDATE; request gzip giải nén qua filter giới hạn 10 MB sau giải nén (NFR-U11-02).

## P3 - Một đường nộp
- `AttemptSubmitter.submit(attemptId, mode)` dùng cho nộp tay, job hết hạn, listener ngừng giao:
  1. `UPDATE submissions SET status = 'SUBMITTED', submitted_at = now(), submit_mode = ?, late = ? WHERE id = ? AND status = 'IN_PROGRESS'`.
  2. 0 dòng → đã nộp trước đó, trả biên nhận cũ (idempotent) (NFR-U11-12).
  3. Tính `receiptHash`, phát `SUBMISSION_SUBMITTED` sau commit.
- Nộp tay: `validateForSubmit` trước bước 1; tự nộp: bỏ qua kiểm, lưu kết quả kiểm thành `warnings`.

## P4 - Bất biến sau nộp
- Trigger `BEFORE UPDATE ON submissions`: nếu dòng cũ đã `SUBMITTED` và `content` đổi → `RAISE EXCEPTION` (NFR-U11-13).

## P5 - Tự nộp theo lịch và theo event
- Job `U11_AUTO_SUBMIT {attemptId}` với `next_attempt_at = deadlineAt + 30s` (chờ lần lưu cuối trong ân hạn).
- Listener `ASSIGNMENT_RETIRED`: lấy mọi lượt `IN_PROGRESS` của publication theo lô 100, gọi P3 với `AUTO_RETIRED`.

## P6 - Rate limit lưu
- Bucket4j Redis `u11:save:{learnerId}` 30/phút; vượt → `429`, client lùi 10 giây (NFR-U11-23).

## P7 - Client tự lưu
- `useAutosave`: debounce 10 s; `navigator.sendBeacon` không dùng được với PUT nên khi rời trang gọi `fetch(..., {keepalive: true})`; nén gzip; hàng đợi một yêu cầu (không gửi chồng); `409` → dừng tự lưu, hộp thoại tải lại.
