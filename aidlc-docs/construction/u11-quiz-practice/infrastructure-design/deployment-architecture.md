# U11 Quiz Practice — deployment-architecture

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U11 Quiz Practice thuộc Iter 3, người code: Người 1; UC 49–52. Lấy phần lượt Quiz của U11 cũ và chấm Quiz luyện tập của U15 cũ. Quiz chỉ có chế độ luyện tập (D01). Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser --> nginx --> backend (package quizpractice) --> postgres (quiz_attempts)
backend --> U10 (đề, chấm, đáp án) ; backend --> U08 (dữ liệu hoạt động)
worker (tự nộp khi hết giờ) --> postgres
```

**Text alternative**: Người học dùng trình duyệt qua Nginx tới backend để xem Quiz, làm, nộp và xem kết quả; giảng viên xem lượt của lớp mình. Backend lấy đề và chấm qua U10, ghi và đọc dữ liệu rời trang qua U08, lưu lượt trong PostgreSQL. Worker tự nộp lượt hết giờ.
