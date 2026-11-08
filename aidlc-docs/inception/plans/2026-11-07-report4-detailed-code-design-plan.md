# Kế hoạch chỉnh Detailed Code Design trong Report 4

## Mục tiêu

Tổ chức lại phần II theo nghiệp vụ, bổ sung luồng tạo Assignment bằng AI API/MCP và đồng bộ class diagram, sequence diagram, tiêu đề, mô tả và mục lục.

Đã hoàn tất nhiệm vụ 1–6. Người dùng đã duyệt bộ hình qua các lần chỉnh và yêu cầu cập nhật Report 4. Bộ 6 class diagram và 15 sequence diagram đã được chèn vào đúng file Report 4 hiện có, sắp lại phần II thành 5 nhóm và cập nhật 70 mục lục. Đã kiểm tra bố cục 47 trang bằng Word local và xác nhận SHA256 khi tải lại từ Drive.

## Nguồn và phạm vi

- Report 4 hiện tại tại https://docs.google.com/document/d/1odPk65Ly9I3DHgz9gWH4rC_WjQHufI70/edit. Bản đã đọc có modifiedTime `2026-11-07T14:56:08.599Z`.
- `docs/class-diagrams.drawio` và `docs/sequence-diagrams.drawio` là nguồn bố cục chính.
- `docs/report4-diagram-specs/class-diagrams.json` và `sequence-diagrams.json` lưu mô hình ngữ nghĩa và đối chiếu code.
- Code hiện tại trong `assignments`, `assignmentwork`, `groupdocs`, `grading`, `ai` và các business rules AI-DLC tương ứng.
- Giữ nội dung và hình của mục Authentication theo yêu cầu. Các phần I, III, IV thuộc phạm vi bảo toàn khi sửa phần II.
- Phạm vi nghiệp vụ là Assignment cá nhân/nhóm, soạn bài, rubric, làm/nộp bài, Practice feedback và chấm điểm. Quiz tiếp tục ngoài phạm vi diagram theo yêu cầu trước.
- Công việc là tài liệu và sơ đồ. Kế hoạch không bao gồm thay đổi chức năng ứng dụng, migration, commit hoặc push.

## Bố cục phần II sau chỉnh sửa

| Mục | Nội dung | Hình dự kiến |
|---|---|---|
| 1. Account Authentication and Session Management | Giữ nguyên 1.1–1.5 hiện tại | 1 class, 4 sequence |
| 2. Assignment Authoring and Publication | Tạo/sửa/xóa bài, AI authoring, rubric và phát hành | 1 class, 5 sequence |
| 3. Assignment Workspace and Submission | Làm/nộp bài cá nhân và nhóm | 1 class, 2 sequence |
| 4. Grading and Practice Feedback | Feedback riêng, chấm API/MCP, chấm tay và công bố điểm | 1 class, 4 sequence |
| 5. Shared AI Integration | Cấu trúc xử lý AI API và MCP dùng chung | 2 class |

Tổng đích là **6 class diagram và 15 sequence diagram**, gồm cả Authentication được giữ nguyên. So với hiện tại, bổ sung 1 class diagram Authoring và 3 sequence diagram. Các hình đang có được tái sử dụng và chỉnh theo ma trận bên dưới.

### 1. Account Authentication and Session Management

- 1.1 Class Diagram
- 1.2 Activate Account
- 1.3 Login
- 1.4 Logout
- 1.5 Reset Password

### 2. Assignment Authoring and Publication

- 2.1 Class Diagram
- 2.2 Create/Update/Delete Individual Assignment (UC35)
- 2.3 Create/Update/Delete Group Assignment (UC53)
- 2.4 Generate Assignment Draft — AI API
- 2.5 Create Assignment Draft — External MCP
- 2.6 Configure Rubric and Publish Assignment

