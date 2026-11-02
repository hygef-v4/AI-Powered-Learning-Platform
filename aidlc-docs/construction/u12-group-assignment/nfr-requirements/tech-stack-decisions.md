# U12 Group Assignment — tech-stack-decisions

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA; nội dung mục, bản nộp dạng `jsonb`; mục và lịch sử ở bảng riêng | Lưu nháp từng mục không tranh khóa cả tài liệu |
| Realtime | Server-Sent Events (Spring `SseEmitter`), phát giữa các instance qua RabbitMQ fanout `platform.realtime` | Chỉ cần đẩy một chiều; code `ISseHub` đã có khung |
| Trình soạn | `DocumentEditor`, `DocumentViewer` của U07 | Dùng chung |
| Test | JUnit 5, Vitest; Testcontainers (PostgreSQL, RabbitMQ) cho integration test do tester viết | Theo NFR-004 |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
