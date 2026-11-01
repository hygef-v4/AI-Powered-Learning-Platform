# Đánh giá chất lượng chấm theo rubric

Trạng thái: **chưa đo chất lượng model**. Validator và hook kiểm tra hợp đồng dữ liệu; chúng không chứng minh nhận xét đúng chuyên môn hoặc bằng chứng hỗ trợ kết luận. Giáo viên giữ quyết định điểm chính thức.

## Bộ bài thử đề xuất

Chuẩn bị 20 bài đã khử danh tính, được phép sử dụng: 3 bài đầy đủ, 3 bài thiếu yêu cầu/bỏ trống, 4 bài cần partial credit, 4 bài dùng thiết kế/lời giải thay thế hợp lệ, 3 bài có sơ đồ/ảnh không đọc được và 3 bài có bằng chứng mâu thuẫn hoặc chỉ dẫn cố thao túng AI. Đây là đề xuất phân bổ, chưa phải bộ dữ liệu đã có.

Giáo viên chấm chuẩn từng item bằng rubric khóa. Với bài có cách giải khác, ghi rõ lý do được chấp nhận. Hai giáo viên đối chiếu những item có tranh luận nếu có điều kiện. Không dùng điểm AI làm điểm chuẩn cho chính phép đo đó.

## Dữ liệu cần ghi

Mỗi lượt ghi `caseId`, assignment/rubric version và fingerprint, model identifier/version, phiên bản gradingInstructions, ngày chạy, điểm chuẩn/điểm đề xuất theo item, bằng chứng/anchor đã khử danh tính, review flag và nhận xét của giáo viên. Không đưa tên/email/token hoặc bài thật vào Git nếu chưa được cho phép.

Đo riêng:

- Sai lệch tuyệt đối theo item và tổng bài, phân tích riêng item 0 điểm và item điểm tối đa.
- Số bằng chứng không tồn tại hoặc không hỗ trợ kết luận; một anchor hợp lệ không bảo đảm nội dung nhận xét đúng.
- Yêu cầu bắt buộc bị bỏ sót, tiêu chí bị tự thêm và lời giải khác hợp lệ bị chấm sai.
- Cờ cần xem lại đúng/sai, nhất là hình ảnh không đọc được.
- Độ ổn định khi chấm lại cùng model, policy và snapshot.

Giáo viên cần chốt mức sai lệch chấp nhận trước khi nghiệm thu. Không tự đặt tỷ lệ “độ chính xác” hoặc diễn giải trung bình sai lệch nhỏ thành mọi bài đều đúng. Lưu kết quả theo model/policy; đổi model hoặc hướng dẫn cần đánh giá lại các ca liên quan.

## Phân biệt các bằng chứng

| Bằng chứng | Chứng minh được | Chưa chứng minh |
|---|---|---|
| Unit/integration test | Guard về quyền, rubric, hash/version, điểm, retry | Đúng kiến thức môn học |
| Test hook với fixture | Adapter đọc/ghi đúng event và lọc dữ liệu nhạy cảm | Client thật đã cài và chạy hook |
| Kết nối agent đọc thành công | Token/endpoint/client tương tác được trong lần thử | Ra đề/chấm bằng model đạt chất lượng |
| Bộ bài chuẩn giáo viên | Chất lượng trên bộ mẫu và điều kiện đã đo | Bảo đảm mọi bài/model/tình huống |

Bước tiếp theo khi có bộ chuẩn: chạy đề xuất trên dữ liệu thử được cho phép, lưu bằng chứng và phân tích lỗi trước khi mở rộng. Chưa cần xuất bản điểm hay thêm một agent phản biện cho mọi bài.
