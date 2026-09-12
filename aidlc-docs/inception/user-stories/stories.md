# User Stories - AI-Powered Learning Platform

## 1. Quy ước

- Stories được nhóm theo miền nghiệp vụ và sắp theo hành trình trong từng miền.
- Mã story có dạng `US-{DOMAIN}-{NNN}`.
- Mỗi story là một lát cắt nhỏ theo giá trị người dùng.
- Acceptance criteria dùng Given/When/Then.
- Lỗi tích hợp được mô tả trong acceptance criteria của story nghiệp vụ liên quan.
- Mỗi story ghi mã requirements liên quan; ràng buộc kỹ thuật thuần túy được giữ trong ma trận downstream thay vì tạo system story.

## 2. Miền Identity and Access

### US-IAM-001 - Nhận và kích hoạt tài khoản

**Story**: Là người học, tôi muốn đăng ký hoặc kích hoạt tài khoản được cấp để có thể bắt đầu sử dụng nền tảng.

**Truy vết**: FR-001, FR-011, NFR-002, SEC-002, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Kích hoạt thành công

- **Given** một lời mời còn hiệu lực hoặc đăng ký hợp lệ
- **When** người học cung cấp dữ liệu bắt buộc và mật khẩu đạt chính sách
- **Then** tài khoản được kích hoạt và người học nhận xác nhận an toàn

#### Scenario 2 - Dữ liệu không hợp lệ

- **Given** dữ liệu đầu vào thiếu, sai định dạng hoặc mật khẩu không đạt chính sách
- **When** người học gửi yêu cầu
- **Then** hệ thống từ chối, chỉ rõ cách khắc phục và không lộ chi tiết nội bộ

### US-IAM-002 - Đăng nhập và đăng xuất an toàn

**Story**: Là người dùng, tôi muốn đăng nhập và đăng xuất an toàn để chỉ mình tôi sử dụng phiên đã xác thực.

**Truy vết**: FR-001, FR-002, NFR-002, SEC-002, SEC-003, SEC-005, SEC-007.

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
- **Then** phiên bị vô hiệu phía server và không thể tiếp tục gọi API được bảo vệ

### US-IAM-003 - Khôi phục mật khẩu riêng tư

**Story**: Là người dùng quên mật khẩu, tôi muốn yêu cầu khôi phục mà không làm lộ trạng thái tài khoản để lấy lại quyền truy cập an toàn.

**Truy vết**: FR-001, FR-011, SEC-002, SEC-003, SEC-007, REL-007.

**Acceptance criteria**

#### Scenario 1 - Yêu cầu khôi phục

- **Given** một địa chỉ email bất kỳ
- **When** người dùng yêu cầu khôi phục
- **Then** hệ thống trả cùng một thông báo dù tài khoản có tồn tại hay không và chỉ gửi liên kết hữu hạn nếu phù hợp

#### Scenario 2 - Email tạm thời lỗi

- **Given** yêu cầu hợp lệ nhưng nhà cung cấp email không khả dụng
- **When** hệ thống thử gửi thông báo
- **Then** hệ thống không lộ tài khoản hoặc lỗi nội bộ, ghi nhận trạng thái có thể xử lý lại với retry hữu hạn

### US-IAM-004 - Quản lý hồ sơ cá nhân

**Story**: Là người dùng, tôi muốn xem và cập nhật thông tin hồ sơ tối thiểu của mình để dữ liệu tài khoản luôn chính xác.

**Truy vết**: FR-001, FR-002, NFR-002, SEC-001, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Cập nhật hồ sơ của chính mình

- **Given** người dùng đã xác thực
- **When** người dùng gửi dữ liệu hồ sơ hợp lệ
- **Then** chỉ hồ sơ của người đó được cập nhật và dữ liệu nhạy cảm không xuất hiện trong log

#### Scenario 2 - Cố sửa hồ sơ người khác

- **Given** người dùng không có quyền quản trị đối tượng đích
- **When** người dùng gửi định danh của người khác
- **Then** hệ thống từ chối phía server mà không tiết lộ dữ liệu của đối tượng

### US-IAM-005 - Quản lý vai trò và phạm vi môn

**Story**: Là quản trị viên, tôi muốn gán hoặc thu hồi vai trò và phạm vi môn để người dùng chỉ có đúng quyền cần thiết.

**Truy vết**: FR-002, FR-003, FR-014, SEC-003, SEC-005, SEC-008.

**Acceptance criteria**

