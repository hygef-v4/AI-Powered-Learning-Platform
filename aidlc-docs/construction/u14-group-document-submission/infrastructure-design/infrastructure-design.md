# U14 Group Document & Submission - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `SectionService`, `GroupSubmitter`, `SseHub`, `GroupChangeAdapter`, `PublicationLifecycleAdapter`, `GroupSubmissionQueryService` | `backend` |
| `GroupDocInitializer`, `AutoSubmitHandler` | `worker` |
| Cột tài liệu nhóm trong `student_groups` (U12 tạo bảng); bảng `sections`, `section_revisions`, `section_comments`, `group_submissions` | `postgres` |
| Rate limit lưu nháp mục | `redis`, khóa `ratelimit:section-save:{learnerId}` |
| RabbitMQ | fanout `platform.realtime` (mỗi backend một queue tạm `jobs.realtime.{instanceId}`, exclusive, auto-delete); queue `jobs.triggered` (job `GROUP_DOC_CREATE`), `jobs.scheduled` (job `GROUP_AUTO_SUBMIT`); không nghe event: U08, U12 báo qua port trong transaction; phát `group.submitted` (chỉ cho thông báo U16) |

## 2. Nginx

- `GET /api/v1/group-docs/*/events` (SSE): `proxy_buffering off`, `proxy_cache off`, `proxy_read_timeout 1h`, `proxy_http_version 1.1`, header `Connection ""`, `X-Accel-Buffering: no` từ backend.
- `PUT /api/v1/group-docs/*/sections/*/draft`: `client_max_body_size 12m`.

## 3. Backend

- Tomcat async bật (mặc định Spring MVC); `spring.mvc.async.request-timeout` = 30 phút cho SSE.
- Số kết nối tối đa Tomcat 400 (100 SSE + request thường).

## 4. Migration

`V20260925_2100__u14_group_workspace.sql`:
- `ALTER TABLE student_groups ADD COLUMN doc_publication_id uuid, shared_blocks jsonb, doc_updated_at timestamptz, doc_version int` (cần migration U12 chạy trước).
- `sections` index `(group_document_id, order_no)`, `(claimed_by)`.
- `section_revisions`, `group_submissions`: `REVOKE UPDATE, DELETE ... FROM app`.
- `section_comments` index `(section_id, created_at)`.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | SSE qua HTTPS cùng domain |
| Rule còn lại | N/A | Dùng chung deploy, health, secret của backend |
