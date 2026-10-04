# U14 Group Document & Submission - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `SectionService`, `GroupSubmitter`, `SseHub`, `GroupChangeAdapter`, `AssignmentLifecycleAdapter`, `GroupSubmissionQueryService` | `backend` |
| `GroupDocInitializer`, `GroupAutoSubmitScanner` | `worker` |
| Bảng `group_documents` | `postgres` |
| Rate limit lưu nháp mục | `redis`, khóa `ratelimit:section-save:{accountId}` |
| RabbitMQ | fanout `platform.realtime` (mỗi backend một queue tạm `jobs.realtime.{instanceId}`, exclusive, auto-delete); queue `jobs.triggered` (việc `GROUP_DOC_CREATE`); tự nộp do `ScheduledScanner` mỗi phút, không qua queue; không nghe event: U08, U12 báo qua port trong transaction; phát `group.submitted` (chỉ cho thông báo U16) |

## 2. Nginx

- `GET /api/v1/group-docs/*/events` (SSE): `proxy_buffering off`, `proxy_cache off`, `proxy_read_timeout 1h`, `proxy_http_version 1.1`, header `Connection ""`, `X-Accel-Buffering: no` từ backend.
- `PUT /api/v1/group-docs/*/sections/*/draft`: `client_max_body_size 12m`.

## 3. Backend

- Tomcat async bật (mặc định Spring MVC); `spring.mvc.async.request-timeout` = 30 phút cho SSE.
- Số kết nối tối đa Tomcat 400 (100 SSE + request thường).

## 4. Migration

`V20260925_1850__u14_group_workspace.sql`:
- `group_documents (id, group_id FK, assignment_id FK, status, sections jsonb, revisions jsonb, submitted_snapshot jsonb, submit_mode, submitted_at, updated_at, version)` unique `(group_id, assignment_id)`; FK tới `student_groups`, `assignments` theo [database](../../../../docs/database.md).
- `sections` = `{sharedBlocks, items[]}` (mỗi mục: id, partId, orderNo, title, status, claimedBy, claimedAt, assignedBy, publishedBlocks, draftBlocks, lastAuthorId); `revisions` là mảng chỉ thêm.
- Index `(assignment_id, submitted_at)` cho scanner tự nộp.
- Không có bảng `sections`, `section_revisions`, `section_comments`, `group_submissions`.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | SSE qua HTTPS cùng domain |
| Rule còn lại | N/A | Dùng chung deploy, health, secret của backend |
