# User Stories - AI-Powered Learning Platform

## 1. Quy ước

- Stories được nhóm theo miền nghiệp vụ và sắp theo hành trình trong từng miền.
- Mã story có dạng `US-{DOMAIN}-{NNN}`.
- Mỗi story là một lát cắt nhỏ theo giá trị người dùng.
- Acceptance criteria dùng Given/When/Then.
- Lỗi tích hợp được mô tả trong acceptance criteria của story nghiệp vụ liên quan.
- Mỗi story ghi mã requirements liên quan; ràng buộc kỹ thuật thuần túy được giữ trong ma trận downstream thay vì tạo system story.
- Danh mục hiện hành chỉ gồm story thuộc MVP. Các mã story đã loại không được tái sử dụng; lịch sử quyết định nằm trong `audit.md`.
- Hệ thống chỉ có bốn persona người dùng: Người học, Giảng viên, Chủ nhiệm môn và Quản trị viên; không có Head of Department/Trưởng bộ môn.
- Các bài dùng ngôn ngữ tự nhiên được mô hình hóa chung là bài viết luận.
- Phạm vi triển khai: **50 story MVP**, được ánh xạ tới 78 use case hiện hành.

## 2. Miền Identity and Access

### US-IAM-001 - Nhận và kích hoạt tài khoản trường cấp

**Story**: Là người học hoặc giảng viên, tôi muốn kích hoạt tài khoản gắn với email trường để bắt đầu sử dụng nền tảng mà không cần tự đăng ký.

**Truy vết**: FR-001, FR-011, NFR-002, SEC-001, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Kích hoạt thành công

- **Given** tài khoản đã được cấp/import ở trạng thái chờ kích hoạt với email thuộc miền của trường
- **When** ở lần đăng nhập đầu người dùng nhập email trường, yêu cầu kích hoạt, xác minh OTP hệ thống gửi qua email và đặt mật khẩu đạt chính sách
- **Then** tài khoản được kích hoạt, OTP bị vô hiệu và người dùng nhận xác nhận an toàn

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
- **Then** hệ thống trả cùng một thông báo dù tài khoản có tồn tại hay không và chỉ gửi OTP có thời hạn nếu tài khoản đang hoạt động

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
- **Then** chỉ hồ sơ của người đó được cập nhật và dữ liệu nhạy cảm không xuất hiện trong log

#### Scenario 2 - Cố sửa hồ sơ người khác

- **Given** người dùng không có quyền quản trị đối tượng đích
- **When** người dùng gửi định danh của người khác
- **Then** hệ thống từ chối phía server mà không tiết lộ dữ liệu của đối tượng

### US-IAM-005 - Quản lý vai trò và phạm vi môn

**Story**: Là quản trị viên, tôi muốn gán hoặc thu hồi vai trò và phạm vi môn để người dùng chỉ có đúng quyền cần thiết.

**Truy vết**: FR-002, FR-003, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Gán Chủ nhiệm môn

- **Given** quản trị viên đã xác thực và một môn hợp lệ
- **When** quản trị viên gán vai trò Chủ nhiệm môn cùng phạm vi môn
- **Then** quyền cấp môn chỉ có hiệu lực với môn đó và thay đổi được ghi actor, thời gian, giá trị trước/sau

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

## 3. Miền Academic Structure and Content

### US-CAT-001 - Quản lý cấu trúc môn và lớp

**Story**: Là quản trị viên, tôi muốn tạo môn, tạo lớp thuộc môn và phân công vai trò để cấu trúc học thuật phản ánh đúng hoạt động đào tạo.

**Truy vết**: FR-002, FR-003, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Tạo cấu trúc hợp lệ

- **Given** quản trị viên và các tài khoản được phân công hợp lệ
- **When** quản trị viên tạo môn, lớp và gán giảng viên/Chủ nhiệm môn
- **Then** mỗi lớp thuộc đúng một môn, các phân công được lưu và thay đổi quan trọng được audit

#### Scenario 2 - Tham chiếu không hợp lệ

- **Given** môn, lớp hoặc người dùng không tồn tại/không phù hợp vai trò
- **When** quản trị viên gửi cấu hình
- **Then** hệ thống từ chối toàn bộ thay đổi không hợp lệ và trả hướng dẫn khắc phục an toàn

### US-CAT-002 - Quản lý vòng đời lớp

**Story**: Là giảng viên, tôi muốn sửa, mở và lưu trữ lớp được phân công (lớp do quản trị viên tạo) để kiểm soát nội dung người học nhìn thấy.

**Truy vết**: FR-002, FR-003, FR-014, NFR-002, SEC-002, SEC-003, SEC-005, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Mở lớp

- **Given** giảng viên được phân công và lớp `DRAFT` có giảng viên chính, môn đang `ACTIVE`
- **When** giảng viên mở lớp
- **Then** lớp chuyển `OPEN`, người học được ghi danh thấy lớp và nội dung đã xuất bản, nhận thông báo ghi danh, và thay đổi được audit

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

**Truy vết**: FR-002, FR-003, FR-011, FR-014, SEC-002, SEC-003, SEC-005, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Ghi danh hợp lệ

- **Given** lớp `DRAFT` hoặc `OPEN` và người thực hiện quản lý lớp
- **When** người học được ghi danh
- **Then** ghi danh được tạo một lần; thông báo ghi danh được xếp gửi khi lớp đã `OPEN`

#### Scenario 2 - Ghi danh trùng hoặc ngoài quyền

- **Given** người học đã được ghi danh hoặc người thực hiện không quản lý lớp
- **When** yêu cầu được gửi
- **Then** hệ thống không tạo bản ghi trùng, không mở rộng quyền và trả kết quả an toàn

#### Scenario 3 - Gỡ người học khỏi lớp

- **Given** người thực hiện quản lý lớp và người học đang được ghi danh
- **When** người thực hiện xác nhận gỡ ghi danh
- **Then** quyền truy cập mới bị thu hồi, dữ liệu học tập lịch sử được giữ theo chính sách và thay đổi được audit

### US-CAT-005 - Tự ghi danh bằng mã mời

**Story**: Là người học, tôi muốn dùng mã mời để tự ghi danh vào lớp được phép mà không phải chờ nhập thủ công.

**Truy vết**: FR-002, FR-003, FR-022, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Mã mời hợp lệ

- **Given** mã mời còn hiệu lực, lớp còn mở và người học đủ điều kiện
- **When** người học xác nhận tham gia
- **Then** hệ thống tạo đúng một ghi danh và gửi thông báo xác nhận

#### Scenario 2 - Mã sai, hết hạn hoặc bị lạm dụng

- **Given** mã không hợp lệ, hết hạn, đã thu hồi hoặc vượt giới hạn thử
- **When** người học gửi mã
- **Then** hệ thống không tiết lộ thông tin lớp, không ghi danh và áp dụng rate limit phù hợp

### US-CNT-001 - Quản lý kho học liệu và RAG cấp môn

**Story**: Là Chủ nhiệm môn, tôi muốn soạn hoặc tải học liệu vào kho cấp môn và theo dõi xử lý RAG để mọi lớp dùng chung nguồn đã kiểm soát.

**Truy vết**: FR-002, FR-004, FR-012, FR-013, FR-014, NFR-003, SEC-005, SEC-002, SEC-003, SEC-006, REL-003.

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

**Truy vết**: FR-002, FR-003, FR-004, FR-013, FR-014, NFR-003, SEC-005, SEC-002, SEC-003, SEC-006, REL-003.

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

### US-CNT-004 - Thông báo và hỏi đáp trong lớp

**Story**: Là thành viên lớp, tôi muốn đọc thông báo và trao đổi hỏi đáp trong đúng lớp để phối hợp học tập tại một nơi.

**Truy vết**: FR-002, FR-003, FR-011, FR-023, SEC-002, SEC-003, SEC-005, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Đăng và phản hồi đúng lớp

- **Given** giảng viên quản lý lớp hoặc người học được ghi danh
- **When** người dùng thực hiện hành động được vai trò cho phép
- **Then** nội dung chỉ hiển thị trong lớp, người liên quan nhận thông báo và actor/thời gian được lưu

#### Scenario 2 - Nội dung không hợp lệ hoặc ngoài lớp

- **Given** nội dung vượt giới hạn, chứa dữ liệu bị cấm hoặc người dùng không thuộc lớp
- **When** yêu cầu được gửi
- **Then** hệ thống từ chối, không phát thông báo và không tiết lộ thành viên/nội dung lớp

### US-CNT-005 - Dùng YouTube làm nguồn RAG theo bài giảng

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn gắn video/playlist YouTube vào bài giảng và xử lý transcript để dùng đúng nguồn đó cho RAG.

**Truy vết**: FR-002, FR-004, FR-012, FR-014, NFR-003, SEC-002, SEC-003, SEC-005, SEC-006, REL-003.

**Acceptance criteria**

#### Scenario 1 - Video có caption

- **Given** người dùng có quyền với bài giảng và URL YouTube hợp lệ có caption
- **When** người dùng yêu cầu xử lý nguồn
- **Then** hệ thống lưu liên kết video/bài giảng, transcript có timestamp và chỉ mục trong đúng phạm vi RAG

#### Scenario 2 - Playlist hoặc video không có caption

- **Given** URL là playlist hợp lệ hoặc video không có caption khả dụng
- **When** tác vụ xử lý chạy
- **Then** hệ thống xử lý từng video, chỉ dùng caption có sẵn; video không có caption được đánh dấu "không có phụ đề", không lập chỉ mục; trạng thái hiển thị riêng cho từng mục

#### Scenario 3 - Nguồn lỗi hoặc ngoài quyền

- **Given** URL không hợp lệ, video không truy cập được, lấy caption thất bại hoặc bài giảng ngoài quyền
- **When** yêu cầu được xử lý
- **Then** hệ thống không lập chỉ mục kết quả lỗi/ngoài quyền, giữ trạng thái có thể retry và không tạo transcript hoàn tất giả

## 4. Miền Group Assignment

### US-GRP-001 - Chia lớp thành nhóm và chỉ định trưởng nhóm

**Story**: Là giảng viên, tôi muốn tạo bộ nhóm cho từng bài nhóm của lớp được phân công (tạo tay, chia ngẫu nhiên hoặc dùng lại nhóm của bài khác) và chỉ định một trưởng nhóm cho mỗi nhóm để tổ chức bài tập nhóm rõ trách nhiệm.

**Truy vết**: FR-002, FR-003, FR-025, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Tạo nhóm hợp lệ

- **Given** giảng viên quản lý lớp và các sinh viên đang được ghi danh
- **When** giảng viên tạo nhóm, thêm thành viên và chọn trưởng nhóm
- **Then** mỗi nhóm có đúng một trưởng nhóm, thành viên thuộc đúng lớp và thay đổi được audit

#### Scenario 2 - Thành viên hoặc trưởng nhóm không hợp lệ

- **Given** sinh viên ngoài lớp, đã thuộc nhóm khác trong cùng bộ chia nhóm hoặc trưởng nhóm không phải thành viên
- **When** giảng viên lưu cấu hình
- **Then** hệ thống từ chối phần cấu hình không nhất quán và không mở rộng quyền ngoài lớp

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

**Story**: Là giảng viên, tôi muốn tạo bài tập nhóm là một tài liệu chung có các mục việc để các thành viên tự nhận và cùng hoàn thành một sản phẩm nhóm.

**Truy vết**: FR-002, FR-007, FR-016, FR-017, FR-026, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Khung có mục việc

- **Given** lớp đã có bộ nhóm hợp lệ cho bài
- **When** giảng viên soạn khung tài liệu với các mục việc (ví dụ use case diagram, activity diagram) và rubric rồi phát hành
- **Then** mỗi nhóm nhận một tài liệu chung theo khung; các mục ở trạng thái trống để thành viên nhận

#### Scenario 2 - Nhả khóa mục khi cần

- **Given** một mục đang bị một thành viên giữ quá lâu hoặc thành viên đó rời nhóm
- **When** trưởng nhóm hoặc giảng viên nhả khóa
- **Then** mục trở lại trạng thái có thể nhận, nội dung đã viết giữ nguyên kèm tác giả và thao tác được audit

### US-GRP-004 - Nhận và làm mục trong tài liệu nhóm

**Story**: Là thành viên nhóm, tôi muốn nhận một mục trong tài liệu nhóm, làm mục đó trong trang riêng rồi đưa vào tài liệu chung để nhóm review.

**Truy vết**: FR-002, FR-007, FR-008, FR-018, FR-026, FR-014, SEC-002, SEC-003, SEC-006, SEC-007, REL-003.

**Acceptance criteria**

#### Scenario 1 - Nhận và làm mục

- **Given** mục đang trống và bài còn hạn
- **When** thành viên nhận mục
- **Then** mục bị khóa cho thành viên đó, mở ra trong trang riêng như bài DOCUMENT thường; thành viên khác không sửa được mục đó

#### Scenario 2 - Xong và review

- **Given** thành viên làm xong mục
- **When** thành viên bấm "Xong"
- **Then** nội dung được ghép realtime vào tài liệu chung, mục chuyển sang chờ review và mở khóa để thành viên khác có thể nhận sửa; lịch sử ghi tác giả phiên bản

#### Scenario 3 - Giảng viên chọn phương thức chấm phần của từng người

- **Given** tài liệu nhóm đã nộp
- **When** giảng viên chấm đóng góp của một thành viên (các mục người đó viết) thủ công hoặc "Nhờ AI đề xuất"
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
- **When** trưởng nhóm nộp (hệ thống cảnh báo nếu còn mục trống hoặc đang nhận)
- **Then** hệ thống lưu một bản bất biến kèm tác giả từng mục; trưởng nhóm có thể nộp lại trước hạn, bản nộp cuối được chấm

#### Scenario 3 - Hết hạn

- **Given** hết hạn mà nhóm chưa nộp
- **When** tới hạn
- **Then** hệ thống tự nộp bản hiện tại, mục đang nhận được đưa vào với nội dung đã lưu gần nhất và giảng viên thấy cảnh báo

### US-GRP-006 - Đối chiếu và chấm tay tài liệu nhóm

**Story**: Là giảng viên, tôi muốn xem tài liệu nhóm đã nộp cùng phần đóng góp của từng thành viên, tự chấm tính tích hợp và quyết định điểm cuối từng sinh viên để phản ánh cả chất lượng chung và mức đóng góp.

**Truy vết**: FR-002, FR-008, FR-009, FR-020, FR-026, FR-014, SEC-005, SEC-002, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Chấm tay và đối chiếu

- **Given** nhóm đã nộp tài liệu chung
- **When** giảng viên mở màn hình review
- **Then** hệ thống hiển thị bản nộp cuối, tác giả từng mục, đề xuất AI cho phần đóng góp nếu có và các vùng điểm/feedback tách biệt

#### Scenario 2 - Không cho AI chấm tài liệu nhóm

- **Given** người dùng đang xem tài liệu chung của nhóm
- **When** chọn phương thức chấm
- **Then** hệ thống chỉ cung cấp chấm thủ công, không gửi tài liệu chung tới AI và lưu giảng viên là người quyết định điểm

#### Scenario 3 - Mục còn trống

- **Given** một hoặc nhiều mục chưa có nội dung khi nộp
- **When** giảng viên review tài liệu nhóm
- **Then** hệ thống chỉ rõ mục còn trống nhưng vẫn cho phép giảng viên xử lý tài liệu nhóm theo chính sách lớp mà không giả định đóng góp

