# Bổ sung theo góp ý review 2026-11-02 (ngoài business rules đã chốt)

Business rules của U06 và U12 đã chốt, file `business-rules.md` giữ nguyên. Các mục dưới đây làm thêm theo góp ý review (thầy Kiên) cho phần của Võ Minh Tài, mã FB-xx dùng trong code và test. Mục nào khác BR đã chốt được ghi rõ và chờ nhóm duyệt.

| Mã | Nội dung | Quan hệ với BR đã chốt |
|---|---|---|
| FB-U06-01 | Nguồn AI mặc định theo tài khoản (Kết nối AI › API cá nhân: API của trường, API cá nhân theo nhà cung cấp, hoặc Hỏi mỗi lần). Có mặc định và nguồn dùng được thì popup Chọn nguồn AI tự gửi; nguồn chưa dùng được thì báo lý do và cho chọn nguồn khác, không tự đổi nguồn. Hộp tạo nháp U07 lấy mặc định làm giá trị ban đầu. | Làm thêm; giữ BR-U06-10, 11 (không tự đổi nguồn). |
| FB-U12-01 | Trưởng nhóm chốt mục đã Xong khi cả nhóm đồng ý: ghi `lockedBy`, `lockedAt`; người phụ trách là thành viên đã hoàn thành mục (`lastAuthorId`), hiện "Phụ trách: …"; mục đã chốt không ai nhận, giao hay sửa được (`409 SECTION_LOCKED`) tới khi trưởng nhóm mở chốt. Chỉ chốt mục `DONE` (`SECTION_NOT_DONE`). | **Khác BR-U12-33**: rule cho thành viên nhận mục `DONE`, mục đã chốt thì không nhận được. Người dùng chốt giữ tính năng (2026-11-02); chờ nhóm duyệt. |
| FB-U12-02 | Xem lịch sử mục (thành viên và giảng viên, 50 bản mới nhất) và người giữ khôi phục một bản vào bản nháp (kiểm `version`), bấm Xong mới đăng. | Làm thêm trên lịch sử đã có ở BR-U12-35. |
| FB-U12-03 | Nhập file, không dùng AI: người giữ nhập DOCX vào mục đang sửa; trưởng nhóm nhập DOCX và/hoặc Draw.io cho cả tài liệu theo tên tiêu đề, tên trang trùng tên mục, xem trước rồi áp dụng, bỏ qua mục đang có người giữ hoặc đã chốt, ghi lịch sử `IMPORT`. | Làm thêm. |
| FB-U12-04 | Đánh giá chéo: mở sau khi nhóm nộp tới 7 ngày sau hạn cuối; thành viên chấm thành viên khác 1–5 kèm nhận xét, chỉ thấy đánh giá của mình; giảng viên thấy trung bình, thang 10 và nhận xét. Chỉ để tham khảo, chưa vào điểm (cần U07/U09). | Làm thêm. |

Tải ảnh trong trình soạn mục nằm trong BR-U12-35 đã chốt (ảnh mang `fileRef`, gắn khi lưu), không phải bổ sung.

## Đã bỏ

- **Tiếp quản mục** (có trong PR #35, #40): trái BR-U12-33 (mục đang có người giữ thì nhả trước, mỗi mục một người sửa). Đã gỡ API `:take-over`, nút và hộp thoại. Nhả và giao lại vẫn theo BR-U12-36 (bản nháp bị xóa). Giá trị `TAKEOVER` của `group_document_revisions.kind` còn trong ràng buộc CSDL cho dữ liệu cũ.
- **Ghi công nhiều người khi chốt** (PR #40): thay bằng một người phụ trách (FB-U12-01).

## Ghi chú

- Chú thích trong migration `groups/V20261102_1000__group_section_collaboration.sql` còn nhắc BR-U12-39, 46, 47; không sửa vì migration đã chạy (Flyway kiểm checksum). Các mã đó tương ứng FB-U12-01, 03, 04.
- PR #35 và #40 từng sửa nhầm `business-rules.md`; đã trả về đúng bản chốt (giữ thay đổi BR-U06-50, BR-U12-42 của thành viên khác).