Hai luồng 2.4/2.5 dùng chung cho bài cá nhân và nhóm. UC35/UC53 vẫn giữ toàn bộ Create/Update/Delete trong một hình. Việc phát sinh đề xuất và giáo viên duyệt đề xuất nằm trong hình AI đầy đủ tương ứng, tránh chép lặp cả chuỗi AI vào hai hình CRUD.

Đây là lựa chọn trình bày để duyệt trong kế hoạch này. Nó cụ thể hóa đề xuất trước về nhánh AI, giúp giới hạn chiều ngang và chiều dài của UC35/UC53. Diagram ghi rõ trạng thái Assignment, chế độ và điều kiện đầu vào của từng luồng.

### 3. Assignment Workspace and Submission

- 3.1 Class Diagram
- 3.2 Complete Individual Assignment
- 3.3 Complete Group Assignment

### 4. Grading and Practice Feedback

- 4.1 Class Diagram
- 4.2 Request Practice Feedback
- 4.3 Grade Submission With AI — Direct API
- 4.4 Grade Submission With AI — External MCP
- 4.5 Grade Manually, Finalize and Publish

### 5. Shared AI Integration

- 5.1 AI API Task Processing — Class Diagram
- 5.2 MCP Integration — Class Diagram

Mỗi class diagram có tiêu đề, caption và mô tả phạm vi riêng. Các sequence nghiệp vụ nằm trong mục 2–4. Mục 5 giải thích các thành phần kỹ thuật được các nghiệp vụ đó sử dụng.

## Quy tắc thể hiện

- Giữ format UML đen trắng hiện tại. Mỗi class một hộp, có stereotype đúng vai trò và các ngăn rõ ràng.
- Method hiển thị đầy đủ tên parameter, tên dễ hiểu và gọn. Giữ return type, bỏ type của parameter theo yêu cầu trước. `sourceOperations` vẫn lưu chữ ký Java đầy đủ.
- Giữ thuộc tính có type, quan hệ và hướng phụ thuộc phù hợp code. Chỉ chọn method/field phục vụ luồng được mô tả.
- Sequence giữ bố cục actor/UI → controller hoặc MCP entry → service → repository → database. Mỗi lifeline mang một tên class cụ thể, dấu `:` cùng dòng với tên class.
- Ưu tiên 6–7 lifeline, khoảng cách đủ thoáng, font và parameter đọc được trên trang dọc. Helper nội bộ được tóm tắt bằng action ngắn, tránh tạo các method giả trên service đang hiển thị.
- Async task phải thể hiện đúng việc trả PENDING trước và xử lý worker sau commit. Kết quả READY cần được giáo viên xem và Apply hoặc Discard.
- Mọi call ứng dụng có tên method và đủ parameter theo code. SQL/action tóm tắt được phân biệt với lời gọi method Java.
- AI API và MCP có entry point, nguồn AI và cách lưu đề xuất riêng. MCP agent dùng mô hình của agent. Teacher review là bước bắt buộc trước khi áp dụng nội dung hoặc ghi điểm chính thức.
- Giữ Draw.io nhiều trang hiện tại và ID trang cũ khi tái sử dụng. Dùng lối sửa XML an toàn của skill cho bố cục hiện hữu, kiểm tra validator và ảnh native sau mỗi nhóm sửa.
- Các hình và mô tả persistence dùng thuật ngữ JDBC theo yêu cầu. Tên class/method vẫn đối chiếu code thực tế.

## Nhiệm vụ 1 — Chốt ma trận code và hình

### File và nguồn đọc

- Hai file Draw.io và hai file JSON hiện tại.
- `backend/src/main/java/edu/aiplatform/assignments/controller/` và `assignments/service/`.
- `backend/src/main/java/edu/aiplatform/assignmentwork/service/`, `groupdocs/service/`, `grading/service/` và `ai/service/`.

