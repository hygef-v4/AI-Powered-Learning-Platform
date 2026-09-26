# Sơ đồ MVP

Các sơ đồ này được tổng hợp từ thiết kế Construction của 16 unit, đối chiếu với danh mục **49 user story / 77 use case** hiện hành. Chúng mô tả thiết kế dự kiến; dự án chưa sinh mã ứng dụng hoặc migration.

| Sơ đồ | Nội dung |
|---|---|
| [ERD draw.io](erd.drawio) · [chú giải](erd.md) | Cả 45 bảng trên một canvas, có thuộc tính/khóa và quan hệ |
| [Screen flow](screen-flow.md) | Điều hướng chính theo người học, giảng viên, Chủ nhiệm môn và quản trị viên |
| [Business flow draw.io](business-flow.drawio) · [diễn giải](business-flow.md) | 5 trang swimlane cho học liệu/RAG, bài cá nhân, bài nhóm, credit AI và thông báo/báo cáo |
| [Use case](use-case.md) | Đủ 77 use case MVP, giữ nguyên mã từ catalog |
| [Bảng use case SRS](use-case-table.md) | Một bảng tiếng Anh gồm 77 use case với bốn cột ID, Use Case, Feature, Use Case Description |
| [Context diagram](context-diagram.md) | Ranh giới nền tảng, người dùng và hệ thống ngoài |

## Quy ước

- **Unit** là ranh giới sở hữu dữ liệu trong cùng backend modular monolith; không phải một database hay service riêng.
- ERD draw.io đặt toàn bộ bảng trên một canvas. Nét đứt đánh dấu tham chiếu qua unit; xử lý nghiệp vụ vẫn đi qua public contract của owner.
- Redis lưu OTP, phiên và bộ đếm có TTL; RabbitMQ chuyển job/event; Google Drive giữ byte file. Các thành phần này không được vẽ thành bảng PostgreSQL.
- Dashboard, bản diff, tệp xuất CSV/XLSX và kết quả xem trước DOCX được tính khi yêu cầu; không có bảng lưu riêng. Không có bảng tiến độ hoàn thành bài học, đề chung cấp môn, báo cáo độ lệch điểm AI hoặc luồng hoàn tiền đã chốt.

Nguồn: [Construction](../aidlc-docs/construction/), [danh mục use case](../aidlc-docs/inception/user-stories/use-cases.md), [phân chia unit](../aidlc-docs/inception/application-design/unit-of-work-story-map.md).
