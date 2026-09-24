# Yêu cầu sản phẩm: AI-Powered Learning Platform

## 1. Tóm tắt phân tích ý định

- **Yêu cầu ban đầu**: "giúp tôi triển khai quy trình ai dlc"
- **Loại yêu cầu**: Dự án mới
- **Độ rõ ban đầu**: Mơ hồ; đã được làm rõ qua ba vòng câu hỏi
- **Phạm vi ước tính**: Toàn hệ thống, gồm web frontend, backend, dữ liệu, tích hợp AI, lưu trữ tệp, thanh toán, email và tài liệu quy trình AI-DLC
- **Độ phức tạp**: Phức tạp
- **Mức độ chi tiết**: Comprehensive
- **Mục tiêu**: Xây dựng MVP nền tảng học tập ứng dụng AI đồng thời duy trì bộ tài liệu và checkpoint AI-DLC có thể tái sử dụng

## 2. Bối cảnh và phạm vi

### 2.1 Người dùng và mô hình vận hành

Nền tảng phục vụ một trường học hoặc trung tâm đào tạo. Bốn vai trò chính là người học, giảng viên, Chủ nhiệm môn (Subject Manager) và quản trị viên. Một môn học có thể có nhiều lớp; Chủ nhiệm môn chịu trách nhiệm học thuật và tài nguyên dùng chung của các môn được phân công, còn giảng viên phụ trách hoạt động và nội dung riêng của các lớp được giao. Phiên bản đầu vận hành trong phạm vi một tổ chức; multi-tenancy không thuộc MVP.

### 2.2 Phạm vi MVP

MVP bao gồm tài khoản và vòng đời tài khoản quản trị, phân quyền, quản lý môn học/khóa học/lớp học, nhóm học tập, bài nhóm gồm các phần cá nhân và một tài liệu tổng hợp, kho học liệu và RAG cấp môn/bài giảng từ tài liệu hoặc YouTube, nội dung riêng của lớp, tải tài liệu, ngân hàng rubric/câu hỏi có versioning, template đề cấp môn, sao chép assignment/rubric giữa các lớp của cùng giảng viên, thi thử giới hạn số lượt, theo dõi trạng thái bài nộp và kết quả đánh giá, đánh giá theo bốn loại sơ đồ Draw.io, trắc nghiệm, Code Lab và bài viết luận, tạo câu hỏi/bài tập bằng AI, phản hồi hoặc chấm điểm có hỗ trợ AI, giám sát sử dụng AI, thanh toán và email/thông báo. MVP không lưu tiến độ hoàn thành hoặc vị trí học của từng bài. Sản phẩm là web desktop-first cho người học; giao diện mobile chỉ cần đáp ứng các thao tác đọc/cơ bản, không tối ưu canvas vẽ sơ đồ hoặc trải nghiệm làm bài phức tạp.

### 2.3 Ngoài phạm vi MVP

- Ứng dụng mobile native
- Multi-tenancy và cô lập dữ liệu giữa nhiều tổ chức
- Đồng bộ LMS hoặc SSO của tổ chức
- Chức năng dành riêng cho vai trò Head of Department/Trưởng bộ môn
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
| Chủ nhiệm môn | Quản lý kho học liệu/RAG cấp môn và biên soạn, phát hành đề chung cho mọi lớp thuộc môn được phân công |
| Quản trị viên | Quản lý người dùng, vai trò, cấu hình nền tảng, thanh toán và audit |
| Đơn vị đào tạo | Vận hành thử nghiệm ổn định, bảo vệ dữ liệu người học và đo hiệu quả MVP |
| Nhóm phát triển | Quy trình AI-DLC rõ ràng, test tự động, container local và hướng dẫn triển khai |

## 4. Yêu cầu chức năng

### FR-001 - Xác thực và tài khoản trường cấp

Hệ thống phải dùng email do trường cấp làm định danh đăng nhập cho người học và giảng viên. Tài khoản được quản trị viên cấp/import hoặc đồng bộ từ nguồn danh tính của trường; không có đăng ký công khai. Hệ thống phải hỗ trợ kích hoạt lần đầu khi cần, đăng nhập, đăng xuất, khôi phục mật khẩu và quản lý hồ sơ tối thiểu.

