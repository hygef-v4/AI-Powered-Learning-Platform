# U13 AI & Code Execution - Logical Components

**Bản tài liệu 2026-10-09**: không primary UC; phần chạy AI và Judge0 của UC 25, 29, 38, 43, nhóm AI của Settings UC 70–71, luồng phụ AI soạn đề (UC 35, 42–45, 57) và tóm tắt/embedding học liệu (UC 34, 55) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 U06/U08/U09 (AI drafts)   U15 (AI proposal)   U11 (try, submit, practice)   U05 (upload)
        |                        |                      |                         |
        v                        v                      v                         v
 +-------------------------------------- backend ---------------------------------------+
 | AiSuggestionService --> AiGuard (role, SettingsPort U03, cap, rate, CreditPort U07)  |
 |                     --> JobPort (AI_TASK)                                            |
 | AiUsageService (AiUsagePort: quote, hold, begin, complete, release; CreditUsagePort) |
 | CodeRunService --> TRY: CodeRunnerPort (sync)    VERIFY/GRADE: code_runs + JobPort   |
 | CodeLabCheckService (CodeLabCheckPort for U08, U09)                                  |
 | AiSettingDefinitions (SettingDefinition for U03)   AiUsageStatsService (U16)         |
 +--------------------------------------------------------------------------------------+
        | jobs.gemini                                  | jobs.code
        v                                              v
 +-------------------------------------- worker ----------------------------------------+
 | AiTaskHandler --> RagRetrievalPort (U05), DocumentModelPort/DiagramCompactPort (U09) |
 |               --> PromptBuilder + InjectionScanner --> AiGateway --> GeminiAdapter   |
 |               --> OutputValidator (schema + U06/U09) --> CreditPort.settle           |
 | CodeRunHandler --> CodeRunnerPort --> Judge0Adapter --[sandbox network]--> Judge0    |
 |                --> CodeScorer --> CodeGradedPort / PracticeResultPort (U15)          |
 | AiPendingSweeper, CodeRunPendingSweeper, CreditReservationScanner                    |
 +--------------------------------------------------------------------------------------+
```

**Text alternative**: U06, U08, U09 gọi `AiSuggestionService` để tạo đề xuất câu hoặc khung; U15 gọi để đề xuất chấm; U11 gọi để chấm Practice, chạy thử và chấm code; U05 gọi `AiUsageService` để báo mức giữ, giữ credit khi tải học liệu và ghi nhận lần tóm tắt/embedding. `AiGuard` kiểm vai trò, đọc kill-switch, trần và tần suất từ Settings (U03), giữ credit U07 rồi tạo job. Trong worker, `AiTaskHandler` lấy học liệu (U05) hoặc nội dung bài nộp (văn bản phẳng và XML rút gọn qua U09), dựng prompt có ranh giới dữ liệu, gọi Gemini qua `AiGateway`, kiểm đầu ra rồi trừ credit. `CodeRunService` chạy thử đồng bộ hoặc tạo dòng `code_runs` và job kiểm lời giải/chấm; `CodeRunHandler` gửi mã sang Judge0 qua mạng sandbox, tính điểm xác định và báo U15. `CodeLabCheckService` trả trạng thái kiểm lời giải cho U08, U09. `AiSettingDefinitions` khai báo mục AI cho Settings; `AiUsageStatsService` cấp số liệu cho Admin Dashboard của U16. Các sweeper và scanner gửi lại việc bị mất, hết hạn chấm và trả credit giữ quá hạn.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `AiGuard` | backend | P1 |
| `AiSuggestionService`, `AiTaskHandler` | backend, worker | F1, F2, F3a; P3-P5 |
| `AiUsageService` | backend, worker | `AiUsagePort` cho U05 (F6, P9), `CreditUsagePort` cho U07 |
| `AiPendingSweeper`, `CreditReservationScanner` | worker | P5 |
| `AiGateway`, `GeminiAdapter`, `FakeAiGateway` | worker | P2 |
| `CodeRunService`, `CodeRunHandler`, `Judge0Adapter`, `CodeScorer`, `ContentHasher`, `CodeRunPendingSweeper` | backend, worker | F3, F4; P6-P8 |
| `CodeLabCheckService` | backend | BR-U13-33 |
| `AiSettingDefinitions` | backend | F5, BR-U13-40 |
| `AiUsageStatsService` | backend | F5, BR-U13-41 (`AiUsageStatsPort` cho U16) |

Bỏ `AiAdminService` (cấu hình chuyển sang Settings của U03, báo cáo chuyển sang Admin Dashboard của U16).

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `GEMINI_API_KEY` | Rỗng → `FakeAiGateway` |
| `U13_MODEL_PRICES` | Bảng giá USD/1M token vào/ra theo model |
| Kill-switch, trần chi phí ngày, giới hạn/phút, model và bật/tắt từng loại việc | Không phải biến môi trường: mục nhóm AI trên Settings (`ai.enabled`, `ai.dailyCostCapUsd` 2, `ai.ratePerMinute` 10, `ai.{task}.model`, `ai.{task}.enabled`), Admin sửa trên Setting Detail |
| `U13_AI_CONCURRENCY` | 3 |
| `U13_CODE_CONCURRENCY` | 2 |
| `JUDGE0_URL` | `http://judge0-server:2358` |
| `JUDGE0_AUTH_TOKEN` | secret |
| `U13_TRY_PER_MINUTE` | 5 |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log prompt/phản hồi/key |
| SECURITY-05 | Compliant | P3, P4 |
| SECURITY-08 | Compliant | `AiGuard` từ chối Admin, giới hạn Student; chỉ người yêu cầu xem đề xuất |
| SECURITY-09 | Compliant | Key, token trong `.env`, không trong Settings |
| SECURITY-15 | Compliant | P1 từ chối trước khi gọi; P5, P9 release credit |
| RESILIENCY-06 | Compliant | P6 kiểm Judge0 khi khởi động |
| RESILIENCY-10 | Compliant | Timeout Gemini, Judge0 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