#### Scenario 4 - Nội dung không nhất quán

- **Given** các mục đúng riêng lẻ nhưng xung đột khi đặt chung
- **When** giảng viên chấm tiêu chí tích hợp và nhất quán
- **Then** lỗi được trừ ở điểm tài liệu chung; chỉ khi xác định được mục/thành viên gây lỗi, giảng viên mới trừ thêm cho người đó và phải ghi lý do

#### Scenario 5 - Quyết định điểm cuối từng sinh viên

- **Given** điểm/feedback phần đóng góp và điểm tài liệu chung đã có
- **When** giảng viên nhập điểm cuối cho từng sinh viên
- **Then** hệ thống hiển thị hai nguồn để tham khảo nhưng không tự áp dụng công thức, đồng thời audit mọi điều chỉnh và lý do

## 5. Miền Learning Journey

### US-LRN-001 - Truy cập lớp đã ghi danh

**Story**: Là người học, tôi muốn xem cấu trúc và nội dung đã xuất bản của lớp được ghi danh để học đúng chương trình.

**Truy vết**: FR-002, FR-003, FR-005, FR-013, NFR-002, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Truy cập hợp lệ

- **Given** người học được ghi danh và nội dung đã xuất bản
- **When** người học mở lớp
- **Then** hệ thống hiển thị nội dung cấp môn/lớp được phép và cấp quyền tệp ngắn hạn khi cần

#### Scenario 2 - Không ghi danh hoặc nội dung nháp

- **Given** người học không thuộc lớp hoặc nội dung chưa xuất bản
- **When** người học dùng URL/ID trực tiếp
- **Then** hệ thống từ chối mà không tiết lộ nội dung hoặc metadata nhạy cảm

## 6. Miền Question and Rubric Bank

### US-QBK-001 - Quản lý ngân hàng rubric

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn tạo, sửa, tìm kiếm và tái sử dụng rubric trong phạm vi được giao để chấm bài nhất quán.

**Truy vết**: FR-002, FR-016, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Quản lý rubric hợp lệ

- **Given** người dùng có quyền với lớp hoặc môn và rubric có các mục checklist hợp lệ
- **When** người dùng tạo, sửa hoặc chọn rubric
- **Then** rubric được lưu đúng phạm vi; mỗi tiêu chí gồm các mục checklist có điểm và điểm rubric là tổng điểm các mục đạt

#### Scenario 2 - Rubric đã được sử dụng

- **Given** rubric đã gắn với bài đánh giá hoặc kết quả chấm
- **When** người dùng sửa hoặc xóa
- **Then** hệ thống tạo phiên bản mới hoặc chặn xóa để kết quả lịch sử không thay đổi

### US-QBK-002 - Quản lý ngân hàng câu hỏi

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn tạo, sửa, tìm kiếm và nhập câu hỏi hàng loạt để tái sử dụng nội dung đánh giá có kiểm soát.

**Truy vết**: FR-002, FR-016, FR-017, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Quản lý câu hỏi đúng phạm vi

- **Given** người dùng có quyền và câu hỏi/đáp án/cấu hình điểm hợp lệ
- **When** người dùng tạo, sửa, tìm kiếm hoặc nhập tệp
- **Then** câu hỏi được lưu đúng lớp/môn, kết quả nhập báo theo dòng và không tạo bản ghi lỗi

#### Scenario 2 - Câu hỏi đã được dùng trong bài

- **Given** câu hỏi đã thuộc một bài được phát hành
- **When** người dùng sửa câu hỏi trong ngân hàng
- **Then** hệ thống tạo version mới trong ngân hàng; bài đã phát hành vẫn dùng version cũ và không bị thay đổi

#### Scenario 3 - Muốn đổi nội dung bài đã phát hành

- **Given** bài đã phát hành cần thay đổi nội dung hoặc đáp án
- **When** giảng viên thử sửa bài
- **Then** hệ thống không cho sửa version đang giao; sau khi ngưng giao hoặc bài đóng, giảng viên sửa để tạo version mới, version cũ giữ nguyên cho bài nộp cũ, thao tác được audit

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

### US-AIG-002 - Tạo bản nháp template/câu hỏi cấp môn bằng AI

**Story**: Là Chủ nhiệm môn, tôi muốn dùng AI tạo bản nháp template đề hoặc câu hỏi ngân hàng cấp môn từ kho học liệu/RAG của môn để giảng viên các lớp dùng lại (không có đề chung giao thẳng cho lớp).

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

**Story**: Là quản trị viên, tôi muốn cấu hình giới hạn và giám sát việc sử dụng AI để kiểm soát chi phí, rủi ro và khả năng vận hành của nền tảng.

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

## 8. Miền Assessment Delivery

### US-ASM-001 - Duyệt và xuất bản bài đánh giá của lớp

**Story**: Là giảng viên, tôi muốn chỉnh sửa, duyệt và xuất bản bản nháp đánh giá cho lớp được phân công để kiểm soát chất lượng trước khi giao.

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
- **Then** hệ thống thêm các block với `origin = LEARNER` vào bản nháp hiện tại, không sửa hoặc xóa khung giảng viên; phần không hỗ trợ được báo rõ, lỗi nhập không làm mất bản nháp