**Tiêu chí chấp nhận:**

- Người dùng đã xác thực có thể đăng nhập và đăng xuất an toàn.
- Phiên hết hạn theo cấu hình và bị vô hiệu hóa khi đăng xuất.
- Luồng khôi phục mật khẩu không tiết lộ tài khoản có tồn tại hay không.
- Email đăng nhập của người học/giảng viên phải thuộc miền email trường được cấu hình; người dùng không thể tự đăng ký bằng email ngoài miền.

### FR-002 - Phân quyền

Hệ thống phải cung cấp bốn vai trò: người học, giảng viên, Chủ nhiệm môn và quản trị viên; mọi thao tác đặc quyền phải được kiểm tra phía server.

**Tiêu chí chấp nhận:**

- Người học không thể gọi chức năng của giảng viên, Chủ nhiệm môn hoặc quản trị viên.
- Giảng viên chỉ truy cập lớp học, khóa học và bài nộp được phân công.
- Chủ nhiệm môn chỉ có quyền cấp môn đối với các môn được phân công, kể cả các lớp do giảng viên khác phụ trách trong những môn đó.
- Quản trị viên có thể quản lý người dùng, phân vai trò và gán phạm vi môn học cho Chủ nhiệm môn.

### FR-003 - Quản lý môn học, khóa học và lớp học

Quản trị viên phải có thể quản lý môn học, gán Chủ nhiệm môn và tổ chức các lớp thuộc môn. Giảng viên hoặc quản trị viên phải có thể tạo, sửa, xuất bản và lưu trữ khóa học/lớp theo phạm vi quyền; quản lý phân công giảng viên và ghi danh người học.

**Tiêu chí chấp nhận:**

- Nội dung chưa xuất bản không hiển thị cho người học.
- Người học chỉ truy cập lớp mình được ghi danh.
- Mỗi lớp thuộc một môn và kế thừa học liệu hoặc đề chung đã được phát hành ở cấp môn.
- Các thay đổi quan trọng về khóa học được ghi audit.

### FR-004 - Nhập và quản lý nội dung học

Hệ thống phải hỗ trợ soạn nội dung trực tiếp, tải lên PDF, DOCX hoặc slide và gắn nguồn YouTube cho từng bài giảng. Chủ nhiệm môn quản lý kho học liệu và nguồn trích xuất RAG dùng chung ở cấp môn; giảng viên vẫn quản lý nội dung riêng của lớp được phân công. Việc xử lý tệp, caption và transcript phải có trạng thái, giới hạn hợp lệ và thông báo lỗi an toàn.

**Tiêu chí chấp nhận:**

- Tệp không hợp lệ bị từ chối trước khi xử lý.
- Tệp hợp lệ được lưu riêng tư và gắn đúng phạm vi môn hoặc lớp.
- Người tải lên và người quản lý được ủy quyền xem được trạng thái chờ, đang xử lý, thành công hoặc thất bại.
- Giảng viên không thể sửa kho học liệu/RAG cấp môn nếu không có quyền Chủ nhiệm môn tương ứng.
- Mỗi bài giảng có thể gắn một video hoặc playlist YouTube; hệ thống ưu tiên caption có sẵn và tự phiên âm audio khi caption không khả dụng.
- Transcript được lưu cùng video, bài giảng, ngôn ngữ và timestamp; chỉ transcript xử lý thành công mới được lập chỉ mục vào đúng phạm vi RAG.
- Giảng viên hoặc Chủ nhiệm môn có quyền xem trạng thái xử lý và retry khi lấy caption, phiên âm hoặc lập chỉ mục thất bại.

### FR-005 - Truy cập nội dung theo lớp

Người học phải có thể xem nội dung đã xuất bản trong lớp được ghi danh và được cấp quyền. Hệ thống không lưu trạng thái hoàn thành hoặc vị trí học của từng bài.

**Tiêu chí chấp nhận:**

