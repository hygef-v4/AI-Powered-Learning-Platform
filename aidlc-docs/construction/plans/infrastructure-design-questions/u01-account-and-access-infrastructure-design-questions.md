# U01 Account & Access - Câu hỏi Infrastructure Design

Hỏi qua giao diện chọn đáp án.

## Câu I1 - Nơi triển khai production

A) AWS.

B) Google Cloud.

C) Một VPS + Docker Compose.

D) Chỉ local/demo.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: C

## Câu I2 - RabbitMQ

A) Managed theo cloud.

B) Tự chạy container RabbitMQ.

C) Bỏ RabbitMQ, dùng bảng outbox + worker poll.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu I3 - Log, metric, cảnh báo

A) Dịch vụ của cloud.

B) Grafana + Prometheus + Loki tự chạy.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu I4 - Secret

A) Secret manager của cloud.

B) Biến môi trường của CI/CD.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu I5 - Backup PostgreSQL

A) pg_dump hằng ngày, mã hóa, đẩy ra ngoài VPS.

B) pg_dump mỗi 6 giờ.

C) Snapshot đĩa của nhà cung cấp VPS.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: X - "bỏ backup đi, bỏ luôn rel008"

## Câu I6 - HTTPS và reverse proxy

A) Caddy tự cấp chứng chỉ.

B) Nginx + certbot.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu I7 - Mã hóa at rest

A) Mã hóa đĩa của nhà cung cấp VPS.

B) Tự mã hóa bằng LUKS.

C) Không mã hóa, chấp nhận rủi ro.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: C
