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

### SEC-001 - Bảo vệ dữ liệu

Mọi kết nối từ Internet phải dùng TLS 1.2 trở lên. Dữ liệu nhạy cảm không được ghi log. Theo quyết định của người dùng tại U01 Infrastructure Design, MVP chạy trên một VPS: dữ liệu trên đĩa không mã hóa at rest và lưu lượng giữa các container trên cùng host đi qua mạng Docker nội bộ không TLS. Đây là ngoại lệ SECURITY-01 được chấp nhận.

### SEC-002 - Xác thực và phiên

Mật khẩu phải được băm bằng thuật toán adaptive và có tối thiểu 8 ký tự. Theo quyết định của người dùng tại U01 NFR Requirements, MVP không kiểm tra danh sách mật khẩu đã lộ và không có MFA, kể cả tài khoản quản trị; đây là ngoại lệ SECURITY-12 được chấp nhận. Cookie phiên phải có `Secure`, `HttpOnly`, `SameSite`, thời hạn server-side và bị vô hiệu khi đăng xuất. Login phải có bảo vệ brute-force.

### SEC-003 - Authorization và API

Mọi endpoint mặc định yêu cầu xác thực trừ khi được đánh dấu public. API phải kiểm tra quyền ở mức object và chức năng, xác thực toàn bộ input, giới hạn payload, dùng truy vấn tham số hóa và giới hạn CORS theo allowlist. Endpoint public phải có rate limiting. Thiết kế phải bao phủ các misuse case gồm leo thang đặc quyền, truy xuất nội dung ngoài khóa học qua prompt, thao túng điểm và webhook replay.

### SEC-004 - Bảo mật web

Endpoint phục vụ HTML phải thiết lập tối thiểu `Content-Security-Policy: default-src 'self'` mà không dùng `unsafe-inline`/`unsafe-eval` nếu không có lý do được duyệt; `Strict-Transport-Security: max-age=31536000; includeSubDomains`; `X-Content-Type-Options: nosniff`; `X-Frame-Options: DENY` trừ khi có yêu cầu framing được duyệt; và `Referrer-Policy: strict-origin-when-cross-origin`.

### SEC-005 - Logging, alerting và audit

Ứng dụng phải dùng structured logging với timestamp, correlation ID, level và message. Mọi load balancer, API gateway hoặc CDN xử lý traffic bên ngoài phải bật access logging vào kho tập trung. Log production phải lưu tối thiểu 90 ngày trong kho append-only hoặc tamper-evident. Cảnh báo phải bao phủ đăng nhập thất bại lặp lại, vi phạm authorization và thay đổi đặc quyền; dashboard phải hiển thị các chỉ số vận hành và bảo mật chính. Ứng dụng không được sửa hoặc xóa audit log của chính nó.

### SEC-006 - Chuỗi cung ứng

Dependency phải có lock file hoặc phiên bản chính xác, lấy từ registry tin cậy, được xác minh integrity khi tải, được quét lỗ hổng và loại bỏ khi không dùng. Build production phải tạo SBOM và dùng tool/base image đã khóa phiên bản. Quyền sửa pipeline phải được kiểm soát và thay đổi phải audit được. External script từ CDN, nếu có, phải dùng Subresource Integrity.

### SEC-007 - Fail-safe và error handling

External call, file I/O và database operation phải xử lý lỗi rõ ràng, giải phóng tài nguyên và fail closed. Backend phải có global error handler; phản hồi production không được lộ stack trace, path, phiên bản framework hoặc chi tiết database. Không được có default credential; sample app, tính năng không dùng, directory listing và documentation endpoint không dành cho production phải bị loại bỏ hoặc vô hiệu hóa. Object storage phải chặn public access.

### SEC-008 - Payment và integrity

Hệ thống không lưu thông tin thẻ thô. Webhook phải xác minh chữ ký, chống replay khi nhà cung cấp hỗ trợ và xử lý idempotent. Thay đổi dữ liệu quan trọng phải truy vết được actor và timestamp.

### SEC-009 - IAM và network least privilege

Mọi IAM policy phải giới hạn action và resource cụ thể; wildcard chỉ được dùng khi API không hỗ trợ resource-level permission và phải ghi lý do. Quyền đọc và ghi phải tách khi phù hợp. Network phải deny-by-default, chỉ public load balancer được mở Internet trên cổng 80/443; application, database và storage phải giới hạn nguồn/đích cần thiết, ưu tiên private subnet hoặc private endpoint.

## 8. Yêu cầu resiliency và vận hành

### REL-001 - Mức quan trọng và tác động

