# U09 — phục hồi yêu cầu chấm AI bị gián đoạn

Phạm vi được chọn theo audit kiến trúc SEP490. Dùng mã yêu cầu hiện có để tiếp tục đề xuất đang chờ mà chưa gắn tác vụ. Giữ cách xác nhận điểm, rubric đã khóa, quyền, HTTP và MCP hiện hành.

- [x] Thêm hai tình huống PostgreSQL trước khi tạo tác vụ và sau khi task cũ được lưu nhưng chưa gắn đề xuất. Ca service tái hiện lỗi trước sửa. Lượt PostgreSQL ban đầu không chạy được vì container chưa sẵn sàng. Cả hai tình huống đạt khi chạy lại sau sửa.
- [x] Khi gửi lại đúng mã và snapshot, tiếp tục đề xuất cũ. Tuần tự hóa khâu dispatch theo khóa đánh giá và lưu gắn task cùng transaction U06. Duy trì trạng thái lỗi hiện hành nếu submit thất bại.
- [x] Giữ mã yêu cầu ở giao diện trong lần gửi lại do lỗi mạng không xác định. Ca thành công hoặc thay đổi phạm vi bắt đầu yêu cầu mới.
- [x] QA Orca rà soát. Test hồi quy xác nhận lỗi đến trễ phải thành `STALE` khi snapshot đổi và payload MCP sai định dạng phải bị từ chối khi gửi lại.
- [x] Chạy 157 test backend và 223 test frontend grading, TypeScript và ESLint các file sửa. PostgreSQL và RabbitMQ thật dùng dữ liệu tổng hợp, hai lượt gửi lại đồng thời dùng một proposal, một task, một khoản giữ quota và một message.
- [ ] Xác minh hành trình trình duyệt qua stack cô lập và chấm lại bằng provider thật.
