# U15 Admin Dashboard — deployment-architecture

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U15 Admin Dashboard thuộc Iter 3, người code: Người 5; UC 66. Lấy phần Admin Dashboard của U16 cũ; thêm trạng thái backup của U03. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser (Admin) --> nginx --> backend (package admindashboard) --> port của U01, U03, U04, U05 --> postgres
```

**Text alternative**: Admin dùng trình duyệt qua Nginx tới backend; U15 gọi port của U01, U03, U04, U05 trong cùng backend để đếm số liệu và đọc trạng thái backup, không có bảng riêng.
