# U13 Reference Materials — tech-stack-decisions

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA; tệp trên Google Drive qua U03 | Như các unit khác |
| Trích chữ | Apache Tika (bộ parser chuẩn: PDFBox, POI), không OCR | Một thư viện cho cả PDF, DOCX, PPTX; `tika-core` đã có |
| Xem PDF | Trình xem PDF của trình duyệt qua link token | Không thêm thư viện |
| Video | Iframe `youtube-nocookie.com` (CSP đã cho phép) | Không gọi API YouTube |
| Test | JUnit 5, Vitest; Testcontainers cho integration test do tester viết | Theo NFR-004 |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
