# Unit of Work - 17 unit logic

## 1. Phạm vi và quy tắc

Đây là 17 unit lập kế hoạch theo hai ảnh tham chiếu, được đối chiếu với catalog 90 use case/59 story hiện hành. Chúng là module logic trong một backend Spring Boot, không phải 17 service triển khai độc lập. Frontend Next.js và worker process dùng contract có version. 57 story còn hiệu lực được gán một primary unit; hai story tiến độ bài học được ghi là ngoài phạm vi.

- Mỗi unit sở hữu dữ liệu và quy tắc nghiệp vụ của mình; unit khác gọi public contract, không đọc bảng/repository trực tiếp.
- U01 kiểm quyền actor/object; U02 giữ audit/job/outbox và phát triển song song với U01 qua authorization contract có version; U03 giữ file/artifact. Tách unit không thay đổi một backend deployable.
- Assignment đã giao sửa bằng version mới cùng stable key. Attempt giữ assignment/question/rubric snapshot cũ.
- AI chỉ tạo draft hoặc grade proposal. Giảng viên duyệt đề và quyết định điểm cuối. RAG là nguồn hỗ trợ tùy chọn cho tạo đề.
- Không sao chép khóa học/lớp. Copy assignment/rubric theo phạm vi đã duyệt tạo identity độc lập có lineage.
- Không có learning path, trạng thái hoàn thành hay tiến độ từng bài học. Tiến độ nộp bài và trạng thái job vẫn có.

## 2. Danh sách unit và ownership

| Unit | Tên | Sở hữu chính | Không sở hữu / ranh giới |
|---|---|---|---|
| U01 | Account & Access | Account, credential, session, role/scope/object authorization | Không sở hữu audit, job hoặc nội dung nghiệp vụ |
| U02 | Audit, Job & Outbox | Audit append-only, job enqueue/lease/status, outbox, retry/dead-letter, correlation | Không quyết định quyền học, payment hoặc điểm |
| U03 | File & Artifact | Upload/download có quyền, scan, checksum, signed access, immutable full Draw.io XML, chống XXE; lưu derived artifact theo metadata/TTL | Không sở hữu nội dung, submission hoặc logic tạo bản compact cho AI |
| U04 | Subject, Class & Enrollment | Môn/lớp, phân công giảng viên/Chủ nhiệm môn, ghi danh, class scope | Không sao chép khóa học/lớp hoặc cấp entitlement |
| U05 | Content, Material & RAG | Học liệu môn/lớp, publication, YouTube transcript, ingestion/index, thông báo/hỏi đáp lớp Phase 2 | Không quyết định quyền truy cập learner hoặc bắt buộc RAG trong tạo đề |
| U06 | Rubric & Question Bank | QuestionVersion/RubricVersion, preview, publish/retire version, analytics Phase 2 | Không sửa hồi tố version đã dùng trong attempt |
| U07 | Payment & Entitlement | Intent, verified webhook, idempotent access grant, reconciliation | Không ghi enrollment; redirect browser không cấp quyền |
| U08 | Learning Access & Dashboard | Kiểm enrollment + entitlement, đọc nội dung được phát hành, lớp/assignment dashboard | Không có learning path hoặc tiến độ từng bài học; không ghi nội dung |
| U09 | Assessment Core & Publication | Assignment aggregate, draft/review, publication lớp, schedule, version khi sửa đề đã giao | Không sở hữu kiểu câu hỏi, attempt hay final grade |
| U10 | Question Type Authoring | Cấu hình quiz/essay, đề chung cấp môn, quy tắc bài loại cụ thể | Không sở hữu bank item hoặc publication transaction |
| U11 | Template, Copy & Simulation | Template môn, copy assignment/rubric giữa lớp có lineage, simulation policy, retire/clone Phase 2 | Không copy lớp/khóa học, publication, attempt, submission hay grade |
| U12 | Attempt & Submission | Attempt snapshot, autosave, nộp cá nhân/Draw.io/Code Lab, receipt, lịch sử/nộp lại | Không chấm điểm hoặc sửa attempt đã nộp |
| U13 | Group & Allocation | Membership, đúng một leader, yêu cầu đổi leader, phân phần việc | Không sở hữu phần nộp hay composite |
| U14 | AI & Code Execution | AI provider-neutral, quota/kill-switch, draft proposal, CodeExecutionService/sandbox, compact XML job | Không publish đề, không chốt grade, không chạy mã không cô lập |
| U15 | Part Submission & Composite | Nộp phần cá nhân, ghép composite từ ordered source versions, giảng viên chốt version chung | Không sửa source part hoặc tự tính điểm cuối thành viên |
| U16 | Grading | Tự chấm xác định, chấm tay, AI proposal review, điểm composite/member, grade history và publication | AI không quyết định final grade; không sửa submission |
| U17 | Reporting & Notification | Read model theo quyền, theo dõi nộp bài, export, analytics, notification delivery | Không ghi ngược transaction nguồn hoặc rollback nghiệp vụ khi gửi lỗi |

