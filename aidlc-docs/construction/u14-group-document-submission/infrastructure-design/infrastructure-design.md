# U14 Group Document & Submission - Infrastructure Design

**Bản tài liệu 2026-10-09**: UC 27 và phần bài nhóm của UC 16, 23, 28, 37 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `GroupDocController`, `GroupDocSseController`, `SectionService`, `GroupSubmitter`, `SseHub`, `GroupChangeAdapter`, `AssignmentLifecycleAdapter`, `GroupSubmissionQueryService` | `backend` |
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

`db/migration/groupdocs/V20260925_1850__create_group_documents.sql` (tên cũ `V20260925_1850__u14_group_workspace.sql` chưa áp dụng ở môi trường nào thì đổi tên; đã áp dụng thì giữ và thêm migration forward-only):
- `group_documents (id, group_id FK, assignment_id FK, status, sections jsonb, revisions jsonb, submitted_snapshot jsonb, submit_mode, submitted_at, updated_at, version)` unique `(group_id, assignment_id)`; FK tới `student_groups`, `assignments` theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md).
- `sections` = `{sharedBlocks, items[]}` (mỗi mục: id, partId, headingBlockId, ancestorHeadingIds, orderNo, title, status, claimedBy, claimedAt, assignedBy, publishedBlocks, draftBlocks, lastAuthorId, version); `revisions` là mảng chỉ thêm. Thêm/bớt trường trong JSON không cần migration.
- Index `(assignment_id, status)` cho scanner tự nộp và bảng tiến độ của giảng viên; index `(group_id)` cho My Group.
- Không có bảng `sections`, `section_revisions`, `section_comments`, `group_submissions`.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | SSE qua HTTPS cùng domain |
| Rule còn lại | N/A | Dùng chung deploy, health, secret của backend |
