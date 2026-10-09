# U13 AI & Code Execution - Code Generation Plan

**Bản tài liệu 2026-10-09**: không primary UC; phần chạy AI và Judge0 của UC 25, 29, 38, 43, nhóm AI của Settings UC 70–71, luồng phụ AI soạn đề (UC 35, 42–45, 57) và tóm tắt/embedding từ màn xem học liệu (UC 15, 34, 55) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U13. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-AIG-001, US-AIG-002, US-AIG-003, US-ASM-005 (phần chạy kiểm), US-ASM-012 (phần AI chấm Practice).
- **Primary UC hiện hành**: Không primary UC (support). U13 chạy AI/Judge0 cho UC 25 (U11), 29 (U11), 38 (U15), 43 (U08, U09); khai báo mục nhóm AI cho Settings UC 70–71 (U03); luồng phụ AI soạn đề của UC 35, 42–45 (U08, U09), 57 (U06) và giữ/ghi nhận credit khi người xem yêu cầu tóm tắt UC 15, 34, 55 (U05). Supporting flows theo current-srs-contract.md.
- **Quyết định 2026-10-09**: Admin không có ví, không dùng AI; cấu hình AI là `SettingDefinition` nhóm AI trên Settings của U03 (bỏ bảng `ai_services`, popup AI Setting, màn AI Usage và API `/admin/ai/*`); số liệu AI cho Admin Dashboard (U16) qua `AiUsageStatsPort`; U10 đã xóa (không còn template; AI soạn câu cho bài của môn qua U08); không còn ngân hàng lớp; U13 tự lưu kết quả kiểm lời giải mẫu theo `contentHash` của phiên bản câu `CODE` (bảng `code_runs`, bỏ `QuestionVerificationPort`); tóm tắt học liệu `MATERIAL_SUMMARY` và giữ credit khi bấm Tóm tắt tài liệu (`AiUsagePort.quote/hold/release`).
- **Thiết kế nguồn**: `construction/u13-ai-code-execution/` (functional-design, nfr-requirements, nfr-design, infrastructure-design). Tham khảo code: `../demo_do_an` (`Judge0CodeRunner`, `SolutionVerifier`, `PromptInjectionScanner`, `docker-compose.yml`, `judge0.conf`).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này. Mã khung U13 đã có (`aiexecution/` chỉ có port và service ném "Chưa cài") không được coi là bước đã xong.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `SubjectScopePort`, `ClassScopePort` | U04 | Dùng thật (R2 Chủ nhiệm môn, R3/R4 giảng viên chính của lớp) |
| `JobPort`, `JobHandler`, `PendingSweeper`, `ScheduledScanner`, `SettingsPort`, `SettingDefinition` | U03 | Dùng thật (Settings theo plan U03 Bước S1–S8) |
| `RagRetrievalPort` | U05 | Dùng thật |
| `BankQueryPort`, `RubricPort`, `DefinitionValidator` | U06 | Dùng thật |
| `CreditPort` | U07 | Dùng thật (`CreditPurpose` có `MATERIAL_SUMMARY`) |
| `AssignmentQueryPort` | U08 | Dùng thật (câu `CODE` của bài, phạm vi, trạng thái) |
| `DocumentModelPort`, `DiagramCompactPort` | U09 | Dùng thật |
| `SubmissionQueryPort`, `AttemptRunResultPort`, `GroupSubmissionQueryPort` | U11, U14 (`C`) | Dùng thật (wave 3, code trước U13) |
| `CodeGradedPort`, `PracticeResultPort` | U15 (`C`) | Dùng thật (wave 3, code trước U13) |
| U13 cài `AiDraftPort` (U06, U08, U09), `CodeRunPort` và `PracticeGradingPort` (U11), `CodeLabCheckPort` (U08, U09), `AiUsagePort` (U05), `CreditUsagePort` (U07), `SettingDefinition` nhóm AI (U03) | | Thay adapter tạm của U05, U06, U07, U08, U09, U11 |
| `AiGradingPort`, `CodeRunPort.grade/regrade` | cho U15 | Thay adapter tạm của U15 |
| `AiUsageStatsPort` | cho U16 (`C`) | U16 dùng khi revision Admin Dashboard (UC 58) |

### Dữ liệu U13 sở hữu

