# U12 Group & Allocation - Deployment Architecture

**Bản tài liệu 2026-10-09**: UC 16, 17 và luồng phụ nhóm (quản lý nhóm, duyệt yêu cầu đổi trưởng nhóm trong tab Students của UC 32; sẵn sàng phát hành bài nhóm của UC 45) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-001, US-GRP-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: U12] --> [postgres: nhóm, thành viên, yêu cầu đổi trưởng nhóm]
                                          |
                                          +--event group.*--> [rabbitmq: platform.events] --> U16
                                          +--GroupChangePort (cùng transaction)--> U14
                                          +--ClassAccessPort--> U04
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U12 trong backend; U12 lưu nhóm, thành viên và yêu cầu đổi trưởng nhóm vào PostgreSQL, hỏi U04 về lớp và ghi danh `ACTIVE`, phát event `group.*` qua RabbitMQ cho thông báo U16; nhóm mới hoặc thành viên rời nhóm thì báo U14 qua `GroupChangePort` trong cùng transaction, và đọc danh sách tài liệu nhóm từ U14 cho UC 16. Không có container hay volume mới.
