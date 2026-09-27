# Kế hoạch tạo User Stories

> Lịch sử lập kế hoạch: các mục nhắc "Phase 2" bên dưới phản ánh quyết định cũ. Hai catalog hiện hành chỉ gồm 49 story/77 use case MVP; story/UC ngoài phạm vi đã bị xóa. Xem `stories.md` và `unit-of-work-story-map.md` để triển khai.

> Bổ sung 2026-09-13: Kế hoạch cũ yêu cầu đặc tả use case đầy đủ. Danh mục hiện hành tại `aidlc-docs/inception/user-stories/use-cases.md` chỉ giữ 77 UC MVP và truy vết 49/49 story đang triển khai; phần 59/59 là mốc lịch sử trước khi rút phạm vi.

## 1. Mục tiêu và phạm vi

Chuyển `aidlc-docs/inception/requirements/requirements.md` thành bộ user stories lấy người dùng làm trung tâm, có thể kiểm thử và truy vết. Kế hoạch bao phủ người học, giảng viên, Chủ nhiệm môn và quản trị viên; các actor hệ thống bên ngoài chỉ xuất hiện khi cần mô tả tương tác hỗ trợ hành trình.

## 2. Các cách phân rã có thể dùng

### A. Kết hợp hành trình và miền nghiệp vụ - khuyến nghị

Nhóm stories theo các miền lớn, sau đó sắp xếp theo hành trình end-to-end bên trong từng miền. Cách này giữ được ngữ cảnh người dùng đồng thời tạo ranh giới hữu ích cho thiết kế và units về sau.

### B. Thuần hành trình người dùng

Tổ chức stories theo luồng từ đầu đến cuối của từng persona. Dễ đọc với stakeholder nhưng các năng lực dùng chung như xác thực, audit và tích hợp có thể bị lặp hoặc phân tán.

### C. Thuần tính năng/miền nghiệp vụ

Nhóm stories theo tài khoản, khóa học, đánh giá, AI, thanh toán và thông báo. Dễ ánh xạ sang component nhưng có thể làm mờ trải nghiệm xuyên suốt của người dùng.

### D. Theo persona

Tập hợp toàn bộ stories của từng vai trò. Cách này làm rõ trách nhiệm và quyền, nhưng các hành trình phối hợp giảng viên-người học dễ bị tách rời.

## 3. Câu hỏi quyết định

Vui lòng điền một chữ cái sau mỗi thẻ `[Answer]:`. Nếu chọn phương án Khác, ghi mô tả ngay sau chữ cái.

### Question 1
Bạn muốn dùng cách phân rã stories nào?

A) Kết hợp hành trình và miền nghiệp vụ như khuyến nghị

B) Thuần hành trình người dùng

C) Thuần tính năng/miền nghiệp vụ

D) Theo persona

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: A

### Question 2
Bộ persona nên có phạm vi nào?

A) Ba persona chính: người học, giảng viên và quản trị viên

B) Ba persona chính và một stakeholder profile cho người vận hành đơn vị đào tạo

C) Ba persona chính cùng các persona kỹ thuật riêng cho nhóm phát triển/vận hành

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: X - Bổ sung persona Chủ nhiệm môn (Subject Manager), là vai trò RBAC thứ tư với quyền cấp môn theo các câu trả lời làm rõ.

### Question 3
Mức độ chi tiết của stories nên được chia như thế nào?

A) Lát cắt nhỏ theo giá trị người dùng, mỗi story dự kiến triển khai và kiểm thử độc lập

B) Stories cỡ vừa bao trọn một bước lớn trong hành trình, chấp nhận cần chia nhỏ khi lập kế hoạch code

C) Epics lớn ở stage này, việc phân rã chi tiết để dành cho Units Generation

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: A

### Question 4
Bạn muốn tiêu chí chấp nhận dùng định dạng nào?

A) Given/When/Then cho từng scenario

