# U03 - Test Results (2026-10-05)

## Backend - `mvn test`: 69 test, 0 lỗi

| Lớp test | Số test | Phạm vi |
|---|---|---|
| `jobs.application.JobPublisherTest` | 5 | Gửi ngay khi không có transaction, sau commit, rollback không gửi, chặn khóa cấm, `jobType` lạ (BR-U03-50, 52) |
| `jobs.application.EventPublisherTest` | 3 | Sau commit, ngay, sai `schemaVersion`/khóa cấm (BR-U03-70, 71) |
| `jobs.domain.JobRoutingAndBackoffTest` | 4 | Mọi `jobType` có đúng một queue, OTP ưu tiên hơn email thông báo, 12 luồng, tên queue thử lại (BR-U03-56, 63) |
| `jobs.infrastructure.AmqpTopologyTest` | 3 | 3 exchange, 12 queue durable, priority `jobs.email`, TTL + DLX queue thử lại, 9 binding |
| `jobs.infrastructure.AmqpPublisherTest` | 3 | RabbitMQ lỗi không làm hỏng nghiệp vụ; RabbitMQ treo không chặn người gọi quá 50 ms; bản thử lại mang header `CC` và priority |
| `jobs.worker.JobListenerTest` | 10 | Channel chỉ ack sau handler, nack requeue khi broker từ chối bản thử lại; ack khi xong; lỗi tạm → queue thử lại `attempt + 1`; hết lượt/lỗi vĩnh viễn → `onFailed`; timeout = lỗi tạm; broker từ chối → requeue; sai `schemaVersion` bỏ; `onFailed` lỗi vẫn ack; sweeper gửi lại; scanner lỗi không chặn scanner khác |
| `jobs.worker.JobHandlerRegistryTest` | 2 | Sai queue, `jobType` lạ, trùng handler bị từ chối |
| `files.domain.FilesDomainTest` | 4 | Trần, vai trò theo `purpose`, `inline`, làm sạch tên (BR-U03-02, 04, 08, 23) |
| `files.application.ContentInspectorTest` | 2 | PNG/PDF/SVG/DOCX theo nội dung; tệp thực thi đổi đuôi không thành ảnh (BR-U03-03) |
| `files.application.UploadServiceTest` | 8 | Metadata đủ trường + `FileRef` hợp lệ; đổi đuôi → 415 + audit; vượt trần kể cả khai báo sai → 413; sai vai trò/`AuthorizationPort` từ chối → 403 + audit; upload thứ 6 → 503; ghi dở thì dọn ngay; dọn lỗi 3 lần → `DRIVE_CLEANUP` (khóa không chứa `fileId`); không còn tệp tạm |
| `files.application.ArtifactAndTokenTest` | 6 | `attach` đúng chủ/`purpose`; sai chủ, sai `purpose`, hết hạn, sửa chữ ký → từ chối; `validateAvatar`; token băm trong Redis TTL 5 phút; token sai người → 404 + audit |
| `files.infrastructure.StorageAndCleanupTest` | 4 | Adapter local đọc/ghi/xóa, chặn path traversal; production thiếu Drive → không khả dụng; `DRIVE_CLEANUP` chỉ retry lỗi tạm |
| `files.api.FileControllersTest` | 8 | Không trả `fileId`; `purpose` sai → 400; sai vai trò → 403; quá 5 MB → 413; SVG có CSP sandbox, `nosniff`, `no-store`, tên RFC 5987; DOCX `attachment`; abuse → 410; kho lỗi → 503; token người khác → 404 |
| `shared.*` | 6 | Che log, `ForbiddenKeyGuard` |

Ngoài bộ test: đã thử khởi động Spring context (bỏ DB) ở chế độ backend, worker, và với `U03_REQUIRE_DRIVE` true/false để kiểm chọn adapter; test tạm này không giữ trong repo.

## Frontend - `npm test`: 18 test, 0 lỗi; `eslint` và `tsc --noEmit` sạch

| Tệp test | Phạm vi |
|---|---|
| `shared/status/usePollStatus.test.tsx` | Poll 3 giây, dừng ở trạng thái cuối, dừng khi rời trang, không poll khi chưa bật; `StatusBadge` |
| `shared/files/FileUploader.test.tsx` | Kiểm sơ bộ đuôi/trần theo `purpose`; gửi `purpose` + cookie, tiến trình, trả `fileRef`; Hủy; hiện lỗi backend; `FileLink` chỉ lấy URL khi bấm |
| `lib/security/csp.test.ts` | CSP trang chỉ cho script có nonce, không `unsafe-inline`, giữ `frame-src` YouTube/Draw.io |
| `lib/api/client.test.ts`, `components/ui/OtpInput.test.tsx` | Khung dự án |

## Chạy thử hạ tầng (Docker Compose local, 2026-10-05)

| Kiểm | Kết quả |
|---|---|
| 8 container (nginx, frontend, backend, worker, postgres, redis, rabbitmq, mailpit) | Đều `healthy`; log backend/worker/frontend/nginx không có ERROR |
| PostgreSQL | Có `migrator`, `app`, extension `vector`, `pgcrypto`; `app` không tạo được bảng, đọc/ghi được bảng do `migrator` tạo |
| Redis | Bắt buộc mật khẩu |
| RabbitMQ | Chỉ user `app` (không có `guest`), vhost `/platform`; 3 exchange, 7 queue (`jobs.email` priority 10), 5 queue thử lại TTL + DLX `jobs`, 9 binding; consumer đúng P11 (tổng 12) |
| Worker `DRIVE_CLEANUP` | Gửi vào `jobs` → tệp bị xóa ngay; gửi vào `jobs.retry.30s` kèm `CC` → sau 30 giây quay về `jobs.drive`, tệp bị xóa; `schemaVersion` lạ → WARN, bỏ |
| API qua nginx chưa đăng nhập | `POST /api/v1/files`, link tải, API khác → `401` problem-details |
| Trang qua nginx | Header bảo mật đủ; CSP có nonce, trình duyệt không báo lỗi, React hydrate |

Lỗi phát hiện và đã sửa khi chạy thử:
- `01-roles.sh` bị Git đổi sang CRLF khi checkout trên Windows nên không chạy → thêm `.gitattributes` giữ LF cho `*.sh`, `Dockerfile`, `*.conf`, `*.template`, `*.yml`, `*.yaml`.
- CSP tĩnh của nginx chặn script inline của Next.js (React không chạy) → CSP trang chuyển sang Next.js có nonce, nginx chỉ gắn CSP cho `/api`.

## Không chạy

- Integration test (Bước J9, 18): tester riêng viết theo kịch bản trong plan.
- Upload/tải tệp đầu-cuối qua API: cần đăng nhập (U01 chưa có).
