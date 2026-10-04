# U03 File, Job & Event - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U03. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story/UC**: không có trực tiếp (hạ tầng dùng chung, kiểm ở gate G1). Phục vụ U01 (avatar), U05 (học liệu), U06/U09/U11/U14 (ảnh trong tài liệu); việc nền, worker và sự kiện thông báo cho mọi unit (chuyển từ U02, 2026-10-04).
- **Thứ tự**: unit code **đầu tiên** của dự án, song song với U02. Làm Nhóm K (khung) → Nhóm J (việc nền) → phần tệp (Bước 0-28). U01 code sau U03 và U02.
- **Thiết kế nguồn**: `construction/u03-file-and-artifact/` (functional-design, nfr-requirements, nfr-design, infrastructure-design) và `construction/shared-infrastructure.md`.
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

U03 dựng khung dự án ở Nhóm K (Bước K1-K6, chuyển từ plan U01). Các unit khác kiểm ở Bước 0 của mình.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` (quyền upload theo `purpose`, vai trò người dùng) | U01 (`C`) | U01 code sau: adapter giả **luôn từ chối** (fail closed), test dùng mock; U01 Bước 15 thay bằng bản thật. Việc nền (Nhóm J) không cần kiểm quyền nên chạy trọn được |
| `AuditPort` | U02 (`C`) | U02 code song song: chưa có thì adapter giả ghi log; thay bằng U02 thật khi xong |

### Dữ liệu U03 sở hữu

Không có bảng PostgreSQL; metadata tệp ở `appProperties` trên Google Shared Drive; Redis `file:download-token:*`. RabbitMQ: exchange `jobs`, `jobs.retry`, `platform.events`; 7 queue việc nền `jobs.triggered`, `jobs.email`, `jobs.gemini`, `jobs.youtube`, `jobs.code`, `jobs.drive`, `jobs.payos`; 5 queue thử lại `jobs.retry.*`. Không có bảng job hay outbox.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  files/
    api/                FileUploadController, DownloadController, DTO
    application/        UploadService, ArtifactService (ArtifactPort),
                        DownloadTokenService, ContentInspector
    domain/             StoredFile, FileRef, ArtifactPurpose, PurposePolicy,
                        FileNameSanitizer
    infrastructure/     FileRefSigner (HMAC), GoogleDriveStorageAdapter,
                        LocalFolderStorageAdapter, DriveHealthIndicator
    worker/             DriveJobHandler
    port/               ArtifactPort, StoragePort
  jobs/                   (việc nền, chuyển từ U02)
    application/        JobPublisher, EventPublisher
    domain/             JobMessage, DomainEventMessage, BackoffPolicy
    infrastructure/     AmqpPublisher, AmqpTopology
    worker/             JobListener, JobRetryPublisher, JobHandlerRegistry,
                        PendingSweepRunner, ScheduledScanRunner
    port/               JobPort, EventPublisherPort, JobHandler, PendingSweeper,
                        ScheduledScanner
    adapter/fake/       FakeAuthorizationPort (từ chối), LoggingAuditAdapter
/frontend/src/shared/files/          FileUploader, FileLink, useFileUpload
/frontend/src/shared/status/         StatusBadge, usePollStatus
/contracts/messages/u03-*.json
/contracts/openapi/u03-files.yaml
```

## 3. Các bước

### Nhóm K - Khung dự án (dùng chung, chuyển từ plan U01)

