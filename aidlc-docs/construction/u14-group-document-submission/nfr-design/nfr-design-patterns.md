# U14 Group Document & Submission - NFR Design Patterns

## P1 - Khóa mục bằng UPDATE có điều kiện
- Nhận: `UPDATE sections SET status='CLAIMED', claimed_by=?, claimed_at=now(), draft_blocks=published_blocks, version=version+1 WHERE id=? AND status IN ('OPEN','IN_REVIEW')` → 0 dòng: đọc người đang giữ, trả `409` (NFR-U14-10).
- Lưu nháp: `... WHERE id=? AND claimed_by=? AND version=?`.
- Xong: một transaction: kiểm block (U09) → cập nhật mục → INSERT `section_revisions` → `status='IN_REVIEW'`, `claimed_by=NULL`.
- Nhả: `WHERE claimed_by IS NOT NULL`; trạng thái về `IN_REVIEW` nếu `published_blocks` có nội dung, không thì `OPEN`.

## P2 - Realtime qua RabbitMQ fanout + SSE
1. Sau commit, `GroupDocEventPublisher` gửi `{groupId, type, payload nhỏ, version}` lên exchange fanout `platform.realtime`.
2. Mỗi backend có queue tạm `jobs.realtime.{instanceId}` (exclusive, auto-delete) bind vào fanout; `SseHub` giữ map `groupId → emitters`, đẩy sự kiện tới emitter của tài liệu đó.
3. Payload chỉ chứa ID, trạng thái, tác giả; nội dung mục lớn thì client gọi `GET` mục theo `version` (tránh đẩy MB qua SSE).
4. Heartbeat 25 s (comment `:ping`); emitter timeout 30 phút, client tự kết nối lại; kết nối lại → client `GET` toàn bộ tài liệu (NFR-U14-03).
5. Worker (tự nộp) phát qua cùng fanout → backend đẩy.

## P3 - Quyền kênh SSE
- Mở kênh kiểm thành viên/giảng viên (U12, U04). `GroupChangePort.onMemberRemoved` (U12 gọi trong transaction) → `SseHub` đóng emitter của người bị bỏ; nhả khóa mục của họ (P1) (NFR-U14-20).

## P4 - Nộp một đường, bất biến
- `GroupSubmitter.submit(docId, mode, actor)` dùng cho trưởng nhóm, job tại hạn, job ngừng giao; chụp tài liệu trong `REPEATABLE READ` để nhất quán; INSERT `group_submissions`.
- Job tự nộp: chỉ nộp nếu `last_changed_at > last_submitted_at` hoặc chưa có bản nộp (NFR-U14-13); job idempotent theo `(docId, deadline)`.

## P5 - Dựng tài liệu từ khung
- Khi publication mở: đọc khung (U09), tách theo heading `workSection` → `Section`; block ngoài mục → `sharedBlocks`; tạo cho mọi nhóm trong một transaction mỗi nhóm.
