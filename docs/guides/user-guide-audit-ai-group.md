# Hướng dẫn sử dụng: Nhật ký, Kết nối AI, Bài nhóm

Áp dụng cho bản trên nhánh `staging` từ 11/2026 (U02 Audit, U06 AI Connections & MCP, U12 Group Assignment). Tên nút và tên màn ghi đúng như trên giao diện.

## 1. Nhật ký (Quản trị viên)

Nhật ký ghi lại các thao tác quan trọng: đăng nhập, đổi quyền, thay khóa AI, giao mục, nhả mục, nộp bài nhóm, v.v.

1. Vào menu quản trị › **Nhật ký**.
2. Lọc theo **Từ ngày**, **Đến ngày**, **Người thực hiện**, **Hành động**, **Đối tượng**, **Kết quả**. Đổi bộ lọc là tra cứu ngay, không cần bấm nút.
3. Bấm một dòng để xem chi tiết (đối tượng, kết quả, thông tin kèm theo).

Lưu ý: nhật ký chỉ đọc, không sửa hay xóa được. Mỗi lần mở màn Nhật ký cũng được ghi lại.

## 2. Kết nối AI (Student, Teacher, Subject Manager)

Mở menu **Kết nối AI**. Trang có ba thẻ:

- **Lịch sử sử dụng**: các lần dùng AI của bạn, số token, nguồn đã dùng.
- **API cá nhân**: nguồn AI mặc định và khóa API của bạn.
- **Agent MCP**: token cho agent bên ngoài (Claude Desktop, Cursor…). Xem `docs/guides/mcp-clients.md`.

### 2.1 Dán khóa API cá nhân

1. Thẻ **API cá nhân** › **Thêm khóa**.
2. Chọn nhà cung cấp (Gemini, OpenAI, Claude), dán khóa, chọn mô hình, bấm **Lưu**.
3. Bấm **Kiểm tra** để thử khóa. Thấy **Đạt** là dùng được.

Khóa chỉ hiện 4 ký tự cuối sau khi lưu. Cách lấy khóa của từng nhà cung cấp: `docs/guides/ai-api-keys.md`.

### 2.2 Đặt nguồn AI mặc định (không phải chọn mỗi lần)

1. Thẻ **API cá nhân** › mục **Nguồn AI mặc định**.
2. Ở ô **Khi dùng chức năng AI**, chọn:
   - **API của trường**: trường chịu chi phí, trừ vào hạn mức hằng ngày (chỉ giảng viên);
   - hoặc **API cá nhân · Gemini/OpenAI/Claude**: bạn chịu chi phí trên khóa của mình;
   - hoặc **Hỏi mỗi lần**.
3. Bấm **Lưu**.

Từ đó, mọi nút AI (tạo đề bằng AI, sinh câu hỏi, đề xuất chấm) sẽ tự gửi bằng nguồn mặc định. Nếu lúc đó nguồn chưa dùng được (chưa có khóa, hết hạn mức, trường tắt API), hệ thống báo lý do và hỏi bạn chọn nguồn khác cho lần đó. Hệ thống không tự đổi nguồn.

Lỗi thường gặp: **Key API không hợp lệ (HTTP 400)** nghĩa là khóa của nguồn đang chọn sai hoặc hết hạn. Với API cá nhân, dán lại khóa rồi bấm **Kiểm tra**. Với API của trường, báo Quản trị viên kiểm tra ở **Cài đặt hệ thống › API AI của trường**.

## 3. Bài nhóm

### 3.1 Giảng viên: chia nhóm

1. Mở lớp › thẻ **Nhóm**.
2. **Tạo nhóm**: đặt tên, chọn thành viên và trưởng nhóm, bấm **Tạo nhóm**. Hoặc **Chia ngẫu nhiên**: chọn số người mỗi nhóm › **Xem trước** (bấm **Chia lại** nếu muốn chia khác) › **Lưu N nhóm**.
3. Sửa nhóm: bấm biểu tượng bút ở nhóm › đổi thành viên, trưởng nhóm › **Lưu nhóm**. Nhóm đã có tài liệu hoặc bản nộp không xóa được, chỉ chuyển thành viên.
4. **Yêu cầu đổi trưởng nhóm** của sinh viên: chọn trưởng nhóm mới rồi bấm **Duyệt**, hoặc **Từ chối**.

