# U12 Group & Allocation - Business Logic Model

## F1 - Tạo bộ nhóm
1. Giảng viên mở bài `GROUP` (DRAFT hoặc REVIEWED/LOCKED) → tạo `GroupSet` nếu chưa có.
2. Chọn cách: tạo tay (BR-U12-04), chia ngẫu nhiên (BR-U12-05), dùng lại nhóm của bài khác (BR-U12-06).
3. Lưu nguyên khối, kiểm BR-U12-02, 03, 07; audit.

## F2 - Chia ngẫu nhiên
1. Lấy người học đang ghi danh chưa có nhóm trong bộ, xáo trộn.
2. Số nhóm mới = trần(số người / sĩ số tối đa); chia vòng tròn để chênh ≤ 1.
3. Đặt tên "Nhóm N" tiếp theo; chọn trưởng nhóm ngẫu nhiên; trả bản xem trước để giảng viên sửa rồi lưu.

## F3 - Phân công phần
1. Bảng nhóm × phần; mỗi ô chọn một thành viên của nhóm (BR-U12-20).
2. "Gán tự động": chia các phần lần lượt cho thành viên theo vòng tròn (giảng viên sửa được).
3. Lưu; phát `GROUP_PART_ASSIGNED`; audit.

## F4 - Sẵn sàng phát hành (`GroupReadinessPort`)
- Kiểm BR-U12-21, trả danh sách lỗi (nhóm/phần/thành viên thiếu).

## F5 - Đổi sau khi phát hành
1. Chuyển phần (BR-U12-22), xóa/thêm thành viên (BR-U12-23, 24), trong một transaction; audit; event.

## F6 - Trưởng nhóm
1. Người học gửi/hủy yêu cầu (BR-U12-10, 11).
2. Giảng viên duyệt/từ chối (ghi chú lý do) hoặc đổi trực tiếp (BR-U12-11, 12); event `GROUP_LEADER_CHANGED`.

## F7 - Người học xem nhóm
- Theo BR-U12-30.