### Quyết định riêng cho U08

Catalog hiện hành chỉ có `US-LRN-001` cho quyền truy cập lớp. `UC-LRN-01`, `UC-LRN-02` và `UC-CNT-04` hỗ trợ dashboard/truy cập nội dung nhưng mô tả cũ còn nhắc tiến độ; phần tiến độ đó không được triển khai. `US-LRN-002`, `US-LRN-003` cùng `UC-LRN-03..05` đã bị loại khỏi phạm vi thiết kế. Không có requirement/story về learning path, vì vậy U08 không tạo lộ trình, prerequisite hay completion model. Dashboard chỉ tổng hợp lớp, assignment, thông báo và trạng thái có sẵn từ owner khác.

## 3. Code organization

- `/frontend`: Next.js, feature folders theo UI/domain, gọi REST OpenAPI.
- `/backend`: một Spring Boot modular monolith; package theo U01-U17, mỗi package có API/application/domain/infrastructure khi cần.
- `/worker`: process/container riêng, handler thuộc unit nghiệp vụ tương ứng và dùng versioned job contract; không import repository nội bộ backend.
- `/contracts`: OpenAPI, event/job schema và compatibility tests.
- `/infra`: cấu hình database, queue, storage, deployment/observability.
- `/aidlc-docs`: chỉ có tài liệu.

Nhóm 5 người mở tối đa năm unit đang triển khai cùng lúc theo dependency trực tiếp và slot trống. Mỗi unit có owner và reviewer; consumer chỉ tích hợp sau khi provider tương ứng đạt kiểm tra contract/behavior, không cần chờ toàn bộ wave đóng.

## 4. Bốn wave mở việc theo dependency

Wave là nhóm công việc và điểm kiểm tra tích hợp, không phải barrier bắt mọi unit trong một cột xong rồi mới mở cột tiếp theo. Unit được mở ngay khi **tất cả provider `H` của chính nó** đạt contract/behavior cần dùng và có người rảnh; không phải chờ unit không liên quan. Cạnh `C` cho phép phát triển song song với contract có version và fake adapter; cạnh `E` cần event/schema của owner và dữ liệu thật tại integration gate. Nhóm có tối đa năm unit đang được triển khai đồng thời trên toàn bộ các wave, kể cả khi hai wave chồng thời gian.

| Wave | Unit (số lượng) | Nhánh có thể mở song song và điều kiện nối tiếp | Điểm dừng tích hợp |
|---|---|---|---|
| 1 - nền | U01, U02, U03, U04 (4) | U01 và U02 chạy song song; U03/U04 mở sau khi cả hai cung cấp phần cần dùng | Identity, audit/job/outbox, file/artifact và class/enrollment contract |
| 2 - nguồn và đề lõi | U05, U06, U07, U08, U09 (5) | Sau U04, U05/U06/U07 chạy song song; U08 mở khi U05 và U07 sẵn sàng; U09 mở khi U05 và U06 sẵn sàng | Content/bank versions, entitlement, Learning access và đề thủ công/publication |
| 3 - biên soạn và thực hiện | U10, U11, U12, U13, U14 (5) | U10/U13 mở sau U09; U14 mở sau U05/U06/U03; U11 sau U10; U12 sau U08/U11, tích hợp U14 qua `C` | Loại câu hỏi, template/simulation, group allocation, AI/Code và attempt/submission |
| 4 - kết quả | U15, U16, U17 (3) | U15 sau U12/U13; U16 sau U15 và U14; U17 hoàn tất projection sau event U16 | Composite, final grade, reporting/notification |

Không có điều kiện “đóng toàn bộ wave N mới được bắt đầu wave N+1”. Ví dụ U01/U02 có thể bắt đầu cùng lúc; U14 thuộc wave 3 có thể bắt đầu khi U03/U05/U06 sẵn sàng, dù U08 hoặc U09 ở wave 2 vẫn đang làm. U02 chỉ phát hành audit/job read API sau khi tích hợp kiểm quyền từ U01. Khi đủ năm người đang giữ unit, unit mới đủ dependency sẽ chờ slot trống. Đường phụ thuộc chi tiết và Mermaid nằm trong `unit-of-work-dependency.md`.

## 5. Integration gates

| Gate | Kiểm tra bắt buộc |
|---|---|
| G1 | U01-U04: deny-by-default auth, audit append-only, job idempotency/retry, upload scan/checksum, XML XXE rejection, signed access và subject/class scope |
| G2 | U05-U09: Content/File scope, QuestionVersion/RubricVersion immutable, verified entitlement, Learning access không có lesson progress và đề thủ công/publication đúng scope |
| G3 | U10-U14: loại câu hỏi, template/copy lineage, simulation policy, group allocation, AI/Code sandbox contract, attempt snapshot và nộp idempotent |
| G4 | U15-U17: composite immutable, chấm tay/final grade, AI proposal không thành final grade và reporting/notification/export theo quyền |

