# U03 File & Artifact - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U03. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story/UC**: không có trực tiếp (hạ tầng dùng chung, kiểm ở gate G1). Phục vụ U01 (avatar), U05 (học liệu), U06/U09/U11/U14 (ảnh trong tài liệu). Rút gọn ngày 2026-09-25: bỏ file Draw.io, file dẫn xuất, xóa file.
- **Thiết kế nguồn**: `construction/u03-file-and-artifact/` (functional-design, nfr-requirements, nfr-design, infrastructure-design) và `construction/shared-infrastructure.md`.
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` (quyền upload theo `purpose`, vai trò người dùng) | U01 | Nếu U01 chưa code: adapter giả **luôn từ chối** (fail closed), test dùng mock |
| `JobPort`, `JobHandler` (job `DRIVE_CLEANUP`) | U02 | Dùng U02 thật nếu đã code; chưa có thì chờ U02 Bước 4-7 (U03 mở sau U01/U02 theo `unit-of-work.md`) |
| `AuditPort` | U02 | Như trên |

### Dữ liệu U03 sở hữu

PostgreSQL `artifacts`; Redis `file:download-token:*`; thư mục trên Google Shared Drive; queue `jobs.drive`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  files/
    api/                FileUploadController, DownloadController, DTO
    application/        UploadService, ArtifactService (ArtifactPort),
                        DownloadTokenService, ContentInspector
    domain/             Artifact, ArtifactStatus, ArtifactPurpose, PurposePolicy,
                        FileNameSanitizer
    infrastructure/     ArtifactRepository (JPA), GoogleDriveStorageAdapter,
                        LocalFolderStorageAdapter, DriveHealthIndicator
    worker/             DriveJobHandler
    port/               ArtifactPort, StoragePort
/backend/src/main/resources/db/migration/u03/
/frontend/src/shared/files/          FileUploader, FileLink, useFileUpload
/contracts/openapi/u03-files.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - Thêm vào `pom.xml`: `google-api-services-drive` v3, `google-auth-library-oauth2-http`, `tika-core`. Thêm biến cấu hình U03 theo `logical-components.md` §3 và cấu hình multipart (`max-file-size=50MB`, `max-request-size=51MB`, `file-size-threshold=0`, `location=/tmp/uploads`).
- [ ] **Bước 2** - Docker Compose: volume `upload-tmp` gắn vào `backend`; volume `files-local` cho local; truyền `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY`, `GOOGLE_SHARED_DRIVE_ID` vào `backend` và `worker`. Nginx: `client_max_body_size 50m`, riêng `/api/v1/files` `proxy_request_buffering off` và `proxy_read_timeout 120s`.

### Nhóm B - Domain và logic

- [ ] **Bước 3** - Domain: `Artifact` với trạng thái `ACTIVE`/`BLOCKED`; `ArtifactPurpose` (`AVATAR`, `MATERIAL`, `DOCUMENT_IMAGE`); `PurposePolicy` (allowlist loại file, trần 50 MB / 5 MB, vai trò được upload); `FileNameSanitizer` (BR-U03-02, 03, 04, 08).
- [ ] **Bước 4** - Port: `ArtifactPort` (`store`, `attach`, `issueDownloadToken`, `open`), `StoragePort`. `AvatarPort` do U01 khai báo (`C`), U03 cài ở Bước 7.
- [ ] **Bước 5** - `ContentInspector`: Tika trên 8 KB đầu, so allowlist của `purpose`; lỗi trả thông điệp chung (BR-U03-03, P3).
- [ ] **Bước 6** - `UploadService`: `Semaphore` 5 permit (hết → `503`), kiểm quyền, kiểm nội dung, SHA-256, đẩy qua `StoragePort`, INSERT, dọn bù trừ khi lỗi (retry 3 lần, vẫn lỗi thì tạo job `DRIVE_CLEANUP`), `finally` xóa file tạm (BR-U03-01…07, 41, P1).
- [ ] **Bước 7** - `ArtifactService`: `attach` (một lần, đúng người tải lên), `open` (chỉ `ACTIVE`), `validateAvatar` (cài `AvatarPort` của U01, bỏ `AvatarUnavailableAdapter` của U01); không có hàm xóa (BR-U03-30…32, F4, F5).
- [ ] **Bước 8** - `DownloadTokenService`: token 32 byte base64url, Redis `file:download-token:{sha256}` TTL 5 phút gắn `accountId`; từ chối `BLOCKED`; kiểm chủ token khi dùng, sai thì `404` và audit (BR-U03-20, 21, 25, 40, P4).
- [ ] **Bước 9** - `DriveJobHandler` trong worker cho `DRIVE_CLEANUP`; đăng ký với `JobHandlerRegistry` của U02 vào queue `jobs.drive` (U02 khai báo).
- [ ] **Bước 10** - Audit các sự kiện của BR-U03-40; không log `providerFileId`, key, token.
- [ ] **Bước 11** - Unit test cho mọi `BR-U03-xx`, gồm file đổi đuôi, file vượt trần theo `purpose` (50 MB / 5 MB), upload thứ 6 bị `503`, token dùng sai người, dọn Drive khi INSERT lỗi.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u03-file-and-artifact/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu và lưu trữ

- [ ] **Bước 13** - Flyway `V20260925_1000__u03_artifacts.sql`: bảng `artifacts` theo `domain-entities.md`, index `(scope_type, scope_id)`, unique `provider_file_id`.
- [ ] **Bước 14** - `ArtifactRepository` (JPA).
- [ ] **Bước 15** - `GoogleDriveStorageAdapter`: đọc key base64 từ `.env`, thư mục theo `purpose` (tạo khi khởi động), timeout 5/60/120 s, retry 3 lần cho 429/5xx, nhận diện `cannotDownloadAbusiveFile` → `BLOCKED` (P5, P6, BR-U03-24).
- [ ] **Bước 16** - `LocalFolderStorageAdapter`; chọn adapter theo có key hay không, cảnh báo khi khởi động (P5).
- [ ] **Bước 17** - `DriveHealthIndicator`: `drives.get` cache 60 s, Drive lỗi không làm backend `DOWN` (P7).
- [ ] **Bước 18** - Integration test Testcontainers (PostgreSQL, Redis) với `LocalFolderStorageAdapter`: upload → attach → cấp token → tải; token hết hạn; INSERT lỗi thì dọn Drive hoặc tạo job `DRIVE_CLEANUP`; adapter Drive test bằng mock HTTP (lỗi 429, abuse).
- [ ] **Bước 19** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 20** - `/contracts/openapi/u03-files.yaml`: `POST /api/v1/files` (multipart `purpose`, `file`), `GET /api/v1/files/download/{token}`.
- [ ] **Bước 21** - `FileUploadController`, `DownloadController` (`StreamingResponseBody`, bộ đệm 64 KB, header `Content-Type`, `Content-Disposition` RFC 5987, `nosniff`, `Cache-Control: private, no-store`; SVG thêm CSP sandbox; `BLOCKED` → `410`) (BR-U03-10, 22, 23, P2).
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
| Gate G1 (hợp đồng U03) | 11, 18, 20 |

## 5. Ngoài phạm vi

- Kiểm quyền nghiệp vụ trước khi cấp token (thuộc unit sở hữu).
- Kiểm và rút gọn XML Draw.io (U09, trong bộ nhớ); file dẫn xuất; xóa file.
- Adapter `AuthorizationPort` thật (khi U01 được code).
