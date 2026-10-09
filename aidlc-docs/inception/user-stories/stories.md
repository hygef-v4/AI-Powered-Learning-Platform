# User Stories - AI-Powered Learning Platform

## 1. Quy ước

- Bản hiệu lực 2026-10-09: **73 UC, 51 story MVP, 15 unit**, theo [catalog](../../../docs/use-cases-73.md) và Page-2 của `docs/G21_Diagrams.drawio`. US-CAT-005 đã rút; US-SET-001 thêm vào U03; U10 đã bỏ.
- Stories dùng US-{DOMAIN}-{NNN}, criteria Given/When/Then và FR/NFR. Baseline giữ 51 story: bỏ US-CAT-005, thêm US-SET-001; không dùng lại các mã US-PAY-003/US-ASM-011 đã rút.
- User là actor trừu tượng; bốn persona là Student, Teacher, Subject Manager, Administrator. Tên AI trong nghiệp vụ không khóa provider; tên Gemini vẫn dùng trong adapter kỹ thuật.
- Khi story ghi Teacher/giảng viên, Subject Manager chỉ dùng chức năng đó khi được giao dạy lớp R4; quản lý học liệu/bài/ngân hàng môn cần R2. Admin không kế thừa Teacher/Subject Manager, không nhận phân công môn/lớp và không có ví credit.
- Student theo R5/enrollment, hồ sơ/ví/kết quả/thông báo theo R1; Teacher theo R3. Các tiêu chí kiểm scope áp dụng ở backend kể cả gọi API trực tiếp.
- Chỉ ngân hàng môn UC 56–57 (Question List/Detail); Teacher dùng câu ACTIVE trong selector soạn bài. Rubric UC 46 thuộc từng bài, tự tạo trống theo câu/phần, điền đủ và khóa khi phát hành.
- Chấm Practice riêng tư Student UC 29; Teacher AI Grading Proposals UC 38. Kết quả tách quyền, không gộp hoặc đưa Practice vào gradebook.
- Nhóm thuộc lớp, leader giao phần và nộp bất kỳ lúc nào trước hạn; không có mục chi tiết/bước review. Không có Simulation Exam hay tiến độ hoàn thành bài học.
- Mỗi UC/story có một primary unit; supporting flows không tạo UC giả. US-AIG-003 hỗ trợ Settings UC 70–71 và AI stats Admin Dashboard UC 58; Audit UC 73; US-SET-001 primary U03.
- Màn theo Page-2 G21_Diagrams.drawio: Class/Manager/Admin Dashboard, Student Assignments, Quiz Practice và Settings. Mốc/câu trả lời/approvals cũ giữ lịch sử.

## 2. Miền Identity and Access

### US-IAM-001 - Nhận và kích hoạt tài khoản trường cấp

**Story**: Là người học hoặc giảng viên, tôi muốn kích hoạt tài khoản gắn với email trường để bắt đầu sử dụng nền tảng mà không cần tự đăng ký.

**Truy vết**: FR-001, FR-011, NFR-002, SEC-001, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Kích hoạt thành công

- **Given** tài khoản đã được cấp/import ở trạng thái chờ kích hoạt với email thuộc miền của trường
- **When** ở lần đăng nhập đầu người dùng nhập email trường, yêu cầu kích hoạt, nhập OTP hệ thống gửi qua email và sau khi OTP được xác minh thì đặt mật khẩu đạt chính sách
- **Then** tài khoản được kích hoạt, OTP bị vô hiệu và người dùng được tự đăng nhập, vào thẳng dashboard theo vai trò

#### Scenario 1a - OTP sai

- **Given** người dùng đã nhận OTP kích hoạt
- **When** người dùng nhập sai hoặc mã đã hết hạn
- **Then** hệ thống báo "mã không hợp lệ hoặc đã hết hạn", không mở bước nhập mật khẩu và trừ lượt thử

#### Scenario 2 - Email chưa được cấp hoặc ngoài miền trường

- **Given** email chưa có tài khoản chờ kích hoạt hoặc ngoài allowlist miền trường
- **When** người dùng yêu cầu kích hoạt
- **Then** hệ thống trả phản hồi trung tính giống trường hợp hợp lệ, không gửi email, không tạo tài khoản công khai và hướng dẫn liên hệ quản trị nếu không nhận được mã

#### Scenario 3 - Yêu cầu OTP quá tần suất

- **Given** người dùng vừa yêu cầu OTP kích hoạt
- **When** yêu cầu lại vượt giới hạn tần suất
- **Then** hệ thống không gửi thêm email và vẫn trả phản hồi trung tính

### US-IAM-002 - Đăng nhập và đăng xuất an toàn

**Story**: Là người dùng, tôi muốn đăng nhập và đăng xuất an toàn để chỉ mình tôi sử dụng phiên đã xác thực.

**Truy vết**: FR-001, FR-002, NFR-002, SEC-001, SEC-002, SEC-003, SEC-005, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Đăng nhập đúng thông tin

- **Given** tài khoản đang hoạt động và thông tin xác thực hợp lệ
- **When** người dùng đăng nhập
- **Then** hệ thống tạo phiên có thời hạn và đưa người dùng tới chức năng đúng vai trò/phạm vi

#### Scenario 2 - Đăng nhập thất bại hoặc bị giới hạn

- **Given** thông tin xác thực sai hoặc có nhiều lần thử thất bại
- **When** người dùng đăng nhập
- **Then** hệ thống trả thông báo không tiết lộ tài khoản, áp dụng bảo vệ brute-force và ghi sự kiện phù hợp

#### Scenario 3 - Đăng xuất hoặc hết hạn

- **Given** người dùng có phiên đang hoạt động
- **When** người dùng đăng xuất hoặc phiên hết hạn
- **Then** refresh token của phiên bị thu hồi phía server nên phiên không thể làm mới; access token còn lại hết hạn trong tối đa 15 phút (BR-U01-44, 45)

### US-IAM-003 - Khôi phục mật khẩu riêng tư

**Story**: Là người dùng quên mật khẩu, tôi muốn yêu cầu khôi phục mà không làm lộ trạng thái tài khoản để lấy lại quyền truy cập an toàn.

**Truy vết**: FR-001, FR-011, SEC-001, SEC-002, SEC-003, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Yêu cầu khôi phục

- **Given** một địa chỉ email bất kỳ
- **When** người dùng yêu cầu khôi phục
- **Then** hệ thống trả cùng một thông báo dù tài khoản có tồn tại hay không và chỉ gửi OTP có thời hạn nếu tài khoản đang hoạt động; người dùng nhập OTP trước, chỉ khi OTP đúng mới được đặt mật khẩu mới

#### Scenario 2 - Email tạm thời lỗi

- **Given** yêu cầu hợp lệ nhưng nhà cung cấp email không khả dụng
- **When** hệ thống thử gửi thông báo
- **Then** hệ thống không lộ tài khoản hoặc lỗi nội bộ, ghi nhận trạng thái có thể xử lý lại với retry hữu hạn

### US-IAM-004 - Quản lý hồ sơ cá nhân

**Story**: Là người dùng, tôi muốn xem và cập nhật thông tin hồ sơ tối thiểu của mình để dữ liệu tài khoản luôn chính xác.

**Truy vết**: FR-001, FR-002, NFR-002, SEC-005, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Cập nhật hồ sơ của chính mình

- **Given** người dùng đã xác thực
- **When** người dùng gửi dữ liệu hồ sơ hợp lệ
- **Then** chỉ display name/phone number được phép của chính người đó được cập nhật; không có cập nhật avatar, email định danh, role/status; dữ liệu nhạy cảm không xuất hiện trong log

#### Scenario 2 - Cố sửa hồ sơ người khác

- **Given** người dùng không có quyền quản trị đối tượng đích
- **When** người dùng gửi định danh của người khác
- **Then** hệ thống từ chối phía server mà không tiết lộ dữ liệu của đối tượng

#### Scenario 3 - Xem hồ sơ

- **Given** người dùng đã xác thực
- **When** mở User Profile
- **Then** hệ thống hiển thị thông tin tài khoản và role của chính người đó; không trả hồ sơ khác bằng ID ngoài quyền

### US-IAM-005 - Quản lý vai trò và phạm vi môn

**Story**: Là quản trị viên, tôi muốn gán hoặc thu hồi vai trò và phạm vi môn để người dùng chỉ có đúng quyền cần thiết.

**Truy vết**: FR-002, FR-003, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Gán Chủ nhiệm môn

- **Given** quản trị viên đã xác thực và một môn hợp lệ
- **When** quản trị viên gán vai trò Chủ nhiệm môn cùng phạm vi môn
- **Then** quyền cấp môn chỉ có hiệu lực với môn đó và thay đổi được ghi actor, thời gian, giá trị trước/sau; tài khoản Administrator không được gán làm Chủ nhiệm môn hay giảng viên lớp

#### Scenario 2 - Thu hồi quyền

- **Given** một quyền hoặc phạm vi đang có hiệu lực
- **When** quản trị viên thu hồi
- **Then** thu hồi phạm vi môn/lớp có hiệu lực ngay ở request tiếp theo; đổi role làm refresh token hết hiệu lực ngay và access token còn lại hết hạn trong tối đa 15 phút (BR-U01-63)

#### Scenario 3 - Người không phải quản trị viên thay đổi quyền

- **Given** người dùng không có quyền quản trị
- **When** người đó gọi chức năng phân vai
- **Then** hệ thống từ chối phía server và ghi sự kiện vi phạm authorization

### US-IAM-006 - Đổi mật khẩu cá nhân

**Story**: Là người dùng đã xác thực, tôi muốn đổi mật khẩu để chủ động bảo vệ tài khoản của mình.

**Truy vết**: FR-001, SEC-001, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Đổi mật khẩu hợp lệ

- **Given** người dùng xác nhận đúng mật khẩu hiện tại và mật khẩu mới đạt chính sách
- **When** người dùng yêu cầu đổi mật khẩu
- **Then** mật khẩu mới được lưu bằng cơ chế băm an toàn, các phiên khác không làm mới được nữa (access token còn lại hết hạn trong tối đa 15 phút) và người dùng nhận xác nhận

#### Scenario 2 - Thông tin không hợp lệ

- **Given** mật khẩu hiện tại sai hoặc mật khẩu mới không đạt chính sách
- **When** người dùng gửi yêu cầu
- **Then** hệ thống từ chối an toàn, không thay đổi credential và không lộ chi tiết nội bộ

### US-IAM-007 - Quản trị vòng đời tài khoản

**Story**: Là quản trị viên, tôi muốn tìm kiếm, tạo, cập nhật và khóa/mở khóa tài khoản để quản lý tài khoản người dùng trong tổ chức.

**Truy vết**: FR-002, FR-015, FR-014, SEC-001, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Quản lý một tài khoản

- **Given** quản trị viên có quyền và dữ liệu tài khoản hợp lệ
- **When** quản trị viên tạo, cập nhật hoặc khóa/mở khóa
- **Then** thay đổi có hiệu lực đúng một tài khoản, tài khoản mới ở trạng thái chờ kích hoạt và không có email nào được gửi, quản trị viên không đặt hay xem mật khẩu, và hành động được audit

#### Scenario 2 - Nhập tài khoản hàng loạt

- **Given** tệp nhập nằm trong giới hạn và có cả dòng hợp lệ lẫn không hợp lệ
- **When** quản trị viên xác nhận nhập
- **Then** hệ thống xử lý idempotent, báo kết quả theo dòng và không tạo tài khoản từ dữ liệu không hợp lệ

#### Scenario 3 - Người không có quyền quản trị tài khoản

- **Given** người dùng không có quyền phù hợp
- **When** gọi chức năng quản trị tài khoản hoặc dùng ID trực tiếp
- **Then** hệ thống từ chối phía server và ghi sự kiện authorization

#### Scenario 4 - Xem danh sách và chi tiết tài khoản

- **Given** Administrator đã xác thực
- **When** mở Account List, chọn Account Detail và cập nhật thông tin được phép/role/status
- **Then** xem đúng tài khoản, validate thay đổi và audit; tài khoản mới tự kích hoạt, Admin không đặt/xem mật khẩu

## 3. Miền Academic Structure and Content

### US-CAT-001 - Quản lý cấu trúc môn và lớp

**Story**: Là quản trị viên, tôi muốn tạo môn và giao môn cho Chủ nhiệm môn; là Chủ nhiệm môn, tôi muốn tạo lớp thuộc môn mình và giao giảng viên để cấu trúc học thuật phản ánh đúng hoạt động đào tạo.

**Truy vết**: FR-002, FR-003, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Tạo cấu trúc hợp lệ

