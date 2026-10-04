# Business flow — các hành trình nghiệp vụ MVP

File [business-flow.drawio](business-flow.drawio) gồm 5 trang swimlane có thể chỉnh sửa trong draw.io. Mỗi trang đặt bước xử lý vào lane của người dùng, backend, hệ thống ngoài hoặc PostgreSQL. Lane PostgreSQL thể hiện lưu trữ của unit sở hữu; các unit khác đọc dữ liệu qua port của owner. Phần chữ dưới đây giải thích các nhánh và quy tắc quan trọng. Mã UC theo [bảng use case](use-case-table.md).

## 1. Học liệu và RAG

**Sơ đồ swimlane:** [Mở trang 1 — Học liệu và RAG](business-flow.drawio).

**Use case liên quan:** UC 11 Manage Content, UC 12 Access Lesson, UC 21 Manage Templates, UC 28 Manage Assignments (AI soạn bài từ nội dung lớp).

**Diễn giải bằng chữ:** Chủ nhiệm môn quản lý học liệu cấp môn, giảng viên quản lý nội dung riêng của lớp; cả hai tạo bài học, tải file hoặc gắn YouTube. Worker trích chữ; YouTube chỉ lấy caption sẵn có, không tự phiên âm. Người tải/phát hành nguồn chịu credit embedding khi xử lý nguồn mới. Nguồn không có chữ/caption không được lập chỉ mục. Bài học có thể phát hành trước khi job RAG xong; sinh viên đã ghi danh chỉ mở bài học đã phát hành. Nguồn đã lập chỉ mục chỉ phục vụ AI soạn đề: giảng viên dùng nội dung lớp (UC 28), Chủ nhiệm môn dùng RAG cấp môn (UC 21), người yêu cầu chịu credit truy xuất/AI. Sinh viên không hỏi đáp hoặc tìm kiếm học liệu bằng AI. Hết quota hệ thống báo “Hệ thống đang bận”; thiếu credit báo riêng.

## 2. Bài cá nhân và chấm điểm

**Sơ đồ swimlane:** [Mở trang 2 — Bài cá nhân và chấm điểm](business-flow.drawio).

**Use case liên quan:** UC 23–26 (Manage Text Essay, Quiz, Diagram Essay, Code Lab), UC 28 Manage Assignments, UC 29 View Assigned Work, UC 30 Submit Assignment, UC 31 Review Attempts, UC 32 Review Submissions, UC 33 Grade Submissions, UC 34 Finalize Grades, UC 35 View Grades, UC 40 Grade Practice with AI.

**Diễn giải bằng chữ:** Bài được soạn và duyệt trước khi giao ở chế độ `GRADED` hoặc `PRACTICE`; version đã phát hành chỉ đọc. Sinh viên bắt đầu lượt trong giới hạn, làm và nộp. Quiz/Code Lab tự chấm theo đáp án/test.

- **`GRADED`:** bài nộp vào hàng chờ của giảng viên. Với Quiz/Code Lab, giảng viên được sửa điểm tự chấm nếu ghi lý do. Text Essay/Diagram Essay được chấm tay hoặc AI đề xuất khi giảng viên yêu cầu; AI không chốt hay công bố điểm. Lượt nộp cuối là lượt được chấm. Giảng viên chốt và công bố điểm cuối; sổ điểm không tính điểm tổng theo hệ số.
- **`PRACTICE`:** không vào hàng chờ giảng viên và không vào sổ điểm. Quiz/Code Lab chỉ hiện kết quả tự chấm cho sinh viên. Text Essay/Diagram Essay được AI chấm một lần khi nộp nếu sinh viên đủ credit (UC 40); thiếu credit thì bài vẫn được nộp nhưng không có điểm AI và không được chấm bù.

## 3. Bài nhóm dạng tài liệu chung

**Sơ đồ swimlane:** [Mở trang 3 — Bài nhóm với tài liệu chung](business-flow.drawio).

