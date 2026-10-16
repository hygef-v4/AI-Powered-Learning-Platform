# U12 Group Assignment — nfr-requirements

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Gộp U12 cũ (nhóm và trưởng nhóm) và U14 cũ (tài liệu nhóm và nộp). Soạn đề, rubric dùng U07; chấm dùng U09. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-01 | Sự kiện realtime tới người đang mở tài liệu ≤ 2 giây. | BR-U12-37 |
| NFR-U12-02 | Lưu nháp mục p95 ≤ 500 ms; nhận, giao, nhả, Xong p95 ≤ 300 ms. | NFR-003 |
| NFR-U12-03 | Lưu bộ nhóm 200 sinh viên p95 ≤ 1 giây; tạo tài liệu cho 50 nhóm trong ≤ 30 giây sau khi bài mở. | NFR-003 |
| NFR-U12-04 | Tự nộp chậm nhất 1 phút sau hạn cuối cộng 30 giây. | BR-U12-43 |
| NFR-U12-10 | Thao tác trên mục khóa dòng mục và kiểm `version` của mục; nộp khóa dòng tài liệu; mỗi mục một người sửa. | BR-U12-33 |
| NFR-U12-11 | Unique: (`classId`, `accountId`) với thành viên; (`groupId`, `assignmentId`) với tài liệu; một yêu cầu `PENDING` mỗi nhóm. Job tạo tài liệu idempotent. | BR-U12-02, 30 |
| NFR-U12-12 | `onGroupSubmitted` của U09 chạy trong transaction nộp; sự kiện và SSE phát sau commit. | BR-U03-64 |
| NFR-U12-30 | Tài liệu kiểm qua `IDocumentModel` của U07; kênh SSE chỉ cho thành viên hiệu lực và giảng viên của lớp, kiểm khi mở kênh và khi đổi thành viên. | SEC-002, SEC-003 |
| NFR-U12-31 | Không log nội dung tài liệu, lý do yêu cầu. | SEC-005 |
| NFR-U12-40 | Người code viết unit test cho mọi `BR-U12-xx`; tester viết integration test, gồm hai người nhận cùng một mục, trưởng nhóm giao mục đang có người giữ, tự nộp khi có mục đang làm, đổi thành viên khi bài đang mở, SSE trên hai instance, và phân quyền 4 role trên mọi endpoint. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U12-31 |
| SECURITY-05 | Compliant | Kiểm nhóm, lý do, tài liệu |
| SECURITY-08 | Compliant | Giảng viên chính của lớp; thành viên hiệu lực; trưởng nhóm cho giao và nộp |
| SECURITY-13 | Compliant | Bản nộp bất biến có `receiptHash`; lịch sử mục chỉ thêm |
| SECURITY-15 | Compliant | Fail closed khi U01, U05, U07 lỗi |
| Rule còn lại | N/A | Không có secret, không gọi dịch vụ ngoài |
