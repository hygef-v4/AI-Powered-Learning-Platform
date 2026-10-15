# U15 Admin Dashboard — nfr-requirements

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U15 Admin Dashboard thuộc Iter 3, người code: Người 5; UC 66. Lấy phần Admin Dashboard của U16 cũ; thêm trạng thái backup của U03. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U15-01 | `GET /api/v1/admin/dashboard` p95 ≤ 1 giây; mỗi nguồn timeout 2 giây, gọi song song. | NFR-003 |
| NFR-U15-02 | Không cache số liệu. | BR-U15-11 |
| NFR-U15-30 | Chỉ `ADMIN`; response chỉ có số đếm và metadata backup đã làm sạch. | SEC-002, SEC-005 |
| NFR-U15-40 | Người code viết unit test cho mọi `BR-U15-xx`; tester viết integration test gồm vai trò khác gọi API, một nguồn lỗi mà các thẻ khác vẫn hiện, U03 lỗi không hiện thành công. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-08 | Compliant | Chỉ `ADMIN`, kiểm phía server |
| SECURITY-03 | Compliant | Không có dữ liệu cá nhân trong response, log |
| SECURITY-15 | Compliant | Nguồn lỗi hiện "không khả dụng" |
| Rule còn lại | N/A | Không có dữ liệu riêng, không gọi dịch vụ ngoài |
