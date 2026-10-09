# U12 Group & Allocation - Logical Components

**Bản tài liệu 2026-10-09**: UC 16, 17 và luồng phụ nhóm (quản lý nhóm, duyệt yêu cầu đổi trưởng nhóm trong tab Students của UC 32; sẵn sàng phát hành bài nhóm của UC 45) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-001, US-GRP-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Browser (teacher R3/R4, student R5)                U08 / U14 / U16
   |                                                   |
   v                                                   v
+----------------------------------------------------------------------------+
| backend                                                                    |
| ClassGroupsController --> ClassGroupsSaver --> ClassGroupsValidator        |
|                       --> RandomSplitter                                   |
|                       --> GroupChangePort (U14, C), EventPublisherPort     |
| LeaderRequestController --> LeaderRequestService                           |
| MyGroupController --> MyGroupQueryService                                  |
| GroupReadinessService (GroupReadinessPort for U08)                         |
| MembershipQueryService (GroupMembershipPort for U14, U16)                  |
| ClassAccessPort (U04): class state, main teacher, ACTIVE enrollments       |
| Repository (student_groups, group_members, leader_change_requests)         |
+----------------------------------------------------------------------------+
```

**Text alternative**: Ở tab Students của Teacher Class Detail, giảng viên lớp lưu nhóm của lớp qua `ClassGroupsSaver` (kiểm bằng `ClassGroupsValidator`) và chia ngẫu nhiên bằng `RandomSplitter`; thay đổi được báo U14 qua `GroupChangePort` và phát event cho U16. Yêu cầu đổi trưởng nhóm (gửi, hủy, duyệt, từ chối) qua `LeaderRequestService`. Sinh viên xem Nhóm của tôi qua `MyGroupQueryService` (tài liệu nhóm lấy từ U14). U08 hỏi `GroupReadinessService` trước khi phát hành bài nhóm; U14, U16 tra thành viên và trưởng nhóm qua `MembershipQueryService`. Mọi quyền và ghi danh `ACTIVE` kiểm qua `ClassAccessPort` của U04.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `ClassGroupsSaver`, `ClassGroupsValidator` | F1, F5, F8; P1 |
| `RandomSplitter` | F2; P2 |
| `LeaderRequestService` | F6; P3 |
| `GroupReadinessService` | F4; P5 |
| `MyGroupQueryService` | F7; P6 |
| `MembershipQueryService` | F8, port cho U14, U16; P4 |

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-05 | Compliant | Kiểm đầu vào |
| SECURITY-08 | Compliant | R3/R4 kiểm giảng viên chính; sinh viên chỉ nhóm mình; Admin bị từ chối |
| SECURITY-15 | Compliant | P1 nguyên khối, P3 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
