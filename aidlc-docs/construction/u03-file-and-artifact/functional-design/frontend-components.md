# U03 File, Job & Event - Frontend Components

**Bản tài liệu 2026-10-08**: không primary UC; primary stories: không primary story. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

U03 không có trang riêng; cung cấp component dùng chung cho các unit.

```
shared/files/
  FileUploader        (props: purpose, accept, maxBytes theo purpose, onUploaded)
  FileLink            (props: getDownloadUrl, fileName)
  useFileUpload       (hook: tiến trình, hủy, lỗi)
shared/status/
  StatusBadge
  usePollStatus       (hook poll trạng thái của dòng nghiệp vụ)
```

| Component | Hành vi |
|---|---|
| `FileUploader` | Chọn hoặc kéo thả file; kiểm sơ bộ đuôi và dung lượng trước khi gửi; hiện thanh tiến trình; nút Hủy dừng request; lỗi từ backend hiện nguyên thông điệp an toàn |
| `useFileUpload` | Gửi `POST /api/v1/files` multipart với `purpose`; trả `fileRef` khi thành công; hủy giữa chừng thì backend không giữ lại gì |
| `FileLink` | Khi bấm mới gọi API của unit sở hữu để lấy URL tải (token 5 phút), rồi mở URL; không lưu URL lâu |

Unit dùng: U05 (`MATERIAL`), U06/U09/U11/U14 (`DOCUMENT_IMAGE` qua trình soạn tài liệu).

## Trạng thái việc nền dùng chung

| Component | Props | Hành vi |
|---|---|---|
| `usePollStatus(url)` | `url` API của unit sở hữu, `intervalMs = 3000` | Poll; dừng khi trạng thái là trạng thái cuối do unit khai báo; dừng khi rời trang |
| `StatusBadge` | `status`, `labels` | Nhãn tiếng Việt do unit truyền vào (ví dụ Đang quét, Đã lập chỉ mục, Lỗi) |

Unit khác (U05, U13, U14...) dùng hai component này trên màn hình của mình.

Không có màn quản lý hay chạy lại việc nền (BR-U03-57).