- **Given** quản trị viên và tài khoản Subject Manager ACTIVE
- **When** quản trị viên thêm môn và chọn Chủ nhiệm môn; Chủ nhiệm môn tạo lớp và gán giảng viên
- **Then** mỗi lớp thuộc một môn; Chủ nhiệm môn là Subject Manager ACTIVE, giảng viên chính là Teacher hoặc Subject Manager ACTIVE; Administrator không được gán; phân công được lưu và audit

#### Scenario 2 - Tham chiếu không hợp lệ

- **Given** môn, lớp hoặc người dùng không tồn tại/không phù hợp vai trò
- **When** gửi cấu hình
- **Then** hệ thống từ chối toàn bộ thay đổi không hợp lệ và trả hướng dẫn khắc phục an toàn

#### Scenario 3 - Chủ nhiệm môn quản lý lớp của môn

- **Given** Chủ nhiệm môn được phân công một môn `ACTIVE`
- **When** mở Class List từ Manager Dashboard để tạo lớp, gán/đổi giảng viên chính, sửa, mở/lưu trữ lớp hoặc thêm/gỡ sinh viên
- **Then** thao tác được thực hiện và audit; cùng thao tác trên lớp của môn không được phân công bị từ chối ở mức đối tượng

#### Scenario 4 - Xem môn và lớp theo scope

- **Given** Administrator, hoặc Subject Manager có phân công môn
- **When** Administrator mở Subject List/Subject Detail; Subject Manager mở Manager Dashboard, Class List/Class Detail
- **Then** Administrator thấy mọi môn và danh sách lớp của môn chỉ đọc; Subject Manager chỉ thấy môn/lớp được giao; quyền cấu trúc không tự cấp gradebook/chấm

### US-CAT-002 - Xem và quản lý vòng đời lớp theo quyền

**Story**: Là Chủ nhiệm môn, tôi muốn sửa/mở/lưu trữ lớp của môn mình; là Teacher của lớp, tôi muốn xem danh sách/chi tiết lớp được giao để quản lý hoạt động giảng dạy.

**Truy vết**: FR-002, FR-003, FR-014, NFR-002, SEC-002, SEC-003, SEC-005, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Mở lớp

- **Given** Chủ nhiệm môn của môn, lớp DRAFT có giảng viên chính và môn ACTIVE
- **When** mở lớp
- **Then** lớp chuyển OPEN, Student ghi danh thấy lớp/học liệu được phép; sự kiện ghi danh được gửi và thay đổi audit

#### Scenario 2 - Lớp đã lưu trữ

- **Given** lớp/học liệu lưu trữ
- **When** Student truy cập
- **Then** không hiển thị nội dung ngoài chính sách đã được cấp rõ ràng

#### Scenario 3 - Quyền đọc không cấp quyền sửa

- **Given** Teacher chỉ có phân công dạy lớp, Administrator, hoặc Subject Manager không được giao môn
- **When** sửa thông tin/vòng đời lớp (UC 52)
- **Then** backend từ chối; quyền xem lớp không đủ quyền sửa cấu trúc

#### Scenario 4 - Xem lớp được giao

- **Given** Teacher hoặc Subject Manager được giao dạy lớp
- **When** mở Class Dashboard rồi Teacher Class Detail
- **Then** chỉ liệt kê lớp được giao; tab Students chỉ xem danh sách và thông tin cơ bản của sinh viên; các lối vào học liệu, quiz, bài, bài nộp/gradebook theo quyền

### US-CAT-003 - Ghi danh người học

**Story**: Là Chủ nhiệm môn, tôi muốn thêm và gỡ sinh viên trong lớp của môn mình để họ nhận đúng nội dung và bài tập.

**Truy vết**: FR-002, FR-003, FR-011, FR-014, SEC-002, SEC-003, SEC-005, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Ghi danh hợp lệ

- **Given** lớp `DRAFT` hoặc `OPEN` và người thực hiện là Chủ nhiệm môn của môn
- **When** người học được ghi danh
- **Then** ghi danh được tạo một lần; thông báo ghi danh được xếp gửi khi lớp đã `OPEN`

#### Scenario 2 - Ghi danh trùng hoặc ngoài quyền

- **Given** người học đã được ghi danh, hoặc người thực hiện không phải Chủ nhiệm môn của môn (kể cả Teacher của lớp)
- **When** yêu cầu được gửi
- **Then** hệ thống không tạo bản ghi trùng, không mở rộng quyền và trả kết quả an toàn

#### Scenario 3 - Gỡ người học khỏi lớp

- **Given** Chủ nhiệm môn của môn và người học đang được ghi danh
- **When** người thực hiện xác nhận gỡ ghi danh
- **Then** quyền truy cập mới bị thu hồi, dữ liệu học tập lịch sử được giữ theo chính sách và thay đổi được audit

### US-CNT-001 - Quản lý kho học liệu và RAG cấp môn

**Story**: Là Chủ nhiệm môn, tôi muốn tạo các module của môn và tải học liệu (tệp hoặc video YouTube) vào module trên Material List để mọi lớp của môn dùng chung nguồn đã kiểm soát, đồng thời theo dõi trạng thái quét RAG.

**Truy vết**: FR-002, FR-004, FR-012, FR-013, FR-014, NFR-003, SEC-005, SEC-002, SEC-003, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Tải tài liệu hợp lệ

- **Given** Chủ nhiệm môn được gán môn và tệp PDF/DOCX/PPTX nằm trong giới hạn
- **When** Chủ nhiệm môn bấm nút tải tệp trên một module ở Material List
- **Then** tệp được lưu riêng tư, tạo một lesson của môn hiển thị ngay cho người học mọi lớp của môn và trạng thái quét chuyển qua chờ/đang quét/thành công hoặc thất bại

#### Scenario 2 - Tệp không hợp lệ

- **Given** tệp sai loại, quá kích thước hoặc không vượt qua kiểm tra đầu vào
- **When** Chủ nhiệm môn tải lên
- **Then** tệp bị từ chối trước xử lý và thông báo nêu cách khắc phục mà không lộ chi tiết nội bộ

#### Scenario 3 - Xử lý RAG thất bại

- **Given** tệp đã lưu nhưng dependency xử lý không khả dụng
- **When** tác vụ hết timeout hoặc thất bại
- **Then** dữ liệu gốc không mất, trạng thái thất bại được hiển thị và retry chỉ diễn ra có giới hạn/backoff

#### Scenario 4 - Truy cập sai môn

- **Given** Chủ nhiệm môn không được gán môn đích
- **When** người đó đọc hoặc sửa kho học liệu
- **Then** hệ thống từ chối ở mức đối tượng và không cấp URL tệp

#### Scenario 5 - Cập nhật và loại bỏ học liệu môn

- **Given** người quản lý môn có R2
- **When** sửa thông tin hoặc xóa học liệu của môn trên Material Detail
- **Then** sửa chỉ đổi tên, không đổi tệp hay link; học liệu bị xóa được lưu trữ, không hiển thị/dùng cho RAG; tham chiếu lịch sử và audit được giữ

#### Scenario 6 - Upload chưa tóm tắt; Chủ nhiệm môn yêu cầu khi xem

- **Given** Chủ nhiệm môn có R2 tải học liệu hợp lệ, kể cả khi số dư credit bằng 0
- **When** upload và trích chữ hoàn tất
- **Then** học liệu xem/tải được, chưa có summary/embedding và không giữ/trừ credit
- **When** Chủ nhiệm môn bấm "Tóm tắt tài liệu" trên Material Detail
- **Then** server kiểm quyền xem, giữ credit của người bấm và chạy tóm tắt/embedding; thiếu credit thì học liệu vẫn còn, có thể mua thêm rồi yêu cầu; kết quả dùng chung trong phạm vi

### US-CNT-002 - Quản lý nội dung riêng của lớp

**Story**: Là giảng viên, tôi muốn tải học liệu riêng của lớp được phân công vào các module của môn trên tab Materials để bổ sung học liệu phù hợp với lớp mình.

**Truy vết**: FR-002, FR-003, FR-004, FR-013, FR-014, NFR-003, SEC-005, SEC-002, SEC-003, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Quản lý nội dung lớp

- **Given** giảng viên được phân công lớp
- **When** giảng viên bấm nút trên một module và tải tệp hoặc link video hợp lệ
- **Then** lesson là học liệu của lớp đó (lớp khác không thấy), lưu riêng tư và có trạng thái quét quan sát được

#### Scenario 2 - Không sửa kho cấp môn

- **Given** giảng viên không có vai trò Chủ nhiệm môn của môn tương ứng
- **When** giảng viên thử tạo/sửa module hoặc sửa học liệu của môn
- **Then** hệ thống từ chối phía server và giữ nguyên tài nguyên

#### Scenario 3 - Storage lỗi

- **Given** dịch vụ lưu trữ tạm thời không khả dụng
- **When** giảng viên tải tệp
- **Then** hệ thống không tạo nội dung ở trạng thái thành công giả, trả trạng thái an toàn và cho phép thử lại có kiểm soát

#### Scenario 4 - Xem và loại bỏ học liệu lớp

- **Given** tài khoản được giao lớp theo R3/R4
- **When** mở tab Materials, chọn Material Detail rồi sửa thông tin hoặc xóa học liệu
- **Then** chỉ tác động học liệu của lớp, học liệu của môn chỉ đọc; sửa không đổi tệp hay link, học liệu bị xóa được lưu trữ, không xóa tham chiếu lịch sử

#### Scenario 5 - Teacher yêu cầu tóm tắt khi xem

- **Given** Teacher/Subject Manager có R3/R4 xem học liệu của lớp hoặc học liệu của môn dùng trong lớp mình dạy
- **When** bấm "Tóm tắt tài liệu" trên Material Detail sau khi trích chữ xong
- **Then** quyền xem đủ để yêu cầu, không cần quyền sửa học liệu của môn; người bấm chịu credit, upload trước đó không giữ credit; có summary sẵn thì dùng lại không gọi/trừ thêm

### US-CNT-004 - Thông báo trong lớp

**Story**: Là thành viên lớp, tôi muốn đọc thông báo của các lớp mình học hoặc dạy tại một nơi; giảng viên của lớp tạo/sửa/xóa thông báo để thông tin đến đúng lớp.

**Truy vết**: FR-002, FR-003, FR-011, FR-023, SEC-002, SEC-003, SEC-005, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Giảng viên đăng thông báo

- **Given** giảng viên được phân công lớp `OPEN`
- **When** giảng viên đăng thông báo
- **Then** thông báo chỉ hiển thị trong lớp, người học đang ghi danh nhận thông báo trong ứng dụng và actor/thời gian được lưu; người học không đăng được thông báo

#### Scenario 2 - Xem thông báo các lớp

- **Given** người học được ghi danh hoặc giảng viên của lớp `OPEN`
- **When** người dùng mở Class Announcements từ Class Dashboard
- **Then** hiện thông báo của mọi lớp mình học hoặc dạy, mới nhất trước, lọc được theo lớp; không có bình luận dưới thông báo

#### Scenario 3 - Nội dung không hợp lệ hoặc ngoài lớp

- **Given** nội dung vượt giới hạn, chứa dữ liệu bị cấm hoặc người dùng không thuộc lớp
- **When** yêu cầu được gửi
- **Then** hệ thống từ chối và không tiết lộ thành viên/nội dung lớp

#### Scenario 4 - Sửa thông báo

- **Given** tài khoản được giao dạy lớp theo R3/R4 và thông báo còn hiệu lực
- **When** gửi nội dung cập nhật hợp lệ cùng version
- **Then** nội dung được cập nhật, audit trước/sau được lưu; không gửi lặp sự kiện tạo mới và version cũ xung đột bị từ chối

#### Scenario 5 - Xóa thông báo

- **Given** tài khoản được giao dạy lớp theo R3/R4
- **When** xác nhận xóa thông báo của lớp
- **Then** thông báo không còn trong feed, lưu dấu đã xóa và audit/tham chiếu; Student hoặc ID ngoài lớp không được xóa

### US-CNT-005 - Dùng YouTube làm nguồn RAG theo bài giảng

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn thêm một video YouTube làm lesson và để hệ thống quét phụ đề, dùng đúng nguồn đó cho RAG.

**Truy vết**: FR-002, FR-004, FR-012, FR-014, NFR-003, SEC-002, SEC-003, SEC-005, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Video có caption

- **Given** người dùng có quyền tải vào module và URL một video YouTube hợp lệ có caption
- **When** người dùng thêm video
- **Then** hệ thống tạo lesson, lấy phụ đề có sẵn, lưu chữ và embedding trên lesson trong đúng phạm vi RAG

#### Scenario 2 - Playlist hoặc video không có caption

- **Given** URL là playlist hoặc video không có caption khả dụng
- **When** người dùng thêm hoặc hệ thống quét
- **Then** playlist bị từ chối ngay; video không có caption được đánh dấu "không có phụ đề" (`NO_CAPTION`), không lập chỉ mục nhưng vẫn xem được

#### Scenario 3 - Nguồn lỗi hoặc ngoài quyền

- **Given** URL không hợp lệ, video không truy cập được, lấy caption thất bại hoặc module ngoài quyền
- **When** yêu cầu được xử lý
- **Then** hệ thống không lập chỉ mục kết quả lỗi/ngoài quyền, tự thử lại khi lỗi tạm, báo quét lỗi khi hết lượt và không ghi kết quả quét giả

## 4. Miền Group Assignment

### US-GRP-001 - Chia lớp thành nhóm và chỉ định trưởng nhóm

**Story**: Là giảng viên, tôi muốn chia sinh viên của lớp được phân công thành nhóm ngay trong danh sách sinh viên của lớp (tạo tay hoặc chia ngẫu nhiên) và chỉ định một trưởng nhóm cho mỗi nhóm để mọi bài tập nhóm của lớp có trách nhiệm rõ ràng.

**Truy vết**: FR-002, FR-003, FR-025, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Tạo nhóm hợp lệ

- **Given** giảng viên quản lý lớp và các sinh viên đang được ghi danh
- **When** giảng viên tạo nhóm, thêm thành viên và chọn trưởng nhóm
- **Then** mỗi nhóm có đúng một trưởng nhóm, thành viên thuộc đúng lớp và thay đổi được audit

#### Scenario 3 - Chia ngẫu nhiên

- **Given** lớp còn sinh viên chưa có nhóm
- **When** giảng viên chọn chia ngẫu nhiên với sĩ số tối đa
- **Then** hệ thống chỉ chia sinh viên chưa có nhóm thành các nhóm chênh nhau tối đa một người, chọn ngẫu nhiên trưởng nhóm và cho giảng viên xem trước, sửa rồi mới lưu

#### Scenario 2 - Thành viên hoặc trưởng nhóm không hợp lệ

- **Given** sinh viên ngoài lớp, đã thuộc nhóm khác trong lớp hoặc trưởng nhóm không phải thành viên
- **When** giảng viên lưu cấu hình
- **Then** hệ thống từ chối phần cấu hình không nhất quán và không mở rộng quyền ngoài lớp

#### Scenario 4 - Student xem nhóm của mình

- **Given** Student đang ghi danh và có nhóm trong lớp
- **When** mở My Group
- **Then** hiển thị thành viên, leader và tài liệu nhóm của chính nhóm; ID nhóm khác bị từ chối

### US-GRP-002 - Yêu cầu thay đổi trưởng nhóm

**Story**: Là thành viên nhóm, tôi muốn gửi yêu cầu thay đổi trưởng nhóm để giảng viên xem xét khi phân công hiện tại không còn phù hợp.

**Truy vết**: FR-002, FR-011, FR-025, FR-014, SEC-002, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Giảng viên phê duyệt

- **Given** người yêu cầu là thành viên nhóm và người được đề xuất cũng thuộc nhóm
- **When** giảng viên phê duyệt và xác nhận trưởng nhóm mới
- **Then** nhóm vẫn có đúng một trưởng nhóm, vai trò điều phối chuyển sang người mới và quyết định được audit mà không đổi các mục thành viên đang nhận trong tài liệu nhóm

#### Scenario 2 - Từ chối hoặc yêu cầu không hợp lệ

- **Given** yêu cầu ngoài nhóm, trùng yêu cầu đang chờ hoặc đề xuất người không thuộc nhóm
- **When** yêu cầu được gửi hoặc giảng viên từ chối
- **Then** trưởng nhóm hiện tại không thay đổi và người liên quan nhận trạng thái/lý do phù hợp

### US-GRP-003 - Tạo bài tập nhóm dạng tài liệu chung

**Story**: Là giảng viên của lớp (bài nhóm chỉ có ở lớp, không có ở cấp môn), tôi muốn tạo bài tập nhóm là một tài liệu chung có khung chia thành các phần, mỗi phần một rubric, để nhóm chia các phần cho thành viên và cùng hoàn thành một sản phẩm nhóm.

**Truy vết**: FR-002, FR-007, FR-016, FR-017, FR-026, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Khung chia phần

- **Given** lớp đã có nhóm hợp lệ
- **When** giảng viên soạn khung tài liệu, hệ thống tự chia khung thành các phần theo heading nhỏ nhất của mỗi nhánh (ví dụ use case diagram, activity diagram) và tự tạo rubric trống cho từng phần, giảng viên điền rubric rồi duyệt và phát hành
- **Then** mỗi nhóm của lớp nhận một tài liệu chung theo khung; mỗi phần là một mục ở trạng thái trống để trưởng nhóm giao hoặc để thành viên nhận

#### Scenario 2 - Nhả khóa mục khi cần

- **Given** một mục đang bị một thành viên giữ quá lâu hoặc thành viên đó rời nhóm
- **When** trưởng nhóm hoặc giảng viên nhả khóa
- **Then** hệ thống cảnh báo trước khi nhả; mục trở lại trạng thái có thể nhận, nội dung đã "Xong" giữ nguyên kèm tác giả, bản nháp chưa "Xong" bị bỏ và thao tác được audit

### US-GRP-004 - Nhận và làm mục trong tài liệu nhóm

**Story**: Là thành viên nhóm, tôi muốn nhận hoặc được trưởng nhóm giao một mục trong tài liệu nhóm, làm mục đó trong popup che kín trang rồi đưa vào tài liệu chung.

**Truy vết**: FR-002, FR-007, FR-008, FR-018, FR-026, FR-014, SEC-002, SEC-003, SEC-006, SEC-007, REL-003.

**Acceptance criteria**

#### Scenario 1 - Nhận và làm mục

- **Given** mục đang trống và bài còn hạn
- **When** thành viên nhận mục
- **Then** mục bị khóa cho thành viên đó, mở ra trong popup che kín trang hiện các heading trên nhánh của phần và nội dung phần, không hiện nhánh khác; thành viên khác không sửa được mục đó

#### Scenario 1b - Trưởng nhóm giao phần

- **Given** tài liệu nhóm gồm các phần của khung
- **When** trưởng nhóm giao một phần cho một thành viên
- **Then** phần được khóa cho thành viên được giao, không ai thêm, xóa hay đổi tên phần, thành viên khác không giao được phần và thao tác được audit

#### Scenario 2 - Xong mục

- **Given** thành viên làm xong mục
- **When** thành viên bấm "Xong"
- **Then** nội dung được ghép realtime vào tài liệu chung, mục chuyển sang xong và mở khóa để thành viên khác có thể nhận sửa; lịch sử ghi tác giả phiên bản; khi mọi mục đều xong, tài liệu không đổi trạng thái và không gửi thông báo

#### Scenario 3 - Giảng viên chọn phương thức chấm tài liệu nhóm

- **Given** tài liệu nhóm đã nộp
- **When** giảng viên chấm tài liệu chung thủ công hoặc "Nhờ AI đề xuất" như bài `DOCUMENT`
- **Then** hệ thống áp dụng đúng luồng đã chọn, lưu actor/thời gian và AI chỉ tạo đề xuất chưa công bố

### US-GRP-005 - Tài liệu chung và nộp bài nhóm

**Story**: Là thành viên nhóm, tôi muốn thấy tài liệu chung cập nhật realtime và trưởng nhóm nộp bài khi xong để giảng viên chấm đúng bản của nhóm.

**Truy vết**: FR-002, FR-007, FR-013, FR-018, FR-026, FR-014, NFR-003, SEC-005, SEC-002, SEC-003, SEC-006, SEC-007, REL-003.

**Acceptance criteria**

#### Scenario 1 - Cập nhật realtime

- **Given** nhiều thành viên đang mở tài liệu chung
- **When** một mục được nhận, nhả hoặc bấm "Xong"
- **Then** mọi người đang xem thấy trạng thái và nội dung mới mà không cần tải lại

#### Scenario 2 - Trưởng nhóm nộp

- **Given** tài liệu chung còn trong hạn
- **When** trưởng nhóm nộp (hệ thống cảnh báo nếu còn mục chưa xong)
- **Then** hệ thống lưu một bản bất biến kèm tác giả từng mục; trưởng nhóm có thể nộp lại trước hạn, bản nộp cuối được chấm

#### Scenario 3 - Hết hạn

- **Given** hết hạn mà nhóm chưa nộp
- **When** tới hạn
- **Then** hệ thống tự nộp bản hiện tại, mục đang nhận được đưa vào với nội dung đã lưu gần nhất và giảng viên thấy cảnh báo; mọi thành viên đang mở tài liệu (đang sửa hoặc chỉ xem) thấy màn chờ nộp rồi được chuyển sang trang bài đã nộp

### US-GRP-006 - Chấm tài liệu nhóm và điểm đóng góp từng thành viên

**Story**: Là giảng viên, tôi muốn chấm tài liệu nhóm đã nộp như một bài tài liệu và đặt điểm đóng góp từng thành viên (mặc định bằng điểm tài liệu chung, chấm tay khi cần) để chấm nhanh mà vẫn điều chỉnh được cho từng người.

**Truy vết**: FR-002, FR-008, FR-009, FR-020, FR-026, FR-014, SEC-005, SEC-002, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Chấm tài liệu chung

- **Given** nhóm đã nộp tài liệu chung
- **When** giảng viên mở bài nộp nhóm trong Grading Workspace
- **Then** hệ thống hiển thị bản nộp cuối như bài `DOCUMENT` (không tô màu theo tác giả) và cho chấm tay theo rubric của từng phần; điểm tài liệu chung là tổng điểm các phần

#### Scenario 2 - Nhờ AI đề xuất cho tài liệu chung

- **Given** giảng viên đang chấm tài liệu chung và đủ credit
- **When** chọn "Nhờ AI đề xuất" (hoặc chọn bài trong chấm hàng loạt)
- **Then** AI trả đề xuất như với bài `DOCUMENT`; giảng viên dùng hoặc sửa và là người quyết định điểm

#### Scenario 3 - Mục còn trống

- **Given** một hoặc nhiều mục chưa có nội dung khi nộp
- **When** giảng viên chấm tài liệu nhóm
- **Then** hệ thống chỉ rõ mục còn trống và giảng viên chấm phần đó theo rubric của phần như bài thường

#### Scenario 4 - Nội dung không nhất quán

- **Given** các mục đúng riêng lẻ nhưng xung đột khi đặt chung
- **When** giảng viên chấm theo rubric từng phần
- **Then** lỗi không khớp được trừ ở rubric của phần liên quan (không có điểm tích hợp riêng, điểm tài liệu chung vẫn là tổng các phần); giảng viên có thể trừ thêm cho một thành viên qua điểm đóng góp, ghi lý do nếu muốn

#### Scenario 5 - Điểm đóng góp từng thành viên

- **Given** điểm tài liệu chung đã có
- **When** giảng viên xem điểm đóng góp các thành viên
- **Then** điểm đóng góp của mọi thành viên mặc định bằng điểm tài liệu chung; giảng viên chấm tay từng người nếu cần, lý do tùy chọn, hệ thống audit mọi thay đổi

#### Scenario 6 - Nhóm nộp lại

- **Given** nhóm nộp lại trước hạn sau khi đã được chấm
- **When** bản nộp mới được lưu
- **Then** xử lý như lượt nộp mới của bài `DOCUMENT`: bài về chờ chấm theo bản mới, điểm cũ giữ trong lịch sử

## 5. Miền Learning Journey

### US-LRN-001 - Truy cập lớp đã ghi danh

**Story**: Là người học, tôi muốn xem các module và học liệu của lớp được ghi danh để học đúng chương trình.

**Truy vết**: FR-002, FR-003, FR-005, FR-013, NFR-002, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Truy cập hợp lệ

- **Given** người học được ghi danh và học liệu đang hiển thị
- **When** người học mở lớp
- **Then** hệ thống hiển thị nội dung cấp môn/lớp được phép và cấp quyền tệp ngắn hạn khi cần

#### Scenario 2 - Không ghi danh hoặc học liệu đã lưu trữ

- **Given** người học không thuộc lớp hoặc học liệu đã lưu trữ
- **When** người học dùng URL/ID trực tiếp
- **Then** hệ thống từ chối mà không tiết lộ nội dung hoặc metadata nhạy cảm

#### Scenario - Student tóm tắt tài liệu được xem

- **Given** Student ghi danh ACTIVE, lớp OPEN, module/lesson ACTIVE thuộc phạm vi lớp và đã trích chữ thành công
- **When** bấm "Tóm tắt tài liệu" trên Learning Material
- **Then** server kiểm lại quyền xem và số dư của Student; một yêu cầu dùng chung cùng một HOLD, chỉ tính AI thật; thiếu credit/AI đang tắt trước nhận yêu cầu không ảnh hưởng xem/tải
- **Given** hai người cùng bấm hoặc người khác mở sau khi có summary
- **Then** chỉ một yêu cầu được nhận, chỉ người tạo yêu cầu chịu credit; người còn lại thấy trạng thái/kết quả, không giữ credit thêm; không cho Student gọi embedding độc lập hay AI soạn/chấm Graded

## 6. Miền Rubric Bank and Quiz Questions


### US-QBK-001 - Quản lý rubric của bài

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn mỗi câu hoặc phần của bài tự có rubric để tôi điền và sửa trong Rubric Detail khi soạn bài, giúp chấm bài nhất quán.

**Truy vết**: FR-002, FR-016, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Quản lý rubric hợp lệ

- **Given** người dùng có quyền với bài (lớp mình dạy hoặc môn mình quản lý), bài còn nháp và rubric có các mục checklist hợp lệ
- **When** người dùng thêm câu Text Essay hoặc khung Diagram Essay/bài nhóm được chia phần, rồi điền tiêu chí và mục trong Rubric Detail
- **Then** mỗi câu hoặc phần đã có sẵn một rubric trống; rubric được lưu cho đúng câu hoặc phần; mỗi tiêu chí gồm các mục checklist có điểm và điểm rubric là tổng điểm các mục đạt; sửa ghi đè trực tiếp; bỏ câu hoặc phần thì rubric bị xóa theo

#### Scenario 2 - Bài đã phát hành

- **Given** bài sở hữu rubric đã được phát hành
- **When** người dùng sửa hoặc xóa rubric
- **Then** hệ thống từ chối vì rubric đã khóa, để kết quả chấm không thay đổi

#### Scenario 2a - Rubric còn trống

- **Given** bài còn câu hoặc phần có rubric trống hoặc chưa hợp lệ
- **When** người dùng phát hành bài
- **Then** hệ thống từ chối và chỉ ra câu hoặc phần cần điền rubric

#### Scenario 3 - Nhân bản đề

- **Given** bài đã có rubric
- **When** bài được nhân bản, tạo version mới hoặc copy sang lớp khác
- **Then** rubric được nhân bản thành rubric mới, chưa khóa, của bài đích; hai bài không dùng chung rubric

### US-QBK-002 - Quản lý ngân hàng câu hỏi của môn

**Story**: Là Chủ nhiệm môn, tôi muốn tạo, sửa, tìm kiếm, nhập hàng loạt câu hỏi của mọi dạng bài (trắc nghiệm, tự luận, code, khung tài liệu) trên Question List và Question Detail để tôi và giảng viên dùng lại khi soạn bài và quiz.

**Truy vết**: FR-002, FR-016, FR-017, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Quản lý câu hỏi đúng phạm vi

- **Given** Chủ nhiệm môn được gán môn và câu hỏi/đáp án/điểm hợp lệ
- **When** người dùng tạo, sửa, tìm kiếm hoặc nhập tệp
- **Then** câu hỏi được lưu vào ngân hàng của môn, kết quả nhập báo theo dòng và không tạo bản ghi lỗi

#### Scenario 1a - Dùng lại câu hỏi đúng dạng bài

- **Given** ngân hàng của môn có câu `ACTIVE` thuộc nhiều dạng
- **When** giảng viên của một lớp thuộc môn hoặc Chủ nhiệm môn thêm câu từ ngân hàng vào quiz, Text Essay, Code Lab, hoặc lấy khung cho Diagram Essay/bài nhóm
- **Then** hệ thống chỉ cho chọn câu khớp dạng; quiz, Text Essay, Code Lab ghim đúng version đã chọn, còn Diagram Essay và bài nhóm sao khung của câu `DOCUMENT` vào bài; đáp án và lời giải mẫu không lộ cho sinh viên

#### Scenario 2 - Câu hỏi đã được dùng trong quiz

- **Given** câu hỏi đã thuộc một bài hoặc quiz được phát hành
- **When** Chủ nhiệm môn sửa câu hỏi
- **Then** hệ thống tạo version mới của câu hỏi; bài đã phát hành vẫn dùng version cũ và không bị thay đổi

#### Scenario 3 - Muốn đổi nội dung bài đã phát hành

- **Given** bài hoặc quiz đã phát hành cần thay đổi nội dung hoặc đáp án
- **When** người soạn thử sửa bài
- **Then** hệ thống không cho sửa version đang giao; sau khi ngưng giao hoặc bài đóng, người soạn sửa để tạo version mới, version cũ giữ nguyên cho lượt làm cũ, thao tác được audit

#### Scenario 4 - Chỉ Chủ nhiệm môn quản lý ngân hàng

- **Given** tài khoản được giao dạy lớp theo R3/R4 nhưng không quản lý môn, hoặc tài khoản Admin
- **When** tài khoản đó thử tạo, sửa hoặc xóa câu trong ngân hàng của môn
- **Then** hệ thống từ chối; giảng viên chỉ chọn được câu `ACTIVE` khi soạn bài hoặc quiz, không có ngân hàng của lớp

#### Scenario 5 - Xóa câu hỏi có tham chiếu

- **Given** câu Draft chưa được dùng hoặc version câu đã gắn với bài/lượt làm
- **When** Chủ nhiệm môn xóa câu
- **Then** Draft chưa dùng được xóa; câu có tham chiếu ngưng dùng cho lần chọn mới nhưng snapshot/lịch sử vẫn giữ

## 7. Miền AI-Assisted Authoring

### US-AIG-001 - Tạo bản nháp bài tập cho lớp bằng AI

**Story**: Là giảng viên, tôi muốn yêu cầu AI tạo câu hỏi/bài tập từ nội dung được phép của lớp để giảm thời gian soạn bài.

**Truy vết**: FR-002, FR-006, FR-012, FR-014, NFR-003, SEC-002, SEC-003, SEC-005, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Tạo bản nháp hợp lệ

- **Given** giảng viên được phân công lớp và chọn nguồn, loại, số lượng, độ khó hợp lệ
- **When** giảng viên gửi yêu cầu
- **Then** tác vụ được xử lý có trạng thái, chỉ dùng nguồn được phép và kết quả được lưu ở trạng thái bản nháp kèm nguồn/cấu hình

#### Scenario 2 - Prompt cố truy xuất ngoài phạm vi

- **Given** yêu cầu tham chiếu nội dung ngoài lớp được phân công
- **When** hệ thống xác thực phạm vi
- **Then** yêu cầu bị từ chối hoặc nguồn ngoài phạm vi bị loại bỏ trước khi gọi AI và sự kiện được ghi phù hợp

#### Scenario 3 - AI timeout hoặc vượt giới hạn

- **Given** provider chậm, lỗi hoặc giới hạn sử dụng/chi phí đã đạt
- **When** tác vụ thực thi
- **Then** không có bản nháp được đánh dấu hoàn tất giả, trạng thái lỗi an toàn được hiển thị và retry có giới hạn

#### Scenario 4 - AI soạn khung cho Diagram Essay hoặc bài nhóm

- **Given** bài `DRAFT` dạng Diagram Essay hoặc bài nhóm (không có câu) và nguồn học liệu đã xử lý trong phạm vi
- **When** giảng viên nhờ AI soạn khung, xem trước rồi xác nhận
- **Then** khung đề xuất (cây heading, hướng dẫn, gợi ý rubric từng phần, kèm nguồn, không có sơ đồ) thay khung hiện tại sau cảnh báo nếu khung đã có nội dung; phần được tính lại và giảng viên sửa gợi ý rubric trước khi tạo rubric từng phần

### US-AIG-002 - Tạo bản nháp bài/câu hỏi cấp môn bằng AI

**Story**: Là Chủ nhiệm môn, tôi muốn dùng AI tạo câu ngân hàng hoặc bản nháp quiz/bài của môn từ học liệu của môn. Text Essay/Code Lab dùng câu; Diagram Essay dùng khung kèm gợi ý rubric; không có bài nhóm cấp môn. Bài của môn được duyệt/phát hành cho mọi lớp OPEN với một lịch chung; giảng viên từng lớp chấm sinh viên lớp mình.

**Truy vết**: FR-002, FR-004, FR-006, FR-012, FR-014, NFR-003, SEC-002, SEC-003, SEC-005, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Tạo đề đúng môn

- **Given** Chủ nhiệm môn được gán môn và chọn nguồn đã xử lý thành công
- **When** gửi cấu hình tạo hợp lệ
- **Then** AI chỉ nhận nội dung của môn đó và tạo bản nháp có nguồn, cấu hình, actor và trạng thái duyệt

#### Scenario 2 - Nguồn chưa sẵn sàng hoặc sai môn

- **Given** nguồn RAG chưa xử lý xong hoặc thuộc môn khác
- **When** Chủ nhiệm môn yêu cầu tạo
- **Then** hệ thống không gọi AI với nguồn đó và giải thích hành động khắc phục an toàn

#### Scenario 3 - Dependency AI lỗi

- **Given** nhà cung cấp AI timeout hoặc không khả dụng
- **When** tác vụ chạy
- **Then** bản nháp cũ/nguồn không bị mất, tác vụ có trạng thái thất bại và retry tuân thủ giới hạn/backoff

### US-AIG-003 - Cấu hình và giám sát sử dụng AI

**Story**: Là quản trị viên, tôi muốn cấu hình giới hạn và giám sát việc sử dụng AI để kiểm soát chi phí, rủi ro và khả năng vận hành của nền tảng; mức tặng hằng tháng vẫn là cấu hình triển khai, còn gói bán do US-PAY-004 quản lý.

**Truy vết**: FR-012, FR-014, FR-021, NFR-003, SEC-002, SEC-003, SEC-005, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Cập nhật cấu hình an toàn

- **Given** quản trị viên có quyền và model/quota/giới hạn chi phí hợp lệ
- **When** quản trị viên cập nhật cấu hình hoặc kill-switch
- **Then** cấu hình mới có hiệu lực nhất quán, không khóa cứng nghiệp vụ vào một provider và thay đổi được audit

#### Scenario 2 - Xem trạng thái và chi phí

- **Given** có các lần gọi AI trong phạm vi thời gian được chọn
- **When** quản trị viên mở báo cáo vận hành
- **Then** hệ thống hiển thị số lượt, latency, lỗi, token/chi phí ước tính mà không lộ secret hoặc nội dung học tập ngoài nhu cầu

#### Scenario 3 - Vượt quota hoặc AI bị tắt

- **Given** quota/giới hạn đã đạt hoặc kill-switch đang bật
- **When** người dùng yêu cầu chức năng AI
- **Then** hệ thống từ chối trước khi gọi provider, giữ dữ liệu nghiệp vụ và giải thích phương án tiếp tục không dùng AI khi có thể

#### Scenario 4 - Mức tặng hằng tháng

- **Given** mức tặng tháng được nạp từ cấu hình triển khai
- **When** bắt đầu kỳ đặt lại của tài khoản
- **Then** ba vai trò Student, Teacher, Subject Manager nhận cùng mức tặng; thay cấu hình có hiệu lực từ kỳ sau, không sửa snapshot thanh toán; tạo/sửa gói bán dùng US-PAY-004

## 8. Miền Assessment Delivery

### US-ASM-001 - Duyệt và xuất bản bài đánh giá của lớp

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn chỉnh sửa, duyệt và xuất bản bản nháp bài tập cho lớp được phân công hoặc cho mọi lớp của môn mình quản lý để kiểm soát chất lượng trước khi giao.

**Truy vết**: FR-002, FR-006, FR-007, FR-014, SEC-002, SEC-003, SEC-005, SEC-006, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Xuất bản bản nháp đã duyệt

- **Given** bản nháp thuộc lớp được phân công và có cấu hình thời gian/số lần làm hợp lệ
- **When** giảng viên duyệt và xuất bản
- **Then** bài đánh giá khả dụng cho đúng lớp và quyết định được audit

#### Scenario 2 - Bỏ qua bước duyệt hoặc sai lớp

- **Given** bản nháp chưa được duyệt hoặc thuộc lớp ngoài phân công
- **When** giảng viên yêu cầu xuất bản
- **Then** hệ thống từ chối phía server và không giao bài cho người học

#### Scenario 3 - Xem danh sách bài Teacher

- **Given** tài khoản được giao lớp theo R3/R4
- **When** mở tab Evals của Teacher Class Detail
- **Then** hiển thị bài của lớp và bài của môn (chỉ đọc) với loại/chế độ/trạng thái, quiz của lớp và của môn, cùng lối vào Assignment Form, Quiz Detail phù hợp

#### Scenario 4 - Bài của môn

- **Given** Chủ nhiệm môn đã duyệt một bài Text Essay, Diagram Essay hoặc Code Lab của môn
- **When** Chủ nhiệm môn phát hành với một lịch
- **Then** bài hiện ở mọi lớp `OPEN` của môn theo lịch đó; giảng viên lớp không sửa hay ngưng giao được, chỉ chấm bài nộp của sinh viên lớp mình

