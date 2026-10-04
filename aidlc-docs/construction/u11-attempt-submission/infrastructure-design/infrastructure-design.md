# U11 Attempt & Submission - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `AttemptStarter`, `DraftSaver`, `AttemptSubmitter`, `AttemptQueryService` | `backend` |
| Bảng `attempts` (gồm nội dung bài làm) | `postgres` |
| Rate limit lưu | `redis`, khóa `ratelimit:attempt-save:{accountId}` |
| Tự nộp | `AttemptDeadlineScanner` (scanner U03) trong `worker` |
| Event phát | Không; chỉ bài `GRADED` báo U15 qua `SubmissionSubmittedPort` trong transaction; Practice có kết quả riêng |

## 2. Nginx

- `PUT /api/v1/attempts/*/content`: `client_max_body_size 12m`, `gzip` request được backend tự giải nén (Nginx chỉ chuyển tiếp).

## 3. Migration

`V20260925_1800__u11_attempts.sql`:
- `attempts` theo [database](../../../../docs/database.md): FK `assignment_id`, `account_id`; unique `(assignment_id, account_id, attempt_no)`; partial unique `(assignment_id, account_id) WHERE status = 'IN_PROGRESS'`; index `(assignment_id, status)`, `(account_id)`, `(status, deadline_at)` cho scanner.
- `attempts` có `snapshot jsonb`, `content jsonb` (gồm cảnh báo kiểm và thời điểm lưu cuối), `content_version`, `run_result jsonb`, `submit_mode`; truy vấn danh sách lượt chỉ chọn cột metadata, không đọc `content`.
- Trigger `trg_attempts_immutable` chặn UPDATE `content` khi lượt đã `SUBMITTED`.

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |
