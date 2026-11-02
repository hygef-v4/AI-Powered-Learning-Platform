# U05 Class & Enrollment — nfr-requirements

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U05 Class & Enrollment thuộc Iter 1, người code: Người 5; UC 20–30. Tách phần lớp và ghi danh từ U04 cũ; mã `BR-U05-xx` ứng với `BR-U04-xx` cũ cùng số. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U05-01 | Kiểm phạm vi (`IClassScope`, `IClassAccess`) là một truy vấn theo index, p95 ≤ 20 ms; không cache để đổi giảng viên và gỡ ghi danh có hiệu lực ngay. | BR-U05-12, 25 |
| NFR-U05-02 | API danh sách lớp và ghi danh p95 ≤ 300 ms, trang ≤ 100. | NFR-003 |
| NFR-U05-03 | Thêm theo danh sách 200 email xử lý đồng bộ ≤ 5 giây, tra U01 theo lô. | BR-U05-23 |
| NFR-U05-04 | Trang lớp của sinh viên p95 ≤ 300 ms phần của U05, chưa tính các phần do unit khác gắn vào. | NFR-003 |
| NFR-U05-10 | Unique trong database: `(subject_id, code)` của lớp; khóa chính `(class_id, account_id)` của ghi danh. | BR-U05-11, 21 |
| NFR-U05-11 | BR-U05-22 kiểm trong transaction có khóa hàng theo (sinh viên, môn) để hai yêu cầu đồng thời không cùng qua. | BR-U05-22 |
| NFR-U05-12 | Sửa lớp, đổi giảng viên, đổi trạng thái dùng khóa lạc quan `version`. | BR-U05-14 |
| NFR-U05-13 | Sự kiện `enrollment.activated` gửi sau commit qua `IEventPublisher`. | BR-U05-26 |
| NFR-U05-30 | Mọi endpoint kiểm quyền phía server; ngoài phạm vi trả `404`. | SEC-002 |
| NFR-U05-32 | Kiểm đầu vào: độ dài, định dạng mã lớp, email, CSV ≤ 200 dòng và ≤ 100 KB. | SEC-003 |
| NFR-U05-33 | Không log email sinh viên trong log ứng dụng; audit ghi `accountId`. | SEC-005 |
| NFR-U05-40 | Người code viết unit test cho mọi `BR-U05-xx`; tester viết integration test, gồm hai yêu cầu ghi danh đồng thời vào hai lớp cùng môn chỉ một thành công, và test phân quyền 4 role trên mọi endpoint (kể cả dùng ID lớp của người khác). | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U05-33 |
| SECURITY-05 | Compliant | NFR-U05-32 |
| SECURITY-08 | Compliant | NFR-U05-30 |
| SECURITY-15 | Compliant | Fail closed khi U01 hoặc U04 lỗi |
| Rule còn lại | N/A | Không có secret, không gọi dịch vụ ngoài |