### US-ASM-003 - Làm và nộp bài

**Story**: Là người học, tôi muốn làm và nộp bài đánh giá đang hiệu lực để hoàn thành yêu cầu học tập.

**Truy vết**: FR-002, FR-007, FR-014, NFR-002, SEC-002, SEC-003, SEC-006, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Nộp bài hợp lệ

- **Given** người học được ghi danh, bài đang hiệu lực và còn lượt làm
- **When** người học nộp bài hợp lệ loại trắc nghiệm, bài viết, bài tài liệu (có sơ đồ Draw.io) hoặc Code Lab
- **Then** hệ thống lưu nguyên vẹn bài nộp, thời điểm và lượt làm; trắc nghiệm và Code Lab được tự chấm ngay, loại khác chờ giảng viên chọn phương thức chấm

#### Scenario 2 - Quá hạn hoặc hết lượt

- **Given** đã qua hạn (và qua hạn nộp trễ nếu bài cho phép nộp trễ) hoặc người học hết lượt
- **When** người học nộp bài
- **Then** hệ thống từ chối phía server và không tạo bài nộp hợp lệ mới

#### Scenario 3 - Truy cập bài nộp người khác

- **Given** một bài nộp thuộc người học khác
- **When** người học thử đọc hoặc sửa bằng ID trực tiếp
- **Then** hệ thống từ chối và không tiết lộ nội dung hay trạng thái bài nộp

#### Scenario 4 - Lưu nháp và khôi phục

- **Given** người học đang làm bài và có thay đổi chưa nộp
- **When** autosave chạy hoặc người học mở lại bài sau gián đoạn
- **Then** bản nháp gần nhất của chính người học được lưu/khôi phục, hiển thị thời điểm lưu và không được tính là bài nộp

#### Scenario 5 - Xem lịch sử và nộp lại

- **Given** bài cho phép nhiều lượt và vẫn còn hiệu lực
- **When** người học xem lịch sử hoặc nộp lại
- **Then** từng attempt được giữ nguyên theo thời gian, lượt mới không ghi đè lịch sử và lượt được chấm được xác định rõ

#### Scenario 6 - Xem danh sách và chi tiết bài

- **Given** Student ghi danh và assignment được phép truy cập
- **When** mở Quiz Assignments, Codelab Assignments, Text Essay Assignments, Diagram Essay Assignments hoặc Group Essay Assignments rồi Assignment Detail
- **Then** danh sách đúng loại có deadline/mode/trạng thái/kết quả được phép; chi tiết trả hướng dẫn/câu hỏi hoặc khung/rubric/lượt còn lại, không lộ đáp án hay lời giải mẫu

### US-ASM-004 - Soạn và làm bài tài liệu có sơ đồ Draw.io (DOCUMENT)

**Story**: Là người học, tôi muốn làm bài tài liệu có sơ đồ vẽ trên canvas Draw.io nhúng trong web để giảng viên xem chính xác bài làm của tôi (XML đầy đủ lưu trong tài liệu).

**Truy vết**: FR-002, FR-006, FR-017, FR-014, SEC-002, SEC-003, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Vẽ và nộp XML đầy đủ

- **Given** người học được phép làm bài và canvas Draw.io đã tải cấu hình bài
- **When** người học vẽ, xem trước và nộp bài
- **Then** hệ thống xác thực và lưu nguyên vẹn XML đầy đủ làm bản nộp chuẩn, đồng thời hiển thị lại bản nộp nhất quán cho người học và giảng viên

#### Scenario 2 - XML không hợp lệ hoặc bị can thiệp

- **Given** XML sơ đồ vượt 2 MB, có DOCTYPE/external entity hoặc phần tử gốc không phải `mxfile`/`mxGraphModel`
- **When** người học lưu hoặc nộp
- **Then** hệ thống từ chối dữ liệu nguy hiểm, không xử lý external entity và giữ bản nháp hợp lệ gần nhất nếu có

#### Scenario 3 - Tạo bản rút gọn khi giảng viên yêu cầu AI chấm

- **Given** XML đầy đủ đã được nộp và giảng viên chọn “Nhờ AI đề xuất”
- **When** hệ thống chuẩn bị dữ liệu gửi AI
- **Then** hệ thống tạo XML rút gọn dẫn xuất theo allowlist chỉ cho lần gọi AI, giữ nguyên bản XML đầy đủ và không hiển thị bản rút gọn như bài nộp gốc

#### Scenario 4 - Người học nhập DOCX vào bài đang làm

- **Given** người học có lượt `DOCUMENT` đang làm và DOCX hợp lệ trong giới hạn
- **When** người học tải DOCX lên, xem trước các block được chuyển đổi rồi xác nhận
- **Then** hệ thống thêm các block với `origin = STUDENT` vào bản nháp hiện tại, không sửa hoặc xóa khung giảng viên; phần không hỗ trợ được báo rõ, lỗi nhập không làm mất bản nháp

### US-ASM-005 - Soạn và kiểm thử Code Lab

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn đưa câu Code Lab vào bài (lấy từ ngân hàng của môn hoặc viết riêng: đề bài, code khởi đầu, lời giải mẫu, test công khai/test ẩn) và chạy thử lời giải mẫu để xác nhận bài có thể chấm tự động.

**Truy vết**: FR-002, FR-006, FR-017, FR-014, NFR-003, SEC-002, SEC-003, SEC-005, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Chạy thử trong sandbox

- **Given** ngôn ngữ, giới hạn tài nguyên và test case hợp lệ
- **When** người dùng chạy lời giải mẫu
- **Then** mã chạy trong môi trường cô lập với timeout/tài nguyên hữu hạn và trả kết quả từng test phù hợp

#### Scenario 2 - Cấu hình không an toàn hoặc sandbox lỗi

- **Given** ngôn ngữ không cho phép, giới hạn không hợp lệ hoặc sandbox không khả dụng
- **When** người dùng kiểm thử hoặc phát hành
- **Then** hệ thống fail closed, không chạy mã trên application host và không đánh dấu bài sẵn sàng giả

### US-ASM-006 - Soạn và kiểm tra bài trắc nghiệm

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn soạn quiz luyện tập gắn với học liệu, với câu trắc nghiệm, đáp án, điểm và cài đặt quiz để hệ thống tự chấm nhất quán.

**Truy vết**: FR-002, FR-006, FR-016, FR-017, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Cấu hình hợp lệ

- **Given** câu hỏi, phương án, đáp án và điểm hợp lệ
- **When** người dùng xem trước hoặc duyệt bài
- **Then** tổng điểm/quy tắc chấm được kiểm tra và đáp án không hiển thị trong góc nhìn người học

#### Scenario 2 - Cấu hình thiếu hoặc mâu thuẫn

- **Given** câu hỏi thiếu đáp án, điểm không hợp lệ hoặc snapshot nguồn không tồn tại
- **When** người dùng yêu cầu phát hành
- **Then** hệ thống chặn phát hành và chỉ rõ mục cần sửa

### US-ASM-007 - Soạn bài viết luận

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn soạn bài viết luận với các câu lấy từ ngân hàng của môn hoặc viết riêng, mỗi câu tự có một rubric để điền, để đánh giá câu trả lời mở theo tiêu chí rõ ràng.

**Truy vết**: FR-002, FR-006, FR-016, FR-017, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Bài viết luận hợp lệ

- **Given** mỗi câu tự luận có rubric hợp lệ (bài viết là văn bản thường, không giới hạn số từ hay số dòng, không nộp tệp)
- **When** người dùng xem trước hoặc duyệt bài
- **Then** hệ thống hiển thị đúng hướng dẫn, tiêu chí và cấu hình nộp cho góc nhìn người học; điểm mỗi câu bằng tổng điểm rubric của câu

#### Scenario 2 - Nội dung dùng ngôn ngữ tự nhiên

- **Given** bài viết sử dụng bất kỳ ngôn ngữ tự nhiên nào
- **When** người dùng cấu hình bài
- **Then** hệ thống vẫn lưu dưới loại bài viết luận và ngôn ngữ chỉ là thuộc tính/cấu hình nếu cần

### US-ASM-008 - Nhân bản, sửa phiên bản và ngừng giao bài

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn nhân bản bài cũ thành bài mới, tạo version mới và ngừng nhận bài mới khi cần để tái sử dụng nội dung mà không sửa dữ liệu đã phát sinh.

**Truy vết**: FR-002, FR-007, FR-016, FR-028, FR-014, SEC-002, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Phiên bản và nhân bản

- **Given** người dùng có quyền với bài nguồn
- **When** người dùng sửa hoặc nhân bản
- **Then** sửa bài đã ngừng giao/đóng tạo version mới; nhân bản tạo định danh mới; không sao chép bài nộp/điểm; không có màn so sánh version (bỏ 2026-10-09)

#### Scenario 2 - Ngừng giao hoặc nhận bài mới

- **Given** bài đã phát hành và chính sách cho phép thu hồi
- **When** người dùng xác nhận ngừng giao/nhận bài mới
- **Then** bài biến mất khỏi danh sách cần làm hoặc khóa lượt nộp mới theo chính sách, nhưng cấu hình đã phát hành, bài nộp và điểm cũ vẫn chỉ đọc được để truy vết

### US-ASM-009 - Phát hành bài của môn cho mọi lớp

**Story**: Là Chủ nhiệm môn, tôi muốn soạn bài của môn thủ công hoặc nhờ AI, duyệt và phát hành một lần cho mọi lớp của môn với một lịch chung để các lớp làm cùng một bài.

**Truy vết**: FR-002, FR-007, FR-014, FR-016, FR-027, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Phát hành bài của môn

- **Given** Chủ nhiệm môn có quyền với môn và bài của môn đã duyệt
- **When** Chủ nhiệm môn phát hành với lịch hợp lệ
- **Then** bài hiện ở mọi lớp `OPEN` của môn, kể cả lớp mở sau đó; người học làm theo lịch chung

#### Scenario 2 - Sai phạm vi hoặc sửa từ lớp

- **Given** giảng viên của một lớp thuộc môn hoặc tài khoản không quản lý môn
- **When** người đó cố sửa, phát hành hoặc ngưng giao bài của môn
- **Then** hệ thống từ chối phía server và giữ nguyên bài

#### Scenario 3 - Chấm theo từng lớp

- **Given** sinh viên nhiều lớp đã nộp bài của môn
- **When** giảng viên từng lớp chấm và công bố
- **Then** mỗi giảng viên chỉ chấm và công bố cho sinh viên lớp mình; điểm vào sổ điểm của lớp đó

#### Scenario 4 - Xem bài đúng môn

- **Given** người quản lý môn có R2
- **When** mở Assignment List từ Manager Dashboard
- **Then** chỉ trả bài của môn được giao; không có dạng bài nhóm ở cấp môn

### US-ASM-010 - Copy assignment và rubric giữa các lớp

**Story**: Là giảng viên, tôi muốn copy assignment và rubric giữa các lớp mình phụ trách để tái sử dụng nội dung mà không mang theo dữ liệu thực thi cũ.

**Truy vết**: FR-002, FR-014, FR-016, FR-028, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Copy hợp lệ

- **Given** giảng viên được phân công cả lớp nguồn và lớp đích
- **When** giảng viên copy assignment (rubric đi theo bài)
- **Then** hệ thống tạo draft/identity độc lập ở lớp đích, giữ nguồn gốc audit và không copy lịch, attempt, bài nộp hoặc điểm

#### Scenario 2 - Lớp đích ngoài quyền

- **Given** giảng viên không được phân công lớp đích dù lớp đó thuộc cùng môn
- **When** yêu cầu copy được gửi
- **Then** hệ thống từ chối ở mức đối tượng và không tiết lộ nội dung lớp đích

### US-ASM-012 - Nộp bài Practice và nhận phản hồi AI khi đủ credit

**Story**: Là Student, tôi muốn nộp bài luyện tập Text Essay hoặc Diagram Essay và nhận một lần điểm/phản hồi AI cho mỗi attempt khi đủ credit để biết mình cần cải thiện gì.

**Truy vết**: FR-002, FR-007, FR-010, FR-017, FR-018, FR-030, SEC-002, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Đủ credit

- **Given** Student có quyền làm bài `PRACTICE` dạng Text Essay hoặc Diagram Essay và đủ credit
- **When** Student nộp attempt rồi bấm "Chấm với AI" trên bài đã nộp
- **Then** hệ thống giữ bài nộp, gọi AI chấm tối đa một kết quả hợp lệ cho attempt, tính credit theo token thực dùng và chỉ Student xem được kết quả luyện tập

#### Scenario 2 - Thiếu credit

