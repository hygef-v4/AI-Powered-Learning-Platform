# Chấm cá nhân cạnh bài nhóm

Người dùng duyệt thiết kế trong hội thoại bằng “giúp tôi sửa giao diện theo đề xuất”.
Triển khai trực tiếp; giữ nguyên WIP 5 mức trước đó, không subagent/commit/push.

## Thiết kế và phạm vi

- Hai cột: DocumentEditor bên trái, khung phải thay đổi giữa Rubric nhóm và Thành viên.
- Tái sử dụng shared useWideLayout (1280px), Tabs, Select, Button và sectionActions;
  thêm một slot sidePanel optional vào DocumentEditor, không thay caller khác.
- Màn rộng dùng khung sticky có cuộn riêng. Màn hẹp dùng ngăn kéo modeless,
  đóng bằng nút/Escape; không khóa cuộn bài vì cần xem bài song song.
- Giữ component và các form được mount khi chuyển tab/người hoặc ẩn ngăn kéo.
  Lưu theo version hiện tại; giữ draft nếu reload hoặc lưu người khác.
- Chỉ liệt kê sectionAuthors đã ghi nhận; không tự suy ra đóng góp. Nút tại mục
  mở đúng thành viên, liên kết ngược dùng document:show-part (hỗ trợ từng phần).
- Không đổi backend, công thức điểm, AI, schema, dữ liệu thật hoặc MCP/Excel.

## Các bước và kiểm chứng

- [x] Regression RED: chọn người/giữ draft, jump section, mở từ sectionActions,
  hai tab, drawer đóng/mở và cảnh báo rời bài khi có đánh giá chưa lưu.
- [x] DocumentEditor thêm sidePanel trong rail hiện hữu; khi có slot không lặp
  comments trong bài. Caller không truyền slot giữ hành vi cũ.
- [x] MemberContributionPanel chọn một người, trước/sau, danh sách mục phụ trách,
  textarea căn cứ, trạng thái Chưa lưu; giữ các form có draft giữa các lần chuyển.
- [x] GroupGradingPanel chứa tab, rubric hiện có, form thành viên; ngăn kéo nhỏ
  modeless focus/Escape và đóng khi đi tới mục. Không tạo cột thứ ba.
- [x] Workspace nối sectionActions/mapping/jump/highlight, bỏ panel dưới bài và
  dùng slot. useGradingWorkspace nhận dirty bổ sung để cảnh báo rời trang, không
  tự lưu điểm cá nhân qua autosave điểm nhóm; chặn công bố khi còn draft cá nhân.
- [x] Test grading, shared document và caller assignmentwork liên quan; lint,
  TypeScript riêng nếu cache Next cũ còn lỗi. Kiểm UI local nếu runtime sẵn sàng.
- [x] Cập nhật hướng dẫn/kết quả/state/audit. Không restart BE, gọi AI hoặc ghi điểm thật.

Quyền/ARCHIVED/phiên bản vẫn qua API hiện hữu. Security/Resiliency giữ phạm vi đã
duyệt, PBT disabled. Tự review diff, không tạo worker hay reviewer ngoài yêu cầu.
