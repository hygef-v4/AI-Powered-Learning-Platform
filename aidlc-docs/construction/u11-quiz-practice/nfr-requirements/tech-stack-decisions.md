# U11 Quiz Practice — tech-stack-decisions

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U11 Quiz Practice thuộc Iter 3, người code: Người 1; UC 49–52. Lấy phần lượt Quiz của U11 cũ và chấm Quiz luyện tập của U15 cũ. Quiz chỉ có chế độ luyện tập (D01). Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA; câu trả lời, ảnh chụp, kết quả dạng `jsonb` | Như các unit khác |
| Trộn câu, đáp án | Seed lưu trên lượt, trộn bằng thuật toán Fisher–Yates có seed | Mở lại lượt vẫn đúng thứ tự |
| Theo dõi hoạt động | `useActivityTracker`, `ActivityTimeline` của U08 | Dùng chung |
| Test | JUnit 5, Vitest; Testcontainers cho integration test do tester viết | Theo NFR-004 |
