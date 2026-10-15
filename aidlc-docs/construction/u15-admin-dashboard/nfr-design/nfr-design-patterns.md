# U15 Admin Dashboard — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U15 Admin Dashboard thuộc Iter 3, người code: Người 5; UC 66. Lấy phần Admin Dashboard của U16 cũ; thêm trạng thái backup của U03. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Ghép nguồn độc lập
- Mỗi nguồn là một lời gọi có timeout; lỗi một nguồn chỉ đánh dấu phần đó `UNAVAILABLE`.

## P2 - Không giả thành công
- Backup chỉ hiện thành công khi U03 trả lần chạy `SUCCESS`; lỗi hoặc không có dữ liệu thì hiện đúng trạng thái.