Gate kiểm tra kết quả của từng nhánh khi nhánh đó sẵn sàng; gate tổng của wave dùng để xác nhận đủ phạm vi, không khóa việc mở unit ở wave sau nếu provider trực tiếp đã sẵn sàng.

## 6. Quy tắc security và resiliency xuyên unit

- Mọi unit áp dụng deny-by-default, input validation, object-level authorization, structured logging và safe error response.
- Secret chỉ đến đúng deployable cần sử dụng; frontend, backend và worker không chia sẻ secret ngoài nhu cầu.
- External call có timeout, retry hữu hạn theo failure class và circuit breaker khi thích hợp.
- Job handler idempotent; nguồn dữ liệu và kết quả quan trọng có checksum/version.
- Database, object storage, queue và traffic phải mã hóa khi triển khai production.
- Health check, metrics, logs và alerting phải bao phủ riêng backend API và worker.
- Backup/restore, RTO/RPO, multi-zone và deployment rollback được chốt ở NFR/Infrastructure Design.

## 7. Definition of Done cho mỗi unit

- Story và acceptance criteria thuộc unit được truy vết tới thiết kế và test.
- Public API/event/job contracts được version hóa và có compatibility test.
- Unit test, module integration test và security negative test chạy đạt.
- Không vi phạm data ownership hoặc dependency direction.
- Migration, observability, error handling và audit events liên quan được xác định.
- Contract/behavior của mọi provider trực tiếp đạt trước khi consumer tích hợp; gate tổng wave không chặn nhánh độc lập.

## 8. Extension compliance tại Units Generation

Trạng thái N/A dưới đây chỉ có nghĩa rule không được kiểm chứng bằng artifact phân rã unit; không phải miễn áp dụng cho toàn dự án.

### Security Baseline

| Rule | Trạng thái tại stage | Lý do |
|---|---|---|
| SECURITY-01 | N/A | Cấu hình mã hóa storage/transport thuộc Infrastructure Design |
| SECURITY-02 | N/A | Network intermediary chưa được chọn |
| SECURITY-03 | Compliant | Structured logging/correlation là Foundation concern cho mọi deployable |
| SECURITY-04 | N/A | HTTP header implementation thuộc NFR Design/Code Generation |
| SECURITY-05 | Compliant | Input/file/XML/job schema validation là boundary bắt buộc |
| SECURITY-06 | Compliant | Backend và worker dùng scoped identity/secret, least-privilege contract |
| SECURITY-07 | N/A | Network topology thuộc Infrastructure Design |
| SECURITY-08 | Compliant | U01 sở hữu deny-by-default, role/scope/object authorization |
| SECURITY-09 | N/A | Runtime hardening thuộc NFR/Infrastructure Design |
| SECURITY-10 | N/A | Dependency pinning, scanning và SBOM thuộc Code Generation/Build and Test |
| SECURITY-11 | Compliant | Auth, payment và AI/final-grade responsibilities được cô lập |
| SECURITY-12 | Compliant | Identity/session/credential responsibility nằm riêng tại U01 |
| SECURITY-13 | Compliant | Immutable artifact/version, checksum, audit và versioned contract đã được định nghĩa |
| SECURITY-14 | N/A | Alert/retention configuration thuộc NFR/Infrastructure Design |
| SECURITY-15 | Compliant | Fail-closed, safe error và dependency failure isolation là cross-unit rule |

Không có blocking Security finding ở Units Generation.

### Resiliency Baseline

| Rule | Trạng thái tại stage | Lý do |
|---|---|---|
| RESILIENCY-01 | N/A | Business criticality đã được xử lý ở Requirements; không đổi bởi unit boundary |
| RESILIENCY-02 | N/A | RTO/RPO được chi tiết ở NFR Requirements |
| RESILIENCY-03 | N/A | Change-management process thuộc NFR/Operations readiness |
| RESILIENCY-04 | N/A | Deployment/rollback selection thuộc Infrastructure Design |
| RESILIENCY-05 | Compliant | Backend và worker đều phải có metrics/logs/health visibility |
| RESILIENCY-06 | Compliant | Health check riêng cho API và worker là boundary requirement |
| RESILIENCY-07 | N/A | Resiliency alarms/tooling thuộc Infrastructure Design |
| RESILIENCY-08 | N/A | Zone/region topology chưa được chọn tại stage này |
| RESILIENCY-09 | Compliant | Worker được scale độc lập và job có capacity boundary |
| RESILIENCY-10 | Compliant | Timeout, retry hữu hạn, circuit breaker và worker isolation đã được yêu cầu |
| RESILIENCY-11 | N/A | DR strategy thuộc NFR/Infrastructure Design |
| RESILIENCY-12 | N/A | Backup/replication thuộc Infrastructure Design |
| RESILIENCY-13 | N/A | Failover/failback runbook thuộc Infrastructure/Operations readiness |
| RESILIENCY-14 | N/A | Resiliency testing plan thuộc NFR Design |
| RESILIENCY-15 | N/A | Incident-response process thuộc NFR Design |

Không có blocking Resiliency finding ở Units Generation.
