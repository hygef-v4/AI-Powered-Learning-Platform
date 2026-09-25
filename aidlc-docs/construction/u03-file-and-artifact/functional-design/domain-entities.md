# U03 File & Artifact - Domain Entities

## 1. Artifact

| Thuộc tính | Ràng buộc |
|---|---|
| `artifactId` | Định danh |
| `ownerAccountId` | Người tải lên |
| `purpose` | Xem bảng mục 2 |
| `scopeType`, `scopeId` | Đối tượng nghiệp vụ gắn file, do unit gọi truyền vào; có thể rỗng lúc tải lên |
| `storageProvider` | `GOOGLE_DRIVE` |
| `providerFileId`, `driveId` | Định danh trên Drive; không bao giờ trả cho frontend |
| `originalFileName` | Đã làm sạch ký tự điều khiển và đường dẫn |
| `mediaType` | Xác định từ nội dung file, không tin header trình duyệt |
| `byteSize` | ≤ trần của `purpose` (mục 2) |
| `checksum` | SHA-256 |
| `status` | `ACTIVE`, `BLOCKED` |
| `createdAt` | |

Byte file không đổi sau khi tạo. Chỉ `status` và `scopeType`/`scopeId` (một lần) được cập nhật. Không có file dẫn xuất và không xóa file.

### Trạng thái

| Từ | Sang | Khi |
|---|---|---|
| (mới) | `ACTIVE` | Upload đủ và lưu Drive thành công |
| `ACTIVE` | `BLOCKED` | Drive từ chối trả file vì bị gắn cờ abuse |

## 2. Mục đích và allowlist

| `purpose` | Loại file | Trần | Dùng bởi |
|---|---|---|---|
| `AVATAR` | JPG, PNG, WebP | 50 MB | U01 |
| `MATERIAL` | PDF, DOCX, PPTX | 50 MB | U05 |
| `DOCUMENT_IMAGE` | PNG, JPEG, GIF, SVG | 5 MB | U06, U09, U11, U14 (ảnh trong khung đề, bài làm, tài liệu nhóm) |

Không có loại file cho Draw.io: XML sơ đồ nằm trong block `DIAGRAM` của tài liệu (U09). Caption YouTube lưu ở U05; bài nhóm xuất DOCX tại chỗ (U09); export báo cáo là Phase 2.

## 3. DownloadToken

| Thuộc tính | Ràng buộc |
|---|---|
| `token` | Ngẫu nhiên 256 bit; Redis lưu băm |
| `artifactId`, `accountId` | Token chỉ dùng được bởi đúng người được cấp |
| `expiresAt` | 5 phút |
| `disposition` | `inline` hoặc `attachment` |

Token dùng lại nhiều lần trong 5 phút (để ảnh và PDF viewer tải từng phần).

## 4. Port U03 cung cấp

| Port | Dùng bởi |
|---|---|
| `ArtifactPort.store(actor, purpose, file)` | API upload chung |
| `ArtifactPort.attach(artifactId, scopeType, scopeId, actor)` | Unit sở hữu khi gắn file vào đối tượng |
| `ArtifactPort.issueDownloadToken(artifactId, accountId)` | Unit sở hữu, **sau khi** tự kiểm quyền |
| `ArtifactPort.open(artifactId)` | Worker U05 đọc học liệu để trích chữ cho RAG |
| `AvatarPort.validateAvatar(artifactId, actor)` | U01 |

## 5. Port U03 dùng

| Port | Unit | Cạnh |
|---|---|---|
| `AuthorizationPort` | U01 | `H` - kiểm đăng nhập và role được phép upload mục đích đó |
| `AuditPort` | U02 | `H` |
