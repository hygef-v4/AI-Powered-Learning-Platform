# U13 AI & Code Execution - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `AiGuard`, `AiProposalService`, `CodeRunService` (TRY đồng bộ), `CodeLabCheckService`, `AiAdminService` | `backend` |
| `AiTaskHandler`, `CodeRunHandler` | `worker` |
| Bảng `ai_task_configs`, `ai_global_settings`, `ai_calls`, `ai_proposals`, `solution_verifications`, `code_runs` | `postgres` |
| Trần chi phí ngày, rate limit | `redis`, khóa `u13:cost:*`, `u13:ai:*`, `u13:try:*` |
| Queue | `jobs.u13.ai-task`, `jobs.u13.code-run` |
| Event | `u13.code.graded`, `u13.ai.proposal-ready` trên `platform.events` |
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
- Bảng giá model (USD/1M token vào/ra) để trong `ai_task_configs`; admin cập nhật khi Google đổi giá.

## 4. Migration

`V20260925_2000__u13_ai_code.sql`: 6 bảng ở §1; seed `ai_task_configs` 4 việc với model mặc định; `REVOKE UPDATE, DELETE ON ai_calls FROM app`; index `ai_calls (created_at, task)`, `code_runs (owner_ref)`.

## 5. Tài nguyên VPS

Tổng giới hạn các container sau khi thêm Judge0 ≈ 7 GB → VPS gợi ý **4 vCPU, 8 GB RAM, 60 GB SSD**. VPS nhỏ hơn: giảm Judge0 workers về 1, `U05_INGEST_CONCURRENCY=2`, `U13_CODE_CONCURRENCY=1`.

## 6. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | N/A | Judge0 không public |
| SECURITY-09 | Compliant | `JUDGE0_AUTH_TOKEN`, `GEMINI_API_KEY` trong secret CI/CD |
| RESILIENCY-04 | Compliant | Judge0 cùng Compose, image cố định phiên bản |
| RESILIENCY-06 | Compliant | Healthcheck `judge0-server` (`/languages`) |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |
