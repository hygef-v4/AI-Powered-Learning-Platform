# U13 AI & Code Execution - Infrastructure Design

**Bản tài liệu 2026-10-08**: không primary UC; primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `AiGuard`, `AiSuggestionService`, `AiUsageService` (`AiUsagePort` cho U05), `CodeRunService` (TRY đồng bộ), `CodeLabCheckService`, `AiAdminService` | `backend` |
| `AiTaskHandler`, `CodeRunHandler`, `CreditReservationScanner`, `AiPendingSweeper` | `worker` |
| Bảng `ai_services`, `ai_suggestions`; kết quả chạy code trong `attempts.run_result` (U11) và `questions.definition` (U06) | `postgres` |
| Trần chi phí ngày, rate limit | `redis`, khóa `gemini:daily-cost:*`, `ratelimit:ai-request:*`, `ratelimit:code-try:*` |
| Queue | `jobs.gemini`, `jobs.code` |
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
- Bảng giá model (USD/1M token vào/ra) là cấu hình triển khai `U13_MODEL_PRICES`; cập nhật bằng lần triển khai mới khi Google đổi giá.

## 4. Migration

`V20260925_2000__u13_ai.sql`: `ai_services` (unique `task_type`; seed `GLOBAL` và 6 loại việc gồm `SKELETON_DRAFT`, `PRACTICE_GRADING`, `EMBEDDING` với model mặc định), `ai_suggestions` (FK `ai_service_id`, `requested_by`; partial unique `(target_id) WHERE target_type = 'PRACTICE_ATTEMPT' AND status IN ('QUEUED','RUNNING','READY')` (một kết quả hợp lệ mỗi attempt, vẫn cho bấm lại sau `FAILED`/`NO_CREDIT`); index `(created_at, ai_service_id)`, `(requested_by, created_at)`, `(credit_status, created_at)` cho scanner trả credit); `REVOKE DELETE ON ai_suggestions FROM app`.

## 5. Tài nguyên VPS

Tổng giới hạn các container sau khi thêm Judge0 ≈ 7 GB → VPS gợi ý **4 vCPU, 8 GB RAM, 60 GB SSD**. VPS nhỏ hơn: giảm Judge0 workers về 1, `U05_SCAN_CONCURRENCY=2`, `U13_CODE_CONCURRENCY=1`.

## 6. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | N/A | Judge0 không public |
| SECURITY-09 | Compliant | `JUDGE0_AUTH_TOKEN`, `GEMINI_API_KEY` trong secret CI/CD |
| RESILIENCY-04 | Compliant | Judge0 cùng Compose, image cố định phiên bản |
| RESILIENCY-06 | Compliant | Healthcheck `judge0-server` (`/languages`) |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |
