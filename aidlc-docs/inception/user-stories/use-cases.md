# Danh mục Use Case - AI-Powered Learning Platform

## 1. Mục đích

Tài liệu liệt kê các use case theo mục tiêu nghiệp vụ của người dùng để phục vụ thiết kế, kiểm thử và nghiệm thu. Các thao tác hỗ trợ như tìm kiếm, lọc, xem trạng thái tác vụ nền, retry và xử lý tự động không được tách thành use case riêng mà được mô tả trong use case nghiệp vụ liên quan.

Danh mục giữ 90 use case và 59 story để truy vết; 87 use case và 57 story thuộc phạm vi hiện hành. `US-LRN-002` và `US-LRN-003` cùng ba use case tiến độ bài học được đánh dấu ngoài phạm vi. Chức năng chưa thuộc MVP được đánh dấu `(Phase 2)`.

## 2. Tác nhân

| Mã | Tác nhân | Trách nhiệm |
|---|---|---|
| ACT-01 | Người học | Truy cập lớp, học, làm và nộp bài, tham gia nhóm, thanh toán và xem kết quả của mình |
| ACT-02 | Giảng viên | Quản lý lớp được phân công, nội dung lớp, nhóm, assignment, bài nộp và điểm |
| ACT-03 | Chủ nhiệm môn | Kế thừa chức năng giảng viên; quản lý học liệu, ngân hàng và assignment chung trong phạm vi môn hoặc lớp được giao |
| ACT-04 | Quản trị viên | Quản lý tài khoản, cấu trúc học thuật, cấu hình AI, thanh toán và audit |
| EXT-01 | Dịch vụ AI | Tạo nội dung nháp và đề xuất chấm theo yêu cầu; không tự phát hành nội dung hoặc quyết định điểm cuối |
| EXT-02 | Google Drive | Lưu trữ file riêng tư và cung cấp file cho backend hoặc worker sau khi ứng dụng xác minh quyền |
| EXT-03 | Cổng thanh toán | Xử lý giao dịch và gửi webhook để hệ thống xác minh trước khi cấp quyền truy cập |
| EXT-04 | Dịch vụ thông báo | Gửi OTP, email và thông báo thiết yếu do hệ thống yêu cầu |
| EXT-05 | Code Sandbox | Biên dịch và chạy code trong môi trường cô lập với giới hạn tài nguyên và mạng |

Các tác nhân `ACT-*` là người dùng chính của hệ thống. Các tác nhân `EXT-*` là hệ thống bên ngoài tham gia hỗ trợ luồng nghiệp vụ, nhưng không đứng tên một use case độc lập chỉ để mô tả xử lý nội bộ.

## 3. Quy ước

- Mỗi use case thể hiện một mục tiêu nghiệp vụ có giá trị đối với actor, không phải một nút bấm hoặc bước xử lý nội bộ.
- Các thao tác xem danh sách, chi tiết, tìm kiếm và lọc được gộp khi phục vụ cùng một mục tiêu.
- Mỗi tài khoản giữ một role cao nhất: `LEARNER`, `INSTRUCTOR`, `SUBJECT_MANAGER` hoặc `ADMIN`.
- `SUBJECT_MANAGER` kế thừa chức năng của `INSTRUCTOR`, nhưng quyền dữ liệu vẫn phụ thuộc phạm vi môn và lớp được giao.
- AI chỉ tạo bản nháp hoặc đề xuất; con người duyệt nội dung và giảng viên quyết định điểm cuối.
- Redis giữ dữ liệu tạm thời có TTL, RabbitMQ vận chuyển job và Google Drive lưu file riêng tư; các cơ chế này không tạo use case độc lập.
- Mã user story nguồn được ghi cuối mỗi mô tả để duy trì truy vết.

## 4. Danh sách Use Case

