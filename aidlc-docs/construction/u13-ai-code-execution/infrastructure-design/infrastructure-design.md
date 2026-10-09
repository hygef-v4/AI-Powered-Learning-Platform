# U13 AI & Code Execution - Infrastructure Design

**Bản tài liệu 2026-10-09**: không primary UC; phần chạy AI và Judge0 của UC 25, 29, 38, 43, nhóm AI của Settings UC 70–71, luồng phụ AI soạn đề (UC 35, 42–45, 57) và tóm tắt/embedding học liệu (UC 34, 55) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `AiGuard`, `AiSuggestionService`, `AiUsageService` (`AiUsagePort` cho U05, `CreditUsagePort` cho U07), `CodeRunService` (TRY đồng bộ), `CodeLabCheckService`, `AiSettingDefinitions`, `AiUsageStatsService` | `backend` |
| `AiTaskHandler`, `CodeRunHandler`, `CreditReservationScanner`, `AiPendingSweeper`, `CodeRunPendingSweeper` | `worker` |
| Bảng `ai_suggestions`, `code_runs`; kết quả chạy code mới nhất của lượt trong `attempts.run_result` (U11) | `postgres` |
| Cài đặt AI | Bảng `system_settings` của U03 (nhóm `AI`), đọc qua `SettingsPort` |
| Bộ đếm chi phí ngày, rate limit | `redis`, khóa `gemini:daily-cost:*`, `ratelimit:ai-request:*`, `ratelimit:code-try:*` |
| Queue | `jobs.gemini` (`AI_TASK`), `jobs.code` (`CODE_RUN`) |
| Event | Không phát event; điểm Code Lab `GRADED` báo U15 qua `CodeGradedPort`, `PRACTICE` qua `PracticeResultPort`; kết quả AI xem bằng cách hỏi trạng thái theo quyền |
| Chạy code | 4 container Judge0 trong mạng `sandbox` |

## 2. Judge0 (theo `demo_do_an`)

| Container | Image | Giới hạn | Ghi chú |
|---|---|---|---|
| `judge0-server` | `judge0/judge0:1.13.1` | 0.5 CPU, 512 MB | API cổng 2358, chỉ trong mạng `sandbox`; `AUTHN_TOKEN` bật |
| `judge0-workers` | `judge0/judge0:1.13.1` (`./scripts/workers`) | 1.5 CPU, 1,5 GB | Chạy code trong isolate; số worker 2 |
| `judge0-db` | `postgres:16-alpine` | 0.25 CPU, 256 MB | Riêng cho Judge0, volume `judge0_db_data` |
| `judge0-redis` | `redis:7-alpine` | 0.1 CPU, 128 MB | Riêng cho Judge0 |

- Judge0 cần `privileged: true` để dùng isolate (cgroup). Giảm rủi ro: không mount thư mục host, mạng `sandbox` là `internal: true` (không ra Internet), không nối mạng `internal`/`edge`, `enable_network=false` cho mọi submission.
- `judge0.conf`: `ENABLE_PER_PROCESS_AND_THREAD_TIME_LIMIT=true`, `ENABLE_PER_PROCESS_AND_THREAD_MEMORY_LIMIT=true`, `MAX_MEMORY_LIMIT` đủ cho JVM (như demo), `MAX_CPU_TIME_LIMIT=10`, `MAX_FILE_SIZE=1024`, `MAX_PROCESSES_AND_OR_THREADS=64`, `ENABLE_NETWORK=false`, `ALLOW_ENABLE_NETWORK=false`.
- `backend` và `worker` nối thêm mạng `sandbox` để gọi `judge0-server`.

## 3. Gemini

- Backend, worker gọi `generativelanguage.googleapis.com:443` (đã mở ở U05).
- Bảng giá model (USD/1M token vào/ra) là cấu hình triển khai `U13_MODEL_PRICES`; cập nhật bằng lần triển khai mới khi Google đổi giá. Model theo loại việc là mục Settings, không cần triển khai lại.

