# U03 File & Artifact - Domain Entities

## 1. Artifact

| Thuộc tính | Ràng buộc |
|---|---|
| `artifactId` | Định danh |
| `ownerAccountId` | Người tải lên; rỗng nếu hệ thống tạo (file dẫn xuất) |
| `purpose` | Xem bảng mục 2 |
| `scopeType`, `scopeId` | Đối tượng nghiệp vụ gắn file, do unit gọi truyền vào; có thể rỗng lúc tải lên |
| `storageProvider` | `GOOGLE_DRIVE` |
| `providerFileId`, `driveId` | Định danh trên Drive; không bao giờ trả cho frontend |
| `originalFileName` | Đã làm sạch ký tự điều khiển và đường dẫn |
| `mediaType` | Xác định từ nội dung file, không tin header trình duyệt |
| `byteSize` | ≤ 50 MB |
| `checksum` | SHA-256 |
| `sourceArtifactId` | File gốc nếu là file dẫn xuất |
| `status` | `ACTIVE`, `BLOCKED`, `DELETED` |
| `createdAt`, `deletedAt` | |

Byte file không đổi sau khi tạo. Chỉ `status`, `scopeType`/`scopeId` (một lần) và `deletedAt` được cập nhật.

### Trạng thái

| Từ | Sang | Khi |
|---|---|---|
| (mới) | `ACTIVE` | Upload đủ và lưu Drive thành công |
| `ACTIVE` | `BLOCKED` | Drive từ chối trả file vì bị gắn cờ abuse |
| `ACTIVE` | `DELETED` | File dẫn xuất bị xóa sau job AI |

## 2. Mục đích và allowlist

| `purpose` | Loại file | Chủ yếu dùng bởi | Dẫn xuất |
|---|---|---|---|
| `AVATAR` | JPG, PNG, WebP | U01 | Không |
| `MATERIAL` | PDF, DOCX, PPTX | U05 | Không |
| `YOUTUBE_TRANSCRIPT` | Văn bản (JSON/TXT) do hệ thống tạo | U05 | Có |
| `DRAWIO_FULL` | XML / `.drawio` | U09, U11 | Không |
| `DRAWIO_AI_COMPACT` | XML do hệ thống tạo | U13 | Có, xóa sau job AI |
| `SUBMISSION_FILE` | PDF, DOCX, JPG, PNG, WebP, ZIP | U11, U14 | Không |
| `GROUP_COMPOSITE` | PDF/DOCX do hệ thống tạo | U14 | Có |
| `REPORT_EXPORT` | CSV/XLSX do hệ thống tạo | U16 | Có |

Mọi mục đích giới hạn **50 MB**.

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
| `ArtifactPort.storeDerived(sourceArtifactId, purpose, bytes)` | Worker của U05, U13, U14, U16 |
| `ArtifactPort.attach(artifactId, scopeType, scopeId, actor)` | Unit sở hữu khi gắn file vào đối tượng |
| `ArtifactPort.issueDownloadToken(artifactId, accountId)` | Unit sở hữu, **sau khi** tự kiểm quyền |
| `ArtifactPort.open(artifactId)` | Worker đọc nội dung (kiểm AI, RAG...) |
| `ArtifactPort.deleteDerived(artifactId)` | U13 sau job AI |
| `AvatarPort.validateAvatar(artifactId, actor)` | U01 |

## 5. Port U03 dùng

| Port | Unit | Cạnh |
|---|---|---|
| `AuthorizationPort` | U01 | `H` - kiểm đăng nhập và role được phép upload mục đích đó |
| `AuditPort` | U02 | `H` |