PostgreSQL `ai_suggestions`, `code_runs`; kết quả chạy code mới nhất của lượt ghi vào `attempts.run_result` (qua U11); mục nhóm AI trong `system_settings` (U03 lưu); Redis `gemini:daily-cost:*`, `ratelimit:ai-request:*`, `ratelimit:code-try:*`; queue `jobs.gemini`, `jobs.code`; 4 container Judge0.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  aiexecution/
    api/                AiSuggestionController, CodeRunController, DTO
    application/        AiGuard, AiSuggestionService, AiUsageService, AiUsageStatsService,
                        CodeRunService, CodeLabCheckService, CodeScorer, ContentHasher
    settings/           AiSettingDefinitions, AiSettings (đọc qua SettingsPort)
    ai/                 AiGateway, GeminiAdapter, FakeAiGateway, PromptBuilder,
                        InjectionScanner, OutputValidator, prompts/ (theo task, có version)
    sandbox/            CodeRunnerPort, Judge0Adapter, FakeCodeRunner, LanguageRegistry
    domain/             AiSuggestion, AiTaskType, CodeRun, CodeRunResult (TRY, VERIFY, GRADE)
    infrastructure/     JPA repository
    worker/             AiTaskHandler, CodeRunHandler, AiPendingSweeper, CodeRunPendingSweeper,
                        CreditReservationScanner
    port/               AiDraftPort, AiGradingPort, PracticeGradingPort, CodeRunPort,
                        AiUsageStatsPort
/backend/src/main/resources/db/migration/aiexecution/
  V20260925_2000__create_ai_suggestions_code_runs.sql
