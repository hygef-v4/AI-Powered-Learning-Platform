# U02 Audit, Job & Event - Câu hỏi NFR Requirements

## Câu N1 - Worker chạy ở đâu

A) Container worker riêng.

B) Chạy chung trong backend.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu N2 - RabbitMQ giữ message khi khởi động lại

A) Queue durable + message persistent.

B) Không lưu.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu N3 - Số job mỗi worker xử lý đồng thời

A) 4 job cùng lúc, audit có luồng riêng.

B) 1 job cùng lúc.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A
