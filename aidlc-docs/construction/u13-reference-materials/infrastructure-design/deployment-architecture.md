# U13 Reference Materials — deployment-architecture

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Lấy phần học liệu của U05 cũ; bỏ module, Lesson, tóm tắt, embedding và RAG (phần mở rộng US-CNT-005). Thông báo lớp của U05 cũ thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser --> nginx --> backend (package materials) --> postgres (materials, material_versions)
backend --> U03 (lưu tệp MATERIAL, token tải, job trích chữ) --> Google Drive
browser --> youtube-nocookie.com (iframe video)
worker (trích chữ) --> U03 (mở tệp) ; worker --> postgres
```

**Text alternative**: Người dùng dùng trình duyệt qua Nginx tới backend để quản lý và xem tài liệu. Backend lưu thông tin tài liệu trong PostgreSQL, lưu tệp và cấp token tải qua U03 (Google Drive), gửi job trích chữ. Trình duyệt mở video YouTube qua iframe `youtube-nocookie.com`. Worker đọc tệp qua U03 để trích chữ cho AI.
