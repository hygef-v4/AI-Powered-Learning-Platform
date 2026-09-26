# U13 AI & Code Execution - Logical Components

## 1. Sơ đồ

```
 U08/U06/U10 (đề xuất câu)   U15 (đề xuất chấm)       U11 (chạy thử, nộp)       ADMIN
        |                           |                          |                  |
        v                           v                          v                  v
 +-------------------------------------- backend ---------------------------------------+
 | AiProposalService --> AiGuard (kill-switch, trần, rate, CreditPort U07) --> JobPort  |
 | CodeRunService --> TRY: CodeRunnerPort (đồng bộ)   VERIFY/GRADE: JobPort             |
 | CodeLabCheckService (cho U08)      AiAdminService (cấu hình, báo cáo)                |
 +--------------------------------------------------------------------------------------+
        | jobs.gemini                        | jobs.code
        v                                         v
 +-------------------------------------- worker ---------------------------------------+
 | AiTaskHandler --> RagRetrievalPort (U05), DocumentModelPort/DiagramCompactPort (U09)|
 |              --> PromptBuilder + InjectionScanner --> AiGateway --> GeminiAdapter   |
 |              --> OutputValidator (schema + U06) --> CreditPort.settle               |
 | CodeRunHandler --> CodeRunnerPort --> Judge0Adapter --[mạng sandbox]--> Judge0      |
 |               --> CodeScorer --> CodeGradedPort (U15)                               |
 +-------------------------------------------------------------------------------------+
```

**Text alternative**: Các unit gọi `AiProposalService` để tạo đề xuất; `AiGuard` kiểm kill-switch, trần chi phí, giới hạn tần suất và giữ credit U07 rồi tạo job. Trong worker, `AiTaskHandler` lấy học liệu (U05) hoặc nội dung bài (U09), dựng prompt có ranh giới dữ liệu, gọi Gemini qua `AiGateway`, kiểm đầu ra rồi trừ credit. `CodeRunService` chạy thử đồng bộ hoặc tạo job kiểm lời giải/chấm; `CodeRunHandler` gửi mã sang Judge0 qua mạng sandbox, tính điểm xác định và phát event cho U15. Admin cấu hình và xem báo cáo qua `AiAdminService`.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `AiGuard` | backend | P1 |
| `AiProposalService`, `AiTaskHandler` | backend, worker | F1, F2; P3-P5 |
| `AiGateway`, `GeminiAdapter`, `FakeAiGateway` | worker | P2 |
| `CodeRunService`, `CodeRunHandler`, `Judge0Adapter`, `CodeScorer` | backend, worker | F3, F4; P6-P8 |
| `CodeLabCheckService` | backend | BR-U13-33 |
| `AiAdminService` | backend | F5 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `GEMINI_API_KEY` | Rỗng → `FakeAiGateway` |
| `AI_DAILY_COST_CAP_USD` | 2 |
| `AI_PER_USER_PER_MINUTE` | 10 |
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
| SECURITY-08 | Compliant | Chỉ API giảng viên thấy đề xuất |
| SECURITY-09 | Compliant | Key, token trong `.env` |
| SECURITY-15 | Compliant | P1 từ chối trước khi gọi; P5 release credit |
| RESILIENCY-06 | Compliant | P6 kiểm Judge0 khi khởi động |
| RESILIENCY-10 | Compliant | Timeout Gemini, Judge0 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
