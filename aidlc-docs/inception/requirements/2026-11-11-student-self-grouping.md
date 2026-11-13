# Sinh viên tự chia nhóm, giảng viên duyệt — yêu cầu

Mở rộng FR-025 / UC 57 / U12. Quyết định: [câu hỏi xác nhận](2026-11-11-student-self-grouping-questions.md), người dùng chọn A cho cả 9 câu (2026-11-11).

## Phân tích yêu cầu

- **Yêu cầu**: "cho các bạn trong lớp tự chia nhóm với nhau sau đó giảng viên duyệt".
- **Loại**: New Feature, mở rộng tính năng nhóm hiện có.
- **Phạm vi**: nhiều thành phần trong một unit: backend `groups` (bảng, service, API), `notifications` (loại thông báo mới), frontend `features/groups` (màn Nhóm của sinh viên, tab Nhóm của giảng viên), tài liệu U12.
- **Độ phức tạp**: vừa.
- **Không đổi**: các quy tắc bộ nhóm chính thức BR-U12-01…12, tài liệu nhóm, chấm điểm nhóm, chia ngẫu nhiên.

## Thuật ngữ

- **Đợt tự chia nhóm**: cấu hình của một lớp cho phép sinh viên lập đề xuất.
- **Đề xuất nhóm**: nhóm do sinh viên lập, chưa thuộc bộ nhóm chính thức.
- **Nhóm chính thức**: nhóm trong `class_groups`, như hiện nay.

## Yêu cầu chức năng

### SG-01 — Bật/tắt đợt tự chia nhóm (Q2, Q3)

- Giảng viên chính của lớp `DRAFT`/`OPEN` bật hoặc tắt đợt cho lớp đó.
- Khi bật, giảng viên đặt sĩ số tối thiểu và tối đa, mỗi số trong 2–20, tối thiểu ≤ tối đa, và hạn chót tùy chọn (thời điểm trong tương lai).
- Giảng viên sửa được sĩ số và hạn chót khi đợt đang bật. Đề xuất đã gửi duyệt không còn thỏa sĩ số mới vẫn chờ duyệt; giảng viên thấy cảnh báo khi duyệt.
- Đợt **mở** khi đang bật, lớp `OPEN` và chưa tới hạn chót. Tắt đợt hoặc tới hạn chót thì đợt **đóng**.
- Đợt đóng: sinh viên không tạo đề xuất, mời, chấp nhận lời mời hay gửi duyệt được; lời mời đang chờ hết hiệu lực. Đề xuất đã gửi duyệt vẫn chờ giảng viên quyết định. Đề xuất nháp được giữ, sinh viên xem được, thao tác lại khi đợt mở lại.

### SG-02 — Lập đề xuất và mời (Q1, Q3, Q7)

- Sinh viên ghi danh `ACTIVE`, chưa có nhóm chính thức và chưa ở đề xuất nào đang hoạt động tạo được một đề xuất khi đợt mở. Người tạo là trưởng nhóm và là thành viên.
- Đề xuất có tên 1–100 ký tự, không trùng tên nhóm chính thức hay đề xuất đang hoạt động khác trong lớp (không phân biệt hoa thường).
- Trưởng nhóm mời sinh viên `ACTIVE` khác của lớp chưa có nhóm chính thức. Số thành viên đã chấp nhận cộng lời mời đang chờ không vượt sĩ số tối đa. Trưởng nhóm hủy được lời mời đang chờ.
- Người được mời chấp nhận hoặc từ chối. Chấp nhận chỉ khi chưa ở đề xuất đang hoạt động khác; một sinh viên có thể nhận nhiều lời mời cùng lúc, chấp nhận một thì các lời mời còn lại tự hủy.
- Thành viên (không phải trưởng nhóm) rời được đề xuất nháp. Trưởng nhóm rút bỏ đề xuất nháp thì đề xuất bị hủy và mọi thành viên được tự do.
- Mỗi sinh viên chỉ là thành viên của **một** đề xuất đang hoạt động (nháp hoặc chờ duyệt), được bảo đảm ở database.

### SG-03 — Gửi duyệt (Q3)

- Trưởng nhóm gửi duyệt khi đợt mở và số thành viên đã chấp nhận ≥ sĩ số tối thiểu. Lời mời còn chờ tự hủy khi gửi.
- Đề xuất chờ duyệt bị khóa. Trưởng nhóm rút về nháp được để sửa.

### SG-04 — Giảng viên duyệt hoặc từ chối (Q4)

- Giảng viên chính của lớp `DRAFT`/`OPEN` thấy danh sách đề xuất chờ duyệt và duyệt hoặc từ chối nguyên trạng; chọn được nhiều đề xuất để duyệt một lần.
- Duyệt: trong một transaction, thêm nhóm (tên, trưởng nhóm, thành viên của đề xuất) vào bộ nhóm của lớp, kiểm bằng các quy tắc của bộ nhóm (BR-U12-02), tăng `version` của bộ nhóm, ghi đề xuất là đã duyệt kèm id nhóm chính thức.
- Duyệt nhiều: mỗi đề xuất được kiểm riêng; đề xuất lỗi được báo kèm lý do, các đề xuất hợp lệ vẫn được duyệt.
- Từ chối: lý do tùy chọn tối đa 1.000 ký tự; các thành viên được tự do lập hoặc vào đề xuất khác.

