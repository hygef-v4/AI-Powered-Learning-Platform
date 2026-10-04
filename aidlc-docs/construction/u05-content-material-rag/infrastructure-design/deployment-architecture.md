# U05 Content, Material & RAG - Deployment Architecture

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

**Text alternative**: Trình duyệt dùng màn học liệu qua Nginx tới module U05 trong backend và nhúng video bằng iframe `youtube-nocookie`. Backend lưu module và lesson vào PostgreSQL có pgvector và gửi việc quét qua RabbitMQ. Worker nhận việc, đọc tệp từ U03 hoặc lấy phụ đề và tiêu đề video từ YouTube, xin phép AI qua U13 (trần chi phí trong Redis), gọi Gemini tạo một vector rồi ghi kết quả quét vào `lessons`. Khi U13 truy xuất, backend gọi Gemini tạo vector câu hỏi rồi tìm lesson gần nhất trong pgvector.

## Lưu ý triển khai
- Đổi image PostgreSQL giữ nguyên volume dữ liệu (cùng major 16).
- Không có key → backend và worker dùng adapter giả, vẫn chạy được local.
