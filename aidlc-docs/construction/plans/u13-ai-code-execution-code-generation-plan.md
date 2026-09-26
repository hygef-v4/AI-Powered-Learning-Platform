# U13 AI & Code Execution - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U13. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-AIG-001, US-AIG-002, US-AIG-003, US-ASM-005. **Use case**: UC-AIG-01..03, UC-ASM-05; phần chạy thử của UC-ASM-13.
- **Thiết kế nguồn**: `construction/u13-ai-code-execution/` (functional-design, nfr-requirements, nfr-design, infrastructure-design). Tham khảo code: `../demo_do_an` (`Judge0CodeRunner`, `SolutionVerifier`, `PromptInjectionScanner`, `docker-compose.yml`, `judge0.conf`).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `JobPort`, `AuditPort`, `EventPublisherPort` | U02 | Dùng thật |
| `RagRetrievalPort` | U05 | Dùng thật |
| `BankQueryPort`, `RubricPort`, `DefinitionValidator` | U06 | Dùng thật |
| `CreditPort` | U07 | Dùng thật |
| `DocumentModelPort`, `DiagramCompactPort` | U09 | Dùng thật |
| `SubmissionQueryPort`, `GroupSubmissionQueryPort` | U11, U14 (`C`) | U11/U14 code sau U13: adapter tạm báo "chưa hỗ trợ"; U11/U14 cắm adapter thật khi được code |
| U13 cài `AiDraftPort` (U08, U06, U10), `CodeRunPort` (U11), `CodeLabCheckPort` (U08), `AiBudgetPort` (U05) | | Thay adapter tạm của U05, U06, U08, U10, U11 |
| `AiGradingPort`, `CodeRunPort.grade`; khai báo `CodeGradedPort` (U15 cài, adapter rỗng tới khi có U15) | cho U15 | Các unit đó dùng khi được code |

### Dữ liệu U13 sở hữu

PostgreSQL `ai_task_configs`, `ai_calls`, `ai_proposals`, `code_runs`; khóa `u13.*` trong `app_settings`; Redis `gemini:daily-cost:*`, `ratelimit:ai-request:*`, `ratelimit:code-try:*`; queue `jobs.gemini`, `jobs.code`; 4 container Judge0.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  u13/
    api/                AiProposalController, CodeRunController, AiAdminController, DTO
    application/        AiGuard, AiProposalService, CodeRunService, CodeLabCheckService,
                        AiAdminService, CodeScorer
    ai/                 AiGateway, GeminiAdapter, FakeAiGateway, PromptBuilder,
                        InjectionScanner, OutputValidator, prompts/ (theo task, có version)
    sandbox/            CodeRunnerPort, Judge0Adapter, FakeCodeRunner, LanguageRegistry
    domain/             AiTaskConfig, AiGlobalSettings, AiCall, AiProposal,
                        CodeRun (gồm lần VERIFY = kiểm lời giải mẫu)
    infrastructure/     JPA repository
    worker/             AiTaskHandler, CodeRunHandler
    port/               AiDraftPort, AiGradingPort, CodeRunPort, CodeLabCheckPort, AiBudgetPort
