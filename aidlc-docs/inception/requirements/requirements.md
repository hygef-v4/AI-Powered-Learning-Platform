# Yêu cầu sản phẩm ALAP — Inception revision 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

## 1. Ý định, nguồn và trạng thái

- Yêu cầu: sửa Inception theo các quyết định trong cuộc trao đổi, không sửa code/Construction hoặc tài liệu Google.
- Phạm vi: web desktop-first cho một tổ chức, focus Assignment tài liệu SWR/SWD; Quiz bổ trợ, Lesson mở rộng.
- Đây là scope revision toàn hệ thống ở cấp tài liệu; approvals/checkpoints 09/10 là lịch sử, không phê duyệt thiết kế mới hoặc xác nhận runtime.
- Catalog hiện hành: [use-cases.md](use-cases.md). Bảng tiếng Anh docs/usecase_table.md đã đồng bộ catalog current. Không dùng docs/history/use-cases-73.md hoặc Page-2 G21_Diagrams.drawio làm nguồn current; chúng là baseline lịch sử.

## 2. Scope và phân quyền

### 2.1 Cốt lõi

Assignment INDIVIDUAL/GROUP; tài liệu thuần hoặc diagramEnabled; Draw.io XML, rubric, workspace, lưu nháp, nộp/lịch sử, phản hồi sau nộp bài Practice, chấm tay/AI proposal và công bố. Quiz độc lập Lesson. Bốn vai trò hiện hành; tài khoản trường cấp, môn/lớp/ghi danh, thông báo, export và audit giữ theo scope đã chốt.

### 2.2 Kết nối AI

API chung của tổ chức chỉ giảng viên được cấp quota; API cá nhân và agent MCP cho Teacher/Subject Manager/Student trong quyền của mình. Gemini/OpenAI/Claude là provider API được hỗ trợ; client agent MCP kiểm chứng riêng. AI là hỗ trợ, không tự publish/chốt điểm. Hết shared quota không auto-fallback personal.

### 2.3 Mở rộng và ngoài phạm vi

- Lesson/module processing, tóm tắt học liệu và RAG nâng cao chỉ mở rộng; link video YouTube dạy học thuộc MVP; không chặn luồng Assignment/Quiz.
- Loại khỏi scope: Code Lab, Judge0, test runner, mua credit/token, ví, payment/PayOS.
- Giữ exclusions trước: mobile native, multi-tenancy, LMS/SSO, mã mời, comments, Simulation/proctored Exam, grade appeal, gia hạn cá nhân và similarity checking chưa được chốt.
- Không tuyên bố AS-IS đã khảo sát. Thu thập/validate quy trình của giảng viên và sinh viên SWR/SWD rồi cập nhật Report 1; không dựng bằng chứng giả.
- Đã bổ sung backup Google Drive, ghi nhận activity khi làm Assignment/Quiz, email sự kiện quan trọng và minh bạch AI theo D05. Chi tiết: [council-features-2026-10-13.md](council-features-2026-10-13.md); không ghi đã triển khai.

### 2.4 Quyết định còn mở

Xem [revision-decisions-2026-10-13.md](revision-decisions-2026-10-13.md). Giữ chính sách cũ không mâu thuẫn: Quiz Practice, Group Graded. Không mặc định quota số cụ thể hoặc quyền Student gửi toàn bài nhóm ra agent.

## 3. Stakeholders

| Vai trò | Trách nhiệm |
|---|---|
| Student | Làm/nộp Assignment/Quiz, thành viên/leader; phản hồi qua kết nối cá nhân; không shared quota |
| Teacher | Assignment/Quiz lớp được giao, rubric và điểm official; shared quota hoặc personal API/MCP |
| Subject Manager | Môn, tài liệu/ngân hàng và lớp; dạy/chấm theo phân công R4 |
| Admin | Tài khoản/môn/Settings, API chung, quota và audit; không quyền học vụ hoặc key cá nhân |

## 4. Yêu cầu chức năng

### FR-001 - Xác thực và tài khoản trường cấp

Hệ thống phải dùng email do trường cấp làm định danh đăng nhập cho người học và giảng viên. Tài khoản do quản trị viên tạo hoặc nhập từ file CSV; không có đăng ký công khai. Hệ thống hỗ trợ OTP qua Brevo để kích hoạt/khôi phục, đăng nhập, đăng xuất, đổi mật khẩu và xem/cập nhật hồ sơ (UC 01–07). Người dùng cập nhật display name và phone number hợp lệ; không cập nhật avatar, email định danh, role hoặc status trong hồ sơ cá nhân.

**Tiêu chí chấp nhận:**

- Người dùng đã xác thực có thể đăng nhập và đăng xuất an toàn.
- Phiên hết hạn theo cấu hình và bị vô hiệu hóa khi đăng xuất.
- Luồng khôi phục mật khẩu không tiết lộ tài khoản có tồn tại hay không.
- Email đăng nhập của người học/giảng viên phải thuộc miền email trường được cấu hình; người dùng không thể tự đăng ký bằng email ngoài miền.

### FR-002 - Phân quyền và nguồn AI

Giữ bốn vai trò STUDENT, TEACHER, SUBJECT_MANAGER, ADMIN và kiểm R1–R5 ở backend. Admin chỉ quản trị, không có quyền học vụ. Subject Manager quản lý môn R2; dạy/chấm lớp cần phân công R4. Student cần ghi danh R5 và quyền sở hữu bài/nhóm.

**Tiêu chí chấp nhận:**
- API/web/MCP đều kiểm actor, scope, ownership, trạng thái và version hiện hành; không tin role/actor do agent tự khai.
- R1 áp dụng cho hồ sơ, kết nối AI cá nhân, lịch sử phản hồi và thông báo; không còn ví/kết quả thanh toán.
- API chung chỉ Teacher đang ACTIVE hoặc Subject Manager ACTIVE trong phạm vi học vụ hợp lệ. Student không được cấp/dùng quota chung, kể cả qua MCP hoặc gọi API trực tiếp.
- Có API key cá nhân hoặc agent không cấp thêm quyền học liệu, bài nộp, đáp án hay kết quả của người khác. Thu hồi quyền/kết nối có hiệu lực ở lần gọi tiếp theo.
- Admin kiểm tra kết nối cấu hình bằng payload tổng hợp; thao tác này không cấp quyền đọc bài/học liệu.

### FR-003 - Quản lý môn học và lớp học

Administrator xem danh sách/chi tiết, thêm và sửa môn, gán Chủ nhiệm môn, lưu trữ/mở lại môn (UC 64–67); Subject Detail của Administrator chỉ xem danh sách lớp của môn. Subject Manager vào Manager Dashboard (UC 53), xem lớp của môn được giao, tạo lớp, gán giảng viên, thêm/gỡ sinh viên, sửa và mở/lưu trữ lớp (UC 47–52). Teacher xem lớp được giao trên Class Dashboard và Teacher Class Detail (UC 31–32); Student xem lớp đã ghi danh trên Class Dashboard và Student Class Detail (UC 13–14). Người dùng chốt 2026-10-09.

**Tiêu chí chấp nhận:**

- Mỗi lớp thuộc một môn, dùng chung module/học liệu môn; không có thực thể khóa học riêng.
- Chủ nhiệm môn phải là Subject Manager ACTIVE; giảng viên chính là Teacher hoặc Subject Manager ACTIVE; Administrator không được gán môn hay lớp. Các phân công là điều kiện R2/R4 và được audit.
- Chỉ Chủ nhiệm môn của môn sửa thông tin/vòng đời lớp và thêm/gỡ sinh viên; Teacher chỉ xem thông tin lớp và danh sách sinh viên.
- Không có tự ghi danh bằng mã mời; sinh viên chỉ vào lớp khi Chủ nhiệm môn thêm.
- Student truy cập theo enrollment; học liệu lưu trữ không hiển thị; thay đổi cấu trúc/phân công được audit.

### FR-004 - Tài liệu tham khảo; Lesson là mở rộng

MVP cho người có quyền gắn tài liệu tham khảo vào môn/lớp/Assignment, xem và tải riêng tư qua U03/U05. Tài liệu và quyền của môn/lớp được tái sử dụng; không bắt buộc một hệ thống Lesson/module để tạo bài.

**Tiêu chí chấp nhận:**
- Kiểm loại/kích thước tệp và scope trước lưu/đọc; tài liệu được lưu trữ vẫn giữ tham chiếu lịch sử.
- Tạo Assignment/Quiz không yêu cầu có Lesson. Thiếu kết nối AI không chặn xem/tải tài liệu.
- Lesson có module, caption/transcription, summary/embedding và RAG nâng cao là mở rộng chưa cam kết trong MVP; lưu/mở link YouTube video dạy học là reference material MVP. Khi bật mở rộng phải chốt provider embedding, nguồn tài trợ, kiểm quyền và acceptance riêng; không dùng lại HOLD credit cũ.

### FR-005 - Truy cập tài liệu và lớp

Student chỉ xem lớp đã ghi danh và tài liệu được phép; Teacher/Subject Manager theo phân công. Link trực tiếp và MCP không vượt quyền. Không lưu tiến độ hoàn thành Lesson trong MVP. Quiz có thể gắn tài liệu nhưng không phụ thuộc bắt buộc vào Lesson.

### FR-006 - AI hỗ trợ soạn Assignment và Quiz

Người soạn có quyền lựa chọn nguồn AI chung hoặc cá nhân; agent riêng đọc nguồn qua MCP. AI tạo bản nháp yêu cầu bài, cấu trúc tài liệu, rubric hoặc câu Quiz. Nguồn chỉ từ tài liệu được phép.

**Tiêu chí chấp nhận:**
- Kết quả luôn là draft có provenance (nguồn kết nối/model nếu biết, người yêu cầu, tài liệu/version).
- Người soạn sửa/duyệt trước phát hành. Không AI tự publish, không thêm bài Code.
- Không có AI hoặc hết quota vẫn soạn thủ công được; không tự đổi sang kết nối cá nhân.
- Không mặc định mọi agent hỗ trợ MCP; hướng dẫn và compatibility phải được thử cho từng client.

### FR-007 - Assignment và Quiz; phát hành và giao bài

Chỉ hai loại bài cấp sản phẩm: ASSIGNMENT và QUIZ. Assignment là tài liệu có cấu trúc, cấu hình riêng submissionMode INDIVIDUAL/GROUP và diagramEnabled true/false. Hai thuộc tính độc lập: đủ bốn tổ hợp cá nhân/nhóm × thuần tài liệu/có sơ đồ. Essay, Diagram và Group không là loại bài riêng.

**Tiêu chí chấp nhận:**
- Giữ review trước publication, trạng thái DRAFT/published/retired, version và snapshot khi làm/nộp.
- Giữ bài cá nhân cấp môn theo baseline trước; bài nhóm thuộc lớp. Không tự mở nhóm cấp môn.
- Giữ Assignment cá nhân GRADED/PRACTICE; nhóm GRADED theo baseline trước, nhóm Practice chưa chốt (D02).
- Quiz không bắt buộc Lesson. Tạm giữ PRACTICE theo baseline trước; Quiz GRADED/lịch mở đóng chưa chốt (D01).
- Dạng bài, mode, deadline, số lượt và rubric được kiểm server-side; không suy ra mode từ diagramEnabled.

### FR-008 - Đánh giá và phản hồi

Quiz chấm xác định bằng đáp án đã snapshot. Assignment chấm rubric theo phần tài liệu, bao gồm yêu cầu sơ đồ khi bật; AI chỉ hỗ trợ phản hồi hoặc đề xuất theo nguồn đã chọn.

**Tiêu chí chấp nhận:**
- Teacher chấm tay hoặc nhận proposal từ API/MCP; chỉ Teacher lớp xác nhận/chốt/công bố điểm chính thức.
- Kết quả AI phải validate cấu trúc, rubric và version đầu vào; kết quả stale không ghi đè bài/điểm mới.
- Student phản hồi luyện tập theo FR-030, không vào Gradebook.
- Nhóm dùng điểm tài liệu chung và điều chỉnh đóng góp thủ công; không AI tự kết luận đóng góp.
- Không test runner, Code Lab hoặc Judge0.

### FR-009 - Sổ điểm và trạng thái bài nộp

Student xem điểm/phản hồi đã công bố và trạng thái của chính mình trên Student Assignments, Assignment Detail/Submission History (UC 22–23, 28); quiz trên Quiz Result (UC 21). Teacher hoặc Subject Manager được giao dạy R3/R4 xem/xuất gradebook lớp (UC 40); Admin bị từ chối, phân công quản lý môn không tự cấp quyền chấm/gradebook.

Sổ điểm chỉ chứa bài GRADED, không tính điểm tổng theo hệ số và không chứa AI Practice. Không lưu tiến độ hoàn thành hay vị trí học theo bài.

### FR-010 - Đã rút: thanh toán mua credit

Bỏ mua credit, gói giá, wallet, checkout, PayOS/webhook/đối soát và hoàn tiền. Không thay bằng bán token. Quota API chung thuộc FR-034; không tái sử dụng ID thanh toán làm UC kết nối AI.

### FR-011 - Email và thông báo

Hệ thống gửi OTP kích hoạt/khôi phục, ghi danh, bài mới/mở giao, công bố điểm và nhắc hạn; thêm email thông báo lớp mới được giảng viên đánh dấu quan trọng và cảnh báo backup thất bại cho Admin. Không tự gửi OTP khi Admin tạo/import tài khoản.

**Tiêu chí chấp nhận:**
- U16 xếp gửi sau commit, kiểm người nhận và tùy chọn nhận email học tập, chống trùng theo event/recipient/channel; OTP thuộc U01, dùng MailPort chung.
- Retry/backoff hữu hạn, trạng thái PENDING/SENT/FAILED; SENT là provider chấp nhận, không bảo đảm inbox. Lỗi email không rollback nghiệp vụ.
- Nội dung tối thiểu, link yêu cầu đăng nhập, không gửi raw điểm/bài nộp/secrets/backup attachment. Tuân hạn mức nhà cung cấp; không retry vô hạn.
- Thông báo lớp chỉnh sửa không tự gửi lại email tạo mới; backup failure chỉ tới Admin theo policy vận hành. Xem đặc tả góp ý hội đồng.

### FR-012 - Kết nối API AI và MCP

Hỗ trợ hai kênh: ALAP gọi API (Gemini/OpenAI/Claude), và agent riêng gọi MCP server của ALAP. Provider API khác tên ứng dụng agent; không hứa mọi client của ba hãng hỗ trợ MCP.

**Tiêu chí chấp nhận:**
- API chung do Admin cấu hình; API cá nhân do chính Student/Teacher/Subject Manager cấu hình. MVP dùng API key hợp lệ, không giả định gói chat cá nhân cấp API miễn phí.
- Key chỉ dùng phía server, lưu được bảo vệ, không plaintext trong log/API response; xem masked, thay/thu hồi, không đọc lại nguyên key.
- Adapter provider-neutral chuẩn hóa output/usage và giữ provenance; model tương thích cấu hình, timeout/retry hữu hạn.
- MCP xác thực người dùng ALAP; mỗi tool kiểm scope/ownership/state/version. Không lộ key chung/cá nhân cho agent.
- Tài liệu/học liệu mở rộng có embedding phải có cấu hình riêng; thay model sinh nội dung không tự đổi vector index.

### FR-013 - Lưu trữ tệp

Hệ thống phải lưu tệp học tập qua một dịch vụ lưu trữ riêng tư, cấp quyền truy cập ngắn hạn và không công khai trực tiếp object chứa dữ liệu học tập. Dung lượng tối đa và loại tệp cho phép theo mục đích do Administrator chỉnh trên Settings trong giới hạn hệ thống (FR-033).

### FR-014 - Audit nghiệp vụ và bảo mật

Administrator xem/tìm Audit Log (UC 73) theo actor, action, object, result và thời gian; chỉ đọc, không sửa/xóa log. Vai trò khác không có quyền tra cứu audit toàn nền tảng.

Hệ thống phải ghi sự kiện đăng nhập thất bại, thay đổi vai trò hoặc phạm vi môn, thay đổi học liệu, thay đổi điểm, phát hành bài của lớp/môn và quiz, thay đổi kết nối/quota AI, backup/restore và truy cập đặc quyền.

### FR-015 - Vòng đời tài khoản do quản trị viên quản lý

Quản trị viên xem danh sách/chi tiết, tìm kiếm, tạo, cập nhật thông tin được phép/role và khóa/mở khóa tài khoản (UC 59–63 giữ ID từ baseline trước; nhập CSV là luồng phụ của UC 60); thao tác hàng loạt phải kiểm tra từng dòng và báo kết quả không làm mất các bản ghi hợp lệ. Quản trị viên không đặt, cấp hay xem mật khẩu người dùng và không kích hoạt việc gửi OTP. Tài khoản mới ở trạng thái chờ kích hoạt; tạo hoặc nhập tài khoản không gửi email. Chỉ khi người dùng yêu cầu kích hoạt ở lần đăng nhập đầu, hệ thống mới gửi OTP qua email để người dùng xác minh và tự đặt mật khẩu lần đầu. Yêu cầu gửi OTP được giới hạn tần suất.

### FR-016 - Ngân hàng Quiz và rubric Assignment

Giữ ngân hàng câu hỏi cấp môn theo quyền và version; chỉ câu trắc nghiệm cho Quiz, nội dung/khung tài liệu làm nguồn Assignment. Không câu CODE hoặc lời giải/test code. Rubric thuộc từng Assignment/phần, khóa lúc phát hành, giữ snapshot; không tự thêm thực thể template cấp môn mới.

### FR-017 - Hai loại bài và cấu hình hợp lệ

ASSIGNMENT: submissionMode INDIVIDUAL/GROUP, diagramEnabled true/false; tài liệu gồm heading/paragraph/list/table/image và DIAGRAM chỉ khi bật. Giảng viên cấu hình khung/rubric, yêu cầu sơ đồ nếu có. QUIZ: câu hỏi/đáp án, điểm, đảo câu/đáp án, số lượt, thời gian và khả năng xem đáp án theo chính sách.

**Tiêu chí chấp nhận:**
- Hai enum sản phẩm, không TEXT_ESSAY/DIAGRAM_ESSAY/GROUP_ASSIGNMENT/CODE_LAB như type riêng.
- Tắt Draw.io thì không tạo/nhập block diagram; bật thì bảo toàn XML đầy đủ và kiểm hợp lệ.
- Văn bản thuần dùng cùng document workspace, không bắt buộc có diagram.
- Sinh viên không sửa đề/rubric/khung đã khóa; nội dung được phép sửa gắn với phần bài làm.
- Quiz hoạt động độc lập Lesson. Không CodeRunner/kiểm lời giải mẫu trước phát hành.

### FR-018 - Lưu nháp, lần nộp và khôi phục bài làm

Hệ thống phải tự động lưu bản nháp theo người học/bài đánh giá, khôi phục an toàn sau gián đoạn và lưu lịch sử các lần nộp; bản nháp không được coi là bài nộp chính thức.

### FR-019 - Theo dõi nộp bài và nhắc nhở

Giảng viên phải xem được trạng thái đã nộp, chưa nộp, đang làm và nộp trễ của lớp được phân công. Hệ thống tự nhắc người học chưa nộp 24 giờ trước hạn (một lần mỗi bài); giảng viên không gửi nhắc thủ công.

### FR-020 - Chốt điểm hàng loạt

Giảng viên phải có thể kiểm tra và chốt điểm hàng loạt cho lớp được phân công; chỉ bài đã đủ điều kiện mới được chốt và mọi thay đổi điểm phải được audit.

### FR-021 - Quản trị kết nối và usage AI

Admin cấu hình kết nối API chung, provider/model, kill-switch, quota giờ/ngày và theo dõi usage tổng hợp. Không vào bài/học liệu để cấu hình. Kết nối cá nhân chỉ chủ tài khoản quản lý, Admin không xem key hoặc dùng thay người dùng.

**Tiêu chí chấp nhận:**
- API chung dành cho giảng viên đủ quyền, không Student; quota hết thì báo và cho người dùng chủ động chọn personal API/MCP.
- Usage direct API lấy từ provider; MCP external có thể chỉ biết tool-call, không giả định biết model token thật.
- Mọi sửa cấu hình, cấp/đổi quota, thu hồi kết nối có audit nhưng không ghi secrets.

### FR-022 - Đã rút khỏi phạm vi: Tự ghi danh bằng mã mời

Người dùng chốt bỏ ngày 2026-10-09 vì không có trong 73 UC; sinh viên chỉ vào lớp khi Chủ nhiệm môn thêm (UC 51). Mã FR-022 giữ làm dấu vết và không dùng lại.

### FR-023 - Thông báo lớp

Teacher của lớp (hoặc Subject Manager được giao dạy, R4) tạo/cập nhật/xóa thông báo (UC 36); Student ghi danh và giảng viên của lớp xem (UC 30) trên Class Announcements. Màn mở từ Class Dashboard, gộp thông báo của các lớp mình học hoặc dạy và lọc được theo lớp.

**Tiêu chí chấp nhận:**

- Chỉ tài khoản được giao dạy lớp tạo/sửa/xóa; Student không đăng thông báo; Admin không vào lớp.
- Tạo mới gửi in-app notification cho Student đang ghi danh; giảng viên có thể đánh dấu quan trọng và chọn email theo FR-011; lưu actor/thời gian. Sửa kiểm version, audit trước/sau, không gửi lại sự kiện tạo mới.
- Xóa đánh dấu đã xóa và loại khỏi feed, giữ audit/tham chiếu.
- Đọc/sửa/xóa ngoài lớp bị từ chối; kiểm giới hạn và đầu vào không an toàn.
- Không có bình luận dưới thông báo (người dùng chốt bỏ 2026-10-09).

### FR-024 - Thống kê quản trị và xuất bảng điểm

Administrator xem Admin Dashboard (UC 58): số tài khoản theo role/status, môn/lớp theo status và enrollment ACTIVE; bổ sung mục Backup gồm lần thành công gần nhất, trạng thái/lịch sử, thời gian, dung lượng và lỗi đã sanitize qua public port U03. Không dữ liệu học vụ cá nhân, tệp backup hoặc dashboard Student; lịch/retention được chỉnh tại UC 71, không Dashboard.

Teacher hoặc Subject Manager được giao dạy lớp R3/R4 xem/xuất GradeBook CSV/XLSX (UC 40). Kiểm toàn bộ bộ lọc trước khi tạo tệp; ghi rõ chưa nộp/chưa chốt, không tính điểm tổng, không xuất quiz hoặc Practice; Admin không có quyền.

Phân bố điểm ẩn danh là luồng phụ của Student Assignments (UC 22): chỉ Chủ nhiệm môn bật/tắt cho lớp trong Edit Class Information (UC 52), mặc định tắt; chỉ bài GRADED đủ mẫu riêng tư, không quiz/Practice, bài của môn tính riêng từng lớp. Tiến độ nộp hiện trên Student Submissions (UC 37), chỉ R3/R4 của lớp. AI Grading Proposals thuộc UC 38, chấm/chốt điểm thuộc UC 39, công bố/xem/xuất Gradebook thuộc UC 40. Phân tích chất lượng câu hỏi/độ lệch điểm AI nằm ngoài phạm vi.

### FR-025 - Quản lý nhóm và trưởng nhóm

Trong danh sách sinh viên của lớp được phân công, giảng viên phải có thể chia sinh viên thành nhiều nhóm bằng cách tạo tay hoặc chia ngẫu nhiên các sinh viên chưa có nhóm, và chỉ định chính xác một trưởng nhóm cho mỗi nhóm. Nhóm thuộc lớp và được dùng cho mọi bài nhóm của lớp; không có chức năng dùng lại nhóm của bài khác. Thành viên có thể gửi yêu cầu đổi trưởng nhóm nhưng chỉ giảng viên được phê duyệt/từ chối và chỉ định người thay thế.

**Tiêu chí chấp nhận:**

- Mỗi nhóm luôn có đúng một trưởng nhóm đang hiệu lực trước khi nhận bài nhóm.
- Chỉ sinh viên đang ghi danh trong lớp mới được thêm vào nhóm của lớp đó; mỗi sinh viên thuộc tối đa một nhóm trong lớp.
- Chia ngẫu nhiên chỉ chia sinh viên chưa có nhóm, giữ nguyên nhóm đã có, các nhóm mới chênh nhau tối đa một người và giảng viên xem trước trước khi lưu.
- Một thay đổi trưởng nhóm chỉ có hiệu lực sau quyết định của giảng viên và được audit.

### FR-026 - Assignment GROUP: tài liệu chung và đóng góp

GROUP là cách làm của Assignment, không loại bài thứ ba. Giữ nhóm thuộc lớp, một leader; leader giao phần hoặc thành viên nhận phần, một editor/phần tại một thời điểm. Workspace hỗ trợ tài liệu thuần hoặc bật Draw.io theo diagramEnabled.

**Tiêu chí chấp nhận:**
- Thành viên chỉnh phần được giao/nhận; leader nộp, version-check, nội dung sau nộp bất biến.
- Giữ lịch sử tác giả/thời điểm; giảng viên đánh giá đóng góp, không tự suy ra điểm từ số thao tác.
- Manual submit/auto-submit theo quy tắc đã chốt của baseline cũ, không đổi vì gộp type.
- GROUP vẫn GRADED cho đến D02 được chốt; quyền Student gửi toàn bài nhóm cho AI chờ D03, mặc định chỉ phần mình được sửa.

### FR-027 - Assignment cấp môn

Chủ nhiệm môn R2 soạn/duyệt/phát hành Assignment INDIVIDUAL cho các lớp OPEN của môn theo lịch chung; giảng viên từng lớp chấm lớp mình. Draw.io tùy chọn. GROUP thuộc lớp, không tự mở cấp môn. Quiz theo phạm vi lớp/môn được phép, tách khỏi Lesson.

### FR-028 - Sao chép và phiên bản Assignment/Quiz

Giữ identity/lineage/version; copy trong scope tạo nội dung và rubric độc lập. Sao cấu hình INDIVIDUAL/GROUP và diagramEnabled nhưng không sao lịch, nhóm/thành viên, attempt, bài nộp, phản hồi hoặc điểm. Chỉ hai loại bài, kiểm mode hợp lệ.

### FR-029 - Đã rút khỏi phạm vi: Simulation Exam

Theo câu trả lời làm rõ ngày 2026-09-29, Simulation Exam được bỏ hoàn toàn khỏi MVP; mã FR-029 giữ làm dấu vết và không dùng lại.

### FR-030 - Phản hồi bài Practice đã nộp bằng API cá nhân/MCP

Student chỉ yêu cầu phản hồi trên Assignment PRACTICE đã nộp (UC29), bằng API cá nhân hoặc agent MCP; không dùng quota chung. Feedback phục vụ lần luyện tập tiếp theo, không sửa snapshot cũ hoặc tạo điểm official.

**Tiêu chí chấp nhận:**
- Trước dispatch hoặc ghi kết quả API/MCP, server kiểm owner/current scope, SUBMITTED, PRACTICE và immutable submissionVersion/hash.
- Draft/IN_PROGRESS, Graded và dữ liệu ngoài quyền bị từ chối; không có teacher opt-in cho feedback trước nộp. UC79 retired.
- Lưu lịch sử nhiều feedback hợp lệ với provenance/rate limits/idempotency; stale version không apply nhầm hoặc ghi đè bài nộp/điểm.
- Không xuất key, đáp án ẩn hoặc dữ liệu người khác; không auto-call mỗi keystroke.
- Student thiếu kết nối vẫn viết/lưu/nộp được. D03 giữ giới hạn ngữ cảnh nhóm, không tự mở toàn bài.

