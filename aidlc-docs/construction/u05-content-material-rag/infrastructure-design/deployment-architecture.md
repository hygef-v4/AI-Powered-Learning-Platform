# U05 Content, Material & RAG - Deployment Architecture

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: U05] --việc--> [rabbitmq: jobs.gemini, jobs.youtube] --> [worker: U05]
   |  (iframe youtube-nocookie)        |      |                             |    |    |
   v                                   v      v                             v    v    v
 YouTube (nhúng video)          [postgres+pgvector] [redis]            U03   Gemini  YouTube
                                 modules,          (qua U13:       (đọc   API     phụ đề
                                 lessons + vector  gemini:daily-    file)
                                                   cost:*)
                                       ^                                    |
                                       +------------ ghi kết quả quét ------+
```

**Text alternative**: Trình duyệt dùng màn học liệu qua Nginx tới module U05 trong backend và nhúng video bằng iframe `youtube-nocookie`. Backend lưu module và lesson vào PostgreSQL có pgvector và gửi việc quét qua RabbitMQ. Worker nhận việc, đọc tệp từ U03 hoặc lấy phụ đề và tiêu đề video từ YouTube, xin phép AI qua U13 (trần chi phí trong Redis), gọi Gemini tóm tắt và tạo một vector từ bản tóm tắt rồi ghi kết quả quét vào `lessons`. Khi U13 truy xuất, backend gọi Gemini tạo vector câu hỏi rồi tìm lesson gần nhất trong pgvector.

## Lưu ý triển khai
- Đổi image PostgreSQL giữ nguyên volume dữ liệu (cùng major 16).
- Không có key → backend và worker dùng adapter giả, vẫn chạy được local.
