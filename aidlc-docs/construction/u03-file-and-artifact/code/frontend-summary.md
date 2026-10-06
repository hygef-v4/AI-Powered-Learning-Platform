# U03 - Frontend Summary

Code: `frontend/src/shared/`.

| Thành phần | Hành vi |
|---|---|
| `files/purposes.ts` | Đuôi tệp, trần và `accept` theo `purpose`; `precheckFile` trả lỗi tiếng Việt |
| `files/useFileUpload.ts` | XHR `POST /api/v1/files` gửi cookie, tiến trình, hủy; lỗi backend hiện `title` của problem-details |
| `files/FileUploader.tsx` | Chọn/kéo thả, kiểm sơ bộ, thanh tiến trình, nút Hủy; `data-testid`: `file-uploader-dropzone`, `-input`, `-select-button`, `-progress`, `-cancel-button` |
| `files/FileLink.tsx` | Chỉ gọi API của unit sở hữu để lấy URL khi bấm, mở tab mới; không lưu URL |
| `status/usePollStatus.ts` | Poll API của unit sở hữu mỗi 3 giây, dừng ở trạng thái cuối, khi lỗi hoặc khi rời trang |
| `status/StatusBadge.tsx` | Nhãn tiếng Việt do unit truyền vào; `data-testid="status-badge"` |

Unit dùng: U01 (`AvatarUploader` bọc `FileUploader` với `AVATAR`), U05 (`MATERIAL`, `usePollStatus` cho trạng thái quét), U06/U09/U11/U14 (`DOCUMENT_IMAGE`).

Khung dùng chung (thêm khi chạy thử stack): `src/proxy.ts` + `src/lib/security/csp.ts` gắn CSP có nonce cho mỗi request trang; `app/layout.tsx` render theo request để nonce khác nhau mỗi lần.
