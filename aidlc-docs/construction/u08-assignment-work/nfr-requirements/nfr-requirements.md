# U08 Assignment Work — nfr-requirements

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U08 Assignment Work thuộc Iter 2, người code: Người 3; UC 37–41. Lấy phần Assignment cá nhân, phản hồi Practice và dữ liệu hoạt động của U11 cũ. Lượt Quiz thuộc U11 mới, bài nhóm thuộc U12. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U08-01 | Tự lưu tài liệu ≤ 2.000 block p95 ≤ 500 ms; body gửi nén gzip. | FR-018 |
| NFR-U08-02 | Student Assignments p95 ≤ 300 ms khi người học có tới 10 lớp. | NFR-003 |
| NFR-U08-03 | Nộp bài p95 ≤ 1 giây. | FR-018 |
| NFR-U08-04 | Tự nộp chậm nhất 1 phút sau `deadlineAt` cộng 30 giây. | BR-U08-23 |
| NFR-U08-05 | Ghi một lô sự kiện hoạt động p95 ≤ 100 ms. | FR-039 |
| NFR-U08-10 | `contentVersion` chống ghi đè; nộp và tự nộp là câu lệnh có điều kiện trạng thái; `ISubmissionListener` chạy trong cùng transaction. | FR-018 |
| NFR-U08-11 | Unique: một lượt `IN_PROGRESS` mỗi (bài, người học); (bài, người học, `attemptNo`); sự kiện theo `clientEventId`. Bắt đầu lượt khóa hàng theo (bài, người học). | BR-U08-11 |
| NFR-U08-12 | Giới hạn tần suất sự kiện hoạt động và yêu cầu phản hồi bằng Bucket4j trên Redis. | BR-U08-44, 52 |
| NFR-U08-30 | Tài liệu kiểm qua `IDocumentModel` của U07 (XML an toàn, SVG sạch, trần kích thước); DOCX đọc trong bộ nhớ như U07. | SEC-003 |
| NFR-U08-31 | Không log nội dung bài làm, phản hồi AI, prompt. | SEC-005 |
| NFR-U08-40 | Người code viết unit test cho mọi `BR-U08-xx`; tester viết integration test, gồm hai tab lưu cùng lượt, nộp hai lần, tự nộp khi hết hạn và khi ngưng giao, phản hồi với hash cũ, sự kiện trùng và lệch thứ tự, và phân quyền 4 role trên mọi endpoint. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U08-31 |
| SECURITY-05 | Compliant | Kiểm tài liệu, DOCX, sự kiện, phản hồi theo schema |
| SECURITY-08 | Compliant | Chủ lượt hoặc giảng viên của lớp; ngoài phạm vi trả `404` |
| SECURITY-13 | Compliant | `receiptHash` cho bài nộp; phản hồi gắn hash |
| SECURITY-15 | Compliant | Fail closed khi U01, U05, U07 lỗi |
| Rule còn lại | N/A | Không có secret, không gọi dịch vụ ngoài trực tiếp |
