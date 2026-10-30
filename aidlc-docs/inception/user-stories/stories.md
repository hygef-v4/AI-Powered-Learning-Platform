# User Stories — Inception revision 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime. D01–D04 đã chốt 2026-10-15.

**50 MVP stories + 1 extension story**, 66 active UC, 15 logical units. IDs giữ lịch sử; US-ASM-005 và US-PAY-* retired; US-ASM-007 gộp tài liệu thuần vào US-ASM-004.

Quy tắc chung: server kiểm R1–R5/ownership/state/version cho web/API/MCP; chỉ ASSIGNMENT/QUIZ; Student không shared quota; AI không tự official decisions. Đã chốt 2026-10-15: Quiz chỉ Practice (D01), GROUP chỉ Graded (D02), bài nhóm không có AI cho Student (D03 bỏ), quota chung theo ngày và trần hệ thống (D04). Chưa có bằng chứng survey.

### US-AIC-001 - Kết nối API cá nhân

**Primary unit**: U06; **Scope**: MVP.

**Current UC trace**: UC 31. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Student/educator, tôi quản lý API Gemini/OpenAI/Claude của mình.

**Truy vết**: FR-012/037. Primary unit U06.

#### Scenario 1

Given owner, When add/test/replace/revoke, Then key protected/masked và không chia sẻ sang user khác.

#### Scenario 2

Given Student, When chọn nguồn, Then personal API hoạt động đúng quyền và shared API bị chặn.

### US-AIC-002 - Kết nối agent riêng qua MCP

**Primary unit**: U06; **Scope**: MVP.

**Current UC trace**: UC 32. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Student/Teacher/Subject Manager ACTIVE, tôi kết nối agent với tool đúng role/scope. Student dùng MCP hoặc API cá nhân cho UC41; Admin không dùng MCP.

**Truy vết**: FR-035/037. Primary unit U06.

#### Scenario 1

Given authenticated actor và scoped credential, When tool read/write, Then kiểm scope/state/version ở domain backend.

#### Scenario 2

Given tool ngoài quyền hoặc revoke, When gọi, Then deny; không tool tự publish/chốt điểm.

#### Scenario 3

Given external agent, When ghi usage, Then chỉ reported/unavailable, không đo token LLM giả.

### US-AIC-003 - API chung và quota educator

**Primary unit**: U06; **Scope**: MVP.

**Current UC trace**: UC 14; UC 15. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Admin/educator, tôi cấp và sử dụng hạn mức token chung.

**Truy vết**: FR-021/034. Primary unit U06.

#### Scenario 1

Given educator đúng quyền, When reserve đồng thời, Then quota atomic theo ngày theo cấu hình, không Student.

#### Scenario 2

Given hết quota, When request, Then báo hết và không auto-fallback personal; không buộc mua token.

#### Scenario 3

Given usage response hoặc timeout, When settle, Then true usage/unknown được phân biệt và không double-count.

#### Scenario 4 - Truy vết quota sau gộp UC

- **Given** Admin mở UC 14 View Settings / UC 15 Edit Setting
- **When** xem hoặc chỉnh quota một giảng viên
- **Then** UI thuộc UC 14 nhưng policy/data/reservation vẫn do U06 sở hữu; giá trị và kết quả được kiểm version/audit, không còn UC 78 (ID cũ) độc lập.



### US-AIC-004 - Lịch sử usage theo nguồn

**Primary unit**: U06; **Scope**: MVP.

**Current UC trace**: UC 33. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là user, tôi xem lượng dùng AI và provenance của mình.

**Truy vết**: FR-036. Primary unit U06.

#### Scenario 1

Given own usage, When xem, Then provider/model/source/token biết thật hiển thị; không key hoặc nội dung người khác.

#### Scenario 2

Given MCP không cung cấp usage, When xem, Then unavailable/tool metadata, không 0 giả hoặc quota shared deduction.

#### Scenario 3 - Minh bạch trước khi dùng AI

- **Given** người dùng chọn nguồn shared/personal/MCP
- **When** xem xác nhận và lịch sử sử dụng
- **Then** thấy nguồn/bên chịu chi phí, quota nếu áp dụng, phân biệt measured/estimated/reported/unavailable; không bán token, không coi thiếu usage MCP bằng 0, không tự gọi personal khi hết shared quota.


### US-AIG-001 - AI soạn bản nháp Assignment/Quiz lớp

**Primary unit**: U06; **Scope**: MVP.

**Current UC trace**: UC 48; UC 35; UC 53. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Teacher, tôi chọn shared/personal API hoặc agent MCP để hỗ trợ soạn bài.

**Truy vết**: FR-006/012/034/035. Primary unit U06.

#### Scenario 1

Given đúng scope và nguồn chọn hợp lệ, When yêu cầu, Then tạo draft/provenance không tự publish.

#### Scenario 2

Given hết quota chung, When tiếp tục, Then chỉ personal sau lựa chọn rõ; vẫn soạn thủ công được.

### US-AIG-002 - AI soạn bài cấp môn

**Primary unit**: U06; **Scope**: MVP.

**Current UC trace**: UC 48; UC 35. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Subject Manager, tôi dùng AI hỗ trợ Assignment INDIVIDUAL/Quiz cấp môn trong R2.

**Truy vết**: FR-002/006/027. Primary unit U06.

#### Scenario 1

Given R2, When chọn nguồn, Then chỉ tài liệu môn và bản nháp đúng quyền được xử lý.

#### Scenario 2

Given không được giao dạy lớp, When yêu cầu chấm lớp, Then bị từ chối kể cả có API cá nhân.

### US-AIG-003 - Vận hành AI chung

**Primary unit**: U06; **Scope**: MVP.

**Current UC trace**: UC 14; UC 15. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Admin, tôi cấu hình API chung và provider/model, kiểm tra kết nối và giám sát trạng thái.

**Truy vết**: FR-021/033/037. Primary unit U06.

#### Scenario 1

Given Admin, When test connection, Then dùng synthetic payload, bảo vệ key và không cấp quyền học vụ.

#### Scenario 2

Given sửa/thu hồi cấu hình, When request mới, Then dùng cấu hình hiện hành và audit không chứa secret.


#### Scenario - Settings API delegation

Given authenticated Admin, When open View Settings or use Edit Setting for institutional connection configuration/test/revoke, Then U03 delegates to U06; responses are masked and version/audit verified, failures never report saved. No academic access is granted.

### US-ASM-001 - Duyệt và phát hành Assignment/Quiz

**Primary unit**: U07; **Scope**: MVP.

**Current UC trace**: UC 48; UC 34; UC 35; UC 53. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là educator có quyền, tôi soạn và duyệt bài trước khi giao.

**Truy vết**: FR-007/017/028. Primary unit U07.

#### Scenario 1

Given ASSIGNMENT, When cấu hình, Then INDIVIDUAL/GROUP và diagramEnabled độc lập, đủ bốn tổ hợp.

#### Scenario 2

Given QUIZ, When publish, Then không yêu cầu Lesson; Quiz chỉ Practice (D01).

#### Scenario 3

Given published, When sửa nội dung, Then phải dùng version/copy theo policy, không sửa snapshot cũ.

### US-ASM-003 - Làm và nộp bài cá nhân/Quiz

**Primary unit**: U08; **Scope**: MVP.

**Current UC trace**: UC 49; UC 50; UC 51; UC 52; UC 37; UC 38; UC 39; UC 40. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Student, tôi làm tài liệu hoặc Quiz và nộp theo policy.

**Truy vết**: FR-007/008/018. Primary unit U08.

#### Scenario 1

Given Assignment INDIVIDUAL đúng quyền, When làm/lưu/nộp, Then version/snapshot/receipt được giữ.

#### Scenario 2

Given Quiz, When nộp, Then chấm đáp án snapshot; không Lesson hoặc AI vẫn làm được.

#### Scenario 3

Given hết hạn/ngoài quyền, When submit API/MCP, Then từ chối an toàn; không thực thi code.

### US-ASM-004 - Assignment tài liệu với Draw.io tùy chọn

**Primary unit**: U07; **Scope**: MVP.

**Current UC trace**: UC 35; UC 53; UC 39; UC 56. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là educator/Student, tôi dùng workspace tài liệu cho Assignment thuần hoặc có sơ đồ.

**Truy vết**: FR-017/026. Primary unit U07.

#### Scenario 1

Given diagramEnabled=false, When edit/import, Then văn bản/table/image được xử lý và diagram bị từ chối.

#### Scenario 2

Given diagramEnabled=true, When Draw.io edit/submit, Then XML đầy đủ và preview lưu với tài liệu, kiểm XML an toàn.

#### Scenario 3

Given cá nhân/nhóm, When cấu hình khung/rubric, Then dùng cùng document model và không tạo type Essay/Diagram/Group riêng.

### US-ASM-006 - Quiz độc lập Lesson

**Primary unit**: U10; **Scope**: MVP.

**Current UC trace**: UC 48. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là educator, tôi tạo Quiz với câu/đáp án và chính sách làm bài.

**Truy vết**: FR-007/017. Primary unit U10.

#### Scenario 1

Given không có Lesson, When tạo Quiz, Then vẫn tạo/publish được; liên kết tài liệu tùy chọn.

#### Scenario 2

Given câu single/multiple choice, When nộp, Then chấm theo đáp án snapshot và visibility hợp lệ.

### US-ASM-008 - Version và ngừng giao bài

**Primary unit**: U07; **Scope**: MVP.

**Current UC trace**: UC 34; UC 35; UC 53. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là educator, tôi giữ lịch sử khi ngừng giao và tạo phiên bản mới.

**Truy vết**: FR-028. Primary unit U07.

#### Scenario 1

Given bài published có attempt, When retire/version, Then bài và rubric cũ không thay đổi.

#### Scenario 2

Given bản mới, When cấu hình, Then giữ type ASSIGNMENT/QUIZ và cấu hình mode/diagram hợp lệ.

### US-ASM-009 - Bài cấp môn

**Primary unit**: U07; **Scope**: MVP.

**Current UC trace**: UC 35. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Subject Manager, tôi giao Assignment cá nhân cho các lớp OPEN theo lịch chung.

**Truy vết**: FR-027. Primary unit U07.

#### Scenario 1

Given R2, When publish, Then giao các lớp theo scope; không GROUP cấp môn.

#### Scenario 2

Given từng lớp, When grade, Then chỉ Teacher/Subject Manager được giao dạy quyết định kết quả.

### US-ASM-010 - Copy bài và rubric

**Primary unit**: U07; **Scope**: MVP.

**Current UC trace**: UC 35; UC 53. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Teacher có quyền, tôi copy nội dung bài và rubric giữa lớp phù hợp.

**Truy vết**: FR-028. Primary unit U07.

#### Scenario 1

Given đúng quyền nguồn/đích, When copy, Then identity/lineage và rubric độc lập.

#### Scenario 2

Given copy, When lưu, Then không copy lịch, nhóm, kết nối AI, attempt, phản hồi và điểm.

### US-ASM-012 - Phản hồi sau nộp bài Practice và luyện tập nhiều lần

**Primary unit**: U08; **Scope**: MVP.

**Current UC trace**: UC 41. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Student, tôi dùng personal API/MCP để nhận phản hồi và cải thiện bài.

**Truy vết**: FR-030/035/036. Primary unit U08.

#### Scenario 1

Given Assignment PRACTICE của mình đã SUBMITTED, When request feedback, Then kiểm current scope/state và lưu submissionVersion/provenance, không dùng shared quota hoặc sửa snapshot bài nộp.

#### Scenario 2

Given Practice đã nộp, When request nhiều lần hợp lệ, Then lưu lịch sử riêng với rate limit, không giới hạn một result/attempt.

#### Scenario 3

Given draft/IN_PROGRESS, bài GRADED hoặc dữ liệu ngoài quyền, When web/MCP request hoặc ghi feedback, Then từ chối; không có teacher opt-in cho draft feedback và không đưa feedback vào Gradebook.

#### Scenario 4

Given AI lỗi/stale hoặc submissionVersion không khớp, When nhận kết quả, Then không apply sai snapshot và không thay đổi bài nộp/điểm.

### US-AUD-001 - Tra cứu audit nghiệp vụ và bảo mật

**Primary unit**: U02; **Scope**: MVP.

**Current UC trace**: UC 13. Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là quản trị viên, tôi muốn tra cứu sự kiện audit theo phạm vi và thời gian để điều tra thay đổi nhạy cảm mà không thể sửa lịch sử.

**Truy vết**: FR-002, FR-014, SEC-005, SEC-002, SEC-003.

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



### US-CAT-001 - Quản lý cấu trúc môn và lớp

**Primary unit**: U04; **Scope**: MVP.

**Current UC trace**: UC 24; UC 25; UC 26; UC 27; UC 30; UC 16; UC 17; UC 18; UC 19. Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là quản trị viên, tôi muốn tạo môn và giao môn cho Chủ nhiệm môn; là Chủ nhiệm môn, tôi muốn tạo lớp thuộc môn mình và giao giảng viên để cấu trúc học thuật phản ánh đúng hoạt động đào tạo.

**Truy vết**: FR-002, FR-003, FR-014, SEC-002, SEC-003, SEC-005.

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

**Primary unit**: U05; **Scope**: MVP.

**Current UC trace**: UC 22; UC 23; UC 24; UC 25; UC 29. Source: [current UC table](../../../docs/usecase_table.md).


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
- **When** sửa thông tin/vòng đời lớp (UC 29)
- **Then** backend từ chối; quyền xem lớp không đủ quyền sửa cấu trúc

#### Scenario 4 - Xem lớp được giao

- **Given** Teacher hoặc Subject Manager được giao dạy lớp
- **When** mở Class Dashboard rồi Teacher Class Detail
- **Then** chỉ liệt kê lớp được giao; tab Students chỉ xem danh sách và thông tin cơ bản của sinh viên; các lối vào học liệu, quiz, bài, bài nộp/gradebook theo quyền



### US-CAT-003 - Ghi danh người học

**Primary unit**: U05; **Scope**: MVP.

**Current UC trace**: UC 28. Source: [current UC table](../../../docs/usecase_table.md).


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



### US-CNT-001 - Tài liệu tham khảo cấp môn

**Primary unit**: U13; **Scope**: MVP.

**Current UC trace**: UC 62; UC 58; UC 59. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Subject Manager, tôi quản lý tài liệu tham khảo cho Assignment/Quiz của môn.

**Truy vết**: FR-004/005. Primary unit U13.

#### Scenario 1

Given có R2, When upload/gắn tài liệu hợp lệ, Then lưu riêng tư và lớp đúng môn xem theo quyền.

#### Scenario 2

Given không tài liệu/Lesson, When tạo bài, Then vẫn tạo Assignment/Quiz được.


#### Scenario - Educational YouTube links

Given authorized subject/class scope, When add/update/remove/list/open a valid teaching-video link, Then save title/url/version and preserve scoped/historical references; invalid links or unauthorized ALAP access are rejected. No transcription, summary or embedding is required.

### US-CNT-002 - Tài liệu tham khảo của lớp

**Primary unit**: U13; **Scope**: MVP.

**Current UC trace**: UC 62; UC 60; UC 61. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Teacher, tôi quản lý tài liệu của lớp được giao.

**Truy vết**: FR-004/005. Primary unit U13.

#### Scenario 1

Given có R3/R4, When thêm/sửa/archive tài liệu, Then kiểm scope/version và giữ lịch sử tham chiếu.

#### Scenario 2

Given người ngoài lớp gọi web/MCP, When yêu cầu đọc, Then bị từ chối không lộ tài liệu.


#### Scenario - Educational YouTube links

Given authorized subject/class scope, When add/update/remove/list/open a valid teaching-video link, Then save title/url/version and preserve scoped/historical references; invalid links or unauthorized ALAP access are rejected. No transcription, summary or embedding is required.

### US-CNT-004 - Thông báo trong lớp

**Primary unit**: U14; **Scope**: MVP.

**Current UC trace**: UC 63; UC 64. Source: [current UC table](../../../docs/usecase_table.md).


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

#### Scenario 6 - Thông báo quan trọng

- **Given** giảng viên lớp tạo thông báo mới và chọn gửi email quan trọng
- **When** commit thành công
- **Then** U14 gửi email tới người nhận hợp lệ theo preferences; sửa không tự gửi lại email tạo mới.


### US-CNT-005 - Xử lý Lesson/video và RAG nâng cao (extension)

**Primary unit**: U13; **Scope**: EXTENSION.

**Current UC trace**: Extension only; no extra committed MVP UC. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là educator, tôi có thể muốn xử lý học liệu nâng cao sau scope cốt lõi.

**Truy vết**: FR-004/012. Primary unit U13.

#### Scenario 1

Given extension chưa được chốt, When lập MVP, Then không coi xử lý video/caption/summary/embedding là prerequisite hoặc chức năng đã cam kết; link YouTube đơn thuần thuộc US-CNT-001/002 MVP.

#### Scenario 2

Given extension được chọn sau, When thiết kế, Then chốt provider embedding, nguồn chi phí và acceptance riêng.

### US-GRD-001 - Chấm Quiz xác định

**Primary unit**: U11; **Scope**: MVP.

**Current UC trace**: UC 52. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Student, tôi nhận kết quả Quiz theo đáp án.

**Truy vết**: FR-008/017. Primary unit U11.

#### Scenario 1

Given Quiz Practice đã nộp, When score, Then dùng answer snapshot không AI; không vào official Gradebook theo D01.

### US-GRD-002 - Đề xuất đánh giá Assignment

**Primary unit**: U09; **Scope**: MVP.

**Current UC trace**: UC 43. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Teacher, tôi yêu cầu AI hỗ trợ rubric qua nguồn được chọn.

**Truy vết**: FR-008/012/035. Primary unit U09.

#### Scenario 1

Given đúng teaching scope, When request API/MCP, Then proposal gắn submission/rubric version.

#### Scenario 2

Given sai format/stale, When nhận, Then reject không official grade; key cá nhân không nới quyền.

### US-GRD-003 - Chấm thủ công, duyệt và ghi đè đề xuất AI

**Primary unit**: U09; **Scope**: MVP.

**Current UC trace**: UC 43; UC 44. Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là giảng viên, tôi muốn chấm thủ công hoặc duyệt/ghi đè đề xuất AI và lưu thành điểm nháp để chịu trách nhiệm cho quyết định học thuật trước khi chốt và công bố (US-GRD-005).

**Truy vết**: FR-002, FR-008, FR-014, SEC-002, SEC-003, SEC-005.

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

**Primary unit**: U09; **Scope**: MVP.

**Current UC trace**: UC 37; UC 45. Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là người dùng, tôi muốn xem điểm, phản hồi và trạng thái bài nộp đúng phạm vi vai trò để theo dõi kết quả mà không lộ dữ liệu ngoài quyền.

**Truy vết**: FR-002, FR-005, FR-009, NFR-002, SEC-005, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Người học xem dữ liệu cá nhân

- **Given** kết quả đã được công bố
- **When** người học mở Student Assignments của lớp (UC 37)
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

**Primary unit**: U09; **Scope**: MVP.

**Current UC trace**: UC 44; UC 45. Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là giảng viên, tôi muốn chốt điểm nháp đã chấm (US-GRD-003) cho từng bài hoặc hàng loạt trong lớp được phân công rồi công bố để người học nhận kết quả nhất quán và có kiểm soát.

**Truy vết**: FR-002, FR-008, FR-009, FR-014, FR-020, SEC-002, SEC-003, SEC-005.

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



### US-GRP-001 - Chia lớp thành nhóm và chỉ định trưởng nhóm

**Primary unit**: U12; **Scope**: MVP.

**Current UC trace**: UC 57 — Create/Update/Delete Class Group (giảng viên tạo/chia/sửa/xóa nhóm); UC 54 — View My Group (Scenario 4). UC 23 là điểm truy cập Teacher Class Detail → Students → Groups. Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là giảng viên, tôi muốn chia sinh viên của lớp được phân công thành nhóm ngay trong danh sách sinh viên của lớp (tạo tay hoặc chia ngẫu nhiên) và chỉ định một trưởng nhóm cho mỗi nhóm để mọi bài tập nhóm của lớp có trách nhiệm rõ ràng.

**Truy vết**: FR-002, FR-003, FR-025, FR-014, SEC-002, SEC-003, SEC-005.

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

**Primary unit**: U12; **Scope**: MVP.

**Current UC trace**: UC 55 — Request Leader Change (thành viên gửi/hủy yêu cầu); UC 57 — Create/Update/Delete Class Group (giảng viên duyệt/từ chối hoặc đổi trưởng nhóm trực tiếp). Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là thành viên nhóm, tôi muốn gửi yêu cầu thay đổi trưởng nhóm để giảng viên xem xét khi phân công hiện tại không còn phù hợp.

**Truy vết**: FR-002, FR-011, FR-025, FR-014, SEC-002, SEC-003.

**Acceptance criteria**

#### Scenario 1 - Giảng viên phê duyệt

- **Given** người yêu cầu là thành viên nhóm và người được đề xuất cũng thuộc nhóm
- **When** giảng viên phê duyệt và xác nhận trưởng nhóm mới
- **Then** nhóm vẫn có đúng một trưởng nhóm, vai trò điều phối chuyển sang người mới và quyết định được audit mà không đổi các mục thành viên đang nhận trong tài liệu nhóm

#### Scenario 2 - Từ chối hoặc yêu cầu không hợp lệ

- **Given** yêu cầu ngoài nhóm, trùng yêu cầu đang chờ hoặc đề xuất người không thuộc nhóm
- **When** yêu cầu được gửi hoặc giảng viên từ chối
- **Then** trưởng nhóm hiện tại không thay đổi và người liên quan nhận trạng thái/lý do phù hợp



### US-GRP-003 - Soạn Assignment GROUP

**Primary unit**: U12; **Scope**: MVP.

**Current UC trace**: UC 53. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Teacher, tôi soạn tài liệu nhóm với diagramEnabled tùy chọn.

**Truy vết**: FR-007/017/026. Primary unit U12.

#### Scenario 1

Given lớp và nhóm hợp lệ, When soạn, Then khung/phần/rubric dùng document Assignment.

#### Scenario 2

Given diagramEnabled=false/true, When cấu hình, Then không bắt buộc nhóm có diagram; mode chỉ Graded (D02).

### US-GRP-004 - Nhận và làm phần Assignment nhóm

**Primary unit**: U12; **Scope**: MVP.

**Current UC trace**: UC 56. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là thành viên, tôi nhận phần và sửa nội dung theo quyền.

**Truy vết**: FR-026/030/035. Primary unit U12.

#### Scenario 1

Given phần được giao/nhận, When edit, Then một editor/phần, version và lịch sử tác giả được giữ.

#### Scenario 2

Given bài nhóm, When Student muốn dùng AI/MCP với tài liệu nhóm, Then không có tính năng AI cho bài nhóm; không lộ key hay bài khác.

### US-GRP-005 - Nộp tài liệu nhóm

**Primary unit**: U12; **Scope**: MVP.

**Current UC trace**: UC 56. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là leader, tôi phối hợp và nộp bài tài liệu chung.

**Truy vết**: FR-018/026. Primary unit U12.

#### Scenario 1

Given leader hiện hành, When nộp, Then kiểm hạn/state, giữ snapshot và receipt, các member không tự submit thay leader.

#### Scenario 2

Given hết hạn, When auto-submit, Then giữ quy tắc baseline cũ cho drafts, không phụ thuộc AI/quota.

### US-GRP-006 - Chấm nhóm và đóng góp

**Primary unit**: U12; **Scope**: MVP.

**Current UC trace**: UC 42; UC 44. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Teacher, tôi chấm tài liệu nhóm và điều chỉnh điểm thành viên.

**Truy vết**: FR-008/026. Primary unit U12.

#### Scenario 1

Given submission hợp lệ, When chấm rubric tay/AI proposal, Then dùng phần/rubric snapshot; Teacher quyết định.

#### Scenario 2

Given history đóng góp, When đánh giá, Then không tự suy ra cheating/điểm đóng góp bằng số thao tác.

### US-IAM-001 - Nhận và kích hoạt tài khoản trường cấp

**Primary unit**: U01; **Scope**: MVP.

**Current UC trace**: UC 01. Source: [current UC table](../../../docs/usecase_table.md).


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

**Primary unit**: U01; **Scope**: MVP.

**Current UC trace**: UC 02; UC 03. Source: [current UC table](../../../docs/usecase_table.md).


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
- **Then** phiên bị xóa phía server nên không làm mới được và access token còn lại bị từ chối ở request tiếp theo (BR-U01-44, 45)



### US-IAM-003 - Khôi phục mật khẩu riêng tư

**Primary unit**: U01; **Scope**: MVP.

**Current UC trace**: UC 04. Source: [current UC table](../../../docs/usecase_table.md).


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

**Primary unit**: U01; **Scope**: MVP.

**Current UC trace**: UC 06; UC 07. Source: [current UC table](../../../docs/usecase_table.md).


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

**Primary unit**: U01; **Scope**: MVP.

**Current UC trace**: UC 11; UC 12. Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là quản trị viên, tôi muốn gán hoặc thu hồi vai trò và phạm vi môn để người dùng chỉ có đúng quyền cần thiết.

**Truy vết**: FR-002, FR-003, FR-014, SEC-002, SEC-003, SEC-005.

**Acceptance criteria**

#### Scenario 1 - Gán Chủ nhiệm môn

- **Given** quản trị viên đã xác thực và một môn hợp lệ
- **When** quản trị viên gán vai trò Chủ nhiệm môn cùng phạm vi môn
- **Then** quyền cấp môn chỉ có hiệu lực với môn đó và thay đổi được ghi actor, thời gian, giá trị trước/sau; tài khoản Administrator không được gán làm Chủ nhiệm môn hay giảng viên lớp

#### Scenario 2 - Thu hồi quyền

- **Given** một quyền hoặc phạm vi đang có hiệu lực
- **When** quản trị viên thu hồi
- **Then** thu hồi phạm vi môn/lớp có hiệu lực ngay ở request tiếp theo; đổi role xóa mọi phiên của người đó, request tiếp theo bị từ chối và phải đăng nhập lại (BR-U01-63)

#### Scenario 3 - Người không phải quản trị viên thay đổi quyền

- **Given** người dùng không có quyền quản trị
- **When** người đó gọi chức năng phân vai
- **Then** hệ thống từ chối phía server và ghi sự kiện vi phạm authorization



### US-IAM-006 - Đổi mật khẩu cá nhân

**Primary unit**: U01; **Scope**: MVP.

**Current UC trace**: UC 05. Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là người dùng đã xác thực, tôi muốn đổi mật khẩu để chủ động bảo vệ tài khoản của mình.

**Truy vết**: FR-001, SEC-001, SEC-002, SEC-003, SEC-006.

