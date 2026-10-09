# Yêu cầu sản phẩm: AI-Powered Learning Platform

## 1. Tóm tắt phân tích ý định

- **Yêu cầu ban đầu**: "giúp tôi triển khai quy trình ai dlc".
- **Mục tiêu**: MVP web cho một tổ chức, có frontend, backend, AI, tệp, thanh toán, email và tài liệu/checkpoint AI-DLC.
- **Bản hiệu lực (2026-10-09)**: [catalog 73 UC](../../../docs/use-cases-73.md) và Page-2 của `docs/G21_Diagrams.drawio` là nguồn local cho revision này; các quyết định FR bên dưới bổ sung scope và permission. Google SRS đã được dùng ở các mốc trước; revision này không xác minh lại tài liệu online.
- **Thay đổi chính**: Admin chỉ quản trị; chỉ ngân hàng môn; bỏ mã mời/bình luận/template/U10; bài của môn giao mọi lớp; quiz luyện tập gắn học liệu; Settings chung U03; summary/credit khi người xem yêu cầu.
- **Quyền**: Student và Teacher → User; Subject Manager → Teacher có điều kiện phân công R4. Administrator chỉ User/quản trị, không phân công môn/lớp, không ví/AI; scope R1–R5 kiểm tại server.
- **Phạm vi tài liệu**: 73 UC, 51 story, 15 unit; US-CAT-005 đã rút, US-SET-001 thuộc U03; version/copy/bài của môn thuộc U08, không U10.
- **Giữ các chi tiết đã chốt**: Năm dạng bài, Practice/Graded, version/snapshot, rubric từng câu/phần, nhóm thuộc lớp, leader giao phần và nộp; không có Simulation Exam hoặc tiến độ hoàn thành bài học.
- **Luồng hỗ trợ**: Ghi danh, chia nhóm, AI soạn nháp, tóm tắt học liệu từ nút trên màn xem, duyệt/phát hành/version/copy, chốt/công bố điểm, audit và hạn mức vẫn thuộc requirement/story liên quan; không tự thêm UC vào bảng mới.
- **Screen Flow**: Page-2 `docs/G21_Diagrams.drawio`; Class Dashboard, Manager Dashboard, Admin Dashboard; Student Assignments và Quiz Practice tách; màn Settings/gói quản trị theo diagram. Các mốc/câu trả lời cũ giữ làm lịch sử.

## 2. Bối cảnh và phạm vi

### 2.1 Người dùng và mô hình vận hành

Nền tảng phục vụ một trường học hoặc trung tâm đào tạo. Bốn vai trò chính là Student (sinh viên), Teacher (giảng viên), Subject Manager (Chủ nhiệm môn) và Administrator (quản trị viên). `STUDENT` thay `LEARNER`; `TEACHER` thay `INSTRUCTOR` trong mã vai trò, API, giao diện và tài liệu hiện hành. Một môn học có thể có nhiều lớp; Chủ nhiệm môn chịu trách nhiệm học thuật và tài nguyên dùng chung của các môn được phân công, còn giảng viên phụ trách hoạt động và nội dung riêng của các lớp được giao. Phiên bản đầu vận hành trong phạm vi một tổ chức; multi-tenancy không thuộc MVP.

### 2.2 Phạm vi MVP

MVP bao gồm tài khoản và vòng đời tài khoản quản trị, phân quyền, quản lý môn học/lớp học, nhóm học tập, bài nhóm là một tài liệu chung mà thành viên tự nhận và làm từng mục, kho học liệu và RAG cấp môn/bài giảng từ tài liệu hoặc YouTube, nội dung riêng của lớp, tải tài liệu, ngân hàng câu hỏi của môn cho mọi dạng bài có versioning, rubric của bài, bài cấp môn, sao chép assignment/rubric giữa các lớp của cùng giảng viên, theo dõi trạng thái bài nộp và kết quả đánh giá, năm dạng assignment Code Lab, Text Essay, Diagram Essay, Group Assignment và Multiple-Choice Quiz với chế độ `GRADED`/`PRACTICE` theo từng dạng, tạo câu hỏi/bài tập bằng AI, chấm bài luyện tập Text Essay/Diagram Essay bằng credit của Student, giám sát sử dụng AI, thanh toán mua credit AI và email/thông báo. Simulation Exam được bỏ khỏi MVP. MVP không lưu tiến độ hoàn thành hoặc vị trí học của từng bài. Sản phẩm là web desktop-first cho người học; giao diện mobile chỉ cần đáp ứng các thao tác đọc/cơ bản, không tối ưu canvas vẽ sơ đồ hoặc trải nghiệm làm bài phức tạp.

### 2.3 Ngoài phạm vi MVP

- Dự án chỉ có một phạm vi MVP, không chia đợt Phase 2. Thông báo lớp, thống kê cho quản trị viên và xuất bảng điểm thuộc MVP; dashboard cá nhân của Student đã bỏ (2026-10-03). Tóm tắt học liệu bằng AI từ nút trên màn xem thuộc MVP (FR-004, người dùng chốt 2026-10-09). Các chức năng đã loại gồm bình luận dưới thông báo và mã mời vào lớp (2026-10-09), tìm kiếm học liệu theo yêu cầu người dùng, phân tích chất lượng câu hỏi, gia hạn nộp bài cá nhân, phúc khảo điểm, kiểm tra tương đồng, báo cáo độ lệch điểm AI, tiến độ hoàn thành bài học và template đề cấp môn; chúng không có story hoặc UC trong catalog hiện hành. Quy trình AI đề xuất điểm rồi giảng viên quyết định điểm cuối vẫn thuộc MVP.
- Ứng dụng mobile native
- Multi-tenancy và cô lập dữ liệu giữa nhiều tổ chức
- Đồng bộ LMS hoặc SSO của tổ chức
- Chức năng dành riêng cho vai trò Head of Department/Trưởng bộ môn
- Simulation Exam và kỳ thi chính thức/proctored exam
- Mọi nội dung dạng viết được mô hình hóa chung là bài viết luận
- Active/active đa region
- Lưu vị trí học, đánh dấu hoàn thành hoặc báo cáo tiến độ hoàn thành từng bài học
- Property-based testing
- Chứng nhận tuân thủ một khung pháp lý cụ thể
- Operations automation hoàn chỉnh ngoài các yêu cầu sẵn sàng và tài liệu được xác định trong quy trình này

## 3. Các bên liên quan

| Bên liên quan | Nhu cầu chính |
|---|---|
| Người học | Truy cập lớp học, học nội dung, làm bài, nhận phản hồi và xem kết quả/trạng thái bài nộp |
| Giảng viên | Quản lý nội dung/lớp học, dùng AI tạo bài, duyệt kết quả và theo dõi người học |
| Chủ nhiệm môn | Quản lý học liệu/ngân hàng câu hỏi và bài của môn; quản lý lớp của môn và ghi danh; bài của môn giao mọi lớp OPEN, giảng viên từng lớp chấm; chỉ chấm khi chính tài khoản được giao dạy lớp |
| Quản trị viên | Quản lý người dùng, vai trò, cấu hình nền tảng, thanh toán và audit |
| Đơn vị đào tạo | Vận hành thử nghiệm ổn định, bảo vệ dữ liệu người học và đo hiệu quả MVP |
| Nhóm phát triển | Quy trình AI-DLC rõ ràng, test tự động, container local và hướng dẫn triển khai |

## 4. Yêu cầu chức năng

### FR-001 - Xác thực và tài khoản trường cấp

Hệ thống phải dùng email do trường cấp làm định danh đăng nhập cho người học và giảng viên. Tài khoản do quản trị viên tạo hoặc nhập từ file CSV; không có đăng ký công khai. Hệ thống hỗ trợ OTP qua Brevo để kích hoạt/khôi phục, đăng nhập, đăng xuất, đổi mật khẩu và xem/cập nhật hồ sơ (UC 01–07). Người dùng cập nhật display name và phone number hợp lệ; không cập nhật avatar, email định danh, role hoặc status trong hồ sơ cá nhân.

**Tiêu chí chấp nhận:**

- Người dùng đã xác thực có thể đăng nhập và đăng xuất an toàn.
- Phiên hết hạn theo cấu hình và bị vô hiệu hóa khi đăng xuất.
- Luồng khôi phục mật khẩu không tiết lộ tài khoản có tồn tại hay không.
- Email đăng nhập của người học/giảng viên phải thuộc miền email trường được cấu hình; người dùng không thể tự đăng ký bằng email ngoài miền.

### FR-002 - Phân quyền

Bốn vai trò là `STUDENT`, `TEACHER`, `SUBJECT_MANAGER`, `ADMIN`; User là actor trừu tượng của chức năng chung. Kế thừa: Student → User, Teacher → User, Subject Manager → Teacher. Administrator dùng chức năng User và chức năng quản trị; không dùng chức năng Subject Manager/Teacher, không vào Manager Dashboard hay Class Dashboard và không nhận phân công môn/lớp (người dùng chốt 2026-10-09). Backend kiểm quyền theo chức năng và đối tượng theo SRS 4.4.

**Tiêu chí chấp nhận:**

- **R1**: Hồ sơ, ví, kết quả thanh toán và thông báo chỉ của chính tài khoản; activation/recovery có quy tắc xác thực riêng.
- **R2**: Subject Manager dùng học liệu, bài của môn và ngân hàng cấp môn khi được phân công quản lý môn. Administrator quản trị tài khoản và môn học toàn nền tảng theo các hàng Full.
- **R3**: Teacher dùng chức năng giảng dạy trong lớp được giao.
- **R4**: Subject Manager dùng chức năng Teacher, bao gồm bài tập, chấm và xem/xuất gradebook, chỉ khi chính tài khoản được giao dạy lớp. Phân công quản lý môn không thay thế phân công giảng viên lớp.
- **R5**: Student chỉ dùng lớp đã ghi danh và dữ liệu được phép; thành viên làm tài liệu nhóm, chỉ leader nộp tài liệu nhóm.
- Kế thừa không cấp ngược chức năng Student cho vai trò khác. Yêu cầu qua ID/URL ngoài scope bị từ chối phía server; thu hồi scope không cho tiếp tục dùng dữ liệu cũ.

### FR-003 - Quản lý môn học và lớp học

Administrator xem danh sách/chi tiết, thêm và sửa môn, gán Chủ nhiệm môn, lưu trữ/mở lại môn (UC 64–67); Subject Detail của Administrator chỉ xem danh sách lớp của môn. Subject Manager vào Manager Dashboard (UC 53), xem lớp của môn được giao, tạo lớp, gán giảng viên, thêm/gỡ sinh viên, sửa và mở/lưu trữ lớp (UC 47–52). Teacher xem lớp được giao trên Class Dashboard và Teacher Class Detail (UC 31–32); Student xem lớp đã ghi danh trên Class Dashboard và Student Class Detail (UC 13–14). Người dùng chốt 2026-10-09.

**Tiêu chí chấp nhận:**

- Mỗi lớp thuộc một môn, dùng chung module/học liệu môn; không có thực thể khóa học riêng.
- Chủ nhiệm môn phải là Subject Manager ACTIVE; giảng viên chính là Teacher hoặc Subject Manager ACTIVE; Administrator không được gán môn hay lớp. Các phân công là điều kiện R2/R4 và được audit.
- Chỉ Chủ nhiệm môn của môn sửa thông tin/vòng đời lớp và thêm/gỡ sinh viên; Teacher chỉ xem thông tin lớp và danh sách sinh viên.
- Không có tự ghi danh bằng mã mời; sinh viên chỉ vào lớp khi Chủ nhiệm môn thêm.
- Student truy cập theo enrollment; học liệu lưu trữ không hiển thị; thay đổi cấu trúc/phân công được audit.

### FR-004 - Nhập và quản lý nội dung học

Học liệu tổ chức theo module của môn: Chủ nhiệm môn tạo module trên Material List, mọi lớp của môn (kể cả lớp tạo sau) dùng chung, không chỉnh từng lớp; mỗi module có nút tải tệp và gắn link video. Mỗi bài học (lesson) là đúng một tệp PDF, DOCX hoặc PPTX tải lên, hoặc một link video YouTube. Không soạn nội dung trực tiếp, không có phiên bản hay bước xuất bản: tải lên xong là hiển thị cho người học và hệ thống trích chữ hoặc lấy phụ đề chạy nền, chưa gọi AI; tóm tắt và embedding chỉ bắt đầu sau khi người có quyền xem yêu cầu. Chủ nhiệm môn tải lên trên Material List là học liệu của môn (mọi lớp thấy, UC 54–55); giảng viên tải lên trên tab Materials của Teacher Class Detail là học liệu riêng của lớp được phân công (UC 33–34); Material Detail dùng chung để xem, sửa thông tin và xóa; trích chữ chạy một lần sau upload; tóm tắt/embedding chỉ bắt đầu khi người xem yêu cầu; mỗi giai đoạn lỗi tạm thì tự retry hữu hạn. Việc quét phải có trạng thái, giới hạn hợp lệ và thông báo lỗi an toàn (quyết định 2026-10-03, 2026-10-04, 2026-10-09).

**Tiêu chí chấp nhận:**

- Tệp không hợp lệ bị từ chối trước khi xử lý.
- Chỉ Chủ nhiệm môn tạo, sửa, sắp xếp, lưu trữ module của môn trên Material List; module dùng chung cho mọi lớp của môn, kể cả lớp tạo sau, và mỗi module có nút tải tệp, gắn link video.
- Tệp hợp lệ được lưu riêng tư và gắn đúng module và phạm vi: Chủ nhiệm môn tải là học liệu của môn (mọi lớp thấy), giảng viên tải là học liệu riêng của lớp.
- Người tải lên và người quản lý được ủy quyền xem được trạng thái chờ, đang xử lý, thành công hoặc thất bại.
- Giảng viên chỉ đọc học liệu của môn; Chủ nhiệm môn (R2) quản lý học liệu của môn nhưng không sửa học liệu riêng của lớp mình không dạy; học liệu của lớp do giảng viên được giao dạy lớp (R3/R4) quản lý. Admin không quản lý học liệu.
- UC 33–34 dùng tab Materials và Material Detail, UC 54–55 dùng Material List và Material Detail. Sửa chỉ đổi thông tin (tên, thứ tự), không thay được tệp hay link; muốn đổi tài liệu thì xóa rồi tải học liệu mới; xóa học liệu là lưu trữ, giữ lịch sử và tham chiếu, quiz gắn với học liệu không còn hiện cho người học.
- Lesson YouTube là một video (không nhận playlist); hệ thống chỉ dùng caption có sẵn (kể cả caption tự động của YouTube), không tự phiên âm audio; video không có caption được báo rõ (`NO_CAPTION`) và không lập chỉ mục.
- Chữ trích được, bản tóm tắt và embedding lưu trên chính lesson; chỉ lesson quét thành công mới được dùng cho RAG trong đúng phạm vi môn/lớp.
- Upload/tạo lesson không giữ credit và không tự tóm tắt hoặc embedding. Trích chữ thành công lưu `extracted_text` và trạng thái `EXTRACTED`; chưa có summary thì chưa vào RAG. Trên View Material (Learning Material hoặc Material Detail), Student, Teacher và Subject Manager có quyền xem được bấm "Tóm tắt tài liệu". Hệ thống kiểm lại quyền xem và credit của người bấm; một HOLD được tạo cùng transaction chấp nhận yêu cầu, thiếu credit không tạo yêu cầu nhưng học liệu vẫn xem/tải được. Summary dùng chung mỗi lesson, người xem sau dùng lại không trả thêm credit. Không thêm UC độc lập: luồng phụ UC 15, 34, 55.
- Vector RAG của mỗi học liệu được tạo từ bản tóm tắt, để tìm theo nội dung chính của cả tài liệu.
- Người có quyền xem thấy trạng thái trích chữ/tóm tắt. Không có nút Quét lại; nút Tóm tắt tài liệu chỉ mở khi trích chữ xong, chưa nhận yêu cầu và chưa có summary. Yêu cầu đã nhận tự retry hữu hạn; deadline 24 giờ từ lúc nhận yêu cầu, HOLD scanner 25 giờ dự phòng. Chốt chỉ lượng AI thực dùng, trả dư; trích chữ sau upload không có HOLD.

### FR-005 - Truy cập nội dung theo lớp

Student xem trước/tải học liệu trên Learning Material (UC 15) theo ghi danh (R5); Teacher hoặc Subject Manager được giao dạy xem học liệu của lớp và của môn trên Material Detail (R3/R4); Chủ nhiệm môn xem học liệu của môn trên Material Detail (R2). Admin không xem học liệu. Hệ thống không lưu trạng thái hoàn thành hoặc vị trí học của từng bài.

**Tiêu chí chấp nhận:**

- Người học chỉ nhận học liệu đang hiển thị trong lớp mình được ghi danh. Thanh toán không ảnh hưởng quyền vào lớp hay xem nội dung.
- Truy cập trực tiếp bằng URL/ID không vượt qua kiểm tra enrollment hoặc trạng thái học liệu.
- View Material hiện nút Tóm tắt tài liệu cho Student/Teacher/Subject Manager trong phạm vi; đang trích chữ/đang xử lý thì khóa nút, có summary thì hiện kết quả dùng chung và không gọi lại AI.
- Learning Material hiện danh sách quiz gắn với học liệu và nút làm quiz (quiz soạn theo UC 35, làm theo UC 20).

### FR-006 - Tạo câu hỏi và bài tập bằng AI

Giảng viên phải có thể yêu cầu AI tạo câu hỏi hoặc bài tập từ nội dung được phép của lớp; Chủ nhiệm môn phải có thể thực hiện tương tự từ kho học liệu/RAG của môn, với loại câu hỏi, số lượng và mức độ khó có giới hạn hợp lệ. Với Diagram Essay và bài nhóm (không có câu), AI đề xuất khung tài liệu (cây heading, hướng dẫn, gợi ý rubric từng phần); người soạn xem trước, xác nhận thì khung đề xuất thay khung hiện tại và sửa gợi ý rubric trước khi tạo rubric (quyết định 2026-10-04).

**Tiêu chí chấp nhận:**

- AI chỉ sử dụng nội dung thuộc phạm vi lớp hoặc môn mà người yêu cầu được phép truy cập.
- Nội dung do AI tạo ở trạng thái bản nháp và cần chính vai trò có quyền xuất bản duyệt trước khi giao cho người học.
- Hệ thống lưu nguồn nội dung, cấu hình tạo và trạng thái duyệt để truy vết.

### FR-007 - Đánh giá và bài nộp

Giảng viên phải có thể xuất bản bài tập riêng cho lớp được phân công (tab Evals của Teacher Class Detail, UC 41–45) với dạng và chế độ hợp lệ theo FR-017. Chủ nhiệm môn soạn bài của môn trên Assignment List (Manager Dashboard): bài giao cho mọi lớp của môn với một lịch chung, không có bài nhóm, giảng viên của từng lớp chấm cho sinh viên lớp mình (người dùng chốt 2026-10-09). Quiz là quiz luyện tập gắn học liệu, không có lịch và không vào sổ điểm (FR-017). Mọi bài qua bước duyệt trước khi phát hành. Student không tự tạo quiz hoặc chọn câu hỏi. Người học được phép phải có thể làm và nộp bài trong thời gian hiệu lực.

**Tiêu chí chấp nhận:**

- Hệ thống lưu dạng assignment, chế độ `GRADED`/`PRACTICE`, bài nộp, thời điểm nộp và trạng thái đánh giá theo từng attempt.
- `PRACTICE` chỉ dành cho luyện tập: không tạo điểm chính thức, không vào sổ điểm/xuất bảng điểm và không vào hàng đợi Teacher chấm.
- Một người học không thể đọc hoặc sửa bài nộp của người khác.
- Quy tắc số lần làm và hạn nộp được thực thi phía server.
- Mỗi lần phát hành lưu actor và lớp hoặc môn đích trong audit.
- Bài của môn hiện ở mọi lớp `OPEN` của môn; giảng viên lớp chỉ xem, không sửa hay ngưng giao bài của môn.

### FR-008 - Chấm điểm và phản hồi tự động

Chỉ bài `GRADED` mới chuyển tới quy trình chấm và chốt điểm của Teacher. Với câu trả lời mở, Teacher quyết định chấm thủ công hoặc yêu cầu AI đề xuất điểm/phản hồi; AI không tự động chấm bài `GRADED` nếu chưa có lựa chọn của Teacher và không bao giờ quyết định điểm cuối. Bài `PRACTICE` không do Teacher chấm; quy trình phản hồi luyện tập được nêu tại FR-030.

**Tiêu chí chấp nhận:**

- Trắc nghiệm và Code Lab tự chấm ngay khi nộp; kết quả tự động có kèm trạng thái. Điểm `GRADED` hiển thị dạng "x / tổng điểm của bài" khi được phép công bố; kết quả `PRACTICE` hiển thị riêng như phản hồi luyện tập. Sổ điểm không tính điểm tổng.
- Mỗi bài nộp `GRADED` cần Teacher xử lý được đưa vào hàng đợi chấm. Mọi bài giảng viên chấm (Text Essay, Diagram Essay, bài nhóm) đều có rubric theo câu hoặc theo phần; chấm tay là tích checklist rubric. Điểm lưu theo người chấm/thời gian, chấm tay hay dùng đề xuất AI lưu như nhau. Bài `PRACTICE` không tạo đánh giá chờ Teacher.
- Kết quả AI chưa duyệt không được coi là quyết định cuối đối với câu trả lời mở.
- Mọi lần sửa điểm lưu người thực hiện và thời gian; sửa điểm tự chấm hoặc điểm đã chốt phải có lý do, điểm khác đề xuất AI không cần lý do.
- Giảng viên chọn nhiều bài trong hàng chờ để AI chấm hàng loạt (đề xuất), rồi xác nhận từng bài trong Grading Workspace; workspace có nút chuyển bài và tự sang bài kế sau khi lưu.
- Điểm đã chốt được công bố từng bài nộp hoặc công bố hàng loạt các bài đã chốt được chọn trong hàng chờ.

### FR-009 - Sổ điểm và trạng thái bài nộp

Student xem điểm/phản hồi đã công bố và trạng thái của chính mình trên Student Assignments, Assignment Detail/Submission History (UC 22–23, 28); quiz trên Quiz Result (UC 21). Teacher hoặc Subject Manager được giao dạy R3/R4 xem/xuất gradebook lớp (UC 40); Admin bị từ chối, phân công quản lý môn không tự cấp quyền chấm/gradebook.

Sổ điểm chỉ chứa bài GRADED, không tính điểm tổng theo hệ số và không chứa AI Practice. Không lưu tiến độ hoàn thành hay vị trí học theo bài.

### FR-010 - Thanh toán

Hệ thống phải tích hợp một nhà cung cấp thanh toán để tạo giao dịch, nhận kết quả qua webhook và cộng credit AI tương ứng mà không lưu dữ liệu thẻ thanh toán thô.

Tài khoản `ACTIVE` với vai trò Student, Teacher hoặc Subject Manager có thể mua và xem credit của chính mình theo các gói credit do Administrator quản lý (FR-031) và được tặng cùng một mức credit hằng tháng (FR-021). Administrator không có ví credit vì không dùng chức năng AI (người dùng chốt 2026-10-09). Student được tiêu credit cho yêu cầu AI chấm attempt PRACTICE Text/Diagram Essay đã nộp của mình hoặc yêu cầu tóm tắt học liệu có quyền xem; Teacher/Subject Manager tiếp tục dùng credit cho các tính năng AI đúng quyền. Student không được AI soạn đề/chấm Graded hoặc embedding/RAG độc lập; được MATERIAL_SUMMARY và child embedding của cùng HOLD đã xác minh quyền xem. Code Lab, Quiz và bài nhóm không dùng credit Student để chấm. Nếu Teacher yêu cầu AI hỗ trợ chấm bài `GRADED`, credit được tính cho Teacher và chỉ kết quả cuối do Teacher công bố mới hiển thị cho Student.

Quyền dùng AI được kiểm theo vai trò hiện hành, quyền kế thừa và scope đối tượng: trong ngữ cảnh Student có chấm PRACTICE (FR-030) và tóm tắt học liệu từ View Material (FR-004/005); các quyền AI của Teacher và Subject Manager không tự chuyển sang ngữ cảnh Student. Mọi khoản trừ credit gắn với tài khoản thực hiện và loại tác vụ.

**Tiêu chí chấp nhận:**

- Webhook được xác minh chữ ký và xử lý idempotent.
- Job định kỳ tự đối soát trạng thái thanh toán với nhà cung cấp khi thiếu webhook; không có thao tác admin đối soát thủ công.
- Thanh toán lỗi, hết hạn hoặc chưa xác minh thì không cộng credit.
- Quyền lợi mua được là credit AI (quy đổi ra token khi gọi AI); thanh toán không mở hay chặn quyền vào lớp và nội dung học.
- UC 08–11 dùng My Credit Package, Public Credit Packages và Credit Package Checkout (mở từ Class Dashboard); kết quả pending/success/cancelled/failed của chính mình hiện trên Checkout khi PayOS quay về; lịch sử toàn hệ thống thuộc FR-032.
- Student có thể xem package, balance, lịch sử credit và tự thanh toán; backend kiểm quyền trên attempt và từ chối yêu cầu AI ngoài `PRACTICE` Text Essay/Diagram Essay trước khi giữ hoặc trừ credit.

### FR-011 - Email và thông báo

Hệ thống phải gửi được các thông báo thiết yếu như kích hoạt/khôi phục tài khoản, ghi danh, giao bài và kết quả đánh giá qua nhà cung cấp email.

### FR-012 - Tích hợp AI/LLM

Backend phải tích hợp nhà cung cấp AI/LLM qua một ranh giới dịch vụ rõ ràng, có timeout, giới hạn chi phí/sử dụng, xử lý lỗi và khả năng thay đổi nhà cung cấp mà không làm rò rỉ chi tiết vào nghiệp vụ cốt lõi.

### FR-013 - Lưu trữ tệp

Hệ thống phải lưu tệp học tập qua một dịch vụ lưu trữ riêng tư, cấp quyền truy cập ngắn hạn và không công khai trực tiếp object chứa dữ liệu học tập. Dung lượng tối đa và loại tệp cho phép theo mục đích do Administrator chỉnh trên Settings trong giới hạn hệ thống (FR-033).

### FR-014 - Audit nghiệp vụ và bảo mật

Administrator xem/tìm Audit Log (UC 73) theo actor, action, object, result và thời gian; chỉ đọc, không sửa/xóa log. Vai trò khác không có quyền tra cứu audit toàn nền tảng.

Hệ thống phải ghi sự kiện đăng nhập thất bại, thay đổi vai trò hoặc phạm vi môn, thay đổi học liệu, thay đổi điểm, phát hành bài của lớp/môn và quiz, sự kiện thanh toán và truy cập đặc quyền.

### FR-015 - Vòng đời tài khoản do quản trị viên quản lý

Quản trị viên xem danh sách/chi tiết, tìm kiếm, tạo, cập nhật thông tin được phép/role và khóa/mở khóa tài khoản (UC 59–63 theo bản 73 UC; nhập CSV là luồng phụ của UC 60); thao tác hàng loạt phải kiểm tra từng dòng và báo kết quả không làm mất các bản ghi hợp lệ. Quản trị viên không đặt, cấp hay xem mật khẩu người dùng và không kích hoạt việc gửi OTP. Tài khoản mới ở trạng thái chờ kích hoạt; tạo hoặc nhập tài khoản không gửi email. Chỉ khi người dùng yêu cầu kích hoạt ở lần đăng nhập đầu, hệ thống mới gửi OTP qua email để người dùng xác minh và tự đặt mật khẩu lần đầu. Yêu cầu gửi OTP được giới hạn tần suất.

### FR-016 - Ngân hàng câu hỏi và rubric

Ngân hàng câu hỏi chỉ có ở cấp môn (Subject Question Bank, UC 56–57): Chủ nhiệm môn quản lý trên Question List và Question Detail mở từ Manager Dashboard. Ngân hàng giữ câu của mọi dạng bài (trắc nghiệm cho quiz, tự luận cho Text Essay, code cho Code Lab, tài liệu làm khung cho Diagram Essay và bài nhóm); không có ngân hàng của lớp (người dùng chốt 2026-10-09). Rubric (UC 46) bắt buộc với Text Essay, Diagram Essay, bài nhóm, được hệ thống tự tạo và người soạn điền, sửa trong Rubric Detail của Assignment Form.

**Tiêu chí chấp nhận:**

- Tạo/sửa/xóa, tìm, nhập file và nhờ AI soạn câu kiểm phạm vi môn; câu là `MCQ_SINGLE`/`MCQ_MULTI`, `ESSAY`, `CODE` hoặc `DOCUMENT`.
- Sửa câu đã kích hoạt tạo version mới; không thay bài/quiz đã phát hành/attempt lịch sử. Xóa Draft chưa dùng, còn bản đã dùng thì ngưng để giữ lịch sử.
- Teacher không có ngân hàng riêng; khi soạn bài hoặc quiz chọn được câu ACTIVE khớp dạng của ngân hàng môn hoặc viết câu riêng của bài, không sửa ngân hàng môn. Admin không vào ngân hàng.
- Rubric (UC 46): hệ thống tự tạo rubric trống cho mỗi câu Text Essay và mỗi phần Diagram/Group; Quiz/Code Lab dùng đáp án/test. Rubric sửa trực tiếp khi bài còn nháp; bài không phát hành được khi còn rubric trống; khóa khi bài đã phát hành; copy bài tạo rubric độc lập.

### FR-017 - Các loại bài đánh giá và kiểm thử trước phát hành

Hệ thống phải hỗ trợ năm dạng assignment sau: `CODE_LAB` (`GRADED` hoặc `PRACTICE`), `TEXT_ESSAY` (`GRADED` hoặc `PRACTICE`), `DIAGRAM_ESSAY` (`GRADED` hoặc `PRACTICE`), `GROUP_ASSIGNMENT` (chỉ `GRADED`) và `MULTIPLE_CHOICE_QUIZ` (chỉ quiz luyện tập). Không có Simulation Exam. `TEXT_ESSAY` là bài viết văn bản thường, không giới hạn số từ hay số dòng, mỗi câu một rubric và điểm câu bằng tổng điểm rubric của câu. `DIAGRAM_ESSAY` dùng nền tảng `DOCUMENT` hiện có: trang soạn thảo kiểu Google Docs có heading, bảng, ảnh và khối sơ đồ Draw.io nhúng; Teacher có thể nhập khung từ DOCX; Diagram Essay bắt buộc có khung; khung Teacher khóa hoàn toàn (Student không sửa, xóa hay di chuyển chữ, ảnh, bảng, sơ đồ của khung, chỉ thêm nội dung của mình); Student có thể nhập DOCX vào lượt đang làm, xem trước rồi xác nhận để thêm nội dung của mình; bài nộp xuất được ra DOCX. Với khối sơ đồ, Student vẽ trên canvas Draw.io nhúng và lưu XML Draw.io đầy đủ. Bản đầy đủ là bài nộp chuẩn để xem/chấm và phải được giữ nguyên; khi Teacher yêu cầu AI hỗ trợ chấm bài `GRADED` hoặc Student yêu cầu AI chấm bài `PRACTICE`, hệ thống mới tạo bản XML rút gọn dẫn xuất theo schema/allowlist để gửi AI. Trước khi phát hành, Teacher hoặc Subject Manager phải xem trước và kiểm tra được cấu hình đặc thù của từng dạng bài.

Quiz là quiz luyện tập gắn với một học liệu (UC 35): Teacher tạo cho học liệu của lớp, Chủ nhiệm môn tạo cho học liệu của môn (mọi lớp của môn thấy); câu trắc nghiệm lấy từ ngân hàng câu hỏi của môn hoặc viết riêng; tự chấm theo đáp án, không có lịch mở/đóng, không nằm trong danh sách bài tập, không vào sổ điểm; sinh viên làm từ Learning Material hoặc Quiz Practice (UC 18–21). Code Lab `PRACTICE` được tự chấm theo test. Kết quả luyện tập chỉ hiển thị cho Student, không vào sổ điểm chính thức.

### FR-018 - Lưu nháp, lần nộp và khôi phục bài làm

Hệ thống phải tự động lưu bản nháp theo người học/bài đánh giá, khôi phục an toàn sau gián đoạn và lưu lịch sử các lần nộp; bản nháp không được coi là bài nộp chính thức.

### FR-019 - Theo dõi nộp bài và nhắc nhở

Giảng viên phải xem được trạng thái đã nộp, chưa nộp, đang làm và nộp trễ của lớp được phân công. Hệ thống tự nhắc người học chưa nộp 24 giờ trước hạn (một lần mỗi bài); giảng viên không gửi nhắc thủ công.

### FR-020 - Chốt điểm hàng loạt

Giảng viên phải có thể kiểm tra và chốt điểm hàng loạt cho lớp được phân công; chỉ bài đã đủ điều kiện mới được chốt và mọi thay đổi điểm phải được audit.

### FR-021 - Quản trị và giám sát dịch vụ AI

Administrator cấu hình model, quota, trần chi phí, kill-switch và xem trạng thái/chi phí an toàn qua port provider-neutral. Cấu hình AI nằm trên màn Settings (UC 70–71, FR-033); không thay thế quản trị gói credit.

Mỗi lời gọi AI tạo nội dung/embedding tính credit theo token: tóm tắt và embedding học liệu tính người bấm nút, truy xuất tính người yêu cầu AI. Student được yêu cầu tóm tắt học liệu trong quyền xem; không được truy xuất RAG/soạn đề. Vượt hạn mức hệ thống báo "Hệ thống đang bận"; yêu cầu bị từ chối không trừ credit.

