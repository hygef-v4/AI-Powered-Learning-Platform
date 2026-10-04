# U15 Grading - Deployment Architecture

```
 [backend: U11/U14 nộp] --port, cùng transaction--> [postgres: evaluations]
 [worker: U13 chấm code xong] --CodeGradedPort--------^
 Trình duyệt --HTTPS--> [nginx] --> [backend: chấm, chốt, công bố, sổ điểm]
                                          |
                                          +--grade.published--> [rabbitmq: platform.events] --> U16
```

**Text alternative**: U11/U14 khi nộp bài và U13 khi chấm code xong gọi port của U15 trong cùng transaction; port ghi thẳng dòng `evaluations` (trắc nghiệm chấm luôn), không có job chấm riêng. Giảng viên và người học thao tác qua Nginx tới backend để chấm, chốt, công bố và xem sổ điểm; công bố phát event `grade.published` qua RabbitMQ cho U16. Không có container mới.
