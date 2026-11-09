# Đề xuất giao diện học liệu và soạn quiz

Trạng thái: người dùng duyệt triển khai bằng “ok giúp tôi sửa theo kế hoạch”. Thực hiện theo [kế hoạch mã](../../construction/plans/code-generation-plan/2026-11-09-material-quiz-ui.md); người dùng yêu cầu tự kiểm tra UI sau. Phạm vi U13 học liệu, U10 quiz/câu hỏi và phần chọn tài liệu tham khảo cho AI. Giữ bố cục và thành phần hiện có; không viết lại giao diện chung. Các thay đổi nhập Excel từ yêu cầu trước vẫn giữ nguyên.

[Bản minh họa giao diện](assets/2026-11-09-material-quiz-ui-proposal.png): hai trạng thái của cùng khung Câu hỏi (soạn tay/AI), không phải hai khung đặt cạnh nhau trong sản phẩm; phần dưới minh họa công tắc và mô tả học liệu. Ảnh được tạo bằng Image Gen, không phải screenshot tính năng đã triển khai.

## Giao diện đề xuất

1. Học liệu có công tắc ở cạnh tên và trong form sửa, nhãn cuối theo yêu cầu bổ sung là “Bật/Tắt”, bỏ chữ Enabled/Disabled trên UI. Tắt ẩn khỏi sinh viên, người soạn vẫn thấy nhãn “Tắt” và bật lại được; không được dùng làm nguồn AI mới. Phương án này đã được duyệt. Trạng thái này độc lập với lưu trữ; không dùng nó thay trạng thái Nháp/Phát hành của quiz. Chặn truy cập trực tiếp và download ở server, không chỉ ẩn trên UI; không xóa lịch sử/bài làm/tài liệu đã tham chiếu.
2. Mô tả có toolbar nhỏ: đậm, nghiêng, gạch chân, căn trái/giữa/phải/đều, danh sách chấm/số, thụt lề và hoàn tác. Xem trước hiển thị đúng định dạng; mô tả text cũ vẫn đọc được. Tái sử dụng bộ soạn/runs có sẵn, tránh mang toàn bộ tính năng tài liệu, rubric và sơ đồ vào trường mô tả. Cần thiết kế lưu/validate nội dung có cấu trúc và giới hạn độ dài khi triển khai; không render HTML chưa lọc.
3. Tài liệu nguồn cho AI đổi từ danh sách checkbox trải dài sang dropdown chọn nhiều: tìm theo tên, checkbox từng mục, chip đã chọn có nút bỏ, bộ đếm n/10, giữ dropdown mở khi tick, chọn các mục phù hợp với kết quả tìm kiếm trong giới hạn. Mục không dùng được vẫn hiện với lý do, không chỉ dựa màu. Điều khiển bằng bàn phím và có nhãn đọc màn hình. Tài liệu nguồn và nguồn API AI là hai khái niệm khác nhau.
4. Ngay trong khung Câu hỏi, nút Thêm câu hỏi mở vùng soạn tại chỗ. Ba cách “Soạn câu mới / Ngân hàng / Soạn bằng AI” dùng chung vùng này. Soạn tay có nội dung, loại, đáp án radio/checkbox, điểm, độ khó và giải thích; lưu thành một câu trong danh sách. Sửa câu mở ngay trong thẻ của câu đó. Hủy bỏ phần chưa lưu, giữ vị trí cuộn, hỏi khi rời trang nếu còn nội dung chưa lưu.
5. Lấy từ ngân hàng hiển thị tìm kiếm/lọc/chọn nhiều trong cùng khung; nút “Thêm n câu”. Soạn bằng AI cũng nằm tại đây: yêu cầu, số câu, loại, độ khó và dropdown tài liệu; khi đang tạo giữ form/trạng thái, khi lỗi giữ input và hiện lý do/thử lại. Kết quả là các thẻ đề xuất tại chỗ, phải chấp nhận trước khi đưa vào quiz. Giữ cơ chế soát version và phân quyền hiện có.

## Bằng chứng hiện trạng