B) Danh sách điều kiện kiểm thử được bằng ngôn ngữ nghiệp vụ

C) Kết hợp: Given/When/Then cho luồng/rule phức tạp, checklist cho điều kiện đơn giản

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: A

### Question 5
Stories về lỗi tích hợp AI, thanh toán, email và lưu trữ nên được thể hiện ra sao?

A) Tách thành stories riêng cho trạng thái lỗi, retry và degraded mode

B) Gộp thành acceptance criteria trong story nghiệp vụ liên quan

C) Kết hợp: tách story khi lỗi tạo ra hành trình phục hồi riêng, còn lỗi cục bộ đặt trong acceptance criteria

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: B

### Question 6
Mức truy vết từ stories về requirements nên là gì?

A) Mỗi story ghi rõ các mã FR/NFR/SEC/REL liên quan

B) Chỉ ghi mã FR chính trên từng story và dùng ma trận riêng cho NFR/SEC/REL

C) Dùng một ma trận truy vết chung ở cuối `stories.md`, không ghi mã trong từng story

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]: A

### Question 7
Stories kỹ thuật không gắn trực tiếp với một persona, như audit, observability, backup và supply-chain security, nên được xử lý thế nào?

A) Viết dưới góc nhìn quản trị viên hoặc đơn vị đào tạo khi có giá trị quan sát được; phần còn lại giữ làm ràng buộc thiết kế/test, không ép thành user story

B) Tạo system stories riêng với actor là hệ thống hoặc nhóm vận hành

C) Không tạo stories kỹ thuật; chỉ ánh xạ các ràng buộc này trong ma trận truy vết

X) Khác (vui lòng mô tả sau thẻ `[Answer]:` bên dưới)

[Answer]:C

## 3.1 Kết quả phân tích câu trả lời

- **Cách phân rã**: Kết hợp hành trình và miền nghiệp vụ.
- **Persona**: Người học, giảng viên, Chủ nhiệm môn và quản trị viên.
- **Quyền Chủ nhiệm môn**: Vai trò RBAC riêng; quản lý học liệu/RAG của các môn được phân công; phát hành trực tiếp đề chung cho mọi lớp thuộc môn mà không cần giảng viên lớp duyệt lại.
- **Ranh giới giảng viên**: Giảng viên tiếp tục quản lý nội dung riêng và hoạt động của các lớp được phân công, nhưng không được sửa kho học liệu/RAG cấp môn nếu không có quyền tương ứng.
- **Granularity**: Lát cắt nhỏ, độc lập theo giá trị người dùng.
- **Acceptance criteria**: Given/When/Then cho từng scenario.
- **Lỗi tích hợp**: Được thể hiện trong acceptance criteria của story nghiệp vụ liên quan.
- **Truy vết**: Mỗi story ghi rõ các mã FR/NFR/SEC/REL liên quan.
- **Ràng buộc kỹ thuật**: Không tạo system story riêng; được quản lý qua truy vết và các stage thiết kế/test.
- **Kết quả rà soát**: Không còn câu trả lời thiếu, mơ hồ hoặc mâu thuẫn.

## 4. Checklist thực thi sau khi kế hoạch được duyệt

