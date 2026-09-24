# U12 Group & Allocation - NFR Design Patterns

## P1 - Lưu nguyên khối theo trạng thái mong muốn
- Client gửi toàn bộ bộ nhóm (nhóm, thành viên, trưởng nhóm, phân công) + `version`.
- `GroupSetSaver`: khóa `GroupSet` theo `version` → `GroupSetValidator` (thuần) kiểm BR-U12-02, 03, 20-24 trên trạng thái mới so với trạng thái cũ → tính khác biệt: thêm/đóng (`removedAt`, `supersededAt`) thay vì xóa → ghi → `version + 1` → event và audit sau commit (NFR-U12-10, 12).

## P2 - Chia ngẫu nhiên thuần
- `RandomSplitter.split(ungrouped, existingGroups, maxSize, random)` thuần, nhận `Random` để test lặp lại được; runtime dùng `SecureRandom` (NFR-U12-13).

## P3 - Yêu cầu đổi trưởng nhóm
- Partial unique `(group_id) WHERE status = 'PENDING'`; duyệt/từ chối bằng UPDATE có điều kiện `status = 'PENDING'`.
- Đổi trực tiếp: một transaction đổi `leaderId` và `CANCELLED` yêu cầu đang chờ.

## P4 - Tra cứu nhanh cho U14
- Index `(group_id, part_id) WHERE superseded_at IS NULL`, `(assignee_id)`; `AllocationPort` chỉ đọc (NFR-U12-02).

## P5 - Sẵn sàng phát hành
- `GroupReadinessService` dùng lại `GroupSetValidator` với luật BR-U12-21; trả danh sách lỗi có mã (`GROUP_WITHOUT_LEADER`, `PART_UNASSIGNED`, `MEMBER_WITHOUT_PART`).
