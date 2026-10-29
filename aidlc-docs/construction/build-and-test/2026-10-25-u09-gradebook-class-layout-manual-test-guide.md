# Hướng dẫn người dùng test UI — Sổ điểm, thông tin lớp và thông báo U09

Trạng thái: Code complete / chờ người dùng test UI (kiểm tra tự động đã hoàn tất; xem báo cáo chi tiết tại `2026-10-25-u09-gradebook-class-layout-implementation-report.md`). Các ô bên dưới dành cho người dùng; chưa ca nào được đánh dấu đạt trong guide này.

## 1. Chuẩn bị

1. Chờ Gemini báo `Code complete / chờ người dùng test UI`, đọc implementation report cùng thư mục. Không coi guide này là bằng chứng giao diện đã qua kiểm thử.
2. Dùng Chrome/Edge trên máy host, mở http://localhost:3000 và đăng nhập teacher u05.teacher@school.test bằng mật khẩu hiện có. Không gửi mật khẩu/API key vào ảnh báo lỗi hoặc log.
3. Nếu app chưa chạy: kiểm tra terminal Orca có sẵn trước khi mở thêm. Backend dùng `.\scripts\run-backend.bat`, frontend `npm --prefix frontend run dev`. Chỉ một API/worker nhận job; không bật Docker worker cũ cùng host worker. Nếu thiếu hạ tầng/quyền, báo lỗi thật, không sửa credential.
4. Mở lớp U05-A (Software Requirement), ID4d18e255-1789-45d6-8fbb-351e2188f672. Dữ liệu hiện tại có1 sinh viên và nhiều bài tên dài, đủ test bố cục cơ bản; số lượng/trạng thái có thể thay đổi do bạn thao tác.
5. Trước khi test ghi trình duyệt, phiên bản, viewport CSS và zoom. Dùng100percent trước; sau đó1440px/1077px/390px CSS và zoom 200%. Ảnh PNG/DPR khác viewport CSS; có thể xem `window.innerWidth`/`innerHeight` trong DevTools.
6. Chỉ xem/tìm kiếm/đổi tab trong các ca mặc định. Không lưu/chốt/công bố bài thật. Các ca có ghi điểm là phần tùy chọn riêng với dữ liệu QA được xác nhận dùng thử.

Liên kết nhanh:

- Thông tin lớp: http://localhost:3000/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672?tab=info
- Sổ điểm: http://localhost:3000/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672?tab=gradebook
- Bài QA hiện có: http://localhost:3000/teaching/evaluations/826f5c29-b5d1-4a32-9999-9313c242d8d3

Bài QA trên đã được bạn công bố10.00 ở lần kiểm tra gần nhất; không chuyển ngược trạng thái để test. Bài gốc7ca08c41-e9d4-4738-946d-24c049e3d1e5 không được ghi đè. Không có dữ liệu phù hợp cho ca nào thì ghi `Chưa test — thiếu dữ liệu`, không đánh dấu đạt.

## 2. Sổ điểm — xem theo bài tập

