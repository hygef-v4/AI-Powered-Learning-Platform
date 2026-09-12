# Câu hỏi làm rõ yêu cầu

Các câu hỏi dưới đây giải quyết hai câu trả lời chưa đúng định dạng và các quyết định bắt buộc của Resiliency Baseline đã được bật. Vui lòng điền một chữ cái lựa chọn sau mỗi thẻ `[Answer]:`. Nếu chọn `X`, hãy ghi thêm mô tả ngay sau chữ `X`.

## Question 1
Xác nhận phạm vi kênh web cho phiên bản đầu.

A) Chỉ web responsive; không đặt yêu cầu chuẩn bị riêng cho ứng dụng mobile

B) Web responsive và thiết kế API để có thể bổ sung ứng dụng mobile sau

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: a

## Question 2
Xác nhận bộ tích hợp bên ngoài bắt buộc cho MVP.

A) Nhà cung cấp AI/LLM và lưu trữ tệp

B) Nhà cung cấp AI/LLM, lưu trữ tệp, thanh toán và email/thông báo

C) Nhà cung cấp AI/LLM, thanh toán và email/thông báo; chưa cần lưu trữ tệp bên ngoài

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: b

## Question 3
Mức độ quan trọng về nghiệp vụ của MVP là gì?

A) Thấp — môi trường demo/thử nghiệm; gián đoạn không ảnh hưởng hoạt động giảng dạy thực tế

B) Trung bình — dùng thử với người dùng thật; gián đoạn gây bất tiện nhưng có thể xử lý thủ công

C) Cao — phục vụ lớp học thật; gián đoạn làm dừng hoạt động học hoặc đánh giá

D) Nghiêm trọng — gián đoạn gây hậu quả lớn về tài chính, pháp lý hoặc vận hành

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: b

## Question 4
Mục tiêu RTO/RPO và chiến lược Disaster Recovery phù hợp là gì?

A) RTO/RPO tính bằng giờ — Backup & Restore, chi phí thấp nhất

B) RTO/RPO vài chục phút — Pilot Light

C) RTO/RPO vài phút — Warm Standby

D) Gần thời gian thực — Active/Active đa vùng, chi phí cao nhất

E) Không cần DR đa region; chấp nhận single-region và dùng khả năng multi-zone khi lên production

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: a

## Question 5
Các thay đổi production sẽ được quản trị theo quy trình nào?

A) Dùng quy trình quản lý thay đổi hiện có của tổ chức; ghi tên công cụ/quy trình sau thẻ `[Answer]:`

B) Chưa có quy trình; AI-DLC đề xuất quy trình gọn gồm change record, phê duyệt và ghi chú rollback

C) MVP được miễn quy trình quản lý thay đổi chính thức; ghi lý do miễn trừ sau thẻ `[Answer]:`

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: b

## Question 6
CI/CD và công cụ triển khai nên được xử lý thế nào?

A) Dùng pipeline hiện có; ghi tên công cụ sau thẻ `[Answer]:`

B) Chưa có pipeline; AI-DLC đề xuất pipeline phù hợp với runtime và cách chạy container của MVP

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: b

## Question 7
Cơ chế rollback khi triển khai thất bại là gì?

A) Triển khai lại artifact/container image đã khóa phiên bản trước đó

B) Chuyển blue/green về môi trường trước đó

C) Canary tự rollback khi health check hoặc metric suy giảm

D) Rollback nhận biết database, gồm chiến lược đảo migration/schema

E) Dùng thủ tục rollback hiện có của tổ chức; ghi tham chiếu sau thẻ `[Answer]:`

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: a

## Question 8
Chiến lược triển khai nào phù hợp với rủi ro của MVP?

A) Direct/in-place — chi phí thấp, blast radius cao hơn

B) Rolling — thay thế instance dần dần

C) Blue/green — chuyển đổi gần như không downtime, chi phí cao hơn

D) Canary — chuyển traffic dần và tự động rollback

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: chưa biết

## Question 9
Topology vùng triển khai khi đưa hệ thống lên production nên là gì?

A) Single-region, multi-zone — chịu lỗi một zone với chi phí thấp hơn

B) Multi-region active-passive — chịu lỗi cả region với cơ chế failover

C) Multi-region active-active — chịu lỗi region và giảm tối đa downtime, chi phí cao nhất

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: single-region, single zone

## Question 10
Sự cố production sẽ được xử lý theo quy trình nào?

A) Dùng quy trình incident response hiện có; ghi tên hoặc tham chiếu sau thẻ `[Answer]:`

B) Chưa có quy trình; AI-DLC đề xuất quy trình incident response và Correction of Errors gọn cho đội ngũ

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: b
