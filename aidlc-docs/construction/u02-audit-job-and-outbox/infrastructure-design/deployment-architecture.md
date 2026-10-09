# U02 Audit - Deployment Architecture

**Bản tài liệu 2026-10-09**: UC 73 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AUD-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
 mạng internal
 +--------------------------------------------------+
 |  [backend] --INSERT audit_logs (cùng transaction)--> [postgres] |
 |  [worker]  --INSERT audit_logs (handler)----------> [postgres] |
 +--------------------------------------------------+
```

**Text alternative**: Backend và worker ghi `audit_logs` vào PostgreSQL trong transaction nghiệp vụ; audit không đi qua RabbitMQ nên RabbitMQ lỗi không ảnh hưởng audit. Tra cứu audit chạy ở backend.
