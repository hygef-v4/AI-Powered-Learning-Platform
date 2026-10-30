# U10 Quiz Authoring — deployment-architecture

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser --> nginx --> backend (package quizzes) --> postgres (questions, quizzes, quiz_items, quiz_ai_drafts)
backend --> U06 (task AI soạn câu, tài liệu tham khảo) ; backend --> RabbitMQ (quiz.published)
worker (handler đề xuất AI) --> postgres
```

**Text alternative**: Chủ nhiệm môn và giảng viên dùng trình duyệt qua Nginx tới backend để quản lý ngân hàng câu hỏi và soạn, phát hành Quiz. Backend lưu dữ liệu trong PostgreSQL, gửi task AI soạn câu qua U06 và phát sự kiện Quiz mới lên RabbitMQ cho U14. Worker nhận kết quả đề xuất AI.
