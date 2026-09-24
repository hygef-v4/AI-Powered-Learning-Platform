# U03 File & Artifact - Business Logic Model

## 1. F1 - Upload

**Vào**: phiên đăng nhập, `purpose`, file multipart.

1. Kiểm quyền upload theo `purpose` (BR-U03-04).
2. Đọc stream, dừng nếu vượt 50 MB (BR-U03-02).
3. Xác định loại từ magic bytes, đối chiếu allowlist (BR-U03-03). Nếu `DRAWIO_FULL` thì parse an toàn (BR-U03-10, 11).
4. Tính SHA-256.
5. Tải lên Drive vào thư mục theo `purpose`, nhận `providerFileId`.
6. INSERT `artifacts` ở `ACTIVE`, commit.
7. Bước 5 hoặc 6 lỗi → xóa file Drive nếu đã tạo, trả lỗi (BR-U03-06).
8. Trả `{ artifactId, mediaType, byteSize, originalFileName }`.

## 2. F2 - Gắn vào đối tượng

1. Unit sở hữu gọi `attach(artifactId, scopeType, scopeId, actor)`.
2. Artifact phải `ACTIVE`, chưa gắn, người gọi là người tải lên (BR-U03-30).
3. Lưu `scopeType`, `scopeId`.

## 3. F3 - Tải về

1. Frontend gọi API của **unit sở hữu** (ví dụ "tải học liệu"). Unit đó tự kiểm quyền nghiệp vụ.
2. Unit sở hữu gọi `issueDownloadToken(artifactId, accountId)`. Artifact phải `ACTIVE` (BR-U03-25). U03 sinh token, lưu băm Redis 5 phút.
3. Frontend nhận URL `/api/v1/files/download/{token}`.
4. Khi tải: tìm token, kiểm `accountId` khớp phiên (BR-U03-21), stream từ Drive với header an toàn (BR-U03-22, 23).
5. Drive báo abuse → `BLOCKED`, audit, trả lỗi (BR-U03-24).

Ảnh đại diện: U01 cấp token cho bất kỳ người đã đăng nhập (BR-U03-20).

## 4. F4 - Tạo file dẫn xuất

1. Worker của unit sở hữu gọi `storeDerived(sourceArtifactId, purpose, bytes)`.
2. File gốc phải `ACTIVE` (BR-U03-31).
3. Lưu như F1 bước 4-7, `ownerAccountId` rỗng, `sourceArtifactId` là file gốc.

## 5. F5 - Xóa XML rút gọn sau job AI

1. U13 gọi `deleteDerived(artifactId)` khi job AI kết thúc.
2. Chỉ chấp nhận artifact dẫn xuất (BR-U03-33).
3. Xóa file Drive, đặt `DELETED`, `deletedAt`. Drive lỗi → để job U02 retry.

## 6. F6 - Worker đọc file

1. Worker gọi `open(artifactId)`; artifact phải `ACTIVE`.
2. Trả stream từ Drive; abuse → `BLOCKED` như F3.

## 7. F7 - Kiểm ảnh đại diện (cho U01)

`validateAvatar(artifactId, actor)`: đúng khi `purpose = AVATAR`, `ACTIVE`, `ownerAccountId = actor`.
