# Học kỳ của lớp — câu hỏi xác nhận

Câu hỏi 1–3 đưa ra trong chat ngày 2026-11-14; người dùng trả lời trong chat lúc 2026-11-14T15:32:28Z (nguyên văn trong `aidlc-docs/audit.md`). Câu 4–8 người dùng trả lời "Theo khuyến nghị" lúc 2026-11-14T15:44:43Z.

Mục đích: lớp có học kỳ để **lọc lớp theo kỳ**. Không thêm chức năng quản lý học kỳ (không danh mục, không màn hình tạo/sửa học kỳ). Quyết định này sẽ thay thế D-U04-04 (bỏ học kỳ khỏi lớp, 2026-11-07); migration cũ giữ nguyên, thêm migration mới.

## Q1 — Định dạng mã học kỳ

A) Mã chuẩn `YYYY-YYYY.N`

B) Chữ tự do

X) Khác

[Answer]: X — Mã là 1 trong 3 kỳ SP/SU/FA + 2 chữ số biểu thị năm (VD: Spring 2027 là SP27).

## Q2 — Học kỳ bắt buộc hay tùy chọn

A) Bắt buộc khi tạo lớp mới, lớp cũ được để trống

B) Tùy chọn hoàn toàn

X) Khác

[Answer]: X — Học kỳ tự động có theo thời gian lớp bắt đầu: tháng 1–4 là SP, tháng 5–8 là SU, tháng 9–12 là FA. Lớp bắt đầu vào tháng 4, 8, 12 có thêm hậu tố `BL5` sau mã kỳ, ngăn cách bằng dấu `-` (lớp block 5). Ví dụ lớp bắt đầu ngày 3/4/2027 có mã học kỳ `SP27-BL5`.

## Q3 — Mã lớp có được dùng lại giữa các kỳ

A) Không — giữ `UNIQUE (subject_id, code)`

B) Có — đổi thành `UNIQUE (subject_id, term, code)`

X) Khác

[Answer]: B

## Quy tắc tính học kỳ (tổng hợp từ Q1–Q2)

- Học kỳ không nhập tay; hệ thống tính từ ngày bắt đầu của lớp và lưu vào cột `term` để lọc, đánh index và ràng buộc duy nhất.
- Mã = kỳ (`SP`, `SU`, `FA`) + 2 chữ số cuối của năm dương lịch của ngày bắt đầu; thêm `-BL5` nếu tháng bắt đầu là 4, 8 hoặc 12. Mẫu: `^(SP|SU|FA)[0-9]{2}(-BL5)?$`, tối đa 8 ký tự.
- Tháng được xác định theo giờ Việt Nam (`Asia/Ho_Chi_Minh`), giống các phần tính theo ngày khác của hệ thống.

| Ngày bắt đầu | Học kỳ |
|---|---|
| 2027-01-10 | SP27 |
| 2027-04-03 | SP27-BL5 |
| 2027-05-15 | SU27 |
| 2027-08-20 | SU27-BL5 |
| 2026-09-07 | FA26 |
| 2026-12-01 | FA26-BL5 |

- Lớp `SP27` và `SP27-BL5` là hai giá trị học kỳ khác nhau khi lọc và khi xét trùng mã lớp.

## Q4 — "Thời gian lớp bắt đầu" lấy từ đâu

Hiện lớp chưa có ngày bắt đầu, chỉ có thời điểm tạo (`created_at`).

A) Thêm trường **Ngày bắt đầu** (chỉ ngày, bắt buộc) vào form tạo lớp; học kỳ tính từ ngày này. (Khuyến nghị: tạo lớp trước cho kỳ sau vẫn ra đúng học kỳ.)

B) Dùng ngày tạo lớp; không thêm ô nhập.

X) Khác (mô tả sau [Answer]:)

[Answer]: A (theo khuyến nghị)

## Q5 — Sửa ngày bắt đầu sau khi tạo lớp (nếu chọn Q4-A)

A) Sửa được khi lớp chưa lưu trữ; học kỳ tự tính lại. Nếu học kỳ mới trùng mã lớp đã có trong môn thì báo lỗi "Mã lớp đã tồn tại trong học kỳ". (Khuyến nghị)

B) Chỉ sửa được khi lớp còn Nháp

C) Không sửa được sau khi tạo

X) Khác (mô tả sau [Answer]:)

[Answer]: A (theo khuyến nghị)

## Q6 — Các lớp đã có (chưa có ngày bắt đầu)

A) Migration gán ngày bắt đầu = ngày tạo lớp (giờ Việt Nam) và tính học kỳ cho mọi lớp cũ; không còn lớp thiếu học kỳ. (Khuyến nghị)

B) Để trống ngày bắt đầu và học kỳ; bộ lọc có mục "Chưa có học kỳ"; trưởng môn tự bổ sung sau.

X) Khác (mô tả sau [Answer]:)

[Answer]: A (theo khuyến nghị)

## Q7 — Luật "sinh viên không ở hai lớp cùng môn"

Hiện khi mở lớp, hệ thống chặn nếu có sinh viên đang ghi danh ở lớp khác **cùng môn chưa lưu trữ** (`ENROLLMENT_CONFLICT`).

A) Giữ nguyên: lớp kỳ trước phải được lưu trữ thì sinh viên mới học lại ở kỳ sau. (Khuyến nghị — không đổi luật hiện có.)

B) Chỉ chặn khi hai lớp cùng học kỳ; sinh viên được ở lớp cùng môn của kỳ khác dù lớp cũ chưa lưu trữ.

X) Khác (mô tả sau [Answer]:)

[Answer]: A (theo khuyến nghị)

## Q8 — Hiển thị học kỳ ở đâu

Vì mã lớp giờ có thể lặp giữa các kỳ, nhãn chỉ có mã môn và mã lớp (ô chuyển lớp trên sidebar, ô lọc lớp ở Thông báo/Bài tập) có thể hiện hai lớp trùng tên.

A) Hiện học kỳ trong danh sách lớp, chi tiết lớp **và** cạnh mã lớp ở mọi nhãn chọn lớp (sidebar, ô lọc lớp). (Khuyến nghị)

B) Chỉ hiện trong danh sách lớp và chi tiết lớp.

X) Khác (mô tả sau [Answer]:)

[Answer]: A (theo khuyến nghị)

## Mặc định chưa hỏi (đổi được nếu cần)

- Bộ lọc học kỳ đặt cạnh tab trạng thái ở trang Lớp học (trưởng môn, giảng viên, sinh viên); lựa chọn mặc định "Tất cả học kỳ"; các kỳ khác lấy từ học kỳ của những lớp đang hiển thị, kỳ mới nhất lên trước; giữ lựa chọn trên URL.
- Ba API danh sách lớp nhận thêm tham số tùy chọn `term`.
