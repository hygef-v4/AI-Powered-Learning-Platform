# U11 Attempt & Submission - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `AttemptStarter`, `DraftSaver`, `AttemptSubmitter`, `AttemptQueryService` | `backend` |
| `AutoSubmitHandler`, `RetiredListener` | `worker` |
| Bảng `submissions`, `submission_contents` | `postgres` |
| Rate limit lưu | `redis`, khóa `u11:save:{learnerId}` |
| Queue | `jobs.u11.auto-submit`; `u11.retired-listener` bind `platform.events` routing key `u08.assignment.retired` |
| Event phát | `u11.submission.submitted` trên `platform.events` |

## 2. Nginx

- `PUT /api/v1/attempts/*/content`: `client_max_body_size 12m`, `gzip` request được backend tự giải nén (Nginx chỉ chuyển tiếp).

## 3. Migration

`V20260925_1800__u11_attempts.sql`:
- `submissions` theo `domain-entities.md` §2; unique `(publication_id, learner_id, attempt_no)`; partial unique `(publication_id, learner_id) WHERE status = 'IN_PROGRESS'`; index `(publication_id, status)`, `(learner_id)`.
- `submission_contents (attempt_id PK FK, content jsonb, content_version, warnings jsonb)`.
- Trigger `trg_submission_contents_immutable` chặn UPDATE khi lượt `SUBMITTED`.

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |
