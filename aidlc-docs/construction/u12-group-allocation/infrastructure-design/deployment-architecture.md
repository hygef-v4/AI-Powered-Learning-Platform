# U12 Group & Allocation - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: U12] --> [postgres: nhóm, thành viên, trưởng nhóm]
                                          |
                                          +--event group.*--> [rabbitmq: platform.events] --> U16
                                          +--GroupChangePort (cùng transaction)--> U14
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U12 trong backend; U12 lưu nhóm, thành viên và yêu cầu đổi trưởng nhóm vào PostgreSQL phát event `group.*` qua RabbitMQ cho thông báo U16; nhóm mới sau khi bài mở hoặc thành viên rời nhóm thì báo U14 qua `GroupChangePort` trong cùng transaction. Không có container hay volume mới.
