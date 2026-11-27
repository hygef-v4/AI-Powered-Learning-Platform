# U12 — Nhập danh sách nhóm từ Excel / Google Sheets: câu hỏi làm rõ

Yêu cầu (2026-11-27): thẻ "Sinh viên tự chia nhóm" ở tab Sinh viên của giảng viên có thêm nhập danh sách nhóm từ file Excel theo mẫu tải về, hoặc tạo link Google Sheets để sinh viên tự điền.

Hiện trạng đối chiếu từ mã:

- Lưu bộ nhóm dùng một lõi chung có `version` (BR-U12-02, 04): tên nhóm ≤ 100 ký tự, không trùng; mỗi sinh viên tối đa một nhóm; mỗi nhóm đúng một trưởng nhóm. Chia ngẫu nhiên (BR-U12-03) đã có bước xem trước, sửa rồi mới lưu.
- Nhập Excel ghi danh (U05) đọc file `.xlsx` ở trình duyệt bằng SheetJS, mẫu tĩnh tải từ `/templates/enrollment-import.xlsx`; server kiểm lại từng dòng.
- Backend có Apache POI và tích hợp Google Drive của U03 (service account hoặc OAuth scope `drive.file`), nhưng chưa dùng Google Sheets API.

Điền chữ cái vào sau mỗi `[Answer]:`. Có thể ghi thêm ý sau chữ cái.

Người dùng 2026-11-27: "Theo khuyến nghị", sau đó "bỏ tính năng google sheets, cho dùng import excel".

## Question 1
Mỗi dòng trong file mẫu chứa gì?

A) Mỗi dòng một sinh viên: `Tên nhóm`, `MSSV hoặc email`, `Trưởng nhóm` (đánh x). Các dòng cùng tên nhóm thành một nhóm. Dễ điền chung trên Google Sheets (khuyến nghị)

B) Mỗi dòng một nhóm: `Tên nhóm`, `Trưởng nhóm`, `Thành viên 2`, `Thành viên 3`… (mỗi cột một MSSV)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
Mẫu tải về có điền sẵn sinh viên của lớp không?

A) Có: mẫu sinh theo lớp, điền sẵn MSSV, họ tên của sinh viên `ACTIVE` chưa có nhóm; người điền chỉ ghi cột `Tên nhóm` và đánh dấu trưởng nhóm (khuyến nghị, tránh gõ sai MSSV)

B) Không: mẫu trống chung cho mọi lớp (file tĩnh như mẫu ghi danh), người điền tự nhập MSSV

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
Nhập xong thì danh sách thành gì?

A) Nhóm chính thức: hiện bản xem trước có báo lỗi từng dòng, giảng viên sửa rồi bấm Lưu, đi qua lõi lưu bộ nhóm như chia ngẫu nhiên (khuyến nghị)

B) Đề xuất nhóm ở trạng thái Chờ duyệt (`SUBMITTED`), giảng viên duyệt từng đề xuất ở mục "Chờ duyệt" như hiện nay

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Lớp đã có nhóm chính thức thì xử lý thế nào?

A) Chỉ thêm nhóm mới. Sinh viên đã có nhóm thì dòng đó báo lỗi và bị bỏ qua; tên trùng nhóm đã có thì báo lỗi (khuyến nghị, không đụng tài liệu nhóm đã có)

B) Cho phép thêm người vào nhóm đã có nếu tên trong file trùng tên nhóm đó; sinh viên đã ở nhóm khác vẫn báo lỗi

C) Thay toàn bộ bộ nhóm theo file; nhóm đã có tài liệu hoặc bản nộp không xóa được thì báo lỗi (BR-U12-05)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
Sĩ số tối thiểu, tối đa của đợt tự chia nhóm có áp dụng cho file nhập không?

A) Chỉ cảnh báo trên bản xem trước, giảng viên vẫn lưu được (giống cảnh báo sĩ số khi duyệt, BR-U12-73) (khuyến nghị)

B) Chặn: nhóm ngoài khoảng sĩ số không lưu được

C) Không xét sĩ số

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
Nhóm trong file không đánh dấu trưởng nhóm (hoặc đánh dấu nhiều người) thì sao?

A) Báo lỗi nhóm đó trên bản xem trước; giảng viên chọn trưởng nhóm ngay trên bản xem trước rồi mới lưu được (khuyến nghị)

B) Tự chọn người đầu tiên trong file làm trưởng nhóm

C) Tự chọn ngẫu nhiên như chia ngẫu nhiên

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 7
Google Sheets làm theo cách nào?

A) Giảng viên tự tạo Google Sheet từ mẫu (hoặc bấm "Mở mẫu trong Google Sheets" để sao chép), chia sẻ cho sinh viên, rồi dán link vào hệ thống và bấm "Đọc từ Google Sheets". Hệ thống đọc bản xuất CSV của sheet (sheet cần để "Bất kỳ ai có đường liên kết đều xem được"), sau đó đi cùng bước xem trước như Excel. Không cần thêm quyền Google mới (khuyến nghị)

B) Hệ thống tự tạo Google Sheet trên Drive của nền tảng (thêm Google Sheets API vào tích hợp Drive của U03), điền sẵn danh sách lớp, trả link chỉnh sửa để giảng viên gửi sinh viên; sau đó giảng viên bấm "Đồng bộ". Cần cấu hình thêm trên mọi môi trường và sheet nằm trên Drive của nền tảng

C) Làm Excel trước; Google Sheets để một lần thay đổi sau

X) Other (please describe after [Answer]: tag below)

[Answer]: X — Bỏ Google Sheets, chỉ nhập Excel (người dùng 2026-11-27: "bỏ tính năng google sheets, cho dùng import excel")

## Question 8
Lưu ý quyền riêng tư: sheet chia sẻ bằng link sẽ lộ MSSV, họ tên cho ai có link. Chấp nhận mức nào?

A) Chấp nhận; trên giao diện nhắc giảng viên chỉ chia sẻ cho email trường hoặc lớp, và mẫu chỉ có MSSV, họ tên (không có email, điểm) (khuyến nghị)

B) Mẫu Google Sheets không điền sẵn họ tên, chỉ có cột trống để sinh viên tự ghi MSSV

X) Other (please describe after [Answer]: tag below)

[Answer]: Không áp dụng vì bỏ Google Sheets; file Excel chỉ nằm ở máy giảng viên nên mẫu có MSSV, email, họ tên để nhận diện

## Question 9
Nút nhập đặt ở đâu, và có cần đợt tự chia nhóm đang bật không?

A) Trong thẻ "Sinh viên tự chia nhóm" cạnh nút Cài đặt, luôn dùng được ở lớp `DRAFT`/`OPEN` dù đợt tự chia nhóm bật hay tắt (khuyến nghị)

B) Trong thẻ "Nhóm" (cạnh Tạo nhóm, Chia ngẫu nhiên)

C) Trong thẻ "Sinh viên tự chia nhóm", chỉ hiện khi đợt đang bật

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 10
Sinh viên đang ở đề xuất nhóm (nháp hoặc chờ duyệt) mà file xếp vào nhóm khác thì sao?

A) Lưu nhóm chính thức theo file; đề xuất của họ được xử lý như khi giảng viên lưu bộ nhóm (BR-U12-88: rời đề xuất, trưởng nhóm thì đề xuất bị hủy). Bản xem trước cảnh báo trước khi lưu (khuyến nghị)

B) Báo lỗi dòng đó, không xếp sinh viên đang ở đề xuất

X) Other (please describe after [Answer]: tag below)

[Answer]: A
