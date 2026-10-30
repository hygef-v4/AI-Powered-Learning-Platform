# U08 Assignment Work — deployment-architecture

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser --> nginx --> backend (package assignmentwork) --> postgres (assignment_attempts, practice_feedback, activity_events)
backend --> U07 (bài, khung, rubric, kiểm tài liệu) ; backend --> U06 (task phản hồi AI) ; backend --> Redis (giới hạn tần suất)
worker (tự nộp, dọn dữ liệu hoạt động, nhận phản hồi AI) --> postgres
```

**Text alternative**: Người học dùng trình duyệt qua Nginx tới backend để xem bài, làm, lưu, nộp, xin phản hồi; trình duyệt gửi sự kiện rời trang khi bài bật theo dõi. Backend đọc bài và kiểm tài liệu qua U07, gửi task phản hồi qua U06, giới hạn tần suất bằng Redis, lưu dữ liệu trong PostgreSQL. Worker tự nộp khi hết hạn, dọn dữ liệu hoạt động quá hạn và nhận phản hồi AI.
