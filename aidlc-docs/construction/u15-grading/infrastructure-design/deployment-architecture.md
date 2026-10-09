# U15 Grading - Deployment Architecture

**Bản tài liệu 2026-10-09**: UC 37, 38, 39, 40 (chốt và công bố là luồng phụ của UC 38, 39) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
+----------------------------------------------------------+
| Trình duyệt: giảng viên lớp, Student                     |
+----------------------------------------------------------+
                              |
                              |  HTTPS qua nginx
                              v
+----------------------------------------------------------+
| backend: Submission Detail, chấm, chốt, công bố, sổ điểm |
| port U15 trong transaction nộp của U11, U14              |
| worker: U13 gọi CodeGradedPort, PracticeResultPort       |
| grade.published --> rabbitmq platform.events --> U16     |
+----------------------------------------------------------+
                              |
                              |  ghi evaluations
                              v
+----------------------------------------------------------+
| postgres: evaluations                                    |
+----------------------------------------------------------+
```

**Text alternative**: Giảng viên lớp và Student dùng trình duyệt, đi qua Nginx tới backend. Backend phục vụ Submission Detail, Grading Workspace, chốt, công bố và sổ điểm; khi U11/U14 nộp bài, port của U15 chạy trong cùng transaction nộp; worker của U13 gọi `CodeGradedPort` khi chấm Code Lab xong và `PracticeResultPort` khi có kết quả Practice. Mọi điểm ghi vào bảng `evaluations` trong PostgreSQL; thời điểm công bố theo (bài, lớp) suy ra từ `published_at` của các đánh giá, không có bảng riêng. Công bố phát event `grade.published` qua RabbitMQ (exchange `platform.events`) cho U16. Không có container mới.
