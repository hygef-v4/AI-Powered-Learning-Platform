# U10 Quiz Authoring — nfr-requirements

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U10-01 | Question List, Quiz List p95 ≤ 300 ms, trang ≤ 100. | NFR-003 |
| NFR-U10-02 | Nhập 500 dòng xong ≤ 10 giây. | BR-U10-07 |
| NFR-U10-03 | `score` cho Quiz 200 câu p95 ≤ 100 ms. | BR-U10-16 |
| NFR-U10-10 | Khóa lạc quan `version` cho câu và Quiz; phát hành, ngưng là câu lệnh có điều kiện trạng thái; listener chạy trong cùng transaction. | BR-U10-22, 23 |
| NFR-U10-11 | Unique: (`lineageId`, `versionNo`); một bản `DRAFT` mỗi lineage ngân hàng; một version đang mở mỗi Quiz nguồn; (`quizId`, `questionId`). | BR-U10-04, 30 |
| NFR-U10-30 | Markdown làm sạch khi hiển thị; tệp nhập kiểm loại theo nội dung, đọc trong bộ nhớ, giá trị bắt đầu bằng `=`, `+`, `-`, `@` coi là văn bản. | SEC-003 |
| NFR-U10-31 | Đáp án đúng không có trong mọi API, DTO cho người học trước khi nộp. | BR-U10-16 |
| NFR-U10-32 | Không log nội dung câu, đáp án, prompt. | SEC-005 |
| NFR-U10-40 | Người code viết unit test cho mọi `BR-U10-xx`; tester viết integration test, gồm phát hành version mới khi có lượt đang làm, hai người sửa cùng câu, nhập tệp có dòng lỗi, người học gọi API Quiz không thấy đáp án, và phân quyền 4 role trên mọi endpoint. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U10-32 |
| SECURITY-05 | Compliant | Kiểm câu, cài đặt, tệp nhập theo quy tắc |
| SECURITY-08 | Compliant | R2 với ngân hàng và Quiz của môn; R3/R4 với Quiz của lớp |
| SECURITY-11 | Compliant | NFR-U10-31; đáp án chỉ ở `IQuizScoring` |
| SECURITY-13 | Compliant | Phiên bản câu bất biến khi đã kích hoạt; Quiz ghim phiên bản |
| SECURITY-15 | Compliant | Fail closed khi U01, U04, U05 lỗi |
| Rule còn lại | N/A | Không có secret, không gọi dịch vụ ngoài trực tiếp |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
