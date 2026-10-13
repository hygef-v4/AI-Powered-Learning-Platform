# U12 Group & Allocation — nfr-design-patterns

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 16, 17. **Primary stories:** US-GRP-001, US-GRP-002. **Requirements:** FR-025.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u12-group-allocation` không quyết định chức năng hiện hành.

## P1 - Lưu nguyên khối theo trạng thái mong muốn
- Client gửi toàn bộ nhóm của lớp (nhóm, thành viên, trưởng nhóm) + `version`.
- `ClassGroupsSaver`: kiểm R3/R4 và lớp `DRAFT`/`OPEN` (BR-U12-09); khóa theo lớp bằng `pg_advisory_xact_lock(class_id)` và kiểm `version` của từng nhóm → `ClassGroupsValidator` (thuần) kiểm BR-U12-02, 03, 08, 14, 15, 22 trên trạng thái mới so với trạng thái cũ → tính khác biệt: thêm nhóm/thành viên, xóa dòng `group_members` của người bị gỡ hoặc đã rời lớp, xóa nhóm chưa dùng (`GroupChangePort.hasGroupWork` = false), đổi cờ `is_leader` (đổi trực tiếp thì `CANCELLED` yêu cầu đang chờ), `CANCELLED` yêu cầu `PENDING` của người đã rời lớp → ghi trong cùng transaction, gọi `GroupChangePort.onGroupCreated/onMemberRemoved`, audit; sau commit phát `group.membership-changed`, `group.leader-changed` khi đổi trưởng nhóm.

## P2 - Chia ngẫu nhiên thuần
- `RandomSplitter.split(ungrouped, existingGroups, maxSize, random)` thuần, nhận `Random` để test lặp lại được; runtime dùng `SecureRandom` (NFR-U12-13).

## P3 - Yêu cầu đổi trưởng nhóm
- Partial unique `(group_id) WHERE status = 'PENDING'`; gửi trùng → `409`. Duyệt/từ chối/hủy bằng UPDATE có điều kiện `status = 'PENDING'`.
- Duyệt: kiểm trưởng nhóm mới là thành viên hiệu lực (người được đề cử hoặc `newLeaderId`; không đề cử mà thiếu `newLeaderId` → `422`), chuyển cờ `is_leader` trong cùng transaction.
- Đổi trực tiếp: một transaction chuyển cờ `is_leader` và `CANCELLED` yêu cầu đang chờ.

## P4 - Tra cứu nhanh thành viên cho U14
- Index `group_members (account_id)`; `GroupMembershipPort` chỉ đọc, lọc thành viên hiệu lực bằng `ClassAccessPort.isActiveStudent`/`listActiveStudents` (NFR-U12-02).

## P5 - Sẵn sàng phát hành
- `GroupReadinessService` dùng lại `ClassGroupsValidator` với luật BR-U12-21 trên nhóm của lớp; trả danh sách có mã: lỗi `CLASS_WITHOUT_GROUP`, `GROUP_WITHOUT_LEADER`, `GROUP_WITHOUT_MEMBER`; cảnh báo `STUDENT_WITHOUT_GROUP`. U08 chặn khi có lỗi và chỉ phát hành với cảnh báo khi giảng viên đã xác nhận.

## P6 - Nhóm của tôi
- `MyGroupQueryService` đọc nhóm, thành viên hiệu lực, yêu cầu gần nhất của người gọi (index `(requester_id, created_at)`); tài liệu nhóm do frontend lấy từ API của U14; chỉ trả nhóm của chính người gọi (NFR-U12-03, 20).

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