Mức tặng credit định kỳ do Administrator chỉnh trên Settings (UC 70–71, FR-033), áp dụng như nhau cho Student, Teacher và Subject Manager, đổi có hiệu lực từ kỳ đặt lại kế tiếp. Gói bán do Administrator thêm/sửa theo FR-031; giao dịch giữ snapshot giá/credit.

### FR-022 - Đã rút khỏi phạm vi: Tự ghi danh bằng mã mời

Người dùng chốt bỏ ngày 2026-10-09 vì không có trong 73 UC; sinh viên chỉ vào lớp khi Chủ nhiệm môn thêm (UC 51). Mã FR-022 giữ làm dấu vết và không dùng lại.

### FR-023 - Thông báo lớp

Teacher của lớp (hoặc Subject Manager được giao dạy, R4) tạo/cập nhật/xóa thông báo (UC 36); Student ghi danh và giảng viên của lớp xem (UC 30) trên Class Announcements. Màn mở từ Class Dashboard, gộp thông báo của các lớp mình học hoặc dạy và lọc được theo lớp.

**Tiêu chí chấp nhận:**

- Chỉ tài khoản được giao dạy lớp tạo/sửa/xóa; Student không đăng thông báo; Admin không vào lớp.
- Tạo mới gửi in-app notification cho Student đang ghi danh; lưu actor/thời gian. Sửa kiểm version, audit trước/sau, không gửi lại sự kiện tạo mới.
- Xóa đánh dấu đã xóa và loại khỏi feed, giữ audit/tham chiếu.
- Đọc/sửa/xóa ngoài lớp bị từ chối; kiểm giới hạn và đầu vào không an toàn.
- Không có bình luận dưới thông báo (người dùng chốt bỏ 2026-10-09).

### FR-024 - Thống kê quản trị và xuất bảng điểm

Administrator xem Admin Dashboard (UC 58): số tài khoản theo role/status, môn/lớp theo status và enrollment ACTIVE; chỉ số đếm, không có dữ liệu cá nhân hoặc dashboard Student.

Teacher hoặc Subject Manager được giao dạy lớp R3/R4 xem/xuất GradeBook CSV/XLSX (UC 40). Kiểm toàn bộ bộ lọc trước khi tạo tệp; ghi rõ chưa nộp/chưa chốt, không tính điểm tổng, không xuất quiz hoặc Practice; Admin không có quyền.

Phân bố điểm ẩn danh là luồng phụ của Student Assignments (UC 22): chỉ Chủ nhiệm môn bật/tắt cho lớp trong Edit Class Information (UC 52), mặc định tắt; chỉ bài GRADED đủ mẫu riêng tư, không quiz/Practice, bài của môn tính riêng từng lớp. Tiến độ nộp hiện trên Student Submissions (UC 37), chỉ R3/R4 của lớp. AI Grading Proposals thuộc UC 38, chấm/chốt điểm thuộc UC 39, công bố/xem/xuất Gradebook thuộc UC 40. Phân tích chất lượng câu hỏi/độ lệch điểm AI nằm ngoài phạm vi.

### FR-025 - Quản lý nhóm và trưởng nhóm

Trong danh sách sinh viên của lớp được phân công, giảng viên phải có thể chia sinh viên thành nhiều nhóm bằng cách tạo tay hoặc chia ngẫu nhiên các sinh viên chưa có nhóm, và chỉ định chính xác một trưởng nhóm cho mỗi nhóm. Nhóm thuộc lớp và được dùng cho mọi bài nhóm của lớp; không có chức năng dùng lại nhóm của bài khác. Thành viên có thể gửi yêu cầu đổi trưởng nhóm nhưng chỉ giảng viên được phê duyệt/từ chối và chỉ định người thay thế.

**Tiêu chí chấp nhận:**

- Mỗi nhóm luôn có đúng một trưởng nhóm đang hiệu lực trước khi nhận bài nhóm.
- Chỉ sinh viên đang ghi danh trong lớp mới được thêm vào nhóm của lớp đó; mỗi sinh viên thuộc tối đa một nhóm trong lớp.
- Chia ngẫu nhiên chỉ chia sinh viên chưa có nhóm, giữ nguyên nhóm đã có, các nhóm mới chênh nhau tối đa một người và giảng viên xem trước trước khi lưu.
- Một thay đổi trưởng nhóm chỉ có hiệu lực sau quyết định của giảng viên và được audit.

### FR-026 - Bài tập nhóm: tài liệu chung và điểm đóng góp cá nhân

Bài tập nhóm là một bài tài liệu (DOCUMENT) chung của nhóm. Giảng viên soạn khung với heading kiểu Word; hệ thống tự chia khung thành các phần theo heading nhỏ nhất của mỗi nhánh (ví dụ sơ đồ use case, activity) và giảng viên tạo một rubric cho mỗi phần; mỗi phần là một phần việc để chia cho thành viên. Trưởng nhóm giao từng phần cho thành viên, thành viên cũng có thể tự nhận phần còn trống; không có mục chi tiết. Người giữ phần sửa trong popup che kín trang và bấm Xong để ghép vào bản chung realtime. Trưởng nhóm nộp bất kỳ lúc nào trước hạn; hết hạn hệ thống tự nộp. Giảng viên chấm tài liệu chung theo rubric của từng phần (tay hoặc AI đề xuất); điểm tài liệu chung là tổng các phần, điểm đóng góp từng thành viên mặc định bằng điểm tài liệu chung.

**Tiêu chí chấp nhận:**

- Mỗi phần có trạng thái (trống, đang làm, xong) và lịch sử phiên bản theo tác giả; tài liệu có trạng thái đang làm hoặc đã đóng (sau khi tự nộp); cả bài dùng chung một hạn.
- Chỉ trưởng nhóm giao phần; không ai thêm, xóa hay đổi tên phần của khung.
- Trưởng nhóm nộp được bất kỳ lúc nào trước hạn và nộp lại được; còn phần chưa xong thì được cảnh báo.
- Tại một thời điểm mỗi phần chỉ một thành viên nhận và sửa; trưởng nhóm hoặc giảng viên có thể nhả khóa phần khi cần (cảnh báo bỏ bản nháp chưa xong), có audit.
- Tài liệu chung được cập nhật realtime khi một phần xong; bản trưởng nhóm nộp (hoặc tự nộp khi hết hạn) là bản bất biến dùng để chấm, giữ tác giả từng phần.
- Giảng viên xem được bản nộp cuối của tài liệu chung cùng tác giả từng phần; bản nộp đó là bản dùng để chấm.
- Tài liệu chung chấm như bài `DOCUMENT`: chấm tay theo rubric của từng phần (điểm tài liệu chung là tổng các phần) hoặc AI đề xuất khi giảng viên yêu cầu; tài liệu hiển thị bình thường, không tô màu theo tác giả.
- Không có điểm tích hợp riêng: lỗi các phần không khớp nhau khi ghép được trừ ở rubric của phần liên quan, điểm tài liệu chung là tổng các phần; một thành viên chỉ bị trừ thêm (qua điểm đóng góp) khi giảng viên xác định được phần hoặc thành viên gây lỗi.
- Mỗi sinh viên có điểm đóng góp riêng, mặc định bằng điểm tài liệu chung nên mọi thành viên như nhau; giảng viên có thể chấm tay điểm đóng góp từng người. Không có công thức tự động.
- Lý do khi sửa điểm một thành viên hoặc quy kết lỗi cho một thành viên là tùy chọn; mọi thay đổi audit actor/thời gian. Nhóm nộp lại thì xử lý như lượt nộp mới của bài `DOCUMENT`: bản nộp cuối được chấm lại.

### FR-027 - Bài cấp môn giao cho mọi lớp

Chủ nhiệm môn (R2) soạn, duyệt và phát hành bài của môn (Text Essay, Diagram Essay, Code Lab; `GRADED` hoặc `PRACTICE`) trên Assignment List mở từ Manager Dashboard. Bài của môn hiện ở mọi lớp `OPEN` của môn với một lịch chung; giảng viên của từng lớp chấm, chốt, công bố điểm cho sinh viên lớp mình. Thay cho template đề cấp môn trước đây (người dùng chốt 2026-10-09).

**Tiêu chí chấp nhận:**

- Chỉ Chủ nhiệm môn của môn tạo, sửa, duyệt, phát hành, sửa lịch, ngưng giao, tạo version bài của môn; giảng viên lớp chỉ xem.
- Bài của môn không có dạng bài nhóm (nhóm thuộc từng lớp).
- Bài của môn hiện cả ở lớp mở sau khi phát hành; người học chỉ thấy khi bài `OPEN`/`CLOSED` như bài của lớp.
- Điểm bài của môn vào sổ điểm của từng lớp và được công bố theo từng lớp.