## 4. Migration

`db/migration/aiexecution/V20260925_2000__create_ai_suggestions_code_runs.sql` (chưa áp dụng ở môi trường nào nên sửa tại chỗ, thay cho `V20260925_2000__u13_ai.sql` của bản trước):

- Không tạo `ai_services` (cấu hình chuyển sang `system_settings` của U03; U13 không seed gì, mục mặc định do `SettingDefinitionRegistry` tạo lúc khởi động).
- `ai_suggestions`: `task_type` (check 6 giá trị gồm `MATERIAL_SUMMARY`), `model`, FK `requested_by` → `accounts`, `hold_id` FK tự tham chiếu, `request_ref` unique khi khác rỗng, `reserve_expires_at`; partial unique `(target_id) WHERE target_type = 'PRACTICE_ATTEMPT' AND status IN ('QUEUED','RUNNING','READY')` (một kết quả hợp lệ mỗi attempt, vẫn cho bấm lại sau `FAILED`/`NO_CREDIT`); index `(created_at, task_type)` cho số liệu Admin Dashboard, `(requested_by, created_at)` cho lịch sử dùng credit, `(credit_status, reserve_expires_at)` cho scanner trả credit, `(hold_id)`.
- `code_runs`: `kind` (`VERIFY`, `GRADE`), `question_id`, `content_hash`, `assignment_id`, `attempt_id`, `requested_by`, `status`, `passed_all`, `score`, `results` jsonb, thời điểm; index `(question_id, content_hash, created_at DESC)`; partial unique `(attempt_id) WHERE kind = 'GRADE' AND status IN ('QUEUED','RUNNING','DONE')`; index `(status, created_at)` cho sweeper. Không FK sang `questions`, `attempts` để U13 chạy migration độc lập thứ tự wave (ID kiểm qua port).
- `REVOKE DELETE ON ai_suggestions, code_runs FROM app`.

## 5. Tài nguyên VPS

Tổng giới hạn các container sau khi thêm Judge0 ≈ 7 GB → VPS gợi ý **4 vCPU, 8 GB RAM, 60 GB SSD**. VPS nhỏ hơn: giảm Judge0 workers về 1, `U05_SCAN_CONCURRENCY=2`, `U13_CODE_CONCURRENCY=1`.

## 6. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | N/A | Judge0 không public |
| SECURITY-09 | Compliant | `JUDGE0_AUTH_TOKEN`, `GEMINI_API_KEY` trong secret CI/CD, không trong `system_settings` |
| RESILIENCY-04 | Compliant | Judge0 cùng Compose, image cố định phiên bản |
| RESILIENCY-06 | Compliant | Healthcheck `judge0-server` (`/languages`) |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |

## Credit hold/checkpoint revision 2026-10-09

`ai_suggestions.request_ref` unique khi không rỗng: HOLD và mỗi chunk/merge/embedding dùng khóa riêng. Dòng gọi có hold_id FK tới dòng HOLD, credit_status NONE; result chứa checkpoint có cấu trúc, không prompt thô. Complete phải cộng credits_used/checkpoint một lần trong cùng transaction bằng điều kiện status; release HOLD chuyển RESERVED → SETTLED/RELEASED idempotent, settle lượng thật. Tra hold bằng target LESSON/request_ref HOLD để worker restart không mất tham chiếu. Không thêm bảng job/checkpoint riêng.

Metadata scanClaimId và checkpoint kind/sourceHash/model/payload nằm trong JSON result của child call, không thêm bảng/cột riêng. AiUsageService trả HoldSnapshot/UsageStart/CallSnapshot và đọc HOLD theo target/requestRef; chỉ U13 truy cập repository này. Transaction U05 kiểm claim rồi gọi U13 ghi checkpoint/usage; complete/release khóa cùng HOLD. Ticket cũ hoặc HOLD đã chốt bị từ chối, READY replay không ghi lại.
