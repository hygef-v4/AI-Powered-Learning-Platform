# U06 Rubric & Question Bank - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: module U06] --> [postgres: bank_items]
                                          |
                                          +--> U03 --> Google Drive (XML mẫu Draw.io)
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U06 trong backend. U06 lưu câu hỏi và rubric vào bảng `bank_items` của PostgreSQL; XML mẫu của câu Draw.io được lưu qua U03 lên Google Drive.

U06 không thêm container hay volume; triển khai cùng image backend.