#### Scenario 1 - Gán Chủ nhiệm môn

- **Given** quản trị viên đã xác thực và một môn hợp lệ
- **When** quản trị viên gán vai trò Chủ nhiệm môn cùng phạm vi môn
- **Then** quyền cấp môn chỉ có hiệu lực với môn đó và thay đổi được ghi actor, thời gian, giá trị trước/sau

#### Scenario 2 - Thu hồi quyền

- **Given** một quyền hoặc phạm vi đang có hiệu lực
- **When** quản trị viên thu hồi
- **Then** các request mới không còn được phép và phiên/quyền cache liên quan được cập nhật an toàn

#### Scenario 3 - Người không phải quản trị viên thay đổi quyền

- **Given** người dùng không có quyền quản trị
- **When** người đó gọi chức năng phân vai
- **Then** hệ thống từ chối phía server và ghi sự kiện vi phạm authorization

## 3. Miền Academic Structure and Content

### US-CAT-001 - Quản lý cấu trúc môn và lớp

**Story**: Là quản trị viên, tôi muốn tạo môn, tạo lớp thuộc môn và phân công vai trò để cấu trúc học thuật phản ánh đúng hoạt động đào tạo.

**Truy vết**: FR-002, FR-003, FR-014, SEC-003, SEC-005, SEC-008.

**Acceptance criteria**

#### Scenario 1 - Tạo cấu trúc hợp lệ

- **Given** quản trị viên và các tài khoản được phân công hợp lệ
- **When** quản trị viên tạo môn, lớp và gán giảng viên/Chủ nhiệm môn
- **Then** mỗi lớp thuộc đúng một môn, các phân công được lưu và thay đổi quan trọng được audit

#### Scenario 2 - Tham chiếu không hợp lệ

- **Given** môn, lớp hoặc người dùng không tồn tại/không phù hợp vai trò
- **When** quản trị viên gửi cấu hình
- **Then** hệ thống từ chối toàn bộ thay đổi không hợp lệ và trả hướng dẫn khắc phục an toàn

### US-CAT-002 - Quản lý vòng đời lớp/khóa học

**Story**: Là giảng viên, tôi muốn tạo, sửa, xuất bản và lưu trữ lớp/khóa học được phân công để kiểm soát nội dung người học nhìn thấy.

**Truy vết**: FR-002, FR-003, FR-014, NFR-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Xuất bản nội dung

- **Given** giảng viên được phân công và nội dung hợp lệ
- **When** giảng viên xuất bản
- **Then** người học được ghi danh có thể thấy nội dung và thay đổi được audit

#### Scenario 2 - Nội dung nháp hoặc đã lưu trữ

- **Given** nội dung chưa xuất bản hoặc lớp đã lưu trữ
- **When** người học yêu cầu truy cập
- **Then** nội dung không được hiển thị trừ khi có quy tắc truy cập đã được cấp rõ ràng

#### Scenario 3 - Ngoài phạm vi phân công

- **Given** giảng viên không được phân công lớp
- **When** giảng viên thử sửa vòng đời lớp
- **Then** hệ thống từ chối ở mức đối tượng và ghi sự kiện phù hợp

### US-CAT-003 - Ghi danh người học

**Story**: Là giảng viên hoặc quản trị viên được phép, tôi muốn ghi danh người học vào lớp để họ nhận đúng nội dung và bài tập.

**Truy vết**: FR-002, FR-003, FR-011, FR-014, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Ghi danh hợp lệ

- **Given** lớp đang hoạt động và người thực hiện có quyền
- **When** người học được ghi danh
- **Then** quyền truy cập lớp được tạo một lần và thông báo ghi danh được xếp gửi

#### Scenario 2 - Ghi danh trùng hoặc ngoài quyền

- **Given** người học đã được ghi danh hoặc người thực hiện không quản lý lớp
- **When** yêu cầu được gửi
- **Then** hệ thống không tạo bản ghi trùng, không mở rộng quyền và trả kết quả an toàn

### US-CNT-001 - Quản lý kho học liệu và RAG cấp môn

**Story**: Là Chủ nhiệm môn, tôi muốn soạn hoặc tải học liệu vào kho cấp môn và theo dõi xử lý RAG để mọi lớp dùng chung nguồn đã kiểm soát.

**Truy vết**: FR-002, FR-004, FR-012, FR-013, FR-014, NFR-003, SEC-001, SEC-003, SEC-005, SEC-007, REL-007.

**Acceptance criteria**