### FR-031 - Đã rút: quản trị gói credit

Không có gói bán hoặc giá token; ID giữ lịch sử. Kết nối chung và quota thuộc FR-034/036.

### FR-032 - Đã rút: lịch sử thanh toán

Không PayOS, payment history, webhook hoặc wallet. Lịch sử usage không phải giao dịch thanh toán; FR-036.

### FR-033 - Settings và kết nối AI

UC70 View Settings và UC71 Edit Setting là hai chức năng Admin riêng; cùng màn cấu hình chứa non-secret policies, quota qua U07 và API chung qua U13 protected connection port. U03 primary UI/orchestration, không đọc/ghi repository U07/U13 hoặc lưu raw key trong Settings. UC76/78/82 consolidated, không còn chức năng độc lập; API cá nhân UC74 và MCP UC75 giữ riêng. Edits validate version/audit; synthetic test không cấp quyền học vụ.

### FR-034 - Quota token API chung của giảng viên

Admin cấp hạn mức token cho Teacher/Subject Manager ACTIVE theo giờ/ngày trong UC 71 (UC70 chỉ xem); U03 điều phối tới U07 qua public port với kiểm quyền/version/audit riêng cho quota. Quota không mua bán, không cộng vào ví và không cấp Student.

**Tiêu chí chấp nhận:**
- Kiểm và reserve ngân sách ước tính trong transaction trước provider call; khóa/cập nhật nguyên tử để request đồng thời không vượt reservation.
- Chốt usage thật khi provider trả, giải phóng phần chưa dùng; request lặp không tính hai lần. Usage chưa biết phải UNKNOWN/PENDING_RECONCILIATION, không ghi 0 giả hoặc retry miễn phí vô hạn.
- Cửa sổ giờ/ngày tính theo Asia/Ho_Chi_Minh, không rollover. Mức cụ thể D04 chưa chốt.
- Quota hết không tự dùng personal key; giảng viên chọn nguồn khác hoặc tiếp tục thao tác thủ công.
- Token giữa model/provider không là một đơn giá tiền; lưu provider/model riêng và trần vận hành toàn hệ thống.

### FR-035 - MCP tools của ALAP

MCP server cung cấp tools đọc đề/rubric/tài liệu/bài làm đúng quyền, ghi bản nháp và lưu phản hồi/proposal. MCP không là proxy mặc định gọi AI chung và không đọc trực tiếp database bỏ qua domain services.

**Tiêu chí chấp nhận:**
- Credential MCP gắn actor, scope, expiry, có thu hồi; mỗi call tái kiểm quyền tài nguyên.
- Tool trả tối thiểu dữ liệu cần; dữ liệu ngoài scope, key, đáp án ẩn không xuất.
- Ghi draft yêu cầu expectedVersion và trạng thái editable; submit bài/điểm official không nằm trong tools giai đoạn đầu.
- Student không đọc bài người khác; Teacher không lớp ngoài phân công; response AI và agent là input không tin cậy.
- Audit tool-call actor/resource/action/outcome, không prompt/key thô. Token agent nếu có chỉ là externallyReported, không dùng trừ quota chung.

### FR-036 - Minh bạch nguồn AI, token và provenance

Direct API lưu usage từ provider (input/output, cache/reasoning nếu có), model, task, owner, shared/personal connection, version và timestamp. Không chuyển token thành credit hoặc giao dịch tiền.

**Tiêu chí chấp nhận:**
- Trước gọi AI hiển thị nguồn, bên chịu chi phí, quota còn lại nếu áp dụng và ước lượng có nhãn; hết quota không tự dùng personal API.
- Chủ tài khoản xem usage của mình; Admin xem số liệu quota chung không key/bài riêng.
- Không bán credit/token. Nếu quy đổi chi phí thì chỉ ước tính với nguồn giá/ngày hiệu lực; không là hóa đơn. MCP thiếu usage ghi unavailable, không 0.
- Phân biệt providerMeasured/estimated/externallyReported/unavailable; giữ raw usage được sanitize để đối chiếu.
- MCP không thấy lời gọi LLM bên ngoài thì chỉ ghi tool metadata; không công bố token chính xác giả.
- Không hứa exactly-once lời gọi provider khi timeout; checkpoint/idempotency bảo vệ dữ liệu nội bộ.

### FR-037 - Hướng dẫn và thử kết nối

Có hướng dẫn kết nối API cho Gemini/OpenAI/Claude và hướng dẫn MCP cho từng ứng dụng agent thực sự được thử: prerequisites, xác thực, cấu hình, test tool, ngắt kết nối và xử lý lỗi. Không hướng dẫn nhập key vào prompt; test connection bằng nội dung tổng hợp, hiển thị có thể phát sinh usage. Không cam kết một gói chat luôn bao gồm API.

### FR-038 - Backup và khôi phục dữ liệu lên Google Drive

U03 backup PostgreSQL nhất quán và bản sao tệp/manifest tương ứng lên thư mục Drive riêng tư, tách lưu trữ đang dùng; Admin xem trạng thái/lịch sử (UC 58) và chỉnh lịch/retention qua Settings. Archive mã hóa, khóa giải mã giữ riêng; không export secrets dạng rõ.

**Tiêu chí chấp nhận:**
- Chỉ SUCCESS khi dump/tệp/upload/checksum đầy đủ; lỗi không xóa bản tốt gần nhất, retry hữu hạn, phát failure event cho U16.
- Tệp trên Drive đang dùng không tự coi là backup; giữ bản độc lập để khôi phục sau xóa nguồn.
- Người vận hành thử restore môi trường cô lập: quyền, tài liệu/XML, bài nộp/điểm và quota/usage; ghi bằng chứng và RPO/RTO đo được. Không có nút restore production/MCP restore trong MVP.

