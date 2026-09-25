# U15 Grading - Deployment Architecture

```
 [rabbitmq] --u11/u13/u14 events--> [worker: tự chấm] --> [postgres: grades, history]
                                                                 ^
 Trình duyệt --HTTPS--> [nginx] --> [backend: chấm, chốt, công bố, sổ điểm]
                                          |
                                          +--u15.grade.published--> [rabbitmq] --> U16
```

**Text alternative**: Worker nhận event nộp bài và chấm code từ RabbitMQ, tự chấm và ghi PostgreSQL. Giảng viên và người học thao tác qua Nginx tới backend để chấm, chốt, công bố và xem sổ điểm; công bố phát event `u15.grade.published` qua RabbitMQ cho U16. Không có container mới.
