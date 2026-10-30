# U07 Assignment Authoring — deployment-architecture

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser --> nginx --> backend (package assignments) --> postgres (assignments, assignment_rubrics, assignment_ai_drafts)
browser --> embed.diagrams.net (iframe Draw.io)
backend --> U03 (ảnh DOCUMENT_IMAGE, sự kiện assignment.opened) ; backend --> U06 (task AI)
worker (scanner mở, đóng bài; handler kết quả AI) --> postgres
```

**Text alternative**: Giảng viên dùng trình duyệt qua Nginx tới backend để soạn đề, rubric, phát hành; trình duyệt nhúng Draw.io từ `embed.diagrams.net` để vẽ sơ đồ. Backend lưu bài, rubric, đề xuất AI trong PostgreSQL, lưu ảnh và phát sự kiện qua U03, gửi task AI qua U06. Worker chạy scanner mở, đóng bài theo lịch và nhận kết quả AI.