**Acceptance criteria**

#### Scenario 1 - Đổi mật khẩu hợp lệ

- **Given** người dùng xác nhận đúng mật khẩu hiện tại và mật khẩu mới đạt chính sách
- **When** người dùng yêu cầu đổi mật khẩu
- **Then** mật khẩu mới được lưu bằng cơ chế băm an toàn, các phiên khác bị xóa và bị từ chối ở request tiếp theo, phiên hiện tại giữ nguyên, và người dùng nhận xác nhận

#### Scenario 2 - Thông tin không hợp lệ

- **Given** mật khẩu hiện tại sai hoặc mật khẩu mới không đạt chính sách
- **When** người dùng gửi yêu cầu
- **Then** hệ thống từ chối an toàn, không thay đổi credential và không lộ chi tiết nội bộ



### US-IAM-007 - Quản trị vòng đời tài khoản

**Primary unit**: U01; **Scope**: MVP.

**Current UC trace**: UC 08; UC 09; UC 10; UC 11; UC 12. Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là quản trị viên, tôi muốn tìm kiếm, tạo, cập nhật và khóa/mở khóa tài khoản để quản lý tài khoản người dùng trong tổ chức.

**Truy vết**: FR-002, FR-015, FR-014, SEC-001, SEC-002, SEC-003, SEC-005.

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



### US-LRN-001 - Truy cập lớp đã ghi danh

**Primary unit**: U05; **Scope**: MVP.

**Current UC trace**: UC 20; UC 21. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Student, tôi vào lớp để xem tài liệu, Assignment/Quiz và nhóm được phép.

**Truy vết**: FR-002/003/005. Primary unit U05.

#### Scenario 1

Given enrollment ACTIVE, When vào lớp, Then nhận đúng tài nguyên đang hiển thị.

#### Scenario 2

Given bị gỡ khỏi lớp, When gọi trực tiếp API/MCP, Then không đọc/ghi tiếp; không phụ thuộc token hay payment.

### US-NTF-001 - Nhận thông báo thiết yếu

**Primary unit**: U14; **Scope**: MVP.

**Current UC trace**: UC 65. Source: [current UC table](../../../docs/usecase_table.md).


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

#### Scenario 4 - Email quan trọng và backup lỗi

- **Given** thông báo lớp mới được chọn email hoặc backup thất bại
- **When** sự kiện được nhận sau commit
- **Then** gửi đúng Student hoặc Admin theo policy, chống trùng event/recipient/channel, không đính điểm/bài nộp/backup; SENT chỉ là provider chấp nhận, lỗi ghi FAILED sau retry hữu hạn.


### US-QBK-001 - Quản lý rubric của bài

**Primary unit**: U07; **Scope**: MVP.

**Current UC trace**: UC 36. Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là giảng viên hoặc Chủ nhiệm môn, tôi muốn mỗi phần của bài tự có rubric để tôi điền và sửa trong Rubric Detail khi soạn bài, giúp chấm bài nhất quán.

**Truy vết**: FR-002, FR-016, FR-014, SEC-002, SEC-003, SEC-005.

**Acceptance criteria**

#### Scenario 1 - Quản lý rubric hợp lệ

- **Given** người dùng có quyền với bài (lớp mình dạy hoặc môn mình quản lý), bài còn nháp và rubric có các mục checklist hợp lệ
- **When** người dùng thêm phần trong tài liệu Assignment cá nhân/nhóm với diagramEnabled tùy chọn, rồi điền tiêu chí và mục trong Rubric Detail
- **Then** mỗi phần đã có sẵn một rubric trống; rubric được lưu cho đúng phần; mỗi tiêu chí gồm các mục checklist có điểm và điểm rubric là tổng điểm các mục đạt; sửa ghi đè trực tiếp; bỏ phần thì rubric bị xóa theo

#### Scenario 2 - Bài đã phát hành

- **Given** bài sở hữu rubric đã được phát hành
- **When** người dùng sửa hoặc xóa rubric
- **Then** hệ thống từ chối vì rubric đã khóa, để kết quả chấm không thay đổi

#### Scenario 2a - Rubric còn trống

- **Given** bài còn phần có rubric trống hoặc chưa hợp lệ
- **When** người dùng phát hành bài
- **Then** hệ thống từ chối và chỉ ra phần cần điền rubric

#### Scenario 3 - Nhân bản đề

- **Given** bài đã có rubric
- **When** bài được nhân bản, tạo version mới hoặc copy sang lớp khác
- **Then** rubric được nhân bản thành rubric mới, chưa khóa, của bài đích; hai bài không dùng chung rubric



### US-QBK-002 - Ngân hàng Quiz cấp môn

**Primary unit**: U10; **Scope**: MVP.

**Current UC trace**: UC 46; UC 47. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Subject Manager, tôi quản lý câu Quiz/version và nguồn nội dung tài liệu được phép.

**Truy vết**: FR-016. Primary unit U10.

#### Scenario 1

Given R2, When tạo/import/activate câu MCQ, Then validation và version được lưu; không câu CODE.

#### Scenario 2

Given Teacher chọn câu ACTIVE, When soạn Quiz, Then pin version và không lộ đáp án ẩn cho Student.

### US-RPT-001 - Theo dõi tiến độ nộp bài và nhắc nhở

**Primary unit**: U09; **Scope**: MVP.

**Current UC trace**: UC 37; UC 42; UC 29. Source: [current UC table](../../../docs/usecase_table.md).


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

