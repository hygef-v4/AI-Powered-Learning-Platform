# U04 Subject Administration — nfr-requirements

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U04-01 | `isSubjectManager` là một truy vấn theo khóa chính, p95 ≤ 20 ms; không cache để đổi Chủ nhiệm môn có hiệu lực ngay. | BR-U04-10 |
| NFR-U04-02 | API danh sách và chi tiết môn p95 ≤ 300 ms, trang ≤ 100, chưa tính thời gian của `ISubjectClasses`. | NFR-003 |
| NFR-U04-03 | Gọi `ISubjectClasses` có timeout 1 giây; lỗi làm ẩn số lớp ở tab chi tiết, danh sách lớp báo "không khả dụng" và chặn lưu trữ; không biến lỗi thành số lớp 0. | BR-U04-04 |
| NFR-U04-10 | Unique `subjects.code` trong database. | BR-U04-02 |
| NFR-U04-12 | Sửa môn, đổi Chủ nhiệm môn, đổi trạng thái dùng khóa lạc quan `version`. | BR-U04-06 |
| NFR-U04-30 | Mọi endpoint kiểm quyền phía server qua U01. | SEC-002 |
| NFR-U04-32 | Kiểm đầu vào: độ dài, định dạng mã. | SEC-003 |
| NFR-U04-40 | Người code viết unit test cho mọi `BR-U04-xx`; tester viết integration test và test phân quyền 4 role trên mọi endpoint. | NFR-004 |

**Yêu cầu đã chốt 2026-11-08:** Áp dụng các [quyết định U04/U13/U14](../../../inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md). Đây là baseline yêu cầu để thiết kế test; không phải xác nhận code/runtime hoặc coverage đã đầy đủ. Các snapshot và số kiểm thử có ngày cũ là lịch sử.

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-05 | Compliant | NFR-U04-32 |
| SECURITY-08 | Compliant | NFR-U04-30 |
| SECURITY-15 | Compliant | Fail closed khi U01 hoặc U05 lỗi |
| Rule còn lại | N/A | Không có secret, không gọi dịch vụ ngoài |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
