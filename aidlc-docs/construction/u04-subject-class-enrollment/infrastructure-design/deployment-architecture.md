# U04 Subject, Class, Enrollment & Learning Access - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: module U04]
                                      |        |          |
                                      v        v          v
                                [postgres]  [redis]   [rabbitmq]
                                subjects    u04:      platform.events
                                classes     invite-   (u04.enrollment.activated)
                                enrollments fail:*          |
                                                            v
                                                    U16 (thông báo, email)
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U04 trong backend. U04 lưu môn, lớp, ghi danh vào PostgreSQL; bộ đếm nhập sai mã mời vào Redis; và phát event ghi danh lên exchange `platform.events` của RabbitMQ để U16 tạo thông báo và gửi email.

U04 không thêm container hay volume mới; triển khai cùng image backend.