- [x] Đọc lại toàn bộ kế hoạch đã được trả lời và xác định bước chưa hoàn thành đầu tiên.
- [x] Chốt taxonomy gồm epic/miền, hành trình và quy ước mã story theo cách phân rã được chọn.
- [x] Tạo `aidlc-docs/inception/user-stories/personas.md` với mục tiêu, động lực, khó khăn, quyền và bối cảnh sử dụng của người học, giảng viên, Chủ nhiệm môn và quản trị viên.
- [x] Tạo danh mục stories bao phủ FR-001 đến FR-014 và các hành vi quan sát được từ NFR/SEC/REL.
- [x] Viết từng story theo mẫu “Là ..., tôi muốn ..., để ...” và tiêu chí chấp nhận theo định dạng được chọn.
- [x] Bao phủ happy path, authorization boundary, validation, failure/degraded mode và audit cho các hành trình có rủi ro.
- [x] Kiểm tra từng story theo INVEST: Independent, Negotiable, Valuable, Estimable, Small và Testable.
- [x] Ánh xạ persona với stories liên quan trong `personas.md`.
- [x] Hoàn thiện `aidlc-docs/inception/user-stories/stories.md` cùng truy vết requirements theo lựa chọn đã duyệt.
- [x] Kiểm tra Security Baseline và Resiliency Baseline; sửa mọi finding áp dụng được trước khi trình duyệt.
- [x] Xác minh không còn placeholder, mâu thuẫn, story không có acceptance criteria hoặc yêu cầu nguồn chưa được bao phủ.
- [x] Đánh dấu từng checkbox `[x]` ngay trong cùng lượt thực hiện khi bước tương ứng hoàn tất.

## 5. Artifact bắt buộc

- [x] `aidlc-docs/inception/user-stories/stories.md` chứa user stories thỏa INVEST và có acceptance criteria.
- [x] `aidlc-docs/inception/user-stories/personas.md` chứa các persona và ánh xạ persona-story.

## 6. Giới hạn của stage

- Không chọn kiến trúc, database, cloud provider hoặc nhà cung cấp tích hợp cụ thể.
- Không lập sprint, timeline hoặc giao việc cho nhóm phát triển.
- Không tạo code hay cấu trúc ứng dụng.
- Các quyết định kỹ thuật chi tiết được chuyển sang Workflow Planning, Application Design và các stage Construction phù hợp.

## 7. Tuân thủ extension dự kiến

- **Security Baseline**: Stories và acceptance criteria phải phản ánh quyền truy cập, input boundary, audit, thanh toán và fail-safe behavior khi có tác động nghiệp vụ; control thuần thiết kế được truy vết downstream.
- **Resiliency Baseline**: Stories phải phản ánh timeout/failure/degraded mode có ảnh hưởng đến hành trình; topology, backup và recovery chi tiết được giữ cho các stage thiết kế/hạ tầng.
- **Property-Based Testing**: N/A vì extension đã bị tắt trong Requirements Analysis.

## 8. Revision 2026-09-13 - Đối chiếu UC1

### Phạm vi đã được phê duyệt

- Đối chiếu bộ stories với `uc1.pdf` và bổ sung các hành trình phù hợp theo phương án được người dùng chấp thuận.
- Không tạo persona hoặc quyền cho Head of Department/Trưởng bộ môn.
- Giữ Chủ nhiệm môn/Subject Manager là role RBAC cấp môn.
- Chuẩn hóa bốn loại bài: sơ đồ Draw.io, trắc nghiệm, Code Lab và bài viết luận; người học vẽ trên canvas web và nộp XML Draw.io rút gọn.
- Người học sử dụng web desktop-first; mobile phục vụ chủ yếu cho đọc nội dung, thông báo và kết quả.
- Tài khoản người học/giảng viên được cấp theo email trường, không có self-registration công khai.
- Sau khi nhận bài, giảng viên chủ động chọn chấm thủ công hoặc yêu cầu AI đề xuất; AI không tự động quyết định phương thức hay điểm cuối.
- Loại bỏ story phân công chấm chéo khỏi Phase 2.
- Mở rộng acceptance criteria nếu mục tiêu đã có; chỉ tạo story mới cho giá trị người dùng độc lập.
- Story sau MVP được ghi rõ hậu tố `(Phase 2)`.
- Giữ payment và learning progress vì người dùng chưa yêu cầu loại khỏi phạm vi hiện tại.
- Giữ tích hợp AI ở mức provider-neutral; không khóa vào Gemini.

### Checklist revision

