# Câu hỏi làm rõ thay đổi yêu cầu ngày 2026-09-22

> Tài liệu là lịch sử làm rõ. Với YouTube, quyết định hiện hành là chỉ dùng caption có sẵn, không tự phiên âm; xem `requirements.md` FR-004.

Các câu hỏi dưới đây chốt những quyết định có ảnh hưởng đến tính công bằng của bài đánh giá, versioning và cách tính điểm. Phương án A ở mỗi câu là đề xuất mặc định của nhóm phân tích.

## Question 1
Sau khi đề đã publish nhưng vẫn còn trong thời hạn làm/nộp bài, việc sửa câu hỏi nên tác động đến sinh viên như thế nào?

A) Không thay đổi snapshot của lượt làm đã bắt đầu; chỉnh sửa tạo version mới và chỉ áp dụng cho lượt làm bắt đầu sau khi version mới được phát hành. Nếu thay đổi lớn, giảng viên phải gia hạn hoặc cho làm lại. (Đề xuất)

B) Cho sửa trực tiếp và áp dụng ngay cho mọi sinh viên chưa nộp, kể cả lượt làm đang diễn ra.

C) Chỉ cho sửa khi chưa có sinh viên nào bắt đầu làm; sau khi có lượt làm đầu tiên thì khóa hoàn toàn đến hết hạn.

D) Chỉ cho sửa khi chưa publish; sau khi publish luôn phải tạo một assignment mới.

E) Other (please describe after [Answer]: tag below)

[Answer]: a

## Question 2
Đề mẫu do Trưởng môn cung cấp cho giảng viên nên có cơ chế sở hữu và cập nhật nào?

A) Trưởng môn phát hành template chỉ đọc theo version; giảng viên copy thành đề riêng của lớp rồi toàn quyền sửa bản copy. Template cập nhật không tự ghi đè đề đã copy. (Đề xuất)

B) Trưởng môn phát hành một đề dùng chung; giảng viên được tạo biến thể liên kết và có thể nhận cập nhật có chọn lọc từ đề gốc.

C) Giảng viên sửa trực tiếp đề mẫu dùng chung; thay đổi có thể ảnh hưởng các lớp khác.

D) Other (please describe after [Answer]: tag below)

[Answer]: a

## Question 3
Thi thử (simulation exam) cần tính số lượt và hiển thị kết quả theo chính sách nào?

A) Giảng viên cấu hình số lượt tối đa, cửa sổ thời gian, cách lấy kết quả (cao nhất/gần nhất/trung bình); thi thử không vào điểm chính thức và có thể hiển thị đáp án sau mỗi lượt hoặc sau khi đóng kỳ thi. (Đề xuất)

B) Chỉ cấu hình số lượt; luôn lấy lượt cao nhất để tham khảo và hiển thị đáp án ngay sau mỗi lượt.

C) Mỗi đề thi thử chỉ có một lượt, hiển thị kết quả sau khi kỳ thi đóng.

D) Other (please describe after [Answer]: tag below)

[Answer]: a, web tôi chỉ có thi thử k có thi thật

## Question 4
Khi giảng viên copy assignment và rubric giữa các lớp mình phụ trách, phạm vi và quan hệ giữa bản gốc/bản sao nên thế nào?

A) Chỉ copy giữa các lớp mà giảng viên đang có quyền; tạo bản nháp độc lập, giữ nguồn gốc để audit nhưng không đồng bộ ngược/xuôi; loại bỏ lịch phát hành, bài nộp và điểm cũ. (Đề xuất)

B) Tạo bản sao có liên kết và cho phép đồng bộ cập nhật nội dung/rubric từ bản gốc.

C) Cho copy sang mọi lớp trong cùng môn, kể cả lớp do giảng viên khác phụ trách.

D) Other (please describe after [Answer]: tag below)

[Answer]:a 

