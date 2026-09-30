# Sơ đồ MVP

Các sơ đồ này được tổng hợp từ thiết kế Construction của 16 unit, đối chiếu với danh mục **49 user story / 40 use case** hiện hành. Chúng mô tả thiết kế dự kiến; dự án chưa sinh mã ứng dụng hoặc migration.

| Sơ đồ | Nội dung |
|---|---|
| [ERD draw.io](erd.drawio) · [chú giải](erd.md) | Cả 45 bảng trên một canvas, có thuộc tính/khóa và quan hệ |
| [Screen flow draw.io](screen-flow.drawio) · [diễn giải](screen-flow.md) | Toàn bộ màn hình và popup ghi dữ liệu/tải file trên một canvas, tỏa ra từ Sign In theo menu từng vai trò, bảng unit và UC từng ô trong phần diễn giải; bản Mermaid theo từng vai trò. Mã UC theo bảng 40 use case |
| [Business flow draw.io](business-flow.drawio) · [diễn giải](business-flow.md) | 5 trang swimlane cho học liệu/RAG, bài cá nhân, bài nhóm, credit AI và thông báo/báo cáo; mỗi trang ghi các UC liên quan |
| [Bảng use case SRS](use-case-table.md) | Một bảng tiếng Anh gồm 40 use case với năm cột ID, Use Case, Actor, Feature, Use Case Description; bảng Merged IDs ghi rõ mã cũ đã gộp hoặc bỏ |
| [Đặc tả use case](use-case-specifications.md) | Đặc tả chi tiết 8 use case chính: tác nhân, tiền điều kiện, luồng chính, luồng thay thế và hậu điều kiện |
| [Context diagram](context-diagram.md) | Ranh giới nền tảng, người dùng và hệ thống ngoài |

## Quy ước

- **Unit** là ranh giới sở hữu dữ liệu trong cùng backend modular monolith; không phải một database hay service riêng.
- ERD draw.io đặt toàn bộ bảng trên một canvas. Nét đứt đánh dấu tham chiếu qua unit; xử lý nghiệp vụ vẫn đi qua public contract của owner.
- Redis lưu OTP, phiên và bộ đếm có TTL; RabbitMQ chuyển job/event; Google Drive giữ byte file. Các thành phần này không được vẽ thành bảng PostgreSQL.
- Dashboard, bản diff, tệp xuất CSV/XLSX và kết quả xem trước DOCX được tính khi yêu cầu; không có bảng lưu riêng. Không có bảng tiến độ hoàn thành bài học, đề chung cấp môn, báo cáo độ lệch điểm AI hoặc luồng hoàn tiền đã chốt.

Nguồn: [Construction](../aidlc-docs/construction/), [bảng use case](use-case-table.md), [phân chia unit](../aidlc-docs/inception/application-design/unit-of-work-story-map.md).
