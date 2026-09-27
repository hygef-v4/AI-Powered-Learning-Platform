# U12 Group & Allocation - Logical Components

## 1. Sơ đồ

```
 Trình duyệt (GV, người học)                        U08 / U14 / U16
   |                                                   |
   v                                                   v
 +------------------------------ backend -------------------------------------+
 | GroupSetController --> GroupSetSaver --> GroupSetValidator                 |
 |                    --> RandomSplitter, GroupSetCopier                      |
 | LeaderRequestController --> LeaderRequestService                           |
 | GroupReadinessService (GroupReadinessPort cho U08)                         |
 | MembershipQueryService (GroupMembershipPort cho U14, U16)                  |
 | Repository (student_groups, group_members,                                 |
 |             leader_change_requests)                                        |
 +----------------------------------------------------------------------------+
```

**Text alternative**: Giảng viên lưu bộ nhóm qua `GroupSetSaver` (kiểm bằng `GroupSetValidator`), chia ngẫu nhiên bằng `RandomSplitter`, dùng lại nhóm bằng `GroupSetCopier`. Yêu cầu đổi trưởng nhóm qua `LeaderRequestService`. U08 hỏi `GroupReadinessService` trước khi phát hành; U14, U16 tra thành viên và trưởng nhóm qua `MembershipQueryService`.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `GroupSetSaver`, `GroupSetValidator` | F1, F3, F5; P1 |
| `RandomSplitter` | F2; P2 |
| `GroupSetCopier` | BR-U12-06 |
| `LeaderRequestService` | F6; P3 |
| `GroupReadinessService` | F4; P5 |
| `MembershipQueryService` | F7; P4 |

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-05 | Compliant | Kiểm đầu vào |
| SECURITY-08 | Compliant | Quyền giảng viên lớp; người học chỉ nhóm mình |
| SECURITY-15 | Compliant | P1 nguyên khối, P3 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
