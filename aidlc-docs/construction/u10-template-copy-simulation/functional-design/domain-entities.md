# U10 Template, Copy & Simulation - Domain Entities

## 1. Phạm vi sở hữu

U10 sở hữu phát hành template cấp môn, dòng nguồn gốc (lineage) của bài được copy, so sánh version, và chính sách thi thử. Bài, version, thành phần và publication vẫn thuộc U08 (template là bài `ownerType = SUBJECT_TEMPLATE`). U10 không ALTER bảng của U08.

## 2. `TemplateRelease`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `templateAssignmentId` | UUID | Version template (bài U08 `SUBJECT_TEMPLATE`, `LOCKED` khi phát hành) |
| `subjectId` | UUID | |
| `stableKey`, `versionNo` | | Lấy từ U08 |
| `status` | enum | `RELEASED`, `WITHDRAWN` |
| `releasedBy`, `releasedAt`, `withdrawnAt` | | |

## 3. `AssignmentLineage` (bảng `assignment_lineage`)

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `targetAssignmentId` | UUID | Khóa |
| `sourceAssignmentId` | UUID | Version nguồn |
| `kind` | enum | `TEMPLATE_COPY`, `CLASS_COPY`, `CLONE`, `NEW_VERSION` |
| `sourceClassId`, `targetClassId` | UUID | |
| `actorId`, `createdAt` | | |

## 4. `SimulationPolicy` (bảng `simulation_policies`)

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `publicationId` | UUID | Khóa; publication `deliveryMode = SIMULATION` |
| `maxAttempts` | số hoặc rỗng | Rỗng = không giới hạn |
| `resultPolicy` | enum | `HIGHEST`, `LATEST`, `AVERAGE` |
| `answerRelease` | enum | `AFTER_ATTEMPT`, `AFTER_CLOSE`, `NEVER` |
| `countsTowardGrade` | bool | |
| `lockedAt` | thời gian | Đặt khi lượt đầu tiên bắt đầu |

## 5. `AssignmentDiff` (tính khi xem, không lưu)

`instructions` (diff theo dòng), `components` (thêm/bớt/đổi thứ tự/đổi điểm/đổi nội dung), `typeConfig` (trường thay đổi), `totalPoints`.

## 6. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `SimulationPolicyPort` | U10 cung cấp cho U11, U15 | Chính sách của publication thi thử; `lock(publicationId)` khi lượt đầu bắt đầu |
| `AssignmentQueryPort`, `AssignmentService`, `PublicationService` | U10 dùng U08 | Đọc version, tạo bài nháp, tạo publication `SIMULATION` |
| `TypeConfigPort` | U10 dùng U09 | Sao chép cấu hình/khung |
| `BankCopyPort` | U10 dùng U06 | Sao chép câu/rubric cấp lớp sang lớp đích |
| `ClassAccessPort` | U10 dùng U04 | Phạm vi lớp/môn |
