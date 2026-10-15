# U10 Quiz Authoring — tech-stack-decisions

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U10 Quiz Authoring thuộc Iter 2, người code: Người 1; UC 46–48. Lấy ngân hàng câu hỏi của U06 cũ, phần Quiz của U08 cũ và cài đặt Quiz của U09 cũ. Ngân hàng chỉ còn câu trắc nghiệm theo UC 46, 47. Làm Quiz thuộc U11. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA; lựa chọn, đáp án, cài đặt, đề xuất dạng `jsonb` | Như các unit khác |
| Nhập tệp | Apache Commons CSV (đã có); Apache POI (đọc XLSX, U07 cũng dùng) | Không thêm thư viện mới |
| Markdown | Hiển thị bằng trình render có làm sạch ở frontend | Chặn script trong câu hỏi |
| Test | JUnit 5, Vitest; Testcontainers cho integration test do tester viết | Theo NFR-004 |