- **Given** Chủ nhiệm môn của môn đã bật hiển thị phân bố điểm trong Edit Class Information (UC 29), bài GRADED đủ điều kiện riêng tư và điểm đã công bố theo lớp
- **When** người học mở Student Assignments của lớp (UC 37)
- **Then** bài GRADED có điểm đã công bố hiện phân bố theo khoảng điểm tổng hợp, ẩn khoảng quá ít người; quiz/Practice không có phân bố; không suy ra danh tính hoặc điểm của người học khác; Teacher chỉ được giao dạy không có quyền bật/tắt cài đặt này



### US-RPT-002 - Thống kê hệ thống cho quản trị viên

**Primary unit**: U15; **Scope**: MVP.

**Current UC trace**: UC 66. Source: [current UC table](../../../docs/usecase_table.md).


**Story**: Là quản trị viên, tôi muốn thấy thống kê ngay trên Admin Dashboard (UC 66) để nắm quy mô người dùng, môn, lớp và ghi danh của hệ thống.

**Truy vết**: FR-002, FR-024, NFR-002, SEC-005, SEC-002, SEC-003.

**Acceptance criteria**

#### Scenario 1 - Xem số liệu tổng hợp

- **Given** quản trị viên đã đăng nhập
- **When** vào Admin Dashboard (UC 66)
- **Then** hệ thống hiển thị số tài khoản theo vai trò và trạng thái (`PENDING`, `ACTIVE`, `DISABLED`), số môn theo trạng thái, số lớp theo trạng thái (`DRAFT`, `OPEN`, `ARCHIVED`) và số ghi danh `ACTIVE`, tính tại thời điểm mở, chỉ là số đếm, không có tên/email

#### Scenario 2 - Từ chối vai trò khác

- **Given** người dùng không có vai trò `ADMIN`
- **When** gọi API thống kê, kể cả gọi trực tiếp
- **Then** hệ thống từ chối theo quyền và không trả số liệu nào

#### Scenario 3 - Backup trong Dashboard UC 66

- **Given** Admin đang xác thực
- **When** mở mục Backup trên Admin Dashboard
- **Then** U15 đọc U03 port và hiển thị trạng thái/lịch sử, thời gian, dung lượng và lỗi đã sanitize; không tải archive hoặc restore production. Lỗi port hiện unavailable, không giả SUCCESS; sửa lịch/retention chuyển UC 15.



### US-RPT-003 - Xuất bảng điểm

**Primary unit**: U09; **Scope**: MVP.

**Current UC trace**: UC 45. Source: [current UC table](../../../docs/usecase_table.md).


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



### US-SET-001 - Settings hiện hành

**Primary unit**: U03; **Scope**: MVP.

**Current UC trace**: UC 14; UC 15. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Admin, tôi chỉnh cài đặt vận hành được phép.

**Truy vết**: FR-033. Primary unit U03.

#### Scenario 1

Given Settings version hợp lệ, When edit, Then validation/audit; không monthly credit grant hoặc giá gói.

#### Scenario 2

Given AI credentials, When cấu hình, Then bản ghi được bảo vệ qua U06, không key plaintext trong Settings.

#### Scenario 3 - Quota trong Settings UC14/15

- **Given** Admin có quyền và quota policy/version hợp lệ
- **When** chỉnh quota giảng viên trong Edit Setting (UC15)
- **Then** U03 gọi U06 port; U06 validate/update/audit, không sửa repository xuyên unit, không cấp Student hoặc lưu credentials; lỗi port không báo cập nhật thành công.



### US-OPS-001 - Backup và kiểm thử khôi phục

**Primary unit**: U03; **Scope**: MVP.

**Current UC trace**: UC 66; UC 14; UC 15. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là Admin, tôi muốn theo dõi backup database trên Drive để người vận hành khôi phục được dữ liệu sau sự cố.

**Truy vết**: FR-038, FR-011, FR-033, REL-004; UC 66. Chi tiết trong requirements/council-features-2026-10-13.md.

#### Scenario 1 - Backup hợp lệ

- **Given** lịch và Drive riêng tư đã cấu hình
- **When** job dump database nhất quán và upload archive được kiểm checksum
- **Then** ghi SUCCESS/lịch sử, bảo vệ archive, giữ khóa giải mã riêng và chỉ giữ các bản trong 7 ngày gần nhất.

#### Scenario 2 - Backup lỗi

- **Given** dump hoặc upload lỗi
- **When** job kết thúc sau retry hữu hạn
- **Then** không SUCCESS, giữ bản tốt trước và phát cảnh báo cho Admin qua U14.

#### Scenario 3 - Restore chứng minh khả năng phục hồi

- **Given** bản thành công và secrets khôi phục giữ riêng
- **When** người vận hành restore vào môi trường cô lập
- **Then** kiểm quyền, tài liệu/XML, bài nộp/điểm và usage/quota và lưu bằng chứng; tệp trên Drive không được khôi phục; không ghi đè production.

#### Scenario 4 - Tách dashboard khỏi backup owner

- **Given** dữ liệu backup/status thuộc U03
- **When** Admin xem trạng thái qua UC 66/U15
- **Then** chỉ metadata được trả qua port; cấu hình lịch thuộc UC 15 (UC14 chỉ xem); story backup vẫn primary U03 dù UC xem là primary U15.



### US-INT-001 - Xem dấu hiệu rời trang khi làm bài

**Primary unit**: U09; **Scope**: MVP.

**Current UC trace**: UC 42. Source: [current UC table](../../../docs/usecase_table.md).

