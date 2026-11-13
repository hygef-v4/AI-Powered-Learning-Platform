# U11 Quiz Practice — nfr-requirements

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U11-01 | Lưu câu trả lời p95 ≤ 200 ms; nộp và chấm Quiz 200 câu p95 ≤ 500 ms. | NFR-003 |
| NFR-U11-02 | Quiz Practice History p95 ≤ 300 ms khi người học có tới 10 lớp. | NFR-003 |
| NFR-U11-03 | Tự nộp chậm nhất 1 phút sau `deadlineAt` cộng 30 giây. | BR-U11-14 |
| NFR-U11-10 | `answerVersion` chống ghi đè; nộp là câu lệnh có điều kiện trạng thái; `onRetired` chạy trong transaction của U10. | BR-U11-13, 14 |
| NFR-U11-11 | Unique: một lượt `IN_PROGRESS` mỗi (Quiz, người học); (Quiz, người học, `attemptNo`). Bắt đầu lượt khóa hàng theo (Quiz, người học). | BR-U11-10 |
| NFR-U11-30 | Đáp án không có trong mọi response trước khi nộp; sau khi nộp chỉ trả theo cài đặt. | BR-U10-16 |
| NFR-U11-31 | Kiểm câu, lựa chọn thuộc đề của lượt; không log câu trả lời. | SEC-003, SEC-005 |
| NFR-U11-40 | Người code viết unit test cho mọi `BR-U11-xx`; tester viết integration test, gồm hết giờ khi đang làm, Quiz bị ngưng khi có lượt đang làm, hai tab lưu cùng lượt, người học xem đáp án trước khi nộp, và phân quyền 4 role trên mọi endpoint. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U11-31 |
| SECURITY-05 | Compliant | Kiểm câu, lựa chọn, sự kiện |
| SECURITY-08 | Compliant | Chủ lượt; giảng viên chính của lớp |
| SECURITY-11 | Compliant | NFR-U11-30 |
| SECURITY-15 | Compliant | Fail closed khi U01, U05, U10 lỗi |
| Rule còn lại | N/A | Không có secret, không gọi dịch vụ ngoài |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
