# U06 Rubric & Question Bank - Deployment Architecture

**Bản tài liệu 2026-10-09**: UC 46, 56, 57 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: module U06] --> [postgres: questions, rubrics]
                                          |
                                          +--> U03 --> Google Drive (ảnh trong khung tài liệu)
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U06 trong backend. U06 lưu câu hỏi và rubric vào bảng `questions` và `rubrics` của PostgreSQL; ảnh trong khung tài liệu được lưu qua U03 lên Google Drive.

U06 không thêm container hay volume; triển khai cùng image backend.
