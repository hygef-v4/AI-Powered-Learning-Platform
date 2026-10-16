# U13 Reference Materials — domain-entities

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Lấy phần học liệu của U05 cũ; bỏ module, Lesson, tóm tắt, embedding và RAG (phần mở rộng US-CNT-005). Thông báo lớp của U05 cũ thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Thiết kế độc lập công nghệ.

## 1. Tổng quan

| Entity | Loại | Lưu ở |
|---|---|---|
| `Material` | Aggregate root | `materials` |
| `MaterialVersion` | Phiên bản tệp hoặc link | `material_versions` |

U13 không sở hữu: tệp trên Drive (U03), Quiz (U10), Assignment (U07), thông báo lớp (U14).

## 2. `Material` và `MaterialVersion`

| Entity | Thuộc tính chính |
|---|---|
| `Material` | `id`, `scope` (`SUBJECT`, `CLASS`), `subjectId` (luôn có), `classId` (chỉ `CLASS`), `kind` (`FILE`, `YOUTUBE_LINK`), `title` (≤ 200), `description` (≤ 2.000), `orderNo`, `status` (`ACTIVE`, `ARCHIVED`), `currentVersionNo`, `createdBy`, `createdAt`, `updatedAt`, `version` |
| `MaterialVersion` | `materialId`, `versionNo` (khóa cùng `materialId`), `fileId`, `fileName`, `mimeType`, `byteSize` (với `FILE`), `videoUrl`, `videoId` (với `YOUTUBE_LINK`), `textStatus` (`PENDING`, `EXTRACTED`, `NO_TEXT`, `FAILED`; chỉ `FILE`), `extractedText` (≤ 1.000.000 ký tự), `createdBy`, `createdAt` |

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: Thêm tài liệu, phiên bản 1
    ACTIVE --> ACTIVE: Sửa thông tin, hoặc thay tệp, link thành phiên bản mới
    ACTIVE --> ARCHIVED: Xóa (lưu trữ)
```

**Text alternative**: Tài liệu tạo ra ở `ACTIVE` với phiên bản 1. Sửa tiêu đề, mô tả, thứ tự không đổi phiên bản; thay tệp hoặc link tạo phiên bản mới và giữ phiên bản cũ. Xóa chuyển `ARCHIVED`: ẩn với người học, tham chiếu cũ vẫn đọc được.

## 3. Contract

### Interface U13 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IReferenceMaterial` (contract của U06, U13 cài) | `listSelectable(actor, scopeRef)`, `extractText(actor, materialIds, maxChars)` | U07, U10 | Người soạn chọn tài liệu và lấy chữ đưa vào prompt AI |
| `IMcpToolProvider` (contract của U06, U13 cài) | tool `read_authorized_material` | U06 | Agent đọc tài liệu đúng quyền |
| `StudentClassSection`, `TeacherClassTab`, `DashboardNavEntry` (contract của U05, U13 cài) | - | U05 | Phần Học liệu trên Student Class Detail; tab Materials; lối vào Material List |
| Slot `AssignmentMaterialSection` (của U08), `QuizMaterialSection` (của U11), U13 cài bằng `MaterialLinkList` | - | U08, U11 | Hiện tài liệu gắn kèm Assignment, Quiz đúng quyền |
| `IJobHandler` cho `MATERIAL_TEXT_EXTRACT`, `IPendingSweeper` (contract của U03, U13 cài) | - | U03 | Trích chữ nền, gửi lại message bị mất |

### Contract U13 khai báo (unit khác cài)

| Contract | Unit cài | Dùng để |
|---|---|---|
| Slot frontend `LearningMaterialSection` | U11 | Danh sách Quiz gắn tài liệu kèm nút Làm Quiz trên Learning Material |

### Interface U13 dùng

| Interface | Unit | Dùng để |
|---|---|---|
| `IAuthorization` | U01 | Kiểm R2, R3/R4, R5 |
| `IAudit` | U02 | Audit |
| `IArtifact`, `IJob` | U03 | Lưu, gắn, mở, cấp token tải tệp `MATERIAL`; job trích chữ |
| `ISubjectQuery` | U04 | Môn còn `ACTIVE`, Chủ nhiệm môn |
| `IClassAccess` | U05 | Lớp, giảng viên, ghi danh, môn của lớp |

## 4. API

| API | UC |
|---|---|
| `GET`, `POST /api/v1/subjects/{subjectId}/materials` | 58, 59 |
| `GET`, `POST /api/v1/classes/{classId}/materials` | 60, 61 |
| `GET`, `PATCH /api/v1/materials/{id}`; `POST .../versions`; `POST .../archive` | 58–61 |
| `GET /api/v1/me/classes/{classId}/materials` (lọc được theo `ids`); `GET /api/v1/materials/{id}/view`; `POST .../download-token` | 62 |