- [x] Cập nhật requirements nguồn và phạm vi từ FR-015 đến FR-024.
- [x] Bổ sung vòng đời tài khoản, ngân hàng rubric/câu hỏi và quản trị AI.
- [x] Bổ sung bốn loại bài cùng preview/test và chuẩn hóa bài viết luận.
- [x] Bổ sung autosave/lịch sử attempt, theo dõi nộp, nhắc nhở và chốt điểm hàng loạt.
- [x] Bổ sung backlog Phase 2 về join code, cộng tác, ngoại lệ đánh giá và báo cáo nâng cao.
- [x] Cập nhật persona-story mapping và xác nhận không có Head of Department/Trưởng bộ môn.
- [x] Chuẩn hóa bài sơ đồ thành canvas Draw.io; lưu XML đầy đủ và chỉ tạo XML rút gọn dẫn xuất khi gửi AI.
- [x] Làm rõ cấp tài khoản email trường, desktop-first và quyền lựa chọn phương thức chấm của giảng viên.
- [x] Loại bỏ US-GRD-009 và mọi truy vết liên quan.
- [x] Bổ sung FR-025/FR-026 và sáu story cho nhóm, leader, phần cá nhân, tài liệu chung và chấm hai cấp; cơ chế DOCX do leader nộp đã được change request 2026-09-22 thay thế.
- [x] Giữ quyết định đổi leader cho giảng viên; từng thành viên nộp phần cá nhân, hệ thống tổng hợp và giảng viên chốt tài liệu chung.
- [x] Giới hạn AI ở phần cá nhân; bài chung chỉ có luồng giảng viên chấm thủ công và đối chiếu.
- [x] Loại bỏ US-CAT-004 cùng phạm vi học kỳ/nhân bản lớp và thu hẹp FR-022 còn join code.
- [x] Hoàn tất kiểm tra traceability, INVEST, Security và Resiliency.
- [x] Trình người dùng checkpoint phê duyệt lại User Stories.

## 9. Revision 2026-09-22 - Versioning, simulation, reuse, YouTube RAG và chấm nhóm

### Quyết định đã được phê duyệt ở Requirements

- Attempt đã bắt đầu giữ snapshot câu hỏi; chỉnh sửa khi bài còn mở tạo version mới cho attempt bắt đầu sau đó.
- Chủ nhiệm môn phát hành template có version; giảng viên copy thành draft độc lập cho lớp.
- Giảng viên copy assignment/rubric chỉ giữa các lớp mình phụ trách và không mang theo dữ liệu thực thi.
- Simulation exam giới hạn lượt, có thể tính hoặc không tính điểm thành phần, nhưng không phải kỳ thi chính thức có giám sát.
- Video/playlist YouTube gắn theo bài giảng; chỉ dùng caption có sẵn trước khi lập chỉ mục RAG, không tự phiên âm (quyết định hiện hành thay thế mô tả cũ).
- Hệ thống ghép phần cá nhân; giảng viên chốt tài liệu chung, tự chấm tính tích hợp và tự quyết định điểm cuối từng sinh viên.

### Checklist revision

- [x] Thêm `US-CNT-005` cho YouTube RAG theo bài giảng.
- [x] Thay `US-GRP-005` bằng luồng tổng hợp/chốt tài liệu và mở rộng `US-GRP-006` cho chấm nhất quán/điểm cuối.
- [x] Mở rộng `US-QBK-002` với snapshot/version theo attempt.
- [x] Thêm `US-ASM-009` đến `US-ASM-011` cho template, copy giữa lớp và simulation exam.
- [x] Cập nhật personas và use cases; danh mục hiện hành truy vết đủ 49/49 story MVP tới 77 UC. Story ngoài phạm vi đã xóa khỏi catalog, quyết định lịch sử còn trong audit.
- [x] Cập nhật traceability FR-004, FR-007, FR-016 và FR-027 đến FR-029.
- [x] Kiểm tra Security/Resiliency và tính nhất quán với requirements đã duyệt.
- [x] Trình người dùng checkpoint phê duyệt lại User Stories.