- Người học chỉ nhận nội dung đã xuất bản trong lớp mình được ghi danh và có entitlement hợp lệ khi nội dung yêu cầu thanh toán.
- Truy cập trực tiếp bằng URL/ID không vượt qua kiểm tra enrollment, entitlement hoặc publication.

### FR-006 - Tạo câu hỏi và bài tập bằng AI

Giảng viên phải có thể yêu cầu AI tạo câu hỏi hoặc bài tập từ nội dung được phép của lớp; Chủ nhiệm môn phải có thể thực hiện tương tự từ kho học liệu/RAG của môn, với loại câu hỏi, số lượng và mức độ khó có giới hạn hợp lệ.

**Tiêu chí chấp nhận:**

- AI chỉ sử dụng nội dung thuộc phạm vi lớp hoặc môn mà người yêu cầu được phép truy cập.
- Nội dung do AI tạo ở trạng thái bản nháp và cần chính vai trò có quyền xuất bản duyệt trước khi giao cho người học.
- Hệ thống lưu nguồn nội dung, cấu hình tạo và trạng thái duyệt để truy vết.

### FR-007 - Đánh giá và bài nộp

Giảng viên phải có thể xuất bản bài đánh giá riêng cho lớp được phân công. Chủ nhiệm môn phải có thể biên soạn và phát hành trực tiếp bài đánh giá chung cho mọi lớp thuộc môn được phân công mà không cần giảng viên từng lớp duyệt lại. Người học được phép phải có thể làm và nộp bài trong thời gian hiệu lực.

**Tiêu chí chấp nhận:**

- Hệ thống lưu bài nộp, thời điểm nộp và trạng thái chấm.
- Một người học không thể đọc hoặc sửa bài nộp của người khác.
- Quy tắc số lần làm và hạn nộp được thực thi phía server.
- Bài đánh giá chung chỉ được phân phối tới các lớp thuộc đúng môn và lưu actor/phạm vi phát hành trong audit.

### FR-008 - Chấm điểm và phản hồi tự động

Sau khi người học nộp bài, bài nộp chuyển tới giảng viên phụ trách. Giảng viên quyết định chấm thủ công hoặc yêu cầu AI đề xuất điểm/phản hồi; AI không tự động chấm nếu chưa có lựa chọn của giảng viên và không bao giờ quyết định điểm cuối.

**Tiêu chí chấp nhận:**

- Điểm tự động có kèm trạng thái và phương thức chấm.
- Mỗi bài nộp mới ở trạng thái chờ giảng viên xử lý; lựa chọn chấm tay hoặc nhờ AI được lưu theo actor/thời gian.
- Kết quả AI chưa duyệt không được coi là quyết định cuối đối với câu trả lời mở.
- Mọi lần ghi đè điểm lưu người thực hiện, thời gian và lý do.

### FR-009 - Sổ điểm và trạng thái bài nộp

Người học phải xem được điểm, phản hồi và trạng thái bài nộp của chính mình; giảng viên xem được tổng hợp theo lớp được phân công; quản trị viên xem dữ liệu theo quyền quản trị. Yêu cầu này không bao gồm tiến độ hoàn thành hoặc vị trí học theo bài.

### FR-010 - Thanh toán

Hệ thống phải tích hợp một nhà cung cấp thanh toán để tạo giao dịch, nhận kết quả qua webhook và ghi nhận quyền truy cập tương ứng mà không lưu dữ liệu thẻ thanh toán thô.

**Tiêu chí chấp nhận:**

- Webhook được xác minh chữ ký và xử lý idempotent.
- Trạng thái thanh toán được đối soát với nhà cung cấp.
- Lỗi thanh toán không tự cấp quyền truy cập.

### FR-011 - Email và thông báo

Hệ thống phải gửi được các thông báo thiết yếu như kích hoạt/khôi phục tài khoản, ghi danh, giao bài và kết quả đánh giá qua nhà cung cấp email.

### FR-012 - Tích hợp AI/LLM

Backend phải tích hợp nhà cung cấp AI/LLM qua một ranh giới dịch vụ rõ ràng, có timeout, giới hạn chi phí/sử dụng, xử lý lỗi và khả năng thay đổi nhà cung cấp mà không làm rò rỉ chi tiết vào nghiệp vụ cốt lõi.

