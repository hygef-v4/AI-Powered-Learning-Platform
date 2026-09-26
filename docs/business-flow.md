# Business flow — các hành trình nghiệp vụ MVP

File [business-flow.drawio](business-flow.drawio) gồm 5 trang swimlane có thể chỉnh sửa trong draw.io. Mỗi trang đặt bước xử lý vào lane của người dùng, backend, hệ thống ngoài hoặc PostgreSQL. Lane PostgreSQL thể hiện lưu trữ của unit sở hữu; các unit khác đọc dữ liệu qua port của owner. Phần chữ dưới đây giải thích các nhánh và quy tắc quan trọng.

## 1. Học liệu và RAG

**Sơ đồ swimlane:** [Mở trang 1 — Học liệu và RAG](business-flow.drawio).

**Diễn giải bằng chữ:** Người có quyền tạo bài học, tải file hoặc gắn YouTube. Worker trích chữ; YouTube chỉ lấy caption sẵn có, không tự phiên âm. Người tải/phát hành nguồn chịu credit embedding khi xử lý nguồn mới; người chủ động dùng AI chịu credit cho truy xuất/AI của mình. Nguồn không có chữ/caption không được lập chỉ mục. Hết quota hệ thống báo “Hệ thống đang bận”; thiếu credit báo riêng. Bài học có thể phát hành trước khi job RAG xong.

## 2. Bài cá nhân và chấm điểm

**Sơ đồ swimlane:** [Mở trang 2 — Bài cá nhân và chấm điểm](business-flow.drawio).

**Diễn giải bằng chữ:** Bài được soạn và duyệt trước khi giao; version đã phát hành chỉ đọc. Người học bắt đầu lượt trong giới hạn, làm và nộp. QUIZ/Code Lab tự chấm theo đáp án/test, giảng viên vẫn có quyền sửa có lý do. ESSAY/DOCUMENT được chấm tay hoặc AI đề xuất khi giảng viên yêu cầu; AI không chốt hay công bố. Với bài thường, lượt nộp cuối được chấm; thi thử dùng `HIGHEST`, `LATEST` hoặc `AVERAGE` trên các lượt có điểm, mặc định 3 lượt, giảng viên chỉnh 1–10 trước lượt đầu. Không tính điểm tổng theo hệ số.

## 3. Bài nhóm dạng tài liệu chung

**Sơ đồ swimlane:** [Mở trang 3 — Bài nhóm với tài liệu chung](business-flow.drawio).

**Diễn giải bằng chữ:** Nhóm làm một tài liệu chung, các thành viên tự nhận mục còn trống, sửa riêng rồi bấm “Xong” để ghép vào bản chung. Lịch sử revision giữ tác giả. Trưởng nhóm nộp; hệ thống tự nộp bản hiện tại khi hết hạn. Giảng viên tự chấm tài liệu chung; phần đóng góp có thể được AI đề xuất theo yêu cầu, sau đó giảng viên nhập điểm cuối từng người. Không có công thức tự động ghép hai nguồn điểm.

## 4. Mua và dùng credit AI

**Sơ đồ swimlane:** [Mở trang 4 — Mua và sử dụng credit AI](business-flow.drawio).

**Diễn giải bằng chữ:** Mọi tài khoản `ACTIVE` có thể mua credit; chỉ webhook hợp lệ hoặc job tự đối soát xác thực mới cộng credit, trang PayOS quay về không tự cộng. Trước lời gọi AI, hệ thống kiểm trần rồi giữ credit; hoàn tất thì quyết toán, lỗi trước khi nhà cung cấp xử lý thì trả phần giữ. Credit miễn phí tháng được dùng trước credit mua. Hết quota hệ thống báo bận và không trừ credit cho lời gọi bị từ chối. Chính sách hoàn tiền cho giao dịch đã `PAID` chưa chốt, không nằm trong luồng này.

## 5. Thông báo, tiến độ và báo cáo

**Sơ đồ swimlane:** [Mở trang 5 — Thông báo và báo cáo](business-flow.drawio).

**Diễn giải bằng chữ:** Sau thay đổi nghiệp vụ, owner phát event; U16 kiểm lại phạm vi trước khi tạo thông báo. Email tùy loại và tùy chọn người dùng; thông báo lớp/hỏi đáp chỉ vào app. U16 nhắc người chưa nộp trước hạn, tổng hợp dashboard cá nhân và stream bảng điểm theo quyền. Tệp xuất không lưu trong U03; không xuất điểm AI đề xuất hay điểm tổng/hệ số.

Nguồn: `business-logic-model.md` và `business-rules.md` của [Construction](../aidlc-docs/construction/), cùng [use case MVP](../aidlc-docs/inception/user-stories/use-cases.md).