### 3.2 Giảng viên: tạo bài nhóm có nhiều phần

1. Tạo bài tập, chọn hình thức **Nhóm**.
2. Trong thân bài, mỗi phần sinh viên nhận là một **tiêu đề**: bấm vào dòng trống dưới đường kẻ, gõ tên phần (ví dụ `1. Phân tích yêu cầu`), giữ con trỏ trên dòng đó rồi chọn **Kiểu chữ › Tiêu đề 1**. Tiêu đề không có tiêu đề con bên dưới là một phần.
3. Gõ hướng dẫn cho từng phần dưới tiêu đề (kiểu **Văn bản thường**), chia điểm rubric theo phần rồi xuất bản.

### 3.3 Sinh viên: làm tài liệu nhóm

Mở bài tập nhóm › thẻ **Bài nhóm** › **Mở tài liệu nhóm**. Cột trái là danh sách mục, cột phải là toàn bộ tài liệu. Mỗi mục có một trong ba trạng thái: **Mở** (chưa ai nhận), **Đang làm** (một người đang soạn), **Xong**.

- **Nhận mục**: bấm **Nhận mục** ở mục đang Mở. Trình soạn mở toàn trang ngay, tự lưu mỗi 10 giây. Mỗi mục chỉ một người soạn tại một thời điểm; mục người khác đang soạn thì chỉ xem.
- **Soạn**: dùng như trình soạn bài cá nhân. Chèn ảnh, bảng, sơ đồ Draw.io (nếu bài bật sơ đồ). **Nhập DOCX** (trong trình soạn): chọn tệp Word, xem trước, bấm **Nhập**; nội dung được thêm vào cuối mục.
- **Xong**: đăng nội dung cho cả nhóm thấy và nhả mục.
- **Soạn tiếp / Sửa mục**: mục mình đang giữ thì bấm **Soạn tiếp**. Mục đã Xong vẫn sửa được: bấm **Sửa mục** để nhận lại, bản nháp bắt đầu từ nội dung đã đăng.
- **Nhả mục**: không làm nữa thì bấm **Nhả mục**. Bản nháp chưa Xong bị bỏ; mục về Xong nếu đã có nội dung, không thì về Mở.
- **Lịch sử**: mỗi lần bấm Xong là một bản kèm người viết và thời điểm; chỉ để xem.

### 3.4 Trưởng nhóm

- **Giao mục** cho một thành viên; người được giao soạn được ngay. Mục đang có người khác soạn thì hệ thống cảnh báo: giao tiếp sẽ nhả mục, bản nháp chưa Xong của người đó bị bỏ.
- **Nhả mục** của thành viên đang giữ lâu để giao cho người khác.
- **Nộp tài liệu**: chỉ trưởng nhóm. Mục chưa Xong không vào bản nộp. Nộp lại được trước hạn; thẻ **Bản nộp** có biên nhận, tác giả từng mục và tải DOCX. Hết hạn mà nhóm chưa nộp bản mới nhất thì hệ thống tự nộp.

### 3.5 Giảng viên: theo dõi và chấm bài nhóm

- **Tiến độ bài nhóm**: ở trang **Sinh viên** của lớp, thẻ **Tiến độ bài nhóm** hiện từng nhóm với trưởng nhóm, số mục Xong, trạng thái (Chưa bắt đầu, Đang làm, Đã nộp, trễ) và mục đang có người soạn. Bấm **Xem tài liệu** để xem tài liệu nhóm (chỉ xem) và **Nhả mục** khi cần.
- **Chấm**: vào **Chấm điểm**, chọn bài nhóm, mở bài nộp của nhóm. Tick mục chấm theo rubric, xem tác giả từng mục; điểm từng thành viên mặc định bằng điểm nhóm và chỉnh được. Bấm **Công bố điểm** để sinh viên xem.