### FR-013 - Lưu trữ tệp

Hệ thống phải lưu tệp học tập qua một dịch vụ lưu trữ riêng tư, cấp quyền truy cập ngắn hạn và không công khai trực tiếp object chứa dữ liệu học tập.

### FR-014 - Audit nghiệp vụ và bảo mật

Hệ thống phải ghi sự kiện đăng nhập thất bại, thay đổi vai trò hoặc phạm vi môn, thay đổi nội dung đã xuất bản, thay đổi điểm, phát hành đề chung, sự kiện thanh toán và truy cập đặc quyền.

### FR-015 - Vòng đời tài khoản do quản trị viên quản lý

Quản trị viên phải có thể tìm kiếm, tạo, cập nhật và khóa/mở khóa tài khoản; thao tác hàng loạt phải kiểm tra từng dòng và báo kết quả không làm mất các bản ghi hợp lệ. Quản trị viên không đặt, cấp hay xem mật khẩu người dùng và không kích hoạt việc gửi OTP. Tài khoản mới ở trạng thái chờ kích hoạt; tạo hoặc nhập tài khoản không gửi email. Chỉ khi người dùng yêu cầu kích hoạt ở lần đăng nhập đầu, hệ thống mới gửi OTP qua email để người dùng xác minh và tự đặt mật khẩu lần đầu. Yêu cầu gửi OTP được giới hạn tần suất.

### FR-016 - Ngân hàng rubric và câu hỏi

Giảng viên và Chủ nhiệm môn phải có thể tạo, sửa, tìm kiếm và tái sử dụng rubric/câu hỏi trong đúng phạm vi lớp hoặc môn. Mọi lần sửa tạo version truy vết được. Version đã gắn với lượt làm hoặc kết quả chấm phải được bảo toàn để không làm thay đổi bài đang làm và kết quả lịch sử.

**Tiêu chí chấp nhận:**

- Câu hỏi chưa publish có thể sửa trong draft hiện tại.
- Khi một assignment đã publish nhưng vẫn còn trong thời hạn làm bài, chỉnh sửa câu hỏi tạo version mới; không sửa snapshot của lượt làm đã bắt đầu.
- Version mới chỉ áp dụng cho lượt làm bắt đầu sau khi giảng viên phát hành version đó; hệ thống lưu version được dùng cho từng attempt.
- Nếu thay đổi ảnh hưởng đáng kể đến tính công bằng, giảng viên có thể gia hạn hoặc cấp lượt làm lại và hành động này phải được audit.
- Rubric đã dùng để chấm không bị ghi đè; thay đổi tạo version mới cho lần sử dụng sau.

### FR-017 - Các loại bài đánh giá và kiểm thử trước phát hành

Hệ thống phải hỗ trợ sơ đồ Draw.io, trắc nghiệm, Code Lab và bài viết luận. Với bài sơ đồ, người học vẽ trực tiếp trên canvas Draw.io nhúng trong web và nộp XML Draw.io đầy đủ. Bản đầy đủ là bài nộp chuẩn để giảng viên xem/chấm và phải được giữ nguyên; chỉ khi giảng viên yêu cầu AI chấm, hệ thống mới tạo một bản XML rút gọn dẫn xuất theo schema/allowlist để gửi AI. Trước khi phát hành, giảng viên hoặc Chủ nhiệm môn phải xem trước và kiểm tra được cấu hình đặc thù của từng loại bài.

### FR-018 - Lưu nháp, lần nộp và khôi phục bài làm

Hệ thống phải tự động lưu bản nháp theo người học/bài đánh giá, khôi phục an toàn sau gián đoạn và lưu lịch sử các lần nộp; bản nháp không được coi là bài nộp chính thức.

### FR-019 - Theo dõi nộp bài và nhắc nhở

Giảng viên phải xem được trạng thái đã nộp, chưa nộp, nộp trễ và được gia hạn của lớp được phân công, đồng thời gửi nhắc nhở có giới hạn tần suất tới đúng người học.