### 4.1 Identity and Access Management

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-IAM-01 | Tất cả người dùng | Kích hoạt tài khoản | Authentication | Cho phép người dùng ở lần đăng nhập đầu nhập email trường để yêu cầu kích hoạt, xác minh mã OTP hệ thống gửi qua email và tự thiết lập mật khẩu; có thể yêu cầu lại OTP trong giới hạn tần suất. Phản hồi luôn trung tính để không lộ email nào đã được cấp tài khoản. (`US-IAM-001`) |
| UC-IAM-02 | Tất cả người dùng | Đăng nhập | Authentication | Cho phép người dùng đăng nhập bằng email trường và mật khẩu với kiểm tra trạng thái, giới hạn thử và phản hồi lỗi an toàn. (`US-IAM-002`) |
| UC-IAM-03 | Người dùng đã đăng nhập | Đăng xuất | Authentication | Cho phép người dùng kết thúc phiên hiện tại và thu hồi thông tin xác thực liên quan. (`US-IAM-002`) |
| UC-IAM-04 | Tất cả người dùng | Khôi phục mật khẩu | Authentication | Cho phép người dùng yêu cầu, xác minh OTP và đặt mật khẩu mới mà không làm lộ tài khoản có tồn tại hay không. (`US-IAM-003`) |
| UC-IAM-05 | Người dùng đã đăng nhập | Đổi mật khẩu | Authentication | Cho phép người dùng đổi mật khẩu sau khi xác minh mật khẩu hiện tại và đáp ứng chính sách bảo mật. (`US-IAM-006`) |
| UC-IAM-06 | Người dùng đã đăng nhập | Xem hồ sơ cá nhân | Profile Management | Cho phép người dùng xem thông tin hồ sơ và tài khoản của chính mình. (`US-IAM-004`) |
| UC-IAM-07 | Người dùng đã đăng nhập | Cập nhật hồ sơ cá nhân | Profile Management | Cho phép người dùng sửa các trường hồ sơ được phép nhưng không tự đổi email định danh hoặc role. (`US-IAM-004`) |
| UC-IAM-08 | Quản trị viên | Xem tài khoản người dùng | Account Management | Cho phép quản trị viên xem danh sách và chi tiết tài khoản theo role hoặc trạng thái cần quản lý. (`US-IAM-007`) |
| UC-IAM-09 | Quản trị viên | Tạo tài khoản thủ công | Account Management | Cho phép quản trị viên tạo một tài khoản trường cấp ở trạng thái chờ kích hoạt; không gửi email lúc tạo và quản trị viên không đặt mật khẩu. (`US-IAM-007`) |
| UC-IAM-10 | Quản trị viên | Nhập tài khoản hàng loạt | Account Management | Cho phép quản trị viên nhập nhiều tài khoản từ file ở trạng thái chờ kích hoạt, không gửi email lúc nhập, và nhận kết quả hợp lệ hoặc lỗi theo từng dòng. (`US-IAM-007`) |
| UC-IAM-11 | Quản trị viên | Cập nhật tài khoản và role | Account Management | Cho phép quản trị viên cập nhật thông tin và role cao nhất của tài khoản với kiểm tra chống leo quyền. (`US-IAM-005`, `US-IAM-007`) |
| UC-IAM-12 | Quản trị viên | Quản lý trạng thái tài khoản | Account Management | Cho phép quản trị viên khóa, mở khóa hoặc vô hiệu hóa tài khoản mà không xóa lịch sử nghiệp vụ. (`US-IAM-007`) |