- [x] Đọc lại metadata và nội dung Report 4 ngay trước triển khai, giữ mọi thay đổi người dùng làm sau bản đã rà.
- [x] Đối chiếu từng page Draw.io với tên hình trong Report 4 và mục đích trong JSON.
- [x] Ghi chữ ký method, actor, điều kiện truy cập, transaction, version và bảng dữ liệu của 11 sequence ngoài Authentication.
- [x] Xác minh entry MCP `create_assignment_draft`, `read_assignment_context`, `save_assignment_draft`, workflow authoring và các bước review/apply.
- [x] Xác minh Practice MCP `read_own_work`, `record_private_feedback` và grading MCP workflow/context/fingerprint.

Kết quả kiểm tra là mọi message dự kiến có nguồn code xác định. Khác biệt API/MCP và thao tác sau commit được ghi trong metadata của spec.

## Nhiệm vụ 2 — Sửa class diagram

### File thay đổi

- `docs/class-diagrams.drawio`
- `docs/report4-diagram-specs/class-diagrams.json`

| Hình đích | Xử lý | Thành phần trọng tâm |
|---|---|---|
| 1.1 Authentication | Giữ nguyên | Các class hiện tại |
| 2.1 Authoring | Bổ sung | ClassAssignmentController, AssignmentController, RubricController, AssignmentAuthoringService, AssignmentPublicationService, RubricService, AssignmentRepository, RubricRepository, Assignment, AssignmentRubric, DocumentPart |
| 3.1 Workspace | Sửa hình hiện tại | AttemptController/Service/Submitter, GroupDocController, section/submission services, repository và model làm bài; thêm collaborator cần cho Start/Resume và allocation lock |
| 4.1 Grading/Feedback | Tái sử dụng hình nghiệp vụ AI Assessment | Evaluation, GradingProposal, PracticeFeedback, các controller/service/repository; xác minh GradingBatchService cho Finalize/Publish |
| 5.1 AI API | Sửa hình AI Task Execution | AI task service/handler/repository/gateway và các handler nhận kết quả; bổ sung AssignmentAiDraftController/Service, AiDraftRepository, AssignmentAiDraft và AssignmentDraftResultHandler cần thiết cho authoring |
| 5.2 MCP | Mở rộng hình External MCP | MCP entry/registry, AssignmentMcpAuthoringService/AssignmentMcpTools, AssignmentWorkMcpTools và grading MCP; chọn quan hệ đến domain services để phân biệt ba nghiệp vụ |

- [x] Di chuyển vị trí và đổi tên hình theo ma trận, giữ class Authentication.
- [x] Chọn field/method phục vụ các sequence, đối chiếu đúng tên class và chữ ký hiện tại.
- [x] Trong hai hình dùng chung, gom phạm vi bằng nhóm bố cục Authoring/Feedback/Grading, giữ từng class riêng biệt.
- [x] Nếu hình vượt chiều rộng phù hợp trang dọc, giảm method phụ và field không tham gia luồng trước khi giảm font.
- [x] Kiểm tra subtype/interface, hướng phụ thuộc, stereotype và tên trùng giữa các hình.
- [x] Chạy validator và xuất native Draw.io để kiểm tra hình học và khả năng đọc.

Điều kiện hoàn tất là 6 hình có phạm vi rõ, mọi entry point của sequence có chỗ đối chiếu trong hình nghiệp vụ hoặc hình AI dùng chung. Hình Authentication giữ nguyên nội dung và bố cục.

## Nhiệm vụ 3 — Sửa và bổ sung sequence diagram

### File thay đổi

- `docs/sequence-diagrams.drawio`
- `docs/report4-diagram-specs/sequence-diagrams.json`

