# U14 Group Document & Submission - NFR Design Patterns

**Bản tài liệu 2026-10-08**: UC 23; primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Khóa mục trong dòng tài liệu
- Mọi thao tác mục (nhận, giao, lưu nháp, xong, nhả) chạy trong một transaction: `SELECT ... FROM group_documents WHERE id=? FOR UPDATE` → kiểm điều kiện trên phần tử của `sections.items` → ghi lại cột JSON, `version = version + 1`, `updated_at = now()`.
- Nhận: chỉ khi `status IN ('OPEN','DONE')`; sai → trả `409` kèm người đang giữ (NFR-U14-10). Gán `draftBlocks = publishedBlocks`.
- Giao (trưởng nhóm): nhả khóa người cũ nếu có rồi đặt `CLAIMED`, `claimedBy`, `assignedBy`.
- Lưu nháp: chỉ khi `claimedBy` = người gọi và `version` của client khớp; lưu nháp không chặn người khác vì khóa dòng chỉ giữ trong vài ms.
- Xong: kiểm block (U09) → cập nhật mục → thêm phần tử `revisions` → `DONE`, bỏ `claimedBy`. Không đổi trạng thái tài liệu.
- Nhả: xóa `draftBlocks` (client đã cảnh báo), trạng thái về `DONE` nếu `publishedBlocks` có nội dung, không thì `OPEN`.

## P2 - Realtime qua RabbitMQ fanout + SSE
1. Sau commit, `GroupDocEventPublisher` gửi `{groupDocumentId, type, payload nhỏ, version}` lên exchange fanout `platform.realtime`.
2. Mỗi backend có queue tạm `jobs.realtime.{instanceId}` (exclusive, auto-delete) bind vào fanout; `SseHub` giữ map `khóa kênh → emitters` (U14 dùng `groupDocumentId`; U16 đăng ký `accountId` cho chuông thông báo), đẩy sự kiện tới emitter của kênh đó.
3. Payload chỉ chứa ID, trạng thái, tác giả; nội dung mục lớn thì client gọi `GET` mục theo `version` (tránh đẩy MB qua SSE).
4. Heartbeat 25 s (comment `:ping`); emitter timeout 30 phút, client tự kết nối lại; kết nối lại → client `GET` toàn bộ tài liệu (NFR-U14-03).
5. Worker (scanner tự nộp) phát qua cùng fanout → backend đẩy.

## P3 - Quyền kênh SSE
- Mở kênh kiểm thành viên/giảng viên (U12, U04). `GroupChangePort.onMemberRemoved` (U12 gọi trong transaction) → `SseHub` đóng emitter của người bị bỏ; nhả khóa mục của họ (P1) (NFR-U14-20).

## P4 - Nộp một đường, bất biến
- `GroupSubmitter.submit(docId, mode, actor)` dùng cho trưởng nhóm và scanner; nộp tay khi `group_documents.status = IN_PROGRESS` (khóa hàng tài liệu bằng `SELECT ... FOR UPDATE`); chụp tài liệu từ chính dòng đang khóa nên nhất quán; ghi `submitted_snapshot`, `submit_mode`, `submitted_at`.
- `GroupAutoSubmitScanner` (mỗi phút): chọn tài liệu `IN_PROGRESS` của bài nhóm (`GROUP_ASSIGNMENT`) đã quá `late_until`/`closes_at` 30 giây hoặc `RETIRED`; trong một transaction: chốt mục `CLAIMED` bằng `draftBlocks` đã lưu (thêm `revisions`, `DONE`), nộp nếu `submitted_at IS NULL OR updated_at > submitted_at` (NFR-U14-13, 14), đặt `status = CLOSED`. Chạy lại không nộp trùng vì chỉ chọn tài liệu `IN_PROGRESS`.
- Mọi client đang mở tài liệu (sửa mục hoặc chỉ xem): đồng hồ về 0 hoặc nhận tin bài ngưng giao → khóa trang, người đang sửa gửi lần lưu cuối, hiện vòng chờ; nhận `GROUP_SUBMITTED` qua SSE (mất kết nối thì hỏi trạng thái bản nộp) rồi chuyển sang Submission History (BR-U14-36).

## P5 - Dựng tài liệu từ khung
- Khi bài mở: đọc khung (U09), tách theo các phần của khung (`config.parts`, U09) → mỗi phần một mục; block ngoài mục → `sharedBlocks`; tạo `group_documents` cho mọi nhóm của lớp (U12), một transaction mỗi nhóm, `INSERT ... ON CONFLICT (group_id, assignment_id) DO NOTHING`.
