# U12 Group & Allocation - NFR Design Patterns

## P1 - Lưu nguyên khối theo trạng thái mong muốn
- Client gửi toàn bộ nhóm của lớp (nhóm, thành viên, trưởng nhóm) + `version`.
- `ClassGroupsSaver`: khóa theo lớp bằng `pg_advisory_xact_lock(class_id)` và kiểm `version` của từng nhóm → `ClassGroupsValidator` (thuần) kiểm BR-U12-02, 03, 08, 21, 22 trên trạng thái mới so với trạng thái cũ → tính khác biệt: thêm nhóm/thành viên, xóa dòng `group_members` của người bị gỡ, xóa nhóm chưa dùng (`GroupChangePort.hasGroupWork` = false), đổi cờ `is_leader` → ghi trong cùng transaction, gọi `GroupChangePort.onGroupCreated/onMemberRemoved` khi lớp có bài nhóm đang mở, audit; sau commit phát `group.membership-changed`.

## P2 - Chia ngẫu nhiên thuần
- `RandomSplitter.split(ungrouped, existingGroups, maxSize, random)` thuần, nhận `Random` để test lặp lại được; runtime dùng `SecureRandom` (NFR-U12-13).

## P3 - Yêu cầu đổi trưởng nhóm
- Partial unique `(group_id) WHERE status = 'PENDING'`; duyệt/từ chối bằng UPDATE có điều kiện `status = 'PENDING'`.
- Đổi trực tiếp: một transaction chuyển cờ `is_leader` và `CANCELLED` yêu cầu đang chờ.

## P4 - Tra cứu nhanh thành viên cho U14
- Index `group_members (account_id)`; `GroupMembershipPort` chỉ đọc (NFR-U12-02).

## P5 - Sẵn sàng phát hành
- `GroupReadinessService` dùng lại `ClassGroupsValidator` với luật BR-U12-21 trên nhóm của lớp; trả danh sách lỗi có mã (`CLASS_WITHOUT_GROUP`, `GROUP_WITHOUT_LEADER`; `STUDENT_WITHOUT_GROUP` là cảnh báo).