- [ ] **Bước K1** - Tạo `/backend/pom.xml`: Spring Boot 3.x, Java 17, Web, Security, Data JPA, Validation, Data Redis, Actuator, Flyway, PostgreSQL, Spring AMQP, Mail, Bucket4j + Redis, jjwt, Commons CSV, Testcontainers (PostgreSQL, Redis, RabbitMQ), JUnit 5. Khóa phiên bản.
- [ ] **Bước K2** - `PlatformApplication`, `application.yml` (đọc cấu hình từ biến môi trường; mỗi unit thêm khóa của mình), profile `worker`, `application-local.yml`.
- [ ] **Bước K3** - Hạ tầng dùng chung trong `shared/`: global error handler trả problem-details an toàn, filter correlation ID, bộ che dữ liệu nhạy cảm trong log, cấu hình Spring Security mặc định từ chối, `ForbiddenKeyGuard` (khóa `password`, `otp`, `token`, `secret`, `phone`) dùng chung cho audit (U02) và payload việc nền (U03).
- [ ] **Bước K4** - Khung `/frontend`: Next.js + TypeScript strict + Tailwind, ESLint, Vitest + Testing Library, `src/lib/api` gửi cookie, component UI cơ bản (Button, Input, PasswordField, OtpInput, Dialog, Table, Alert).
- [ ] **Bước K5** - `/infra/docker-compose.yml` và `docker-compose.local.yml`: nginx, frontend, backend, postgres (`pgvector/pgvector:pg16`), redis, rabbitmq, mailpit (local); mạng `edge`/`internal`, giới hạn tài nguyên, healthcheck. Service `worker` (cùng image backend, profile `worker`, chỉ mạng `internal`, healthcheck); RabbitMQ vhost `/platform`, user `app`, tắt `guest`; PostgreSQL hai user `migrator` (Flyway) và `app` (runtime).
- [ ] **Bước K6** - `.github/workflows/ci.yml`: test backend + frontend, build image tag SHA. Bước deploy qua SSH để dạng khung, chưa bật.

### Nhóm J - Việc nền, worker và sự kiện (chuyển từ U02, 2026-10-04)

- [ ] **Bước J1** - Biến cấu hình việc nền theo `logical-components.md` §4 (`U03_JOB_*`, `U03_SWEEP_*`, `U03_SCAN_INTERVAL`, `RABBITMQ_*`).
- [ ] **Bước J2** - Domain: `JobMessage` (`schemaVersion`, `jobType`, `idempotencyKey`, `payload`, `attempt`, `correlationId`), `DomainEventMessage`, `BackoffPolicy` (30 s, 1, 2, 4, 8 phút).
- [ ] **Bước J3** - Port: `JobPort`, `EventPublisherPort`, `JobHandler` (xử lý + `onFailed`), `PendingSweeper`, `ScheduledScanner`. Port dùng: `AuthorizationPort` (U01) → `FakeAuthorizationPort` luôn từ chối; `AuditPort` (U02) → adapter giả ghi log nếu U02 chưa xong.
- [ ] **Bước J4** - `JobPublisher`: kiểm payload bằng `ForbiddenKeyGuard`; có transaction thì gửi sau commit, rollback không gửi; không có transaction thì gửi ngay. `EventPublisher` gửi `platform.events` sau commit, lỗi chỉ log (BR-U03-50…52, 70, 71, P8).
- [ ] **Bước J5** - `AmqpTopology` khai báo exchange `jobs`, `jobs.retry`, `platform.events`, 7 queue việc nền và 5 queue thử lại (TTL cố định, dead-letter về `jobs`) bằng `Declarables`; `AmqpPublisher` với publisher confirm 2 s.
- [ ] **Bước J6** - Worker: `JobHandlerRegistry` (`jobType` → handler, `onFailed`, queue), `JobListener` cho 7 queue với số luồng theo P11 (`jobs.email` là priority queue), `JobRetryPublisher` (vào `jobs.retry.*` theo lượt, hết 5 lượt gọi `onFailed`) (BR-U03-53, 56, 57, 63, P9, P11).
- [ ] **Bước J7** - `PendingSweepRunner` và `ScheduledScanRunner` mỗi phút (BR-U03-58, 60, P10).
- [ ] **Bước J8** - Unit test mọi `BR-U03-50…71`.
- [ ] **Bước J9** - Integration test Testcontainers (PostgreSQL, RabbitMQ): gửi sau commit, rollback không gửi, gửi ngay khi không có transaction, retry theo backoff, `onFailed` sau 5 lượt, sweeper gửi lại, scanner chạy lại không trùng (NFR-U03-55).
- [ ] **Bước J10** - JSON schema message: `/contracts/messages/u03-job-message.json`, `u03-domain-event.json`.
- [ ] **Bước J11** - Frontend dùng chung: `usePollStatus` (poll 3 giây, dừng ở trạng thái cuối), `StatusBadge`; test hook dừng poll.
- [ ] **Bước J12** - README: chạy worker, xem RabbitMQ qua SSH tunnel, cách một unit thêm loại việc nền (chọn 1 trong 7 queue theo BR-U03-63, đăng ký handler, `onFailed`, `PendingSweeper`; không tạo queue mới) và việc hẹn giờ (đăng ký `ScheduledScanner`).


### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm Nhóm K và J đã xong (phần tệp dùng `JobPort` cho `DRIVE_CLEANUP`).
- [ ] **Bước 1** - Thêm vào `pom.xml`: `google-api-services-drive` v3, `google-auth-library-oauth2-http`, `tika-core`. Thêm biến cấu hình U03 theo `logical-components.md` §3 và cấu hình multipart (`max-file-size=50MB`, `max-request-size=51MB`, `file-size-threshold=0`, `location=/tmp/uploads`).
- [ ] **Bước 2** - Docker Compose: volume `upload-tmp` gắn vào `backend`; volume `files-local` cho local; truyền `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY`, `GOOGLE_SHARED_DRIVE_ID` vào `backend` và `worker`. Nginx: `client_max_body_size 50m`, riêng `/api/v1/files` `proxy_request_buffering off` và `proxy_read_timeout 120s`.

### Nhóm B - Domain và logic

- [ ] **Bước 3** - Domain: `StoredFile` (metadata trong `appProperties`), `FileRef` có chữ ký HMAC hạn 1 giờ; `ArtifactPurpose` (`AVATAR`, `MATERIAL`, `DOCUMENT_IMAGE`); `PurposePolicy` (allowlist loại file, trần 50 MB / 5 MB, vai trò được upload); `FileNameSanitizer` (BR-U03-02, 03, 04, 08).
- [ ] **Bước 4** - Port: `ArtifactPort` (`store`, `attach`, `issueDownloadToken`, `open`), `StoragePort`. `AvatarPort` (chữ ký theo thiết kế U01: `validateAvatar(fileRef, actor)` → `fileId`): U03 code trước nên tạo interface và cài ở Bước 7; U01 dùng khi được code.
- [ ] **Bước 5** - `ContentInspector`: Tika trên 8 KB đầu, so allowlist của `purpose`; lỗi trả thông điệp chung (BR-U03-03, P3).
- [ ] **Bước 6** - `UploadService`: `Semaphore` 5 permit (hết → `503`), kiểm quyền, kiểm nội dung, SHA-256, đẩy file và `appProperties` qua `StoragePort` (không có bảng), dọn bù trừ khi lỗi (xóa thử 3 lần, vẫn lỗi thì gửi việc `DRIVE_CLEANUP`), `finally` xóa file tạm (BR-U03-01…07, 41, P1).
- [ ] **Bước 7** - `ArtifactService`: `attach` (kiểm chữ ký, hạn, người tải lên, `purpose`; trả `fileId` cho unit sở hữu lưu), `open`, `validateAvatar` (cài `AvatarPort` cho U01); không có hàm xóa (BR-U03-30…32, F4, F5).
- [ ] **Bước 8** - `DownloadTokenService`: token 32 byte base64url, Redis `file:download-token:{sha256}` TTL 5 phút gắn `accountId`; kiểm chủ token khi dùng, sai thì `404` và audit (BR-U03-20, 21, 40, P4).
- [ ] **Bước 9** - `DriveJobHandler` trong worker cho `DRIVE_CLEANUP`; đăng ký với `JobHandlerRegistry` (Bước J6) vào queue `jobs.drive`.
- [ ] **Bước 10** - Audit các sự kiện của BR-U03-40; không log `providerFileId`, key, token.
- [ ] **Bước 11** - Unit test cho mọi `BR-U03-xx`, gồm file đổi đuôi, file vượt trần theo `purpose` (50 MB / 5 MB), upload thứ 6 bị `503`, token dùng sai người, dọn Drive khi ghi `appProperties` lỗi.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u03-file-and-artifact/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu và lưu trữ