### FR-020 - Chốt điểm hàng loạt

Giảng viên phải có thể kiểm tra và chốt điểm hàng loạt cho lớp được phân công; chỉ bài đã đủ điều kiện mới được chốt và mọi thay đổi điểm phải được audit.

### FR-021 - Quản trị và giám sát dịch vụ AI

Quản trị viên phải có thể cấu hình model được phép, quota, giới hạn chi phí và kill-switch qua ranh giới provider-neutral; xem nhật ký trạng thái/chi phí mà không lộ prompt, dữ liệu học tập hoặc secret ngoài quyền.

### FR-022 - Tự ghi danh bằng mã mời lớp (Phase 2)

Phase 2 hỗ trợ người học tự ghi danh bằng mã mời còn hiệu lực, có giới hạn thử và không tiết lộ thông tin lớp khi mã không hợp lệ.

### FR-023 - Cộng tác và xử lý ngoại lệ đánh giá (Phase 2)

Phase 2 hỗ trợ thông báo/hỏi đáp lớp, gia hạn nộp bài theo cá nhân, phúc khảo và kiểm tra tương đồng mang tính tham khảo.

### FR-024 - Báo cáo và phân tích nâng cao (Phase 2)

Phase 2 hỗ trợ dashboard kết quả cá nhân, xuất bảng điểm, phân tích câu hỏi và so sánh điểm AI đề xuất với điểm giảng viên chốt; báo cáo không được dùng để tự động kết luận gian lận hoặc đánh giá năng lực cá nhân giảng viên.

### FR-025 - Quản lý nhóm và trưởng nhóm

Giảng viên phải có thể chia sinh viên của lớp được phân công thành nhiều nhóm và chỉ định chính xác một trưởng nhóm cho mỗi nhóm. Thành viên có thể gửi yêu cầu đổi trưởng nhóm nhưng chỉ giảng viên được phê duyệt/từ chối và chỉ định người thay thế.

**Tiêu chí chấp nhận:**

- Mỗi nhóm luôn có đúng một trưởng nhóm đang hiệu lực trước khi nhận bài nhóm.
- Chỉ sinh viên đang ghi danh trong lớp mới được thêm vào nhóm của lớp đó.
- Một thay đổi trưởng nhóm chỉ có hiệu lực sau quyết định của giảng viên và được audit.

### FR-026 - Bài tập nhóm, bài cá nhân và bài chung

Giảng viên phải có thể tạo một bài tập nhóm, định nghĩa cấu trúc/thứ tự các phần và giao từng phần cho thành viên, ví dụ sơ đồ use case hoặc activity. Mỗi sinh viên nộp phần được giao; hệ thống tổng hợp các phần đã nộp thành một tài liệu chung theo cấu trúc do giảng viên định nghĩa. Giảng viên xem trước, đổi thứ tự hoặc loại phần không hợp lệ rồi chốt tài liệu tổng. Giảng viên có thể nhờ AI đề xuất điểm/phản hồi cho phần cá nhân nhưng phải tự chấm tài liệu chung và tự quyết định điểm cuối của từng sinh viên.

**Tiêu chí chấp nhận:**

- Phần cá nhân có deadline/trạng thái/bài nộp riêng nhưng cùng truy vết về một bài tập nhóm và một phiên bản tài liệu tổng hợp.
- Mỗi phần cá nhân được gán cho đúng một thành viên và chỉ thành viên đó nộp; giảng viên có thể đổi phân công trước hạn với audit.
- Hệ thống chỉ tổng hợp các phiên bản phần cá nhân đã nộp, giữ liên kết nguồn và tạo lại tài liệu khi giảng viên yêu cầu; không ghi đè artifact nguồn.
- Giảng viên xem được tài liệu chung cạnh các phần cá nhân, điều chỉnh cấu trúc tổng hợp và chốt một version để chấm.
- AI chỉ tạo đề xuất điểm/phản hồi cho phần cá nhân khi giảng viên chủ động yêu cầu; hệ thống không cung cấp hành động chấm AI cho tài liệu chung.
- Giảng viên chấm tài liệu chung bằng rubric có tiêu chí tích hợp và nhất quán. Lỗi chung trừ ở bài chung; phần cá nhân chỉ bị trừ thêm khi giảng viên xác định được phần hoặc thành viên gây lỗi.
- Điểm/feedback phần cá nhân và điểm tài liệu chung được lưu riêng và hiển thị cạnh nhau. Hệ thống không tự áp dụng công thức; giảng viên dựa trên hai nguồn cùng mức đóng góp để nhập điểm cuối cho từng sinh viên.
- Mọi ghi đè đề xuất AI, điều chỉnh điểm cuối và quy kết lỗi nhất quán cho một phần/thành viên phải lưu lý do và audit actor/thời gian.

