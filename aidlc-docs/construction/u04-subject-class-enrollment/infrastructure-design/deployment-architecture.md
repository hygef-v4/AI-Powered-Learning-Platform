# U04 Subject, Class, Enrollment & Learning Access - Deployment Architecture

**Bản tài liệu 2026-10-09**: UC 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: module academics]
                                      |                  |
                                      v                  v
                                [postgres]          [rabbitmq]
                                subjects            platform.events
                                course_classes      (enrollment.activated)
                                enrollments               |
                                                          v
                                                  U16 (thông báo, email)
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U04 trong backend. U04 lưu môn, lớp, ghi danh vào PostgreSQL và phát event ghi danh lên exchange `platform.events` của RabbitMQ để U16 tạo thông báo và gửi email.

U04 không thêm container hay volume mới; triển khai cùng image backend.
