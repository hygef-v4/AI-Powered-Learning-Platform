# U06 AI Connections — nfr-requirements

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U06 AI Connections thuộc Iter 2, người code: Người 5; UC 31–33 và phần API chung, quota của UC 14–15 (màn do U03). Gộp U07 cũ (AI Usage & Teacher Quota) và U13 cũ (AI Connections & MCP); bỏ mua credit và chạy code. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U06-01 | Giữ chỗ quota p95 ≤ 50 ms; khóa dòng cửa sổ theo thứ tự cố định (ngày toàn hệ thống rồi ngày người gọi) để tránh deadlock. | BR-U06-13 |
| NFR-U06-02 | API danh sách và usage p95 ≤ 300 ms, trang ≤ 100. | NFR-003 |
| NFR-U06-03 | Gọi provider timeout 120 giây; kiểm tra kết nối timeout 20 giây. Mỗi provider của API chung tối đa 2 lời gọi đồng thời trong worker. | BR-U06-25, U03 P11 |
| NFR-U06-04 | Phần xử lý của U06 cho một lời gọi tool MCP p95 ≤ 100 ms, chưa tính unit sở hữu tool. | FR-035 |
| NFR-U06-10 | Quota lưu trong PostgreSQL, giữ chỗ và quyết toán nguyên tử; Redis không là nguồn quota. | FR-034 |
| NFR-U06-11 | `requestRef` duy nhất trong database; message đến lại không gọi provider lần hai. | BR-U06-21, 22 |
| NFR-U06-30 | Key mã hóa AES-256-GCM bằng khóa từ biến môi trường, có `keyVersion` để đổi khóa; không log key, token, prompt, kết quả AI. | SEC-005, SEC-006 |
| NFR-U06-31 | Token MCP 256 bit ngẫu nhiên, lưu SHA-256, so sánh thời gian hằng. | FR-035 |
| NFR-U06-32 | Chỉ gọi HTTPS tới địa chỉ provider cố định trong code. | FR-012 |
| NFR-U06-33 | Kết quả AI và tham số, kết quả tool kiểm JSON schema và kích thước; frontend không render HTML thô từ AI. | FR-035 |
| NFR-U06-34 | API dùng cookie có chống CSRF; `/mcp` chỉ nhận bearer token, không nhận cookie. | SEC-002 |
| NFR-U06-40 | Người code viết unit test cho mọi `BR-U06-xx`, adapter provider test bằng HTTP server giả; tester viết integration test, gồm giữ chỗ đồng thời không vượt mức, worker nhận lại message không gọi provider hai lần, credential MCP thu hồi bị từ chối ngay, và test phân quyền 4 role trên mọi endpoint. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-01 | Compliant | Key mã hóa khi lưu; gọi provider qua HTTPS |
| SECURITY-03 | Compliant | NFR-U06-30 |
| SECURITY-05 | Compliant | Kiểm key, model, schema tham số tool và kết quả AI |
| SECURITY-08 | Compliant | Chủ sở hữu hoặc Admin; tool kiểm quyền qua `IAuthorization` |
| SECURITY-09 | Compliant | Khóa mã hóa từ biến môi trường, không giá trị mặc định |
| SECURITY-10 | Compliant | Khóa phiên bản MCP SDK và thư viện kiểm JSON schema |
| SECURITY-11 | Compliant | Không nhận URL người dùng; gửi AI ngữ cảnh tối thiểu |
| SECURITY-12 | Compliant | Token MCP băm, có hạn, thu hồi được; tài khoản không `ACTIVE` thì bị từ chối |
| SECURITY-15 | Compliant | Fail closed khi U01, U03 lỗi; không gọi lại provider khi không rõ kết quả |
| Rule còn lại | N/A | Hạ tầng mạng, header, giám sát thuộc U03 và hạ tầng chung |
