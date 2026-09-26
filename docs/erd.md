# ERD MVP — một canvas draw.io

[Mở sơ đồ ERD có thể chỉnh sửa](erd.drawio)

File `erd.drawio` đặt **62 bảng của U01–U16 trên cùng một canvas**, không tách trang hoặc khung nhóm. Mỗi bảng ghi tên unit sở hữu trong tiêu đề. Bảng thể hiện các cột chính của thiết kế Construction; kiểu cột và ràng buộc cuối cùng sẽ được chốt khi viết migration.

**Chú giải:** `PK` là khóa chính; `FK` là cột tham chiếu. Đường liền thể hiện 36 quan hệ từ ERD đã tổng hợp. Nét đứt thể hiện 19 tham chiếu logic quan trọng giữa các unit. Các unit nằm trong cùng backend nhưng nghiệp vụ truy cập dữ liệu của unit khác qua public contract, không đọc repository của nhau.

**Diễn giải bằng chữ:** Tài khoản, môn/lớp, học liệu/RAG, ngân hàng câu hỏi, thanh toán credit, đề và lượt làm, nhóm, AI/chạy code, điểm và thông báo liên kết thành một mô hình dữ liệu MVP. Redis (OTP, phiên, bộ đếm), RabbitMQ (job/event), Google Drive (byte file) và Judge0 không phải bảng PostgreSQL trong ERD. Dashboard, diff và file CSV/XLSX được tạo khi đọc, không có bảng lưu riêng. Chính sách hoàn tiền chưa chốt nên không có bảng refund.

Nguồn: `functional-design/domain-entities.md` và `infrastructure-design/infrastructure-design.md` của [16 unit Construction](../aidlc-docs/construction/). Danh mục chức năng hiện hành là [77 use case MVP](../aidlc-docs/inception/user-stories/use-cases.md).