## Question 5
Nguồn YouTube cho RAG theo bài giảng cần được nhập và xử lý ở mức nào?

A) Mỗi bài giảng nhận một hoặc nhiều URL video; hệ thống lấy transcript/caption kèm timestamp, cho giảng viên xem và duyệt trước khi lập chỉ mục; video không có transcript thì báo lỗi hoặc yêu cầu transcript thủ công. (Đề xuất)

B) Chấp nhận URL video/playlist và tự phiên âm audio khi không có caption.

C) Chỉ lưu link YouTube để tham khảo, không đưa transcript vào RAG.

D) Other (please describe after [Answer]: tag below)

[Answer]: b

## Question 6
Trong bài tập nhóm, ai chịu trách nhiệm ghép các phần cá nhân thành tài liệu chung?

A) Hệ thống tự ghép các phần theo cấu trúc/thứ tự do giảng viên định nghĩa; giảng viên xem trước, chỉnh thứ tự hoặc loại phần rồi chốt tài liệu tổng. (Đề xuất)

B) Hệ thống đẩy các phần cho giảng viên và giảng viên tự ghép thủ công.

C) Trưởng nhóm ghép và nộp tài liệu chung; giảng viên chỉ đối chiếu với các phần cá nhân.

D) Other (please describe after [Answer]: tag below)

[Answer]: a

## Question 7
Ai chấm từng phần và cách tổng hợp điểm bài nhóm nên được cấu hình thế nào?

A) Giảng viên chính phân công từng phần cho một hoặc nhiều người chấm có quyền trong lớp; mỗi phần có rubric/trọng số; hệ thống tính điểm tổng đề xuất, giảng viên chính được điều chỉnh với lý do và audit log. (Đề xuất)

B) Chỉ giảng viên chính chấm mọi phần; hệ thống cộng theo trọng số và cho điều chỉnh điểm tổng.

C) Thành viên tự/chéo chấm phần của nhau trước, giảng viên duyệt và chốt điểm.

D) Other (please describe after [Answer]: tag below)

[Answer]: b, có kết hợp ai cho ai chấm đề xuất , gv soát lại

## Question 8
Nếu các phần bài nhóm đúng riêng lẻ nhưng không nhất quán khi ghép lại, nên chấm theo nguyên tắc nào?

A) Rubric hai tầng: điểm từng phần và điểm tích hợp chung. Lỗi nhất quán chỉ trừ ở tiêu chí tích hợp chung, trừ khi xác định rõ phần nào gây lỗi; điểm cuối = tổng điểm phần theo trọng số + điểm tích hợp. (Đề xuất)

B) Trừ cùng một tỷ lệ trên điểm của tất cả thành viên khi tài liệu chung không nhất quán.

C) Giảng viên tự quyết định ngoài rubric cho từng trường hợp và ghi nhận lý do.

D) Chỉ chấm các phần riêng, không chấm tính nhất quán của tài liệu tổng.

E) Other (please describe after [Answer]: tag below)

[Answer]: bài riêng thì giảng viên có thể chấm bằng ai, bài chung thì giảng viên phải tự chấm, điểm chốt cho từng sinh viên thì dựa trên đóng góp của thành viên so với bài chung

## Question 9
Điểm bài nhóm cuối cùng nên được phân bổ cho thành viên ra sao?

A) Mỗi sinh viên có điểm cá nhân từ phần được giao cộng với điểm tích hợp chung của nhóm; giảng viên có thể điều chỉnh cá nhân với lý do và audit log. (Đề xuất)

B) Mọi thành viên nhận cùng một điểm tổng của nhóm.

C) Điểm cá nhân chỉ dựa trên phần được giao; điểm tích hợp chỉ là phản hồi, không tính điểm.

D) Other (please describe after [Answer]: tag below)

[Answer]: bài riêng thì giảng viên có thể chấm bằng ai để feedback bài làm, giảng viên dựa vào đó để tính điểm cho sinh viên .
