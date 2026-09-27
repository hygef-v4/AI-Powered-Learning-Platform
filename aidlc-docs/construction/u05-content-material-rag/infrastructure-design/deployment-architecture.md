# U05 Content, Material & RAG - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: U05] --job--> [rabbitmq: jobs.gemini, jobs.youtube] --> [worker: U05]
   |  (iframe youtube-nocookie)        |      |                             |    |    |
   v                                   v      v                             v    v    v
 YouTube (nhúng video)          [postgres+pgvector] [redis]            U03   Gemini  YouTube
                                 nội dung, đoạn,   (qua U13:       (đọc   API     Data API /
                                 vector            gemini:daily-    file)          caption
                                                   cost:*)
                                       ^                                    |
                                       +------------ ghi đoạn --------------+
```

**Text alternative**: Trình duyệt dùng trang nội dung qua Nginx tới module U05 trong backend và nhúng video bằng iframe `youtube-nocookie`. Backend lưu nội dung vào PostgreSQL có pgvector, đếm trần embedding trong Redis, và tạo job qua RabbitMQ. Worker nhận job, đọc file từ U03, lấy caption/playlist từ YouTube, gọi Gemini tạo vector rồi ghi đoạn vào PostgreSQL. Khi U13 truy xuất, backend gọi Gemini tạo vector câu hỏi và tìm trong pgvector.

## Lưu ý triển khai
- Đổi image PostgreSQL giữ nguyên volume dữ liệu (cùng major 16).
- Không có key → backend và worker dùng adapter giả, vẫn chạy được local.
