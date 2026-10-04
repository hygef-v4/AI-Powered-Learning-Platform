# U12 Group & Allocation - Frontend Components

```
app/teaching/classes/[id]/students/          ClassStudentsGroupsPanel (panel nhúng vào màn Class Detail của U04 — screen flow ghi màn này cho U04; nằm trong danh sách sinh viên)
  GroupsToolbar            Tạo nhóm, Chia ngẫu nhiên
  GroupCard                tên, thành viên, chọn trưởng nhóm, thêm/bớt, xóa nhóm chưa dùng
  UngroupedStudentsPanel   sinh viên chưa có nhóm
  RandomSplitDialog        sĩ số tối đa, xem trước
  LeaderRequestsPanel      yêu cầu đổi trưởng nhóm: Duyệt / Từ chối
app/teaching/assignments/[id]                 GroupReadinessPanel (U08 nhúng khi phát hành bài nhóm)
app/learning/classes/[classId]/group             MyGroupPanel
  MyGroupCard              thành viên, trưởng nhóm; danh sách tài liệu nhóm theo bài (U14)
  LeaderChangeRequestDialog  popup Request Leader Change
```

| Component | Hành vi | API |
|---|---|---|
| `ClassStudentsGroupsPanel` | Sửa trên bản nháp phía client, bấm Lưu gửi nguyên khối | `GET`, `PUT /api/v1/classes/{id}/groups` |
| `RandomSplitDialog` | | `POST /api/v1/classes/{id}/groups:random-split` (trả xem trước) |
| `GroupReadinessPanel` | Lỗi chặn phát hành; cảnh báo sinh viên chưa có nhóm cần giảng viên xác nhận | `GET /api/v1/classes/{id}/groups/readiness` |
| `LeaderRequestsPanel` | Duyệt chọn trưởng nhóm mới (mặc định người được đề xuất); từ chối bắt buộc lý do; đổi trực tiếp trong `GroupCard` | `GET /api/v1/classes/{id}/leader-requests`, `POST /api/v1/leader-requests/{id}/approve`, `.../reject` |
| `LeaderChangeRequestDialog` | Lý do 10–1000 ký tự, chọn người đề xuất trong nhóm; hủy khi còn chờ | `POST /api/v1/groups/{id}/leader-requests`, `POST /api/v1/leader-requests/{id}/cancel` |
| `MyGroupCard` | Chỉ nhóm của mình | `GET /api/v1/classes/{id}/my-group` |