### US-ASM-005 - Soạn và kiểm thử Code Lab

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn cấu hình Code Lab cùng test công khai/test ẩn và chạy thử để xác nhận bài có thể chấm tự động.

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

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn soạn bài trắc nghiệm với đáp án và quy tắc điểm để hệ thống có thể chấm nhất quán.

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

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn soạn bài viết luận với hướng dẫn và rubric để đánh giá câu trả lời mở theo tiêu chí rõ ràng.

**Truy vết**: FR-002, FR-006, FR-016, FR-017, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Bài viết luận hợp lệ

- **Given** đề bài và rubric hợp lệ (bài viết là văn bản thường, không giới hạn số từ, không nộp tệp)
- **When** người dùng xem trước hoặc duyệt bài
- **Then** hệ thống hiển thị đúng hướng dẫn, tiêu chí và cấu hình nộp cho góc nhìn người học

#### Scenario 2 - Nội dung dùng ngôn ngữ tự nhiên

- **Given** bài viết sử dụng bất kỳ ngôn ngữ tự nhiên nào
- **When** người dùng cấu hình bài
- **Then** hệ thống vẫn lưu dưới loại bài viết luận và ngôn ngữ chỉ là thuộc tính/cấu hình nếu cần

### US-ASM-008 - Nhân bản, sửa phiên bản và ngừng giao bài

**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn nhân bản bài cũ thành bài mới, xem thay đổi phiên bản và ngừng nhận bài mới khi cần để tái sử dụng nội dung mà không sửa dữ liệu đã phát sinh.

**Truy vết**: FR-002, FR-007, FR-016, FR-028, FR-014, SEC-002, SEC-003, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Phiên bản và nhân bản

- **Given** người dùng có quyền với bài nguồn
- **When** người dùng sửa hoặc nhân bản
- **Then** sửa bài đã ngừng giao/đóng tạo version mới, xem được diff giữa hai version; nhân bản tạo định danh mới; không sao chép bài nộp/điểm

#### Scenario 2 - Ngừng giao hoặc nhận bài mới

- **Given** bài đã phát hành và chính sách cho phép thu hồi
- **When** người dùng xác nhận ngừng giao/nhận bài mới
- **Then** bài biến mất khỏi danh sách cần làm hoặc khóa lượt nộp mới theo chính sách, nhưng cấu hình đã phát hành, bài nộp và điểm cũ vẫn chỉ đọc được để truy vết

### US-ASM-009 - Phát hành và sử dụng template đề cấp môn

**Story**: Là Chủ nhiệm môn, tôi muốn phát hành template đề có version để giảng viên copy và điều chỉnh cho lớp mà không làm thay đổi template gốc.

**Truy vết**: FR-002, FR-014, FR-016, FR-027, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Phát hành và copy template

- **Given** Chủ nhiệm môn có quyền với môn và template hợp lệ
- **When** Chủ nhiệm môn phát hành, sau đó giảng viên của lớp thuộc môn copy template
- **Then** hệ thống tạo draft độc lập cho lớp, lưu source template/version và không tự đồng bộ cập nhật sau này

#### Scenario 2 - Sai phạm vi hoặc sửa nguồn

- **Given** giảng viên không phụ trách lớp đích hoặc cố sửa trực tiếp template chỉ đọc
- **When** yêu cầu được gửi
- **Then** hệ thống từ chối phía server và giữ nguyên template

### US-ASM-010 - Copy assignment và rubric giữa các lớp

**Story**: Là giảng viên, tôi muốn copy assignment và rubric giữa các lớp mình phụ trách để tái sử dụng nội dung mà không mang theo dữ liệu thực thi cũ.

**Truy vết**: FR-002, FR-014, FR-016, FR-028, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Copy hợp lệ

- **Given** giảng viên được phân công cả lớp nguồn và lớp đích
- **When** giảng viên copy assignment hoặc rubric
- **Then** hệ thống tạo draft/identity độc lập ở lớp đích, giữ nguồn gốc audit và không copy lịch, attempt, bài nộp hoặc điểm

#### Scenario 2 - Lớp đích ngoài quyền

- **Given** giảng viên không được phân công lớp đích dù lớp đó thuộc cùng môn
- **When** yêu cầu copy được gửi
- **Then** hệ thống từ chối ở mức đối tượng và không tiết lộ nội dung lớp đích

### US-ASM-011 - Làm simulation exam giới hạn lượt

**Story**: Là người học, tôi muốn làm simulation exam theo số lượt và chính sách rõ ràng để luyện tập hoặc nhận điểm thành phần mà không nhầm đây là kỳ thi chính thức.

**Truy vết**: FR-002, FR-007, FR-014, FR-018, FR-029, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Làm trong giới hạn

- **Given** simulation exam đang mở và người học còn lượt
- **When** người học bắt đầu và nộp attempt
- **Then** hệ thống giữ snapshot đề/version, cập nhật số lượt (mặc định 3, giảng viên chỉnh từ 1 đến 10) và áp dụng chính sách kết quả cao nhất/gần nhất/trung bình đã công bố

#### Scenario 2 - Công bố và tính điểm

- **Given** giảng viên đã cấu hình thời điểm hiện đáp án và trạng thái tính điểm thành phần
- **When** attempt được hoàn tất hoặc cửa sổ bài đóng
- **Then** hệ thống chỉ hiển thị đáp án đúng thời điểm và đưa kết quả vào điểm thành phần chỉ khi cấu hình cho phép