### 4.2 Academic Structure and Enrollment

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-CAT-01 | Quản trị viên | Xem môn học | Subject Management | Cho phép quản trị viên xem danh sách, chi tiết, Chủ nhiệm môn và các lớp của môn học. (`US-CAT-001`) |
| UC-CAT-02 | Quản trị viên | Tạo môn học | Subject Management | Cho phép quản trị viên tạo môn học với mã và thông tin hợp lệ. (`US-CAT-001`) |
| UC-CAT-03 | Quản trị viên | Cập nhật môn học | Subject Management | Cho phép quản trị viên cập nhật thông tin môn học mà không làm mất lịch sử. (`US-CAT-001`) |
| UC-CAT-04 | Quản trị viên | Phân công Chủ nhiệm môn | Subject Management | Cho phép quản trị viên chỉ định tài khoản có role phù hợp quản lý một môn học. (`US-IAM-005`, `US-CAT-001`) |
| UC-CAT-05 | Quản trị viên / Giảng viên / Chủ nhiệm môn | Xem lớp học | Class Management | Cho phép người có quyền xem danh sách và chi tiết lớp trong phạm vi được phân công. (`US-CAT-001`, `US-CAT-002`) |
| UC-CAT-06 | Quản trị viên | Tạo lớp học | Class Management | Cho phép quản trị viên tạo lớp thuộc đúng một môn học. (`US-CAT-001`) |
| UC-CAT-07 | Quản trị viên / Giảng viên | Cập nhật lớp học | Class Management | Cho phép quản trị viên hoặc giảng viên được phân công cập nhật thông tin lớp. (`US-CAT-002`) |
| UC-CAT-08 | Quản trị viên | Phân công giảng viên chính | Class Management | Cho phép quản trị viên chỉ định đúng một giảng viên chính cho một lớp. (`US-CAT-001`) |
| UC-CAT-09 | Quản trị viên / Giảng viên | Quản lý vòng đời lớp | Class Lifecycle | Cho phép người quản lý mở hoặc lưu trữ lớp trong khi giữ nội dung và lịch sử học tập. (`US-CAT-002`) |
| UC-CAT-10 | Quản trị viên / Giảng viên | Xem danh sách học viên | Enrollment | Cho phép người quản lý lớp xem học viên đang hoặc từng được ghi danh. (`US-CAT-003`) |
| UC-CAT-11 | Quản trị viên / Giảng viên | Ghi danh học viên | Enrollment | Cho phép người quản lý thêm học viên vào lớp đúng một lần và gửi thông báo liên quan. (`US-CAT-003`) |
| UC-CAT-12 | Quản trị viên / Giảng viên | Gỡ học viên khỏi lớp | Enrollment | Cho phép người quản lý thu hồi quyền truy cập mới nhưng giữ dữ liệu học tập lịch sử. (`US-CAT-003`) |
| UC-CAT-13 | Người học | Tự ghi danh bằng mã mời (Phase 2) | Enrollment | Cho phép người học dùng mã mời hợp lệ để tự ghi danh vào lớp đang mở. (`US-CAT-005`) |

### 4.3 Learning Content and RAG

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-CNT-01 | Chủ nhiệm môn | Quản lý học liệu và RAG cấp môn | Subject Content | Cho phép Chủ nhiệm môn xem, tải lên, cập nhật, lưu trữ và lập chỉ mục học liệu trong môn được giao; trạng thái xử lý là một phần của luồng này. (`US-CNT-001`) |
| UC-CNT-02 | Giảng viên | Quản lý nội dung lớp | Class Content | Cho phép giảng viên xem, tạo, tải file, cập nhật, sắp xếp và lưu trữ nội dung riêng của lớp được phân công. (`US-CNT-002`) |
| UC-CNT-03 | Giảng viên | Xuất bản nội dung lớp | Class Content | Cho phép giảng viên công bố nội dung hợp lệ cho học viên đã ghi danh. (`US-CNT-002`) |
| UC-CNT-04 | Người học | Truy cập bài học | Learning Content | Cho phép người học đã ghi danh xem nội dung đã phát hành và tải file được phép. (`US-LRN-001`) |
| UC-CNT-05 | Người học / Giảng viên / Chủ nhiệm môn | Tóm tắt học liệu (Phase 2) | AI Content | Cho phép người dùng yêu cầu bản tóm tắt có căn cứ từ nguồn trong phạm vi được phép. (`US-CNT-003`) |
| UC-CNT-06 | Giảng viên | Đăng thông báo lớp (Phase 2) | Class Communication | Cho phép giảng viên đăng thông báo tới đúng lớp được phân công. (`US-CNT-004`) |
| UC-CNT-07 | Người học / Giảng viên | Trao đổi hỏi đáp trong lớp (Phase 2) | Class Communication | Cho phép thành viên đăng câu hỏi và phản hồi trong đúng phạm vi lớp. (`US-CNT-004`) |
| UC-CNT-08 | Giảng viên / Chủ nhiệm môn | Dùng YouTube làm nguồn RAG theo bài giảng | Lesson RAG | Cho phép người có quyền gắn video/playlist, lấy caption có sẵn (không tự phiên âm), theo dõi trạng thái và lập chỉ mục transcript có timestamp trong đúng phạm vi. (`US-CNT-005`) |