- **Given** Student thiếu credit khi bấm chấm
- **When** Student bấm "Chấm với AI"
- **Then** hệ thống báo thiếu credit, không gọi AI, không trừ credit; bài vẫn chờ chấm và Student bấm lại được sau khi mua credit

#### Scenario 2a - Quá thời gian

- **Given** yêu cầu chấm đã gửi
- **When** quá 5 phút chưa có kết quả
- **Then** hệ thống báo lỗi, chốt lượng AI đã dùng thật nếu có và trả phần credit còn giữ (chưa dùng AI thì hoàn toàn bộ), cho Student bấm lại; không tính trùng kết quả đã hoàn tất

#### Scenario 3 - Làm lại

- **Given** Student còn lượt và đã nộp một attempt
- **When** Student làm lại và nộp attempt mới
- **Then** attempt mới có nút chấm riêng; retry kỹ thuật không tạo kết quả hoặc khoản trừ trùng

#### Scenario 4 - Các dạng luyện tập tự chấm

- **Given** bài `PRACTICE` dạng Code Lab hoặc Quiz
- **When** Student nộp bài
- **Then** hệ thống chấm theo test/đáp án mà không gọi AI hoặc trừ credit Student; kết quả không vào sổ điểm chính thức

## 9. Miền Grading and Progress

### US-GRD-001 - Nhận kết quả tự chấm câu hỏi xác định

**Story**: Là người học, tôi muốn câu hỏi có đáp án xác định được tự chấm nhất quán để nhận kết quả theo chính sách công bố.

**Truy vết**: FR-007, FR-008, FR-009, SEC-002, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Tự chấm sau nộp

- **Given** bài nộp hợp lệ chứa câu hỏi có đáp án xác định
- **When** người học nộp bài
- **Then** hệ thống tự chấm ngay theo đáp án/test đã xuất bản; nếu bài bật "hiện điểm ngay sau nộp" thì người học thấy điểm, không thì kết quả chờ giảng viên chốt và công bố; giảng viên vẫn sửa được kèm lý do

#### Scenario 2 - Không lộ kết quả sớm

- **Given** chính sách chưa cho phép công bố
- **When** người học xem bài nộp
- **Then** đáp án/điểm chưa được hiển thị trước thời điểm hoặc trạng thái cho phép

### US-GRD-002 - Nhận đề xuất chấm câu trả lời mở từ AI

**Story**: Là giảng viên, tôi muốn sau khi nhận bài có thể chọn AI đề xuất điểm và phản hồi để rút ngắn thời gian chấm mà không mất quyền quyết định.

**Truy vết**: FR-002, FR-008, FR-012, FR-014, NFR-003, SEC-002, SEC-003, SEC-005, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Giảng viên chủ động chọn chấm bằng AI

- **Given** giảng viên quản lý lớp, đã nhận bài nộp hợp lệ của bài có rubric
- **When** giảng viên chọn “Nhờ AI đề xuất” và tác vụ hoàn tất
- **Then** điểm/phản hồi được lưu là đề xuất chưa duyệt và không được công bố là quyết định cuối; nếu bài có sơ đồ Draw.io thì chỉ XML rút gọn dẫn xuất được gửi AI, còn XML đầy đủ vẫn là bản nộp chuẩn cho giảng viên

#### Scenario 1a - Chấm hàng loạt

- **Given** giảng viên chọn nhiều bài cần chấm theo rubric trong Grading Queue và đủ credit cho cả lô
- **When** bấm "Chấm hàng loạt bằng AI"
- **Then** hệ thống tạo đề xuất cho từng bài ở nền; xong thì giảng viên mở Grading Workspace xác nhận từng bài, dùng nút ‹ › để chuyển bài và lưu xong tự sang bài kế; thiếu credit cho cả lô thì báo và không chạy

#### Scenario 2 - Giảng viên chọn chấm thủ công

- **Given** giảng viên đã nhận bài nộp hợp lệ
- **When** giảng viên chọn chấm thủ công
- **Then** hệ thống không gửi bài nộp tới AI và cung cấp checklist rubric cùng ô phản hồi

#### Scenario 3 - AI lỗi

- **Given** AI timeout, lỗi hoặc đạt giới hạn sử dụng
- **When** tác vụ chấm chạy
- **Then** bài nộp không mất, không có điểm cuối giả và giảng viên có thể chấm thủ công hoặc thử lại có kiểm soát

#### Scenario 4 - Ngoài lớp phân công

- **Given** bài nộp thuộc lớp khác
- **When** giảng viên yêu cầu AI chấm
- **Then** hệ thống từ chối trước khi gửi dữ liệu tới provider

### US-GRD-003 - Chấm thủ công, duyệt và ghi đè đề xuất AI

**Story**: Là giảng viên, tôi muốn chấm thủ công hoặc duyệt/ghi đè đề xuất AI và lưu thành điểm nháp để chịu trách nhiệm cho quyết định học thuật trước khi chốt và công bố (US-GRD-005).

**Truy vết**: FR-002, FR-008, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Chấp nhận đề xuất

- **Given** đề xuất AI chưa duyệt thuộc lớp được phân công
- **When** giảng viên chấp nhận đề xuất và lưu
- **Then** kết quả trở thành điểm nháp do giảng viên quyết định, lưu actor/thời gian (như chấm tay) và người học chưa thấy điểm cho đến khi được chốt và công bố

#### Scenario 2 - Ghi đè đề xuất

- **Given** giảng viên không đồng ý với đề xuất
- **When** nhập điểm khác và (nếu muốn) sửa phần giải thích của AI rồi lưu
- **Then** điểm nháp được cập nhật mà không cần ghi lý do, đề xuất gốc vẫn truy vết được và audit ghi người thực hiện, thời gian

#### Scenario 3 - Chấm hoàn toàn thủ công

- **Given** giảng viên đã chọn chấm thủ công cho bài nộp thuộc lớp được phân công
- **When** nhập điểm/phản hồi hợp lệ và lưu
- **Then** điểm nháp được lưu với actor/thời gian và không có lời gọi AI nào cho bài nộp đó

#### Scenario 4 - Thao túng điểm ngoài quyền

- **Given** người dùng không quản lý lớp hoặc không có quyền chấm
- **When** gửi yêu cầu thay đổi điểm
- **Then** hệ thống từ chối phía server và ghi sự kiện vi phạm

### US-GRD-004 - Xem sổ điểm theo quyền

**Story**: Là người dùng, tôi muốn xem điểm, phản hồi và trạng thái bài nộp đúng phạm vi vai trò để theo dõi kết quả mà không lộ dữ liệu ngoài quyền.

**Truy vết**: FR-002, FR-005, FR-009, NFR-002, SEC-005, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Người học xem dữ liệu cá nhân

- **Given** kết quả đã được công bố
- **When** người học mở Student Assignments của lớp (UC 22)
- **Then** cạnh từng bài chỉ hiện điểm, phản hồi và trạng thái bài nộp của chính người học

#### Scenario 2 - Giảng viên xem lớp

- **Given** giảng viên được phân công lớp
- **When** mở sổ điểm lớp
- **Then** hệ thống liệt kê sinh viên của lớp, mỗi sinh viên là một mục đóng/mở chứa điểm và trạng thái các bài, không có điểm tổng

#### Scenario 3 - Vai trò kế thừa phải có phân công lớp

- **Given** Subject Manager hoặc Administrator
- **When** mở gradebook lớp
- **Then** chỉ được xem khi chính tài khoản được giao dạy lớp theo R4; chỉ quản lý môn mà không được giao dạy lớp, hoặc mọi tài khoản Admin, bị từ chối

### US-GRD-005 - Chốt và công bố điểm từng bài hoặc hàng loạt

**Story**: Là giảng viên, tôi muốn chốt điểm nháp đã chấm (US-GRD-003) cho từng bài hoặc hàng loạt trong lớp được phân công rồi công bố để người học nhận kết quả nhất quán và có kiểm soát.

**Truy vết**: FR-002, FR-008, FR-009, FR-014, FR-020, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Chốt các bài đủ điều kiện

- **Given** giảng viên quản lý lớp và danh sách gồm các bài đã có điểm/phản hồi hợp lệ
- **When** giảng viên xác nhận chốt hàng loạt
- **Then** chỉ bài đủ điều kiện chuyển thành điểm cuối, kết quả theo từng bài được trả và mỗi thay đổi được audit

#### Scenario 2 - Một phần dữ liệu không hợp lệ

- **Given** danh sách có bài ngoài lớp, thiếu điểm hoặc đã bị thay đổi đồng thời
- **When** yêu cầu được xử lý
- **Then** hệ thống không chốt nhầm, báo rõ từng mục thất bại và không mở rộng quyền từ các mục hợp lệ

#### Scenario 3 - Chốt và công bố một bài

- **Given** giảng viên quản lý lớp và một bài có điểm nháp hợp lệ
- **When** giảng viên chốt rồi công bố bài đó, hoặc chọn nhiều bài đã chốt và bấm "Công bố hàng loạt"
- **Then** điểm trở thành điểm cuối, người học đúng bài thấy điểm và phản hồi theo chính sách, đề xuất AI không bị lộ và audit ghi actor/thời gian

## 10. Miền Reporting and Analytics

### US-RPT-001 - Theo dõi tiến độ nộp bài và nhắc nhở

**Story**: Là giảng viên, tôi muốn theo dõi trạng thái nộp bài, để hệ thống tự nhắc người học chưa nộp trước hạn và cho người học xem phân bố điểm ẩn danh của lớp, giúp họ hoàn thành đúng hạn và tự đánh giá kết quả.

**Truy vết**: FR-002, FR-009, FR-011, FR-019, FR-024, NFR-002, SEC-005, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Xem tiến độ đúng lớp

- **Given** giảng viên được phân công và bài đã giao
- **When** mở báo cáo tiến độ
- **Then** hệ thống phân biệt đã nộp, chưa nộp, đang làm, nộp trễ và thời gian còn lại

#### Scenario 2 - Tự nhắc trước hạn

- **Given** bài còn 24 giờ tới hạn và có người học chưa nộp
- **When** tới thời điểm nhắc
- **Then** hệ thống gửi một lần thông báo trong app và email (nếu người học không tắt) cho đúng người chưa nộp; bài đã ngừng giao không được nhắc

#### Scenario 3 - Phân bố điểm ẩn danh trên danh sách bài Student

- **Given** Chủ nhiệm môn của môn đã bật hiển thị phân bố điểm trong Edit Class Information (UC 52), bài GRADED đủ điều kiện riêng tư và điểm đã công bố theo lớp
- **When** người học mở Student Assignments của lớp (UC 22)
- **Then** bài GRADED có điểm đã công bố hiện phân bố theo khoảng điểm tổng hợp, ẩn khoảng quá ít người; quiz/Practice không có phân bố; không suy ra danh tính hoặc điểm của người học khác; Teacher chỉ được giao dạy không có quyền bật/tắt cài đặt này

### US-RPT-002 - Thống kê hệ thống cho quản trị viên

**Story**: Là quản trị viên, tôi muốn thấy thống kê ngay trên Admin Dashboard (UC 58) để nắm quy mô người dùng, môn, lớp và ghi danh của hệ thống.

**Truy vết**: FR-002, FR-024, NFR-002, SEC-005, SEC-002, SEC-003.

**Acceptance criteria**

#### Scenario 1 - Xem số liệu tổng hợp

- **Given** quản trị viên đã đăng nhập
- **When** vào Admin Dashboard (UC 58)
- **Then** hệ thống hiển thị số tài khoản theo vai trò và trạng thái (`PENDING`, `ACTIVE`, `DISABLED`), số môn theo trạng thái, số lớp theo trạng thái (`DRAFT`, `OPEN`, `ARCHIVED`) và số ghi danh `ACTIVE`, tính tại thời điểm mở, chỉ là số đếm, không có tên/email

#### Scenario 2 - Từ chối vai trò khác

- **Given** người dùng không có vai trò `ADMIN`
- **When** gọi API thống kê, kể cả gọi trực tiếp
- **Then** hệ thống từ chối theo quyền và không trả số liệu nào

### US-RPT-003 - Xuất bảng điểm

**Story**: Là Teacher hoặc Subject Manager được giao dạy lớp theo R4, tôi muốn xuất bảng điểm theo lớp/bài để phục vụ lưu trữ và xử lý nghiệp vụ ngoài hệ thống.

**Truy vết**: FR-002, FR-009, FR-024, SEC-005, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Xuất đúng phạm vi

- **Given** tài khoản có phân công dạy lớp theo R3/R4
- **When** yêu cầu xuất Excel/CSV
- **Then** tệp chứa trạng thái, điểm đã chốt, thời gian nộp và phản hồi đúng phạm vi; mục chưa nộp/chưa chốt được ghi rõ

#### Scenario 2 - Ngăn xuất ngoài quyền