- [ ] **Bước 13** - Không có migration (U03 không có bảng); kiểm `appProperties` ghi đủ `ownerAccountId`, `purpose`, `mediaType`, `originalFileName`, `sha256`.
- [ ] **Bước 14** - `FileRefSigner`: khóa HMAC từ biến môi trường `U03_FILEREF_SECRET`, xoay khóa bằng triển khai lại.
- [ ] **Bước 15** - `GoogleDriveStorageAdapter`: đọc key base64 từ `.env`, thư mục theo `purpose` (tạo khi khởi động), timeout 5/60/120 s, retry 3 lần cho 429/5xx, nhận diện `cannotDownloadAbusiveFile` → lỗi "file không khả dụng" (P5, P6, BR-U03-24).
- [ ] **Bước 16** - `LocalFolderStorageAdapter`; chọn adapter theo có key hay không, cảnh báo khi khởi động (P5).
- [ ] **Bước 17** - `DriveHealthIndicator`: `drives.get` cache 60 s, Drive lỗi không làm backend `DOWN` (P7).
- [ ] **Bước 18** - Integration test Testcontainers (Redis) với `LocalFolderStorageAdapter`: upload → attach → cấp token → tải; token và `FileRef` hết hạn; ghi `appProperties` lỗi thì dọn Drive hoặc gửi việc `DRIVE_CLEANUP`; adapter Drive test bằng mock HTTP (lỗi 429, abuse).
- [ ] **Bước 19** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 20** - `/contracts/openapi/u03-files.yaml`: `POST /api/v1/files` (multipart `purpose`, `file`), `GET /api/v1/files/download/{token}`.
- [ ] **Bước 21** - `FileUploadController`, `DownloadController` (`StreamingResponseBody`, bộ đệm 64 KB, header `Content-Type`, `Content-Disposition` RFC 5987, `nosniff`, `Cache-Control: private, no-store`; SVG thêm CSP sandbox; Drive báo abuse → `410`) (BR-U03-10, 22, 23, P2).
- [ ] **Bước 22** - Test MockMvc: không trả `providerFileId`, header đúng (gồm CSP cho SVG), sai `purpose`/vai trò bị từ chối, file quá lớn `413`.
- [ ] **Bước 23** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 24** - `useFileUpload` (XHR có tiến trình và hủy), `FileUploader` (chọn/kéo thả, kiểm sơ bộ đuôi và dung lượng), `FileLink` (lấy URL khi bấm, không lưu lâu).
- [ ] **Bước 25** - Test frontend: chặn file vượt trần theo `purpose` phía client, hủy upload, hiển thị lỗi backend.
- [ ] **Bước 26** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 27** - Cập nhật `README.md`: tạo service account, thêm vào Shared Drive, mã hóa key base64 vào `.env`; chạy local không có key; cách unit khác dùng `ArtifactPort`.
- [ ] **Bước 28** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| F1 Upload | 3, 5, 6, 13-16, 20-22, 24 |
| F2 Gắn | 7 |
| F3 Tải về | 8, 15, 21 |
| F4 Worker đọc file | 7, 15 |
| F5 Kiểm ảnh đại diện | 4, 7 |
| NFR-U03 hiệu năng, Drive, bảo mật | 1, 2, 5, 6, 8, 15-17, 21 |
| Gate G1 (hợp đồng U03) | 11, 18, 20, J8, J9 |
| Khung dự án | K1-K6 |
| Việc nền J1–J4, sự kiện E1 | J1-J12 |

## 5. Ngoài phạm vi

- Kiểm quyền nghiệp vụ trước khi cấp token (thuộc unit sở hữu).
- Kiểm và rút gọn XML Draw.io (U09, trong bộ nhớ); file dẫn xuất; xóa file.
- Adapter `AuthorizationPort` thật (U01 Bước 15) và `AuditPort` thật (U02).
- Handler nghiệp vụ, `PendingSweeper` và `ScheduledScanner` của từng unit (thuộc unit sở hữu).
