# Kế hoạch sửa nhập Excel và phát YouTube

Nguồn: [yêu cầu và tiêu chí chấp nhận](../requirements/2026-11-09-excel-youtube-fixes.md). Người dùng duyệt bằng “a”.

## Điều chỉnh phạm vi được người dùng yêu cầu trong lúc thực hiện

“thôi tôi k bị lỗi yt link nữa giúp tôi chuyển import csv bất cứ chỗ nào thành import excel, 2 phàn này có ở trong admin với manager”. Hủy bước sửa YouTube. Mở rộng chuyển Excel sang ghi danh sinh viên Manager (U05) và chọn/mẫu nhập câu hỏi Manager (U10); không đổi xuất CSV hoặc các API CSV cũ.

- [x] U05 đọc sheet email bằng xlsx hiện có, giữ contract danh sách text và server kiểm lại; thêm mẫu Excel, chặn lỗi tệp/công thức/quá giới hạn trước khi gửi.
- [x] U10 đã đọc XLSX ở backend: đổi file picker, nhãn và mẫu Excel thật; giữ backend tương thích CSV.

## Tác động và trình tự

Thay đổi trong U01 và U13 hiện hữu, không đổi kiến trúc hoặc schema. Rủi ro vừa: đọc workbook là trust boundary; video phụ thuộc dữ liệu và phản hồi bên ngoài. Hoàn tác bằng khôi phục các file sửa; không có migration dữ liệu. Thực hiện tuần tự trong workspace hiện tại, không dùng subagent.

Luồng công việc: phát hiện workspace (xong) → yêu cầu (xong) → duyệt kế hoạch (chờ) → tái hiện/test thất bại → sửa U01 → sửa U13 theo bằng chứng → build/test → báo kết quả.

| Giai đoạn | Quyết định | Lý do |
|---|---|---|
| Reverse Engineering | Dùng baseline và đối chiếu source đã đọc | Hai chức năng hiện hữu |
| User Stories | Bỏ qua | Tiêu chí chấp nhận đã cụ thể, không thêm luồng nghiệp vụ |
| Application Design / Units Generation | Bỏ qua | Giữ ranh giới U01/U13 và service hiện tại |
| Functional / NFR / Infrastructure Design | Bỏ qua giai đoạn riêng | Tái sử dụng quy tắc nhập và hạ tầng hiện có |
| Code Generation | Thực hiện sau duyệt | Các bước chi tiết bên dưới |
| Build and Test | Thực hiện | Kiểm tra parser, API và giao diện |
| Operations | Không thực hiện | Không triển khai |

## Các bước thực hiện mã và kiểm thử

- [x] 1. Kiểm tra workspace, quy ước, baseline; xác nhận các điểm CSV-only và khung YouTube hiện tại.
- [x] 2. Tìm toàn bộ caller; test workbook hợp lệ, thiếu cột, dữ liệu lỗi, vượt giới hạn, tương thích CSV, quyền và preview. Test backend đã thất bại đúng vì parser CSV không đọc workbook; test UI thất bại do chưa có đầu vào Excel.
- [x] 3. Controller/service nhận Excel qua POI; giữ endpoint/validation/transaction/audit và API CSV cũ; từ chối workbook hỏng, macro, công thức và ô lỗi.
- [x] 4. UI Admin chuyển Excel, mẫu workbook tĩnh thật; giữ preview/confirm, cập nhật test.
- [x] 5. Kiểm tra mẫu YouTube trên localhost: iframe tải 200 và hiện player; không có video lỗi trong lớp hiện hữu. Người dùng hủy yêu cầu YouTube sau câu hỏi bổ sung; đóng tab kiểm tra riêng.
- [x] 6. Hủy theo yêu cầu người dùng; không sửa YouTube/CSP.
- [x] 7. Backend liên quan 39/0 skip, toàn bộ frontend 814/119 file, lint/typecheck/build qua. Test đọc lại ba mẫu và UI preview/xác nhận. YouTube đã hủy; không kiểm nhập thật qua trình duyệt.
- [x] 8. Kiểm tra diff, đồng bộ tài liệu U01/U05/U10 và audit/state; [kết quả và giới hạn](../../construction/build-and-test/2026-11-09-excel-import-results.md). Không commit/push/deploy.

## Extension

Giữ Security và Resiliency trong phạm vi đã duyệt; bảo toàn phân quyền, validation tệp và xử lý lỗi. PBT đang tắt nên bỏ qua. Thay đổi CI, DR, topology và hạ tầng không áp dụng. Chưa có triển khai để chứng nhận kiểm soát runtime.

## Duyệt kế hoạch

Bạn chọn cách tiếp tục nào cho toàn bộ trình tự trên?

A) Duyệt kế hoạch và cho phép thực hiện các bước sửa mã, kiểm thử trong phạm vi này.

B) Điều chỉnh kế hoạch trước khi sửa mã.

C) Bổ sung giai đoạn đang bỏ qua.

D) Khác — mô tả sau câu trả lời.

[Answer]: A — người dùng trả lời “a”. Phạm vi điều chỉnh sau đó được yêu cầu trực tiếp như ghi ở đầu kế hoạch.
