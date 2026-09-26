# U02 Audit, Job & Event - Deployment Architecture

```
 mạng internal
 +--------------------------------------------------------------+
 |  [backend] --INSERT jobs / SELECT audit--> [postgres]        |
 |      |                                          ^            |
 |      | afterCommit                              |            |
 |      v                                          |            |
 |  [rabbitmq]  8 queue jobs.*  ------------> [worker]         |
 |                                    <-- gửi lại (sweeper) --  |
 +--------------------------------------------------------------+
```

**Text alternative**: Backend ghi `jobs` vào PostgreSQL và gửi message sang RabbitMQ sau commit. Audit được INSERT thẳng vào PostgreSQL trong transaction nghiệp vụ. Worker nhận message từ 8 queue `jobs.*`, đọc/ghi PostgreSQL, và lượt quét của worker gửi lại message cho job đến hạn. Cả bốn container nằm trên mạng `internal`, không container nào publish cổng ra ngoài.

## Luồng tạo job

1. Backend INSERT `jobs` trong transaction nghiệp vụ, commit.
2. `afterCommit` gửi `JobMessage` sang exchange `jobs`.
3. Worker nhận, claim bằng UPDATE có điều kiện, chạy handler, cập nhật `jobs`, ack.

## Khi RabbitMQ khởi động lại

- Message persistent vẫn còn.
- Message gửi lỗi trong lúc RabbitMQ tắt: job vẫn `PENDING`, lượt quét gửi lại sau tối đa 5 phút. Audit không đi qua RabbitMQ nên không bị ảnh hưởng; chỉ event thông báo trong khoảng đó có thể mất.
