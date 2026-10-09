# U12 Group & Allocation - NFR Design Patterns

**Bản tài liệu 2026-10-09**: UC 16, 17 và luồng phụ nhóm (quản lý nhóm, duyệt yêu cầu đổi trưởng nhóm trong tab Students của UC 32; sẵn sàng phát hành bài nhóm của UC 45) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-001, US-GRP-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

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
