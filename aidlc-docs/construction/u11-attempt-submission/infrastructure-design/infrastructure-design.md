# U11 Attempt & Submission - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `AttemptStarter`, `DraftSaver`, `AttemptSubmitter`, `AttemptQueryService` | `backend` |
| `AutoSubmitHandler` | `worker` |
| Bảng `submissions` (gồm nội dung bài làm) | `postgres` |
| Rate limit lưu | `redis`, khóa `ratelimit:attempt-save:{learnerId}` |
| Queue | `jobs.scheduled` (job `ATTEMPT_AUTO_SUBMIT`) |
| Event phát | Không; báo U15 qua `SubmissionSubmittedPort` trong transaction |

## 2. Nginx

- `PUT /api/v1/attempts/*/content`: `client_max_body_size 12m`, `gzip` request được backend tự giải nén (Nginx chỉ chuyển tiếp).

## 3. Migration

`V20260925_1800__u11_attempts.sql`:
- `submissions` theo `domain-entities.md` §2; unique `(publication_id, learner_id, attempt_no)`; partial unique `(publication_id, learner_id) WHERE status = 'IN_PROGRESS'`; index `(publication_id, status)`, `(learner_id)`.
- `submissions` có `content jsonb`, `content_version`, `warnings jsonb`; truy vấn danh sách lượt chỉ chọn cột metadata, không đọc `content`.
- Trigger `trg_submissions_immutable` chặn UPDATE `content` khi lượt đã `SUBMITTED`.

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |
