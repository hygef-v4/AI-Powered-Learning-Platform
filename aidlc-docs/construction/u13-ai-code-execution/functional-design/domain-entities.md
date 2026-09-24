# U13 AI & Code Execution - Domain Entities

## 1. Phạm vi sở hữu

U13 sở hữu cấu hình AI (model theo việc, trần chi phí, kill-switch), nhật ký lời gọi AI, đề xuất AI (câu hỏi, chấm), kiểm lời giải mẫu Code Lab và các lần chạy code. U13 **không** sở hữu: bài/câu hỏi (U08, U06), bài nộp (U11, U14), điểm (U15), credit (U07), RAG (U05), rút gọn XML (U09).

## 2. `AiTaskConfig` (một dòng mỗi loại việc)

| `task` | Model mặc định | Ghi chú |
|---|---|---|
| `QUESTION_DRAFT` | `gemini-2.5-flash` | Tạo câu hỏi/đề nháp từ RAG |
| `GRADING_PROPOSAL` | `gemini-2.5-pro` | Đề xuất chấm theo rubric (bài dài, cần suy luận) |
| `CODE_FEEDBACK` | `gemini-2.5-flash` | Nhận xét code sau khi đã có kết quả test |
| `SHORT_TEXT` | `gemini-2.5-flash-lite` | Việc ngắn (gợi ý tiêu đề, tóm tắt ngắn) |

Thuộc tính: `task`, `model` (trong danh sách cho phép: `gemini-2.5-flash-lite`, `gemini-2.5-flash`, `gemini-2.5-pro`), `maxOutputTokens`, `temperature`, `enabled`.

`AiGlobalSettings`: `killSwitch` (bool), `dailyCostCapUsd`, `perUserPerMinute` (mặc định 10).

## 3. `AiCall` (nhật ký, không lưu nội dung)

`id`, `task`, `model`, `requestedBy`, `scopeRef` (lớp/môn), `inputTokens`, `outputTokens`, `creditsCharged`, `estimatedCostUsd`, `latencyMs`, `status` (`SUCCEEDED`, `FAILED`, `REJECTED_QUOTA`, `REJECTED_KILL_SWITCH`, `INVALID_OUTPUT`), `errorCode`, `createdAt`.

## 4. `AiProposal`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `kind` | enum | `QUESTION_DRAFT`, `GRADING_PROPOSAL` |
| `requestedBy` | UUID | |
| `target` | JSON | Bài/template/ngân hàng đích, hoặc lượt bài nộp/phần |
| `input` | JSON | Tham số (loại câu, số câu 1-20, độ khó, chương/bài) — không lưu prompt thô |
| `result` | JSON | Câu hỏi đề xuất kèm trích dẫn nguồn; hoặc từng mục checklist đạt/không + nhận xét + bằng chứng |
| `status` | enum | `QUEUED`, `RUNNING`, `READY`, `FAILED`, `ACCEPTED`, `DISCARDED` |
| `jobId` | UUID | Job U02 |

## 5. Code Lab

`SolutionVerification`: `assignmentId`/`bankItemId`, `contentHash` (đề + test + lời giải), `passed`, `results[]`, `verifiedAt`.

`CodeRun`:

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `kind` | enum | `TRY` (người học chạy thử test công khai), `VERIFY` (lời giải mẫu), `GRADE` (bài nộp, mọi test) |
| `ownerRef` | | Lượt làm / phần / bài |
| `language` | enum | `JAVA`, `PYTHON`, `C`, `CPP`, `JAVASCRIPT`, `DART`, `CSHARP` |
| `status` | enum | `QUEUED`, `RUNNING`, `DONE`, `SANDBOX_ERROR` |
| `results[]` | JSON | Mỗi test: `passed`, `status` (`ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT`, `MEMORY_LIMIT`, `RUNTIME_ERROR`, `COMPILE_ERROR`), `timeMs`, `memoryKb`, output rút gọn (test ẩn không trả output) |
| `score` | numeric(6,2) | `GRADE`: tổng điểm test đạt |

Kết quả `CodeRun` bất biến sau `DONE`.

## 6. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `AiDraftPort` | U13 cung cấp cho U08 (`C`), U06, U10 | Tạo/đọc/nhận đề xuất câu hỏi |
| `AiGradingPort` | U13 cung cấp cho U14, U15 | Tạo/đọc đề xuất chấm |
| `CodeRunPort` | U13 cung cấp cho U11 (`C`), U14, U15 | `try`, `grade`, kết quả |
| `CodeLabCheckPort` | U13 cài cho U08 (`C`, qua danh sách kiểm duyệt) | Bài `CODE_LAB` đã kiểm lời giải mẫu với đúng nội dung hiện tại |
| `AiKillSwitchPort` | U13 cung cấp cho U05 | Thay biến `.env` tạm của U05 |
| `CreditPort` | U13 dùng U07 | Giữ/trừ/trả credit |
| `RagRetrievalPort` | U13 dùng U05 | Đoạn học liệu trong phạm vi |
| `BankQueryPort` | U13 dùng U06 | Câu hỏi, rubric |
| `DiagramCompactPort`, `DocumentModelPort` | U13 dùng U09 | XML rút gọn, văn bản phẳng |
| `JobPort`, `AuditPort` | U13 dùng U02 | Chạy nền, audit |
