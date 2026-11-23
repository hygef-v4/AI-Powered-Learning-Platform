# Bảng điểm theo sinh viên và bài tập — kết quả kiểm tra

Ngày: 2026-11-11. Requested by: Quang Hung. Triển khai trên `fix/u05-u09-u15`, chưa commit/push/deploy.

## Thay đổi

- Đọc file mẫu `Done-FA26 - MLN131 - SE1925-JS(IT).xlsx` cục bộ, không sửa dữ liệu gốc. Dùng cấu trúc một hàng/sinh viên, cột theo bài tính điểm; không suy ra mã sinh viên, trọng số hoặc điểm tổng kết.
- Trang Chấm điểm đổi nút Xuất bảng điểm thành Bảng điểm. Chế độ `gradebookView=matrix` trong trang lớp có chọn một bài/toàn bộ, tìm sinh viên, phân trang20 hàng, link xem chi tiết và xuất tệp. Quay lại giữ các bộ lọc chấm trước đó.
- XLSX/CSV dùng STT, Họ và tên, Email, tên từng bài kèm điểm tối đa. Xuất toàn bộ sinh viên trong phạm vi bài, không giới hạn theo trang/tìm kiếm. Bài nhóm tái sử dụng điểm riêng từng thành viên; điểm0 giữ nguyên, chưa chấm/chưa nộp để trống.
- XLSX có điểm kiểu số, văn bản an toàn, tiêu đề đậm/xuống dòng, độ rộng cột, bộ lọc và cố định D2. Không đổi endpoint, schema, dependency hoặc chính sách sinh viên chỉ xem điểm công bố.

## Kiểm tra tự động

- RED:3 test ma trận frontend thất bại trước triển khai;4 kỳ vọng cấu trúc hàng backend thất bại trên cấu trúc cũ. Test XLSX mới thất bại khi writer vẫn dùng toàn bộ ô chuỗi.
- GREEN: toàn bộ frontend **970/970**,137 tệp. Sau điều chỉnh CSS cuối, chạy lại hai tệp giao diện liên quan **19/19**; TypeScript, full ESLint và scoped lint cuối đều qua; production build cuối thành công32 trang.
- Backend phần grading **216/216**, gồm PostgreSQL integration, không bỏ qua. Full Maven `verify` với heap768MB: **2.153/2.153**, không lỗi/bỏ qua, đóng gói thành công. Guard bắt buộc: Subject31, Notifications7, EvaluationVersion11, Distribution1, PlatformWorkflow26;57 ca traceability qua.
- Hai ca kiểm tra xuất sai assignment/thiếu quyền và assertion xuất điểm riêng thành viên được bổ sung sau full verify; chạy lại Assembly/Exporter/Controller **19/19** trên test cuối. Không cộng lượt này vào số full verify.
- Dùng writer Java đã biên dịch tạo workbook kiểm tra riêng trong `.tmp`, đọc lại bằng openpyxl: điểm0/8.25 là số, tên bắt đầu `=SUM` vẫn là chuỗi, ô chưa chấm trống, header đậm/wrap, freeze D2, filter A1:E3. Test writer kiểm tra thêm cột AB, escape XML và không có công thức.
- `git diff --check` qua. Các log local ở `.tmp/matrix-*`, không đưa vào Git.

## Trình duyệt và giới hạn

- Orca browser, localhost3000, tài khoản teacher hiện có, lớp SWD392/SE1933: mở Bảng điểm từ nút thực, kiểm tra5 sinh viên/5 cột bài, điểm nhóm10 và điểm0; chọn riêng bài, tìm theo email trả đúng1 sinh viên; popup xuất mặc định đúng phạm vi/Excel. Không thay đổi điểm hay dữ liệu lớp.
- Kiểm ảnh1440×1000 và390×844, hai giao diện sáng/tối. Bảng cuộn ngang trong khung; chiều rộng document bằng390px ở màn390px. Đã cuộn ngang và đo vị trí header/cột sinh viên vẫn cố định khớp nhau.
- Download thật trả200 và giữ tên assignment, nhưng backend đang chạy vẫn xuất **cấu trúc cũ9 cột**, chưa nạp mã mới. Không restart backend: thao tác này từng bị automatic approval review chặn trong phiên. Cần người dùng restart backend trước khi kiểm chứng download mới xuyên suốt UI/API; không gọi đây là kiểm thử live export mới đã qua.
- Không kiểm thử Excel desktop/VPS, không tính trung bình/trọng số hay nhập dữ liệu từ file mẫu. Cảnh báo CSP inline-style từ trang có sẵn vẫn xuất hiện; không nới CSP để xử lý việc xuất bảng điểm.

Security/Resiliency: giữ quyền đọc/xuất theo lớp, kiểm tra assignment thuộc phạm vi, receipt/member override, chỉ xuất điểm đã lưu, audit, CSV formula neutralization và XLSX text escaping. Không lộ bản nháp qua student endpoint. PBT disabled theo cấu hình; các quy tắc khác N/A trong thay đổi này.
