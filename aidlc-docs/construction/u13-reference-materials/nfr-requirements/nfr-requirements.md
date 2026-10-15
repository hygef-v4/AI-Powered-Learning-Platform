# U13 Reference Materials — nfr-requirements

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Lấy phần học liệu của U05 cũ; bỏ module, Lesson, tóm tắt, embedding và RAG (phần mở rộng US-CNT-005). Thông báo lớp của U05 cũ thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U13-01 | Danh sách tài liệu p95 ≤ 300 ms; cấp token tải p95 ≤ 200 ms. | NFR-003 |
| NFR-U13-02 | Trích chữ tệp 50 MB xong trong ≤ 2 phút; không chặn việc xem, tải. | BR-U13-30 |
| NFR-U13-03 | `extractText` cho tối đa 10 tài liệu p95 ≤ 1 giây (đọc chữ đã trích sẵn). | BR-U13-31 |
| NFR-U13-10 | Khóa lạc quan `version`; phiên bản mới và lưu trữ là câu lệnh có điều kiện. | BR-U13-12 |
| NFR-U13-11 | Job trích chữ idempotent theo (`materialId`, `versionNo`). | BR-U13-30 |
| NFR-U13-30 | Tệp kiểm loại theo nội dung ở U03; trình trích chữ chạy với giới hạn thời gian và bộ nhớ, không chạy macro. URL YouTube kiểm bằng biểu thức chặt, chỉ lưu id video. | SEC-003 |
| NFR-U13-31 | Không log nội dung tài liệu. | SEC-005 |
| NFR-U13-40 | Người code viết unit test cho mọi `BR-U13-xx`; tester viết integration test, gồm người học lớp khác mở tài liệu, tải bằng token hết hạn, thay phiên bản khi job trích chữ đang chạy, và phân quyền 4 role trên mọi endpoint. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U13-31 |
| SECURITY-05 | Compliant | Kiểm tệp, URL, độ dài |
| SECURITY-08 | Compliant | R2, R3/R4, R5 theo phạm vi; token tải 5 phút |
| SECURITY-15 | Compliant | Fail closed khi U01, U04, U05 lỗi |
| Rule còn lại | N/A | Không có secret, không gọi dịch vụ ngoài |
