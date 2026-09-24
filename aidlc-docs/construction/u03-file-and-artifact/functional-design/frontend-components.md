# U03 File & Artifact - Frontend Components

U03 không có trang riêng; cung cấp component dùng chung cho các unit.

```
shared/files/
  FileUploader        (props: purpose, accept, maxBytes = 50 MB, onUploaded)
  FileLink            (props: getDownloadUrl, fileName)
  useFileUpload       (hook: tiến trình, hủy, lỗi)
```

| Component | Hành vi |
|---|---|
| `FileUploader` | Chọn hoặc kéo thả file; kiểm sơ bộ đuôi và dung lượng trước khi gửi; hiện thanh tiến trình; nút Hủy dừng request; lỗi từ backend hiện nguyên thông điệp an toàn |
| `useFileUpload` | Gửi `POST /api/v1/files` multipart với `purpose`; trả `artifactId` khi thành công; hủy giữa chừng thì backend không giữ lại gì |
| `FileLink` | Khi bấm mới gọi API của unit sở hữu để lấy URL tải (token 5 phút), rồi mở URL; không lưu URL lâu |

Unit dùng: U01 (`AvatarUploader` bọc `FileUploader` với `purpose = AVATAR`), U05, U11, U14, U16.
