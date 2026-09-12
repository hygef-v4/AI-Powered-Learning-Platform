# Câu hỏi làm rõ kế hoạch User Stories

## Vấn đề cần làm rõ

Câu trả lời cho Question 2 bổ sung **Chủ nhiệm môn (Subject Manager)** với quyền quản lý học liệu/RAG và phát hành đề bài xuyên các lớp thuộc môn. Đây là thay đổi nghiệp vụ so với requirements hiện tại, vốn chỉ định nghĩa ba vai trò và chưa có thực thể/quyền cấp môn. Các quyết định dưới đây cần được xác nhận trước khi kế hoạch User Stories được phê duyệt.

Vui lòng điền một chữ cái sau mỗi thẻ `[Answer]:`. Nếu chọn `X`, ghi thêm mô tả ngay sau chữ cái.

## Question 1
“Chủ nhiệm môn” nên được mô hình hóa trong phân quyền như thế nào?

A) Vai trò thứ tư riêng biệt, được kiểm tra phía server; cập nhật Requirements để bổ sung role và quyền cấp môn

B) Một giảng viên được gán thêm quyền/phạm vi quản lý môn, không tạo role RBAC thứ tư cố định

C) Chỉ là persona nghiệp vụ để mô tả trách nhiệm, không có quyền hệ thống khác giảng viên

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]:a

## Question 2
Quyền phát hành đề bài chung của Chủ nhiệm môn nên tương tác với giảng viên đứng lớp như thế nào?

A) Chủ nhiệm môn biên soạn và phát hành trực tiếp cho mọi lớp thuộc môn; giảng viên đứng lớp không cần duyệt lại

B) Chủ nhiệm môn phát hành mẫu đề chung; giảng viên đứng lớp phải duyệt trước khi giao cho lớp mình

C) Chủ nhiệm môn quản lý kho đề dùng chung; giảng viên đứng lớp tự chọn và phát hành cho lớp mình

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]:a

## Question 3
Ranh giới quản lý học liệu giữa Chủ nhiệm môn và giảng viên đứng lớp nên là gì?

A) Chủ nhiệm môn quản lý kho học liệu/RAG cấp môn; giảng viên vẫn quản lý nội dung riêng của lớp mình trong phạm vi được phép

B) Chỉ Chủ nhiệm môn được thay đổi học liệu/RAG cấp môn; giảng viên chỉ sử dụng nội dung đã phát hành và quản lý hoạt động lớp

C) Chủ nhiệm môn và mọi giảng viên của môn có cùng quyền chỉnh sửa kho học liệu/RAG cấp môn

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]:a
