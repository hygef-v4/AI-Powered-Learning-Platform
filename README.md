# AI-Powered-Learning-Platform

Nền tảng học tập cho một trường hoặc trung tâm đào tạo với bốn vai trò: Student, Teacher, Subject Manager và Administrator. Hệ thống quản lý môn/lớp, học liệu có RAG (kể cả nguồn YouTube), năm dạng bài (Text Essay, Multiple-Choice Quiz, Diagram Essay, Code Lab, Group Assignment) ở chế độ `GRADED` hoặc `PRACTICE`, chấm điểm có AI hỗ trợ và credit AI thanh toán qua PayOS.

Dự án đang ở giai đoạn thiết kế theo quy trình AI-DLC; chưa có mã ứng dụng. Kiến trúc dự kiến: backend modular monolith Java 17 / Spring Boot 3, frontend Next.js (TypeScript), PostgreSQL, Redis, RabbitMQ, Google Drive, Gemini và Judge0.

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [docs/](docs/README.md) | Bảng 40 use case, đặc tả use case chính, ERD, screen flow, business flow, context diagram |
| [Requirements](aidlc-docs/inception/requirements/requirements.md) | Yêu cầu chức năng và phi chức năng |
| [User stories](aidlc-docs/inception/user-stories/stories.md) | 49 story MVP và ma trận story ↔ use case |
| [Application design](aidlc-docs/inception/application-design/unit-of-work.md) | 16 unit, phụ thuộc và story map |
| [Construction](aidlc-docs/construction/) | Thiết kế chức năng, NFR, hạ tầng và code plan từng unit |
| [Trạng thái AI-DLC](aidlc-docs/aidlc-state.md) | Tiến độ các stage và việc còn mở |
