# U08 Assignment Work — tech-stack-decisions

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U08 Assignment Work thuộc Iter 2, người code: Người 3; UC 37–41. Lấy phần Assignment cá nhân, phản hồi Practice và dữ liệu hoạt động của U11 cũ. Lượt Quiz thuộc U11 mới, bài nhóm thuộc U12. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA; nội dung lượt dạng `jsonb` | Cùng mô hình tài liệu của U07 |
| Nén | Gzip cho body lưu nháp | Tài liệu có sơ đồ khá lớn |
| Giới hạn tần suất | Bucket4j + Redis (đã có trong `pom.xml`) | Dùng chung cho nhiều instance |
| Theo dõi trang | Page Visibility API (`visibilitychange`), không dùng `blur` | Không tính iframe Draw.io là rời trang |
| Trình soạn | `DocumentEditor` của U07 | Dùng chung |
| Test | JUnit 5, Vitest; Testcontainers cho integration test do tester viết | Theo NFR-004 |
