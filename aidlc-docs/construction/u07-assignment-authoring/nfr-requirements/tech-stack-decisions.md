# U07 Assignment Authoring — tech-stack-decisions

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U07 Assignment Authoring thuộc Iter 2, người code: Người 4; UC 34–36. Lấy phần Assignment của U08 cũ (vòng đời, phát hành), U09 cũ (tài liệu, khung, DOCX) và rubric của U06 cũ. Quiz và ngân hàng thuộc U10. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA; khung và rubric dạng `jsonb` | Khung là cây block, đọc ghi cả khối |
| DOCX | Apache POI (XWPF) để đọc, ghi | Thư viện Java phổ biến cho DOCX |
| Ảnh sơ đồ khi xuất | Apache Batik dựng PNG từ SVG; ghi chunk `tEXt` `mxfile` bằng `javax.imageio` | Không cần trình duyệt phía server |
| XML | Parser của JDK với `disallow-doctype-decl` và tắt entity ngoài | Chặn XXE |
| Trình soạn tài liệu | TipTap (ProseMirror) với node riêng cho sơ đồ và block khóa | Có sẵn heading, danh sách, bảng, ảnh; dùng lại cho U08, U12 |
| Draw.io | Chế độ nhúng của `embed.diagrams.net` (giao tiếp JSON qua `postMessage`) | CSP hiện tại đã cho phép host này |
| Test | JUnit 5; Vitest cho frontend; Testcontainers cho integration test do tester viết | Theo NFR-004 |
