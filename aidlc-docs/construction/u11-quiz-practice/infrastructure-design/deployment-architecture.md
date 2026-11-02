# U11 Quiz Practice — deployment-architecture

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser --> nginx --> backend (package quizpractice) --> postgres (quiz_attempts)
backend --> U10 (đề, chấm, đáp án) ; backend --> U08 (dữ liệu hoạt động)
worker (tự nộp khi hết giờ) --> postgres
```

**Text alternative**: Người học dùng trình duyệt qua Nginx tới backend để xem Quiz, làm, nộp và xem kết quả; giảng viên xem lượt của lớp mình. Backend lấy đề và chấm qua U10, ghi và đọc dữ liệu rời trang qua U08, lưu lượt trong PostgreSQL. Worker tự nộp lượt hết giờ.
