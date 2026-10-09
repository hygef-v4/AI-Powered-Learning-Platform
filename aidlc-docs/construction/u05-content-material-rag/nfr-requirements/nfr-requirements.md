# U05 Content, Material & RAG - NFR Requirements

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Hiệu năng và tài nguyên

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-01 | Worker quét tối đa **4** lesson cùng lúc; cần VPS ≥ 8 GB RAM, nếu VPS nhỏ hơn hạ về 2 qua cấu hình. | Câu N2 |
| NFR-U05-02 | Trích chữ đọc file theo luồng từ U03, không nạp cả file 50 MB vào RAM; lưu tối đa 2 000 000 ký tự mỗi lesson, vượt thì chỉ lấy phần đầu. | Thiết kế |
| NFR-U05-03 | Tài liệu 50 trang quét xong ≤ 1 phút khi Gemini bình thường. | NFR-003 |
| NFR-U05-04 | `retrieve` p95 ≤ 1,5 s (gồm gọi Gemini tạo vector câu hỏi); tìm vector trong DB p95 ≤ 200 ms với ≤ 50 000 lesson. | NFR-003 |
| NFR-U05-05 | Danh sách module/học liệu trên Material List và tab Materials p95 ≤ 300 ms; feed Class Announcements p95 ≤ 300 ms. | NFR-003 |

## 2. Gemini và YouTube

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-10 | Mỗi lần tóm tắt hoặc embedding đi qua `AiUsagePort` của U13: kiểm AI bật, trần chi phí Gemini/ngày dùng chung (Redis do U13 quản lý), giữ/trừ credit và ghi `ai_suggestions`. | Câu N1, REL-005 |
| NFR-U05-11 | Hết trần hoặc Gemini báo hết quota sau các lần thử lại: lesson sang `BUSY` hiển thị "Hệ thống đang bận"; `retrieve` trả `503`. Không trừ credit cho lời gọi chưa được Gemini xử lý. | Câu N1 |
| NFR-U05-13 | `GEMINI_API_KEY` và `YOUTUBE_API_KEY` đọc từ `.env`, không commit, không log. | Câu N3, SEC-006 |
| NFR-U05-14 | Timeout: Gemini kết nối 5 s, đọc 30 s; YouTube 5 s/15 s. 429/5xx thử lại theo U03; lỗi 400/403 không thử lại. | REL-003 |
| NFR-U05-16 | Credit embedding: 1 credit = 1 000 token, làm tròn lên mỗi lần gọi; quét tính cho người tải lên, truy xuất tính cho người yêu cầu AI; thử lại không trừ trùng (U13 giữ một dòng hold và các dòng lời gọi theo requestRef chunk/merge/embedding, complete idempotent). | BR-U05-39, 44 |
| NFR-U05-17 | Tóm tắt khi quét: tối đa 200 000 ký tự đầu, chia đoạn 30 000 ký tự, bản tóm tắt ≤ 4 000 ký tự; mỗi lời gọi Gemini dùng timeout như NFR-U05-14; credit 1 credit = 1 000 token như embedding, tính cho người tải lên và chỉ khi thật sự gọi Gemini. | BR-U05-45…47 |
| NFR-U05-18 | Hold/tạo lesson cùng transaction; terminal settle lượng AI thật, trả dư. scan_expires_at cố định 24 giờ từ tạo lesson; hold scanner U13 25 giờ là dự phòng. Claim/lease/CAS phục hồi SCANNING hết hạn; thử lại không xử lý trùng đồng thời hoặc cộng lại checkpoint complete. Summary có rồi được giữ nếu embedding lỗi. | BR-U05-37, 39, 46 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-20 | Markdown của thông báo hiển thị qua bộ lọc cho phép danh sách thẻ an toàn; không HTML thô, không `javascript:` URL. | BR-U05-62, SEC-003 |
| NFR-U05-21 | Chỉ chấp nhận URL YouTube đúng mẫu BR-U05-22; server tự dựng URL gọi API từ `videoId`, không gọi URL người dùng nhập (tránh SSRF). | SEC-003 |
| NFR-U05-22 | CSP cho phép `frame-src https://www.youtube-nocookie.com`. | BR-U05-24, SEC-004 |
| NFR-U05-23 | `retrieve` chỉ gọi nội bộ từ U13 (không có endpoint HTTP công khai). | BR-U05-40 |
| NFR-U05-24 | Không gửi dữ liệu người dùng lên Gemini ngoài nội dung học liệu và câu hỏi truy xuất. | SEC-005 |
| NFR-U05-25 | API thông báo kiểm người đọc đang học hoặc dạy lớp `OPEN`; chỉ giảng viên của lớp tạo, sửa, xóa thông báo. | US-CNT-004, BR-U05-60…64 |
| NFR-U05-26 | API học liệu kiểm phạm vi ở backend: R2 chỉ với học liệu của môn, R3/R4 với học liệu của đúng lớp và học liệu của môn (chỉ đọc), R5 qua ghi danh; `classId` trên query chỉ là ngữ cảnh, server vẫn kiểm quan hệ thật. | BR-U05-02, 04 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-30 | Unit test mọi `BR-U05-xx`; markdown chứa script bị lọc; URL YouTube giả mạo hoặc playlist bị từ chối. | NFR-004 |
| NFR-U05-31 | `EmbeddingPort` và `YoutubePort` có adapter giả (vector cố định, phụ đề mẫu) cho test và khi chạy local không có key. | NFR-004, NFR-005 |
| NFR-U05-32 | Integration test: `retrieve` không trả lesson lưu trữ, lớp khác hoặc môn khác; phạm vi môn không trả học liệu của lớp; người học lớp khác không thấy học liệu của lớp; quét chạy lại không trừ credit hai lần. | BR-U05-41 |
| NFR-U05-33 | Kiểm thử thông báo: lớp khác không đọc/ghi, người học không đăng được thông báo, feed chỉ có lớp mình học hoặc dạy, payload nguy hiểm bị lọc, event U16 phát một lần sau commit, thông báo đã xóa không hiện. | US-CNT-004 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U05-13 |
| SECURITY-04 | Compliant | NFR-U05-22 |
| SECURITY-05 | Compliant | NFR-U05-20, 21 |
| SECURITY-08 | Compliant | BR-U05-01..04, NFR-U05-23 |
| SECURITY-09 | Compliant | Key trong `.env` |
| SECURITY-12 | N/A | Xác thực thuộc U01 |
| SECURITY-15 | Compliant | Lỗi không để lại kết quả dở; vượt trần báo rõ |
| RESILIENCY-06 | N/A | Health dùng chung backend |
| RESILIENCY-10 | Compliant | NFR-U05-14 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

## Announcement revision
PATCH/DELETE kiểm R3/R4 + object scope ở backend, optimistic version, đầu vào sạch, audit nguyên tử; stale version 409 không ghi dở. Chỉ tạo mới phát notification, edit/delete không gửi lặp. Actor quản lý môn không tự được sửa thông báo lớp.
