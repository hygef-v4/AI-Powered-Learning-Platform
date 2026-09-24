# U08 Assessment Core & Publication - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: U08] --> [postgres: assignments, publications]
                                          |
                                          +--job mở/đóng--> [rabbitmq] --> [worker: U08]
                                                                              |
                                                          event u08.assignment.* --> U11, U16
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U08 trong backend; U08 lưu bài và lượt phát hành vào PostgreSQL và tạo job mở/đóng qua RabbitMQ. Worker chạy job đúng giờ, đổi trạng thái và phát event `u08.assignment.*` cho U11 và U16.

U08 không thêm container hay volume.
