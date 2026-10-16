# U04 Subject Administration — nfr-requirements

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U04 Subject Administration thuộc Iter 1, người code: Người 4; UC 16–19. Tách phần môn học từ U04 cũ (Subject, Class & Enrollment); phần lớp và ghi danh thuộc U05. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U04-01 | `isSubjectManager` là một truy vấn theo khóa chính, p95 ≤ 20 ms; không cache để đổi Chủ nhiệm môn có hiệu lực ngay. | BR-U04-10 |
| NFR-U04-02 | API danh sách và chi tiết môn p95 ≤ 300 ms, trang ≤ 100, chưa tính thời gian của `ISubjectClasses`. | NFR-003 |
| NFR-U04-03 | Gọi `ISubjectClasses` có timeout 1 giây; lỗi chỉ làm phần số lớp hoặc danh sách lớp hiện "không khả dụng", và chặn lưu trữ. | BR-U04-04 |
| NFR-U04-10 | Unique `subjects.code` trong database. | BR-U04-02 |
| NFR-U04-12 | Sửa môn, đổi Chủ nhiệm môn, đổi trạng thái dùng khóa lạc quan `version`. | BR-U04-06 |
| NFR-U04-30 | Mọi endpoint kiểm quyền phía server qua U01. | SEC-002 |
| NFR-U04-32 | Kiểm đầu vào: độ dài, định dạng mã. | SEC-003 |
| NFR-U04-40 | Người code viết unit test cho mọi `BR-U04-xx`; tester viết integration test và test phân quyền 4 role trên mọi endpoint. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-05 | Compliant | NFR-U04-32 |
| SECURITY-08 | Compliant | NFR-U04-30 |
| SECURITY-15 | Compliant | Fail closed khi U01 hoặc U05 lỗi |
| Rule còn lại | N/A | Không có secret, không gọi dịch vụ ngoài |
