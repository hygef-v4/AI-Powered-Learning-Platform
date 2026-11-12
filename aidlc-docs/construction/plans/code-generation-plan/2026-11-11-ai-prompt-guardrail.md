# Kế hoạch — Guardrail hai tầng cho prompt người dùng (2026-11-11)

Nhánh `feat/ai-prompt-guardrail` tạo từ `main@8923d45d` (không theo dõi upstream). Thiết kế và vị trí file: [U06 ai-prompt-guardrail](../../u06-ai-connections/code/ai-prompt-guardrail.md).

## Phân tích yêu cầu (mức Standard)

Yêu cầu của người dùng đã đặc tả đủ: hai tầng tuần tự, fail-closed, 4 phân loại, 4 phạm vi, HTTP 400 thân thiện, không tốn credit, tiện ích ghép prompt có thẻ phân định, unit test. Các quyết định tự đưa ra do yêu cầu viết cho stack Next.js nhưng dự án là Spring Boot:

| Điểm | Quyết định | Lý do |
|---|---|---|
| Ngôn ngữ/vị trí | Java trong module `ai` (U06): interface ở `ai.contract`, cài đặt ở `ai.service` | Backend là nơi duy nhất gọi AI; ArchUnit buộc unit khác chỉ dùng `contract` |
| Điểm áp dụng | Gọi sớm trong service trước khi lưu draft, và kiểm lại tập trung trong `AiTaskService.submit` | Chặn trước khi có side effect; không unit nào vượt được guardrail |
| Dữ liệu không phải chữ tự do | Bài nộp, tài liệu nguồn không qua bộ lọc, chỉ đóng thẻ | Tránh chặn nhầm bài môn CSDL/bảo mật có SQL/script hợp lệ |
| Fail-closed | Lỗi khi kiểm → `HARMFUL_MALICIOUS`, mã `T2_EVALUATION_ERROR` | Chỉ 4 phân loại được phép; không lỗi nào được cho qua |
| Phạm vi chưa có điểm vào | `GRADING_RUBRIC`, `STUDENT_FEEDBACK`, `GENERAL` có chính sách và test nhưng chưa nối API | Hệ thống hiện không có ô nhập tương ứng |

User Stories, Application Design, Units Generation, NFR, Infrastructure: bỏ qua (thay đổi trong ranh giới U06 và các service gọi hiện có, không có màn hình mới).

## Các bước

- [x] Khảo sát điểm nhận prompt: 3 điểm có chữ tự do (soạn đề, sửa đề, soạn quiz) + lượt tạo lại quiz; chấm bài và góp ý chỉ có dữ liệu
- [x] Contract: `IPromptGuardrail`, `UserPromptInput`, `PromptContext`, `PromptAction`, `PromptFlowState`, `GuardrailCategory`, `GuardrailVerdict`, `UntrustedPrompt`; `AiTaskRequest` thêm `userInput`
- [x] Tầng 1: `PromptTextNormalizer`, `PromptRuleScanner`, `PromptContextPolicy`
- [x] Tầng 2: `PromptTextAnalysis`, `PromptLexicon`, `PromptFlowMachine`, `PromptPolicyEngine`
- [x] Pipeline `PromptGuardrailService` (tuần tự, fail-closed, log không chứa nội dung)
- [x] Nối guardrail vào `AssignmentAiDraftService`, `QuizAiDraftService`, `QuizSourceRetry`, `AiTaskService`
- [x] Ghép prompt bằng `UntrustedPrompt` cho soạn đề, sửa đề, soạn quiz, góp ý luyện tập, system prompt chấm bài
- [x] Unit test: `PromptGuardrailServiceTest` (56 ca), `UntrustedPromptTest` (6), 4 ca mới trong `AiTaskFlowTest`, 2 ca trong `QuizAiDraftServiceTest`, 1 ca trong `AssignmentAiRevisionTest`
- [x] Chạy test liên quan: guardrail, AI task, quiz, assignment, practice feedback, grading, ArchitectureTest
- [x] Chạy toàn bộ `mvn verify` backend (kết quả ghi bên dưới)
- [x] Kiểm trên trình duyệt thông báo lỗi ở màn sửa đề (2026-11-12)

## Kết quả kiểm tra

`mvn -B -o verify` trong image `maven:3.9-eclipse-temurin-17` (mount cả repo và Docker socket cho Testcontainers), 2026-11-11: 2.220 test, 0 lỗi, 0 bỏ qua, BUILD SUCCESS, gồm ArchitectureTest. Một assert cũ trong `QuizAiDraftServiceTest` được đổi từ `</ material>` sang `&lt;/material>` do escape mới chặt hơn. Chưa gọi nhà cung cấp AI thật.

Trình duyệt (2026-11-12): backend của nhánh chạy riêng ở cổng 8081 (Flyway tắt để không đổi DB local dùng chung), frontend dev trỏ tới đó, đăng nhập giảng viên test. Ô "Sửa đề bằng AI" của một đề nháp nhận yêu cầu injection tiếng Việt: API trả 400, khung chat hiện câu báo thân thiện màu đỏ kèm "Đề chưa thay đổi."; DB không có bản đề xuất hay AI task mới; log ghi `tier=1 rule=T1_OVERRIDE_VI`, không ghi nội dung. Không gửi yêu cầu hợp lệ để tránh gọi nhà cung cấp AI thật. Container tạm đã xóa.