MVP có mức quan trọng **Trung bình**: dùng thử với người thật; downtime gây bất tiện nhưng có thể xử lý thủ công. Application Design phải phân loại từng deployable component và ghi rõ dependency/tác động khi không khả dụng.

### REL-002 - Recovery objectives

- Chiến lược DR: Backup & Restore.
- RTO mục tiêu: tính bằng giờ.
- RPO mục tiêu: tính bằng giờ, được tinh chỉnh theo lịch backup trong Infrastructure Design.
- Production topology: một VPS chạy Docker Compose, không multi-zone (ngoại lệ được chấp nhận tại U01 Infrastructure Design).
- Local/demo: được phép chạy một instance và không phải mô hình HA.

### REL-003 - Change management

Do chưa có quy trình tổ chức, AI-DLC phải đề xuất quy trình nhẹ gồm change record, phê duyệt trước production và ghi chú rollback. Git history và tài liệu AI-DLC là nguồn truy vết thay đổi ban đầu.

### REL-004 - CI/CD, deployment và rollback

- AI-DLC phải đề xuất pipeline CI/CD phù hợp với Next.js, Spring Boot và container.
- Chiến lược MVP: direct/in-place.
- Rollback: triển khai lại artifact/container image đã khóa phiên bản trước đó.
- Database migration phải ưu tiên backward compatibility; migration phá vỡ phải có kế hoạch khôi phục riêng trước khi được duyệt.

### REL-005 - Observability và health

Mỗi component production phải phát metrics về latency, error rate, throughput và saturation; log có cấu trúc phải tập trung. Kiến trúc nhiều service phải có distributed tracing và dashboard sức khỏe vận hành. Mỗi service phải có shallow health check; component quan trọng phải có deep health check cho dependency. Health check phải tích hợp với load balancer/service discovery và endpoint public phải có synthetic monitoring hoặc lý do N/A được duyệt.

### REL-006 - Capacity và fault isolation

MVP chạy trên một VPS, không phân bố nhiều availability zone và không có load balancer dự phòng; VPS lỗi thì hệ thống dừng tới khi khôi phục thủ công. Đây là ngoại lệ RESILIENCY-08 được chấp nhận. Mọi container vẫn phải có giới hạn CPU/bộ nhớ và cảnh báo khi đĩa, RAM hoặc CPU vượt 80%.

### REL-007 - Dependency isolation

External call phải có timeout. Dependency quan trọng phải có circuit breaker khi phù hợp; connection/thread pool phải được tách theo bulkhead khi một dependency có thể làm cạn tài nguyên dùng chung; mọi pool/resource limit phải hữu hạn; tính năng không thiết yếu phải có degraded mode thay vì gây lỗi dây chuyền.

### REL-008 - Backup và recovery

Theo quyết định của người dùng tại U01 Infrastructure Design, MVP **không có backup**. VPS hỏng hoặc dữ liệu bị xóa thì mất toàn bộ dữ liệu, không khôi phục được; RPO không xác định. Đây là ngoại lệ RESILIENCY-11 và RESILIENCY-12 được chấp nhận.

### REL-009 - Incident response

AI-DLC phải đề xuất quy trình incident response và Correction of Errors nhẹ, gồm phân loại sự cố, người chịu trách nhiệm, kênh thông báo, post-mortem và theo dõi corrective action.

### REL-010 - Resiliency testing

NFR Design phải trình người dùng lựa chọn cách kiểm thử failover/recovery theo RESILIENCY-14; kịch bản, lịch thực hiện và cơ chế lưu kết quả phải được ghi nhận trước khi hoàn tất thiết kế resiliency.

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
| Security Baseline Q15 | SEC-001 đến SEC-008 và Security Compliance |
| Resiliency Baseline Q16 | REL-001 đến REL-010 và Resiliency Compliance |
| Property-Based Testing Q17 | NFR-004, extension bị tắt |
| Làm rõ vòng 1 Q1-Q10 | Web, tích hợp, criticality, DR, change, CI/CD, rollback, topology, incident response |
| Làm rõ vòng 2 Q1-Q2 | Direct/in-place; production single-region multi-zone |
| Làm rõ User Stories Q1-Q3 | Vai trò Chủ nhiệm môn, quyền phát hành đề chung và ranh giới học liệu cấp môn/lớp |
| Đối chiếu `uc1.pdf` và yêu cầu ngày 2026-09-13 | FR-015 đến FR-024; loại Head of Department; dùng chung loại bài viết luận; phân tách MVP và Phase 2 |
| Yêu cầu bài tập nhóm ngày 2026-09-13 | FR-025, FR-026; nhóm/leader và phần cá nhân; cơ chế trưởng nhóm nộp DOCX chung đã được change request 2026-09-22 thay thế bằng tài liệu do hệ thống tổng hợp |
| Change request và làm rõ ngày 2026-09-22 | FR-004, FR-016, FR-026 đến FR-029; YouTube RAG, question version, template/copy, simulation exam và tổng hợp/chấm bài nhóm |