### FR-027 - Template đề cấp môn và đề lấy điểm thành phần

Chủ nhiệm môn phải có thể phát hành một template đề chỉ đọc, có version, cho giảng viên các lớp thuộc môn. Giảng viên copy template thành draft riêng của lớp, chỉnh sửa và phát hành cho sinh viên làm hoặc lấy điểm thành phần trong phạm vi lớp được giao.

**Tiêu chí chấp nhận:**

- Chỉ Chủ nhiệm môn có quyền phát hành hoặc tạo version mới của template cấp môn.
- Bản copy thuộc lớp đích và độc lập với template nguồn; cập nhật template không tự ghi đè bản đã copy.
- Hệ thống lưu `source template/version`, người copy, lớp đích và thời gian để truy vết.
- Bản copy không mang theo lịch phát hành, attempt, bài nộp hoặc điểm từ nguồn.

### FR-028 - Sao chép assignment và rubric giữa các lớp

Giảng viên phải có thể copy assignment và rubric từ một lớp sang lớp khác mà chính giảng viên đang được phân công.

**Tiêu chí chấp nhận:**

- Backend kiểm tra quyền của giảng viên trên cả lớp nguồn và lớp đích.
- Bản copy là draft độc lập, giữ nguồn gốc để audit nhưng không đồng bộ hai chiều.
- Assignment copy loại bỏ lịch phát hành, deadline, attempt, bài nộp và điểm; rubric copy giữ cấu trúc/tiêu chí nhưng có identity/version riêng ở lớp đích.
- Không cho copy sang lớp ngoài phạm vi được phân công, kể cả khi thuộc cùng môn.

### FR-029 - Simulation exam

Hệ thống phải cung cấp simulation exam để sinh viên thi thử. Không có chế độ kỳ thi chính thức có giám sát; giảng viên có thể cấu hình một simulation exam tính hoặc không tính vào điểm thành phần.

**Tiêu chí chấp nhận:**

- Giảng viên cấu hình số lượt tối đa, cửa sổ làm bài, cách lấy kết quả cao nhất/gần nhất/trung bình và thời điểm hiển thị đáp án.
- Server thực thi giới hạn lượt và thời gian; mỗi attempt giữ snapshot đề/version riêng.
- Nếu được cấu hình không tính điểm, kết quả chỉ phục vụ luyện tập/phản hồi và không đi vào điểm chính thức.
- Nếu được cấu hình tính điểm thành phần, chính sách lấy kết quả được khóa khi đã có attempt; thay đổi sau đó cần version mới và audit.
- Giao diện và báo cáo phải ghi rõ đây là thi thử, có hay không tính điểm, không được mô tả là kỳ thi chính thức/proctored exam.

## 5. Luồng người dùng chính

### USCN-001 - Chuẩn bị và giao bài cấp lớp bằng AI

Giảng viên tạo khóa học hoặc lớp, nhập nội dung/tải tài liệu, yêu cầu AI tạo câu hỏi, chỉnh sửa và duyệt bản nháp, sau đó xuất bản bài đánh giá cho lớp.

### USCN-001A - Quản lý học liệu và giao đề chung cấp môn

Chủ nhiệm môn quản lý kho học liệu/RAG của môn được phân công, dùng AI biên soạn và duyệt đề chung, sau đó phát hành trực tiếp cho mọi lớp thuộc môn; hệ thống bảo đảm phạm vi môn và ghi audit mà không yêu cầu giảng viên từng lớp duyệt lại.

