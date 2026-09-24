# U02 Audit, Job & Outbox - Câu hỏi Functional Design

Hỏi qua giao diện chọn đáp án.

## Câu 1 - Lưu trạng thái job

A) Bảng `jobs` trong PostgreSQL.

B) Redis có TTL.

C) Không có job status.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 2 - Chuyển sự kiện sang RabbitMQ

A) Bảng outbox + relay quét định kỳ.

B) Gửi thẳng RabbitMQ sau commit.

C) Bỏ RabbitMQ, worker quét bảng.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu 3 - Job hết lượt retry

A) Admin xem và bấm chạy lại.

B) Chỉ ghi log.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu 4 - Ai xem trạng thái job

A) Người tạo job và admin.

B) Theo quyền trên đối tượng của job.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 5 - Ghi audit

A) Cùng giao dịch nghiệp vụ.

B) Bất đồng bộ qua RabbitMQ.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu 6 - Thời gian giữ audit

A) Vĩnh viễn.

B) 1 năm.

C) 90 ngày.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 7 - Ai tra cứu audit

A) Chỉ ADMIN.

B) ADMIN + giảng viên xem audit lớp mình.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 8 - Job kẹt ở PENDING do gửi RabbitMQ lỗi

A) Quét gửi lại job PENDING quá 1 phút.

B) Chấp nhận mất.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: X - "quét lại nhưng để lên đủ lâu để nó có thể xử lý"; làm rõ: ngưỡng 5 phút

## Câu 9 - Audit có thể mất khi gửi lỗi

A) Chấp nhận có thể mất.

B) Ghi vào bảng chờ rồi quét gửi.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A
