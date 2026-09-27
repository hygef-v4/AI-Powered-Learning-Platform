# U09 Question Type Authoring - Deployment Architecture

```
 Trình duyệt ------ iframe ------> embed.diagrams.net (Internet)
     |
     +--HTTPS--> [nginx] --(DOCX ≤ 20 MB)--> [backend: U09] --> [postgres: config, khung]
                                                   |
                                                   +--> U03 --> Google Drive (ảnh tài liệu)
```

**Text alternative**: Trình duyệt tải trình vẽ Draw.io trực tiếp từ `embed.diagrams.net` trong iframe. Các thao tác cấu hình, khung, nhập/xuất DOCX đi qua Nginx (DOCX tối đa 20 MB) tới module U09 trong backend; U09 lưu cấu hình và khung vào PostgreSQL, ảnh trong tài liệu lưu qua U03 lên Google Drive.
