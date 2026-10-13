# U03 - API Summary

Hợp đồng: `contracts/openapi/files.yaml`.

| Endpoint | Controller | Hành vi |
|---|---|---|
| `POST /api/v1/files` (multipart `purpose`, `file`) | `FileUploadController` | Cần đăng nhập. `201` trả `fileRef`, `expiresAt`, `mediaType`, `byteSize`, `originalFileName`; không có `fileId`. Lỗi: `400` (`purpose` sai), `403` `FILE_UPLOAD_FORBIDDEN`, `413` `FILE_TOO_LARGE`, `415` `FILE_TYPE_NOT_ALLOWED`, `503` `UPLOAD_BUSY`/`STORAGE_UNAVAILABLE` |
| `GET /api/v1/files/download/{token}` | `DownloadController` | Cần đăng nhập đúng tài khoản được cấp token. Stream bộ đệm 64 KB; `Content-Type` từ metadata; `Content-Disposition` RFC 5987 (`inline` cho ảnh/PDF, còn lại `attachment`); `X-Content-Type-Options: nosniff`; `Cache-Control: no-store, private`; SVG thêm `Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; sandbox`. Lỗi: `404` (token sai/hết hạn/sai người), `410` `FILE_UNAVAILABLE` (abuse), `503` |

Thay đổi dùng chung trong `shared/`:
- `SecurityConfig`: mọi `/api/v1/**` cần đăng nhập (trừ đăng nhập/OTP/webhook PayOS); 401/403 trả problem-details.
- `CurrentActor`: lấy `ActorRef` từ SecurityContext (U01 đặt principal).
- `GlobalExceptionHandler`: vượt `max-request-size` của multipart → `413 FILE_TOO_LARGE`.
- Health: trạng thái `DEGRADED` ánh xạ HTTP 200.