#### Scenario 1 - Tải tài liệu hợp lệ

- **Given** Chủ nhiệm môn được gán môn và tệp PDF/DOCX/slide nằm trong giới hạn
- **When** tài liệu được tải lên
- **Then** tệp được lưu riêng tư, gắn đúng môn và trạng thái chuyển qua chờ/đang xử lý/thành công hoặc thất bại

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

### US-CNT-002 - Quản lý nội dung riêng của lớp

**Story**: Là giảng viên, tôi muốn soạn hoặc tải nội dung riêng cho lớp được phân công để bổ sung học liệu phù hợp với lớp mình.

**Truy vết**: FR-002, FR-003, FR-004, FR-013, FR-014, NFR-003, SEC-001, SEC-003, SEC-005, SEC-007, REL-007.

**Acceptance criteria**

#### Scenario 1 - Quản lý nội dung lớp

- **Given** giảng viên được phân công lớp
- **When** giảng viên tạo hoặc tải nội dung hợp lệ
- **Then** nội dung được gắn đúng lớp, lưu riêng tư và có trạng thái xử lý quan sát được

#### Scenario 2 - Không sửa kho cấp môn

- **Given** giảng viên không có vai trò Chủ nhiệm môn của môn tương ứng
- **When** giảng viên thử sửa tài nguyên cấp môn
- **Then** hệ thống từ chối phía server và giữ nguyên tài nguyên

#### Scenario 3 - Storage lỗi

- **Given** dịch vụ lưu trữ tạm thời không khả dụng
- **When** giảng viên tải tệp
- **Then** hệ thống không tạo nội dung ở trạng thái thành công giả, trả trạng thái an toàn và cho phép thử lại có kiểm soát

## 4. Miền Learning Journey

### US-LRN-001 - Truy cập lớp đã ghi danh

**Story**: Là người học, tôi muốn xem cấu trúc và nội dung đã xuất bản của lớp được ghi danh để học đúng chương trình.

**Truy vết**: FR-002, FR-003, FR-005, FR-013, NFR-002, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Truy cập hợp lệ

- **Given** người học được ghi danh và nội dung đã xuất bản
- **When** người học mở lớp
- **Then** hệ thống hiển thị nội dung cấp môn/lớp được phép và cấp quyền tệp ngắn hạn khi cần

#### Scenario 2 - Không ghi danh hoặc nội dung nháp

- **Given** người học không thuộc lớp hoặc nội dung chưa xuất bản
- **When** người học dùng URL/ID trực tiếp
- **Then** hệ thống từ chối mà không tiết lộ nội dung hoặc metadata nhạy cảm

### US-LRN-002 - Lưu tiến độ và tiếp tục học

**Story**: Là người học, tôi muốn đánh dấu hoàn thành và tiếp tục từ vị trí gần nhất để duy trì tiến độ qua nhiều phiên.

**Truy vết**: FR-005, FR-009, NFR-002, NFR-003, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Cập nhật tiến độ

- **Given** người học đang xem một đơn vị nội dung được phép
- **When** người học đánh dấu hoàn thành hoặc lưu vị trí
- **Then** tiến độ của chính người học và đúng đơn vị nội dung được cập nhật

#### Scenario 2 - Tiếp tục phiên sau

- **Given** đã có vị trí học hợp lệ
- **When** người học quay lại lớp
- **Then** hệ thống cho phép tiếp tục từ vị trí gần nhất và không hiển thị tiến độ của người khác

### US-LRN-003 - Theo dõi tiến độ lớp

**Story**: Là giảng viên, tôi muốn xem tiến độ tổng hợp và chi tiết phù hợp của lớp được phân công để hỗ trợ người học kịp thời.

**Truy vết**: FR-002, FR-005, FR-009, NFR-002, SEC-001, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Xem lớp được phân công

- **Given** giảng viên được phân công lớp
- **When** giảng viên mở báo cáo tiến độ
- **Then** hệ thống hiển thị dữ liệu của người học trong lớp đó theo quyền

#### Scenario 2 - Xem lớp khác

- **Given** giảng viên không được phân công lớp đích
- **When** giảng viên dùng bộ lọc hoặc ID trực tiếp
- **Then** hệ thống từ chối và không trả dữ liệu tổng hợp hay chi tiết

## 5. Miền AI-Assisted Authoring

### US-AIG-001 - Tạo bản nháp bài tập cho lớp bằng AI

**Story**: Là giảng viên, tôi muốn yêu cầu AI tạo câu hỏi/bài tập từ nội dung được phép của lớp để giảm thời gian soạn bài.

**Truy vết**: FR-002, FR-006, FR-012, FR-014, NFR-003, SEC-003, SEC-005, SEC-007, REL-007.

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

### US-AIG-002 - Tạo bản nháp đề chung cấp môn bằng AI

**Story**: Là Chủ nhiệm môn, tôi muốn dùng AI tạo đề từ kho học liệu/RAG của môn để chuẩn bị đánh giá thống nhất cho các lớp.

**Truy vết**: FR-002, FR-004, FR-006, FR-012, FR-014, NFR-003, SEC-003, SEC-005, SEC-007, REL-007.

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

## 6. Miền Assessment Delivery

### US-ASM-001 - Duyệt và xuất bản bài đánh giá của lớp

**Story**: Là giảng viên, tôi muốn chỉnh sửa, duyệt và xuất bản bản nháp đánh giá cho lớp được phân công để kiểm soát chất lượng trước khi giao.

**Truy vết**: FR-002, FR-006, FR-007, FR-014, SEC-003, SEC-005, SEC-007, SEC-008.

**Acceptance criteria**

#### Scenario 1 - Xuất bản bản nháp đã duyệt

- **Given** bản nháp thuộc lớp được phân công và có cấu hình thời gian/số lần làm hợp lệ
- **When** giảng viên duyệt và xuất bản
- **Then** bài đánh giá khả dụng cho đúng lớp và quyết định được audit

#### Scenario 2 - Bỏ qua bước duyệt hoặc sai lớp

- **Given** bản nháp chưa được duyệt hoặc thuộc lớp ngoài phân công
- **When** giảng viên yêu cầu xuất bản
- **Then** hệ thống từ chối phía server và không giao bài cho người học

### US-ASM-002 - Phát hành đề chung cho mọi lớp thuộc môn

**Story**: Là Chủ nhiệm môn, tôi muốn duyệt và phát hành trực tiếp đề chung tới mọi lớp thuộc môn được phân công để bảo đảm đánh giá thống nhất.

**Truy vết**: FR-002, FR-003, FR-006, FR-007, FR-014, SEC-003, SEC-005, SEC-007, SEC-008.

**Acceptance criteria**

#### Scenario 1 - Phát hành đúng phạm vi

- **Given** bản nháp đã duyệt và Chủ nhiệm môn được gán môn
- **When** Chủ nhiệm môn phát hành đề chung
- **Then** đề được giao một lần tới tất cả lớp hiện hành thuộc môn mà không cần giảng viên lớp duyệt lại

#### Scenario 2 - Không lan sang môn khác

- **Given** có lớp thuộc môn ngoài phạm vi của Chủ nhiệm môn
- **When** đề chung được phát hành
- **Then** lớp ngoài phạm vi không nhận đề và không thể truy cập bằng ID trực tiếp

#### Scenario 3 - Truy vết phát hành

- **Given** một lần phát hành thành công hoặc bị từ chối
- **When** audit được ghi
- **Then** audit chứa actor, môn, tập lớp đích, thời gian và kết quả nhưng không chứa dữ liệu nhạy cảm

### US-ASM-003 - Làm và nộp bài

**Story**: Là người học, tôi muốn làm và nộp bài đánh giá đang hiệu lực để hoàn thành yêu cầu học tập.

**Truy vết**: FR-002, FR-007, FR-014, NFR-002, SEC-003, SEC-007, SEC-008.

**Acceptance criteria**

#### Scenario 1 - Nộp bài hợp lệ

- **Given** người học được ghi danh, bài đang hiệu lực và còn lượt làm
- **When** người học nộp câu trả lời hợp lệ
- **Then** hệ thống lưu bài nộp, thời điểm, lượt làm và trạng thái chấm một cách nguyên vẹn

#### Scenario 2 - Quá hạn hoặc hết lượt

- **Given** đã qua hạn hoặc người học hết lượt
- **When** người học nộp bài
- **Then** hệ thống từ chối phía server và không tạo bài nộp hợp lệ mới

#### Scenario 3 - Truy cập bài nộp người khác

- **Given** một bài nộp thuộc người học khác
- **When** người học thử đọc hoặc sửa bằng ID trực tiếp
- **Then** hệ thống từ chối và không tiết lộ nội dung hay trạng thái bài nộp

## 7. Miền Grading and Progress

### US-GRD-001 - Nhận kết quả tự chấm câu hỏi xác định

**Story**: Là người học, tôi muốn câu hỏi có đáp án xác định được tự chấm nhất quán để nhận kết quả theo chính sách công bố.

**Truy vết**: FR-007, FR-008, FR-009, SEC-003, SEC-008.

**Acceptance criteria**

#### Scenario 1 - Tự chấm sau nộp

- **Given** bài nộp hợp lệ chứa câu hỏi có đáp án xác định
- **When** hệ thống chấm
- **Then** điểm được tính theo đáp án/rule đã xuất bản và lưu phương thức chấm cùng trạng thái

#### Scenario 2 - Không lộ kết quả sớm

- **Given** chính sách chưa cho phép công bố
- **When** người học xem bài nộp
- **Then** đáp án/điểm chưa được hiển thị trước thời điểm hoặc trạng thái cho phép

### US-GRD-002 - Nhận đề xuất chấm câu trả lời mở từ AI

**Story**: Là giảng viên, tôi muốn AI đề xuất điểm và phản hồi cho câu trả lời mở để rút ngắn thời gian chấm mà không mất quyền quyết định.

**Truy vết**: FR-002, FR-008, FR-012, FR-014, NFR-003, SEC-003, SEC-005, SEC-007, REL-007.

**Acceptance criteria**

#### Scenario 1 - Tạo đề xuất

- **Given** giảng viên quản lý lớp, bài nộp hợp lệ và rubric đã được chọn
- **When** yêu cầu chấm AI hoàn tất
- **Then** điểm/phản hồi được lưu là đề xuất chưa duyệt, kèm phương thức và không được công bố là quyết định cuối

#### Scenario 2 - AI lỗi

- **Given** AI timeout, lỗi hoặc đạt giới hạn sử dụng
- **When** tác vụ chấm chạy
- **Then** bài nộp không mất, không có điểm cuối giả và giảng viên có thể chấm thủ công hoặc thử lại có kiểm soát

#### Scenario 3 - Ngoài lớp phân công

- **Given** bài nộp thuộc lớp khác
- **When** giảng viên yêu cầu AI chấm
- **Then** hệ thống từ chối trước khi gửi dữ liệu tới provider

### US-GRD-003 - Duyệt, ghi đè và công bố điểm

**Story**: Là giảng viên, tôi muốn duyệt hoặc ghi đè đề xuất AI và công bố kết quả để chịu trách nhiệm cho quyết định học thuật cuối cùng.

**Truy vết**: FR-002, FR-008, FR-009, FR-014, SEC-003, SEC-005, SEC-008.

**Acceptance criteria**

#### Scenario 1 - Chấp nhận đề xuất

- **Given** đề xuất AI chưa duyệt thuộc lớp được phân công
- **When** giảng viên chấp nhận và công bố
- **Then** kết quả trở thành điểm cuối, lưu actor/thời gian/phương thức và hiển thị theo chính sách

#### Scenario 2 - Ghi đè đề xuất

- **Given** giảng viên không đồng ý với đề xuất
- **When** nhập điểm/phản hồi mới cùng lý do và công bố
- **Then** điểm cuối được cập nhật, đề xuất gốc vẫn truy vết được và audit ghi người thực hiện, thời gian, lý do

#### Scenario 3 - Thao túng điểm ngoài quyền

- **Given** người dùng không quản lý lớp hoặc không có quyền chấm
- **When** gửi yêu cầu thay đổi điểm
- **Then** hệ thống từ chối phía server và ghi sự kiện vi phạm

### US-GRD-004 - Xem sổ điểm theo quyền

**Story**: Là người dùng, tôi muốn xem điểm và tiến độ đúng phạm vi vai trò để theo dõi kết quả mà không lộ dữ liệu ngoài quyền.

**Truy vết**: FR-002, FR-005, FR-009, NFR-002, SEC-001, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Người học xem dữ liệu cá nhân

- **Given** kết quả đã được công bố
- **When** người học mở sổ điểm
- **Then** chỉ điểm, phản hồi và tiến độ của chính người học được hiển thị

#### Scenario 2 - Giảng viên xem lớp

- **Given** giảng viên được phân công lớp
- **When** mở sổ điểm lớp
- **Then** hệ thống hiển thị tổng hợp và chi tiết cần thiết của lớp đó

#### Scenario 3 - Quản trị viên xem theo quyền

- **Given** quản trị viên có quyền báo cáo phù hợp
- **When** yêu cầu dữ liệu theo phạm vi quản trị
- **Then** hệ thống trả đúng phạm vi, không mở quyền sửa điểm nếu chưa được cấp riêng

## 8. Miền Payment and Entitlement

### US-PAY-001 - Bắt đầu thanh toán an toàn

**Story**: Là người dùng, tôi muốn bắt đầu thanh toán qua nhà cung cấp để mua quyền truy cập mà nền tảng không lưu dữ liệu thẻ thô.

**Truy vết**: FR-010, FR-014, NFR-002, SEC-001, SEC-003, SEC-007, SEC-008, REL-007.

**Acceptance criteria**

#### Scenario 1 - Tạo giao dịch

- **Given** người dùng đã xác thực và sản phẩm/quyền lợi hợp lệ
- **When** người dùng bắt đầu thanh toán
- **Then** hệ thống tạo giao dịch nội bộ duy nhất và chuyển sang luồng provider mà không thu/lưu dữ liệu thẻ thô

#### Scenario 2 - Provider lỗi

- **Given** provider timeout hoặc từ chối tạo giao dịch
- **When** yêu cầu được xử lý
- **Then** giao dịch không bị đánh dấu đã thanh toán, quyền truy cập không được cấp và người dùng nhận trạng thái an toàn có thể thử lại

### US-PAY-002 - Nhận quyền sau xác nhận thanh toán

**Story**: Là người dùng đã thanh toán, tôi muốn quyền truy cập chỉ được cấp sau xác nhận hợp lệ để trạng thái mua hàng chính xác.

**Truy vết**: FR-010, FR-014, SEC-003, SEC-005, SEC-007, SEC-008, REL-007.

**Acceptance criteria**

#### Scenario 1 - Webhook hợp lệ

- **Given** giao dịch đang chờ và webhook có chữ ký/trạng thái hợp lệ
- **When** backend xử lý sự kiện
- **Then** trạng thái được đối soát, quyền truy cập được cấp đúng một lần và sự kiện được audit

#### Scenario 2 - Webhook trùng lặp

- **Given** sự kiện đã được xử lý
- **When** cùng định danh webhook được gửi lại
- **Then** hệ thống trả kết quả idempotent, không cấp trùng quyền hoặc tạo giao dịch bổ sung

#### Scenario 3 - Webhook sai chữ ký, replay hoặc thất bại

- **Given** chữ ký không hợp lệ, sự kiện replay không được phép hoặc trạng thái thanh toán thất bại
- **When** backend nhận webhook
- **Then** hệ thống fail closed, không cấp quyền và ghi sự kiện bảo mật phù hợp

### US-PAY-003 - Đối soát trạng thái thanh toán

**Story**: Là quản trị viên, tôi muốn đối soát giao dịch với nhà cung cấp để xử lý trạng thái chờ hoặc sai lệch mà không cấp quyền nhầm.

**Truy vết**: FR-002, FR-010, FR-014, SEC-003, SEC-005, SEC-007, SEC-008, REL-007.

**Acceptance criteria**

#### Scenario 1 - Đối soát giao dịch

- **Given** quản trị viên có quyền và giao dịch cần kiểm tra
- **When** hệ thống lấy trạng thái từ provider trong timeout
- **Then** kết quả được so sánh, cập nhật idempotent theo rule và ghi audit

#### Scenario 2 - Không thể đối soát

- **Given** provider không khả dụng hoặc trả dữ liệu không xác minh được
- **When** đối soát chạy
- **Then** trạng thái hiện tại không được nâng lên thành công, quyền không được cấp và lỗi có thể retry được ghi an toàn

## 9. Miền Notification and Audit

### US-NTF-001 - Nhận thông báo thiết yếu

**Story**: Là người dùng, tôi muốn nhận thông báo về tài khoản, ghi danh, giao bài và kết quả để không bỏ lỡ hành động quan trọng.

**Truy vết**: FR-011, NFR-002, NFR-003, SEC-001, SEC-007, REL-007.

**Acceptance criteria**

#### Scenario 1 - Xếp gửi thông báo

- **Given** một sự kiện thuộc nhóm kích hoạt/khôi phục tài khoản, ghi danh, giao bài hoặc công bố kết quả
- **When** giao dịch nghiệp vụ chính hoàn tất
- **Then** thông báo đúng người nhận được xếp gửi mà không làm lộ dữ liệu người khác

#### Scenario 2 - Email provider lỗi

- **Given** provider timeout hoặc không khả dụng
- **When** gửi thông báo
- **Then** giao dịch nghiệp vụ chính không bị rollback sai, trạng thái gửi được ghi và retry hữu hạn/backoff không tạo thông báo trùng ngoài chính sách

### US-AUD-001 - Tra cứu audit nghiệp vụ và bảo mật

**Story**: Là quản trị viên, tôi muốn tra cứu sự kiện audit theo phạm vi và thời gian để điều tra thay đổi nhạy cảm mà không thể sửa lịch sử.

**Truy vết**: FR-002, FR-014, SEC-001, SEC-003, SEC-005, SEC-008.

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

- **Given** đăng nhập thất bại, thay đổi role/phạm vi môn, thay đổi nội dung/điểm, phát hành đề chung, thanh toán hoặc truy cập đặc quyền
- **When** hành động hoàn tất hoặc bị từ chối
- **Then** sự kiện tương ứng được ghi với actor, thời gian, đối tượng và kết quả phù hợp

## 10. Ma trận bao phủ yêu cầu chức năng

| Requirement | Stories chính |
|---|---|
| FR-001 | US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004 |
| FR-002 | US-IAM-002, US-IAM-004, US-IAM-005, US-CAT-001 đến US-AUD-001 theo phạm vi actor |
| FR-003 | US-IAM-005, US-CAT-001, US-CAT-002, US-CAT-003, US-ASM-002 |
| FR-004 | US-CNT-001, US-CNT-002, US-AIG-002 |
| FR-005 | US-LRN-001, US-LRN-002, US-LRN-003, US-GRD-004 |
| FR-006 | US-AIG-001, US-AIG-002, US-ASM-001, US-ASM-002 |
| FR-007 | US-ASM-001, US-ASM-002, US-ASM-003 |
| FR-008 | US-GRD-001, US-GRD-002, US-GRD-003 |
| FR-009 | US-LRN-002, US-LRN-003, US-GRD-004 |
| FR-010 | US-PAY-001, US-PAY-002, US-PAY-003 |
| FR-011 | US-IAM-001, US-IAM-003, US-CAT-003, US-NTF-001 |
| FR-012 | US-CNT-001, US-AIG-001, US-AIG-002, US-GRD-002 |
| FR-013 | US-CNT-001, US-CNT-002, US-LRN-001 |
| FR-014 | US-IAM-002, US-IAM-005, US-CAT-001, US-CAT-002, US-AIG-001, US-AIG-002, US-ASM-001, US-ASM-002, US-ASM-003, US-GRD-002, US-GRD-003, US-PAY-001, US-PAY-002, US-PAY-003, US-AUD-001 |

## 11. Ràng buộc phi chức năng và kỹ thuật downstream

| Requirement | Xử lý tại User Stories | Stage xác minh chi tiết |
|---|---|---|
| NFR-001 | Không tạo system story; mọi story giả định web Next.js và API Spring Boot | NFR Requirements, Application Design, Code Generation |
| NFR-002 | Các luồng UI cốt lõi yêu cầu responsive, keyboard/accessibility labels và lỗi có hành động khắc phục | Functional Design, Code Generation, Build and Test |
| NFR-003 | Stories file/AI/payment/email có trạng thái, timeout và retry hữu hạn; API thường giữ mục tiêu p95 | NFR Design, Infrastructure Design, Build and Test |
| NFR-004 | Acceptance criteria là đầu vào cho unit, integration, system và e2e test; adapter/webhook cần contract test | Code Generation, Build and Test |
| NFR-005 | Không tạo system story; container, secret và version pinning là tiêu chí triển khai | Infrastructure Design, Code Generation, Build and Test |
| SEC-001 đến SEC-009 | Được gắn trên stories có hành vi quan sát được; control hạ tầng/chuỗi cung ứng giữ downstream | NFR Design, Infrastructure Design, Code Generation, Build and Test |
| REL-001 đến REL-010 | Failure/degraded behavior gắn vào story tích hợp; topology, DR, observability và incident process giữ downstream | Application Design, NFR Design, Infrastructure Design, Build and Test |

## 12. Kiểm tra INVEST

| Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|
| Independent | Đạt | Mỗi story có một kết quả nghiệp vụ chính; quan hệ phụ thuộc được thể hiện bằng trạng thái Given thay vì gộp luồng lớn |
| Negotiable | Đạt | Stories mô tả giá trị/hành vi, không khóa nhà cung cấp, database hoặc kiến trúc triển khai |
| Valuable | Đạt | Mỗi story gắn với một trong bốn persona và nêu lợi ích rõ ràng |
| Estimable | Đạt | Phạm vi được giới hạn theo một thao tác hoặc kết quả quan sát được |
| Small | Đạt | Các hành trình lớn được tách theo kích hoạt, nội dung, tạo AI, phát hành, nộp, chấm và công bố |
| Testable | Đạt | Tất cả 27 stories có acceptance criteria Given/When/Then và truy vết requirements |

