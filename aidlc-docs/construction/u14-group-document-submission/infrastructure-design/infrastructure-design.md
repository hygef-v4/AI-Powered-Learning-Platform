# U14 Group Document & Submission - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `SectionService`, `GroupSubmitter`, `SseHub`, `GroupChangeAdapter`, `PublicationLifecycleAdapter`, `GroupSubmissionQueryService` | `backend` |
| `GroupDocInitializer`, `AutoSubmitHandler` | `worker` |
| Bảng `group_documents`, `sections`, `section_revisions`, `section_comments`, `group_submissions` | `postgres` |
| Rate limit lưu nháp mục | `redis`, khóa `ratelimit:section-save:{studentId}` |
| RabbitMQ | fanout `platform.realtime` (mỗi backend một queue tạm `jobs.realtime.{instanceId}`, exclusive, auto-delete); queue `jobs.triggered` (job `GROUP_DOC_CREATE`), `jobs.scheduled` (job `GROUP_AUTO_SUBMIT`); không nghe event: U08, U12 báo qua port trong transaction; phát `group.submitted` (chỉ cho thông báo U16) |

## 2. Nginx

- `GET /api/v1/group-docs/*/events` (SSE): `proxy_buffering off`, `proxy_cache off`, `proxy_read_timeout 1h`, `proxy_http_version 1.1`, header `Connection ""`, `X-Accel-Buffering: no` từ backend.
- `PUT /api/v1/group-docs/*/sections/*/draft`: `client_max_body_size 12m`.

## 3. Backend

- Tomcat async bật (mặc định Spring MVC); `spring.mvc.async.request-timeout` = 30 phút cho SSE.
- Số kết nối tối đa Tomcat 400 (100 SSE + request thường).

## 4. Migration

`V20260925_2100__u14_group_workspace.sql`:
- `group_documents (id, group_id, publication_id, status, shared_blocks jsonb, updated_at, version)` unique `(group_id, publication_id)`; `group_id`, `publication_id` tham chiếu U12, U08 (không FK chéo unit).
- `sections (id, group_document_id FK, parent_section_id FK, origin, order_no, title, status, claimed_by, claimed_at, assigned_by, published_blocks, draft_blocks, last_author_id, version)` index `(group_document_id, parent_section_id, order_no)`, `(claimed_by)`; `CHECK (origin <> 'GROUP' OR parent_section_id IS NOT NULL)`.
- `group_submissions (…, group_document_id FK, …)`.
- `section_revisions`, `group_submissions`: `REVOKE UPDATE, DELETE ... FROM app`.
- `section_comments` index `(section_id, created_at)`.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | SSE qua HTTPS cùng domain |
| Rule còn lại | N/A | Dùng chung deploy, health, secret của backend |
