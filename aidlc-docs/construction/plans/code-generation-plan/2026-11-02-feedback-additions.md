# Bổ sung theo góp ý review 2026-11-02 (ngoài business rules đã chốt)

Business rules của U06 và U12 đã chốt, file `business-rules.md` giữ nguyên. Các mục dưới đây làm thêm theo góp ý review (thầy Kiên) cho phần của Võ Minh Tài, mã FB-xx dùng trong code và test. Mục nào khác BR đã chốt được ghi rõ và chờ nhóm duyệt. Ngày 2026-11-08 U12 quay về đúng BR, các mục FB-U12-01..04 chuyển xuống phần Đã bỏ.

| Mã | Nội dung | Quan hệ với BR đã chốt |
|---|---|---|
| FB-U06-01 | Nguồn AI mặc định theo tài khoản (Kết nối AI › API cá nhân: API của trường, API cá nhân theo nhà cung cấp, hoặc Hỏi mỗi lần). Có mặc định và nguồn dùng được thì popup Chọn nguồn AI tự gửi; nguồn chưa dùng được thì báo lý do và cho chọn nguồn khác, không tự đổi nguồn. Hộp tạo nháp U07 lấy mặc định làm giá trị ban đầu. | Làm thêm; giữ BR-U06-10, 11 (không tự đổi nguồn). |

## Chờ làm

- **FB-U12-05 Khóa nhóm** (người dùng hoãn 2026-11-02): sinh viên không tự rời nhóm (đã đúng theo BR-U12-01); giảng viên chỉ đổi nhóm lúc đầu môn, sau đó giữ nguyên nhóm tới cuối môn, kể cả khi sinh viên bị gỡ khỏi lớp. Khác BR-U12-06, 07; chưa chọn mốc khóa.

Tải ảnh trong trình soạn mục nằm trong BR-U12-35 đã chốt (ảnh mang `fileRef`, gắn khi lưu), không phải bổ sung.

## Đã bỏ

- **FB-U12-01..04** (bỏ 2026-11-08, người dùng chọn U12 tối giản: sinh viên làm bài nhóm, giảng viên chấm, không thêm luồng phức tạp; U12 quay về đúng BR-U12-33, 34, 36):
  - FB-U12-01 Phân công rồi chốt (khác BR-U12-33, 34): bỏ API `assignment:lock/unlock`, cột `group_documents.assignment_locked_by/at` và `group_document_sections.assignee_id` không còn dùng. Nhận mục Mở hoặc Xong là soạn ngay; trưởng nhóm giao, nhả mục.
  - FB-U12-02 Lịch sử mục: giữ phần xem (BR-U12-35 đã lưu lịch sử), bỏ khôi phục (`:restore`).
  - FB-U12-03 Nhập file: giữ nhập DOCX vào mục đang soạn (như trình soạn bài cá nhân), bỏ nhập DOCX/Draw.io cho cả tài liệu (`import:preview`, `import:apply`).
  - FB-U12-04 Đánh giá chéo: bỏ; bảng `group_peer_reviews` còn trong CSDL, không dùng.

- **Tiếp quản mục** (có trong PR #35, #40): trái BR-U12-33 (mục đang có người giữ thì nhả trước, mỗi mục một người sửa). Đã gỡ API `:take-over`, nút và hộp thoại. Nhả và giao lại vẫn theo BR-U12-36 (bản nháp bị xóa). Giá trị `TAKEOVER` của `group_document_revisions.kind` còn trong ràng buộc CSDL cho dữ liệu cũ.
- **Ghi công nhiều người khi chốt** (PR #40) và **chốt từng mục đã Xong**: thay bằng phân công rồi chốt (FB-U12-01). Cột `locked_by`, `locked_at` của mục không còn dùng (migration đã chạy, không xóa cột).

## Ghi chú

- Chú thích trong migration `groups/V20261102_1000__group_section_collaboration.sql` còn nhắc BR-U12-39, 46, 47; không sửa vì migration đã chạy (Flyway kiểm checksum). Các mã đó tương ứng FB-U12-01, 03, 04.
- PR #35 và #40 từng sửa nhầm `business-rules.md`; đã trả về đúng bản chốt (giữ thay đổi BR-U06-50, BR-U12-42 của thành viên khác).