### 4.4 Group Management and Group Assignment

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-GRP-01 | Người học / Giảng viên | Xem thông tin nhóm | Group Management | Cho phép người có quyền xem nhóm, trưởng nhóm, thành viên và trạng thái đóng góp trong lớp. (`US-GRP-001`, `US-GRP-003`) |
| UC-GRP-02 | Giảng viên | Quản lý nhóm và trưởng nhóm | Group Management | Cho phép giảng viên tạo hoặc cập nhật nhóm, thêm hoặc gỡ thành viên và chỉ định đúng một trưởng nhóm. (`US-GRP-001`) |
| UC-GRP-03 | Người học | Gửi yêu cầu đổi trưởng nhóm | Leader Management | Cho phép thành viên gửi lý do và đề xuất trưởng nhóm mới để giảng viên xem xét. (`US-GRP-002`) |
| UC-GRP-04 | Giảng viên | Xử lý yêu cầu đổi trưởng nhóm | Leader Management | Cho phép giảng viên phê duyệt hoặc từ chối yêu cầu và thông báo quyết định. (`US-GRP-002`) |
| UC-GRP-05 | Giảng viên | Phân chia phần việc cá nhân | Group Assignment | Cho phép giảng viên tạo phần việc, giao hoặc chuyển phần việc chưa chốt cho thành viên và giữ lịch sử. (`US-GRP-003`) |
| UC-GRP-06 | Người học | Xem và nộp phần việc cá nhân | Group Submission | Cho phép thành viên xem và nộp đúng phần được giao; thành viên khác không thể nộp thay. (`US-GRP-004`) |
| UC-GRP-07 | Giảng viên | Tổng hợp và chốt tài liệu nhóm | Group Submission | Cho phép giảng viên yêu cầu hệ thống ghép phần cá nhân theo cấu trúc, điều chỉnh thứ tự/phần được dùng và chốt version chung có truy vết nguồn. (`US-GRP-005`) |
| UC-GRP-08 | Giảng viên | Đối chiếu và chấm bài chung | Group Grading | Cho phép giảng viên xem tài liệu chung cạnh các phần cá nhân, tự chấm tính tích hợp/nhất quán và quyết định điểm cuối từng sinh viên không theo công thức tự động. (`US-GRP-006`) |

### 4.5 Learning Journey

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-LRN-01 | Người học | Xem dashboard học tập | Learning Dashboard | Cho phép người học xem lớp, assignment sắp đến hạn, thông báo và trạng thái đánh giá đã có. (`US-LRN-001`) |
| UC-LRN-02 | Người học | Truy cập lớp đã ghi danh | Learning Dashboard | Cho phép người học xem các lớp và chi tiết lớp gồm nội dung, assignment và nhóm trong phạm vi quyền. (`US-LRN-001`) |
| UC-LRN-03 | Người học | Lưu và tiếp tục tiến độ học (Ngoài phạm vi) | Learning Progress | Ngoài phạm vi: không lưu vị trí học hoặc trạng thái hoàn thành nội dung. (`US-LRN-002`) |
| UC-LRN-04 | Người học | Xem tiến độ cá nhân (Ngoài phạm vi) | Learning Progress | Ngoài phạm vi: không triển khai tiến độ hoàn thành nội dung từng bài. (`US-LRN-002`) |
| UC-LRN-05 | Giảng viên | Theo dõi tiến độ lớp (Ngoài phạm vi) | Progress Tracking | Ngoài phạm vi: không triển khai báo cáo tiến độ hoàn thành nội dung từng bài. (`US-LRN-003`) |

