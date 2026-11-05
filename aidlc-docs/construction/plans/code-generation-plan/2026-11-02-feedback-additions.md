# Bổ sung theo góp ý review 2026-11-02 (ngoài business rules đã chốt)

Business rules của U06 và U12 đã chốt, file `business-rules.md` giữ nguyên. Các mục dưới đây làm thêm theo góp ý review (thầy Kiên) cho phần của Võ Minh Tài, mã FB-xx dùng trong code và test. Mục nào khác BR đã chốt được ghi rõ và chờ nhóm duyệt.

| Mã | Nội dung | Quan hệ với BR đã chốt |
|---|---|---|
| FB-U06-01 | Nguồn AI mặc định theo tài khoản (Kết nối AI › API cá nhân: API của trường, API cá nhân theo nhà cung cấp, hoặc Hỏi mỗi lần). Có mặc định và nguồn dùng được thì popup Chọn nguồn AI tự gửi; nguồn chưa dùng được thì báo lý do và cho chọn nguồn khác, không tự đổi nguồn. Hộp tạo nháp U07 lấy mặc định làm giá trị ban đầu. | Làm thêm; giữ BR-U06-10, 11 (không tự đổi nguồn). |
| FB-U12-01 | **Phân công rồi chốt.** Chưa chốt phân công: thành viên nhận mục chưa có ai hoặc trưởng nhóm giao, chỉ ghi người phụ trách (`assignee_id`), chưa soạn được (`409 ASSIGNMENT_NOT_LOCKED`); mục đã có người nhận thì người khác không nhận được. Trưởng nhóm bấm **Chốt phân công** cho cả tài liệu khi mọi mục có người phụ trách (`ASSIGNMENT_INCOMPLETE` nếu thiếu); từ đó mỗi người chỉ soạn mục mình phụ trách, kể cả mở lại mục đã Xong (`SECTION_ASSIGNED_TO_OTHER`), không nhận hay giao thêm (`ASSIGNMENT_LOCKED`). Trưởng nhóm **Mở chốt** để phân lại; nội dung đã Xong và bản nháp giữ nguyên. Người rời nhóm thôi phụ trách mục. Audit `GROUP_ASSIGNMENT_LOCKED/UNLOCKED`. | **Khác BR-U12-33, 34**: theo rule ai nhận mục là soạn ngay và nhận được mục `DONE`. Người dùng chốt (2026-11-02); chờ nhóm duyệt. Thay cho chốt từng mục (đã bỏ). |
| FB-U12-02 | Xem lịch sử mục (thành viên và giảng viên, 50 bản mới nhất) và người giữ khôi phục một bản vào bản nháp (kiểm `version`), bấm Xong mới đăng. | Làm thêm trên lịch sử đã có ở BR-U12-35. |
| FB-U12-03 | Nhập file, không dùng AI: người giữ nhập DOCX vào mục đang sửa; trưởng nhóm nhập DOCX và/hoặc Draw.io cho cả tài liệu theo tên tiêu đề, tên trang trùng tên mục, xem trước rồi áp dụng, bỏ qua mục đang có người giữ hoặc đã chốt, ghi lịch sử `IMPORT`. | Làm thêm. |
| FB-U12-04 | Đánh giá chéo: mở sau khi nhóm nộp tới 7 ngày sau hạn cuối; thành viên chấm thành viên khác 1–5 kèm nhận xét, chỉ thấy đánh giá của mình; giảng viên thấy trung bình, thang 10 và nhận xét. Chỉ để tham khảo, chưa vào điểm (cần U07/U09). | Làm thêm. |

## Chờ làm

- **FB-U12-05 Khóa nhóm** (người dùng hoãn 2026-11-02): sinh viên không tự rời nhóm (đã đúng theo BR-U12-01); giảng viên chỉ đổi nhóm lúc đầu môn, sau đó giữ nguyên nhóm tới cuối môn, kể cả khi sinh viên bị gỡ khỏi lớp. Khác BR-U12-06, 07; chưa chọn mốc khóa.

Tải ảnh trong trình soạn mục nằm trong BR-U12-35 đã chốt (ảnh mang `fileRef`, gắn khi lưu), không phải bổ sung.

## Đã bỏ

- **Tiếp quản mục** (có trong PR #35, #40): trái BR-U12-33 (mục đang có người giữ thì nhả trước, mỗi mục một người sửa). Đã gỡ API `:take-over`, nút và hộp thoại. Nhả và giao lại vẫn theo BR-U12-36 (bản nháp bị xóa). Giá trị `TAKEOVER` của `group_document_revisions.kind` còn trong ràng buộc CSDL cho dữ liệu cũ.
- **Ghi công nhiều người khi chốt** (PR #40) và **chốt từng mục đã Xong**: thay bằng phân công rồi chốt (FB-U12-01). Cột `locked_by`, `locked_at` của mục không còn dùng (migration đã chạy, không xóa cột).

## Ghi chú

- Chú thích trong migration `groups/V20261102_1000__group_section_collaboration.sql` còn nhắc BR-U12-39, 46, 47; không sửa vì migration đã chạy (Flyway kiểm checksum). Các mã đó tương ứng FB-U12-01, 03, 04.
- PR #35 và #40 từng sửa nhầm `business-rules.md`; đã trả về đúng bản chốt (giữ thay đổi BR-U06-50, BR-U12-42 của thành viên khác).
