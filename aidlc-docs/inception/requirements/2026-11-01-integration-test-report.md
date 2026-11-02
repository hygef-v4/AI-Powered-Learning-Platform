# Integration test report — phạm vi và tiêu chí nghiệm thu

## Yêu cầu đã nhận

Viết integration test case cho AI-Powered Learning Platform theo phương án functional integration, dựa trên luồng nghiệp vụ và kỹ thuật black-box. Dùng workbook template người dùng cung cấp, giữ format và styling. Tạo branch mới từ staging trước khi thực hiện, tên branch không có tiền tố codex/.

## Baseline và giới hạn

- Branch: `integration-test-report`, tạo từ local `staging` tại `e54359e46e449342e863cbb94ee14d5b8a754259`.
- Template: `E:\FPTUni\sem9_fa26\test_report\Report-5.2_Integration Test_Template.xlsx`.
- Tài liệu nguồn: requirements hiện hành, catalog 66 UC, thiết kế 15 unit, thay đổi MCP/AI đã chốt ngày 2026-11-01 và code/contracts tương ứng.
- Đây là tạo tài liệu kiểm thử; không sửa hành vi ứng dụng, không thực thi toàn bộ test case, không gọi provider có phí và không ghi kết quả Passed khi chưa chạy.
- Thay đổi có sẵn của báo cáo Word nằm ngoài phạm vi và được giữ nguyên.
- Security/Resiliency giữ cấu hình đã có; PBT vẫn tắt. Không yêu cầu lại lựa chọn extension đã được ghi trong aidlc-state.md.

## Hai cách tổ chức có thể chọn

A. Bộ đầy đủ theo 15 unit, có truy vết luồng xuyên unit (đề xuất). Bao phủ các tương tác của phạm vi hiện hành, thuận tiện phân công và tổng hợp báo cáo.

B. Bộ tập trung các luồng chính: ghi danh, truy cập lớp, bài tập, nộp bài, chấm/công bố điểm, quiz và thông báo. Ít case hơn, nhưng không đại diện đầy đủ toàn hệ thống.

Không đặt số lượng case làm mục tiêu. Số case được xác định sau khi lập ma trận yêu cầu–điểm tích hợp–tình huống để tránh thiếu coverage hoặc thêm case trùng.

## Nội dung dự kiến nếu chọn A

| Unit | Nhóm tương tác cần rà soát |
|---|---|
| U01 Account & Access | Kích hoạt/khôi phục qua OTP-email; session; trạng thái tài khoản và quyền backend |
| U02 Audit | Nghiệp vụ phát sinh audit; đọc/lọc đúng actor/đối tượng; giới hạn quyền xem |
| U03 Job and Settings | File/storage/quyền tải; thiết lập và tác động; job/backup/trạng thái lỗi |
| U04 Subject Administration | Môn và chủ nhiệm môn; thay đổi phân công và quyền truy cập liên quan |
| U05 Class & Enrollment | Môn/lớp; phân công giảng viên; ghi danh và quyền truy cập; bảo toàn lịch sử |
| U06 AI Connections | Nguồn shared/personal/MCP; quota; thu hồi; quyền/scope; usage và lỗi provider |
| U07 Assignment Authoring | Phạm vi lớp/môn; rubric; review/publication; copy/version và nội dung độc lập |
| U08 Assignment Work | Quyền workspace; lưu/đọc lại; nộp/snapshot; Practice feedback và quyền sở hữu |
| U09 Assignment Grading | Bài nộp/rubric; AI proposal và duyệt; công bố; gradebook/export; bảo mật điểm |
| U10 Quiz Authoring | Ngân hàng câu hỏi; scope; publication/version; đáp án và snapshot |
| U11 Quiz Practice | Attempt; câu trả lời; chấm xác định; giới hạn lượt; lịch sử/kết quả riêng |
| U12 Group Assignment | Thành viên/leader; quyền phần tài liệu; chuyển leader; version; nộp nhóm |
| U13 Reference Materials | Tệp/link; scope môn/lớp; truy cập storage; xóa/thay thế và quyền tải |
| U14 Announcements & Notifications | Người nhận ghi danh; quan trọng/email; tạo/sửa; quyền đọc/đánh dấu thông báo |
| U15 Admin Dashboard | Tổng hợp dữ liệu thật; backup history/status; quyền admin |

## Quy tắc viết test case