- Model Material hiện có ACTIVE/ARCHIVED, chưa có công tắc hiển thị riêng.
- MaterialDialogs dùng textarea; LearningMaterialPage render mô tả dạng text với xuống dòng. Bộ soạn dùng chung đã hỗ trợ căn lề và danh sách.
- QuizDetailPage mở InlineQuestionDialog cho soạn mới/sửa; QuizAiDraftPanel mở AiDraftDialog ở khu vực riêng. McqEditor và validation hiện tại tái sử dụng được khi đổi vị trí trình bày.
- Form AI đã nhận nhiều materialIds, tối đa 10, chặn hasText=false. Đây là đổi cách trình bày, không cần thêm API chọn nhiều chỉ để đổi checkbox sang dropdown.
- Kiểm tra đọc trên phiên production hiện có: quiz 533d6e09-110c-4c78-9a91-160c5897fe68 có ba tài liệu nguồn đều là video YouTube, API reference-materials trả 200 với hasText=false cho cả ba; ai-drafts trả 200 với danh sách rỗng. Không gửi yêu cầu AI, không tạo quiz hay sửa dữ liệu. Chưa chứng minh một task AI bị lỗi; trường hợp này chưa có nguồn văn bản để chọn. Không được quảng cáo URL YouTube/mô tả là transcript video.
- Chụp ảnh trình duyệt bị timeout hai lần nên không có ảnh capture mới được chấp nhận. Ảnh người dùng cung cấp và DOM/API đọc trong lượt này là bằng chứng; hình Image Gen chỉ là minh họa đề xuất.

## Audit phạm vi — chỉ phát hiện có trường hợp cụ thể

Giả định tải: một trường, nhiều giảng viên/chủ nhiệm môn sử dụng đồng thời; lượt này không đo tải.

### Should fix

1. Hướng dẫn chọn nguồn AI không còn đúng (`frontend/src/features/quizzes/components/AiDrafts.tsx:270`). Form nói bấm gửi để chọn nguồn và không tự đổi nguồn, nhưng form gửi trực tiếp và AiTaskService đã tự chọn nguồn, worker có cơ chế thử nguồn kế tiếp. Sửa tối thiểu là cập nhật lời hướng dẫn theo hành vi hiện tại; giữ nguyên lựa chọn nguồn tự động đã được quyết định trước đây. Nếu bỏ qua, người soạn chờ popup không xuất hiện và hiểu sai nguồn/quota.
2. Tài liệu về cuối có thể không còn nội dung trong prompt mà người soạn không được báo (`backend/src/main/java/edu/aiplatform/materials/service/ReferenceMaterialService.java:79`, `quizzes/service/QuizDraftPrompt.java:74`). Quiz giới hạn tổng 60.000 ký tự; nếu tài liệu đầu dùng hết ngân sách, các tài liệu sau vẫn vào danh sách nhưng text rỗng, và prompt không dùng cờ truncated để thông báo. Giới hạn tổng là hành vi có chủ đích, vấn đề là độ bao phủ nguồn bị khuất. Sửa tối thiểu là báo nguồn bị cắt/bỏ trống trước khi tạo hoặc trong kết quả; không tự tăng ngân sách hay thêm hệ thống truy xuất mới. Nếu bỏ qua, chọn nhiều nguồn có thể sinh câu chỉ dựa trên tài liệu đầu.

Verdict: làm rõ nguồn dùng được và lỗi thực tế trước khi sửa tác vụ AI; giao diện soạn tại chỗ tái sử dụng form và API hiện có.

Not checked: chưa chạy test/build trong lượt audit; chưa gọi provider, tạo AI task hay sửa dữ liệu production; không audit toàn repo, hạ tầng, bảo mật tổng thể hoặc tải. Các kết quả test của lần sửa Excel không phải bằng chứng nghiệm thu thay đổi đề xuất này.

## Quyết định cần chốt

Disabled hoạt động thế nào?

A) Ẩn với sinh viên, người soạn quản lý được, không dùng làm nguồn AI mới (đề xuất).

B) Sinh viên vẫn thấy tên nhưng không mở được.

C) Khác — mô tả thêm.

[Answer]: A — áp dụng phương án đề xuất khi người dùng duyệt toàn bộ kế hoạch.

Sau khi duyệt bố cục và hành vi Disabled, mới lập kế hoạch sửa/kiểm thử theo AI-DLC. Security/Resiliency giữ cấu hình hiện có; việc triển khai phải kiểm tra phân quyền, direct URL/download, nội dung có định dạng, trạng thái AI và version. PBT đang tắt. Chưa thay đổi chức năng để chứng nhận tuân thủ runtime.
