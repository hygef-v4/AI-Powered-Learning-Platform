# U09 Question Type Authoring - Deployment Architecture

**Bản tài liệu 2026-10-09**: nội dung theo dạng bài của UC 35 (cài đặt quiz), 42, 43, 44, 45 và popup Rubric Detail của UC 46 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
 Trình duyệt ------ iframe ------> embed.diagrams.net (Internet)
     |
     +--HTTPS--> [nginx] --(DOCX ≤ 20 MB)--> [backend: U09] --> [postgres: config, khung]
                                                   |
                                                   +--> U03 --> Google Drive (ảnh tài liệu)
```

**Text alternative**: Trình duyệt tải trình vẽ Draw.io trực tiếp từ `embed.diagrams.net` trong iframe. Các thao tác cấu hình, khung, nhập/xuất DOCX đi qua Nginx (DOCX tối đa 20 MB) tới module U09 trong backend; U09 lưu cấu hình và khung vào PostgreSQL, ảnh trong tài liệu lưu qua U03 lên Google Drive.
