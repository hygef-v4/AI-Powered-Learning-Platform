# U07 Payment & AI Credit - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: U07] --tạo link/tra cứu--> PayOS
     |                     ^                |                               |
     | chuyển sang trang   | webhook HTTPS  v                               |
     +------> PayOS -------+          [postgres]  [redis]  [rabbitmq]       |
                                        payments,   rate    jobs.payos      |
                                        số dư       limit                   |
                                                                |           |
                                                                v           |
                                                  [worker: đối soát] -------+
```

**Text alternative**: Người dùng mua qua Nginx tới module U07 trong backend; backend tạo link PayOS và chuyển người dùng sang trang PayOS để quét QR. PayOS gửi webhook qua HTTPS vào Nginx rồi tới backend; backend kiểm chữ ký, cập nhật `payments` và số dư trong PostgreSQL, rate limit webhook bằng Redis. Scanner của worker chọn giao dịch cần đối soát và gửi việc `PAYOS_CHECK` qua RabbitMQ; handler tra cứu trạng thái trên PayOS.
