# Rà soát lại tính thống nhất của SRS — 2026-10-09

Tài liệu: [Report 3 — Software Requirement Specification](https://docs.google.com/document/d/1ebPCxJyusasIm8aiMmm3pWiIAaEr7D7n/edit).

Bản đầu vào có modifiedTime `2026-10-09T11:32:24.966Z`. Đã đọc các phần I–V, bảng yêu cầu, use case, danh mục màn hình/job/API và xem toàn bộ 86 ảnh nhúng, gồm diagram và UI. Các ảnh được đối chiếu qua bản trích xuất từ chính file hiện tại; chữ nhỏ trong diagram cần xem ảnh riêng khi chỉnh sửa chi tiết.

## Kết luận

Tài liệu **chưa đồng nhất hoàn toàn**. Description màn hình trong I.5 và III đang bám UI hiện tại theo lựa chọn của người dùng. IV–V và một số job/API đã bám AI-DLC mới, còn mô hình dữ liệu, luồng nghiệp vụ và một số ảnh vẫn chứa thiết kế cũ. Vì vậy việc sửa mục lục không giải quyết các khác biệt nghiệp vụ dưới đây.

Lần này chỉ cập nhật Table of Contents theo nội dung thực tế; không thay đổi requirement, description, ảnh UI hoặc diagram. Giữ độ sâu mục lục Heading 1–3 của template.

## Những điểm còn lệch

| Điểm | Vị trí cần đối chiếu | Nội dung chưa thống nhất |
|---|---|---|
| Quyền Admin | I.1; I.4.3.5 diagram; I.4.4 Permission Matrix; II UC38/39/43/44 ↔ IV Security; V BR01/18/19/25 | Phần đầu vẫn cho Admin quyền học thuật, quản lý lớp/ghi danh, kế thừa quyền Teacher/Subject Manager và ví credit. Quy tắc mới giới hạn Admin ở quản trị hệ thống, không tham gia các thao tác học thuật và ví cá nhân. |
| Upload và tóm tắt học liệu | I.2.2 BF02; I.3 Lesson; I.4 UC15/33/54; III.3.6/3.7 và UI Learning Material/Teacher Materials ↔ I.5.3; III JOB06/07/17; IV Gemini; V BR04/21 | Luồng/ảnh cũ còn quét và lập chỉ mục sau upload, có retry/rescan và trạng thái INDEXED. Thiết kế mới chỉ trích xuất khi upload; người xem chủ động yêu cầu tóm tắt rồi mới dùng AI và lập chỉ mục. Các ảnh hiện chưa có nút tóm tắt mới. |
| Quiz, lịch và bài tập theo môn | BF06; I.3 Assignment; III Quiz List, Assignment List, Student Class Detail, Teacher Evals/Student Detail ↔ JOB04; V BR06/07/11/18 | Ảnh/luồng cũ còn Graded Quiz hoặc hạn nộp Quiz. Quy tắc mới chỉ có Practice Quiz, không có opens_at/closes_at. Mô hình Assignment còn mô tả template theo môn, chưa thể hiện đầy đủ bài tập chung cho các lớp OPEN và một lịch dùng chung. |
| Rubric | I.3 ERD/entity/data rules; III.3.2 Question List; II UC44 ↔ V BR08 | Mô hình/ảnh cũ có rubric độc lập, version/ACTIVE và liên kết rubric với ngân hàng câu hỏi. Quy tắc mới dùng rubric thuộc assignment, không version, được khóa khi publication. Description Question List hiện phản ánh đúng cột Rubric trong ảnh nên chưa thể đồng thời phản ánh thiết kế mới. |
| Announcement | I.3 entity/data rules; III.4.4 Class Announcements và ảnh ↔ JOB03; V BR23/27 | Entity còn VISIBLE/HIDDEN và không sửa; data rules lại có version/soft delete. UI có bình luận. Quy tắc mới cho phép tạo/sửa/xóa mềm, chỉ tạo mới mới phát thông báo và không hỗ trợ bình luận. |
| Credit khi AI lỗi | I.2.7 BF07; II UC29/38 ↔ JOB09/14/17; IV Performance; V BR21/22 | Luồng/use case cũ diễn đạt lỗi hoặc timeout thì hoàn toàn bộ credit. Quy tắc mới chốt lượng dùng thật đã ghi, trả phần dư; chỉ hoàn toàn bộ khi chưa dùng AI. Con số 5 credit trong UI là ví dụ hiển thị, không nên suy ra một mức phí cố định cho mọi tác vụ. |
| Mốc timeout chấm AI | II UC29/38; III JOB09/14; IV.2.2; V BR22 ↔ AI-DLC U13 BR-U13-24 | Một số mô tả dùng 5 phút từ lúc xử lý cho mọi yêu cầu. AI-DLC phân biệt yêu cầu thường: từ lúc bấm/được chấp nhận; batch: từ lúc từng bài bắt đầu xử lý. Material summary có deadline riêng, không áp dụng timeout chấm bài này. |
| Settings và Credit Package | I.3 AIService/CreditPackage; III Setting List/Detail và ảnh ↔ IV.2.2; V BR19 | Mô hình AIService toàn cục và giới hạn số request/ngày trên ảnh chưa khớp Settings, giới hạn tốc độ và ngân sách Gemini trong IV. CreditPackage còn mô tả seed khi deploy, trong khi UI và BR19 có quản trị package. 50 credit/tháng trên ảnh có thể là cấu hình mẫu; không phải tự nó mâu thuẫn với default 100. |
| Tên màn hình và role landing | II UC24/25/26/43/44; I.5/III Class Dashboard ↔ Screen Inventory và IV role landing | Use case còn dùng TextEssay/Codelab/DiagramEssay Assignments hoặc editor/list riêng, trong khi inventory dùng Student Assignments, Assignment Detail, Assignment Form và Workspace chung. Description Class Dashboard chỉ nêu Student/Teacher, chưa thể hiện Subject Manager khi được phân công giảng dạy. Không cần tự động thêm màn mới chỉ vì tên khác. |
| Thông tin đếm và phân trang | Change Log; footer | Change Log còn nhắc 55 màn, danh mục hiện có 51; đây là thông tin lịch sử cần phân biệt với inventory hiện tại. Footer vẫn hiển thị tổng 18 trang trong khi bản render hiện có 143 trang. Footer nằm ngoài phạm vi chỉnh mục lục lần này. |

## Mục lục đã sửa

- Đồng bộ đủ **134 đề mục không rỗng** cấp Heading 1–3, theo đúng thứ tự và tên hiện tại trong body.
- Bổ sung **7.17 Summarize and Index Material on Viewer Request (JOB-17)**, trang 127.
- Đồng bộ tên **7.6 Extract File Learning Materials (JOB-06)** và **7.9 Settle Expired AI Credit Reservations (JOB-09)**.
- Cập nhật số trang của toàn bộ đề mục; mục lục vẫn gồm 4 trang (trang 2–5).
- Giữ liên kết đến heading, cấp thụt lề và font hiện tại; khôi phục dấu chấm dẫn đến số trang.

## Kiểm tra thay đổi

So sánh gói DOCX: chỉ `word/document.xml` thay đổi. Toàn bộ các phần khác, bao gồm ảnh, style, header/footer và relationship, được giữ nguyên byte. Body được giữ nguyên ngoài mục lục và một bookmark điều hướng thêm cho JOB-17; bookmark không thay đổi nội dung hay định dạng heading.

Đã render bằng Word chạy ẩn, kiểm tra hình ảnh cả 4 trang mục lục và đối chiếu số trang của **134/134** entry với vị trí bookmark trong tài liệu cuối. Không còn sai lệch số trang; tài liệu render có 143 trang.

Đây là rà soát và chỉnh điều hướng tài liệu; không thay đổi stage AI-DLC hoặc triển khai phần mềm. Security/resiliency runtime checks và runtime tests: N/A. Các khác biệt nghiệp vụ được ghi nhận, không coi là đã giải quyết.

## Rà lại sau khi bổ sung Acronyms — bản 2026-10-09T12:11:28.650Z

Đã tải bản hiện tại, đối chiếu lại I–V và xem lại 86 ảnh nhúng qua các trang tổng hợp. Context Diagram được mở riêng ở độ phân giải gốc. Bảng Acronyms hiện đủ 16 mục và dùng ALAP thống nhất. Những khác biệt nghiệp vụ nêu trên vẫn còn; lần này chỉ review, chưa sửa nội dung online.

### Kết luận riêng cho Context Diagram

Phần giới thiệu nền tảng, mua credit không cấp quyền học, Practice AI riêng tư, Teacher duyệt AI proposals và vai trò các dịch vụ ngoài nhìn chung đúng. Có ba mô tả sai quyền và một thiếu sót chức năng quan trọng:

1. Administrator không quản lý lớp hoặc ghi danh. Admin quản trị môn, có thể xem danh sách lớp chỉ đọc trong Subject Detail và số đếm dashboard, nhưng không mở/sửa chi tiết lớp. Context text và luồng `Subjects, Classes & Enrollment` phía Admin của diagram đều cần sửa (BR01, IV Admin APIs; U04 BR-U04-05).
2. Personal credit không dùng cho cả bốn role. Chỉ Student, Teacher và Subject Manager có ví/mua credit; Admin không có ví (BR19; U07 BR-U07-01).
3. Administrator không kế thừa Teacher và không nhận phân công dạy lớp. Chỉ Subject Manager có thể dùng chức năng Teacher khi được giao dạy lớp đó (BR01; U01 BR-U01-60).
4. Cần nêu yêu cầu tóm tắt chủ động trên màn xem học liệu của Student, Teacher và Subject Manager, không chạy AI khi upload hay chỉ mở trang. Gemini xử lý summary và embedding sau yêu cầu này. Diagram đã có `Subject Materials & Summary Requests` phía Subject Manager nhưng chưa thể hiện rõ yêu cầu tương ứng của Student/Teacher (BR03/04/21; U05 BR-U05-45).

`RAG resources` trong mô tả Subject Manager quá chung: nên diễn đạt quản lý subject materials/question bank và dùng các học liệu được phép cho AI authoring; không ngụ ý có màn quản trị RAG hoặc Student RAG độc lập. Các dòng tích hợp Brevo, PayOS, Drive, Judge0, YouTube và Draw.io không cần đổi bản chất.

### Các chỗ cần đồng bộ ngoài Context

Ưu tiên Permission Matrix và diagram kế thừa Admin, rồi BF02 upload/indexing, Lesson lifecycle, rubric version/bank association, Quiz Graded/schedule, announcement comments, BF07/UC29/UC38 settlement và mốc timeout thường/batch. API03, JOB06/07/17 và BR04 đã mô tả yêu cầu tóm tắt chủ động; cần đưa các phần cũ về cùng quy tắc đó. Description UI hiện vẫn mô tả đúng ảnh cũ theo lựa chọn của người dùng, nên phải ghi rõ các khác biệt này khi cập nhật thiết kế; không tự thay ảnh hoặc thêm chức năng vào description chưa có trên ảnh.

Phát hiện tham chiếu AI-DLC cũng còn một điểm cũ: U13 BR-U13-41 vẫn nói Admin Dashboard có AI usage stats, trong khi U16 BR-U16-40/41 và BR25 của SRS chỉ nêu số đếm quản trị. Không dùng riêng dòng U13 này để thêm thống kê AI vào Context/UI.
