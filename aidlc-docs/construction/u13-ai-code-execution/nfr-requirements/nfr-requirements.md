# U13 AI & Code Execution - NFR Requirements

**Bản tài liệu 2026-10-09**: không primary UC; phần chạy AI và Judge0 của UC 25, 29, 38, 43, nhóm AI của Settings UC 70–71, luồng phụ AI soạn đề (UC 35, 42–45, 57) và tóm tắt/embedding học liệu (UC 34, 55) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U13-01 | Đề xuất 10 câu hỏi hoàn tất ≤ 60 s (p90) khi Gemini bình thường. | NFR-003 |
| NFR-U13-02 | Đề xuất chấm một bài tài liệu ≤ 20 trang ≤ 120 s (p90). | NFR-003 |
| NFR-U13-03 | Chạy thử code (`TRY`, UC 25) trả kết quả ≤ 10 s (p90) với ≤ 10 test công khai. | UC 25 |
| NFR-U13-04 | Chấm code (`GRADE`) 50 test ≤ 2 phút; 100 bài nộp dồn cuối hạn xử lý hết ≤ 30 phút. Kiểm lời giải mẫu (`VERIFY`, UC 43) ≤ 2 phút. | NFR-003 |
| NFR-U13-05 | Worker chạy tối đa 3 job AI và 2 job chạy code cùng lúc (cấu hình). | Tài nguyên VPS |
| NFR-U13-06 | `quote`, `hold` của `AiUsagePort` p95 ≤ 100 ms (chạy trong request tải học liệu). | BR-U13-50, 51 |

## 2. Chi phí và giới hạn

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U13-10 | Trần chi phí AI ngày là mục `ai.dailyCostCapUsd` trên Settings (mặc định 2 USD, Admin sửa trên Setting Detail), ước tính theo bảng giá model `U13_MODEL_PRICES`; đạt trần → từ chối mới với "Hệ thống đang bận", không trừ credit người dùng; job đang chạy được hoàn tất; quét học liệu của U05 sang `BUSY`. | REL-005, FR-021, FR-033 |
| NFR-U13-11 | Ước tính token trước khi gọi để `reserve` credit: độ dài prompt/4 + `maxOutputTokens`; mức giữ khi tải học liệu tính theo BR-U13-50. | U07 |
| NFR-U13-12 | Timeout Gemini: kết nối 5 s, đọc 90 s (`pro`: 150 s). | REL-003 |
| NFR-U13-13 | Judge0 gọi qua mạng nội bộ, timeout 30 s mỗi lô; Judge0 không phản hồi → `SANDBOX_ERROR` sau 3 lần retry. | REL-003 |
| NFR-U13-14 | Thay đổi cài đặt AI (kill-switch, trần, tần suất, model) có hiệu lực ở mọi instance trong tối đa 30 giây (cache của `SettingsPort`). | BR-U03-86 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U13-20 | Judge0 nằm trong mạng Docker riêng `sandbox`, không route ra Internet, không truy cập được `postgres`, `redis`, `rabbitmq` của hệ thống; chỉ `worker` và `backend` nói chuyện với Judge0 server. | US-ASM-005 S2, SEC-003 |
| NFR-U13-21 | Judge0 bật giới hạn per-process (thời gian, bộ nhớ, số tiến trình, kích thước file), tắt `enable_network`. | BR-U13-32 |
| NFR-U13-22 | `GEMINI_API_KEY` chỉ ở backend/worker; `JUDGE0_AUTH_TOKEN` cho server Judge0. Không log, không đưa vào Settings. | SEC-006, BR-U03-85 |
| NFR-U13-23 | Lời giải mẫu, test ẩn và đề xuất AI cho bài `GRADED` không bao giờ ra API Student. Kết quả `PRACTICE` chỉ chủ attempt xem được. | SEC-002, FR-030 |
| NFR-U13-24 | Đầu ra AI kiểm bằng JSON schema và quy tắc nghiệp vụ trước khi lưu; văn bản AI hiển thị dạng văn bản thuần/markdown đã làm sạch. | SEC-003 |
| NFR-U13-25 | Kiểm vai trò và phạm vi ở backend trước mọi lời gọi AI hoặc Judge0: `ADMIN` bị từ chối mọi API AI và chạy code; Student chỉ `TRY` lượt của mình và `PRACTICE_GRADING`; R2/R3/R4 đọc hiện thời, không dựa cache vai trò. | SECURITY-08, current SRS contract |

## 4. Khả dụng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U13-30 | Gemini không khả dụng hoặc AI bị tắt: chức năng không dùng AI (soạn tay, chấm tay, tải học liệu) vẫn chạy đầy đủ. | US-AIG-003 S3 |
| NFR-U13-31 | Judge0 không khả dụng: nộp bài vẫn lưu (U11); chấm code chờ và retry; giảng viên thấy "chưa chấm được"; kiểm lời giải báo "Chưa chạy được". | REL-003 |

## 5. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U13-40 | Unit test mọi `BR-U13-xx` với `FakeAiGateway` (trả JSON cố định/sai định dạng/timeout), `FakeCodeRunner` và `SettingsPort` giả. | NFR-004 |
| NFR-U13-41 | Kịch bản Judge0 thật (7 ngôn ngữ chạy "hello" + một test đúng/sai/quá giờ/quá bộ nhớ; mã cố mở kết nối mạng bị chặn) chuyển cho tester; unit không viết integration test. | US-ASM-005, quyết định 2026-10-05 |
| NFR-U13-42 | Test trừ credit: thành công settle đúng, lỗi release, bị từ chối không trừ; giữ khi tải học liệu: thiếu credit từ chối, `NO_TEXT` trả toàn bộ, thử lại không trừ trùng, quá 25 giờ tự trả. | U07, BR-U13-50…53 |

## 6. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U13-22, `ai_suggestions` không lưu prompt thô |
| SECURITY-05 | Compliant | NFR-U13-24; giá trị cài đặt do U03 kiểm theo `SettingDefinition` |
| SECURITY-08 | Compliant | NFR-U13-23, 25 |
| SECURITY-09 | Compliant | Key trong `.env` |
| SECURITY-15 | Compliant | NFR-U13-30, 31 |
| RESILIENCY-06 | Compliant | Health Judge0 trong health backend (không làm backend `DOWN`) |
| RESILIENCY-10 | Compliant | NFR-U13-12, 13 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