| Hình đích | Xử lý và nội dung phải thể hiện |
|---|---|
| 1.2–1.5 | Giữ 4 luồng Authentication |
| 2.2 UC35 | Chuyển từ 2.4 cũ, giữ Create/Update/Delete liên tục; kiểm quyền, version và editable state. Phân biệt đường tạo theo class/subject khi áp dụng |
| 2.3 UC53 | Chuyển từ 2.5 cũ, giữ Create/Update/Delete; class scope, group support và GRADED mode |
| 2.4 AI API authoring | Thêm. Assignment DRAFT đã tồn tại → chọn yêu cầu/nguồn/tài liệu → request → PENDING/task → worker xử lý → get READY → review → Apply hoặc Discard → lưu version |
| 2.5 MCP authoring | Thêm. Agent đọc authoring workflow/context, sinh bằng mô hình riêng, gọi create_assignment_draft hoặc lưu đề xuất vào draft hiện có, nhận reviewUrl, giáo viên duyệt và Apply/Discard |
| 2.6 Rubric/Publish | Thêm. Mở rubric của part → cập nhật scoring rows → version-check → kiểm readiness → publish → khóa nội dung/rubric và SCHEDULED/OPEN. Rubric được tạo cùng part |
| 3.2 Individual workspace | Sửa. Start/Resume → kiểm quyền/lịch/attempt → Edit/Autosave → Submit → snapshot, receipt và evaluation trong transaction. Thể hiện xử lý deadline/retirement ở nhánh liên quan |
| 3.3 Group workspace | Sửa. Assign/Claim → lock allocation → holder Edit/Autosave/Done → leader Submit. Recheck membership, leader và version. Closure dùng saved work theo policy |
| 4.2 Practice feedback | Sửa. Own SUBMITTED PRACTICE → personal API hoặc MCP → feedback private gắn submission hash. Web có PENDING/READY, MCP có read_own_work và record_private_feedback. Giữ ranh giới quota và quyền riêng tư |
| 4.3 AI API grading | Sửa. Từ request/PENDING/READY đến review/acknowledgement và áp dụng proposal vào DRAFT evaluation. Giữ teacher confirmation và tách Finalize/Publish |
| 4.4 MCP grading | Sửa. read_agent_workflow → đủ context pages → fingerprint/version/read progress → record proposal → reviewUrl → teacher review và áp dụng |
| 4.5 Manual grading | Giữ một luồng Save DRAFT → Finalize → Publish. Kiểm tra điểm thành viên và yêu cầu adjustment reason đúng method tương ứng |

- [x] Xử lý 3 hình Authoring mới trước để kiểm chứng class Authoring và class AI dùng chung.
- [x] Chỉnh hai hình Workspace, sau đó các hình Grading/Feedback.
- [x] Với nhánh lỗi, chỉ thể hiện các trường hợp chi phối luồng như mất quyền, version cũ, draft không hợp lệ, task lỗi hoặc kết quả stale.
- [x] Phân biệt task xử lý nền và tool MCP đồng bộ. Thao tác ghi bắt buộc có kiểm tra quyền/version đúng entry point.
- [x] Kiểm tra tổng 15 trang sequence, thứ tự trang, parameter và tên class khớp spec.
- [x] Chạy validator và đọc ảnh native từng trang, đặc biệt hình API/MCP có nhiều bước.

## Nhiệm vụ 4 — Duyệt bộ Draw.io trước khi cập nhật Report 4

- [x] Cập nhật `docs/report4-diagram-specs/README.md` để phản ánh 6 class/15 sequence và bố cục mục mới.
- [x] Xuất các hình đã đổi vào thư mục tạm local, kèm danh sách trang và phạm vi nghiệp vụ.
- [x] Giao bộ Draw.io để người dùng duyệt tên, độ gọn, khoảng cách và mức chi tiết.
- [x] Chỉ chuyển sang cập nhật doc sau khi người dùng duyệt bộ hình theo yêu cầu làm việc trước của họ.

Điểm duyệt này giữ quyền kiểm tra hình của người dùng trước khi các hình được thay trên Report 4.

## Nhiệm vụ 5 — Cập nhật phần II của Report 4

### Tài liệu đích

Report 4, file ID `1odPk65Ly9I3DHgz9gWH4rC_WjQHufI70`.