### 4.6 Question and Rubric Bank

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-QBK-01 | Giảng viên / Chủ nhiệm môn | Quản lý ngân hàng rubric | Rubric Bank | Cho phép người có quyền xem, tạo, cập nhật, nhân bản và quản lý phiên bản rubric trong phạm vi được phép. (`US-QBK-001`) |
| UC-QBK-02 | Giảng viên / Chủ nhiệm môn | Quản lý ngân hàng câu hỏi | Question Bank | Cho phép người có quyền xem, tạo, cập nhật, nhân bản, xem trước và phát hành version mới; attempt đã bắt đầu luôn giữ snapshot cũ. (`US-QBK-002`) |
| UC-QBK-03 | Giảng viên / Chủ nhiệm môn | Phân tích chất lượng câu hỏi (Phase 2) | Question Analytics | Cho phép người có quyền xem chỉ số chất lượng khi dữ liệu đạt ngưỡng phù hợp. (`US-QBK-003`) |

### 4.7 AI-Assisted Authoring and Administration

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-AIG-01 | Giảng viên | Tạo và duyệt bản nháp assignment cấp lớp bằng AI | AI Authoring | Cho phép giảng viên yêu cầu AI tạo bản nháp từ nội dung lớp, xem căn cứ, chỉnh sửa, chấp nhận hoặc loại bỏ kết quả trước khi phát hành. (`US-AIG-001`) |
| UC-AIG-02 | Chủ nhiệm môn | Tạo và duyệt bản nháp assignment chung bằng AI | AI Authoring | Cho phép Chủ nhiệm môn yêu cầu AI tạo bản nháp từ RAG cấp môn, xem căn cứ, chỉnh sửa, chấp nhận hoặc loại bỏ kết quả. (`US-AIG-002`) |
| UC-AIG-03 | Quản trị viên | Quản lý và giám sát AI | AI Administration | Cho phép quản trị viên xem usage, quota, chi phí, cấu hình model và bật hoặc tắt dịch vụ AI. (`US-AIG-003`) |

