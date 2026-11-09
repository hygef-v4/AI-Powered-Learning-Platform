# Nhập Excel trong Admin và Manager

## Phạm vi cuối theo yêu cầu người dùng

Người dùng đã duyệt kế hoạch bằng “a”, rồi hủy yêu cầu sửa YouTube: “thôi tôi k bị lỗi yt link nữa giúp tôi chuyển import csv bất cứ chỗ nào thành import excel, 2 phàn này có ở trong admin với manager”. Phạm vi cuối gồm U01 nhập tài khoản, U05 nhập danh sách sinh viên và U10 nhập câu hỏi. Các đoạn YouTube bên dưới ghi nhận yêu cầu ban đầu, không còn là tiêu chí thực hiện.

- U05: nhận `.xlsx` có một cột `email` ở sheet đầu, tối đa 100 KB/200 dòng; cung cấp mẫu Excel, xem trước trước khi xác nhận, giữ server kiểm lại quyền và từng email theo contract text hiện có.
- U10: UI chỉ chọn `.xlsx`, dùng mẫu workbook thật và nhãn Nhập Excel. Backend đã hỗ trợ XLSX, giữ tương thích CSV hiện hành.
- Không đổi xuất CSV. Không sửa YouTube/CSP. Không thêm dependency/schema.

## Phân tích yêu cầu

- Nguồn: ảnh người dùng đính kèm, yêu cầu “giúp tôi fix các lỗi sau”.
- Loại: cải tiến nhập tài khoản và sửa lỗi phát video; phạm vi U01 và U13.
- Độ sâu: tối thiểu; độ phức tạp dự kiến vừa, có xử lý tệp đầu vào.
- Dự án brownfield Java/Spring JDBC/Maven và TypeScript/Next.js/npm. Cây làm việc sạch khi bắt đầu; sử dụng baseline reverse-engineering hiện có và đối chiếu trực tiếp mã liên quan.

## Yêu cầu và tiêu chí chấp nhận

1. Trang quản trị tài khoản hiển thị “Nhập Excel”, chọn được tệp `.xlsx` và tải mẫu Excel thật với các cột `email`, `display_name`, `role`. Phạm vi mặc định là `.xlsx`; không thêm `.xls` cũ. Giữ khả năng xử lý CSV cũ của API để tránh phá caller hiện hữu.
2. Excel dùng cùng quy tắc nhập tài khoản hiện hành: xem trước không ghi dữ liệu; xác nhận kiểm lại và tạo các dòng hợp lệ; không nhập ADMIN; kiểm email trùng, vai trò, họ tên và trường bắt buộc; giữ transaction, audit/checksum và giới hạn 1 MB/1.000 dòng.
3. Video YouTube hợp lệ và được phép nhúng phát trực tiếp trong trang học. Tái hiện và xác định nguyên nhân trước khi chọn cách sửa; không mặc định thay URL hoặc nới CSP khi chưa có bằng chứng. Video không thể nhúng có hướng dẫn rõ và liên kết mở YouTube.
4. Giữ validation ID video, phân quyền và các chức năng xem/tải học liệu khác. Test hồi quy cho nhập Excel và hành vi khung phát; chạy typecheck/lint/build và các test liên quan theo phần sửa. Ghi riêng kết quả kiểm tra phát video thật.

## Bằng chứng từ mã nguồn

- `AccountListPage` mở `ImportAccountsPanel`; panel nhận `.csv` và sinh mẫu CSV.
- Frontend `validateImport`/`commitImport` gửi multipart đến hai endpoint hiện hành; `AdminAccountController.read` từ chối tên tệp khác `.csv`.
- `AccountImportService` chỉ giải mã UTF-8 và đọc bằng Commons CSV. Vì vậy đổi nhãn hoặc file picker đơn lẻ chưa đáp ứng nhập Excel.
- Apache POI đã có trong backend; `xlsx` đã có trong manifest frontend. Ưu tiên thư viện/pattern hiện hữu, chưa cần thêm dependency.
- `MaterialPreview` đã dùng iframe `youtube-nocookie`, ID 11 ký tự và referrer policy; `proxy.ts` đã cho phép domain này trong CSP. Ảnh chưa có mã lỗi YouTube hay thông tin network/console: chưa xác nhận được nguyên nhân lỗi phát.

## Phạm vi cần kiểm tra khi thực hiện

- Frontend: AccountListPage, ImportAccountsPanel, API caller, mẫu tải về và AdminScreens tests.
- Backend: AdminAccountController, AccountImportService, toàn bộ caller service và AccountImportServiceTest; kiểm tra controller/API regressions và tài liệu contract nhập tệp.
- Học liệu: mọi caller MaterialPreview, test MaterialPreview và proxy; cấu hình header triển khai nếu bằng chứng runtime chỉ ra liên quan.
- Không đổi schema, thêm dependency hay triển khai trong phạm vi này.

## Quy trình và extension

- [x] Đọc CLAUDE.md, quy ước mã nguồn và rule AI-DLC liên quan.
- [x] Workspace Detection và đối chiếu baseline/mã hiện hữu.
- [x] Phân tích yêu cầu và lập danh sách ảnh hưởng sơ bộ.
- [x] Người dùng yêu cầu tiếp tục bằng “continue”; tiếp tục lập kế hoạch theo phạm vi này.
- [x] Workflow Planning và các bước sửa/kiểm thử: xem `../plans/2026-11-09-excel-youtube-execution-plan.md`; chờ duyệt trước sửa mã.
- [x] Sửa và kiểm thử nhập Excel trong U01/U05/U10; YouTube hủy theo yêu cầu. Xem báo cáo kết quả 2026-11-09-excel-import-results.md.

Giữ cấu hình extension hiện có trong aidlc-state.md: Security và Resiliency bật theo phạm vi đã duyệt; Property-Based Testing tắt, bỏ qua quy tắc PBT. Chưa có thay đổi mã để đánh giá tuân thủ triển khai. Bảo toàn validation/phân quyền và xử lý lỗi là điều kiện bắt buộc; hạ tầng, DR và topology không áp dụng cho phạm vi này.

## Duyệt yêu cầu

Chọn hướng tiếp theo sau khi xem yêu cầu ở trên:

A) Duyệt yêu cầu và tiếp tục Workflow Planning.

B) Yêu cầu điều chỉnh phạm vi trước khi tiếp tục.

C) Khác — mô tả yêu cầu.

[Answer]: A — người dùng trả lời “a”; sau đó điều chỉnh phạm vi trực tiếp như ghi ở đầu tài liệu.
