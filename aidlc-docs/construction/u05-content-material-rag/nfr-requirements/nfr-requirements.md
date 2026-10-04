# U05 Content, Material & RAG - NFR Requirements

## 1. Hiệu năng và tài nguyên

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-01 | Worker quét tối đa **4** lesson cùng lúc; cần VPS ≥ 8 GB RAM, nếu VPS nhỏ hơn hạ về 2 qua cấu hình. | Câu N2 |
| NFR-U05-02 | Trích chữ đọc file theo luồng từ U03, không nạp cả file 50 MB vào RAM; lưu tối đa 2 000 000 ký tự mỗi lesson, vượt thì chỉ lấy phần đầu. | Thiết kế |
| NFR-U05-03 | Tài liệu 50 trang quét xong ≤ 1 phút khi Gemini bình thường. | NFR-003 |
| NFR-U05-04 | `retrieve` p95 ≤ 1,5 s (gồm gọi Gemini tạo vector câu hỏi); tìm vector trong DB p95 ≤ 200 ms với ≤ 50 000 lesson. | NFR-003 |
| NFR-U05-05 | Danh sách module/lesson trong Class Detail p95 ≤ 300 ms; danh sách thông báo kèm 2 bình luận mới nhất mỗi thông báo p95 ≤ 300 ms. | NFR-003 |

## 2. Gemini và YouTube

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-10 | Mỗi lần embedding đi qua `AiUsagePort` của U13: kiểm AI bật (`ai_services`), trần chi phí Gemini/ngày dùng chung (Redis do U13 quản lý), giữ/trừ credit và ghi `ai_suggestions`. | Câu N1, REL-005 |
| NFR-U05-11 | Hết trần hoặc Gemini báo hết quota sau các lần thử lại: lesson sang `BUSY` hiển thị "Hệ thống đang bận"; `retrieve` trả `503`. Không trừ credit cho lời gọi chưa được Gemini xử lý. | Câu N1 |
| NFR-U05-13 | `GEMINI_API_KEY` và `YOUTUBE_API_KEY` đọc từ `.env`, không commit, không log. | Câu N3, SEC-006 |
| NFR-U05-14 | Timeout: Gemini kết nối 5 s, đọc 30 s; YouTube 5 s/15 s. 429/5xx thử lại theo U03; lỗi 400/403 không thử lại. | REL-003 |
| NFR-U05-16 | Credit embedding: 1 credit = 1 000 token, làm tròn lên mỗi lần gọi; quét tính cho người tải lên, truy xuất tính cho người yêu cầu AI; thử lại không trừ trùng (U13 giữ một dòng `ai_suggestions` cho mỗi lượt quét). | BR-U05-39, 44 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-20 | Markdown của thông báo hiển thị qua bộ lọc; bình luận là văn bản thuần, escape khi hiển thị; bộ lọc cho phép danh sách thẻ an toàn; không HTML thô, không `javascript:` URL. | BR-U05-62, SEC-003 |
| NFR-U05-21 | Chỉ chấp nhận URL YouTube đúng mẫu BR-U05-22; server tự dựng URL gọi API từ `videoId`, không gọi URL người dùng nhập (tránh SSRF). | SEC-003 |
| NFR-U05-22 | CSP cho phép `frame-src https://www.youtube-nocookie.com`. | BR-U05-24, SEC-004 |
| NFR-U05-23 | `retrieve` chỉ gọi nội bộ từ U13 (không có endpoint HTTP công khai). | BR-U05-40 |
| NFR-U05-24 | Không gửi dữ liệu người dùng lên Gemini ngoài nội dung học liệu và câu hỏi truy xuất. | SEC-005 |
| NFR-U05-25 | API thông báo và bình luận kiểm tra người gửi và người đọc thuộc lớp đang hoạt động; chỉ giảng viên phụ trách lớp đăng thông báo hoặc ẩn bài. | US-CNT-004, BR-U05-60…64 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-30 | Unit test mọi `BR-U05-xx`; markdown chứa script bị lọc; URL YouTube giả mạo hoặc playlist bị từ chối. | NFR-004 |
| NFR-U05-31 | `EmbeddingPort` và `YoutubePort` có adapter giả (vector cố định, phụ đề mẫu) cho test và khi chạy local không có key. | NFR-004, NFR-005 |
| NFR-U05-32 | Integration test: `retrieve` không trả lesson lưu trữ, lớp khác hoặc môn khác; phạm vi môn không trả học liệu của lớp; người học lớp khác không thấy học liệu của lớp; quét chạy lại không trừ credit hai lần. | BR-U05-41 |
| NFR-U05-33 | Kiểm thử thông báo và bình luận: lớp khác không đọc/ghi, người học không đăng được thông báo, bình luận không tạo thông báo, payload nguy hiểm bị lọc, event U16 phát một lần sau commit và bài bị ẩn không hiển thị. | US-CNT-004 |

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