### 4.8 Assignment Authoring and Submission

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-ASM-01 | Giảng viên / Chủ nhiệm môn | Xem assignment quản lý | Assignment Management | Cho phép người có quyền xem danh sách và chi tiết assignment trong phạm vi được giao. (`US-ASM-001`, `US-ASM-002`) |
| UC-ASM-02 | Giảng viên / Chủ nhiệm môn | Soạn bài viết luận | Assignment Authoring | Cho phép người có quyền tạo bài viết luận với hướng dẫn, giới hạn và rubric. (`US-ASM-007`) |
| UC-ASM-03 | Giảng viên / Chủ nhiệm môn | Soạn bài trắc nghiệm | Assignment Authoring | Cho phép người có quyền tạo bài trắc nghiệm với câu hỏi, đáp án và quy tắc điểm. (`US-ASM-006`) |
| UC-ASM-04 | Giảng viên / Chủ nhiệm môn | Soạn bài thực hành vẽ UML | Assignment Authoring | Cho phép người có quyền tạo bài thực hành vẽ UML trên Draw.io, cấu hình yêu cầu và quy tắc kiểm tra XML. (`US-ASM-004`) |
| UC-ASM-05 | Giảng viên / Chủ nhiệm môn | Soạn và kiểm thử Code Lab | Assignment Authoring | Cho phép người có quyền cấu hình đề code, ngôn ngữ, quota, test case và chạy lời giải mẫu trong sandbox. (`US-ASM-005`) |
| UC-ASM-06 | Giảng viên | Soạn bài tập nhóm | Assignment Authoring | Cho phép giảng viên tạo bài chung, gắn rubric và cấu hình các phần việc cá nhân. (`US-GRP-003`) |
| UC-ASM-07 | Giảng viên | Duyệt và phát hành assignment cho lớp | Assignment Publication | Cho phép giảng viên cấu hình lịch, lượt nộp, xem trước, duyệt và phát hành assignment cho lớp được phân công. (`US-ASM-001`) |
| UC-ASM-08 | Chủ nhiệm môn | Duyệt và phát hành assignment chung | Assignment Publication | Cho phép Chủ nhiệm môn xem trước, duyệt và phát hành đề chung tới mọi lớp hiện hành thuộc môn được giao. (`US-ASM-002`) |
| UC-ASM-09 | Người học | Xem assignment được giao | Assignment Delivery | Cho phép người học xem danh sách, yêu cầu, rubric, thời hạn, số lượt và trạng thái assignment. (`US-ASM-003`) |
| UC-ASM-10 | Người học | Làm và nộp bài viết luận | Assignment Workspace | Cho phép người học soạn, lưu nháp và nộp câu trả lời mở đang hiệu lực. (`US-ASM-003`, `US-ASM-007`) |
| UC-ASM-11 | Người học | Làm và nộp bài trắc nghiệm | Assignment Workspace | Cho phép người học trả lời, lưu nháp và nộp bài; câu hỏi xác định được tự chấm theo phiên bản đáp án. (`US-ASM-003`, `US-ASM-006`, `US-GRD-001`) |
| UC-ASM-12 | Người học | Làm và nộp bài thực hành vẽ UML | Diagram Assignment | Cho phép người học vẽ UML trên canvas Draw.io, lưu nháp và nộp XML Draw.io đầy đủ làm bản chuẩn. (`US-ASM-003`, `US-ASM-004`) |
| UC-ASM-13 | Người học | Làm và nộp bài Code Lab | Code Assignment | Cho phép người học viết, chạy thử trong sandbox, lưu nháp và nộp mã nguồn theo giới hạn đề. (`US-ASM-003`, `US-ASM-005`) |
| UC-ASM-14 | Người học | Xem lịch sử và nộp lại assignment | Submission | Cho phép người học xem các attempt của mình và tạo attempt mới khi còn thời gian và lượt nộp. (`US-ASM-003`) |
| UC-ASM-15 | Giảng viên / Chủ nhiệm môn | Quản lý vòng đời assignment (Phase 2) | Assignment Lifecycle | Cho phép người có quyền nhân bản, tạo phiên bản mới hoặc ngừng giao assignment mà không sửa dữ liệu lịch sử. (`US-ASM-008`) |
| UC-ASM-16 | Chủ nhiệm môn / Giảng viên | Phát hành và copy template đề cấp môn | Assignment Template | Cho phép Chủ nhiệm môn phát hành template có version và giảng viên copy thành draft độc lập cho lớp được phân công. (`US-ASM-009`) |
| UC-ASM-17 | Giảng viên | Copy assignment và rubric giữa lớp | Assignment Reuse | Cho phép giảng viên copy nội dung giữa hai lớp mình phụ trách mà không mang theo lịch, attempt, bài nộp hoặc điểm. (`US-ASM-010`) |
| UC-ASM-18 | Người học / Giảng viên | Cấu hình và làm simulation exam | Simulation Exam | Cho phép giảng viên cấu hình lượt, cửa sổ, cách lấy kết quả, thời điểm hiện đáp án và trạng thái tính điểm; người học làm trong giới hạn với snapshot theo attempt. (`US-ASM-011`) |