#### Scenario 3 - Không phải kỳ thi chính thức

- **Given** người học hoặc giảng viên xem simulation exam
- **When** giao diện hiển thị thông tin bài
- **Then** hệ thống ghi rõ đây là thi thử, số lượt, cách lấy kết quả và việc có/không tính điểm; không hiển thị như proctored exam

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

- **Given** giảng viên quản lý lớp, đã nhận bài nộp hợp lệ và rubric đã được chọn
- **When** giảng viên chọn “Nhờ AI đề xuất” và tác vụ hoàn tất
- **Then** điểm/phản hồi được lưu là đề xuất chưa duyệt, kèm actor/thời gian lựa chọn và không được công bố là quyết định cuối; nếu bài có sơ đồ Draw.io thì chỉ XML rút gọn dẫn xuất được gửi AI, còn XML đầy đủ vẫn là bản nộp chuẩn cho giảng viên

#### Scenario 2 - Giảng viên chọn chấm thủ công

- **Given** giảng viên đã nhận bài nộp hợp lệ
- **When** giảng viên chọn chấm thủ công
- **Then** hệ thống không gửi bài nộp tới AI và cung cấp biểu mẫu điểm/phản hồi theo rubric

#### Scenario 3 - AI lỗi

- **Given** AI timeout, lỗi hoặc đạt giới hạn sử dụng
- **When** tác vụ chấm chạy
- **Then** bài nộp không mất, không có điểm cuối giả và giảng viên có thể chấm thủ công hoặc thử lại có kiểm soát

#### Scenario 4 - Ngoài lớp phân công

- **Given** bài nộp thuộc lớp khác
- **When** giảng viên yêu cầu AI chấm
- **Then** hệ thống từ chối trước khi gửi dữ liệu tới provider

### US-GRD-003 - Duyệt, ghi đè và công bố điểm

**Story**: Là giảng viên, tôi muốn chấm thủ công hoặc duyệt/ghi đè đề xuất AI rồi công bố kết quả để chịu trách nhiệm cho quyết định học thuật cuối cùng.

**Truy vết**: FR-002, FR-008, FR-009, FR-014, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Chấp nhận đề xuất

- **Given** đề xuất AI chưa duyệt thuộc lớp được phân công
- **When** giảng viên chấp nhận và công bố
- **Then** kết quả trở thành điểm cuối, lưu actor/thời gian/phương thức và hiển thị theo chính sách

#### Scenario 2 - Ghi đè đề xuất

- **Given** giảng viên không đồng ý với đề xuất
- **When** nhập điểm/phản hồi mới cùng lý do và công bố
- **Then** điểm cuối được cập nhật, đề xuất gốc vẫn truy vết được và audit ghi người thực hiện, thời gian, lý do

#### Scenario 3 - Chấm hoàn toàn thủ công

- **Given** giảng viên đã chọn chấm thủ công cho bài nộp thuộc lớp được phân công
- **When** nhập điểm/phản hồi hợp lệ và công bố
- **Then** điểm trở thành kết quả cuối với phương thức chấm thủ công, actor/thời gian và không có lời gọi AI nào cho bài nộp đó

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
- **When** người học mở sổ điểm
- **Then** chỉ điểm, phản hồi và trạng thái bài nộp của chính người học được hiển thị

#### Scenario 2 - Giảng viên xem lớp

- **Given** giảng viên được phân công lớp
- **When** mở sổ điểm lớp
- **Then** hệ thống hiển thị tổng hợp và chi tiết cần thiết của lớp đó

#### Scenario 3 - Quản trị viên xem theo quyền

- **Given** quản trị viên có quyền báo cáo phù hợp
- **When** yêu cầu dữ liệu theo phạm vi quản trị
- **Then** hệ thống trả đúng phạm vi, không mở quyền sửa điểm nếu chưa được cấp riêng

### US-GRD-005 - Kiểm tra và chốt điểm hàng loạt

**Story**: Là giảng viên, tôi muốn kiểm tra và chốt điểm hàng loạt cho lớp được phân công để công bố kết quả nhất quán và có kiểm soát.

**Truy vết**: FR-002, FR-008, FR-014, FR-020, SEC-002, SEC-003, SEC-005, SEC-007.

**Acceptance criteria**

#### Scenario 1 - Chốt các bài đủ điều kiện

- **Given** giảng viên quản lý lớp và danh sách gồm các bài đã có điểm/phản hồi hợp lệ
- **When** giảng viên xác nhận chốt hàng loạt
- **Then** chỉ bài đủ điều kiện chuyển thành điểm cuối, kết quả theo từng bài được trả và mỗi thay đổi được audit

#### Scenario 2 - Một phần dữ liệu không hợp lệ

- **Given** danh sách có bài ngoài lớp, thiếu điểm hoặc đã bị thay đổi đồng thời
- **When** yêu cầu được xử lý
- **Then** hệ thống không chốt nhầm, báo rõ từng mục thất bại và không mở rộng quyền từ các mục hợp lệ

## 10. Miền Reporting and Analytics

### US-RPT-001 - Theo dõi tiến độ nộp bài và nhắc nhở

