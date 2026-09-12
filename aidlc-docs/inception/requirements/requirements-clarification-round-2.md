# Câu hỏi làm rõ yêu cầu - Vòng 2

Hai quyết định dưới đây cần được xác nhận để đáp ứng Resiliency Baseline đã bật. Vui lòng điền một chữ cái lựa chọn sau mỗi thẻ `[Answer]:`. Nếu chọn `X`, hãy ghi thêm mô tả ngay sau chữ `X`.

## Question 1
Chiến lược triển khai nào sẽ được dùng cho MVP?

A) Direct/in-place — phù hợp nhất với MVP chạy container, mức quan trọng trung bình và rollback bằng image đã khóa phiên bản

B) Rolling — thay thế instance dần dần, cần nhiều hơn một instance khi triển khai

C) Blue/green — duy trì hai môi trường để chuyển đổi, chi phí và vận hành cao hơn

D) Canary — chuyển traffic dần theo metric, phức tạp hơn phạm vi MVP hiện tại

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: a

## Question 2
Resiliency Baseline yêu cầu workload production chạy trên ít nhất hai availability zone. Bạn muốn xử lý topology thế nào?

A) Giữ Resiliency Baseline: local/demo có thể chạy một instance; khi triển khai production sẽ dùng single-region, multi-zone

B) Tắt Resiliency Baseline: cho phép production chạy single-region, single-zone và chấp nhận rủi ro mất dịch vụ khi zone gặp sự cố

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: a