/infra/judge0/judge0.conf
/frontend/src/shared/ai/        AiDraftDialog, AiErrorNotice
/frontend/src/shared/codelab/   CodeEditor, CodeRunResult, VerifySolutionButton
/contracts/openapi/ai-execution.yaml
```

`AiUsagePort` (U05 khai báo ở `content.port`), `CodeLabCheckPort` (U08 khai báo ở `assessments.port`), `CreditUsagePort` (U07 khai báo) do U13 cài. Không có `app/admin/ai/`.

## 3. Các bước

### Nhóm A - Khung và hạ tầng

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) và Settings của U03 (Bước S1–S3) đã có.
- [ ] **Bước 1** - `pom.xml`: `networknt/json-schema-validator`. Biến cấu hình U13 theo `logical-components.md` §3 (không còn biến cho trần/tần suất/model).
- [ ] **Bước 2** - Docker Compose: 4 container Judge0 (theo `demo_do_an`), mạng `sandbox` `internal: true`, `backend`/`worker` nối thêm `sandbox`; `infra/judge0/judge0.conf` theo `infrastructure-design.md` §2; secret `JUDGE0_AUTH_TOKEN`.

### Nhóm B - AI

- [ ] **Bước 3** - Domain AI (`AiSuggestion`, `AiTaskType` 6 giá trị); `AiGateway` + `GeminiAdapter` (`generateContent`, JSON schema, token, timeout theo model) + `FakeAiGateway` (P2).
- [ ] **Bước 4** - `AiSettingDefinitions` (các mục `ai.*` theo `domain-entities.md` §2) và `AiSettings` đọc qua `SettingsPort`; `AiGuard`: từ chối `ADMIN`; Student được PRACTICE_GRADING cho attempt Text/Diagram Essay của mình hoặc MATERIAL_SUMMARY qua U05 theo quyền xem; Teacher/Subject Manager theo R2/R3/R4; kill-switch, trần ngày Redis, rate limit theo Settings và `CreditPort.reserve` theo purpose/attemptRef (P1, BR-U13-02, 03, 40).
- [ ] **Bước 5** - `PromptBuilder` (khối `<data>`, prompt theo task có version), `InjectionScanner`, `OutputValidator` (schema + U06/U09/rubric) (P3, P4, BR-U13-07).
- [ ] **Bước 6** - `AiSuggestionService` + `AiTaskHandler`: `QUESTION_DRAFT` (đích ngân hàng môn R2, quiz/bài của lớp R3/R4, quiz/bài của môn R2; RAG phạm vi lớp hoặc môn; trích dẫn; nhận/bỏ) và `SKELETON_DRAFT` cho U09 (kiểm `validateSkeleton`, BR-U06-30) (F1, BR-U13-10…15).
- [ ] **Bước 7** - `GRADING_PROPOSAL` cho giảng viên chính của lớp (từng bài và `requestBatch`, hạn 5 phút tính từ lúc xử lý từng bài; bài của môn chỉ sinh viên lớp mình) và `PRACTICE_GRADING` cho Student (văn bản phẳng + XML rút gọn U09, rubric khóa và `RubricPort.score`); Practice tối đa một dòng hợp lệ mỗi attempt, ghi qua `PracticeResultPort.record`, không vào sổ điểm (F2, F3a, BR-U13-20…24).
- [ ] **Bước 8** - Settle/release credit theo `credit_status`, số liệu và `model` trên `ai_suggestions`, việc idempotent, thử lại, `AiPendingSweeper` (chấm quá 5 phút → `FAILED` + chốt lượng dùng thật/trả dư; soạn đề `QUEUED` quá 5 phút → gửi lại), `CreditReservationScanner` theo `reserve_expires_at` (30 phút; dòng giữ `MATERIAL_SUMMARY` 25 giờ) (P5, BR-U13-04…06, 08, 24, 53).
- [ ] **Bước 9** - `AiUsageService` cài `AiUsagePort` cho U05: `quote`, `hold`, `begin` (có `holdId`), `complete`, `fail`, `release` (F6, P9, BR-U13-50…53); cài `CreditUsagePort` cho U07; `AiUsageStatsService` cài `AiUsageStatsPort` cho U16 (BR-U13-41).

### Nhóm C - Code Lab

- [ ] **Bước 10** - `LanguageRegistry` (7 ngôn ngữ → `language_id`, kiểm `/languages` khi khởi động), `Judge0Adapter` (batch, `additional_files`, poll), `FakeCodeRunner` (P6).
- [ ] **Bước 11** - `CodeRunService`: `TRY` đồng bộ + rate limit (chủ lượt đang làm); `VERIFY` (R3/R4 bài của lớp hoặc R2 bài của môn, bài `DRAFT`, câu thuộc bài; `ContentHasher`; dùng lại dòng cùng hash) và `GRADE`/`regrade` qua `code_runs` + job; `CodeScorer`; ẩn chi tiết test ẩn; `CodeRunPendingSweeper` (F3, F4, P7, P8, BR-U13-30…37).
- [ ] **Bước 12** - `CodeLabCheckService` cài `CodeLabCheckPort` (`check` cho duyệt U08, `statusOf` cho U09) từ dòng `code_runs` `VERIFY` mới nhất cùng `contentHash`; `CodeRunHandler` `GRADE`: bài `GRADED` gọi `CodeGradedPort.onGraded`, bài `PRACTICE` gọi `PracticeResultPort.record`; ghi `attempts.run_result` qua `AttemptRunResultPort`.
- [ ] **Bước 13** - Thay adapter tạm: `AiDraftPort` (U06, U08, U09), `CodeRunPort` và `PracticeGradingPort` (U11), `AiGradingPort` và `CodeRunPort` (U15), `CodeLabCheckPort` (U08, U09), `AiUsagePort` (U05), `CreditUsagePort` (U07: đọc `ai_suggestions` của chính chủ ví, phân trang 20, cho My Credit Package).
- [ ] **Bước 14** - Unit test mọi `BR-U13-xx` với `FakeAiGateway`/`FakeCodeRunner`/`SettingsPort` giả (gồm Admin bị từ chối, kill-switch đọc từ Settings, giữ credit khi yêu cầu tóm tắt học liệu, kiểm lời giải theo hash).
- [ ] **Bước 15** - Tóm tắt: `aidlc-docs/construction/u13-ai-code-execution/code/business-logic-summary.md`.

### Nhóm D - Dữ liệu và tích hợp

- [ ] **Bước 16** - Flyway `db/migration/aiexecution/V20260925_2000__create_ai_suggestions_code_runs.sql` theo `infrastructure-design.md` §4 (`ai_suggestions` có `task_type`, `model`, `hold_id`, `request_ref`, `reserve_expires_at`; `code_runs`; không tạo `ai_services`, không seed).
- [ ] **Bước 17** - JPA repository.
- [ ] **Bước 18** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test: AI bị từ chối không trừ credit; lỗi chốt lượng dùng thật và trả dư, chưa dùng mới hoàn toàn bộ; trần ngày và quota Gemini báo "Hệ thống đang bận"; đổi `ai.enabled` trên Settings có hiệu lực ≤ 30 giây; U05 embedding và U13 tạo nội dung dùng `requestRef` riêng, không trừ trùng khi retry; upload thiếu credit vẫn tạo học liệu/trích chữ; chỉ thiếu credit lúc yêu cầu summary mới từ chối yêu cầu; NO_TEXT không có HOLD; BUSY sau nhận giữ HOLD tới terminal/deadline. Chấm Practice: đủ/thiếu credit, quá 5 phút → `FAILED`, chốt lượng dùng thật/trả dư rồi bấm lại được, chỉ một kết quả hợp lệ mỗi attempt. Judge0 thật: 7 ngôn ngữ, đúng/sai/quá giờ/quá bộ nhớ, mã mở mạng bị chặn; kiểm lời giải rồi sửa test → duyệt bị chặn.
- [ ] **Bước 19** - Tóm tắt: `code/repository-summary.md`.

### Nhóm E - API

- [x] **Bước 20** - `/contracts/openapi/ai-execution.yaml` (U13 không phát event); cần sửa theo mục 8.
- [ ] **Bước 21** - Controller + DTO + validation.
- [ ] **Bước 22** - Test MockMvc: Admin bị từ chối mọi API U13; Student TRY lượt của mình, đọc kết quả Practice của mình và yêu cầu summary qua U05 theo quyền xem, không gọi các task AI khác hoặc đọc đề xuất của giảng viên; `TRY` dùng Judge0 không trừ credit; `TRY` quá 5/phút `429`; `VERIFY` ngoài phạm vi bài `403`.
- [ ] **Bước 23** - Tóm tắt: `code/api-summary.md`.

### Nhóm F - Frontend

- [ ] **Bước 24** - `AiDraftDialog`, `AiErrorNotice` (credit ước tính, trích dẫn, chọn câu) gắn vào Quiz Detail, Assignment Form (U08) và `AiQuestionDraftDialog` của Question List (U06); API cho nút "Chấm với AI" của U11 trên Submission History (UC 29) và panel AI đề xuất của U15 trong Grading Workspace (UC 38); không có component chấm AI của U13.
- [ ] **Bước 25** - `CodeEditor` (Monaco, nhiều file), `CodeRunResult`, `VerifySolutionButton`; gắn vào Codelab Workspace (U11) và `CodeLabCheckPanel` (U09) trên Assignment Form.
- [ ] **Bước 26** - Không còn màn AI Usage và popup AI Setting: mục AI hiện trên Setting List/Setting Detail của U03 nhờ `AiSettingDefinitions` (không có code frontend riêng).
- [ ] **Bước 27** - Test frontend: dialog phân biệt "Không đủ credit AI", "Hệ thống đang bận" và "AI đang tắt"; kết quả test ẩn chỉ đạt/không; `VerifySolutionButton` hiện "Cần kiểm lại" khi hash lệch.
- [ ] **Bước 28** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm G - Hoàn tất

- [ ] **Bước 29** - Cập nhật `README.md`: chạy Judge0 (privileged, mạng sandbox), kiểm ngôn ngữ, mục AI trên Settings (model, trần, kill-switch), chạy local với AI giả.
- [ ] **Bước 30** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| UC 25 Complete Code Lab (chạy test công khai, chấm khi nộp; U11 chủ trì) | 10, 11, 12, 25 |
| UC 29 Grade Practice Assignment (AI chấm Practice; U11 chủ trì) | 4, 7, 8, 24 |
| UC 38 Grade Submission With AI (đề xuất chấm; U15 chủ trì) | 4, 7, 8, 24 |
| UC 43 Create/Update/Delete Code Lab (kiểm lời giải mẫu; U08, U09 chủ trì) | 10, 11, 12, 25 |
| UC 70–71 Settings (nhóm AI; U03 chủ trì) | 4, 26 |
| US-AIG-001 (AI soạn câu/khung cho bài của lớp, luồng phụ UC 35, 42–45) | 4, 5, 6, 24 |
| US-AIG-002 (AI soạn câu ngân hàng môn và bài của môn, luồng phụ UC 35, 42–44, 57) | 6, 24 |
| US-AIG-003 (cấu hình qua Settings, số liệu cho Admin Dashboard UC 58) | 4, 9, 26 |
| UC 34, 55 (giữ credit, tóm tắt/embedding học liệu; U05 chủ trì) | 9, 14, 16 |

## 5. Ngoài phạm vi

- Quyết định dùng đề xuất chấm và chốt điểm (U15); lưu câu AI vào bài/ngân hàng (U08/U06); thay khung (U09); màn và bảng Settings (U03); hiển thị Admin Dashboard (U16); tóm tắt và quét học liệu (U05).

## 6. Revision implementation scope - 2026-10-08
- [ ] Không primary UC; hỗ trợ AI và Judge0 theo mục 1 (số UC theo bản 73 UC). Admin vận hành AI qua Settings (US-AIG-003, US-SET-001), không tạo UC riêng.
- [ ] Unit gọi xác minh R2/R3/R4/R5; giữ provider-neutral business descriptions và provider Gemini trong adapter/config.

## 7. Tóm tắt học liệu - 2026-10-09

- [ ] Thêm loại việc `MATERIAL_SUMMARY` (model mặc định `gemini-2.5-flash`): `AiUsagePort.begin` nhận loại việc này cho người bấm Tóm tắt tài liệu (Student, Teacher, Subject Manager) đã có quyền xem; thêm giá trị tương ứng vào `CreditPurpose` của U07.
- [ ] Khai báo model của `MATERIAL_SUMMARY` trong nhóm AI trên Settings (`ai.materialSummary.model`, U03 giữ chung Settings).

## 8. Revision theo bản 73 UC - 2026-10-09

Contract (`contracts/openapi/ai-execution.yaml`, sửa ở lượt code):
- [ ] Bỏ `GET /api/v1/admin/ai/usage`, `GET`/`PUT /api/v1/admin/ai/settings` và schema `AiUsageRow`, `AiSettings` (cấu hình qua `/api/v1/admin/settings` của U03; số liệu qua `AiUsageStatsPort` cho U16).
- [ ] `POST /api/v1/ai/question-drafts`: chỉ ngân hàng môn (body `subjectId` thay `targetType`/`targetId`, bỏ `TEMPLATE`); `x-roles` chỉ `SUBJECT_MANAGER`.
- [ ] `x-roles` mọi endpoint bỏ `ADMIN`; `GET /ai-suggestions/{id}` giữ `STUDENT` (chỉ `PRACTICE_GRADING` của mình); `GET /ai-suggestions`, `accept`, `discard` chỉ `TEACHER`, `SUBJECT_MANAGER`.
- [ ] `POST /api/v1/code-runs`: `VERIFY` thêm `assignmentId` bắt buộc; `TRY` trả `200` kèm kết quả (đồng bộ), `VERIFY` trả `202`; `x-roles` `TRY` → `STUDENT`, `VERIFY` → `TEACHER`, `SUBJECT_MANAGER`. `GET /api/v1/code-runs/{runId}` chỉ dòng `VERIFY`, `x-roles` `TEACHER`, `SUBJECT_MANAGER`.
- [ ] Schema `CodeRun` thêm `questionId`, `passedAll`; `info.description` bỏ "cấu hình AI cho ADMIN", "Template".

Port và code (khung hiện có trong `backend/src/main/java/edu/aiplatform`):
- [ ] `content.port.AiUsagePort` thêm `quote(accountId)`, `hold(taskType, requestedBy, targetType, targetId, requestRef)`, `release(holdId)`; `begin` thêm `holdId` tùy chọn; `AiUsageService` cài đủ.
- [ ] `billing.port.CreditPurpose` thêm `MATERIAL_SUMMARY` (U07 sửa enum; U13 dùng).
- [ ] `assessments.port.CodeLabCheckPort` thêm `statusOf(assignmentId)` cho U09; `CodeLabCheckService` đọc `code_runs`.
- [ ] Bỏ dùng `questionbank.port.QuestionVerificationPort` (U06 xóa port); U13 lưu kết quả kiểm lời giải trong `code_runs` theo `contentHash`.
- [ ] `CodeRunPort` thêm `regrade(attemptId)`; port mới `AiUsageStatsPort` cho U16.
- [ ] `AiDraftPort`, `DraftRequest`: bỏ đích template; đích là ngân hàng môn, quiz/bài của lớp, quiz/bài của môn; javadoc người dùng U06, U08, U09 (bỏ U10).
- [ ] Thêm `AiSettingDefinitions` (`SettingDefinition` nhóm `AI`); bỏ `AiAdminService`, `AiAdminController`, `AiUsagePage`, `AiSettingsDialog`.
- [ ] `AiGuard` từ chối `ADMIN` cho mọi task (kể cả gọi API trực tiếp).
- [ ] Mức giữ credit tối đa khi yêu cầu tóm tắt học liệu (giới hạn đầu vào 200 000 ký tự giữ nguyên) không vượt mức tặng mặc định 100 credit/tháng (Settings credit.monthlyFreeCredits); upload không phụ thuộc mức giữ; unit test kiểm.

Migration:
- [ ] Đổi `V20260925_2000__u13_ai.sql` thành `db/migration/aiexecution/V20260925_2000__create_ai_suggestions_code_runs.sql` (chưa áp dụng nên sửa tại chỗ): bỏ `ai_services` và seed; `ai_suggestions` dùng `task_type`, `model` thay FK `ai_service_id`, thêm `hold_id`, `request_ref`, `reserve_expires_at`; thêm bảng `code_runs`.

## 9. Hold và checkpoint học liệu — 2026-10-09

- [ ] HOLD requestRef riêng `lessonId:HOLD`, child calls theo SUMMARY:chunkIndex/SUMMARY:MERGE/EMBEDDING với hold_id; result checkpoint có cấu trúc, không prompt thô. Lookup hold theo lesson/requestRef giữ cố định.
- [ ] Complete và cộng tổng dùng vào hold nguyên tử/idempotent; dùng lại checkpoint READY, release terminal settle phần AI đã dùng thật và trả dư, chưa gọi AI trả toàn bộ; hold scanner 25 giờ dự phòng deadline scan 24 giờ.
- [ ] Unit test nhiều chunk, retry sau chunk hoàn tất, duplicate complete/release, lỗi embedding sau summary, lỗi/timeout provider và scanner hết hạn không settle hai lần.

## Bổ sung sau recheck 2026-10-09 — checkpoint và settlement

- [ ] Cài findHold, HoldSnapshot, UsageStart/CallSnapshot và complete(ticket, tokens, cost, checkpoint), fail(ticket, usage?) theo contract U05; begin trả checkpoint khi REPLAY, ticket có metadata scanClaimId khi RUN. Không thêm bảng/cột checkpoint hoặc HTTP API.
- [ ] Kiểm ticket/claim metadata và khóa HOLD khi complete/fail/release; READY không cộng lại, stale ticket/HOLD đã chốt bị từ chối. Scanner chỉ xử lý RESERVED, chốt lượng đã dùng bằng CreditPort.settle, bằng 0 mới CreditPort.release.
- [ ] Kịch bản kiểm: lỗi embedding sau summary, scanner 25 giờ với lượng dùng > 0/0, child NONE không hoàn HOLD, READY replay và complete cạnh tranh release. Đây là việc chưa triển khai, không thay dấu hoàn tất cũ.

## Revision: nút tóm tắt trên View Material — 2026-10-09

Các bước cũ giả định summary/credit lúc upload được thay bởi yêu cầu hiện hành dưới đây; dấu [x] implementation cũ giữ lịch sử, không xác nhận code mới.

- [ ] Upload MATERIAL và tạo lesson không gọi Gemini/quote/hold, không chặn vì thiếu credit; extraction worker lưu EXTRACTED, không summary/vector.
- [ ] Migration/DTO U05 thêm EXTRACTED, summary_requested_by/at; hai giai đoạn deadline và guard jobType; U03 nhận MATERIAL_SUMMARY trên jobs.gemini.
- [ ] View Material có SummaryAction cho Student/Teacher/Subject Manager theo quyền xem, kể cả Teacher xem học liệu môn chỉ đọc; Admin denied. GET lesson summary-credit/scan và POST summary (classId, Idempotency-Key).
- [ ] Nhận yêu cầu atomically khóa lesson + kiểm quyền xem/AI guard + HOLD của requester + metadata/deadline + enqueue. Student MATERIAL_SUMMARY hợp lệ; child EMBEDDING cùng HOLD, không cho embedding độc lập. Hai actor bấm chỉ một payer; kết quả dùng chung, không charge lại.
- [ ] Worker phục hồi/checkpoint/lease/fencing, một HOLD summary+embedding; AI deadline 24 giờ và HOLD fallback 25 giờ từ yêu cầu; lỗi settle thực dùng/trả dư, giữ summary khi embedding lỗi.
- [ ] Kiểm upload zero-credit/no-AI; 3 role hợp lệ, ngoài scope/Admin denied; hai actor bấm đồng thời/duplicate; late first request sau upload >24 giờ vẫn hợp lệ; thiếu credit/AI guard trước nhận giữ EXTRACTED; no text/caption; cache summary; retry/crash/embedding lỗi; Student không truy xuất RAG hoặc soạn/chấm Graded.

AiUsagePort.checkAvailability(task, actor, target) dùng AiGuard U13, trả allowed/reason trước nhận yêu cầu, không reserve; hold cũng kiểm guard để tránh khoảng trống giữa preflight và nhận. Worker begin kiểm lại guard; nếu bị chặn sau nhận thì BUSY/retry theo deadline. U13 không đọc bảng lesson: U05 truyền target/context đã kiểm, rồi U13 xác minh actor/task/target/HOLD.
