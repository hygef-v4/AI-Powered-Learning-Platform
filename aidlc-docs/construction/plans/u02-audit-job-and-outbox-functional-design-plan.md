# U02 Audit, Job & Event - Functional Design Plan

## 1. Phạm vi

- Unit U02 trong kế hoạch 16 unit. Story `US-AUD-001`; use case `UC-OPS-02`.
- Sở hữu: audit append-only, bảng `jobs` và vòng đời job, publish sự kiện sang RabbitMQ, retry/dead-letter, correlation.
- Không sở hữu: nội dung nghiệp vụ của job (thuộc unit gọi), gửi thông báo (U16), phân quyền (U01).
- Thay đổi quy trình: thiết kế U02 trước khi code U01 theo yêu cầu người dùng; plan code U01 tạm dừng.

## 2. Việc cần làm

- [x] Đọc unit-of-work, story map, US-AUD-001, UC-OPS-02, component-methods, services, ERD `audit_events`.
- [x] Hỏi người dùng qua giao diện, ghi vào `u02-audit-job-and-outbox-functional-design-questions.md`.
- [x] Tạo `business-logic-model.md`, `business-rules.md`, `domain-entities.md`, `frontend-components.md`.
- [x] Đồng bộ tài liệu U01: `OutboxPort` thành `JobPort`, không còn bảng outbox.
- [x] Ghi compliance theo phạm vi rút gọn.
- [x] Trình duyệt Functional Design U02.

## 3. Cần đồng bộ ERD sau

- Thêm bảng `jobs` (ERD §3.1 đang ghi không có bảng này).
- Không có bảng `outbox`.
