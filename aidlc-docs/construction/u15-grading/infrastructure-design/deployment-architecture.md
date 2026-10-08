# U15 Grading - Deployment Architecture

**Bản tài liệu 2026-10-08**: UC 34, 35, 36, 37; primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
 [backend: U11/U14 nộp] --port, cùng transaction--> [postgres: evaluations]
 [worker: U13 chấm code xong] --CodeGradedPort--------^
 Trình duyệt --HTTPS--> [nginx] --> [backend: chấm, chốt, công bố, sổ điểm]
                                          |
                                          +--grade.published--> [rabbitmq: platform.events] --> U16
```

**Text alternative**: U11/U14 khi nộp bài và U13 khi chấm code xong gọi port của U15 trong cùng transaction; port ghi thẳng dòng `evaluations` (trắc nghiệm chấm luôn), không có job chấm riêng. Giảng viên và người học thao tác qua Nginx tới backend để chấm, chốt, công bố và xem sổ điểm; công bố phát event `grade.published` qua RabbitMQ cho U16. Không có container mới.
