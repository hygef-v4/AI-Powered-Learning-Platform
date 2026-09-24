# U01 Account & Access - Câu hỏi NFR Design

Hỏi qua giao diện chọn đáp án.

## Câu D1 - Cài đặt rate limit

A) Bucket4j + Redis, token bucket.

B) Tự viết bộ đếm cửa sổ cố định bằng Redis INCR + TTL.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu D2 - Circuit breaker cho Redis, SMTP, U04

A) Resilience4j cho cả ba.

B) Chỉ timeout, không circuit breaker.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu D3 - Đăng xuất với JWT

A) Xóa cookie + thu hồi refresh, chấp nhận access còn hạn tối đa 15 phút.

B) Thêm denylist jti trong Redis.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu D4 - Kiểm thử chịu lỗi (RESILIENCY-14)

A) Integration test dừng phụ thuộc bằng Testcontainers, chạy trong CI.

B) Kiểm tay theo checklist trước demo.

C) Để sang Operations.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A