/backend/src/main/resources/db/migration/u13/
/infra/judge0/judge0.conf
/frontend/src/shared/ai/        AiDraftDialog, AiGradingPanel
/frontend/src/shared/codelab/   CodeEditor, CodeRunResult, VerifySolutionButton
/frontend/src/app/admin/ai/     AiSettingsPage, AiUsageDashboard
/contracts/openapi/u13-ai-code.yaml
/contracts/messages/u13-events.json
```

## 3. Các bước

### Nhóm A - Khung và hạ tầng

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - `pom.xml`: `networknt/json-schema-validator`. Biến cấu hình U13 theo `logical-components.md` §3.
- [ ] **Bước 2** - Docker Compose: 4 container Judge0 (theo `demo_do_an`), mạng `sandbox` `internal: true`, `backend`/`worker` nối thêm `sandbox`; `infra/judge0/judge0.conf` theo `infrastructure-design.md` §2; secret `JUDGE0_AUTH_TOKEN`.

### Nhóm B - AI

- [ ] **Bước 3** - Domain AI; `AiGateway` + `GeminiAdapter` (`generateContent`, JSON schema, token, timeout theo model) + `FakeAiGateway` (P2).
- [ ] **Bước 4** - `AiGuard` (kill-switch, trần ngày Redis, rate limit, `CreditPort.reserve`) (P1, BR-U13-03).
- [ ] **Bước 5** - `PromptBuilder` (khối `<data>`, prompt theo task có version), `InjectionScanner`, `OutputValidator` (schema + U06/rubric) (P3, P4, BR-U13-07).
- [ ] **Bước 6** - `AiProposalService` + `AiTaskHandler`: `QUESTION_DRAFT` (phạm vi GV/CN môn, RAG, trích dẫn, nhận/bỏ) (F1, BR-U13-10…15).
- [ ] **Bước 7** - `GRADING_PROPOSAL` (văn bản phẳng + XML rút gọn U09, code + kết quả test, `RubricPort.score`) (F2, BR-U13-20…23).
- [ ] **Bước 8** - Settle/release credit, `AiCall`, job idempotent, retry (P5, BR-U13-04…06, 08).
- [ ] **Bước 9** - `AiAdminService`: cấu hình theo việc, trần, kill-switch (cài `AiBudgetPort` cho U05: kill-switch và trần chi phí Gemini dùng chung, thay adapter `.env` của U05), báo cáo (F5, BR-U13-40, 41).

### Nhóm C - Code Lab

- [ ] **Bước 10** - `LanguageRegistry` (7 ngôn ngữ → `language_id`, kiểm `/languages` khi khởi động), `Judge0Adapter` (batch, `additional_files`, poll), `FakeCodeRunner` (P6).
- [ ] **Bước 11** - `CodeRunService`: `TRY` đồng bộ + rate limit; `VERIFY`, `GRADE` qua job; `CodeScorer`; ẩn chi tiết test ẩn (F3, F4, P7, P8, BR-U13-30…37).
- [ ] **Bước 12** - `CodeLabCheckService` (lời giải mẫu đạt, `contentHash` khớp) cài vào danh sách kiểm duyệt U08; `CodeRunPort.grade` (U15 gọi khi nộp bài `CODE_LAB`) tạo job `CODE_RUN`, xong thì gọi `CodeGradedPort.onGraded` trong transaction kết thúc job.
- [ ] **Bước 13** - Thay adapter tạm: `AiDraftPort` (U06, U08, U10), `CodeRunPort` (U11).
- [ ] **Bước 14** - Unit test mọi `BR-U13-xx` với `FakeAiGateway`/`FakeCodeRunner`.
- [ ] **Bước 15** - Tóm tắt: `aidlc-docs/construction/u13-ai-code-execution/code/business-logic-summary.md`.

### Nhóm D - Dữ liệu và tích hợp

- [ ] **Bước 16** - Flyway `V20260925_2000__u13_ai_code.sql` (seed 4 việc, seed khóa `u13.killSwitch`, `u13.dailyCostCapUsd`, `u13.perUserPerMinute` trong `app_settings`, `REVOKE` trên `ai_calls`).
- [ ] **Bước 17** - JPA repository.
- [ ] **Bước 18** - Integration test: AI bị từ chối không trừ credit; lỗi release credit; trần ngày và quota Gemini báo "Hệ thống đang bận"; U05 embedding và U13 tạo nội dung dùng `requestRef` riêng, không trừ trùng khi retry. Judge0 thật: 7 ngôn ngữ, đúng/sai/quá giờ/quá bộ nhớ, mã mở mạng bị chặn.
- [ ] **Bước 19** - Tóm tắt: `code/repository-summary.md`.

### Nhóm E - API

- [ ] **Bước 20** - `/contracts/openapi/u13-ai-code.yaml` (U13 không phát event).
- [ ] **Bước 21** - Controller + DTO + validation.
- [ ] **Bước 22** - Test MockMvc: người học không đọc được đề xuất AI; không phải ADMIN không sửa cấu hình; `TRY` quá 5/phút `429`.
- [ ] **Bước 23** - Tóm tắt: `code/api-summary.md`.

### Nhóm F - Frontend

- [ ] **Bước 24** - `AiDraftDialog` (credit ước tính, trích dẫn, chọn câu) gắn vào U08, U06, U10; `AiGradingPanel` (cho U15).
- [ ] **Bước 25** - `CodeEditor` (Monaco, nhiều file), `CodeRunResult`, `VerifySolutionButton`; gắn vào `CodeWorkspace` (U11) và trình soạn câu `CODE` (U06).
- [ ] **Bước 26** - `AiSettingsPage`, `AiUsageDashboard`.
- [ ] **Bước 27** - Test frontend: dialog phân biệt "Không đủ credit AI", "Hệ thống đang bận" và "AI đang tắt"; kết quả test ẩn chỉ đạt/không.
- [ ] **Bước 28** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm G - Hoàn tất

- [ ] **Bước 29** - Cập nhật `README.md`: chạy Judge0 (privileged, mạng sandbox), kiểm ngôn ngữ, cấu hình model/trần/kill-switch, chạy local với AI giả.
- [ ] **Bước 30** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-AIG-001 (UC-AIG-01) | 4, 5, 6, 24 |
| US-AIG-002 (UC-AIG-02) | 6, 24 |
| US-AIG-003 (UC-AIG-03) | 4, 9, 26 |
| US-ASM-005 (UC-ASM-05) | 2, 10, 11, 12, 25 |
| Chấm code tự động, đề xuất chấm | 7, 11, 12 |

## 5. Ngoài phạm vi

- Quyết định dùng đề xuất chấm và chốt điểm (U15); lưu câu AI vào bài/ngân hàng/template (U08/U06/U10).
