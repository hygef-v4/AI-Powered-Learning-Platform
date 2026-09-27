# U05 Content, Material & RAG - NFR Requirements

## 1. Hiệu năng và tài nguyên

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-01 | Worker chạy tối đa **4** job `RAG_INGEST` cùng lúc; cần VPS ≥ 8 GB RAM, nếu VPS nhỏ hơn hạ về 2 qua cấu hình. | Câu N2 |
| NFR-U05-02 | Trích chữ đọc file theo luồng từ U03, không nạp cả file 50 MB vào RAM; giới hạn 2 triệu ký tự chữ mỗi tài liệu, vượt thì chỉ lấy phần đầu và ghi cảnh báo. | Thiết kế |
| NFR-U05-03 | Tài liệu 50 trang lập chỉ mục xong ≤ 2 phút khi Gemini bình thường. | NFR-003 |
| NFR-U05-04 | `retrieve` p95 ≤ 1,5 s (gồm gọi Gemini tạo vector câu hỏi); tìm vector trong DB p95 ≤ 200 ms với ≤ 200 000 đoạn. | NFR-003 |
| NFR-U05-05 | Cây chương/bài và trang học viên p95 ≤ 300 ms. | NFR-003 |

## 2. Gemini và YouTube

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-10 | Embedding dùng chung trần chi phí Gemini/ngày của hệ thống (`u13.dailyCostCapUsd`, một bộ đếm Redis `gemini:daily-cost` do U13 quản lý qua `AiBudgetPort`), tính theo ngày giờ Việt Nam. | Câu N1, REL-005 |
| NFR-U05-11 | Hết hạn mức embedding nội bộ hoặc Gemini báo hết quota sau các lần retry: job ingest dừng với `errorCode = BUSY`, trạng thái `FAILED` hiển thị "Hệ thống đang bận, vui lòng thử lại sau"; người quản lý bấm Thử lại được. `retrieve` trả `503` "Hệ thống đang bận". Không trừ credit cho lời gọi chưa được Gemini xử lý. | Câu N1, quyết định đồng bộ 2026-09-25 |
| NFR-U05-12 | Kill-switch AI (cờ dùng chung với U13, FR-021) **bật** (`true`) → từ chối embedding mới với "Hệ thống đang bận" và không trừ credit; mặc định `false` là cho phép AI hoạt động. | FR-021 |
| NFR-U05-13 | `GEMINI_API_KEY` và `YOUTUBE_API_KEY` đọc từ `.env`, không commit, không log. | Câu N3, SEC-006 |
| NFR-U05-14 | Timeout: Gemini kết nối 5 s, đọc 30 s; YouTube 5 s/15 s. 429/5xx retry theo job U02; lỗi 400/403 không retry. | REL-003 |
| NFR-U05-15 | YouTube Data API dùng trong quota miễn phí 10 000 đơn vị/ngày (mỗi trang playlist 1 đơn vị). | Câu N3, REL-005 |
| NFR-U05-16 | Mỗi lời gọi Gemini embedding giữ và quyết toán credit qua U07 (1 credit = 1 000 token, làm tròn lên mỗi lần gọi); ingest tính cho người tạo nguồn, truy xuất tính cho người yêu cầu AI. Thiếu credit được báo riêng với hết hạn mức hệ thống; `requestRef` idempotent để retry không trừ trùng. | BR-U05-39, 44; BR-U07-30, 40…43 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-20 | Markdown hiển thị qua bộ lọc cho phép danh sách thẻ an toàn; không HTML thô, không `javascript:` URL. | BR-U05-20, SEC-003 |
| NFR-U05-21 | Chỉ chấp nhận URL YouTube đúng mẫu BR-U05-22; server tự dựng URL gọi API từ ID, không gọi URL người dùng nhập (tránh SSRF). | SEC-003 |
| NFR-U05-22 | CSP cho phép `frame-src https://www.youtube-nocookie.com`. | BR-U05-24, SEC-004 |
| NFR-U05-23 | `retrieve` chỉ gọi nội bộ từ U13 (không có endpoint HTTP công khai). | BR-U05-40 |
| NFR-U05-24 | Không gửi dữ liệu người dùng lên Gemini ngoài nội dung học liệu và câu hỏi truy xuất. | SEC-005 |
| NFR-U05-25 | API thông báo/hỏi đáp lớp kiểm tra người gửi và người đọc thuộc lớp đang hoạt động; chỉ giảng viên phụ trách/ADMIN đăng thông báo hoặc ẩn bài theo quyền. Nội dung Markdown được lọc trước hiển thị. | US-CNT-004, BR-U05-60…64 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-30 | Unit test mọi `BR-U05-xx`; markdown chứa script bị lọc; URL YouTube giả mạo bị từ chối. | NFR-004 |
| NFR-U05-31 | `EmbeddingPort` và `YoutubePort` có adapter giả (vector cố định, caption mẫu) cho test và khi chạy local không có key. | NFR-004, NFR-005 |
| NFR-U05-32 | Integration test: `retrieve` không trả đoạn của bài nháp, bài lưu trữ, lớp khác. | BR-U05-41 |
| NFR-U05-33 | Kiểm thử thông báo, câu hỏi và trả lời: lớp khác không đọc/ghi, payload nguy hiểm bị lọc, event U16 phát một lần sau commit và bài bị ẩn không hiển thị. | US-CNT-004 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U05-13 |
| SECURITY-04 | Compliant | NFR-U05-22 |
| SECURITY-05 | Compliant | NFR-U05-20, 21 |
| SECURITY-08 | Compliant | BR-U05-01..04, NFR-U05-23 |
| SECURITY-09 | Compliant | Key trong `.env` |
| SECURITY-12 | N/A | Xác thực thuộc U01 |
| SECURITY-15 | Compliant | Lỗi không để lại đoạn dở; vượt trần báo rõ |
| RESILIENCY-06 | N/A | Health dùng chung backend |
| RESILIENCY-10 | Compliant | NFR-U05-14 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
