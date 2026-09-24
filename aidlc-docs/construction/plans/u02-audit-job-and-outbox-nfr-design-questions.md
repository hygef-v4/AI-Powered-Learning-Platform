# U02 Audit, Job & Outbox - Câu hỏi NFR Design

## Câu D1 - Cơ chế retry job

A) Bảng `jobs` + lượt quét.

B) Queue trễ của RabbitMQ.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu D2 - Tổ chức queue

A) Mỗi loại job một queue.

B) Một queue chung cho mọi job.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A
