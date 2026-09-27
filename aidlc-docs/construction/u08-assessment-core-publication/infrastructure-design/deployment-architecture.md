# U08 Assessment Core & Publication - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: U08] --> [postgres: assignments, publications]
                                          |
                                          +--job mở/đóng--> [rabbitmq] --> [worker: U08]
                                                                              |
                                           port onOpened/onRetired --> U11, U14; event assignment.opened --> U16
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U08 trong backend; U08 lưu bài và lượt phát hành vào PostgreSQL và tạo job mở/đóng qua RabbitMQ. Worker chạy job đúng giờ và đổi trạng thái; trong cùng transaction gọi `PublicationLifecyclePort` để U11, U14 tạo job của mình (tạo tài liệu nhóm, tự nộp), sau commit phát event `assignment.opened` cho U16 gửi thông báo.

U08 không thêm container hay volume.
