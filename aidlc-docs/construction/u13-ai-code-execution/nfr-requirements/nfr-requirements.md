# U13 AI & Code Execution - NFR Requirements

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U13-01 | Đề xuất 10 câu hỏi hoàn tất ≤ 60 s (p90) khi Gemini bình thường. | NFR-003 |
| NFR-U13-02 | Đề xuất chấm một bài tài liệu ≤ 20 trang ≤ 120 s (p90). | NFR-003 |
| NFR-U13-03 | Chạy thử code (`TRY`) trả kết quả ≤ 10 s (p90) với ≤ 10 test công khai. | UC-ASM-13 |
| NFR-U13-04 | Chấm code (`GRADE`) 50 test ≤ 2 phút; 100 bài nộp dồn cuối hạn xử lý hết ≤ 30 phút. | NFR-003 |
| NFR-U13-05 | Worker chạy tối đa 3 job AI và 2 job chạy code cùng lúc (cấu hình). | Tài nguyên VPS |

## 2. Chi phí và giới hạn

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U13-10 | Trần chi phí AI ngày mặc định 2 USD (`AI_DAILY_COST_CAP_USD`), ước tính theo bảng giá model trong cấu hình; đạt trần → từ chối mới, job đang chạy được hoàn tất. | REL-005, FR-021 |
| NFR-U13-11 | Ước tính token trước khi gọi để `reserve` credit: độ dài prompt/4 + `maxOutputTokens`. | U07 |
| NFR-U13-12 | Timeout Gemini: kết nối 5 s, đọc 90 s (`pro`: 150 s). | REL-003 |
| NFR-U13-13 | Judge0 gọi qua mạng nội bộ, timeout 30 s mỗi lô; Judge0 không phản hồi → `SANDBOX_ERROR` sau 3 lần retry. | REL-003 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U13-20 | Judge0 nằm trong mạng Docker riêng `sandbox`, không route ra Internet, không truy cập được `postgres`, `redis`, `rabbitmq` của hệ thống; chỉ `worker` và `backend` nói chuyện với Judge0 server. | US-ASM-005 S2, SEC-003 |
| NFR-U13-21 | Judge0 bật giới hạn per-process (thời gian, bộ nhớ, số tiến trình, kích thước file), tắt `enable_network`. | BR-U13-32 |
| NFR-U13-22 | `GEMINI_API_KEY` chỉ ở backend/worker; `JUDGE0_AUTH_TOKEN` cho server Judge0. Không log. | SEC-006 |
| NFR-U13-23 | Lời giải mẫu, test ẩn, đề xuất AI không bao giờ ra API người học. | SEC-002 |
| NFR-U13-24 | Đầu ra AI kiểm bằng JSON schema và quy tắc nghiệp vụ trước khi lưu; văn bản AI hiển thị dạng văn bản thuần/markdown đã làm sạch. | SEC-003 |

## 4. Khả dụng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U13-30 | Gemini không khả dụng: chức năng không dùng AI (soạn tay, chấm tay) vẫn chạy đầy đủ. | US-AIG-003 S3 |
| NFR-U13-31 | Judge0 không khả dụng: nộp bài vẫn lưu (U11); chấm code chờ và retry; giảng viên thấy "chưa chấm được". | REL-003 |

## 5. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U13-40 | Unit test mọi `BR-U13-xx` với `FakeAiGateway` (trả JSON cố định/sai định dạng/timeout) và `FakeCodeRunner`. | NFR-004 |
| NFR-U13-41 | Integration test Judge0 thật (Testcontainers hoặc compose test): 7 ngôn ngữ chạy "hello" + một test đúng/sai/quá giờ/quá bộ nhớ; mã cố mở kết nối mạng bị chặn. | US-ASM-005 |
| NFR-U13-42 | Test trừ credit: thành công settle đúng, lỗi release, bị từ chối không trừ. | U07 |

## 6. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U13-22, `AiCall` không lưu nội dung |
| SECURITY-05 | Compliant | NFR-U13-24 |
| SECURITY-08 | Compliant | NFR-U13-23 |
| SECURITY-09 | Compliant | Key trong `.env` |
| SECURITY-15 | Compliant | NFR-U13-30, 31 |
| RESILIENCY-06 | Compliant | Health Judge0 trong health backend (không làm backend `DOWN`) |
| RESILIENCY-10 | Compliant | NFR-U13-12, 13 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