**Use case liên quan:** UC 9 Manage Classes (nhóm, trưởng nhóm), UC 15 Request Leader Change, UC 27 Manage Group Assignment, UC 16 Submit Group Document, UC 17 Grade Group Document, UC 33 Grade Submissions, UC 34 Finalize Grades.

**Diễn giải bằng chữ:** Giảng viên chia nhóm ngay trong danh sách sinh viên của lớp (tạo tay hoặc chia ngẫu nhiên), chỉ định trưởng nhóm và soạn bài nhóm `GRADED` gồm các mục chính; mọi bài nhóm của lớp dùng chung các nhóm này. Mỗi nhóm làm một tài liệu chung: trưởng nhóm thêm/sửa mục chi tiết dưới mục chính và giao cho thành viên, thành viên cũng có thể tự nhận mục còn trống; người giữ mục sửa riêng rồi bấm “Xong” để ghép vào bản chung realtime. Lịch sử revision giữ tác giả. Khi mọi mục đã xong, tài liệu chuyển sang review để cả nhóm xem lại và bình luận; cần sửa thì nhận lại mục và tài liệu quay về đang làm. Trưởng nhóm chỉ nộp khi tài liệu đang review; hệ thống tự nộp bản hiện tại khi hết hạn. Giảng viên tự chấm tài liệu chung; phần đóng góp có thể được AI đề xuất theo yêu cầu, sau đó giảng viên nhập điểm cuối từng người. Không có công thức tự động ghép hai nguồn điểm.

## 4. Mua và dùng credit AI

**Sơ đồ swimlane:** [Mở trang 4 — Mua và sử dụng credit AI](business-flow.drawio).

**Use case liên quan:** UC 37 Buy AI Credits, UC 22 Manage AI Service (gói credit, mức tặng hằng tháng); các UC tiêu credit là UC 11, UC 21, UC 28, UC 33 và UC 40.

**Diễn giải bằng chữ:** Tài khoản `ACTIVE` có vai trò Sinh viên, Giảng viên, Chủ nhiệm môn hoặc Quản trị viên có thể mua và xem credit của chính mình. Chỉ webhook hợp lệ hoặc job tự đối soát xác thực mới cộng credit cho người mua; trang PayOS quay về không tự cộng. Giảng viên, Chủ nhiệm môn và Quản trị viên được tặng credit hằng tháng và dùng credit cho các chức năng AI đúng quyền; credit tặng được trừ trước credit mua. Sinh viên không được tặng credit và chỉ dùng credit để AI chấm bài `PRACTICE` Text Essay/Diagram Essay của chính mình; làm bài, chạy Code Lab và xem điểm không tiêu credit. Trước mỗi lời gọi AI, hệ thống kiểm quyền và trần quota rồi giữ credit; hoàn tất thì quyết toán theo token, lỗi trước khi nhà cung cấp xử lý thì trả phần giữ. Hết quota hệ thống báo bận và không trừ credit cho lời gọi bị từ chối. Chính sách hoàn tiền cho giao dịch đã `PAID` chưa chốt, không nằm trong luồng này.

## 5. Thông báo, tiến độ và báo cáo

**Sơ đồ swimlane:** [Mở trang 5 — Thông báo và báo cáo](business-flow.drawio).

**Use case liên quan:** UC 38 View Notifications, UC 36 Monitor Submissions, UC 18 View Learning Overview.

**Diễn giải bằng chữ:** Sau thay đổi nghiệp vụ, owner phát event; U16 kiểm lại phạm vi trước khi tạo thông báo. Email tùy loại và tùy chọn người dùng; thông báo lớp/hỏi đáp chỉ vào app. U16 nhắc người chưa nộp 24 giờ trước hạn, tổng hợp dashboard cá nhân và stream bảng điểm theo quyền. Tệp xuất không lưu trong U03; không xuất điểm AI đề xuất, kết quả `PRACTICE` hay điểm tổng/hệ số.

Nguồn: `business-logic-model.md` và `business-rules.md` của [Construction](../aidlc-docs/construction/), cùng [bảng use case](use-case-table.md).
