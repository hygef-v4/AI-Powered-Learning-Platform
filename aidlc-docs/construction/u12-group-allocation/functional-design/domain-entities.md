# U12 Group & Allocation - Domain Entities

## 1. Phạm vi sở hữu

U12 sở hữu bộ nhóm của từng bài nhóm, thành viên, trưởng nhóm, yêu cầu đổi trưởng nhóm. U12 **không** sở hữu: bài nhóm (U08), tài liệu nhóm, mục việc và khóa mục (U14), điểm (U15). Không có phân công phần của giảng viên.

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

## 6. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `GroupReadinessPort` | U12 cài cho U08 (`C`) | Bộ nhóm đủ điều kiện phát hành |
| `GroupMembershipPort` | U12 cung cấp cho U14, U16 | `groupOf(learnerId, assignmentId)`, `members(groupId)`, `leaderOf(groupId)`, lịch sử thành viên |
| `AssignmentQueryPort` | U12 dùng U08 | Bài `GROUP`, trạng thái publication |
| `ClassAccessPort` | U12 dùng U04 | Người học đang ghi danh |
| `AuditPort`, `EventPublisherPort` | U12 dùng U02 | Audit; event `GROUP_MEMBERSHIP_CHANGED`, `GROUP_LEADER_CHANGED` |