| ID | Thao tác | Kết quả mong đợi | Kết quả của bạn |
|---|---|---|---|
| GB01 | Mở link sổ điểm mới hoặc reload. | Mặc định `Theo bài tập`, có bộ chọn bài và bảng Sinh viên/Điểm/Trạng thái/Thao tác; không trải mọi tên bài thành cột. | Chưa test |
| GB02 | Chọn bài tên dài, rồi chọn bài khác. | Tên đầy đủ đọc được ở bộ chọn/tiêu đề; điểm/trạng thái thuộc đúng bài, không lấy nhầm thứ tự cột. | Chưa test |
| GB03 | Chọn bài QA đã công bố. | Thấy điểm10.00/10.00 và `Đã công bố` nếu dữ liệu chưa đổi; không tự sửa trạng thái/điểm. | Chưa test |
| GB04 | Chọn bài chưa nộp/chưa có evaluation. | Hiện `—` và trạng thái đúng; không có link giả hoặc điểm0 thay cho chưa chấm. | Chưa test |
| GB05 | Với bài có điểm0 thực, xem ô điểm. | Hiện0.00/max, không thành `—`. Thiếu fixture thì ghi chưa test. | Chưa test |
| GB06 | Tìm bằng một phần tên, email, chuỗi không khớp; xóa tìm kiếm. | Kết quả đúng, có empty state khi không khớp, xóa tìm kiếm khôi phục danh sách; đổi query về trang1. | Chưa test |
| GB07 | Khi có trên 20 sinh viên, sang trang2, đổi bài rồi đổi chế độ. | Không mất query/trang do đổi bài/chế độ; paging không lặp/mất hàng. Lớp1 sinh viên không đủ chứng nhận ca này. | Chưa test |
| GB08 | Bấm xem/chấm trên một evaluation. | Đi đúng evaluation/bài/sinh viên; Back trở về được, không có mutation chỉ vì mở link. | Chưa test |

## 3. Sổ điểm — tổng hợp và xuất

| ID | Thao tác | Kết quả mong đợi | Kết quả của bạn |
|---|---|---|---|
| GB09 | Chuyển `Tổng hợp`, cuộn ngang bảng. | Cột sinh viên cố định, cột bài đều khoảng160px, điểm không xuống dòng, trang bên ngoài không bị kéo ngang. | Chưa test |
| GB10 | Đọc tiêu đề bài tên dài; mở `Danh mục bài tập`/cách xem đầy đủ được Gemini ghi trong report. | Đọc được tên đầy đủ bằng chuột, bàn phím và chạm; không bắt buộc hover. Số Bài1/Bài2 khớp đúng bài/link. | Chưa test |
| GB11 | Cuộn dọc trong bảng có nhiều sinh viên. | Header vẫn thấy, góc sinh viên/header không bị ô điểm che xuyên; không xuất hiện hai tên khác nhau ở cùng ô. | Chưa test |
| GB12 | Từ Tổng hợp chuyển về Theo bài tập. | Query/trang/bài đã chọn còn đúng, không nhảy ngẫu nhiên hoặc hiển thị điểm cũ trong lúc chuyển. | Chưa test |
| GB13 | Mở Xuất sổ điểm rồi Hủy/Đóng. | Không tải file, không thay đổi điểm; scope xuất được mô tả rõ, không tự hiểu là chỉ xuất trang tìm kiếm. | Chưa test |
| GB14 | Chọn CSV và phạm vi1bài, tải và mở file. | Đúng bài, cột/tên/điểm/status hợp lệ, UTF-8 tiếng Việt đọc được; không có điểm tổng/trọng số tự sinh. Xuất có audit theo cơ chế hiện tại. | Chưa test |
| GB15 | Chọn XLSX và Toàn bộ bài Graded, mở file. | Có các bài theo scope; bài chưa nộp/chưa chốt được ghi đúng, không lộ điểm nháp như đã công bố; Practice/Quiz không lẫn vào. | Chưa test |
| GB16 | Mở lớp khác rồi quay lại hoặc reload link tab. | Không giữ bài chọn từ lớp trước; không có URL/evaluation sai lớp. | Chưa test |

## 4. Thông tin lớp