### 4.9 Grading and Feedback

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-GRD-01 | Giảng viên | Xem và review bài nộp | Submission Review | Cho phép giảng viên xem danh sách, bài làm, rubric, attempt và kết quả tự chấm của lớp được phân công. (`US-GRD-002`, `US-GRD-003`) |
| UC-GRD-02 | Giảng viên | Chấm bài thủ công | Manual Grading | Cho phép giảng viên nhập điểm và phản hồi cho bài cá nhân mà không gọi AI. (`US-GRD-002`, `US-GRD-003`) |
| UC-GRD-03 | Giảng viên | Chấm bài với AI hỗ trợ | AI-Assisted Grading | Cho phép giảng viên chủ động yêu cầu, xem, chấp nhận hoặc ghi đè đề xuất AI; AI không quyết định điểm cuối. (`US-GRD-002`, `US-GRD-003`) |
| UC-GRD-04 | Giảng viên | Chốt và công bố điểm | Grade Finalization | Cho phép giảng viên xác nhận điểm cuối, công bố điểm và phản hồi cho đúng người học. (`US-GRD-003`) |
| UC-GRD-05 | Giảng viên | Chốt điểm hàng loạt | Grade Finalization | Cho phép giảng viên kiểm tra điều kiện và chốt nhiều điểm hợp lệ trong lớp. (`US-GRD-005`) |
| UC-GRD-06 | Người học | Xem điểm và phản hồi cá nhân | Gradebook | Cho phép người học xem điểm cuối đã công bố và phản hồi của chính mình. (`US-GRD-004`) |
| UC-GRD-07 | Giảng viên / Quản trị viên | Xem sổ điểm và lịch sử điểm | Gradebook | Cho phép người có quyền xem sổ điểm cùng lịch sử thay đổi, actor, thời gian và lý do. (`US-GRD-003`, `US-GRD-004`) |
| UC-GRD-08 | Người học | Yêu cầu gia hạn nộp bài (Phase 2) | Submission Exception | Cho phép người học gửi yêu cầu gia hạn với lý do. (`US-GRD-006`) |
| UC-GRD-09 | Giảng viên | Xử lý yêu cầu gia hạn (Phase 2) | Submission Exception | Cho phép giảng viên phê duyệt hoặc từ chối hạn riêng của người học. (`US-GRD-006`) |
| UC-GRD-10 | Người học | Khiếu nại điểm (Phase 2) | Grade Appeal | Cho phép người học gửi khiếu nại trong thời hạn cho phép. (`US-GRD-007`) |
| UC-GRD-11 | Giảng viên | Xử lý phúc khảo điểm (Phase 2) | Grade Appeal | Cho phép giảng viên xem xét, quyết định và lưu lịch sử phúc khảo. (`US-GRD-007`) |
| UC-GRD-12 | Giảng viên | Kiểm tra tương đồng bài nộp (Phase 2) | Similarity Review | Cho phép giảng viên yêu cầu và xem báo cáo tương đồng mang tính tham khảo. (`US-GRD-008`) |

### 4.10 Reporting and Analytics

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-RPT-01 | Giảng viên | Theo dõi tình trạng nộp bài | Submission Monitoring | Cho phép giảng viên xem tình trạng nộp trong lớp và gửi nhắc có giới hạn tới học viên cần xử lý. (`US-RPT-001`) |
| UC-RPT-02 | Người học | Xem dashboard kết quả cá nhân (Phase 2) | Learning Analytics | Cho phép người học xem xu hướng kết quả cá nhân và phân bố lớp đã ẩn danh. (`US-RPT-002`) |
| UC-RPT-03 | Giảng viên / Quản trị viên | Xuất bảng điểm (Phase 2) | Grade Export | Cho phép người có quyền tạo và tải file bảng điểm đúng phạm vi. (`US-RPT-003`) |
| UC-RPT-04 | Quản trị viên | Đối sánh điểm AI và điểm chốt (Phase 2) | AI Analytics | Cho phép quản trị viên xem báo cáo sai lệch khi cỡ mẫu đáp ứng quy tắc riêng tư. (`US-RPT-004`) |

### 4.11 Payment and Access

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-PAY-01 | Người học | Thanh toán và nhận quyền truy cập | Payment | Cho phép người học xem gói, bắt đầu thanh toán, theo dõi trạng thái và nhận access grant sau khi giao dịch được xác minh. (`US-PAY-001`, `US-PAY-002`) |
| UC-PAY-02 | Quản trị viên | Đối soát thanh toán | Payment Operations | Cho phép quản trị viên đối chiếu payment, webhook và access grant, rồi xử lý chênh lệch có lý do. (`US-PAY-003`) |

### 4.12 Notification and Audit