- [x] Tải bản mới nhất và lưu bản gốc local trước khi ghi. So sánh revision/modifiedTime ngay trước cập nhật.
- [x] Giữ nguyên format font, tiêu đề, caption, viền và màu bảng hiện hành.
- [x] Sắp lại các heading theo bố cục 5 mục. Chuyển UC35/UC53 sang Authoring, Workspace sang mục 3, Grading sang mục 4 và hai class kỹ thuật sang mục 5.
- [x] Thêm class Authoring và ba sequence mới, thay các hình đã chỉnh theo đúng thứ tự và tỷ lệ.
- [x] Mỗi class diagram có caption riêng. Viết 1–2 câu giới thiệu phạm vi từng nhóm, cùng mô tả ngắn mục đích của hình mới khi cần.
- [x] Đảm bảo mô tả tạo bài AI giải thích rõ PENDING, READY, review, Apply/Discard và khác biệt MCP tạo draft mới.
- [x] Cập nhật mục lục, số trang và các tham chiếu nội bộ bị đổi số mục.
- [x] Kiểm tra bố cục local hoặc trực tiếp trên tài liệu đích. Dùng file tạm local cho ảnh/PDF, giữ thư mục báo cáo trên Drive gọn, không tạo thêm doc preview.
- [x] Cập nhật chính file Report 4 hiện có, đọc lại để kiểm chứng dữ liệu và hình đã lưu.

## Nhiệm vụ 6 — Kiểm tra hoàn tất

- [x] Authentication có cùng nội dung/hình như trước. Các phần I, III, IV được bảo toàn, ngoại trừ số trang mục lục do reflow.
- [x] Đủ 6 class diagram và 15 sequence diagram, có tên/caption riêng và ánh xạ đủ các luồng trong ma trận.
- [x] Mỗi method ứng dụng có class sở hữu thật, đủ parameter và tương thích chữ ký code. Phân biệt tên parameter trình bày với tên Java trong spec.
- [x] AI API/MCP authoring, feedback và grading đều có teacher/student actor và quyền truy cập đúng. Proposal chỉ ảnh hưởng nội dung/điểm qua thao tác review và apply hợp lệ.
- [x] Cá nhân và nhóm có snapshot/evaluation đúng transaction, allocation lock và manual/automatic submission đúng business rules.
- [x] Validator có 0 ERROR, xử lý cảnh báo hình học trước khi giao. Kiểm tra native export về chữ, đường nối và ngắt trang.
- [x] Kiểm tra file cuối từ Drive và đối chiếu với bản đã duyệt. Lưu bằng chứng kiểm tra và audit trong repo.

## Các điểm cần chú ý khi thực hiện

1. AI API tạo đề xuất trên draft hiện có, còn MCP có tool tạo draft mới. Nhiệm vụ 1 và 3 phải kiểm tra riêng cả hai đường.
2. Class/service của tạo bài bằng AI khác lớp chấm điểm. Nhiệm vụ 2 phải bổ sung authoring result handler và tool provider đúng miền.
3. Resubmission và grade locks có thể làm proposal cũ hết hiệu lực. Nhiệm vụ 3 phải thể hiện version/hash và thao tác Apply hợp lệ.
4. Nhánh async có thể trả PENDING trước khi worker gọi provider. Nhiệm vụ 3 phải giữ đúng thứ tự và quyền gọi provider.
5. Người dùng có thể sửa doc trong lúc triển khai. Nhiệm vụ 5 phải kiểm tra metadata và ghép thay đổi trước khi ghi.

## Kết quả bàn giao

- Hai file Draw.io đã chỉnh và spec JSON tương ứng.
- README mô tả bộ sơ đồ và quy tắc bảo trì.
- Phần II Report 4 có bố cục 5 mục, hình/caption/mô tả đồng bộ và mục lục cập nhật.
- Bằng chứng kiểm tra hình, mapping method và readback tài liệu trong scratch/audit local.

Đã cập nhật Report 4 theo bộ Draw.io được duyệt. Bằng chứng kiểm tra và readback nằm trong .tmp/report4-section-II-publish và audit.md.
