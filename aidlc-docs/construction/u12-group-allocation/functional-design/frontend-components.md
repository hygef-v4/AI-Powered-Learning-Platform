# U12 Group & Allocation - Frontend Components

```
app/teaching/assignments/[id]/groups/       GroupSetPage (bài GROUP)
  GroupSetToolbar          Tạo nhóm, Chia ngẫu nhiên, Dùng lại nhóm của bài khác
  GroupCard                tên, thành viên, chọn trưởng nhóm, thêm/bớt
  UngroupedLearnersPanel   người học chưa có nhóm
  RandomSplitDialog        sĩ số tối đa, xem trước
  ReuseGroupsDialog        chọn bài nhóm khác trong lớp
  AllocationMatrix         hàng = nhóm, cột = phần, ô = chọn thành viên; Gán tự động
  ReadinessPanel           lỗi còn thiếu trước khi phát hành
  LeaderRequestsPanel      yêu cầu đổi trưởng nhóm: Duyệt / Từ chối
app/learn/assignments/[publicationId]/group  MyGroupPanel
  MyGroupCard              thành viên, trưởng nhóm, phần của từng người (phần của tôi nổi bật)
  LeaderChangeRequestDialog
```

| Component | Hành vi | API |
|---|---|---|
| `GroupSetPage` | Sửa trên bản nháp phía client, bấm Lưu gửi nguyên khối | `GET`, `PUT /api/v1/assignments/{id}/group-set` |
| `RandomSplitDialog` | | `POST /api/v1/assignments/{id}/group-set:random-split` (trả xem trước) |
| `ReuseGroupsDialog` | | `POST /api/v1/assignments/{id}/group-set:copy-from` |
| `AllocationMatrix` | Chuyển phần đã có bài nộp: xác nhận "bài cũ giữ nguyên, người mới nộp lại" | `PUT /api/v1/assignments/{id}/allocations` |
| `LeaderRequestsPanel` | | `POST /api/v1/leader-requests/{id}/approve`, `.../reject` |
| `LeaderChangeRequestDialog` | | `POST /api/v1/groups/{id}/leader-requests` |