### FR-039 - Ghi nhận dấu hiệu rời trang khi làm bài

Giảng viên tùy chọn activityMonitoringEnabled cho Assignment/Quiz, mặc định tắt; policy snapshot và thông báo cho Student. U11 ghi sự kiện hidden/visible cho cá nhân/Quiz và nhận sự kiện nhóm qua port U14; U15 hiển thị cho giảng viên được giao dạy trong UC 37 — View Student Submissions, qua phần chi tiết bài làm/lượt Quiz được phép. Quiz Practice không có điểm official/Gradebook; quyền review activity không cho phép sửa kết quả Quiz hoặc bỏ qua policy/phân công lớp.

**Tiêu chí chấp nhận:**
- Server kiểm actor/enrollment/attempt/group/state, dedup và giới hạn tần suất; không thu thập khi policy off.
- Hiển thị lần/thời gian rời trang và khoảng dữ liệu thiếu; không cộng trùng blur/visibility hoặc tính iframe Draw.io là rời trang.
- Chỉ dữ liệu trong quyền, nhóm phân biệt từng thành viên; retention cấu hình/thông báo trước triển khai.
- Client event là tín hiệu không đáng tin tuyệt đối. Không tự kết luận gian lận, trừ điểm hoặc nộp bài; không webcam/screenshots/chặn ứng dụng ngoài.

## 5. Luồng chính

1. Admin quản trị tài khoản/môn/API chung/quota; Subject Manager triển khai lớp/ghi danh.
2. Teacher tạo Assignment INDIVIDUAL/GROUP và diagramEnabled, khung/rubric; hoặc Quiz không bắt buộc Lesson; review/publish.
3. Student làm trong workspace, autosave, nhận phản hồi cá nhân nếu policy cho phép, sửa/nộp; nhóm dùng section ownership và leader submit.
4. Teacher chấm tay hoặc AI proposal, chốt/publish; Student chỉ nhận kết quả được phép.
5. Người dùng chủ động cấu hình personal API hoặc MCP; shared quota chỉ educator. Mọi đường kiểm quyền như nhau.

## 6. Yêu cầu phi chức năng và kiểm chứng

- Giữ auth/session/OTP, input/file/XML validation, snapshot, concurrency/version check, private file và append-only audit theo các quyết định không mâu thuẫn trước đây. Construction cũ là tham chiếu kỹ thuật chưa đồng bộ, không nguồn scope mới.
- Kết nối cá nhân: bảo vệ secret khi lưu, mask output, thu hồi, không log; chỉ provider/endpoint hợp lệ, không biến cấu hình thành proxy tùy ý.
- Timeout/retry hữu hạn, job idempotent, không mất bài khi provider lỗi; không coi lỗi/quota là bài đã chấm.
- Test 4 tổ hợp Assignment, Quiz không Lesson, deny Student shared quota, quota cạnh tranh, provider usage unknown, revoke connection, MCP scope/version và stale feedback.
- Không đặt SLA mới hoặc performance số chưa đo. Mục tiêu benchmark cần thiết kế/kiểm thử downstream.

## 7. Tiêu chí NFR/security/operations kế thừa

### NFR-001 - Công nghệ

- Frontend: Next.js với TypeScript.
- Backend: Java với Spring Boot.
- Giao tiếp frontend-backend qua API web có versioning hoặc quy ước tương thích rõ ràng.
- Database PostgreSQL và hạ tầng hiện có giữ nguyên; direct AI adapters Gemini/OpenAI/Claude theo FR-012; embedding không bắt buộc trong MVP.

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
- System test phải kiểm thử toàn diện sự phối hợp giữa Frontend (Next.js), Backend (Spring Boot), Database và các mock/sandbox của dịch vụ bên ngoài (AI provider và MCP client/server) trên môi trường container trước khi triển khai.
- Contract test bao phủ ba provider adapters và MCP tools/auth, quota concurrency, revocation và version feedback; không payment/webhook.
- Không áp dụng Property-Based Testing theo quyết định của người dùng.

### NFR-005 - Môi trường chạy

- Môi trường phát triển và demo phải chạy local bằng container với hướng dẫn tái tạo được.
- Secret triển khai truyền qua môi trường/secret store; API key cấu hình trong app lưu ở trường credentials được bảo vệ phía server, không raw Settings, không commit hoặc log. Không tự yêu cầu mã hóa toàn database; protected connection secrets là yêu cầu riêng.
- Image và dependency phải khóa phiên bản; không dùng tag `latest` cho artifact triển khai.

## 7. Yêu cầu bảo mật và quyền riêng tư

Phạm vi rút gọn cho đồ án sinh viên, chỉ giữ các control rẻ để làm và cần có. Giữ phạm vi extension rút gọn ở state; điều chỉnh boundary MCP/API key như FR-012/035.

### SEC-001 - Mật khẩu và phiên

Mật khẩu băm bằng bcrypt, tối thiểu 8 ký tự có chữ và số. Sai 5 lần thì khóa tạm. Token nằm trong cookie `HttpOnly`, `Secure`, `SameSite`, có hạn và bị thu hồi khi đăng xuất. Không có MFA, không kiểm danh sách mật khẩu bị lộ.

### SEC-002 - Phân quyền

Mọi API mặc định yêu cầu đăng nhập, trừ các endpoint được đánh dấu public. Quyền được kiểm phía server ở mức chức năng và đối tượng; frontend ẩn nút chỉ để tiện dùng.

### SEC-003 - Kiểm tra đầu vào