- Ngôn ngữ đã duyệt: tiếng Anh dễ hiểu, dùng câu ngắn và động từ rõ ràng; giữ tên cột và cấu trúc template gốc. Không dịch tên màn hình tiếng Việt thành nhãn UI tiếng Anh không tồn tại; khi cần ghi nhãn UI thực tế trong ngoặc.
- Mỗi case nêu rõ thành phần tích hợp, UC/FR nguồn, kỹ thuật coverage, mức ưu tiên và cách thực thi trong cột Note, không thêm cột mới.
- Có ID ổn định; mục tiêu cụ thể; preconditions có role, scope, trạng thái và dữ liệu; procedure đánh số; expected results kiểm chứng được.
- Luồng thành công, sai quyền/scope, biến đổi trạng thái, dữ liệu biên và lỗi dependency được chọn theo quy tắc thực tế, không nhân case máy móc.
- Tách case thuần UI/validation phía client khỏi coverage integration; kiểm tra backend/HTTP hoặc persistence khi mục tiêu yêu cầu.
- Dùng UI khi tester có thể kiểm chứng đủ; bổ sung API/DB/log/mail sandbox khi cần và ghi rõ trong case. Không dùng việc có toast thành công làm bằng chứng duy nhất của dữ liệu đã lưu.
- Không tự đặt mã HTTP, thời gian chờ, giới hạn, nội dung thông báo, semantics retry hoặc rollback nếu nguồn chưa xác định. Điểm chưa thống nhất phải ghi rõ cần xác minh.
- Dữ liệu kiểm thử có định danh nhất quán, tách lớp/giảng viên/sinh viên khác phạm vi. Case thay đổi trạng thái nêu cách khôi phục hoặc bộ dữ liệu riêng để tránh phụ thuộc thứ tự.
- Ba vòng test khởi tạo Pending; ngày test, tester và defect để trống. Creator/Reviewer không tự nhận là người đã nghiệm thu.

## Bảo toàn Excel

- Tạo bản đầu ra mới trong workspace; không ghi đè template bên ngoài repository.
- Giữ các sheet Cover, Test Cases, Test Statistics; sao chép sheet feature mẫu cho từng unit, giữ tên cột A:P và ba vòng test.
- Giữ font, màu nền, đường viền, căn lề, wrap text, merged cells và chiều rộng cột của template. Chỉ điều chỉnh chiều cao hàng theo nội dung.
- Giữ cấu trúc SecurityTest, thay dữ liệu mẫu không thuộc dự án bằng case phân quyền/bảo mật tích hợp của dự án; tránh đếm trùng case với sheet unit.
- Cập nhật danh mục sheet, hyperlink, tổng số case, công thức thống kê theo dữ liệu thực. Template đang có tham chiếu #REF! trong metadata và số mẫu, nên các tham chiếu bị ảnh hưởng phải được sửa đúng trong bản đầu ra.
- Rà soát nội dung toàn bộ case; kiểm tra ID duy nhất, truy vết, số liệu, công thức và trạng thái; render để kiểm tra layout các sheet.

## Kết quả bàn giao

Một file `.xlsx` có thể dùng để tester thực hiện và ghi nhận kết quả; lời bàn giao nêu số case/sheet thực tế và phạm vi chưa thực thi. Các script hỗ trợ không phải đầu ra chính.

## Xác nhận phạm vi

Chọn phương án để tiếp tục lập kế hoạch chi tiết và tạo workbook:

A) Duyệt phạm vi đầy đủ 15 unit và tiêu chí trên (đề xuất).

B) Chọn bộ tập trung các luồng chính như phương án B.

C) Khác — ghi thay đổi mong muốn sau thẻ Answer.

[Answer]: x
Theo phương án A nhưng sửa ở quy tắc viết test case: ngôn ngữ tiếng Anh nhưng không quá khó hiểu

## Tiến độ

- [x] Tạo và xác minh branch từ staging; giữ thay đổi có sẵn.
- [x] Đọc quy trình dự án, yêu cầu/cấu trúc nghiệp vụ và cấu trúc/style template.
- [x] Chuẩn bị phạm vi, tiêu chí và phương án để review.
- [x] Nhận quyết định phạm vi của người dùng: phương án A; điều chỉnh sang tiếng Anh dễ hiểu. Giá trị Answer là x nhưng phần diễn giải xác định rõ lựa chọn A, không có mâu thuẫn về phạm vi.
- [x] Lập ma trận coverage và kế hoạch tạo workbook.
- [x] Viết test case và tạo workbook giữ styling.
- [x] Kiểm tra nội dung, thống kê và layout; bàn giao.


## Kết quả — 2026-11-01

Đã bàn giao đặc tả 164 case/19 sheet, tiếng Anh dễ hiểu, truy vết UC01–66. Điều kiện contract stubs đặt ở môi trường chung. Font/màu/viền/chiều rộng cột và style hàng case giữ từ template; thêm wrap cho metadata dài và định dạng ngày để tránh cắt nội dung. Chưa thực thi case trên ứng dụng.
