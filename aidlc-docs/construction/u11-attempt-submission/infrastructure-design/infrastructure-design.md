# U11 Attempt & Submission - Infrastructure Design

**Bản tài liệu 2026-10-09**: UC 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); danh sách quiz của học liệu trên Learning Material (UC 15, màn của U05); primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `AttemptStarter`, `DraftSaver`, `AttemptSubmitter`, `AttemptQueryService`, `StudentViewService` | `backend` |
| Bảng `attempts` (gồm nội dung bài làm) | `postgres` |
| Rate limit lưu | `redis`, khóa `ratelimit:attempt-save:{accountId}` |
| Tự nộp | `AttemptDeadlineScanner` (scanner U03) trong `worker` |
| Event phát | Không; chỉ bài tập `GRADED` báo U15 qua `SubmissionSubmittedPort` trong transaction; quiz và Practice có kết quả riêng |

## 2. Nginx

- `PUT /api/v1/attempts/*/content`: `client_max_body_size 12m`, `gzip` request được backend tự giải nén (Nginx chỉ chuyển tiếp).
- `POST /api/v1/attempts/*/docx:preview`: `client_max_body_size 20m`, `proxy_read_timeout 60s` (theo `shared-infrastructure.md`).

## 3. Migration

`db/migration/attempts/V20260925_1800__create_attempts.sql` (đổi tên từ `V20260925_1800__u11_attempts.sql`; chưa áp dụng nên sửa trực tiếp):
- `attempts` theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md): FK `assignment_id` → `assignments`, `class_id` → `course_classes`, `account_id` → `accounts`; unique `(assignment_id, account_id, attempt_no)`; partial unique `(assignment_id, account_id) WHERE status = 'IN_PROGRESS'`; index `(assignment_id, class_id, status)`, `(account_id, assignment_id)`, `(status, deadline_at)` cho scanner.
- `deadline_at` cho phép NULL (quiz không giới hạn giờ); scanner chỉ chọn dòng có `deadline_at`.
- `attempts` có `snapshot jsonb`, `content jsonb` (gồm cảnh báo kiểm và thời điểm lưu cuối), `content_version`, `run_result jsonb`, `submit_mode`; truy vấn danh sách lượt chỉ chọn cột metadata, không đọc `content`.
- Trigger `trg_attempts_immutable` chặn UPDATE `content` khi lượt đã `SUBMITTED`.

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |
