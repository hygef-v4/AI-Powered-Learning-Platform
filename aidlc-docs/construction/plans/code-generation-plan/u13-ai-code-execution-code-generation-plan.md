# U13 AI & Code Execution - Code Generation Plan

**Bản tài liệu 2026-10-08**: không primary UC; primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U13. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-AIG-001, US-AIG-002, US-AIG-003, US-ASM-005.
- **Primary UC hiện hành**: Không primary UC (support). Supporting flows theo current-srs-contract.md.
- **Thiết kế nguồn**: `construction/u13-ai-code-execution/` (functional-design, nfr-requirements, nfr-design, infrastructure-design). Tham khảo code: `../demo_do_an` (`Judge0CodeRunner`, `SolutionVerifier`, `PromptInjectionScanner`, `docker-compose.yml`, `judge0.conf`).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `ClassAccessPort` | U04 | Dùng thật (giảng viên của lớp) |
| `JobPort`, `JobHandler`, `PendingSweeper`, `ScheduledScanner` | U03 | Dùng thật |
| `RagRetrievalPort` | U05 | Dùng thật |
| `BankQueryPort`, `RubricPort`, `DefinitionValidator` | U06 | Dùng thật |
| `CreditPort` | U07 | Dùng thật |
| `DocumentModelPort`, `DiagramCompactPort` | U09 | Dùng thật |
| `SubmissionQueryPort`, `GroupSubmissionQueryPort` | U11, U14 (`C`) | Dùng thật (U11, U14 ở wave 3, code trước U13) |
| `PracticeResultPort` | U15 (`C`) | Dùng thật (U15 ở wave 3, code trước U13) |
| U13 cài `AiDraftPort` (U08, U06, U09, U10), `CodeRunPort` và `PracticeGradingPort` (U11), `CodeLabCheckPort` (U08, U09), `AiUsagePort` (U05), `CreditUsagePort` (U07) | | Thay adapter tạm của U05, U06, U07, U08, U09, U10, U11 |
| `AiGradingPort`, `CodeRunPort.grade`; `CodeGradedPort` (U15 đã cài) | cho U15 | Thay adapter tạm của U15 (U15 ở wave 3, code trước U13) |

### Dữ liệu U13 sở hữu

PostgreSQL `ai_services`, `ai_suggestions`; kết quả chạy code ghi vào `attempts.run_result` (qua U11) và `questions.definition` (qua U06); Redis `gemini:daily-cost:*`, `ratelimit:ai-request:*`, `ratelimit:code-try:*`; queue `jobs.gemini`, `jobs.code`; 4 container Judge0.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  aiexecution/
    api/                AiSuggestionController, CodeRunController, AiAdminController, DTO
    application/        AiGuard, AiSuggestionService, AiUsageService, CodeRunService, CodeLabCheckService,
                        AiAdminService, CodeScorer
    ai/                 AiGateway, GeminiAdapter, FakeAiGateway, PromptBuilder,
                        InjectionScanner, OutputValidator, prompts/ (theo task, có version)
    sandbox/            CodeRunnerPort, Judge0Adapter, FakeCodeRunner, LanguageRegistry
    domain/             AiService, AiSuggestion, CodeRunResult (TRY, VERIFY, GRADE)
    infrastructure/     JPA repository
    worker/             AiTaskHandler, CodeRunHandler, AiPendingSweeper, CreditReservationScanner
    port/               AiDraftPort, AiGradingPort, PracticeGradingPort, CodeRunPort, CodeLabCheckPort,
                        AiUsagePort, CreditUsagePort