Mọi request body và tham số được validate kiểu, độ dài và định dạng. Truy vấn database luôn tham số hóa. Endpoint public có giới hạn tần suất.

### SEC-004 - Header HTTP

Nginx/Next.js phối hợp CSP tương thích script nonce và iframe Draw.io đã cho phép, `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`. Truy cập từ Internet dùng HTTPS với chứng chỉ Let's Encrypt miễn phí; dùng tên miền có sẵn, nếu chưa có thì dùng subdomain miễn phí (DuckDNS).

### SEC-005 - Log

Log không chứa mật khẩu, OTP, token hay dữ liệu cá nhân nhạy cảm. Sự kiện đăng nhập thất bại, đổi quyền và thao tác đặc quyền được ghi audit.

### SEC-006 - Xử lý lỗi và cấu hình an toàn

Có global error handler; phản hồi lỗi không lộ stack trace, đường dẫn hay chi tiết database. Không có mật khẩu mặc định. Tắt Swagger và endpoint debug ở production. Secret không commit vào repository. Dependency khóa phiên bản.

### SEC-007 - Retired: thanh toán

Không thuộc scope mới; giữ ID lịch sử. Request AI/quota/MCP có idempotency theo FR-034/035/036, không kế thừa webhook payment.

## 8. Yêu cầu vận hành

### REL-001 - Triển khai

Chạy trên VPS nhóm đã có sẵn bằng Docker Compose; không dùng dịch vụ trả phí mới. Image gắn tag theo commit; rollback bằng cách chạy lại tag trước. Migration database tương thích ngược.

### REL-002 - Health check

Mỗi container có healthcheck trong Docker Compose; backend có `/health`.

### REL-003 - Timeout

Mọi lời gọi ra ngoài (database, Redis, SMTP, AI, MCP và unit khác) có timeout hữu hạn và retry có giới hạn. Lỗi phụ thuộc thì từ chối an toàn.

### REL-004 - Backup tối thiểu và giới hạn vận hành

Backup tối thiểu và kiểm thử restore thuộc FR-038; cảnh báo backup thất bại thuộc FR-011. Không mở rộng sang multi-zone/auto-scaling, DR tự động hoặc monitoring stack. Admin Dashboard vẫn thuộc MVP; log xem bằng docker compose logs. RPO/RTO phải đo từ cấu hình và kiểm thử, chưa được nghiệm thu.

### REL-005 - Không phát sinh chi phí

Mọi thành phần bảo mật và vận hành phải miễn phí: thư viện mã nguồn mở, Let's Encrypt, GitHub Actions và GHCR với repository public, Mailpit khi phát triển, Brevo SMTP khi demo/production (trong hạn mức gói miễn phí).

AI API chung do đơn vị vận hành cung cấp/tài trợ có quota và kill-switch; personal API/agent theo tài khoản người dùng. Không mặc định API miễn phí hoặc ngân sách trường đã có. Embedding/RAG chỉ extension với nguồn chi phí riêng.


## 8. Security/resiliency và downstream

Giữ SECURITY-03/04/05/08/09/12/15 và RESILIENCY-04/06/10 theo phạm vi đã chọn; Property-Based Testing disabled. Áp dụng cho input/API/MCP, secret, audit, auth và lỗi an toàn ở cấp tài liệu; runtime chưa kiểm chứng. Backup/activity/email bổ sung không tự kích hoạt extension khác; archive bảo vệ dữ liệu, activity tối thiểu và kiểm quyền theo phạm vi đã chọn.

Construction design/shared infrastructure/code planning đã revision theo scope mới. Code/contracts implementation/migrations, Google Reports và diagram cũ cần revision riêng trước chạy scope mới. Không sửa tiến độ/approvals cũ thành hoàn thành mới.

## UC consolidation — 65 active UC

UC 70 View Settings và UC 71 Edit Setting là hai UC riêng, primary U03; bao gồm UI cấu hình quota/API chung. U07 giữ quota service/data; U13 giữ connection/provider/credentials bảo vệ và test synthetic qua public port. UC 76/78/82 không còn độc lập. UC 58 gộp 80 (U16 dashboard, U03 backup); UC 37 gộp 81 (U15 review, U11/U14 activity). UC 42 soạn Individual, UC 45 soạn Group (U09); Draw.io tùy chọn trong UC 24/27, UC 26 retired. UC 79 retired; UC 29 chỉ feedback Assignment Practice đã nộp. 50 MVP + 1 extension story, 15 units; không tự phê duyệt D01–D04 hoặc runtime.

### Tiêu chí tích hợp UC đã gộp

- UC70/71: chỉ Admin; UC70 chỉ xem, UC71 sửa/test/revoke; cập nhật Settings và quota là hai thao tác có owner/version/audit riêng, không hứa transaction nguyên tử xuyên unit. Quota service chưa sẵn sàng hiển thị unavailable, không báo cập nhật thành công giả. Institutional credentials are viewed/edited through UC70/71 via U13 protected connection records, not ordinary Settings plaintext.
- UC 58: U16 đọc BackupStatusPort của U03; không đọc repository hoặc trả archive/secrets. Khi lỗi port hiển thị unavailable/last-known timestamp, không ghi SUCCESS hoặc không có backup giả.
- UC 37: U15 đọc ActivityQueryPort của U11 sau kiểm R3/R4, attempt/class/member và policy; policy off hiển thị monitoring disabled, không có sự kiện hiển thị no recorded data, lỗi port hiển thị unavailable. Không coi các trạng thái này là không gian lận hoặc 0 lần chắc chắn.

Construction current docs: [README](../../construction/README.md), [sync plan](../../construction/plans/construction-66-uc-sync-2026-10-13.md). Stage approvals/code progress không thay vì revision tài liệu.
