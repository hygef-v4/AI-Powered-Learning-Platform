# U12 Group & Allocation - Business Rules

**Bản tài liệu 2026-10-09**: UC 16, 17 và luồng phụ nhóm (quản lý nhóm, duyệt yêu cầu đổi trưởng nhóm trong tab Students của UC 32; sẵn sàng phát hành bài nhóm của UC 45) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-001, US-GRP-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Nhóm của lớp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-01 | Nhóm thuộc lớp và được quản lý trong tab Students của Teacher Class Detail; mọi bài nhóm (`GROUP_ASSIGNMENT`, chỉ có ở lớp) của lớp dùng chung các nhóm này. Chỉ giảng viên lớp (R3/R4: Teacher hoặc Subject Manager là giảng viên chính) tạo/sửa. Admin và Chủ nhiệm môn không được giao dạy lớp không xem, không sửa nhóm. | UC 32, 45, FR-025; người dùng chốt 2026-10-09 |
| BR-U12-02 | Chỉ sinh viên đang ghi danh `ACTIVE` của lớp được vào nhóm; mỗi sinh viên tối đa một nhóm đang hiệu lực trong lớp. | US-GRP-001 S2 |
| BR-U12-03 | Mỗi nhóm ≥ 1 thành viên và đúng một trưởng nhóm là thành viên. | FR-025 |
| BR-U12-04 | Tạo tay: tạo nhóm, thêm/bớt thành viên, chọn trưởng nhóm. | US-GRP-001 S1 |
| BR-U12-05 | Chia ngẫu nhiên: giảng viên nhập sĩ số tối đa (2-20); chỉ chia sinh viên chưa có nhóm trong lớp; giữ nguyên nhóm đã có; nhóm mới cân bằng (chênh nhau ≤ 1); trưởng nhóm chọn ngẫu nhiên trong nhóm; kết quả là bản xem trước. | US-GRP-001 S3 |
| BR-U12-06 | (Đã bỏ 2026-10-01) Không có chức năng dùng lại nhóm của bài khác vì nhóm đã thuộc lớp. | Thay đổi 2026-10-01 |
| BR-U12-07 | Lưu nhóm của lớp nguyên khối; cấu hình mâu thuẫn → từ chối toàn bộ, báo từng lỗi. | US-GRP-001 S2 |
| BR-U12-08 | Không xóa nhóm đã có tài liệu nhóm hoặc bản nộp (hỏi U14 qua `GroupChangePort.hasGroupWork`); chỉ được chuyển thành viên. Nhóm chưa dùng ở bài nào được xóa. Gỡ thành viên là xóa dòng `group_members`, lịch sử nằm trong audit. | Thiết kế |
| BR-U12-09 | Sửa nhóm, gửi/duyệt yêu cầu đổi trưởng nhóm chỉ khi lớp `DRAFT` hoặc `OPEN`; lớp `ARCHIVED` chỉ xem. Sinh viên chỉ thao tác ở lớp `OPEN` (R5). | Thiết kế, BR-U04 vòng đời lớp |