### SG-05 — Sinh viên chưa có nhóm (Q5)

- Tab Nhóm của giảng viên hiện danh sách sinh viên `ACTIVE` chưa có nhóm chính thức, ghi rõ ai đang ở đề xuất nào và trạng thái đề xuất.
- Giảng viên xử lý người còn lại bằng công cụ hiện có (tạo tay, chia ngẫu nhiên người chưa có nhóm). Không thêm logic xếp tự động.

### SG-06 — Sau khi duyệt (Q6)

- Nhóm đã duyệt là nhóm chính thức bình thường. Sinh viên không thay đổi thành viên; trưởng nhóm vẫn đổi tên nhóm và thành viên vẫn gửi yêu cầu đổi trưởng nhóm như hiện nay.

### SG-07 — Giảng viên sửa tay khi đợt đang mở (Q7)

- Giảng viên vẫn sửa bộ nhóm chính thức bất kỳ lúc nào theo BR-U12.
- Khi lưu bộ nhóm, sinh viên vừa được xếp vào nhóm chính thức rời đề xuất đang có của họ (lời mời đang chờ của họ tự hủy). Nếu người đó là trưởng nhóm, đề xuất bị hủy. Đề xuất chờ duyệt còn ít hơn sĩ số tối thiểu thì quay về nháp.
- Sinh viên bị gỡ khỏi lớp: rời đề xuất theo cùng cách.

### SG-08 — Thông báo (Q8)

Thông báo trong ứng dụng qua đường sự kiện → thông báo hiện có:

- Người được mời: có lời mời mới.
- Giảng viên chính của lớp: có đề xuất mới chờ duyệt.
- Thành viên đề xuất: đề xuất được duyệt hoặc bị từ chối (kèm lý do nếu có).

Không gửi email.

### SG-09 — Giao diện

- **Sinh viên** (trang Nhóm của lớp), theo trạng thái:
  - Chưa có nhóm, đợt đóng: thông báo chờ giảng viên xếp nhóm như hiện nay.
  - Chưa có nhóm, đợt mở: nút tạo đề xuất, danh sách lời mời đến mình, sĩ số và hạn chót.
  - Đang ở đề xuất: thành viên, lời mời đang chờ, trạng thái (nháp, chờ duyệt, bị từ chối kèm lý do); trưởng nhóm mời/hủy mời, gửi duyệt, rút về nháp, hủy đề xuất; thành viên rời đề xuất.
  - Đã có nhóm chính thức: như hiện nay.
- **Giảng viên** (tab Nhóm của lớp): hộp cài đặt đợt (bật/tắt, sĩ số, hạn chót); khu "Đề xuất chờ duyệt" có số đếm, duyệt từng cái hoặc nhiều cái, từ chối; danh sách sinh viên chưa có nhóm.

## Yêu cầu phi chức năng

- **Phân quyền (Security)**: mọi kiểm tra ở backend. Sinh viên chỉ thao tác trong lớp mình ghi danh `ACTIVE`, lớp `OPEN`; chỉ trưởng nhóm thao tác đề xuất của mình; chỉ người được mời trả lời lời mời; chỉ giảng viên chính cài đặt đợt và duyệt. Không lộ đề xuất của lớp khác.
- **Audit**: bật/tắt/sửa đợt, gửi duyệt, duyệt, từ chối được ghi audit như các thao tác nhóm khác.
- **Nhất quán khi đồng thời**: ràng buộc một đề xuất đang hoạt động mỗi sinh viên ở database; đề xuất có `version`; duyệt dùng khóa `version` của bộ nhóm, xung đột thì báo để tải lại, không ghi một nửa.
- **Resiliency**: lỗi gửi thông báo không làm hỏng thao tác chính, theo cách xử lý sự kiện hiện có.
- **Hiệu năng**: lớp tới vài trăm sinh viên; tab Nhóm tải đề xuất và người chưa có nhóm trong một lượt gọi.
- **Accessibility và UI**: theo quy ước UI hiện có (WCAG 2.2 AA, `PRODUCT.md`).
- **Migration**: chỉ thêm bảng/cột mới, tương thích ngược; lớp chưa bật đợt hoạt động như hiện nay.

## Giả định

Các điểm sau câu trả lời chưa nói tới, AI chọn theo hướng đơn giản: lời mời hết hiệu lực khi đợt đóng; chấp nhận một lời mời thì hủy các lời mời khác; trưởng nhóm không rời mà chỉ hủy đề xuất; sĩ số tối đa tính cả lời mời đang chờ; đề xuất chờ duyệt không thỏa sĩ số mới vẫn được giữ. Người dùng sửa được khi duyệt tài liệu này.

## Extension

Security Baseline và Resiliency Baseline bật trong phạm vi đã duyệt; Property-Based Testing tắt (Q9).
