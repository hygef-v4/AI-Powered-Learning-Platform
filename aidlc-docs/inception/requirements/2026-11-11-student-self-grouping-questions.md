# Sinh viên tự chia nhóm, giảng viên duyệt — yêu cầu và câu hỏi xác nhận

Chưa sửa mã nguồn.

## Yêu cầu đã ghi nhận

Giảng viên muốn cho sinh viên trong lớp tự chia nhóm với nhau, sau đó giảng viên duyệt.

- **Loại yêu cầu**: New Feature, mở rộng FR-025 / UC 57 / U12.
- **Phạm vi**: backend `groups` (bảng mới, service, API), frontend `features/groups` (màn Nhóm của sinh viên và tab Nhóm của giảng viên), thông báo, tài liệu U12.
- **Độ phức tạp**: vừa. Dùng lại `GroupSetRules`, khóa `version` của bộ nhóm và cách duyệt của yêu cầu đổi trưởng nhóm.
- **Độ sâu**: Standard.

## Hiện trạng liên quan

- Bộ nhóm chính thức: `class_groups`, `class_group_members`; mỗi sinh viên tối đa một nhóm trong lớp (BR-U12-02); lưu cả bộ kèm `version` (BR-U12-04).
- Chỉ giảng viên chính quản lý bộ nhóm; trưởng nhóm được đổi tên nhóm của mình (BR-U12-01).
- Chia ngẫu nhiên chỉ chia sinh viên chưa có nhóm, có xem trước (BR-U12-03).
- Sửa nhóm khi lớp `DRAFT`/`OPEN`; sinh viên chỉ thao tác ở lớp `OPEN` (BR-U12-06).
- Yêu cầu đổi trưởng nhóm: PENDING / APPROVED / REJECTED / CANCELLED, tối đa một PENDING mỗi nhóm (BR-U12-10, 11).

## Mô hình đề xuất

Đề xuất nhóm nằm riêng, không ghi thẳng vào bộ nhóm chính thức. Khi giảng viên duyệt, hệ thống thêm nhóm vào bộ nhóm trong một transaction, kiểm lại bằng `GroupSetRules` và tăng `version`. Các câu dưới đây chốt chi tiết; phương án đầu tiên của mỗi câu là đề xuất của AI.

## Question 1
Sinh viên lập nhóm theo cách nào?

A) Một bạn tạo đề xuất và làm trưởng nhóm, mời bạn khác trong lớp; người được mời phải chấp nhận mới vào nhóm (đề xuất)

B) Một bạn tạo đề xuất; bạn khác tự xin vào, trưởng nhóm chấp nhận hoặc từ chối

C) Cả A và B: trưởng nhóm mời được, sinh viên cũng xin vào được

D) Nhóm mở: sinh viên tự vào nhóm còn chỗ, không cần ai chấp nhận

E) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
Giảng viên bật tính năng tự chia nhóm thế nào?

A) Giảng viên bật/tắt cho từng lớp, hạn chót tùy chọn; tới hạn thì sinh viên không tạo, mời hay gửi duyệt được nữa (đề xuất)

B) Bắt buộc có thời gian mở và hạn chót mỗi lần bật

C) Luôn bật khi lớp `OPEN`, không có cấu hình

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
Ràng buộc sĩ số của đề xuất?

A) Giảng viên đặt sĩ số tối thiểu và tối đa khi bật (2–20 như chia ngẫu nhiên); đề xuất phải đủ tối thiểu mới gửi duyệt, không mời quá tối đa (đề xuất)

B) Chỉ đặt sĩ số tối đa

C) Không ràng buộc; giảng viên tự xem khi duyệt

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Giảng viên duyệt đề xuất thế nào?

A) Duyệt hoặc từ chối nguyên trạng, duyệt được nhiều đề xuất một lần; từ chối có lý do tùy chọn, các thành viên được tự do lập nhóm khác (đề xuất)

B) Như A, và giảng viên được sửa đề xuất (đổi tên, thêm/bớt người, đổi trưởng nhóm) rồi duyệt

C) Tự động duyệt nếu đề xuất thỏa ràng buộc; giảng viên chỉ xem và chỉnh bộ nhóm sau

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
Sinh viên chưa có nhóm sau hạn chót xử lý thế nào?

A) Giảng viên xem danh sách sinh viên chưa có nhóm và dùng công cụ sẵn có: tạo tay hoặc chia ngẫu nhiên phần còn lại (đề xuất, không thêm logic mới)

B) Như A, thêm nút "Xếp vào nhóm còn thiếu": đưa người còn lại vào các nhóm chưa đủ tối đa, có xem trước

C) Hệ thống tự chia ngẫu nhiên khi tới hạn chót

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
Sau khi đề xuất đã thành nhóm chính thức, sinh viên còn thay đổi được gì?

A) Không; mọi thay đổi thành viên do giảng viên làm như hiện nay, trưởng nhóm vẫn đổi được tên nhóm (đề xuất)

B) Trưởng nhóm được mời thêm người khi nhóm chưa đủ tối đa và đợt còn mở; người được mời chấp nhận thì vào thẳng nhóm

C) Như B, và thành viên được xin rời nhóm, giảng viên duyệt

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 7
Khi đợt tự chia nhóm đang mở, giảng viên có sửa tay bộ nhóm được không?

A) Có. Sinh viên đã có nhóm chính thức thì không tham gia đề xuất; người được giảng viên xếp nhóm tay thì tự rời đề xuất đang có, đề xuất thiếu tối thiểu thì quay về nháp (đề xuất)

B) Không; giảng viên chỉ sửa tay sau khi tắt đợt

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 8
Gửi thông báo nào?

A) Thông báo trong ứng dụng: lời mời cho người được mời; đề xuất mới chờ duyệt cho giảng viên; kết quả duyệt hoặc từ chối cho thành viên đề xuất (đề xuất)

B) Như A, cộng thêm email theo cài đặt thông báo của từng người

C) Không gửi thông báo; chỉ hiện trên trang Nhóm

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 9
Extension của AI-DLC cho tính năng này?

A) Giữ cấu hình hiện tại: Security Baseline và Resiliency Baseline bật trong phạm vi đã duyệt, Property-Based Testing tắt (đề xuất)

B) Bật thêm Property-Based Testing cho các quy tắc chia nhóm

C) Other (please describe after [Answer]: tag below)

[Answer]: A