## 2. Trưởng nhóm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-10 | Thành viên (kể cả trưởng nhóm) gửi yêu cầu đổi trưởng nhóm kèm lý do 10-1000 ký tự; **có thể** đề cử người thay, người được đề cử phải là thành viên khác trưởng nhóm hiện tại. Mỗi nhóm tối đa một yêu cầu `PENDING`. | UC 17, US-GRP-002 |
| BR-U12-11 | Chỉ giảng viên lớp duyệt/từ chối; duyệt thì chọn trưởng nhóm mới (mặc định người được đề cử; bắt buộc chọn khi yêu cầu không có đề cử); từ chối bắt buộc lý do. Người gửi hủy được khi còn `PENDING`. | FR-025, US-GRP-002 S1, S2 |
| BR-U12-12 | Giảng viên lớp đổi trưởng nhóm trực tiếp; yêu cầu `PENDING` của nhóm tự `CANCELLED`. | US-GRP-001 S1 |
| BR-U12-13 | Đổi trưởng nhóm chuyển quyền giao phần và nộp bài nhóm (U14) sang trưởng nhóm mới; không đổi phần ai đang giữ. | US-GRP-002 S1 |
| BR-U12-14 | Trưởng nhóm rời nhóm → giảng viên phải chọn trưởng nhóm mới trong cùng thao tác. | BR-U12-03 |
| BR-U12-15 | Sinh viên bị Chủ nhiệm môn gỡ khỏi lớp (UC 51) không còn là thành viên hiệu lực: `GroupMembershipPort`, Nhóm của tôi và readiness bỏ qua người có ghi danh khác `ACTIVE`; tab Students hiện họ với nhãn "Đã rời lớp"; lần lưu sau xóa dòng của họ và gọi `GroupChangePort.onMemberRemoved`. Nếu người đó là trưởng nhóm, nhóm bị coi là thiếu trưởng nhóm đến khi giảng viên chọn người mới. Yêu cầu `PENDING` do họ gửi tự `CANCELLED` khi lưu; yêu cầu đề cử họ phải chọn người khác khi duyệt. | UC 51; thiết kế (xem "Cần chốt" của plan) |

## 3. Phần việc và sẵn sàng

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-20 | Giảng viên **không** giao phần cho từng sinh viên; khung bài nhóm tự chia thành các phần theo heading nhỏ nhất (U09), trưởng nhóm giao phần cho thành viên trong tài liệu nhóm (U14). | UC 27, 45; người dùng chốt 2026-10-04 |
| BR-U12-21 | Bài nhóm sẵn sàng phát hành khi lớp có ít nhất một nhóm và mọi nhóm hợp lệ (BR-U12-03, tính theo thành viên hiệu lực BR-U12-15). Lỗi chặn: `CLASS_WITHOUT_GROUP`, `GROUP_WITHOUT_LEADER`, `GROUP_WITHOUT_MEMBER`. Còn sinh viên đang ghi danh chưa có nhóm chỉ là cảnh báo `STUDENT_WITHOUT_GROUP`: giảng viên xác nhận thì vẫn phát hành. | UC 45, FR-025; người dùng chốt 2026-10-09 |
| BR-U12-22 | Đổi thành viên khi lớp có bài nhóm đang mở được phép; mục đang do người bị bỏ giữ tự nhả khóa (U14), bản nháp chưa "Xong" của người đó bị bỏ, nội dung đã "Xong" giữ nguyên với tên tác giả. Bản nộp đã có không đổi. | US-GRP-003 S2, U14 |

## 4. Sinh viên và audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U12-30 | Sinh viên (R5) thấy nhóm của mình trong lớp ở Student Class Detail: tên nhóm, thành viên, trưởng nhóm, tài liệu nhóm của từng bài nhóm đã mở và yêu cầu đổi trưởng nhóm gần nhất của mình (trạng thái, lý do từ chối). Không thấy nhóm khác, không thấy yêu cầu của người khác (chỉ biết nhóm đang có yêu cầu chờ). | UC 16, FR-025 |
| BR-U12-31 | Thành viên rời nhóm, nhóm mới khi lớp có bài nhóm đang mở: báo U14 qua `GroupChangePort` trong cùng transaction (nhả khóa mục, tạo tài liệu nhóm). Event `group.membership-changed`, `group.leader-requested`, `group.leader-changed`, `group.leader-request-rejected` sau commit chỉ cho U16 báo trong app (yêu cầu mới báo giảng viên lớp); quyền của trưởng nhóm U14 đọc trực tiếp qua `GroupMembershipPort`. | FR-011 |
| BR-U12-32 | Audit: tạo/sửa/xóa nhóm, chia ngẫu nhiên (khi lưu), đổi trưởng nhóm, gửi/hủy/duyệt/từ chối yêu cầu; truy cập ngoài phạm vi bị từ chối. | FR-014 |
| BR-U12-33 | Popup Student Detail (U04, mở từ tab Students) hiện nhóm của sinh viên từ dữ liệu `GET /classes/{classId}/groups` đã tải; U12 không thêm API riêng và backend U04 không gọi U12. | UC 32; người dùng chốt 2026-10-09 |