| ID | Actor | Use Case | Feature | Use Case Description |
|---|---|---|---|---|
| UC-OPS-01 | Người dùng | Nhận và xem thông báo | Notification | Cho phép người dùng nhận và xem thông báo assignment, hạn nộp, nhóm, điểm và thanh toán của mình. (`US-NTF-001`) |
| UC-OPS-02 | Quản trị viên | Xem nhật ký audit | Audit | Cho phép quản trị viên được phép xem sự kiện theo actor, hành động, đối tượng, kết quả và thời gian; audit không thể sửa hoặc xóa. (`US-AUD-001`) |

## 5. Quy tắc nghiệp vụ cốt lõi

1. Mỗi tài khoản có một role cao nhất; `SUBJECT_MANAGER` kế thừa chức năng giảng viên nhưng quyền dữ liệu vẫn phụ thuộc phân công môn và lớp.
2. Mỗi môn có một Chủ nhiệm môn và mỗi lớp có đúng một giảng viên chính; một Chủ nhiệm môn có thể được phân công làm giảng viên chính.
3. Mỗi nhóm có đúng một trưởng nhóm; từng thành viên chỉ nộp phần được giao, còn hệ thống tổng hợp và giảng viên chốt tài liệu chung.
4. Phần cá nhân có thể được AI đề xuất điểm; tài liệu chung chỉ do giảng viên chấm thủ công và không có công thức tự động quyết định điểm cuối.
5. Giảng viên luôn quyết định điểm cuối; AI không tự chốt hoặc công bố điểm.
6. XML Draw.io đầy đủ là bản nộp chuẩn; XML rút gọn chỉ là dữ liệu dẫn xuất tạm thời khi giảng viên yêu cầu AI chấm.
7. Nội dung, assignment, question/rubric version, bài nộp, điểm, payment và audit đã phát sinh không bị xóa hồi tố; attempt giữ snapshot đã bắt đầu.
8. Payment webhook phải được xác minh chữ ký, số tiền, tiền tệ, transaction ID, chống replay và xử lý idempotent trước khi cấp access grant.

## 6. Bảo mật và khả năng phục hồi

- Validate và giới hạn mọi input, upload, URL YouTube, caption/transcript, XML, tài liệu tổng hợp và dữ liệu nhận từ dịch vụ ngoài.
- File phải được quét trước khi được dùng cho RAG, AI, nội dung hoặc bài nộp.
- XML parser phải tắt external entities và áp dụng schema hoặc allowlist Draw.io.
- Code Lab chạy trong sandbox có quota CPU, bộ nhớ, thời gian và network policy.
- Dữ liệu gửi AI phải tối thiểu và đúng phạm vi; không gửi secret hoặc dữ liệu không cần thiết.
- AI, Google Drive, payment, notification và Code Sandbox phải có timeout, retry hữu hạn, backoff và idempotency phù hợp.
- Lỗi tích hợp không được làm mất nháp, bài nộp, điểm đã chốt hoặc giao dịch đã xác nhận.
- Log và audit không chứa mật khẩu, OTP, token, secret hoặc dữ liệu bài làm dư thừa.

## 7. Kiểm tra độ đầy đủ

- Toàn bộ 59/59 user story có ít nhất một use case truy vết trực tiếp trong mô tả.
- Danh mục chỉ sử dụng bốn actor nghiệp vụ; hệ thống và dịch vụ ngoài không đứng tên use case riêng.
- Tìm kiếm, lọc, xử lý nền, retry và tự chấm được giữ như hành vi bên trong use case liên quan.
- Các bước tạo, xem, sửa, chấp nhận và loại bỏ bản nháp AI được gộp theo phạm vi cấp lớp hoặc cấp môn.
- Luồng nhóm phân biệt quản lý nhóm, đổi trưởng nhóm, phần cá nhân, tổng hợp tài liệu và chấm bài chung.
- Luồng chấm phân biệt chấm tay, AI hỗ trợ, chốt điểm, công bố điểm và các ngoại lệ Phase 2.
- Các chức năng Phase 2 được đánh dấu rõ và không bị trộn vào phạm vi MVP.
