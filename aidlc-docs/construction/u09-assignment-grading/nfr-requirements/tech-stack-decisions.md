# U09 Assignment Grading — tech-stack-decisions

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U09 Assignment Grading thuộc Iter 2, người code: Người 2; UC 42–45. Lấy phần chấm Assignment và sổ điểm của U15 cũ và phần tiến độ nộp, phân bố điểm, xuất bảng điểm của U16 cũ. Kết quả Quiz luyện tập thuộc U11, thông báo thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA; checklist, lịch sử, đề xuất dạng `jsonb` | Như các unit khác |
| Xuất CSV | Apache Commons CSV (đã có trong `pom.xml`) | Escape chuẩn |
| Xuất XLSX | Apache POI SXSSF | Ghi dạng luồng, ít bộ nhớ |
| Hiển thị bài nộp, dòng thời gian | `DocumentViewer` của U07, `ActivityTimeline` của U08 | Dùng chung |
| Test | JUnit 5, Vitest; Testcontainers cho integration test do tester viết | Theo NFR-004 |