### FR-028 - Sao chép assignment và rubric giữa các lớp

Giảng viên phải có thể copy assignment và rubric từ một lớp sang lớp khác mà chính giảng viên đang được phân công.

**Tiêu chí chấp nhận:**

- Backend kiểm tra quyền của giảng viên trên cả lớp nguồn và lớp đích.
- Bản copy là draft độc lập, giữ nguồn gốc để audit nhưng không đồng bộ hai chiều.
- Assignment copy loại bỏ lịch phát hành, deadline, attempt, bài nộp và điểm; rubric copy giữ cấu trúc/tiêu chí nhưng có identity/version riêng ở lớp đích.
- Không cho copy sang lớp ngoài phạm vi được phân công, kể cả khi thuộc cùng môn.

### FR-029 - Đã rút khỏi phạm vi: Simulation Exam

Theo câu trả lời làm rõ ngày 2026-09-29, Simulation Exam được bỏ hoàn toàn khỏi MVP; mã FR-029 giữ làm dấu vết và không dùng lại.

### FR-030 - Student dùng credit AI để chấm bài luyện tập

Student có thể dùng credit để AI chấm một attempt `PRACTICE` dạng `TEXT_ESSAY` hoặc `DIAGRAM_ESSAY` đã nộp của chính mình. Nộp bài không tự chấm; Student bấm "Chấm với AI" trên bài đã nộp, hệ thống kiểm credit tại lúc bấm. Mỗi attempt có tối đa một kết quả AI hợp lệ. Thiếu credit thì báo, Student mua thêm rồi bấm lại được. Quá 5 phút chưa có kết quả thì báo lỗi và trả credit. Kết quả luyện tập chỉ dành cho Student, không phải điểm chính thức và không cần Teacher duyệt. Teacher không chấm bài `PRACTICE`.

**Tiêu chí chấp nhận:**

- Backend kiểm tra tài khoản `ACTIVE`, enrollment, quyền trên assignment/attempt, chế độ `PRACTICE`, dạng bài hợp lệ và credit trước khi gọi AI; gọi API trực tiếp không vượt qua các kiểm tra này.
- Khi Student bấm chấm và đủ credit, hệ thống tạo một yêu cầu chấm AI cho attempt đó và tính credit theo token thực dùng; retry kỹ thuật hoặc gửi lại cùng yêu cầu không tạo lần chấm hay khoản trừ credit thứ hai.
- Nếu Student không đủ credit khi bấm chấm, hệ thống không gọi AI, không trừ credit và báo thiếu credit; bài vẫn ở trạng thái chưa chấm AI để bấm lại sau khi mua credit.
- Yêu cầu chấm quá 5 phút chưa có kết quả thì báo lỗi, trả credit đã giữ; Student bấm lại được.
- AI lỗi, hết quota hoặc kết quả không hợp lệ không làm mất bài nộp; credit giữ chưa dùng được trả lại (`credit_status = RELEASED`). Retry kỹ thuật cho cùng yêu cầu chấm không vượt quá một kết quả hợp lệ cho attempt đó.
- Code Lab và Quiz `PRACTICE` dùng test/đáp án để tự chấm, không gọi AI và không trừ credit Student. `GROUP_ASSIGNMENT` không có `PRACTICE`.
- Kết quả luyện tập được tách khỏi gradebook, phân bố điểm và file xuất điểm; Teacher không có hàng đợi chấm/chốt/công bố điểm cho bài này.

### FR-031 - Quản trị gói credit

Administrator xem gói trên Credit Package List (UC 68), thêm/sửa thông tin, giá, số credit và trạng thái bán trong popup Credit Package Detail (UC 69). Mức tặng hằng tháng chỉnh trên Settings, không thuộc thao tác này.

**Tiêu chí chấp nhận:**

- Chỉ Administrator thực hiện; gói có thông tin định danh hợp lệ, giá dương và credit nguyên dương.
- Thêm/sửa kiểm dữ liệu/version và audit actor/thời gian/giá trị trước-sau; cập nhật chỉ áp dụng giao dịch tạo sau đó.
- Giao dịch đã tạo giữ package ID và snapshot thông tin/giá/credit; sửa gói không sửa giao dịch hoặc số dư đã cấp.
- Không thêm chức năng xóa gói, hoàn tiền hoặc chỉnh credit người dùng thủ công.

### FR-032 - Lịch sử thanh toán toàn nền tảng

Administrator xem Payment History (UC 72): tài khoản mua, gói, số tiền, thời gian và trạng thái.

**Tiêu chí chấp nhận:**

- Chỉ Administrator đọc toàn nền tảng; vai trò khác chỉ xem kết quả/lịch sử của chính mình.
- Phân trang và lọc tài khoản/gói/thời gian/trạng thái, sử dụng snapshot giao dịch.
- Đọc không đổi status, không cộng/trừ credit và không đối soát thủ công; không lộ secret PayOS hoặc dữ liệu thẻ.

### FR-033 - Cài đặt hệ thống

Administrator xem (UC 70) và sửa (UC 71) cài đặt trên màn Setting List và Setting Detail, gồm ba nhóm: AI (model, giới hạn sử dụng, trần chi phí, kill-switch), credit tặng định kỳ, và dung lượng/loại tệp tải lên theo mục đích (người dùng chốt 2026-10-09, U03 giữ chung).

**Tiêu chí chấp nhận:**

- Chỉ Administrator xem và sửa; mỗi mục có giới hạn hợp lệ do hệ thống đặt, giá trị ngoài giới hạn bị từ chối.
- Sửa kiểm version để không ghi đè thay đổi đồng thời, ghi audit trước/sau, có hiệu lực trong tối đa 30 giây và không ảnh hưởng dữ liệu đã tạo (tệp đã tải, giao dịch cũ).
- Bí mật (API key, mật khẩu SMTP, khóa Google Drive) không nằm trong Settings, vẫn là cấu hình triển khai.

## 5. Luồng người dùng chính

### USCN-001 - Chuẩn bị và giao bài cấp lớp bằng AI

Trong lớp được Chủ nhiệm môn tạo và phân công, giảng viên tải học liệu (tệp hoặc video YouTube), yêu cầu AI tạo câu hỏi, chỉnh sửa và duyệt bản nháp, sau đó xuất bản bài đánh giá cho lớp.

### USCN-001A - Quản lý học liệu, ngân hàng và bài cấp môn

Chủ nhiệm môn R2 quản lý học liệu/ngân hàng của môn và soạn bài của môn, có thể dùng AI đề xuất. Bài Text Essay/Code Lab/Diagram Essay được duyệt/phát hành cho mọi lớp OPEN với một lịch chung, không bài nhóm. Giảng viên từng lớp chấm/công bố cho sinh viên lớp mình. Quiz của môn gắn học liệu, không lịch/gradebook; không template hay bước copy template vào lớp.

### USCN-002 - Học và nhận phản hồi

Người học đăng nhập, truy cập lớp được ghi danh, học nội dung, làm bài, nộp bài và xem điểm/phản hồi khi được công bố.

### USCN-003 - Duyệt chấm điểm AI

Sau khi nhận bài nộp, giảng viên chọn chấm thủ công hoặc yêu cầu AI đề xuất điểm theo rubric. Nếu dùng AI, giảng viên kiểm tra, chấp nhận hoặc ghi đè đề xuất trước khi công bố; điểm lưu như chấm tay (không ghi riêng là dùng AI) và audit quyết định cuối.

### USCN-004 - Thanh toán và cộng credit

Student/Teacher/Subject Manager chọn gói trên Public Credit Packages; PayOS xử lý, kết quả owner-only hiện trên Credit Package Checkout. Backend verified webhook/tự đối soát cộng credit đúng một lần; không đổi enrollment. Admin quản trị gói/historical query riêng, không có ví.

### USCN-005 - Xử lý lỗi phụ thuộc

Khi AI, email, lưu trữ hoặc thanh toán tạm thời không khả dụng, hệ thống không làm mất dữ liệu nghiệp vụ, hiển thị trạng thái an toàn và cho phép retry có kiểm soát.

### USCN-006 - Thực hiện và đánh giá bài tập nhóm

