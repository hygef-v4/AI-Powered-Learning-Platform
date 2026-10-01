# U10 Template & Copy - Domain Entities

Thiết kế độc lập công nghệ. Truy vết: `US-ASM-008`…`010`, `US-AIG-002` (nhận bản nháp AI); UC 21, UC 27. Tên thư mục lịch sử được giữ để các liên kết cũ vẫn hoạt động.

## 1. Tổng quan

| Entity | Loại | Lưu ở | Unit ghi |
|---|---|---|---|
| `TemplateRelease` | Aggregate root | `template_releases` | U10 |
| `AssignmentLineage` | Value object của `Assignment` (U08) | `assignments` | U10 |
| `AssignmentDiff` | Kết quả tính | Không lưu | U10 |

U10 sở hữu nghiệp vụ: phát hành template cấp môn, nguồn gốc của bài được copy và so sánh version. Bài, version, thành phần và publication thuộc U08 (template là bài `ownerType = SUBJECT_TEMPLATE`); U10 ghi lineage qua port của U08.

## 2. `TemplateRelease`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `templateAssignmentId` | UUID | Version template (bài U08 `SUBJECT_TEMPLATE`, `LOCKED` khi phát hành) |
| `subjectId` | UUID | |
| `stableKey`, `versionNo` | | Lấy từ U08 |
| `status` | enum | `RELEASED`, `WITHDRAWN` |
| `releasedBy`, `releasedAt`, `withdrawnAt` | | |

### Trạng thái

```mermaid
stateDiagram-v2
    [*] --> RELEASED: Chủ nhiệm môn phát hành version template
    RELEASED --> WITHDRAWN: Thu hồi
```

**Text alternative**: Mỗi version template được Chủ nhiệm môn phát hành ở `RELEASED`, giảng viên các lớp thuộc môn copy được. Thu hồi thì `WITHDRAWN`, không copy được nữa; bản đã copy không bị ảnh hưởng. Xoá template đã phát hành cũng chuyển mọi release về `WITHDRAWN` và lưu trữ các version (BR-U10-07).

## 3. `AssignmentLineage`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `sourceAssignmentId` | UUID | Version nguồn |
| `kind` | enum | `TEMPLATE_COPY`, `CLASS_COPY`, `CLONE`, `NEW_VERSION` |
| `sourceClassId` | UUID | Lớp nguồn; lớp đích là lớp của chính bài |
| `actorId`, `createdAt` | | |

Ghi một lần khi tạo bài bằng copy/nhân bản/version mới; không đồng bộ hai chiều.

## 4. `AssignmentDiff`

`instructions` (diff theo dòng), `components` (thêm/bớt/đổi thứ tự/đổi điểm/đổi nội dung), `typeConfig` (trường thay đổi), `totalPoints`. Tính khi xem, không lưu.

## 5. Contract

### Port U10 cung cấp

| Port | Dùng bởi | Mô tả |
|---|---|---|

### Port U10 dùng

| Port | Unit | Mô tả |
|---|---|---|
| `AssignmentQueryPort`, `AssignmentService`, `AssignmentExtensionPort` | U08 | Đọc version, tạo bài nháp và ghi lineage |
| `TypeConfigPort` | U09 | Sao chép cấu hình/khung |
| `BankCopyPort` | U06 | Sao chép câu/rubric cấp lớp sang lớp đích |
| `ClassAccessPort` | U04 | Phạm vi lớp/môn |
| `AiDraftPort` | U13 | Nhận câu hỏi AI đề xuất vào template |
