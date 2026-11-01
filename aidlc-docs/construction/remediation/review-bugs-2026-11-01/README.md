# Hồ sơ sửa lỗi review toàn dự án — 2026-11-01

Nhánh: `fix/review-bugs-2026-11-01`, commit `1cd58c02`. Phạm vi: 26 lỗi tìm được khi review toàn dự án trên `aa30ad60`, cộng lỗi CSP của trình xem DOCX (ban đầu chỉ là nghi vấn, kiểm lại thì có thật). Lỗi nào đã được sửa trên upstream trước khi bắt đầu thì không nằm trong danh sách.

## Thay đổi và tài liệu đã cập nhật

| Unit | Thay đổi hành vi | Business rule / tài liệu |
|---|---|---|
| U01 | Trừ lượt và so OTP là một bước nguyên tử trên Redis (Lua); mỗi IP nhập OTP tối đa 30 lần mỗi 15 phút, vượt thì 429 `OTP_TOO_MANY_ATTEMPTS` | BR-U01-22 |
| U01 | `?next=` chỉ nhận đường dẫn cùng origin, bỏ ký tự điều khiển, khoảng trắng, `\` (chặn open redirect) | [frontend-components](../../u01-account-and-access/functional-design/frontend-components.md) LoginPage |
| U03 | Upload: U01 báo không kiểm được quyền thì 503 `UPLOAD_AUTHORIZATION_UNAVAILABLE`, không audit từ chối | BR-U03-04 |
| U03 | Tệp tải lên mà không ai gắn được dọn sau khi `FileRef` hết hạn cộng 10 phút (`UnattachedFiles`, `UnattachedFileScanner`); gắn tệp cần Redis, lỗi thì 503 | BR-U03-07 (thay quyết định cũ "không dọn tệp chưa gắn") |
| U03 | Listener chờ 5 giây trước khi NACK requeue | BR-U03-53 |
| U03 | Handler quá giờ chưa dừng thì cùng việc không chạy song song lần hai | BR-U03-56 |
| U03 | Sweeper gửi lại cùng việc tối đa một lần mỗi 20 phút; sweeper backup `for update skip locked` | BR-U03-58 |
| U03 | Hàng chờ gửi đầy thì từ chối và ghi log WARN/ERROR theo việc có sweeper hay không | [nfr-design-patterns](../../u03-job-and-settings/nfr-design/nfr-design-patterns.md) P8 |
| U03 | Cache cài đặt xóa sau commit | BR-U03-86 |
| U03 | Frontend: upload gặp 401 làm mới phiên và gửi lại một lần; `usePollStatus` poll tiếp khi lỗi tạm | [frontend-components](../../u03-job-and-settings/functional-design/frontend-components.md), [code-summary](../../u03-job-and-settings/code/code-summary.md) |
| U05 | Lớp `ARCHIVED` mở lại thành `OPEN` cũng phát `enrollment.activated` | BR-U05-26 |
| U06 | Message đến lại khi lần gọi provider có thể còn chạy (trong `requestTimeout` + 30 giây) thì giữ `RUNNING`, quá cửa sổ mới `UNKNOWN` | BR-U06-22, [domain-entities](../../u06-ai-connections/functional-design/domain-entities.md), [nfr-design-patterns](../../u06-ai-connections/nfr-design/nfr-design-patterns.md) |
| U06 | Token ra của Gemini gồm cả `thoughtsTokenCount` | BR-U06-15 |
| U07 | Quyền bài tập: dependency lỗi thì 503 `ASSIGNMENT_DEPENDENCY_UNAVAILABLE` | BR-U07-72 |
| U07/U13 | Bộ trích chữ đủ 2 việc thì 503 `ASSIGNMENT_SOURCE_BUSY` thay vì báo tệp hỏng | [U07 code-summary](../../u07-assignment-authoring/code/code-summary.md) |
| U09 | Sổ điểm và `/submission-progress` chỉ lấy bài `GRADED` đã giao (`OPEN`, `CLOSED`, `RETIRED`) | BR-U09-60 |
| U10 | Quiz `OPEN` đã có lượt làm không sửa câu được: 409 `QUIZ_HAS_ATTEMPTS` (tạo version mới); không kiểm được lượt làm thì 503 `QUIZ_DEPENDENCY_UNAVAILABLE`. Contract mới `IQuizAttemptPresence` do U11 cài | BR-U10-13, các đoạn trạng thái Quiz trong U10 |
| U10 | Mở lại version đã phát hành khi chuỗi có version khác đang `OPEN`: 409 `QUIZ_OTHER_VERSION_OPEN` | BR-U10-23, F10 trong [business-logic-model](../../u10-quiz-authoring/functional-design/business-logic-model.md) |
| U11 | Xem đề, lưu, nộp, ghi hoạt động của lượt đang làm kiểm lại ghi danh lớp mỗi lần; kết quả cũ vẫn xem được | BR-U11-12 (BR-U11-20 giữ nguyên) |
| U12 | Tự nộp tính trễ theo lần sửa cuối của nhóm | BR-U12-43 |
| U12 | Nháp được làm sạch như bài cá nhân; không trả `fileId`; endpoint ảnh `GET /api/v1/group-docs/{id}/blocks/{blockId}/image` | BR-U12-35, [api-inventory](../../../inception/reverse-engineering/api-inventory.md) |
| U12 | SSE đóng hẳn (401) thì làm mới phiên và nối lại có backoff tối đa 30 giây | BR-U12-37, [frontend-components](../../u12-group-assignment/functional-design/frontend-components.md) |
| U13 | Môn `ACTIVE` chưa có module thì tự tạo "Module 1" khi staff mở trang học liệu | BR-U13-M02 |
| U13 | Style của `docx-preview` chèn lại kèm nonce; CSP `font-src 'self' blob:` | [U13 code-summary](../../u13-reference-materials/code/code-summary.md) |
| U14 | Sửa thông báo lớp trả `version` mới | BR-U14-04 |
| Frontend | Bỏ 2 import thừa gây cảnh báo lint | Không đổi hành vi |

## Kiểm tra

- Frontend: `npm ci`, eslint 0 lỗi, tsc 0 lỗi, vitest 605/605 (96 tệp), production build đạt.
- Backend: máy không có Maven. Code và unit test được biên dịch bằng `javac` với thư viện lấy từ boot jar cũ; `json-schema-validator` chỉ có bản giả để biên dịch. Chạy 1.515 unit test: 1.387 đạt, 128 lỗi do môi trường (thiếu `json-smart`, bản giả JSON Schema, classpath quá dài cho JVM con của bộ trích chữ). Không lỗi nào thuộc các test đã sửa hoặc thêm.
- Chưa chạy: `mvn verify`, test tích hợp Testcontainers, `ArchitectureTest`, `QuizMcpToolsSchemaTest`. Cần chạy trước khi merge.

## Lưu ý còn lại

- Giao diện chỉ hiện lỗi `QUIZ_HAS_ATTEMPTS`, chưa có nút tạo version mới ngay trong thông báo.
- `pptx-preview` chèn chữ slide bằng `innerHTML`; CSP chặn được script nhưng nên cân nhắc thay thư viện.