### USCN-002 - Học và nhận phản hồi

Người học đăng nhập, truy cập lớp được ghi danh, học nội dung, làm bài, nộp bài và xem điểm/phản hồi khi được công bố.

### USCN-003 - Duyệt chấm điểm AI

Sau khi nhận bài nộp, giảng viên chọn chấm thủ công hoặc yêu cầu AI đề xuất điểm theo rubric. Nếu dùng AI, giảng viên kiểm tra, chấp nhận hoặc ghi đè đề xuất trước khi công bố; hệ thống lưu lựa chọn phương thức và audit quyết định cuối.

### USCN-004 - Thanh toán và cấp quyền

Người dùng bắt đầu thanh toán; nhà cung cấp xử lý giao dịch; backend xác minh webhook idempotent; hệ thống chỉ cấp quyền sau trạng thái thanh toán hợp lệ.

### USCN-005 - Xử lý lỗi phụ thuộc

Khi AI, email, lưu trữ hoặc thanh toán tạm thời không khả dụng, hệ thống không làm mất dữ liệu nghiệp vụ, hiển thị trạng thái an toàn và cho phép retry có kiểm soát.

### USCN-006 - Thực hiện và đánh giá bài tập nhóm

Giảng viên chia lớp thành nhóm, chỉ định một trưởng nhóm, định nghĩa cấu trúc bài chung và giao các phần cá nhân. Thành viên nộp phần được giao; hệ thống ghép các phần thành tài liệu tổng để giảng viên rà soát và chốt. Giảng viên có thể nhờ AI đề xuất cho phần cá nhân nhưng tự chấm tài liệu chung, đánh giá tính tích hợp/nhất quán và quyết định điểm cuối từng sinh viên dựa trên cả hai cấp bài làm.

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

Mọi thành phần bảo mật và vận hành phải miễn phí: thư viện mã nguồn mở, Let's Encrypt, GitHub Actions và GHCR với repository public, Mailpit khi phát triển, Gmail SMTP (App Password) khi demo.

## 9. Ràng buộc và giả định đã xác nhận

- Chỉ một tổ chức trong MVP.
- Bốn vai trò trong MVP gồm người học, giảng viên, Chủ nhiệm môn và quản trị viên.
- Không có role Head of Department/Trưởng bộ môn trong hệ thống.
- Chủ nhiệm môn là vai trò RBAC riêng, được gán phạm vi một hoặc nhiều môn; giảng viên vẫn quản lý nội dung riêng của lớp được phân công.
- Chỉ web responsive.
- Nội dung được nhập trực tiếp hoặc tải PDF/DOCX/slide.
- Bốn loại bài đánh giá là sơ đồ Draw.io, trắc nghiệm, Code Lab và bài viết luận; bài sơ đồ lưu/nộp XML Draw.io đầy đủ cho giảng viên, còn XML rút gọn chỉ là dữ liệu dẫn xuất gửi AI khi giảng viên chủ động yêu cầu.
- Bài tập nhóm gồm các phần cá nhân và tài liệu chung do hệ thống tổng hợp theo cấu trúc giảng viên định nghĩa; hệ thống không cung cấp trình soạn thảo cộng tác DOCX.
- Mỗi nhóm có đúng một trưởng nhóm do giảng viên chỉ định; việc nộp phần vẫn thuộc từng thành viên, còn giảng viên chốt tài liệu tổng hợp.
- Không có loại kỳ thi chính thức/proctored exam; simulation exam có thể được cấu hình tính hoặc không tính điểm thành phần.
- Template cấp môn và bản copy giữa lớp luôn tạo bản độc lập có truy vết nguồn, không đồng bộ hoặc mang theo dữ liệu phát hành/kết quả.
- Tích hợp bắt buộc gồm AI/LLM, lưu trữ tệp, thanh toán và email/thông báo.
- Triển khai đợt đầu ưu tiên local container.
- MVP phải có test tự động (bao gồm unit test, integration test, system test, e2e test), tài liệu chạy và khả năng triển khai thử nghiệm.
- Quản trị quy trình AI-DLC: Repository phải duy trì state tracking, audit trail, requirements, user stories, thiết kế, kế hoạch code, kết quả kiểm thử và các checkpoint phê duyệt trong `aidlc-docs/`; mã nguồn ứng dụng không được đặt trong thư mục này.
- Chưa chọn nhà cung cấp AI, payment, email, storage, database hoặc cloud; lựa chọn cụ thể thuộc các stage thiết kế sau và phải tuân thủ yêu cầu trong tài liệu này.