**Story**: Là giảng viên, tôi muốn theo dõi trạng thái nộp bài và để hệ thống tự nhắc người học chưa nộp trước hạn, giúp họ hoàn thành đúng hạn.

**Truy vết**: FR-002, FR-011, FR-019, NFR-002, SEC-005, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Xem tiến độ đúng lớp

- **Given** giảng viên được phân công và bài đã giao
- **When** mở báo cáo tiến độ
- **Then** hệ thống phân biệt đã nộp, chưa nộp, đang làm, nộp trễ và thời gian còn lại

#### Scenario 2 - Tự nhắc trước hạn

- **Given** bài còn 24 giờ tới hạn và có người học chưa nộp
- **When** tới thời điểm nhắc
- **Then** hệ thống gửi một lần thông báo trong app và email (nếu người học không tắt) cho đúng người chưa nộp; bài đã ngừng giao không được nhắc

### US-RPT-002 - Dashboard kết quả cá nhân

**Story**: Là người học, tôi muốn xem dashboard điểm, trạng thái bài nộp và bài sắp đến hạn để ưu tiên việc học của mình.

**Truy vết**: FR-002, FR-009, FR-024, NFR-002, SEC-005, SEC-002, SEC-003.

**Acceptance criteria**

#### Scenario 1 - Tổng hợp dữ liệu cá nhân

- **Given** người học đã ghi danh
- **When** mở dashboard
- **Then** hệ thống chỉ hiển thị dữ liệu của người học, chỉ hiện điểm đã công bố, phân biệt bài chờ chấm và ưu tiên bài sắp đến hạn

#### Scenario 2 - Phân bố lớp ẩn danh

- **Given** lớp cho phép hiển thị so sánh
- **When** người học xem phân bố điểm
- **Then** dữ liệu được tổng hợp/ẩn danh và không suy ra danh tính hoặc điểm của người học khác

### US-RPT-003 - Xuất bảng điểm

**Story**: Là giảng viên hoặc quản trị viên có quyền, tôi muốn xuất bảng điểm theo lớp/bài để phục vụ lưu trữ và xử lý nghiệp vụ ngoài hệ thống.

**Truy vết**: FR-002, FR-009, FR-024, SEC-005, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Xuất đúng phạm vi

- **Given** người dùng có quyền xem lớp hoặc bài đánh giá
- **When** yêu cầu xuất Excel/CSV
- **Then** tệp chứa trạng thái, điểm đã chốt, thời gian nộp và phản hồi đúng phạm vi; mục chưa nộp/chưa chốt được ghi rõ

#### Scenario 2 - Ngăn xuất ngoài quyền

- **Given** bộ lọc hoặc ID tham chiếu lớp ngoài quyền
- **When** người dùng yêu cầu xuất
- **Then** hệ thống từ chối trước khi tạo tệp và không rò rỉ dữ liệu qua tên tệp/metadata

## 11. Miền Payment and AI Credit

### US-PAY-001 - Bắt đầu thanh toán an toàn

**Story**: Là người dùng, tôi muốn bắt đầu thanh toán qua nhà cung cấp để mua credit AI mà nền tảng không lưu dữ liệu thẻ thô.

**Truy vết**: FR-010, FR-014, NFR-002, SEC-005, SEC-002, SEC-003, SEC-006, SEC-007, REL-003.

**Acceptance criteria**

#### Scenario 1 - Tạo giao dịch

- **Given** người dùng đã xác thực và gói credit hợp lệ
- **When** người dùng bắt đầu thanh toán
- **Then** hệ thống tạo giao dịch nội bộ duy nhất và chuyển sang luồng provider mà không thu/lưu dữ liệu thẻ thô

#### Scenario 2 - Provider lỗi

- **Given** provider timeout hoặc từ chối tạo giao dịch
- **When** yêu cầu được xử lý
- **Then** giao dịch không bị đánh dấu đã thanh toán, credit không được cộng và người dùng nhận trạng thái an toàn có thể thử lại

### US-PAY-002 - Nhận credit AI sau xác nhận thanh toán

**Story**: Là người dùng đã thanh toán, tôi muốn credit AI chỉ được cộng sau xác nhận hợp lệ để trạng thái mua hàng chính xác.

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

### US-PAY-003 - Đối soát trạng thái thanh toán

**Story**: Là quản trị viên, tôi muốn đối soát giao dịch với nhà cung cấp để xử lý trạng thái chờ hoặc sai lệch mà không cộng credit nhầm.

**Truy vết**: FR-002, FR-010, FR-014, SEC-002, SEC-003, SEC-005, SEC-006, SEC-007, REL-003.

**Acceptance criteria**

#### Scenario 1 - Đối soát giao dịch

- **Given** quản trị viên có quyền và giao dịch cần kiểm tra
- **When** hệ thống lấy trạng thái từ provider trong timeout
- **Then** kết quả được so sánh, cập nhật idempotent theo rule và ghi audit

#### Scenario 2 - Không thể đối soát

- **Given** provider không khả dụng hoặc trả dữ liệu không xác minh được
- **When** đối soát chạy
- **Then** trạng thái hiện tại không được nâng lên thành công, credit không được cộng và lỗi có thể retry được ghi an toàn

## 12. Miền Notification and Audit

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

