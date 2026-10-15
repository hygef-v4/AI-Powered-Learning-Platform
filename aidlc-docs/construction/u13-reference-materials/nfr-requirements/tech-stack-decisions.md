# U13 Reference Materials — tech-stack-decisions

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Lấy phần học liệu của U05 cũ; bỏ module, Lesson, tóm tắt, embedding và RAG (phần mở rộng US-CNT-005). Thông báo lớp của U05 cũ thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA; tệp trên Google Drive qua U03 | Như các unit khác |
| Trích chữ | Apache Tika (bộ parser chuẩn: PDFBox, POI), không OCR | Một thư viện cho cả PDF, DOCX, PPTX; `tika-core` đã có |
| Xem PDF | Trình xem PDF của trình duyệt qua link token | Không thêm thư viện |
| Video | Iframe `youtube-nocookie.com` (CSP đã cho phép) | Không gọi API YouTube |
| Test | JUnit 5, Vitest; Testcontainers cho integration test do tester viết | Theo NFR-004 |