Giảng viên chia nhóm trong lớp, chỉ định một trưởng nhóm và soạn khung tài liệu; hệ thống tự chia khung thành các phần theo heading nhỏ nhất của mỗi nhánh, mỗi phần một rubric. Trưởng nhóm giao phần hoặc thành viên tự nhận phần, sửa trong popup che kín trang (chỉ hiện nhánh của phần) rồi bấm Xong để ghép realtime vào tài liệu chung; trưởng nhóm nộp bất kỳ lúc nào trước hạn, hết hạn thì hệ thống tự nộp gồm phần đang làm. Giảng viên chấm tài liệu chung theo rubric của từng phần (điểm tài liệu chung là tổng các phần); điểm đóng góp từng thành viên mặc định bằng điểm tài liệu chung, giảng viên chấm tay khi cần.

## 6. Yêu cầu phi chức năng

### NFR-001 - Công nghệ

- Frontend: Next.js với TypeScript.
- Backend: Java với Spring Boot.
- Giao tiếp frontend-backend qua API web có versioning hoặc quy ước tương thích rõ ràng.
- Database và nhà cung cấp bên ngoài sẽ được chọn trong NFR/Application Design, ưu tiên giải pháp ít vận hành cho MVP.

### NFR-002 - Khả năng sử dụng và truy cập

- Giao diện người học phải desktop-first và tối ưu cho trình duyệt máy tính, đặc biệt canvas Draw.io và các luồng làm bài; tablet/mobile vẫn phải responsive cho đăng nhập, đọc nội dung, thông báo và xem kết quả nhưng không phải mục tiêu chính cho thao tác vẽ/làm bài phức tạp.
- Các luồng cốt lõi phải dùng được bằng bàn phím và có nhãn hỗ trợ công nghệ trợ năng.
- Thông báo lỗi phải nêu hành động khắc phục mà không lộ chi tiết nội bộ.

### NFR-003 - Quy mô và hiệu năng

- Quy mô mục tiêu ban đầu: dưới 1.000 tài khoản và dưới 100 người dùng đồng thời.
- API đồng bộ không phụ thuộc AI nên đạt p95 không quá 500 ms trong điều kiện tải mục tiêu, không tính độ trễ mạng công cộng.
- Tác vụ xử lý tệp và tạo nội dung AI phải chạy bất đồng bộ khi có thể, cung cấp trạng thái thay vì giữ request không giới hạn.
- Mọi external call phải có timeout; retry phải có giới hạn và backoff.

### NFR-004 - Khả năng kiểm thử

- MVP phải có kiểm thử ở các cấp độ: unit test, integration test, system test và end-to-end test cho các hành trình cốt lõi.
- System test phải kiểm thử toàn diện sự phối hợp giữa Frontend (Next.js), Backend (Spring Boot), Database và các mock/sandbox của dịch vụ bên ngoài (AI, Payment Gateway) trên môi trường container trước khi triển khai.
- Contract/webhook test phải bao phủ thanh toán và các adapter bên ngoài quan trọng.
- Không áp dụng Property-Based Testing theo quyết định của người dùng.

### NFR-005 - Môi trường chạy

- Môi trường phát triển và demo phải chạy local bằng container với hướng dẫn tái tạo được.
- Secret chỉ được truyền qua biến môi trường hoặc secret store, không commit vào repository.
- Image và dependency phải khóa phiên bản; không dùng tag `latest` cho artifact triển khai.

## 7. Yêu cầu bảo mật và quyền riêng tư

Phạm vi rút gọn cho đồ án sinh viên, chỉ giữ các control rẻ để làm và cần có. Rule ngoài phạm vi ghi ở mục 12.

### SEC-001 - Mật khẩu và phiên

Mật khẩu băm bằng bcrypt, tối thiểu 8 ký tự có chữ và số. Sai 5 lần thì khóa tạm. Token nằm trong cookie `HttpOnly`, `Secure`, `SameSite`, có hạn và bị thu hồi khi đăng xuất. Không có MFA, không kiểm danh sách mật khẩu bị lộ.

### SEC-002 - Phân quyền

Mọi API mặc định yêu cầu đăng nhập, trừ các endpoint được đánh dấu public. Quyền được kiểm phía server ở mức chức năng và đối tượng; frontend ẩn nút chỉ để tiện dùng.

### SEC-003 - Kiểm tra đầu vào

Mọi request body và tham số được validate kiểu, độ dài và định dạng. Truy vấn database luôn tham số hóa. Endpoint public có giới hạn tần suất.

### SEC-004 - Header HTTP

Nginx thêm `Content-Security-Policy: default-src 'self'`, `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`. Truy cập từ Internet dùng HTTPS với chứng chỉ Let's Encrypt miễn phí; dùng tên miền có sẵn, nếu chưa có thì dùng subdomain miễn phí (DuckDNS).

### SEC-005 - Log

Log không chứa mật khẩu, OTP, token hay dữ liệu cá nhân nhạy cảm. Sự kiện đăng nhập thất bại, đổi quyền và thao tác đặc quyền được ghi audit.

### SEC-006 - Xử lý lỗi và cấu hình an toàn

Có global error handler; phản hồi lỗi không lộ stack trace, đường dẫn hay chi tiết database. Không có mật khẩu mặc định. Tắt Swagger và endpoint debug ở production. Secret không commit vào repository. Dependency khóa phiên bản.

### SEC-007 - Thanh toán

Không lưu thông tin thẻ. Webhook phải xác minh chữ ký và xử lý idempotent.

## 8. Yêu cầu vận hành

### REL-001 - Triển khai

Chạy trên VPS nhóm đã có sẵn bằng Docker Compose; không dùng dịch vụ trả phí mới. Image gắn tag theo commit; rollback bằng cách chạy lại tag trước. Migration database tương thích ngược.

### REL-002 - Health check

Mỗi container có healthcheck trong Docker Compose; backend có `/health`.

### REL-003 - Timeout

Mọi lời gọi ra ngoài (database, Redis, SMTP, AI, thanh toán, unit khác) có timeout hữu hạn và retry có giới hạn. Lỗi phụ thuộc thì từ chối an toàn.

### REL-004 - Ngoài phạm vi đồ án

Không có multi-zone, auto-scaling, backup, DR, runbook failover, chaos testing, incident response, dashboard hay cảnh báo tự động. Log xem bằng `docker compose logs`.

### REL-005 - Không phát sinh chi phí

Mọi thành phần bảo mật và vận hành phải miễn phí: thư viện mã nguồn mở, Let's Encrypt, GitHub Actions và GHCR với repository public, Mailpit khi phát triển, Brevo SMTP khi demo/production (trong hạn mức gói miễn phí).

Ngoại lệ duy nhất là lời gọi AI (Google Gemini: LLM và embedding): ưu tiên gói miễn phí, được phép tốn ít chi phí nhưng phải có trần quota/chi phí và kill-switch theo FR-021.

## 9. Ràng buộc và giả định đã xác nhận

- Chỉ một tổ chức trong MVP.
- Bốn vai trò trong MVP gồm `STUDENT`, `TEACHER`, `SUBJECT_MANAGER` và `ADMIN`; `LEARNER`/`INSTRUCTOR` là tên cũ cần được thay thế ở các tài liệu và thiết kế phụ thuộc.
- Không có role Head of Department/Trưởng bộ môn trong hệ thống.
- Chủ nhiệm môn là vai trò RBAC riêng, được gán phạm vi một hoặc nhiều môn; giảng viên vẫn quản lý nội dung riêng của lớp được phân công.
- Chỉ web responsive.
- Học liệu chỉ tải lên (PDF/DOCX/PPTX) hoặc gắn link một video YouTube; không soạn nội dung trực tiếp.
- Năm dạng assignment hiện hành là Code Lab, Text Essay, Diagram Essay (dùng nền tảng DOCUMENT và Draw.io), Group Assignment và Multiple-Choice Quiz; chế độ hợp lệ theo FR-017. XML sơ đồ đầy đủ được giữ trong bài nộp, bản rút gọn chỉ là dữ liệu dẫn xuất gửi AI khi Teacher yêu cầu với bài `GRADED` hoặc Student yêu cầu với bài `PRACTICE`.
- Bài tập nhóm là một tài liệu chung theo khung của giảng viên; mỗi mục tại một thời điểm chỉ một thành viên sửa, thay đổi được ghép realtime khi mục xong; không soạn đồng thời từng phím gõ.
- Mỗi nhóm có đúng một trưởng nhóm do giảng viên chỉ định; trưởng nhóm giao phần hoặc thành viên tự nhận phần, trưởng nhóm nộp tài liệu chung (hết hạn thì hệ thống tự nộp), giảng viên chấm tài liệu chung theo rubric từng phần (tay hoặc AI đề xuất).
- Không có Simulation Exam hoặc kỳ thi chính thức/proctored exam.
- Copy/version/clone bài do U08 giữ; bản copy lớp có identity/rubric độc lập và truy vết nguồn, không copy lịch/kết quả. Bài của môn là một bài lịch chung giao mọi lớp OPEN, không phải template/copy cho từng lớp.
- Tích hợp bắt buộc gồm AI/LLM, lưu trữ tệp, thanh toán mua credit AI và email/thông báo.
- Triển khai đợt đầu ưu tiên local container.
- MVP phải có test tự động (bao gồm unit test, integration test, system test, e2e test), tài liệu chạy và khả năng triển khai thử nghiệm.
- Quản trị quy trình AI-DLC: Repository phải duy trì state tracking, audit trail, requirements, user stories, thiết kế, kế hoạch code, kết quả kiểm thử và các checkpoint phê duyệt trong `aidlc-docs/`; mã nguồn ứng dụng không được đặt trong thư mục này.
- Nhà cung cấp được chọn ở Construction: Gemini (AI), PayOS (thanh toán), Brevo SMTP/Mailpit (email), Google Shared Drive (file), PostgreSQL + pgvector (database), VPS chạy Docker Compose (xem `construction/shared-infrastructure.md`).