**Story**: Là giảng viên được giao dạy, tôi xem hoạt động làm bài để đối chiếu và trao đổi với sinh viên khi cần.

**Truy vết**: FR-039, FR-002; UC 42. U07 và U10 bật theo dõi; U08 sở hữu ActivityEvent; U11, U12 ghi sự kiện qua port U08.

#### Scenario 1 - Policy và sự kiện

- **Given** policy bật được snapshot và thông báo trước lượt làm
- **When** Student rời/quay lại trang
- **Then** lưu sự kiện đúng actor/attempt, chống trùng; policy off không thu thập, iframe Draw.io không tự coi là rời trang.

#### Scenario 2 - Review và giới hạn

- **Given** giảng viên có phân công lớp và có dữ liệu activity
- **When** xem timeline từng sinh viên kể cả bài nhóm
- **Then** thấy số lần/thời lượng/khoảng thiếu, không tự kết luận gian lận hoặc đổi điểm; ngoài quyền bị từ chối.

#### Scenario 3 - Activity trong UC 42

- **Given** giảng viên được giao lớp và có quyền xem lượt tương ứng
- **When** mở activity tại chi tiết bài làm hoặc lượt Quiz Practice
- **Then** kiểm policy/scope hiện hành, phân biệt disabled/no data/unavailable; Quiz vẫn Practice, không tự thêm Gradebook hoặc chỉnh điểm vì activity.



## Coverage

| Story | Unit | Scope |
|---|---|---|
| US-AIC-001 | U06 | MVP |
| US-AIC-002 | U06 | MVP |
| US-AIC-003 | U06 | MVP |
| US-AIC-004 | U06 | MVP |
| US-AIG-001 | U06 | MVP |
| US-AIG-002 | U06 | MVP |
| US-AIG-003 | U06 | MVP |
| US-ASM-001 | U07 | MVP |
| US-ASM-003 | U08 | MVP |
| US-ASM-004 | U07 | MVP |
| US-ASM-006 | U10 | MVP |
| US-ASM-008 | U07 | MVP |
| US-ASM-009 | U07 | MVP |
| US-ASM-010 | U07 | MVP |
| US-ASM-012 | U08 | MVP |
| US-AUD-001 | U02 | MVP |
| US-CAT-001 | U04 | MVP |
| US-CAT-002 | U05 | MVP |
| US-CAT-003 | U05 | MVP |
| US-CNT-001 | U13 | MVP |
| US-CNT-002 | U13 | MVP |
| US-CNT-004 | U14 | MVP |
| US-CNT-005 | U13 | EXTENSION |
| US-GRD-001 | U11 | MVP |
| US-GRD-002 | U09 | MVP |
| US-GRD-003 | U09 | MVP |
| US-GRD-004 | U09 | MVP |
| US-GRD-005 | U09 | MVP |
| US-GRP-001 | U12 | MVP |
| US-GRP-002 | U12 | MVP |
| US-GRP-003 | U12 | MVP |
| US-GRP-004 | U12 | MVP |
| US-GRP-005 | U12 | MVP |
| US-GRP-006 | U12 | MVP |
| US-IAM-001 | U01 | MVP |
| US-IAM-002 | U01 | MVP |
| US-IAM-003 | U01 | MVP |
| US-IAM-004 | U01 | MVP |
| US-IAM-005 | U01 | MVP |
| US-IAM-006 | U01 | MVP |
| US-IAM-007 | U01 | MVP |
| US-LRN-001 | U05 | MVP |
| US-NTF-001 | U14 | MVP |
| US-QBK-001 | U07 | MVP |
| US-QBK-002 | U10 | MVP |
| US-RPT-001 | U09 | MVP |
| US-RPT-002 | U15 | MVP |
| US-RPT-003 | U09 | MVP |
| US-SET-001 | U03 | MVP |
| US-OPS-001 | U03 | MVP |
| US-INT-001 | U09 | MVP |

## Validation

Mỗi story có Given/When/Then; baseline scope và UC owner ở requirements/use-cases.md. FR/NFR/SEC tham chiếu requirements hiện hành; UC dùng số mới 01–66 đánh theo unit ngày 2026-10-14 (bảng đối chiếu số cũ ở use-cases.md), không đọc catalog 73 cũ. Không xác nhận tests đã chạy; Construction design/plans đã sync; code/contracts/migrations thực tế chưa revision. Security/resiliency enabled subset giữ nguyên; PBT disabled.

## Current acceptance deltas — 2026-10-30

US-AIC-002: Student own-Practice MCP restored, Admin credentials/tools denied (including old tokens), wrong owner/expired/revoked deny; owner token reveal requires ACTIVE and ciphertext. US-AIC-003: multiple institutional keys/provider and Admin reveal audited; personal keys remain private. Class lifecycle, Quiz reopen, optional leader reasons and material modules inherit the code-derived UC clarifications without new story IDs. Source behavior is documented; fresh acceptance is pending.

## MCP correction and audit remediation — 2026-10-30

Student/Teacher/Subject Manager ACTIVE manage own MCP credentials; Admin is denied, including pre-existing tokens. UC41 personal API and MCP share own/SUBMITTED/PRACTICE/current-enrollment/hash/schema/pending/daily-budget restrictions. MCP READ_ONLY cannot record feedback; READ_WRITE can. API and MCP quota checks are serialized per account inside a transaction. UC52 remains deterministic permitted Quiz result/explanation, not an AI generation service. Current code is a remediation working tree on794a4053; dated initial audit and staff-expansion docs are historical where superseded.