- **Given** bộ lọc hoặc ID tham chiếu lớp ngoài quyền
- **When** người dùng yêu cầu xuất
- **Then** hệ thống từ chối trước khi tạo tệp và không rò rỉ dữ liệu qua tên tệp/metadata

## 11. Miền Payment and AI Credit

### US-PAY-001 - Bắt đầu thanh toán an toàn

**Story**: Là Student, Teacher hoặc Subject Manager, tôi muốn bắt đầu thanh toán qua nhà cung cấp để mua credit AI mà nền tảng không lưu dữ liệu thẻ thô.

**Truy vết**: FR-010, FR-014, NFR-002, SEC-005, SEC-002, SEC-003, SEC-006, SEC-007, REL-003.

**Acceptance criteria**

#### Scenario 1 - Tạo giao dịch

- **Given** tài khoản `ACTIVE` có vai trò Student, Teacher hoặc Subject Manager và gói credit hợp lệ
- **When** người dùng bấm Thanh toán trên Credit Package Checkout
- **Then** hệ thống tạo giao dịch nội bộ duy nhất và chuyển sang luồng provider mà không thu/lưu dữ liệu thẻ thô

#### Scenario 3 - Student không được dùng AI ngoài bài Practice hợp lệ

- **Given** tài khoản chỉ có vai trò Student đã đăng nhập
- **When** tài khoản yêu cầu AI tạo đề, xử lý học liệu hoặc chấm bài `GRADED`, kể cả bằng cách gọi trực tiếp API
- **Then** hệ thống từ chối theo quyền, không giữ/trừ credit và không gọi AI; Student vẫn được xem ví, mua credit và dùng cho bài `PRACTICE` Text Essay/Diagram Essay của chính mình

#### Scenario 2 - Provider lỗi

- **Given** provider timeout hoặc từ chối tạo giao dịch
- **When** yêu cầu được xử lý
- **Then** giao dịch không bị đánh dấu đã thanh toán, credit không được cộng và người dùng nhận trạng thái an toàn có thể thử lại

#### Scenario 4 - Xem gói credit

- **Given** tài khoản `ACTIVE` có ví (Student, Teacher, Subject Manager)
- **When** mở Public Credit Packages hoặc My Credit Package từ Class Dashboard
- **Then** hiển thị các gói đang bán, giá và số credit do Administrator cấu hình, cùng số dư và lịch sử của chính tài khoản; tài khoản Admin không có ví

### US-PAY-002 - Nhận credit AI sau xác nhận thanh toán

**Story**: Là Student, Teacher hoặc Subject Manager đã thanh toán, tôi muốn credit AI chỉ được cộng sau xác nhận hợp lệ để trạng thái mua hàng chính xác.

**Truy vết**: FR-010, FR-014, SEC-002, SEC-003, SEC-005, SEC-006, SEC-007, REL-003.

**Acceptance criteria**

#### Scenario 1 - Webhook hợp lệ

- **Given** giao dịch đang chờ và webhook có chữ ký/trạng thái hợp lệ
- **When** backend xử lý sự kiện
- **Then** trạng thái được đối soát, credit được cộng đúng một lần và sự kiện được audit

#### Scenario 2 - Webhook trùng lặp

- **Given** sự kiện đã được xử lý
- **When** cùng định danh webhook được gửi lại
- **Then** hệ thống trả kết quả idempotent, không cộng trùng credit hoặc tạo giao dịch bổ sung

#### Scenario 3 - Webhook sai chữ ký, replay hoặc thất bại

- **Given** chữ ký không hợp lệ, sự kiện replay không được phép hoặc trạng thái thanh toán thất bại
- **When** backend nhận webhook
- **Then** hệ thống fail closed, không cộng credit và ghi sự kiện bảo mật phù hợp

#### Scenario 4 - Tự xác minh khi thiếu webhook

- **Given** giao dịch còn chờ hoặc đã hết hạn gần đây nhưng chưa được xác nhận
- **When** job định kỳ tra trạng thái giao dịch qua API PayOS
- **Then** giao dịch đã trả được ghi nhận và cộng credit đúng một lần; trạng thái hủy/hết hạn được cập nhật phù hợp

#### Scenario 5 - PayOS không khả dụng khi tự xác minh

- **Given** PayOS không trả lời hoặc trả dữ liệu không xác minh được
- **When** job định kỳ tra trạng thái giao dịch
- **Then** hệ thống giữ nguyên trạng thái, không cộng credit và thử lại ở lần job sau

#### Scenario 6 - Xem kết quả thanh toán của chính mình

- **Given** tài khoản sở hữu giao dịch
- **When** PayOS quay về Credit Package Checkout
- **Then** màn hiện trạng thái pending/success/cancelled/failed đã xác minh; redirect không tự cộng credit và ID giao dịch người khác bị từ chối

### US-PAY-004 - Quản trị gói credit

**Story**: Là Administrator, tôi muốn xem, thêm và sửa gói credit để quản lý thông tin bán, giá và số credit.

**Truy vết**: FR-002, FR-010, FR-014, FR-031, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Xem/thêm/sửa gói hợp lệ

- **Given** Administrator đã xác thực
- **When** mở Credit Package List rồi thêm/sửa tên, thông tin, giá dương, credit nguyên dương và trạng thái bán trong popup Credit Package Detail
- **Then** gói được lưu với kiểm version, audit actor/thời gian/trước-sau; Public Credit Packages dùng cấu hình mới cho giao dịch tạo sau đó

#### Scenario 2 - Bảo toàn giao dịch đã tạo

- **Given** gói đã có giao dịch pending hoặc PAID
- **When** Administrator đổi thông tin/giá/credit
- **Then** giao dịch cũ giữ snapshot, không đổi số tiền hoặc credit sẽ/đã cấp, không điều chỉnh số dư thủ công

#### Scenario 3 - Sai quyền hoặc dữ liệu

- **Given** vai trò khác hoặc dữ liệu/version không hợp lệ
- **When** gọi API thêm/sửa
- **Then** backend từ chối, không thay gói hoặc giao dịch; UC này không cấp quyền xóa gói; mức tặng tháng đổi trên Settings

### US-PAY-005 - Xem lịch sử thanh toán nền tảng

**Story**: Là Administrator, tôi muốn xem và lọc lịch sử mua credit toàn nền tảng để theo dõi giao dịch.

**Truy vết**: FR-002, FR-010, FR-014, FR-032, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Xem lịch sử theo bộ lọc

- **Given** Administrator đã xác thực
- **When** mở Payment History và lọc tài khoản/gói/thời gian/trạng thái
- **Then** trả danh sách phân trang với tài khoản mua, snapshot gói, số tiền, thời gian và trạng thái; không lộ secret/dữ liệu thẻ

#### Scenario 2 - Chỉ đọc và đúng quyền

- **Given** vai trò khác hoặc một thao tác định sửa giao dịch/credit
- **When** gọi chức năng lịch sử toàn nền tảng
- **Then** bị từ chối; đọc của Administrator không đổi status, không cộng/trừ credit và không đối soát thủ công

## 12. Miền Notification, Audit and Settings

### US-NTF-001 - Nhận thông báo thiết yếu

**Story**: Là người dùng, tôi muốn nhận thông báo về tài khoản, ghi danh, giao bài và kết quả để không bỏ lỡ hành động quan trọng.

**Truy vết**: FR-011, NFR-002, NFR-003, SEC-005, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Xếp gửi thông báo

- **Given** một sự kiện thuộc nhóm kích hoạt/khôi phục tài khoản, ghi danh, giao bài hoặc công bố kết quả
- **When** giao dịch nghiệp vụ chính hoàn tất
- **Then** thông báo đúng người nhận được xếp gửi mà không làm lộ dữ liệu người khác

#### Scenario 2 - Email provider lỗi

- **Given** provider timeout hoặc không khả dụng
- **When** gửi thông báo
- **Then** giao dịch nghiệp vụ chính không bị rollback sai, trạng thái gửi được ghi và retry hữu hạn/backoff không tạo thông báo trùng ngoài chính sách

#### Scenario 3 - Xem và đánh dấu đã đọc

- **Given** tài khoản đã xác thực
- **When** xem notifications trên Navigation Bar, mở đối tượng liên quan hoặc đánh dấu đã đọc
- **Then** chỉ trả/sửa notification của chính tài khoản, mở đối tượng vẫn kiểm quyền hiện hành

### US-AUD-001 - Tra cứu audit nghiệp vụ và bảo mật

**Story**: Là quản trị viên, tôi muốn tra cứu sự kiện audit theo phạm vi và thời gian để điều tra thay đổi nhạy cảm mà không thể sửa lịch sử.

**Truy vết**: FR-002, FR-014, SEC-005, SEC-002, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Tra cứu được phép

- **Given** quản trị viên có quyền audit
- **When** lọc theo actor, loại sự kiện, đối tượng hoặc thời gian
- **Then** hệ thống trả các sự kiện được phép gồm timestamp/correlation/actor/kết quả mà không lộ secret hoặc dữ liệu nhạy cảm

#### Scenario 2 - Không sửa hoặc xóa audit

- **Given** bất kỳ người dùng ứng dụng nào, kể cả quản trị viên
- **When** cố sửa hoặc xóa sự kiện audit qua chức năng ứng dụng
- **Then** thao tác bị từ chối và lịch sử vẫn nguyên vẹn

#### Scenario 3 - Sự kiện bắt buộc

- **Given** đăng nhập thất bại, thay đổi role/phạm vi môn, thay đổi nội dung/điểm, phát hành bài lớp/môn hoặc quiz, thanh toán, thay đổi cài đặt hệ thống (Settings) hoặc truy cập đặc quyền
- **When** hành động hoàn tất hoặc bị từ chối
- **Then** sự kiện tương ứng được ghi với actor, thời gian, đối tượng và kết quả phù hợp

### US-SET-001 - Quản lý cài đặt hệ thống

**Story**: Là quản trị viên, tôi muốn xem và chỉnh cài đặt AI, credit tặng định kỳ và giới hạn tệp tải lên trong giới hạn cho phép để vận hành nền tảng mà không phải triển khai lại.

**Truy vết**: FR-033, FR-013, FR-014, FR-021, SEC-002, SEC-005.

**Acceptance criteria**

#### Scenario 1 - Xem cài đặt

- **Given** quản trị viên đã xác thực
- **When** mở Setting List
- **Then** thấy các mục theo nhóm AI, Credit, Tệp với giá trị hiện hành, người sửa và thời điểm sửa gần nhất; không thấy bí mật như API key

#### Scenario 2 - Sửa cài đặt hợp lệ

- **Given** quản trị viên mở Setting Detail của một mục
- **When** nhập giá trị trong giới hạn và lưu với version hiện tại
- **Then** giá trị mới được lưu, audit giá trị trước/sau và có hiệu lực trong tối đa 30 giây

#### Scenario 3 - Giá trị không hợp lệ hoặc bị sửa đồng thời

- **Given** giá trị ngoài giới hạn hoặc version đã cũ
- **When** quản trị viên lưu
- **Then** hệ thống từ chối, không thay đổi gì và báo lý do hoặc yêu cầu tải lại

#### Scenario 4 - Người không phải quản trị viên

- **Given** tài khoản không phải Administrator
- **When** gọi xem hoặc sửa cài đặt
- **Then** hệ thống từ chối phía server và ghi audit

## 13. Ma trận bao phủ yêu cầu chức năng

| Requirement | Stories chính |
|---|---|
| FR-001 | US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-006 |
| FR-002 | Quy tắc kiểm quyền xuyên suốt 51 story MVP; chi tiết actor và phạm vi nằm trong từng story/UC |
| FR-003 | US-IAM-005, US-CAT-001 đến US-CAT-003 |
| FR-004 | US-CNT-001, US-CNT-002, US-CNT-005, US-AIG-002 |
| FR-005 | US-LRN-001 |
| FR-006 | US-AIG-001, US-AIG-002, US-ASM-001, US-ASM-004 đến US-ASM-007 |
| FR-007 | US-ASM-001, US-ASM-003, US-ASM-008, US-ASM-012 |
| FR-008 | US-GRD-001 đến US-GRD-005 |
| FR-009 | US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003 |
| FR-010 | US-PAY-001, US-PAY-002 |
| FR-011 | US-IAM-001, US-IAM-003, US-CAT-003, US-CNT-004, US-RPT-001, US-NTF-001 |
| FR-012 | US-CNT-001, US-AIG-001, US-AIG-002, US-AIG-003, US-GRD-002 |
| FR-013 | US-CNT-001, US-CNT-002, US-LRN-001 |
| FR-014 | US-IAM-002, US-IAM-005, US-CAT-001, US-CAT-002, US-AIG-001, US-AIG-002, US-ASM-001, US-ASM-003, US-GRD-002, US-GRD-003, US-PAY-001, US-PAY-002, US-AUD-001 |
| FR-015 | US-IAM-007 |
| FR-016 | US-QBK-001, US-QBK-002, US-ASM-006, US-ASM-007, US-ASM-009, US-ASM-010 |
| FR-017 | US-QBK-002, US-ASM-003 đến US-ASM-007 |
| FR-018 | US-ASM-003 |
| FR-019 | US-RPT-001 |
| FR-020 | US-GRD-005 |
| FR-021 | US-AIG-003 |
| FR-022 | Đã rút khỏi phạm vi (2026-10-09) |
| FR-023 | US-CNT-004 |
| FR-024 | US-RPT-001, US-RPT-002, US-RPT-003 |
| FR-025 | US-GRP-001, US-GRP-002 |
| FR-026 | US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006 |
| FR-027 | US-ASM-009 |
| FR-028 | US-ASM-010 |
| FR-029 | Đã rút khỏi phạm vi; `US-ASM-011` chỉ còn là mã lịch sử |
| FR-030 | US-ASM-012, US-PAY-001, US-PAY-002 |
| FR-031 | US-PAY-004 |
| FR-032 | US-PAY-005 |
| FR-033 | US-SET-001 |

