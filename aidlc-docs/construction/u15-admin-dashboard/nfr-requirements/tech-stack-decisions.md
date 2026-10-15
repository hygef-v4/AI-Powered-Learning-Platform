# U15 Admin Dashboard — tech-stack-decisions

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U15 Admin Dashboard thuộc Iter 3, người code: Người 5; UC 66. Lấy phần Admin Dashboard của U16 cũ; thêm trạng thái backup của U03. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | Không có | Số liệu tính khi đọc |
| Gọi nguồn | Gọi song song các port trong cùng backend, mỗi lời gọi có timeout | Một nguồn chậm không làm chậm cả trang |
| Test | JUnit 5, Vitest; integration test do tester viết | Theo NFR-004 |
