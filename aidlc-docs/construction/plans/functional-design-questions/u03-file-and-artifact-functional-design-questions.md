# U03 File & Artifact - Câu hỏi Functional Design

Hỏi qua giao diện chọn đáp án.

## Câu 1 - Nơi lưu file

A) Google Drive Shared Drive.

B) Ổ đĩa VPS.

C) MinIO trên VPS.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 2 - Cách upload

A) Gửi qua backend trong một request.

B) Hai bước: xin phiên rồi upload thẳng.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 3 - Cách tải về

A) Link tạm qua backend, hạn 5 phút.

B) Backend stream trực tiếp mỗi lần tải.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 4 - Giới hạn kích thước

A) Theo mục đích.

B) Chung 10 MB.

C) Chung 50 MB.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: C

## Câu 5 - Loại file

A) Allowlist theo mục đích.

B) Allowlist chung.

C) Mọi loại trừ file chạy được.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 6 - Bản Draw.io XML rút gọn cho AI

A) Giữ 7 ngày.

B) Xóa ngay sau khi job AI xong.

C) Giữ vĩnh viễn.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu 7 - File tải lên nhưng bỏ ngang

A) Xóa sau 24 giờ.

B) Giữ lại.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: X - "nếu bỏ ngang thì xoá luôn, chỉ up full thành công thì mới tính"; hiểu là: upload lỗi/ngắt giữa chừng thì xóa ngay, upload đủ mới thành artifact và được giữ
