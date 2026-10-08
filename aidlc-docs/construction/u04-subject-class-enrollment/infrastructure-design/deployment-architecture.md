# U04 Subject, Class, Enrollment & Learning Access - Deployment Architecture

**Bản tài liệu 2026-10-08**: UC 12, 13, 27, 28, 45, 46, 47, 48, 49, 50, 63, 64, 65, 66; primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-CAT-005, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: module U04]
                                      |              |            |
                                      v              v            v
                                [postgres]        [redis]     [rabbitmq]
                                subjects          ratelimit:  platform.events
                                course_classes    invite-     (enrollment.activated)
                                enrollments       code:*            |
                                                                    v
                                                            U16 (thông báo, email)
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U04 trong backend. U04 lưu môn, lớp, ghi danh vào PostgreSQL; bộ đếm nhập sai mã mời vào Redis; và phát event ghi danh lên exchange `platform.events` của RabbitMQ để U16 tạo thông báo và gửi email.

U04 không thêm container hay volume mới; triển khai cùng image backend.
