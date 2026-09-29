# Làm rõ thay đổi role, assignment và credit (2026-09-29)

Các quyết định đã rõ từ yêu cầu: đổi tên `Learner` thành `Student`, `Instructor` thành `Teacher`; Student có thể mua credit và chủ động dùng credit của mình để nhờ AI phản hồi/chấm bài `PRACTICE`; Teacher không chấm hoặc công bố điểm chính thức cho bài `PRACTICE`; `GROUP_ASSIGNMENT` chỉ có chế độ `GRADED`. Các câu hỏi dưới đây chỉ xử lý phần còn nhiều cách hiểu. Điền chữ lựa chọn sau mỗi `[Answer]:`; chọn `X` nếu cần cách khác.

## Question 1
`Diagram Essay` trong danh sách sáu loại sẽ thay thế bài `DOCUMENT` hiện tại như thế nào?

A) Tách thành loại `DIAGRAM_ESSAY` độc lập; giữ `TEXT_ESSAY` là văn bản, và chuyển chức năng vẽ Draw.io hiện tại sang loại sơ đồ.

B) Giữ nền tảng `DOCUMENT` hiện tại và dùng tên `DIAGRAM_ESSAY` cho bài tài liệu có sơ đồ Draw.io nhúng; `TEXT_ESSAY` vẫn là loại riêng.

X) Other (mô tả sau `[Answer]:`)

[Answer]: b

## Question 2
Trong hình, `Multiple-Choice Quiz` có các nguồn `Self`, `SM` và `Teacher`. `Self` có nghĩa gì?

A) Student tự tạo quiz từ câu hỏi/nội dung của mình để luyện tập; Subject Manager tạo quiz `PRACTICE` hoặc `GRADED`; Teacher tạo quiz `GRADED`.

B) Student chỉ tự chọn và làm quiz luyện tập từ ngân hàng câu hỏi có sẵn; Subject Manager tạo quiz `PRACTICE` hoặc `GRADED`; Teacher tạo quiz `GRADED`.

C) `Self` chỉ có nghĩa Student tự làm quiz luyện tập đã được Teacher hoặc Subject Manager tạo, không tự tạo hoặc tự chọn câu hỏi.

X) Other (mô tả sau `[Answer]:`)

[Answer]:c

## Question 3
AI chấm `PRACTICE` theo yêu cầu của Student áp dụng cho những loại nào và kết quả được dùng ra sao?

A) Áp dụng cho Code Lab, Text Essay, Diagram Essay, Simulation Exam và Quiz; kết quả chỉ là phản hồi/điểm luyện tập riêng tư, không vào sổ điểm chính thức.

B) Chỉ áp dụng cho Text Essay và Diagram Essay; Code Lab/Quiz/Simulation Exam dùng bộ test hoặc đáp án tự chấm, không tiêu credit AI của Student.

X) Other (mô tả sau `[Answer]:`)

[Answer]:b

## Question 4
`Simulation Exam: full exam, practice` trong hình tương ứng với chế độ điểm nào?

A) `FULL_EXAM` là một `GRADED` simulation exam được tính vào điểm thành phần; `PRACTICE` chỉ luyện tập, không vào sổ điểm.

B) `FULL_EXAM` là một loại/chế độ thi riêng ngoài simulation exam, có quy trình và điểm chính thức riêng; `PRACTICE` là thi thử.

X) Other (mô tả sau `[Answer]:`)

[Answer]: bỏ simulation exam đi

## Question 5
Với bài `PRACTICE`, Student có thể làm lại và nhờ AI chấm nhiều lần như thế nào?

A) Teacher cấu hình hạn và số lượt khi phát hành; mỗi lần Student yêu cầu AI chấm một bài nộp thì trừ credit theo token dùng thực tế, không chấm tự động nếu Student chưa yêu cầu.

B) Bài luyện tập không giới hạn lượt hoặc hạn; Student có thể yêu cầu AI chấm lại cùng một bài nộp nhiều lần, mỗi lần đều tính credit.

X) Other (mô tả sau `[Answer]:`)

[Answer]:  mỗi 1 lần submit thì gọi chấm 1 lần, nếu làm lại thì chấm lần thứ 2
