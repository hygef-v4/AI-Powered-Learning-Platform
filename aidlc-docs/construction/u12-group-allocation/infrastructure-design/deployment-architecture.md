# U12 Group & Allocation - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: U12] --> [postgres: nhóm, thành viên, trưởng nhóm]
                                          |
                                          +--event u12.group.*--> [rabbitmq] --> U16
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U12 trong backend; U12 lưu nhóm, thành viên và yêu cầu đổi trưởng nhóm vào PostgreSQL và phát event `u12.group.*` qua RabbitMQ cho U16. Không có container hay volume mới.
