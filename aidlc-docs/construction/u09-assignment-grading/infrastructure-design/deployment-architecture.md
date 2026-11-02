# U09 Assignment Grading — deployment-architecture

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser --> nginx --> backend (package grading) --> postgres (evaluations, grading_proposals)
backend --> U07, U08 (bài, rubric, bài nộp, hoạt động) ; backend --> U06 (task đề xuất) ; backend --> RabbitMQ (grade.published)
worker (handler đề xuất AI) --> postgres
```

**Text alternative**: Giảng viên dùng trình duyệt qua Nginx tới backend để xem bài nộp, chấm, chốt, công bố và xuất sổ điểm; người học xem điểm của mình. Backend đọc bài, rubric, bài nộp, dữ liệu hoạt động qua U07, U08, gửi task đề xuất qua U06, lưu đánh giá trong PostgreSQL và phát sự kiện công bố điểm lên RabbitMQ cho U14. Worker nhận kết quả đề xuất AI.