## 13. Security Compliance tại User Stories

| Rule | Trạng thái | Áp dụng/N/A |
|---|---|---|
| SECURITY-01 | Compliant | Stories về hồ sơ, học liệu, điểm và thanh toán yêu cầu bảo vệ dữ liệu; mã hóa chi tiết downstream |
| SECURITY-02 | N/A | Network access logging là control hạ tầng, đã truy vết downstream |
| SECURITY-03 | Compliant | US-AUD-001 và các failure scenario cấm log dữ liệu nhạy cảm |
| SECURITY-04 | N/A | HTTP security headers không tạo giá trị persona riêng; giữ cho thiết kế/code/test |
| SECURITY-05 | Compliant | Input/file/config/payment scenarios yêu cầu validation, giới hạn và lỗi an toàn |
| SECURITY-06 | N/A | IAM policy cloud là control Infrastructure Design |
| SECURITY-07 | N/A | Network deny-by-default là control Infrastructure Design |
| SECURITY-08 | Compliant | Object/function authorization được thể hiện xuyên IAM, môn, lớp, nội dung, bài nộp, điểm và payment |
| SECURITY-09 | Compliant | Failure scenarios yêu cầu fail closed và safe error; hardening chi tiết downstream |
| SECURITY-10 | N/A | Supply-chain controls được truy vết tới Code Generation/Build and Test theo lựa chọn không tạo system story |
| SECURITY-11 | Compliant | Misuse cases gồm leo quyền, prompt vượt phạm vi, sửa điểm và webhook replay có acceptance criteria |
| SECURITY-12 | Compliant | US-IAM-001/002/003 bao phủ password, session, brute-force; MFA admin giữ downstream |
| SECURITY-13 | Compliant | US-PAY-002 và US-AUD-001 bao phủ integrity/replay/audit; artifact integrity downstream |
| SECURITY-14 | Compliant | US-AUD-001 xác định sự kiện và tính bất biến; retention/alerting downstream |
| SECURITY-15 | Compliant | External failure scenarios yêu cầu fail closed, không mất dữ liệu và không lộ nội bộ |

Không có blocking security finding tại User Stories.

## 14. Resiliency Compliance tại User Stories

| Rule | Trạng thái | Áp dụng/N/A |
|---|---|---|
| RESILIENCY-01 | N/A | Phân loại component/dependency thuộc Application Design |
| RESILIENCY-02 | N/A | RTO/RPO đã chốt ở Requirements và được chi tiết downstream |
| RESILIENCY-03 | N/A | Change management không phải hành trình persona sản phẩm |
| RESILIENCY-04 | N/A | CI/CD/rollback là ràng buộc construction/infrastructure |
| RESILIENCY-05 | N/A | Metrics/logs/traces/dashboard thuộc NFR/Infrastructure Design |
| RESILIENCY-06 | N/A | Health checks thuộc NFR/Infrastructure Design |
| RESILIENCY-07 | N/A | Resiliency/capacity alarms thuộc Infrastructure Design |
| RESILIENCY-08 | N/A | Multi-zone topology đã chốt và thuộc Infrastructure Design |
| RESILIENCY-09 | N/A | Auto-scaling/quota thuộc Infrastructure Design |
| RESILIENCY-10 | Compliant | File, AI, payment và email scenarios yêu cầu timeout, retry hữu hạn và degraded/fail-safe behavior |
| RESILIENCY-11 | N/A | DR strategy đã chốt; runbook thuộc Infrastructure Design/Build and Test |
| RESILIENCY-12 | N/A | Backup/retention/test restore thuộc Infrastructure Design/Build and Test |
| RESILIENCY-13 | N/A | Failover/failback procedures thuộc Infrastructure Design/Build and Test |
| RESILIENCY-14 | N/A | Decision gate được giữ cho NFR Design theo REL-010 |
| RESILIENCY-15 | N/A | Incident response/COE thuộc NFR/Infrastructure Design |

Không có blocking resiliency finding tại User Stories; các mục N/A vẫn là ràng buộc bắt buộc tại stage downstream đã chỉ định.
