# U12 Group & Allocation - Business Rules

## 1. Nhóm của lớp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-01 | Nhóm thuộc lớp và được quản lý trong danh sách sinh viên của lớp; mọi bài `GROUP` của lớp dùng chung các nhóm này. Chỉ giảng viên của lớp tạo/sửa. | UC 9, FR-025, thay đổi 2026-10-01 |
| BR-U12-02 | Chỉ sinh viên đang ghi danh `ACTIVE` của lớp được vào nhóm; mỗi sinh viên tối đa một nhóm đang hiệu lực trong lớp. | US-GRP-001 S2 |
| BR-U12-03 | Mỗi nhóm ≥ 1 thành viên và đúng một trưởng nhóm là thành viên. | FR-025 |
| BR-U12-04 | Tạo tay: tạo nhóm, thêm/bớt thành viên, chọn trưởng nhóm. | Câu 3 |
| BR-U12-05 | Chia ngẫu nhiên: giảng viên nhập sĩ số tối đa (2-20); chỉ chia sinh viên chưa có nhóm trong lớp; giữ nguyên nhóm đã có; nhóm mới cân bằng (chênh nhau ≤ 1); trưởng nhóm chọn ngẫu nhiên trong nhóm. | Câu 3, demo_do_an |
| BR-U12-06 | (Đã bỏ 2026-10-01) Không có chức năng dùng lại nhóm của bài khác vì nhóm đã thuộc lớp. | Thay đổi 2026-10-01 |
| BR-U12-07 | Lưu nhóm của lớp nguyên khối; cấu hình mâu thuẫn → từ chối toàn bộ, báo từng lỗi. | US-GRP-001 S2 |
| BR-U12-08 | Không xóa nhóm đã có tài liệu nhóm hoặc bản nộp; chỉ được chuyển thành viên. Nhóm chưa dùng ở bài nào được xóa. | Thiết kế |

## 2. Trưởng nhóm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-10 | Thành viên gửi yêu cầu đổi trưởng nhóm kèm lý do, có thể đề xuất người thay (phải là thành viên); mỗi nhóm tối đa một yêu cầu `PENDING`. | US-GRP-002, Câu 4 |
| BR-U12-11 | Chỉ giảng viên duyệt/từ chối; duyệt thì chọn trưởng nhóm mới (mặc định người được đề xuất). Người yêu cầu hủy được khi còn `PENDING`. | FR-025 |
| BR-U12-12 | Giảng viên đổi trưởng nhóm trực tiếp; yêu cầu `PENDING` của nhóm tự `CANCELLED`. | Câu 4 |
| BR-U12-13 | Đổi trưởng nhóm chuyển quyền quản lý mục chi tiết, giao mục và nộp bài nhóm (U14) sang trưởng nhóm mới; không đổi mục ai đang giữ. | US-GRP-002 S1 |
| BR-U12-14 | Trưởng nhóm rời nhóm → giảng viên phải chọn trưởng nhóm mới trong cùng thao tác. | BR-U12-03 |

## 3. Mục việc và sẵn sàng

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-20 | Giảng viên **không** giao mục cho từng sinh viên; giảng viên chuẩn bị mục chính, trưởng nhóm thêm mục chi tiết và giao mục cho thành viên trong tài liệu nhóm (U14). | UC 16, UC 28 |
| BR-U12-21 | Bài `GROUP` sẵn sàng phát hành khi lớp có ít nhất một nhóm, mọi nhóm hợp lệ (BR-U12-03) và mọi sinh viên đang ghi danh đã có nhóm (cảnh báo nếu còn người chưa có nhóm, giảng viên xác nhận vẫn phát hành). | FR-025 |
| BR-U12-22 | Đổi thành viên khi lớp có bài nhóm đang mở được phép; mục đang do người bị bỏ giữ tự nhả khóa (U14), nội dung đã viết giữ nguyên với tên tác giả. Bản nộp đã có không đổi. | U12 Câu 7, U14 |

## 4. Sinh viên và audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-30 | Sinh viên thấy nhóm của mình trong lớp: tên nhóm, thành viên, trưởng nhóm và tài liệu nhóm của từng bài nhóm. Không thấy nhóm khác. | FR-025 |
| BR-U12-31 | Thành viên rời nhóm, nhóm mới khi lớp có bài nhóm đang mở: báo U14 qua `GroupChangePort` trong cùng transaction (nhả khóa mục, tạo tài liệu nhóm). Event `group.membership-changed`, `group.leader-changed`, `group.leader-request-rejected` sau commit chỉ cho U16 báo trong app; quyền của trưởng nhóm U14 đọc trực tiếp qua `GroupMembershipPort`. | FR-011 |
| BR-U12-32 | Audit: tạo/sửa/xóa nhóm, chia ngẫu nhiên, đổi trưởng nhóm, duyệt/từ chối yêu cầu. | FR-014 |
