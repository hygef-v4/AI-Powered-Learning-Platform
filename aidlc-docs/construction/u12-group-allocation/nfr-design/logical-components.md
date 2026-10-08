# U12 Group & Allocation - Logical Components

**Bản tài liệu 2026-10-08**: UC 15, 16; primary stories: US-GRP-001, US-GRP-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Trình duyệt (GV, sinh viên)                        U08 / U14 / U16
   |                                                   |
   v                                                   v
 +------------------------------ backend -------------------------------------+
 | ClassGroupsController --> ClassGroupsSaver --> ClassGroupsValidator        |
 |                       --> RandomSplitter                                   |
 |                       --> GroupChangePort (U14, C), EventPublisherPort     |
 | LeaderRequestController --> LeaderRequestService                           |
 | GroupReadinessService (GroupReadinessPort cho U08)                         |
 | MembershipQueryService (GroupMembershipPort cho U14, U16)                  |
 | Repository (student_groups, group_members,                                 |
 |             leader_change_requests)                                        |
 +----------------------------------------------------------------------------+
```

**Text alternative**: Trong danh sách sinh viên của lớp, giảng viên lưu nhóm của lớp qua `ClassGroupsSaver` (kiểm bằng `ClassGroupsValidator`) và chia ngẫu nhiên bằng `RandomSplitter`. Yêu cầu đổi trưởng nhóm qua `LeaderRequestService`. U08 hỏi `GroupReadinessService` trước khi phát hành; U14, U16 tra thành viên và trưởng nhóm qua `MembershipQueryService`.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `ClassGroupsSaver`, `ClassGroupsValidator` | F1, F5; P1 |
| `RandomSplitter` | F2; P2 |
| `LeaderRequestService` | F6; P3 |
| `GroupReadinessService` | F4; P5 |
| `MembershipQueryService` | F7; P4 |

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-05 | Compliant | Kiểm đầu vào |
| SECURITY-08 | Compliant | Quyền giảng viên lớp; sinh viên chỉ nhóm mình |
| SECURITY-15 | Compliant | P1 nguyên khối, P3 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