## 12. Security Compliance tại Requirements Analysis

| Rule | Trạng thái | Cách đáp ứng ở requirements |
|---|---|---|
| SECURITY-01 | Compliant | SEC-001 yêu cầu mã hóa at rest và TLS 1.2+ |
| SECURITY-02 | Compliant | SEC-005 yêu cầu access/centralized logging cho thành phần network-facing ở production |
| SECURITY-03 | Compliant | SEC-005 yêu cầu structured logging và cấm log dữ liệu nhạy cảm |
| SECURITY-04 | Compliant | SEC-004 xác định đầy đủ nhóm HTTP security headers |
| SECURITY-05 | Compliant | SEC-003 yêu cầu validation, size limit, sanitization và parameterized query |
| SECURITY-06 | Compliant | SEC-009 yêu cầu IAM action/resource cụ thể và tách quyền đọc/ghi |
| SECURITY-07 | Compliant | SEC-009 yêu cầu network deny-by-default, giới hạn cổng/nguồn và private placement |
| SECURITY-08 | Compliant | FR-002 và SEC-003 yêu cầu server-side, object-level và function-level authorization |
| SECURITY-09 | Compliant | SEC-007 và NFR-005 yêu cầu hardening, safe errors, no defaults, private storage, secret handling và image pinning |
| SECURITY-10 | Compliant | SEC-006 xác định pinning, scanning, trusted registry và SBOM |
| SECURITY-11 | Compliant | FR-002, SEC-003 và thiết kế abuse controls/rate limiting được đặt làm ràng buộc downstream |
| SECURITY-12 | Compliant | SEC-002 xác định password, MFA admin, session và brute-force controls |
| SECURITY-13 | Compliant | SEC-006 và SEC-008 yêu cầu artifact/pipeline/data integrity, SRI và audit |
| SECURITY-14 | Compliant | SEC-005 xác định alerting, retention và log integrity |
| SECURITY-15 | Compliant | SEC-007 xác định fail-closed, cleanup, global handler và safe errors |

Không có blocking security finding tại Requirements Analysis. Việc triển khai từng control phải được xác minh lại ở các stage thiết kế, code và test.

## 13. Resiliency Compliance tại Requirements Analysis

| Rule | Trạng thái | Cách đáp ứng ở requirements |
|---|---|---|
| RESILIENCY-01 | Compliant | REL-001 xác định mức quan trọng Trung bình và yêu cầu impact/dependency mapping |
| RESILIENCY-02 | Compliant | REL-002 xác định RTO/RPO theo giờ và Backup & Restore |
| RESILIENCY-03 | Compliant | REL-003 yêu cầu quy trình change management nhẹ |
| RESILIENCY-04 | Compliant | REL-004 xác định CI/CD cần đề xuất, direct/in-place và version-pinned rollback |
| RESILIENCY-05 | Compliant | REL-005 yêu cầu metrics, logs, traces và dashboard downstream |
| RESILIENCY-06 | Compliant | REL-005 yêu cầu shallow/deep health checks và integration với routing downstream |
| RESILIENCY-07 | Compliant | REL-005 và REL-006 yêu cầu resiliency/capacity alarms; tool cụ thể thuộc Infrastructure Design |
| RESILIENCY-08 | Compliant | REL-002 và REL-006 xác định production single-region multi-zone; local được miễn HA |
| RESILIENCY-09 | Compliant | REL-006 yêu cầu scaling limits, triggers và quota awareness |
| RESILIENCY-10 | Compliant | REL-007 yêu cầu timeout, circuit breaker, bulkhead/resource limit và degraded mode |
| RESILIENCY-11 | Compliant | REL-002 và REL-008 xác định Backup & Restore cùng runbook |
| RESILIENCY-12 | Compliant | REL-008 yêu cầu backup tự động, mã hóa, retention và test restore |
| RESILIENCY-13 | Compliant | REL-008 yêu cầu failover/failback và recovery validation |
| RESILIENCY-14 | Compliant | REL-010 giữ decision gate bắt buộc tại NFR Design như rule cho phép |
| RESILIENCY-15 | Compliant | REL-009 yêu cầu quy trình incident response và COE nhẹ |

Không có blocking resiliency finding tại Requirements Analysis. Các quyết định và artifact chi tiết phải được xác minh lại ở các stage thiết kế, hạ tầng, code và test.
