# U15 Grading - Deployment Architecture

```
 [rabbitmq: jobs.triggered] --job GRADE_INIT--> [worker: tự chấm] --> [postgres: grades, history]
                                                                 ^
 Trình duyệt --HTTPS--> [nginx] --> [backend: chấm, chốt, công bố, sổ điểm]
                                          |
                                          +--grade.published--> [rabbitmq: platform.events] --> U16
```

**Text alternative**: U11/U14 khi nộp bài và U13 khi chấm code xong gọi port của U15 trong cùng transaction; port tạo job `GRADE_INIT` (queue `jobs.triggered`) hoặc ghi điểm code. Worker chạy job, tự chấm và ghi PostgreSQL. Giảng viên và người học thao tác qua Nginx tới backend để chấm, chốt, công bố và xem sổ điểm; công bố phát event `grade.published` qua RabbitMQ cho U16. Không có container mới.
