# U12 Group & Allocation — logical-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 16, 17. **Primary stories:** US-GRP-001, US-GRP-002. **Requirements:** FR-025.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u12-group-allocation` không quyết định chức năng hiện hành.

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

## Quy tắc tích hợp revision hiện hành

- Groups belong class, not assessment. Teacher assigned R3/R4 creates/allocates; Admin/manager without teaching assignment denied.
- Members ACTIVE enrolled, one effective group/class; exactly one leader in members. Random assignment only ungrouped students, preserve existing groups, preview balanced difference <=1 before save.
- Leader-change request PENDING/APPROVED/REJECTED/CANCELLED; max one pending/group; reason/proposed member validated. Only assigned teacher decides, requester may cancel pending.
- Changing/removing leader must select replacement atomically. Removal current membership revokes edit/submit next call but preserves revision authorship.
- Publication readiness for ASSIGNMENT submissionMode GROUP through GroupReadinessPort; không separate Group product type.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Balanced preview preserve grouped students; concurrent version conflict; exactly leader; nonteacher denied.
- Member removal denies U14/AI context next call while historical author retained; group readiness missing leader blocks publish.
