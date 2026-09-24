# U07 Payment & AI Credit - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: U07] --tạo link/tra cứu--> PayOS
     |                     ^                |                               |
     | chuyển sang trang   | webhook HTTPS  v                               |
     +------> PayOS -------+          [postgres]  [redis]  [rabbitmq]       |
                                        ví, sổ cái  rate    jobs.u07.*      |
                                                    limit       |           |
                                                                v           |
                                                  [worker: đối soát] -------+
```

**Text alternative**: Người dùng mua qua Nginx tới module U07 trong backend; backend tạo link PayOS và chuyển người dùng sang trang PayOS để quét QR. PayOS gửi webhook qua HTTPS vào Nginx rồi tới backend; backend kiểm chữ ký, ghi ví và sổ cái trong PostgreSQL, rate limit webhook bằng Redis. Worker nhận job đối soát qua RabbitMQ và tra cứu trạng thái trên PayOS.
