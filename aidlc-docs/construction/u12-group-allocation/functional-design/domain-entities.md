# U12 Group & Allocation - Domain Entities

## 1. Phạm vi sở hữu

U12 sở hữu bộ nhóm của từng bài nhóm, thành viên, trưởng nhóm, yêu cầu đổi trưởng nhóm và phân công phần việc. U12 **không** sở hữu: bài nhóm và các phần (thành phần của bài `GROUP`, U08), bài nộp phần và tài liệu tổng (U14), điểm (U15).

## 2. `GroupSet`

`id`, `assignmentId` (bài `GROUP`, một bộ nhóm mỗi bài), `copiedFromGroupSetId`, `createdBy`, `version`.

## 3. `StudentGroup` và `GroupMember`

`StudentGroup`: `id`, `groupSetId`, `name` (≤ 100, duy nhất trong bộ), `leaderId`.

`GroupMember`: `groupId`, `learnerId`, `joinedAt`, `removedAt`. Mỗi người học tối đa một nhóm đang hiệu lực trong một bộ nhóm.

## 4. `LeaderChangeRequest`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `groupId` | UUID | |
| `requesterId` | UUID | Thành viên nhóm |
| `proposedLeaderId` | UUID | Tùy chọn, phải là thành viên |
| `reason` | chuỗi 10-1000 | |
| `status` | enum | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| `decidedBy`, `decidedAt`, `decisionNote` | | |

## 5. `PartAllocation`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `groupId` | UUID | |
| `partId` | UUID | Thành phần của bài `GROUP` (U08) |
| `assigneeId` | UUID | Thành viên đang hiệu lực của nhóm |
| `assignedBy`, `assignedAt` | | |
| `supersededAt` | thời gian | Khi chuyển sang người khác (lịch sử giữ lại) |

Mỗi `(groupId, partId)` đúng một phân công đang hiệu lực.

## 6. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `GroupReadinessPort` | U12 cài cho U08 (`C`) | Bộ nhóm đủ điều kiện phát hành |
| `AllocationPort` | U12 cung cấp cho U14, U15, U16 | `assigneeOf(groupId, partId)`, `partsOf(learnerId, assignmentId)`, `groupOf(learnerId, assignmentId)`, lịch sử phân công |
| `AssignmentQueryPort` | U12 dùng U08 | Bài `GROUP`, các phần, trạng thái publication |
| `ClassAccessPort` | U12 dùng U04 | Người học đang ghi danh |
| `AuditPort`, `EventPublisherPort` | U12 dùng U02 | Audit; event `GROUP_PART_ASSIGNED`, `GROUP_LEADER_CHANGED` |
