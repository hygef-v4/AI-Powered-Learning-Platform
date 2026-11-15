# Ô ngày, kiểm tra AI và đóng nhận bài trước chấm

Người dùng duyệt đề xuất audit bằng “ok giúp tôi sửa theo đề xuất $ponytail:ponytail”.
Brownfield trên staging@7d0905d4, dùng lại reverse engineering và thiết kế hiện có.

## Yêu cầu đã duyệt

- DATE-01: mọi ô chọn ngày hiện và nhận `dd/MM/yyyy`, logic/API giữ `yyyy-MM-dd`.
  Dùng component chung, giữ chọn lịch, bàn phím, kiểm ngày thật/min/max/năm nhuận.
  Áp dụng tạo/sửa lớp, Audit, lịch sử AI, lịch giao bài và hạn tự chia nhóm; giờ `HH:mm`
  theo Việt Nam, không đổi dữ liệu cũ.
- AI-01: kiểm API key với model đang chọn bằng probe đủ ngân sách thay cho 5 token;
  phân biệt khóa/quyền, hạn mức, tần suất, timeout và giới hạn token đầu ra. Giữ
  validation phản hồi hoàn chỉnh cho tạo đề/chấm bài. Không lộ key hoặc body provider.
- GRADE-01: giảng viên chỉ xem trước khi kết thúc nhận bài; chấm tay/AI, lưu điểm nhóm
  và thành viên, finalize tương thích, công bố, batch và MCP phải bị chặn ở backend
  trước khi bài đóng. UI hiện lý do chỉ đọc. Practice feedback giữ quy tắc hiện có.
- CLOSE-01: hạn tự động gồm `lateUntil ?? closesAt` và 30 giây ân hạn hiện có. Chốt
  các lượt đang làm trước khi mở chấm, tránh tranh chấp với auto-submit còn chạy.
- CLOSE-02: nút “Đóng nhận bài” cho giảng viên phụ trách trên bài lớp đang OPEN,
  xác nhận rõ ngừng sửa/nộp và chốt lượt đang làm; kiểm quyền/version, khóa và audit.
  Giữ hạn gốc, dùng CLOSED hiện có, không thêm reopen/cấu hình tùy ý. Ngưng giao
  RETIRED giữ ý nghĩa và hoạt động riêng. Không mất nội dung đang lưu phía server.

## Thiết kế và giới hạn

Tái sử dụng shared UI/native calendar, không thêm thư viện. Kiểm tra quyền/version
và trạng thái ở service dùng chung, không chỉ khóa nút. Đóng nhận bài và chốt dữ liệu
phải có thứ tự/transaction rõ; không biến lỗi phụ thuộc thành cho phép chấm.
API bổ sung thao tác close; không tự sửa điểm/dữ liệu thật. Không commit/push/deploy
khi chưa được yêu cầu. Security/Resiliency giữ phạm vi đã duyệt; PBT disabled.
