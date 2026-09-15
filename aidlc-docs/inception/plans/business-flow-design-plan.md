# Main Business Flow Design Plan

## Mục tiêu

Tạo một tài liệu `aidlc-docs/inception/application-design/business-flows.md` theo mẫu cung cấp. Mỗi business flow có mã BF, Trigger, End condition, swimlane diagram và text alternative.

## Danh sách business flow đề xuất

| ID | Business flow | Swimlanes chính |
|---|---|---|
| BF-01 | Đăng nhập và quản lý phiên | Người dùng, Backend, Redis |
| BF-02 | Thiết lập môn học, lớp và phân công | Quản trị viên, Backend, PostgreSQL |
| BF-03 | Upload tài liệu, AI tóm tắt và chia thành bài học | Giảng viên/Chủ nhiệm môn, Backend, Google Drive, RabbitMQ/Worker |
| BF-04 | Soạn, duyệt và phát hành assignment | Giảng viên/Chủ nhiệm môn, Backend, AI Worker, Người học |
| BF-05 | Người học truy cập bài học và ghi nhận tiến độ | Người học, Backend, Google Drive, PostgreSQL |
| BF-06 | Tổ chức nhóm, phân chia phần việc và đổi leader | Giảng viên, Sinh viên, Backend |
| BF-07 | Làm và nộp bài cá nhân/bài chung | Sinh viên/Leader, Backend, Google Drive, PostgreSQL |
| BF-08 | Chọn phương pháp chấm, AI/chấm tay và công bố điểm | Giảng viên, Backend, RabbitMQ/AI Worker, Sinh viên |
| BF-09 | Thanh toán và cấp quyền truy cập | Sinh viên, Backend, Cổng thanh toán, PostgreSQL |

## Quy ước biểu diễn

- Dùng Mermaid `flowchart LR` và `subgraph` để mô phỏng swimlane trong Markdown.
- Actor khởi tạo nằm ở lane đầu; hệ thống và dịch vụ ngoài nằm ở các lane sau.
- Decision dùng node hình thoi; nhánh Yes/No có nhãn.
- Không mô tả chi tiết endpoint hoặc implementation nội bộ không ảnh hưởng business outcome.
- Mỗi sơ đồ có text alternative để vẫn đọc được khi công cụ không render Mermaid.
- `SUBJECT_MANAGER` được phép thực hiện chức năng giảng viên nhưng vẫn bị giới hạn bởi môn/lớp được phân công.
- File nằm trên Google Drive; Redis giữ session/OTP; RabbitMQ giữ job tạm; kết quả nghiệp vụ nằm trong PostgreSQL.

## Question 1 - Phê duyệt phạm vi business flow

Danh sách 9 business flow và cách biểu diễn trên có phù hợp để tạo tài liệu chính thức không?

A) Approve - tạo đủ 9 business flow như kế hoạch (khuyến nghị)

B) Request Changes - điều chỉnh danh sách hoặc cách biểu diễn trước khi tạo

X) Other (mô tả sau thẻ [Answer]:)

[Answer]: A - Approved in chat on 2026-09-14

## Post-generation correction - Horizontal Pool 1

- [x] Inspect the diagrams.net “Horizontal Pool 1” template and record its pool/lane XML structure.
- [x] Replace rectangle-based lane backgrounds and separate labels in all nine standalone Draw.io flows.
- [x] Apply the same pool/lane conversion to all nine pages in the consolidated Draw.io file.
- [x] Reparent every activity into its actual lane while keeping cross-lane connectors at page level.
- [x] Validate XML, cell references, content, styles, and absolute geometry against the prior diagrams.
- [x] Render BF-01 in diagrams.net and visually verify the horizontal pool, vertical lane labels, activities, decisions, and connectors.
