# U12 Group Assignment — tech-stack-decisions

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Gộp U12 cũ (nhóm và trưởng nhóm) và U14 cũ (tài liệu nhóm và nộp). Soạn đề, rubric dùng U07; chấm dùng U09. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA; nội dung mục, bản nộp dạng `jsonb`; mục và lịch sử ở bảng riêng | Lưu nháp từng mục không tranh khóa cả tài liệu |
| Realtime | Server-Sent Events (Spring `SseEmitter`), phát giữa các instance qua RabbitMQ fanout `platform.realtime` | Chỉ cần đẩy một chiều; code `SseHub` đã có khung |
| Trình soạn | `DocumentEditor`, `DocumentViewer` của U07 | Dùng chung |
| Test | JUnit 5, Vitest; Testcontainers (PostgreSQL, RabbitMQ) cho integration test do tester viết | Theo NFR-004 |