| ID | Thao tác | Kết quả mong đợi | Kết quả của bạn |
|---|---|---|---|
| CL01 | Mở Thông tin lớp. | Header gọn có môn/mã lớp/trạng thái; hàng tóm tắt sinh viên/nhóm/bài đang mở/chờ chấm. Không còn panel lớn chỉ chứa vài dòng ở đầu. | Chưa test |
| CL02 | Đối chiếu Sinh viên với tab Sinh viên; Nhóm với tab Sinh viên/nhóm. | Số liệu cùng ngữ nghĩa; không cộng sinh viên đã rời lớp hoặc bịa0 khi chưa tải được dữ liệu. Số chưa có nhóm vẫn đọc được. | Chưa test |
| CL03 | Đối chiếu bài đang mở với tab Đánh giá và chờ chấm với danh sách bài nộp. | Số liệu thật nhất quán; không dùng điểm đã công bố để tính chờ chấm. Ghi lại giới hạn bài ẩn nếu có. | Chưa test |
| CL04 | Xem bố cục desktop. | Metadata khoảng1/3, tiến độ khoảng2/3; các card không cao/trống quá mức, không đè nhau. | Chưa test |
| CL05 | Với mô tả lớp dài, mở/thu gọn. | Đọc được toàn văn, không mất mã lớp/học kỳ/giảng viên; văn bản không thành HTML/script. | Chưa test |
| CL06 | Xem danh sách tiến độ, bấm Xem tất cả rồi Thu gọn. | Mặc định tối đa 5 bài; ưu tiên còn chờ chấm, rồi hạn gần; mở thêm thấy đủ bài, đóng không đổi số liệu. | Chưa test |
| CL07 | Xem một bài cá nhân và một bài nhóm. | Đơn vị đúng sinh viên/nhóm, submitted/total và phần trăm hợp lý, progress dùng màu theme; tổng0 không hiệnNaN/Infinity/100percent giả. | Chưa test |
| CL08 | Bấm Xem bài nộp trong một dòng tiến độ. | Đúng assignment/class, quay lại được. | Chưa test |
| CL09 | Để phần phân bố điểm đóng, rồi mở và chọn bài đã công bố. | Khi đóng không bung biểu đồ mọi bài. Mở mới tải bài chọn; đổi bài không hiển thị nhầm dữ liệu bài cũ. Có thể quan sát Network để xác nhận không gọi tất cả distribution lúc vào trang. | Chưa test |
| CL10 | Chọn bài không đủ dữ liệu công bố. | Thông báo chưa đủ dữ liệu/quyền riêng tư; không tạo biểu đồ0 hoặc suy ngược điểm từng người. Lớp chỉ1 sinh viên có thể không hiện phân bố do ngưỡng riêng tư. | Chưa test |
| CL11 | Chuyển qua Sinh viên/Học liệu/Đánh giá/Sổ điểm, rồi về Thông tin lớp. | Slot U12/U09 vẫn hiện đúng, không lặp panel/request, chức năng nhóm không biến mất. | Chưa test |
| CL12 | Dùng tài khoản student trong một phiên trình duyệt riêng nếu có. | Giao diện student vẫn đúng, không lộ tab/quyền teacher; không đăng xuất teacher ở tab đang test nếu chưa cần. | Chưa test |

## 5. Lỗi tải, bàn phím và responsive

| ID | Thao tác | Kết quả mong đợi | Kết quả của bạn |
| UX01 | Chỉ dùng Tab/Shift+Tab/Enter/Space để đổi chế độ, chọn bài, mở tên đầy đủ và phân bố. | Focus thấy rõ, thứ tự hợp lý, không mắc kẹt; trạng thái và label có chữ, không chỉ màu. | Chưa test |
| UX02 | Test1440px và 1077px CSS, zoom 100%. | Thanh công cụ wrap gọn, tên dài/điểm không đè nhau; cuộn bảng giữ cột/header. | Chưa test |
| UX03 | DevTools Responsive 390px CSS. | Theo bài tập dùng được, thông tin lớp xếp summary→progress→metadata; bảng tổng hợp chỉ cuộn trong vùng riêng; body không bị tràn ngang. | Chưa test |
| UX04 | Tắt Responsive, zoom browser200percent. | Text/controls không bị cắt/mất; có thể chạm/tab tới mọi action. Đây là test zoom thật, không thay bằng thu hẹp một div. | Chưa test |
| UX05 | DevTools Network Offline khi tải trang, sau đó Online rồi Retry. | Báo lỗi đọc được, không hiện0 như dữ liệu thật, retry phục hồi. Không gửi thao tác ghi điểm khi offline. Tắt Offline khi xong. | Chưa test |
| UX06 | Throttle mạng, đổi bài/tìm kiếm nhanh. | Kết quả cuối khớp lựa chọn mới nhất, không bị response chậm ghi đè. Khôi phục No throttling khi xong. | Chưa test |
| UX07 | Kiểm lớp chưa có bài/sinh viên nếu có fixture riêng. | Empty state có hướng dẫn, selector/export/pagination không lỗi; không cần xóa dữ liệu để tạo empty state. | Chưa test |

