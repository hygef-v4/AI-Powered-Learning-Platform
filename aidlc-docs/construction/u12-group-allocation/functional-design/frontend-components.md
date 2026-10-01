# U12 Group & Allocation - Frontend Components

```
app/teaching/classes/[id]/students/          ClassStudentsGroupsPanel (trong Class Detail của U04, danh sách sinh viên)
  GroupsToolbar            Tạo nhóm, Chia ngẫu nhiên
  GroupCard                tên, thành viên, chọn trưởng nhóm, thêm/bớt, xóa nhóm chưa dùng
  UngroupedStudentsPanel   sinh viên chưa có nhóm
  RandomSplitDialog        sĩ số tối đa, xem trước
  LeaderRequestsPanel      yêu cầu đổi trưởng nhóm: Duyệt / Từ chối
app/teaching/assignments/[id]                 GroupReadinessPanel (U08 nhúng khi phát hành bài GROUP)
app/learn/classes/[classId]/group             MyGroupPanel
  MyGroupCard              thành viên, trưởng nhóm; danh sách tài liệu nhóm theo bài (U14)
  LeaderChangeRequestDialog
```

| Component | Hành vi | API |
|---|---|---|
| `ClassStudentsGroupsPanel` | Sửa trên bản nháp phía client, bấm Lưu gửi nguyên khối | `GET`, `PUT /api/v1/classes/{id}/groups` |
| `RandomSplitDialog` | | `POST /api/v1/classes/{id}/groups:random-split` (trả xem trước) |
| `GroupReadinessPanel` | Lỗi/cảnh báo trước khi phát hành | `GET /api/v1/classes/{id}/groups/readiness` |
| `LeaderRequestsPanel` | | `POST /api/v1/leader-requests/{id}/approve`, `.../reject` |
| `LeaderChangeRequestDialog` | | `POST /api/v1/groups/{id}/leader-requests` |
