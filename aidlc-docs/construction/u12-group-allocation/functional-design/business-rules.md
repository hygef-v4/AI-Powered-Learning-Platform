# U12 Group & Allocation - Business Rules

## 1. Bộ nhóm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-01 | Mỗi bài `GROUP` có bộ nhóm riêng; chỉ giảng viên của lớp tạo/sửa. | Câu 1, FR-025 |
| BR-U12-02 | Chỉ người học đang ghi danh `ACTIVE` của lớp được vào nhóm; mỗi người tối đa một nhóm trong một bộ. | US-GRP-001 S2 |
| BR-U12-03 | Mỗi nhóm ≥ 1 thành viên và đúng một trưởng nhóm là thành viên. | FR-025 |
| BR-U12-04 | Tạo tay: tạo nhóm, thêm/bớt thành viên, chọn trưởng nhóm. | Câu 3 |
| BR-U12-05 | Chia ngẫu nhiên: giảng viên nhập sĩ số tối đa (2-20); chỉ chia người học chưa có nhóm trong bộ; giữ nguyên nhóm đã có; nhóm mới cân bằng (chênh nhau ≤ 1); trưởng nhóm chọn ngẫu nhiên trong nhóm. | Câu 3, demo_do_an |
| BR-U12-06 | Dùng lại nhóm của bài nhóm khác cùng lớp: sao chép nhóm, thành viên (bỏ người không còn ghi danh), trưởng nhóm; hai bộ độc lập sau đó. | Câu 5 |
| BR-U12-07 | Lưu bộ nhóm nguyên khối; cấu hình mâu thuẫn → từ chối toàn bộ, báo từng lỗi. | US-GRP-001 S2 |

## 2. Trưởng nhóm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-10 | Thành viên gửi yêu cầu đổi trưởng nhóm kèm lý do, có thể đề xuất người thay (phải là thành viên); mỗi nhóm tối đa một yêu cầu `PENDING`. | US-GRP-002, Câu 4 |
| BR-U12-11 | Chỉ giảng viên duyệt/từ chối; duyệt thì chọn trưởng nhóm mới (mặc định người được đề xuất). Người yêu cầu hủy được khi còn `PENDING`. | FR-025 |
| BR-U12-12 | Giảng viên đổi trưởng nhóm trực tiếp; yêu cầu `PENDING` của nhóm tự `CANCELLED`. | Câu 4 |
| BR-U12-13 | Đổi trưởng nhóm không đổi quyền nộp phần của ai. | US-GRP-002 S1 |
| BR-U12-14 | Trưởng nhóm rời nhóm → giảng viên phải chọn trưởng nhóm mới trong cùng thao tác. | BR-U12-03 |

## 3. Phân công phần

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-20 | Giảng viên gán mỗi phần của mỗi nhóm cho đúng một thành viên; một người có thể nhiều phần. | Câu 2, 6, US-GRP-003 |
| BR-U12-21 | Sẵn sàng phát hành khi: mọi nhóm hợp lệ (BR-U12-03), mọi phần của mọi nhóm đã có người, mọi thành viên có ≥ 1 phần. | Câu 6 |
| BR-U12-22 | Chuyển phần cho người khác được khi phần chưa hết hạn và chưa chốt điểm: phân công cũ `supersededAt`; bài người cũ đã nộp giữ nguyên (chỉ đọc); người mới nộp mới; audit. | Câu 7, US-GRP-003 S2 |
| BR-U12-23 | Xóa thành viên còn phần đang hiệu lực → phải chuyển phần trước; bài đã nộp của họ giữ nguyên. | Câu 7 |
| BR-U12-24 | Thêm thành viên sau khi đã phát hành: phải gán ≥ 1 phần (chuyển từ người khác) trong cùng thao tác. | BR-U12-21 |

## 4. Người học và audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-30 | Người học thấy nhóm của mình trong mỗi bài nhóm: tên nhóm, thành viên, trưởng nhóm, ai làm phần nào. Không thấy nhóm khác. | FR-025 |
| BR-U12-31 | Event `GROUP_PART_ASSIGNED` (phân công mới/chuyển) và `GROUP_LEADER_CHANGED` cho U16 báo trong app. | FR-011 |
| BR-U12-32 | Audit: tạo/sửa bộ nhóm, chia ngẫu nhiên, dùng lại nhóm, đổi trưởng nhóm, duyệt/từ chối yêu cầu, gán/chuyển phần. | FR-014 |