## 10. Tiêu chí thành công của MVP

- Một giảng viên có thể tạo lớp, đưa nội dung vào hệ thống, dùng AI tạo và duyệt bài đánh giá.
- Một giảng viên có thể quản lý rubric/câu hỏi theo version, copy assignment/rubric giữa các lớp được phân công, tổ chức simulation exam giới hạn lượt, theo dõi nộp bài và chốt điểm hàng loạt.
- Một Chủ nhiệm môn có thể quản lý kho học liệu/RAG gồm nguồn YouTube theo bài giảng, phát hành đề chung hoặc template đề có version tới đúng phạm vi môn được phân công.
- Một người học được ghi danh có thể học, nộp bài và nhận điểm/phản hồi đúng quyền.
- Bản nháp và lịch sử lần nộp của người học được bảo toàn qua gián đoạn mà không bị coi nhầm là bài nộp chính thức.
- Quản trị viên có thể quản lý vòng đời tài khoản và kiểm soát quota/kill-switch/chi phí AI mà không khóa hệ thống vào một provider.
- Một nhóm có thể nộp các phần cá nhân để hệ thống tạo tài liệu tổng; AI chỉ hỗ trợ chấm phần cá nhân, còn giảng viên tự chấm tài liệu chung, xử lý lỗi không nhất quán và quyết định điểm cuối từng thành viên.
- Luồng thanh toán thử nghiệm cấp quyền chính xác và chống xử lý webhook trùng lặp.
- Các vai trò không thể truy cập dữ liệu hoặc chức năng ngoài quyền.
- Dữ liệu và hành động nhạy cảm có audit trail phù hợp.
- Dự án chạy được local bằng container và bộ test cốt lõi chạy tự động.
- Tài liệu AI-DLC phản ánh đầy đủ quyết định, checkpoint và trạng thái triển khai.

## 11. Truy vết nguồn yêu cầu

| Nguồn | Yêu cầu liên quan |
|---|---|
| Phiếu xác minh Q1-Q14 | FR-001 đến FR-014, NFR-001 đến NFR-005 |
| Security Baseline Q15 | SEC-001 đến SEC-007 và mục 12 (phạm vi rút gọn) |
| Resiliency Baseline Q16 | REL-001 đến REL-004 và mục 13 (phạm vi rút gọn) |
| Property-Based Testing Q17 | NFR-004, extension bị tắt |
| Làm rõ vòng 1 Q1-Q10 | Web, tích hợp, criticality, DR, change, CI/CD, rollback, topology, incident response |
| Làm rõ vòng 2 Q1-Q2 | Direct/in-place; production single-region multi-zone |
| Làm rõ User Stories Q1-Q3 | Vai trò Chủ nhiệm môn, quyền phát hành đề chung và ranh giới học liệu cấp môn/lớp |
| Đối chiếu `uc1.pdf` và yêu cầu ngày 2026-09-13 | FR-015 đến FR-024; loại Head of Department; dùng chung loại bài viết luận; phân tách MVP và Phase 2 |
| Yêu cầu bài tập nhóm ngày 2026-09-13 | FR-025, FR-026; nhóm/leader và phần cá nhân; cơ chế trưởng nhóm nộp DOCX chung đã được change request 2026-09-22 thay thế bằng tài liệu do hệ thống tổng hợp |
| Change request và làm rõ ngày 2026-09-22 | FR-004, FR-016, FR-026 đến FR-029; YouTube RAG, question version, template/copy, simulation exam và tổng hợp/chấm bài nhóm |

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
