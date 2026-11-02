# Góp ý Quiz, Assignment, chấm bài, bài nhóm — 2026-10-29

Ghi lại góp ý (một phần theo thầy Kiên) để chuẩn bị làm lại phần Quiz (nằm trong Learning Material) và Assignment. Đây là danh sách việc cần làm và câu hỏi còn mở, chưa phải thiết kế đã chốt. Mỗi mục ghi unit dự kiến bị ảnh hưởng.

## 1. Learning Material — U05

- [ ] Chia tài liệu theo module (nhóm tài liệu theo từng module/chương thay vì một danh sách phẳng).
- Quiz sẽ nằm trong Learning Material (xem mục 2).

## 2. Quiz — U10 (soạn), U11 (làm), U15 (chấm/xem)

- [ ] Phần "Tài liệu tham khảo" trong Quiz có vẻ thừa → xem lại, có thể bỏ.
- [ ] **Quiz config (sinh đề):** khi sinh viên bắt đầu làm thì hệ thống sinh đề mới, không cố định bộ câu hỏi. Người tạo cấu hình theo tag, ví dụ: tag 1 lấy 4 câu, tag 2 lấy 5 câu, tag 3 lấy 7 câu.
- [ ] **Chống gian lận:** dùng JS ghi số lần sinh viên rời tab (out tab).
  - Hạn chế đã biết: sinh viên có thể tắt JS / chặn script, nên đây chỉ là tín hiệu, không phải bằng chứng.
  - [ ] Cho cấu hình **bao nhiêu lượt out tab thì trừ bao nhiêu điểm**.
  - ⚠ Mâu thuẫn với quyết định cũ FR-039 (council-features-2026-10-13, mục 2: "không tự trừ điểm, không kết luận gian lận"). Cần chốt lại trước khi làm.
- [ ] Hiển thị câu hỏi **mỗi câu một trang**; màn xem kết quả cũng mỗi câu một trang.
- [ ] **Ngân hàng câu hỏi:** thêm cơ chế chống trùng câu hỏi, hoặc cho chủ nhiệm môn duyệt khi có câu hỏi mới vào ngân hàng.
- [ ] **Bước duyệt hiện tại đang thừa.** Chỉ giữ duyệt cho một trường hợp: giảng viên đưa câu hỏi vào ngân hàng câu hỏi → chủ nhiệm môn duyệt.
- [ ] **Tạo đề bằng AI:** không dùng popup (hoặc nếu giữ popup thì phải kéo rộng ra).

## 3. Assignment — U07 (soạn), U08 (làm)

- [ ] Assignment có thể có **tiêu chí chung** (áp cho cả bài) → cần thêm.
- [ ] Màn tạo đề chuyển thành **trang phẳng (flat page)**, giảng viên gõ và sửa trực tiếp trên trang.
- [ ] Nội dung do **AI sinh hiện màu vàng**; khi người dùng sửa phần đó thì trở về màu bình thường, để phân biệt rõ phần AI sinh và phần người viết.
- [ ] Khi tạo đề có thể tạo **câu trả lời mẫu**.
- [ ] Giao diện hiện khó dùng → chuyển hết thành flat page.
- [ ] **Cấu hình cách dùng AI một lần:** người dùng cài đặt trước dùng nguồn AI nào (API hệ thống / API cá nhân / Agent MCP), không bắt chọn lại mỗi lần dùng chức năng AI.

## 4. Chấm bài — U09, U15

- [ ] Bỏ popup duyệt đề xuất của AI khi chấm bài.
- [ ] Tiêu chí chấm không đặt bên cạnh bài làm của sinh viên nữa (đổi bố cục).
- [ ] Thêm **tìm kiếm** và **chuyển nhanh** giữa bài làm của các sinh viên trong màn chấm bài.

## 5. Bài nhóm — U12, U14

- [ ] Hiện tại một thành viên nhận phần thì người khác, kể cả nhóm trưởng, không sửa được → không nên như vậy.
  - Hướng đề xuất: thành viên nhận phần thoải mái, **nhóm trưởng chốt phần**; và/hoặc thêm **lịch sử thay đổi**.
- [ ] Câu hỏi mở: có nên thêm **đánh giá chéo giữa các thành viên**?
  - Nếu có, điểm bài có thành % điểm giảng viên + % điểm thành viên đánh giá không?
  - Thực tế ASIS: 3 đầu điểm — điểm tài liệu, điểm thành viên nhóm cho nhau, điểm presentation (giống Edunext).
- [ ] **Nộp file rồi hệ thống bóc tách:** vẫn cho nộp file; hệ thống tách nội dung và AI điền vào từng ô.
  - Ảnh có thể mapping vào ô tương ứng.
  - Sinh viên nộp 2 file: 1 file Word, 1 file diagram; quy ước mỗi page trong file Draw.io tương ứng một diagram.
  - Cảnh báo người dùng là việc này có thể tốn nhiều token.

## 6. Chung

- [ ] Giao diện có cả **tiếng Anh và tiếng Việt**.
- [ ] Làm **tài liệu hướng dẫn sử dụng**.

## Câu hỏi cần chốt trước khi làm

1. Trừ điểm theo số lần out tab có thay quyết định FR-039 "không tự trừ điểm" không?
2. Quiz sinh đề theo tag: lấy ngẫu nhiên từ ngân hàng câu hỏi của môn hay của riêng quiz? Không đủ câu theo tag thì xử lý thế nào?
3. Chống trùng câu hỏi: tự phát hiện trùng (so khớp nội dung / AI) hay chỉ dựa vào chủ nhiệm môn duyệt?
4. Đánh giá chéo trong nhóm: có làm không, và tỷ lệ điểm giảng viên / thành viên / presentation là bao nhiêu?
5. Song ngữ: áp cho toàn bộ giao diện ngay hay làm sau phần Quiz/Assignment?