/backend/src/main/resources/db/migration/aiexecution/
/infra/judge0/judge0.conf
/frontend/src/shared/ai/        AiDraftDialog
/frontend/src/shared/codelab/   CodeEditor, CodeRunResult, VerifySolutionButton
/frontend/src/app/admin/ai/     AiUsagePage, AiSettingsDialog
/contracts/openapi/ai-execution.yaml
```

## 3. Các bước

### Nhóm A - Khung và hạ tầng

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - `pom.xml`: `networknt/json-schema-validator`. Biến cấu hình U13 theo `logical-components.md` §3.
- [ ] **Bước 2** - Docker Compose: 4 container Judge0 (theo `demo_do_an`), mạng `sandbox` `internal: true`, `backend`/`worker` nối thêm `sandbox`; `infra/judge0/judge0.conf` theo `infrastructure-design.md` §2; secret `JUDGE0_AUTH_TOKEN`.

### Nhóm B - AI

- [ ] **Bước 3** - Domain AI; `AiGateway` + `GeminiAdapter` (`generateContent`, JSON schema, token, timeout theo model) + `FakeAiGateway` (P2).
- [ ] **Bước 4** - `AiGuard`: kiểm vai trò và phạm vi nghiệp vụ; Student chỉ được `PRACTICE_GRADING` cho attempt Text/Diagram Essay của mình, các task AI khác bị từ chối; sau đó kiểm kill-switch, trần ngày Redis, rate limit và `CreditPort.reserve` theo purpose/attemptRef (P1, BR-U13-03).
- [ ] **Bước 5** - `PromptBuilder` (khối `<data>`, prompt theo task có version), `InjectionScanner`, `OutputValidator` (schema + U06/rubric) (P3, P4, BR-U13-07).
- [ ] **Bước 6** - `AiSuggestionService` + `AiTaskHandler`: `QUESTION_DRAFT` (phạm vi GV/CN môn, RAG, trích dẫn, nhận/bỏ) và `SKELETON_DRAFT` cho U09 (cây heading, hướng dẫn, gợi ý rubric từng phần; kiểm bằng `validateSkeleton`, BR-U06-30) (F1, BR-U13-10…15).
- [ ] **Bước 7** - `GRADING_PROPOSAL` cho Teacher (từng bài và `requestBatch` chấm hàng loạt, hạn 5 phút tính từ lúc xử lý từng bài) và chấm Practice cho Student (kết quả ghi `evaluations` qua `PracticeResultPort`) (văn bản phẳng + XML rút gọn U09, rubric và `RubricPort.score`); Practice tối đa một dòng hợp lệ mỗi attempt (`PRACTICE_ATTEMPT`), chỉ Student xem, ghi qua `PracticeResultPort.record`, không vào sổ điểm (F2, F3a, FR-030).
- [ ] **Bước 8** - Settle/release credit theo `credit_status`, số liệu trên `ai_suggestions`, việc idempotent, thử lại, `AiPendingSweeper` (chấm quá 5 phút → `FAILED` + trả credit; việc khác `QUEUED` quá 5 phút → gửi lại), `CreditReservationScanner` (`RESERVED` quá 30 phút) (P5, BR-U13-04…06, 08, 24).
- [ ] **Bước 9** - `AiAdminService`: sửa `ai_services` (model, bật/tắt theo việc, dòng `GLOBAL`: kill-switch, trần, tần suất), audit; `AiUsageService` cài `AiUsagePort` cho U05, thay adapter `.env` (F5, P1, BR-U13-40).

### Nhóm C - Code Lab

- [ ] **Bước 10** - `LanguageRegistry` (7 ngôn ngữ → `language_id`, kiểm `/languages` khi khởi động), `Judge0Adapter` (batch, `additional_files`, poll), `FakeCodeRunner` (P6).
- [ ] **Bước 11** - `CodeRunService`: `TRY` đồng bộ + rate limit; `VERIFY`, `GRADE` qua job; `CodeScorer`; ẩn chi tiết test ẩn (F3, F4, P7, P8, BR-U13-30…37).
- [ ] **Bước 12** - `CodeLabCheckService` (lời giải mẫu đạt, `contentHash` khớp) cài vào danh sách kiểm duyệt U08; `CodeRunPort.grade` tạo job `CODE_RUN`: bài `GRADED` từ U15 thì gọi `CodeGradedPort.onGraded`, bài `PRACTICE` từ U11 thì trả kết quả riêng cho U11.
- [ ] **Bước 13** - Thay adapter tạm: `AiDraftPort` (U06, U08, U09, U10), `CodeRunPort` (U11), `PracticeGradingPort` (U11), `AiGradingPort` (U15), `CreditUsagePort` (U07: đọc `ai_suggestions` của chính chủ ví, phân trang 20, cho bảng lần dùng credit trên AI Credits).
- [ ] **Bước 14** - Unit test mọi `BR-U13-xx` với `FakeAiGateway`/`FakeCodeRunner`.
- [ ] **Bước 15** - Tóm tắt: `aidlc-docs/construction/u13-ai-code-execution/code/business-logic-summary.md`.

### Nhóm D - Dữ liệu và tích hợp

- [ ] **Bước 16** - Flyway `V20260925_2000__u13_ai.sql` theo `infrastructure-design.md` §4 (seed `GLOBAL` với trần 2 USD và 6 loại việc; partial unique `PRACTICE_ATTEMPT` theo trạng thái hợp lệ).
- [ ] **Bước 17** - JPA repository.
- [ ] **Bước 18** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test: AI bị từ chối không trừ credit; lỗi release credit; trần ngày và quota Gemini báo "Hệ thống đang bận"; U05 embedding và U13 tạo nội dung dùng `requestRef` riêng, không trừ trùng khi retry. Chấm Practice: đủ/thiếu credit, quá 5 phút → `FAILED` và trả credit rồi bấm lại được, chỉ một kết quả hợp lệ mỗi attempt. Judge0 thật: 7 ngôn ngữ, đúng/sai/quá giờ/quá bộ nhớ, mã mở mạng bị chặn.
- [ ] **Bước 19** - Tóm tắt: `code/repository-summary.md`.

### Nhóm E - API

- [x] **Bước 20** - `/contracts/openapi/ai-execution.yaml` (U13 không phát event).
- [ ] **Bước 21** - Controller + DTO + validation.
- [ ] **Bước 22** - Test MockMvc: Student chỉ xem kết quả AI Practice của chính mình, không gọi task AI khác hoặc đọc đề xuất của Teacher; Code Lab `TRY` dùng Judge0 không trừ credit; không phải ADMIN không sửa cấu hình; `TRY` quá 5/phút `429`.
- [ ] **Bước 23** - Tóm tắt: `code/api-summary.md`.

### Nhóm F - Frontend

- [ ] **Bước 24** - `AiDraftDialog` (credit ước tính, trích dẫn, chọn câu) gắn vào U08, U06, U10; API đề xuất chấm cho `GradeWithAiDialog` của U11 (UC 25, 35).
- [ ] **Bước 25** - `CodeEditor` (Monaco, nhiều file), `CodeRunResult`, `VerifySolutionButton`; gắn vào `CodeWorkspace` (U11), trình soạn câu `CODE` (U06) và `CodeLabConfigForm` (U09).
- [ ] **Bước 26** - `AiUsagePage` (màn AI Usage từ Admin Sidebar) và `AiSettingsDialog` (popup AI Setting trên AI Usage).
- [ ] **Bước 27** - Test frontend: dialog phân biệt "Không đủ credit AI", "Hệ thống đang bận" và "AI đang tắt"; kết quả test ẩn chỉ đạt/không.
- [ ] **Bước 28** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm G - Hoàn tất

- [ ] **Bước 29** - Cập nhật `README.md`: chạy Judge0 (privileged, mạng sandbox), kiểm ngôn ngữ, cấu hình model/trần/kill-switch, chạy local với AI giả.
- [ ] **Bước 30** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-AIG-001 (UC 38, 39, 40, 41, 42, 43, luồng AI) | 4, 5, 6, 24 |
| US-AIG-002 (UC 53, 54, luồng AI) | 6, 24 |
| US-AIG-003 (vận hành AI (không UC trực tiếp)) | 4, 9, 26 |
| US-ASM-005 (UC 41, phần chạy kiểm; U09 chủ trì) | 2, 10, 11, 12, 25 |
| Chấm code tự động, đề xuất chấm | 7, 11, 12 |

## 5. Ngoài phạm vi

- Quyết định dùng đề xuất chấm và chốt điểm (U15); lưu câu AI vào bài/ngân hàng/template (U08/U06/U10).

## 6. Revision implementation scope - 2026-10-08
- [ ] Không primary UC; hỗ trợ AI UC 25/35, draft UC 39–43/54/56, Judge0 UC 21/41. Admin vận hành AI là US-AIG-003, không tạo UC riêng.
- [ ] Unit gọi xác minh R2/R3/R4/R5; giữ provider-neutral business descriptions và provider Gemini trong adapter/config.
