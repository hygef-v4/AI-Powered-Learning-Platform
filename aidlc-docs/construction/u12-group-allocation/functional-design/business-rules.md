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
| BR-U12-13 | Đổi trưởng nhóm chuyển quyền nộp bài nhóm (U14) sang trưởng nhóm mới; không đổi mục ai đang nhận. | US-GRP-002 S1 |
| BR-U12-14 | Trưởng nhóm rời nhóm → giảng viên phải chọn trưởng nhóm mới trong cùng thao tác. | BR-U12-03 |

## 3. Mục việc và sẵn sàng

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-20 | Giảng viên **không** phân công phần; thành viên tự nhận mục việc trong tài liệu nhóm (U14). | U14 Câu 5, 6 |
| BR-U12-21 | Sẵn sàng phát hành khi mọi nhóm hợp lệ (BR-U12-03) và mọi người học đang ghi danh của lớp đã có nhóm (cảnh báo nếu còn người chưa có nhóm, giảng viên xác nhận vẫn phát hành). | FR-025 |
| BR-U12-22 | Đổi thành viên sau khi phát hành được; mục đang do người bị bỏ nhận tự nhả khóa (U14), nội dung đã viết giữ nguyên với tên tác giả. | U12 Câu 7, U14 |

## 4. Người học và audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-30 | Người học thấy nhóm của mình trong mỗi bài nhóm: tên nhóm, thành viên, trưởng nhóm. Không thấy nhóm khác. | FR-025 |
| BR-U12-31 | Thành viên rời nhóm, nhóm mới sau khi bài mở: báo U14 qua `GroupChangePort` trong cùng transaction (nhả khóa mục, tạo tài liệu nhóm). Event `GROUP_MEMBERSHIP_CHANGED`, `GROUP_LEADER_CHANGED` sau commit chỉ cho U16 báo trong app; quyền nộp của trưởng nhóm U14 đọc trực tiếp qua `GroupMembershipPort`. | FR-011 |
| BR-U12-32 | Audit: tạo/sửa bộ nhóm, chia ngẫu nhiên, dùng lại nhóm, đổi trưởng nhóm, duyệt/từ chối yêu cầu. | FR-014 |