## 14. Story ↔ use case hiện hành

Tên/ID theo [73 UC](../../../docs/use-cases-73.md). Primary unit là owner của UC; các story trong hàng có thể do unit khác đóng góp. Primary story ownership theo [unit map](../application-design/unit-of-work-story-map.md).

| UC | Use case | Stories liên quan | Primary unit |
|---|---|---|---|
| 01 | Activate Account | US-IAM-001 | U01 |
| 02 | Login | US-IAM-002 | U01 |
| 03 | Logout | US-IAM-002 | U01 |
| 04 | Reset Password | US-IAM-003 | U01 |
| 05 | Change Password | US-IAM-006 | U01 |
| 06 | View Profile Information | US-IAM-004 | U01 |
| 07 | Update Profile Information | US-IAM-004 | U01 |
| 08 | View My Credit Package | US-PAY-001, US-PAY-002 | U07 |
| 09 | View Public Credit Package | US-PAY-001 | U07 |
| 10 | Purchase Credit Package | US-PAY-001, US-PAY-002 | U07 |
| 11 | View Payment Result | US-PAY-002 | U07 |
| 12 | View Notifications | US-NTF-001 | U16 |
| 13 | View Enrolled Classes | US-LRN-001 | U04 |
| 14 | View Enrolled Class Detail | US-LRN-001 | U04 |
| 15 | View Learning Material | US-CNT-001, US-CNT-002, US-CNT-005 | U05 |
| 16 | View My Group | US-GRP-001 | U12 |
| 17 | Request Leader Change | US-GRP-002 | U12 |
| 18 | View Quiz Practice History | US-ASM-006, US-ASM-003 | U11 |
| 19 | View Quiz Practice Detail | US-ASM-006, US-ASM-003 | U11 |
| 20 | Take Quiz | US-ASM-006, US-ASM-003 | U11 |
| 21 | View Quiz Result | US-ASM-006, US-GRD-001 | U11 |
| 22 | View Student Assignment List | US-ASM-003, US-RPT-001 | U11 |
| 23 | View Assignment Detail | US-ASM-003 | U11 |
| 24 | Complete Essay Assignment | US-ASM-003, US-ASM-007 | U11 |
| 25 | Complete Code Lab | US-ASM-003, US-ASM-005 | U11 |
| 26 | Complete Diagram Assignment | US-ASM-003, US-ASM-004 | U11 |
| 27 | Complete Group Assignment | US-GRP-004, US-GRP-005 | U14 |
| 28 | View Submission History | US-ASM-003, US-ASM-012 | U11 |
| 29 | Grade Practice Assignment | US-ASM-012 | U11 |
| 30 | View Class Announcements | US-CNT-004 | U05 |
| 31 | View Assigned Class List | US-CAT-002 | U04 |
| 32 | View Assigned Class Detail | US-CAT-002, US-GRP-001 | U04 |
| 33 | View Uploaded Learning Materials | US-CNT-002 | U05 |
| 34 | Add/Update/Delete Learning Material | US-CNT-002, US-CNT-005 | U05 |
| 35 | Create/Update/Delete Quiz | US-ASM-006, US-ASM-001, US-AIG-001, US-AIG-002 | U09 |
| 36 | Create/Update/Delete Announcement | US-CNT-004 | U05 |
| 37 | View Student Submissions | US-GRD-004, US-RPT-001 | U15 |
| 38 | Grade Submission With AI | US-GRD-002, US-GRP-006 | U15 |
| 39 | Grade Submissions Manually | US-GRD-003, US-GRD-005, US-GRP-006 | U15 |
| 40 | View/Export GradeBook | US-GRD-004, US-RPT-003 | U15 |
| 41 | View Teacher Assignment List | US-ASM-001, US-ASM-008, US-ASM-009 | U08 |
| 42 | Create/Update/Delete Essay | US-ASM-007, US-ASM-001, US-ASM-008, US-ASM-010, US-QBK-001, US-QBK-002, US-AIG-001, US-ASM-009, US-AIG-002 | U09 |
| 43 | Create/Update/Delete Code Lab | US-ASM-005, US-ASM-001, US-ASM-008, US-ASM-010, US-QBK-001, US-QBK-002, US-AIG-001, US-ASM-009, US-AIG-002 | U09 |
| 44 | Create/Update/Delete Diagram Assignment | US-ASM-004, US-ASM-001, US-ASM-008, US-ASM-010, US-QBK-001, US-QBK-002, US-AIG-001, US-ASM-009, US-AIG-002 | U09 |
| 45 | Create/Update/Delete Group Assignment | US-GRP-003, US-ASM-001, US-ASM-008, US-ASM-010, US-QBK-001, US-QBK-002, US-AIG-001 | U09 |
| 46 | Add/Update/Delete Rubric | US-QBK-001 | U06 |
| 47 | View Managed Subject Classes | US-CAT-002 | U04 |
| 48 | View Managed Class Detail | US-CAT-002 | U04 |
| 49 | Create Class | US-CAT-001 | U04 |
| 50 | Assign Teacher To Class | US-CAT-001 | U04 |
| 51 | Add/Remove Class Students | US-CAT-003 | U04 |
| 52 | Edit Class Information | US-CAT-002, US-RPT-001 | U04 |
| 53 | View Managed Subject | US-CAT-001, US-ASM-009 | U04 |
| 54 | View Subject Materials | US-CNT-001 | U05 |
| 55 | Add/Update/Delete Subject Material | US-CNT-001, US-CNT-005 | U05 |
| 56 | View Subject Question Bank | US-QBK-002 | U06 |
| 57 | Create/Update/Delete Subject Question | US-QBK-002, US-AIG-002 | U06 |
| 58 | View Admin Dashboard | US-RPT-002, US-AIG-003 | U16 |
| 59 | View Account List | US-IAM-007 | U01 |
| 60 | Add Account | US-IAM-001, US-IAM-007 | U01 |
| 61 | View Account Detail | US-IAM-007 | U01 |
| 62 | Update Account Information | US-IAM-005, US-IAM-007 | U01 |
| 63 | Change Account Status | US-IAM-007 | U01 |
| 64 | View Subject List | US-CAT-001 | U04 |
| 65 | Add Subject | US-CAT-001 | U04 |
| 66 | View Subject Detail | US-CAT-001 | U04 |
| 67 | Update Subject Information | US-CAT-001, US-IAM-005 | U04 |
| 68 | View Credit Package Setting | US-PAY-004 | U07 |
| 69 | Add/Edit Credit Package | US-PAY-004 | U07 |
| 70 | View Settings | US-SET-001, US-AIG-003 | U03 |
| 71 | Edit Setting | US-SET-001, US-AIG-003 | U03 |
| 72 | View Payment History | US-PAY-005 | U07 |
| 73 | View Audit Log | US-AUD-001 | U02 |

## 15. Ràng buộc phi chức năng và kỹ thuật downstream

| Requirement | Xử lý tại User Stories | Stage xác minh chi tiết |
|---|---|---|
| NFR-001 | Không tạo system story; mọi story giả định web Next.js và API Spring Boot | NFR Requirements, Application Design, Code Generation |
| NFR-002 | Các luồng UI cốt lõi yêu cầu responsive, keyboard/accessibility labels và lỗi có hành động khắc phục | Functional Design, Code Generation, Build and Test |
| NFR-003 | Stories file/AI/payment/email có trạng thái, timeout và retry hữu hạn; API thường giữ mục tiêu p95 | NFR Design, Infrastructure Design, Build and Test |
| NFR-004 | Acceptance criteria là đầu vào cho unit, integration, system và e2e test; adapter/webhook cần contract test | Code Generation, Build and Test |
| NFR-005 | Không tạo system story; container, secret và version pinning là tiêu chí triển khai | Infrastructure Design, Code Generation, Build and Test |
| SEC-001 đến SEC-007 | Được gắn trên stories có hành vi quan sát được; phạm vi rút gọn cho đồ án | NFR Design, Infrastructure Design, Code Generation, Build and Test |
| REL-001 đến REL-004 | Timeout và fail-closed gắn vào story tích hợp; topology, DR, monitoring và incident process ngoài phạm vi đồ án | Application Design, NFR Design, Infrastructure Design, Build and Test |

## 16. Kiểm tra INVEST

| Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|
| Independent | Đạt | Mỗi story có một kết quả nghiệp vụ chính; quan hệ phụ thuộc được thể hiện bằng trạng thái Given thay vì gộp luồng lớn |
| Negotiable | Đạt | Stories mô tả giá trị/hành vi, không khóa nhà cung cấp, database hoặc kiến trúc triển khai |
| Valuable | Đạt | Mỗi story gắn với một trong bốn persona và nêu lợi ích rõ ràng |
| Estimable | Đạt | Phạm vi được giới hạn theo một thao tác hoặc kết quả quan sát được |
| Small | Đạt | Các hành trình lớn được tách theo kích hoạt, nội dung, tạo AI, phát hành, nộp, chấm và công bố |
| Testable | Đạt | Cả 51 story MVP có acceptance criteria Given/When/Then và truy vết requirements |

## 17. Security Compliance tại User Stories

> Bảng này lập trước khi rút gọn phạm vi (2026-09-24). Hiện chỉ SECURITY-03, 04, 05, 08, 09, 12, 15 và RESILIENCY-04, 06, 10 còn áp dụng; các rule khác là N/A "ngoài phạm vi đồ án", kể cả những dòng ghi "downstream" (xem `requirements.md` mục 12-13).

| Rule | Trạng thái | Áp dụng/N/A |
|---|---|---|
| SECURITY-01 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-02 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-03 | Compliant | US-AUD-001 và các failure scenario cấm log dữ liệu nhạy cảm |
| SECURITY-04 | N/A | HTTP security headers không tạo giá trị persona riêng; giữ cho thiết kế/code/test |
| SECURITY-05 | Compliant | Input/file/config/payment scenarios yêu cầu validation, giới hạn và lỗi an toàn |
| SECURITY-06 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-07 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-08 | Compliant | Object/function authorization được thể hiện xuyên IAM, môn, lớp, nhóm/leader, nội dung, bài nộp, điểm và payment |
| SECURITY-09 | Compliant | Failure scenarios yêu cầu fail closed và safe error; hardening chi tiết downstream |
| SECURITY-10 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-11 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-12 | Compliant | US-IAM-001/002/003 bao phủ password, session, brute-force; MFA admin giữ downstream |
| SECURITY-13 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-14 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-15 | Compliant | External failure scenarios yêu cầu fail closed, không mất dữ liệu và không lộ nội bộ |

Không có blocking security finding tại User Stories.

## 18. Resiliency Compliance tại User Stories

| Rule | Trạng thái | Áp dụng/N/A |
|---|---|---|
| RESILIENCY-01 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-02 | N/A | Ngoài phạm vi đồ án (không RTO/RPO) |
| RESILIENCY-03 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-04 | N/A | CI/CD/rollback là ràng buộc construction/infrastructure |
| RESILIENCY-05 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-06 | N/A | Health checks thuộc NFR/Infrastructure Design |
| RESILIENCY-07 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-08 | N/A | Ngoài phạm vi đồ án (không multi-zone) |
| RESILIENCY-09 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-10 | Compliant | File, AI, payment và email scenarios yêu cầu timeout, retry hữu hạn và degraded/fail-safe behavior |
| RESILIENCY-11 | N/A | Ngoài phạm vi đồ án (không DR) |
| RESILIENCY-12 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-13 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-14 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-15 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |

Không có blocking resiliency finding tại User Stories. Sau khi rút gọn phạm vi (2026-09-24), chỉ RESILIENCY-04, 06, 10 còn áp dụng; các mục N/A khác không còn là ràng buộc downstream.
