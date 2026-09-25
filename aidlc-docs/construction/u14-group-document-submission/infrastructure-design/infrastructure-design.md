# U14 Group Document & Submission - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `SectionService`, `GroupSubmitter`, `SseHub`, `MembershipListener`, `GroupSubmissionQueryService` | `backend` |
| `GroupDocInitializer`, `AutoSubmitHandler` | `worker` |
| Bảng `group_documents`, `sections`, `section_revisions`, `section_comments`, `group_submissions` | `postgres` |
| Rate limit lưu nháp mục | `redis`, khóa `u14:save:{learnerId}` |
| RabbitMQ | fanout `platform.realtime` (mỗi backend một queue exclusive auto-delete); queue `jobs.u14.auto-submit`; listener `u08.assignment.opened`, `u08.assignment.retired`, `u12.group.membership-changed`; phát `u14.group.submitted` |

## 2. Nginx

- `GET /api/v1/group-docs/*/events` (SSE): `proxy_buffering off`, `proxy_cache off`, `proxy_read_timeout 1h`, `proxy_http_version 1.1`, header `Connection ""`, `X-Accel-Buffering: no` từ backend.
- `PUT /api/v1/group-docs/*/sections/*/draft`: `client_max_body_size 12m`.

## 3. Backend

- Tomcat async bật (mặc định Spring MVC); `spring.mvc.async.request-timeout` = 30 phút cho SSE.
- Số kết nối tối đa Tomcat 400 (100 SSE + request thường).

## 4. Migration

`V20260925_2100__u14_group_documents.sql`:
- `group_documents` unique `(publication_id, group_id)`.
- `sections` index `(group_document_id, order_no)`, `(claimed_by)`.
- `section_revisions`, `group_submissions`: `REVOKE UPDATE, DELETE ... FROM app`.
- `section_comments` index `(section_id, created_at)`.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | SSE qua HTTPS cùng domain |
| Rule còn lại | N/A | Dùng chung deploy, health, secret của backend |