- **Given** đăng nhập thất bại, thay đổi role/phạm vi môn, thay đổi nội dung/điểm, phát hành bài/template, thanh toán hoặc truy cập đặc quyền
- **When** hành động hoàn tất hoặc bị từ chối
- **Then** sự kiện tương ứng được ghi với actor, thời gian, đối tượng và kết quả phù hợp

## 13. Ma trận bao phủ yêu cầu chức năng

| Requirement | Stories chính |
|---|---|
| FR-001 | US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-006 |
| FR-002 | Quy tắc kiểm quyền xuyên suốt 50 story MVP; chi tiết actor và phạm vi nằm trong từng story/UC |
| FR-003 | US-IAM-005, US-CAT-001 đến US-CAT-003, US-CAT-005 |
| FR-004 | US-CNT-001, US-CNT-002, US-CNT-005, US-AIG-002 |
| FR-005 | US-LRN-001 |
| FR-006 | US-AIG-001, US-AIG-002, US-ASM-001, US-ASM-004 đến US-ASM-007 |
| FR-007 | US-ASM-001, US-ASM-003, US-ASM-008, US-ASM-011 |
| FR-008 | US-GRD-001 đến US-GRD-005 |
| FR-009 | US-GRD-004, US-RPT-002, US-RPT-003 |
| FR-010 | US-PAY-001, US-PAY-002, US-PAY-003 |
| FR-011 | US-IAM-001, US-IAM-003, US-CAT-003, US-CNT-004, US-RPT-001, US-NTF-001 |
| FR-012 | US-CNT-001, US-AIG-001, US-AIG-002, US-AIG-003, US-GRD-002 |
| FR-013 | US-CNT-001, US-CNT-002, US-LRN-001 |
| FR-014 | US-IAM-002, US-IAM-005, US-CAT-001, US-CAT-002, US-AIG-001, US-AIG-002, US-ASM-001, US-ASM-003, US-GRD-002, US-GRD-003, US-PAY-001, US-PAY-002, US-PAY-003, US-AUD-001 |
| FR-015 | US-IAM-007 |
| FR-016 | US-QBK-001, US-QBK-002, US-ASM-006, US-ASM-007, US-ASM-009, US-ASM-010 |
| FR-017 | US-QBK-002, US-ASM-003 đến US-ASM-007 |
| FR-018 | US-ASM-003 |
| FR-019 | US-RPT-001 |
| FR-020 | US-GRD-005 |
| FR-021 | US-AIG-003 |
| FR-022 | US-CAT-005 |
| FR-023 | US-CNT-004 |
| FR-024 | US-RPT-002, US-RPT-003 |
| FR-025 | US-GRP-001, US-GRP-002 |
| FR-026 | US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006 |
| FR-027 | US-ASM-009 |
| FR-028 | US-ASM-010 |
| FR-029 | US-ASM-011 |

## 14. Ràng buộc phi chức năng và kỹ thuật downstream

| Requirement | Xử lý tại User Stories | Stage xác minh chi tiết |
|---|---|---|
| NFR-001 | Không tạo system story; mọi story giả định web Next.js và API Spring Boot | NFR Requirements, Application Design, Code Generation |
| NFR-002 | Các luồng UI cốt lõi yêu cầu responsive, keyboard/accessibility labels và lỗi có hành động khắc phục | Functional Design, Code Generation, Build and Test |
| NFR-003 | Stories file/AI/payment/email có trạng thái, timeout và retry hữu hạn; API thường giữ mục tiêu p95 | NFR Design, Infrastructure Design, Build and Test |
| NFR-004 | Acceptance criteria là đầu vào cho unit, integration, system và e2e test; adapter/webhook cần contract test | Code Generation, Build and Test |
| NFR-005 | Không tạo system story; container, secret và version pinning là tiêu chí triển khai | Infrastructure Design, Code Generation, Build and Test |
| SEC-001 đến SEC-007 | Được gắn trên stories có hành vi quan sát được; phạm vi rút gọn cho đồ án | NFR Design, Infrastructure Design, Code Generation, Build and Test |
| REL-001 đến REL-004 | Timeout và fail-closed gắn vào story tích hợp; topology, DR, monitoring và incident process ngoài phạm vi đồ án | Application Design, NFR Design, Infrastructure Design, Build and Test |

## 15. Kiểm tra INVEST

| Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|
| Independent | Đạt | Mỗi story có một kết quả nghiệp vụ chính; quan hệ phụ thuộc được thể hiện bằng trạng thái Given thay vì gộp luồng lớn |
| Negotiable | Đạt | Stories mô tả giá trị/hành vi, không khóa nhà cung cấp, database hoặc kiến trúc triển khai |
| Valuable | Đạt | Mỗi story gắn với một trong bốn persona và nêu lợi ích rõ ràng |
| Estimable | Đạt | Phạm vi được giới hạn theo một thao tác hoặc kết quả quan sát được |
| Small | Đạt | Các hành trình lớn được tách theo kích hoạt, nội dung, tạo AI, phát hành, nộp, chấm và công bố |
| Testable | Đạt | Cả 50 story MVP có acceptance criteria Given/When/Then và truy vết requirements |

## 16. Security Compliance tại User Stories

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

## 17. Resiliency Compliance tại User Stories

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