## 6. Hồi quy thông báo 4 giây — tùy chọn, chỉ dùng bài QA có trạng thái phù hợp

Không dùng bài thật. QA826f5c29... đã PUBLISHED nên không chốt lại được; điều đó là đúng. Nếu muốn test hai bước này, dùng một bài QA mới đã nộp và đang DRAFT, do bạn tạo/xác nhận là dữ liệu bỏ được. Không chỉnh trực tiếp trạng thái DB, không sửa bài gốc để ép thử. Công bố thực có thể tạo thông báo/email cho tài khoản test theo thiết lập hiện có.

| ID | Thao tác | Kết quả mong đợi | Kết quả của bạn |
|---|---|---|---|
| FB01 | Trên QA DRAFT bấm Chốt điểm, đọc xác nhận rồi hủy. | Không đổi trạng thái/điểm, không có thông báo thành công. | Chưa test |
| FB02 | Chốt QA DRAFT và xác nhận. | Sau server thành công hộp thoại đóng, thông báo nhỏ `Đã chốt điểm cho 1 bài` xuất hiện khoảng4 giây rồi tự ẩn. Trang thao tác được, sinh viên chưa thấy điểm do chốt. | Chưa test |
| FB03 | Công bố QA đã chốt, xác nhận. | Hiện thông báo nhỏ công bố thành công, tự ẩn4 giây; sinh viên được xem theo quyền. Không còn dialog `Bài 1: SUCCESS` bắt đóng thêm. | Chưa test |
| FB04 | Thử nút đóng thông báo trên một thao tác QA khác hợp lệ nếu cần. | Thông báo biến mất ngay, không gửi lại request. Không công bố thêm chỉ để chụp ảnh. | Chưa test |
| FB05 | Với fixture lỗi/partial do người phát triển cung cấp, không cố gây lỗi bằng bài thật. | Lỗi/partial vẫn có chi tiết bền, không báo thành công giả hoặc tự biến mất. Thiếu fixture thì ghi chưa test; phần này có automated tests. | Chưa test |

## 7. Cách báo kết quả

Gửi danh sách ID đạt/lỗi/chưa test. Mỗi lỗi ghi:

- ID ca test, URL/tab, trình duyệt, viewport CSS, zoom.
- Dữ liệu bài/sinh viên đang chọn và trạng thái trước thao tác.
- Các bước tái hiện ngắn; mong đợi so với thực tế.
- Ảnh toàn vùng lỗi, lỗi console hoặc HTTP status/code nếu có. Che mật khẩu, API key, token/cookie và dữ liệu không cần thiết.
- Với thao tác ghi QA: ghi trạng thái sau thao tác, có gửi thông báo/email hay không. Không xóa hay reset QA trước khi lưu bằng chứng.

Mẫu: `GB09 — Chrome,1077x900 CSS,100percent — chọn Tổng hợp, cuộn ngang — mong cột sinh viên đứng yên; thực tế cột trôi — ảnh ...`.

Gemini chỉ báo cáo code và kiểm tra tự động. Bạn đánh dấu kết quả UI; những ca thiếu dữ liệu/quyền/tool phải để Chưa test cùng lý do. Nếu có lỗi, gửi ID và bằng chứng để sửa đúng phạm vi. Không tự commit/push/merge khi chưa yêu cầu tiếp.