## 10. Tiêu chí thành công của MVP

- Quản trị viên hoặc Chủ nhiệm môn có thể tạo lớp; giảng viên của lớp có thể đưa nội dung vào hệ thống, dùng AI tạo và duyệt bài đánh giá.
- Một giảng viên có thể quản lý rubric/câu hỏi theo version, copy assignment/rubric giữa các lớp được phân công, giao bài `GRADED` hoặc `PRACTICE` đúng dạng, theo dõi nộp bài `GRADED` và chốt điểm hàng loạt.
- Chủ nhiệm môn quản lý học liệu/RAG (gồm caption YouTube), ngân hàng và bài của môn; bài môn một lịch chung mọi lớp OPEN, Teacher từng lớp chấm; không bài nhóm cấp môn.
- Một Student được ghi danh có thể học, nộp bài, xem điểm chính thức đúng quyền, mua credit và nhận điểm/phản hồi AI cho attempt luyện tập Text Essay/Diagram Essay đã nộp khi bấm chấm và đủ credit (thiếu credit thì mua thêm rồi bấm lại).
- Bản nháp và lịch sử lần nộp của người học được bảo toàn qua gián đoạn mà không bị coi nhầm là bài nộp chính thức.
- Quản trị viên có thể quản lý vòng đời tài khoản và kiểm soát quota/kill-switch/chi phí AI mà không khóa hệ thống vào một provider.
- Một nhóm cùng làm tài liệu chung theo mục và trưởng nhóm nộp; tài liệu chung chấm như bài `DOCUMENT` (AI chỉ đề xuất), điểm đóng góp thành viên mặc định bằng điểm tài liệu chung và giảng viên quyết định điểm cuối.
- Luồng thanh toán thử nghiệm cấp quyền chính xác và chống xử lý webhook trùng lặp.
- Các vai trò không thể truy cập dữ liệu hoặc chức năng ngoài quyền.
- Dữ liệu và hành động nhạy cảm có audit trail phù hợp.
- Dự án chạy được local bằng container và bộ test cốt lõi chạy tự động.
- Tài liệu AI-DLC phản ánh đầy đủ quyết định, checkpoint và trạng thái triển khai.

## 11. Truy vết nguồn yêu cầu

Các dòng theo ngày trước 2026-10-08 là nguồn lịch sử; mã UC ở đó là mã tại thời điểm tương ứng. Bản hiện hành dùng 73 UC; các hàng nguồn theo ngày trước đó giữ số UC lịch sử.


| Nguồn | Yêu cầu liên quan |
|---|---|
| Catalog local 73 UC và G21_Diagrams Page-2, revision 2026-10-09 | FR-001–033 theo scope; Settings U03, copy/bài môn U08, quyền Admin và story/unit map |
| SRS 4.1/4.2/4.4 và Screen Flow mới, 2026-10-08 (historical) | 70 UC; FR-001–003, FR-004–010, FR-015–016, FR-023–024, FR-027, FR-030–032; story/UC/unit map và màn mới |
| Phiếu xác minh Q1-Q14 | FR-001 đến FR-014, NFR-001 đến NFR-005 |
| Security Baseline Q15 | SEC-001 đến SEC-007 và mục 12 (phạm vi rút gọn) |
| Resiliency Baseline Q16 | REL-001 đến REL-004 và mục 13 (phạm vi rút gọn) |
| Property-Based Testing Q17 | NFR-004, extension bị tắt |
| Làm rõ vòng 1 Q1-Q10 | Web, tích hợp, criticality, DR, change, CI/CD, rollback, topology, incident response |
| Làm rõ vòng 2 Q1-Q2 | Direct/in-place; production single-region multi-zone (sau đó bỏ multi-zone khi rút gọn phạm vi 2026-09-24) |
| Làm rõ User Stories Q1-Q3 | Vai trò Chủ nhiệm môn, quyền phát hành đề chung (sau đó đã loại đề chung, 2026-09-24) và ranh giới học liệu cấp môn/lớp |
| Đối chiếu `uc1.pdf` và yêu cầu ngày 2026-09-13 | FR-015 đến FR-024; loại Head of Department; dùng chung loại bài viết luận; phân tách MVP và Phase 2 |
| Yêu cầu bài tập nhóm ngày 2026-09-13 | FR-025, FR-026; nhóm/leader và phần cá nhân; cơ chế trưởng nhóm nộp DOCX chung đã được change request 2026-09-22 thay thế bằng tài liệu do hệ thống tổng hợp |
| Change request và làm rõ ngày 2026-09-22 | FR-004, FR-016, FR-026 đến FR-029; YouTube RAG, question version, template/copy, simulation exam và tổng hợp/chấm bài nhóm |
| Change request và câu trả lời ngày 2026-09-29 | FR-002, FR-007, FR-008, FR-010, FR-017, FR-029 (rút), FR-030; đổi role, năm dạng assignment, Practice/Graded, Student mua credit và chấm AI bài luyện tập |
| Đồng bộ tài liệu ngày 2026-10-01 | FR-025, FR-026; nhóm cấp lớp và chia ngẫu nhiên, trưởng nhóm thêm/giao mục chi tiết, trạng thái review trước khi nộp; UC 9, 16, 22, 24, 27, 36 |
| Tách bài nhóm ngày 2026-10-01 | FR-025, FR-026; bảng 40 use case: UC 27 Manage Group Assignment (tạo, sửa bài nhóm, mục chính, nhả khóa mục), UC 28 Manage Assignments; UC 28–39 cũ thành 29–40 |
| Gộp danh mục use case ngày 2026-10-01 | FR-006, FR-027; bảng 39 use case (40 tại thời điểm đó): AI soạn bài thuộc Manage Assignments (UC 28 tại thời điểm đó), UC 21 Manage Templates gồm tạo thủ công/AI, sửa, xoá template |
| Gộp danh mục use case ngày 2026-09-30 | FR-016; bảng 40 use case (sau đó 39 rồi 40 tại thời điểm đó); từ 2026-10-01 câu hỏi mọi dạng bài thuộc ngân hàng, dùng trong UC 23–27. Truy vết story ↔ use case nằm trong `stories.md` mục 14 |

## 12. Phạm vi Security Baseline

Rút gọn cho đồ án sinh viên theo quyết định của người dùng ngày 2026-09-24.

| Rule | Áp dụng | Đáp ứng bởi / lý do |
|---|---|---|
| SECURITY-03 | Có | SEC-005 |
| SECURITY-04 | Có | SEC-004 |
| SECURITY-05 | Có | SEC-003 |
| SECURITY-08 | Có | SEC-002 |
| SECURITY-09 | Có | SEC-006 |
| SECURITY-12 | Có, rút gọn | SEC-001; không MFA, không kiểm mật khẩu bị lộ |
| SECURITY-15 | Có | SEC-006 |
| SECURITY-01, 02, 06, 07, 10, 11, 13, 14 | Không | Ngoài phạm vi đồ án: mã hóa at rest, access log tập trung, IAM cloud, network nhiều lớp, SBOM/quét lỗ hổng, misuse-case analysis, phân quyền CI/CD, alerting |

## 13. Phạm vi Resiliency Baseline

| Rule | Áp dụng | Đáp ứng bởi / lý do |
|---|---|---|
| RESILIENCY-04 | Có | REL-001 |
| RESILIENCY-06 | Có | REL-002 |
| RESILIENCY-10 | Có, chỉ timeout | REL-003; không circuit breaker |
| RESILIENCY-01, 02, 03, 05, 07, 08, 09, 11, 12, 13, 14, 15 | Không | Ngoài phạm vi đồ án (REL-004) |
