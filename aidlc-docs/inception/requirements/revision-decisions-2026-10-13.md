# Quyết định còn mở — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

Đây là các chi tiết chưa được người dùng quyết định; không chặn việc đồng bộ các thay đổi đã chốt. Dùng defaults cũ không mâu thuẫn và không cam kết triển khai lựa chọn mới.

## D01 — Quiz
Quiz có cần tạo điểm official và lịch mở/đóng không?

A) Practice như baseline trước, không Gradebook (đang giữ tạm).

B) Có cả Practice/Graded, lịch và Gradebook.

X) Khác: mô tả cụ thể.

[Answer]: Chưa chốt; giữ A trong tài liệu tạm.

## D02 — Assignment nhóm
Có cần Group Practice không?

A) Nhóm chỉ Graded như baseline trước (đang giữ tạm).

B) Nhóm có cả Practice/Graded.

X) Khác: mô tả cụ thể.

[Answer]: Chưa chốt; giữ A trong tài liệu tạm.

## D03 — Student AI và tài liệu nhóm
Sinh viên được gửi dữ liệu nào cho AI/agent riêng?

A) Chỉ phần mình được sửa; mở toàn bài nhóm phải chốt riêng (đang giữ tạm).

B) Toàn bài nhóm nếu giảng viên cho phép.

X) Khác: mô tả cụ thể.

[Answer]: Chưa chốt; dùng A hạn chế nhất.

## D04 — Quota API chung
Giới hạn giờ/ngày và cách đặt mức theo provider/model cần thống nhất.

A) Mỗi giảng viên có giới hạn giờ và ngày, thêm trần toàn hệ thống.

B) Chỉ giới hạn ngày, thêm trần toàn hệ thống.

X) Khác: ghi mức và đơn vị cụ thể.

[Answer]: Chưa chốt; không tự gán số token mặc định.

## D05 — Đã chốt: bổ sung góp ý hội đồng

[Answer]: Người dùng yêu cầu bổ sung ngày 2026-10-13: backup Google Drive có kiểm thử restore; ghi nhận hoạt động rời trang cho Assignment/Quiz để giảng viên xem xét; email sự kiện quan trọng; minh bạch token/nguồn AI, không bán credit.

Chi tiết và tiêu chí nghiệm thu: [council-features-2026-10-13.md](council-features-2026-10-13.md). Chốt phạm vi tài liệu, chưa xác nhận triển khai. Lịch/retention backup, retention activity và quota số cụ thể cần cấu hình/xác nhận trước triển khai; không chốt thi giám sát hoặc tự kết luận gian lận.
