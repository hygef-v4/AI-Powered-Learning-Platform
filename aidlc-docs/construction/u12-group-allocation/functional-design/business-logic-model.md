# U12 Group & Allocation - Business Logic Model

## F1 - Quản lý nhóm của lớp
1. Giảng viên mở danh sách sinh viên của lớp (Class Detail, U04) → xem các nhóm của lớp (mọi `StudentGroup` cùng `classId`, rỗng nếu chưa có) cùng sinh viên chưa có nhóm.
2. Chọn cách: tạo/sửa tay (BR-U12-04) hoặc chia ngẫu nhiên (BR-U12-05).
3. Lưu nguyên khối, kiểm BR-U12-02, 03, 07, 08; audit.

## F2 - Chia ngẫu nhiên
1. Lấy sinh viên đang ghi danh chưa có nhóm trong lớp, xáo trộn.
2. Số nhóm mới = trần(số người / sĩ số tối đa); chia vòng tròn để chênh ≤ 1.
3. Đặt tên "Nhóm N" tiếp theo; chọn trưởng nhóm ngẫu nhiên; trả bản xem trước để giảng viên sửa rồi lưu.

## F3 - (Đã bỏ) Phân công phần của giảng viên
- Giảng viên chỉ chuẩn bị các mục chính trong khung bài nhóm (U09/U14). Trưởng nhóm thêm mục chi tiết và giao mục cho thành viên trong tài liệu nhóm (U14).

## F4 - Sẵn sàng phát hành (`GroupReadinessPort`)
- U08 gọi khi phát hành bài `GROUP`: kiểm BR-U12-21 trên nhóm của lớp, trả danh sách lỗi/cảnh báo (lớp chưa có nhóm, nhóm thiếu trưởng nhóm, sinh viên chưa có nhóm).

## F5 - Đổi nhóm khi lớp đã có bài nhóm đang mở
1. Thêm/bớt thành viên, đổi nhóm, tạo nhóm mới (BR-U12-22) trong một transaction.
2. Nhóm mới: gọi `GroupChangePort.onGroupCreated(groupId)` → U14 tạo tài liệu nhóm cho mọi publication bài nhóm đang mở của lớp. Thành viên rời nhóm: gọi `GroupChangePort.onMemberRemoved(groupId, studentId)` → U14 nhả khóa mục của người đó ở mọi tài liệu nhóm đang mở. Cả hai trong cùng transaction.
3. Audit; sau commit phát event `group.membership-changed` cho thông báo.

## F6 - Trưởng nhóm
1. Sinh viên gửi/hủy yêu cầu (BR-U12-10, 11).
2. Giảng viên duyệt/từ chối (ghi chú lý do) hoặc đổi trực tiếp (BR-U12-11, 12). Sau commit: duyệt hoặc đổi trực tiếp phát `group.leader-changed`; từ chối phát `group.leader-request-rejected` để U16 báo người gửi yêu cầu.

## F7 - Sinh viên xem nhóm
- Theo BR-U12-30.
