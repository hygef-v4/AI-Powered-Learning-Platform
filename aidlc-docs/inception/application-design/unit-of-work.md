# Unit of Work

## 1. Mục đích và nguyên tắc

Hệ thống được chia thành tám unit logic để lập kế hoạch, thiết kế, phát triển và kiểm thử. Các unit backend vẫn tạo thành một Spring Boot modular monolith. Worker là project/process/container riêng trong cùng monorepo, giao tiếp với backend bằng job/event contract được version hóa.

Các nguyên tắc bắt buộc:

- Mỗi story có đúng một unit chủ trì; unit khác chỉ hỗ trợ qua public contract.
- Mỗi module sở hữu schema/table của mình; không truy cập trực tiếp repository hoặc bảng của module khác.
- Cross-module write đi qua application service hoặc domain event/outbox.
- Frontend không quyết định authorization; backend kiểm tra role, scope và object-level permission.
- Submission và artifact đã nộp là immutable; các thay đổi tạo version mới.
- Full Draw.io XML là artifact chuẩn. Compact XML chỉ là derived artifact cho AI job do giảng viên yêu cầu.
- AI chỉ tạo draft/proposal; không tự publish đề hoặc final grade.
- Worker payload chỉ mang ID/reference tối thiểu và không sao chép business rule từ backend.

## 2. Danh sách unit

### U01 - Platform Foundation and Identity

**Mục tiêu:** Cung cấp nền tảng bảo mật và kỹ thuật dùng chung trước khi các unit nghiệp vụ bắt đầu.

**Sở hữu:**

- Identity, account lifecycle, school-email authentication, session và password recovery.
- Role/scope authorization và object authorization contracts.
- Append-only audit và correlation context.
- File/artifact metadata, checksum, scan state và signed access contract.
- Job lifecycle, outbox, retry/backoff, dead-letter và idempotency primitives.
- OpenAPI/event schema conventions và worker bootstrap contract.

**Không sở hữu:** Nội dung học, bài đánh giá, submission, grade hoặc provider-specific AI logic.

**Điều kiện hoàn thành:** Các module khác có thể xác thực actor, kiểm tra quyền, lưu artifact, enqueue job và ghi audit mà không phụ thuộc implementation nội bộ.

### U02 - Academic Administration

**Mục tiêu:** Thiết lập cấu trúc môn/lớp và phạm vi thành viên làm nền cho mọi hành trình học.

**Sở hữu:**

- Subject, class lifecycle và phân công giảng viên.
- Enrollment do quản trị viên/giảng viên thực hiện.
- Subject Manager scope cho một hoặc nhiều môn được phân công.
- Invite-code enrollment Phase 2.
- Public contract để kiểm tra membership và quyền quản lý lớp/môn.

**Không sở hữu:** Nội dung lớp, nhóm, bài đánh giá, điểm hoặc entitlement transaction.

### U03 - Content, Learning and Banks

**Mục tiêu:** Cung cấp học liệu, hành trình học và tài nguyên biên soạn có version.

**Sở hữu:**

- Học liệu/RAG source cấp môn và nội dung riêng cấp lớp.
- Content publication/version và authorized search.
- Learning access, progress idempotent và class progress.
- Rubric/question bank versioning, reuse và question analytics.
- Worker handlers cho parsing, chunking và indexing RAG.

**Không sở hữu:** Assessment publication, submission hoặc final grade.

### U04 - Assessment Authoring and Publication

**Mục tiêu:** Quản lý vòng đời đề/bài từ draft đến review, publish và retire.

**Sở hữu:**

- Assessment draft, version, preview, validation và schedule.
- Bài cấp lớp và đề chung cấp môn.
- Cấu hình trắc nghiệm, bài viết luận và các policy chung của bài.
- Clone/version/retire Phase 2.
- Publication contract được Submission sử dụng.

**Không sở hữu:** Nội dung bài làm, attempt artifact, grade hoặc AI final decision.

### U05 - Submission and Group Work

**Mục tiêu:** Bảo toàn bài làm cá nhân/nhóm và thực thi đúng quyền nộp.

**Sở hữu:**

- Autosave draft, attempt, idempotent submission receipt và immutable submission version.
- Draw.io embedded-canvas handoff và full XML artifact.
- Group membership, đúng một leader, leader-change request và individual allocation.
- Individual group-part submission và leader-only shared DOCX submission.
- Authorized view để giảng viên đối chiếu bài chung với các phần cá nhân.

**Không sở hữu:** AI proposal hoặc final grade; không chỉnh sửa cộng tác DOCX trong hệ thống.

### U06 - Grading, AI and Code Execution

**Mục tiêu:** Điều phối chấm điểm có con người quyết định cuối và các tác vụ AI/Code Lab cô lập.

**Sở hữu:**

- Deterministic grading, manual grade, AI proposal review, finalize và publish grade.
- AI authoring/grading orchestration, provider-neutral ports, quota và kill-switch.
- Compact Draw.io derived artifact chỉ khi giảng viên yêu cầu AI chấm.
- Code Lab authoring/test execution qua sandbox port.
- Worker handlers cho AI generation, AI grading và Code Lab.
- Extension, regrade và similarity-check workflows Phase 2.

**Không sở hữu:** Full submission artifact, assessment publication hoặc quyền tự chốt điểm cho AI.

### U07 - Reporting and Notification

**Mục tiêu:** Tạo read model/báo cáo theo quyền và gửi thông báo không làm rollback nghiệp vụ.

**Sở hữu:**

- Submission tracking, reminder, learner dashboard và grade export.
- Question/grade analytics projection và AI-versus-final comparison.
- Notification outbox consumption, template, recipient scope và delivery status.
- Worker handlers cho export và notification delivery.

**Không sở hữu:** Transaction nghiệp vụ nguồn; báo cáo không tự kết luận gian lận hoặc đánh giá giảng viên.

### U08 - Payment and Entitlement

**Mục tiêu:** Xử lý thanh toán và cấp quyền theo verified provider event.

**Sở hữu:**

- Payment intent, provider adapter và transaction state.
- Signature/timestamp/replay validation cho webhook.
- Idempotent entitlement grant và reconciliation.
- Worker handler cho payment reconciliation.

**Không sở hữu:** Dữ liệu thẻ thô, browser redirect như bằng chứng thanh toán hoặc class enrollment lifecycle.

## 3. Tổ chức source code

```text
/
  frontend/
  backend/
  worker/
  infra/
  contracts/
  aidlc-docs/
```

- `/frontend`: Next.js desktop-first; các feature folder bám theo unit và dùng generated OpenAPI client.
- `/backend`: Spring Boot modular monolith; mỗi unit có `api`, `application`, `domain`, `infrastructure` và test package tương ứng.
- `/worker`: Ứng dụng worker độc lập; handler theo job type, adapter provider và consumer của versioned contracts. Worker không import repository nội bộ của backend.
- `/infra`: Docker Compose/IaC, database, queue, object storage, networking, logging và monitoring configuration.
- `/contracts`: OpenAPI, job/event schemas và compatibility tests dùng chung giữa backend, frontend và worker.
- `/aidlc-docs`: Chỉ chứa tài liệu AI-DLC, không chứa application code.

Nếu worker dùng ngôn ngữ khác backend, `/contracts` là ranh giới tích hợp. Dữ liệu job phải có `schemaVersion`, `jobId`, `type`, correlation ID và reference ID; dữ liệu nhạy cảm không được nhúng khi có thể tải qua scoped service.

## 4. Chiến lược nhóm 5 người

| Wave | Unit | Cách thực hiện |
|---|---|---|
| 0 | U01 | Cả nhóm chia Foundation theo identity/auth, file, job, audit và contract; hoàn tất integration gate chung |
| 1 | U02 | Hoàn thành subject/class/enrollment và public authorization contracts |
| 2 | U03 và U08 | Có thể làm song song sau U01/U02; integration gate xác nhận content access và entitlement contract |
| 3 | U04 | Hoàn thành assessment authoring/publication trên scope môn/lớp ổn định |
| 4 | U05 | Hoàn thành individual/group submission dựa trên publication contract |
| 5 | U06 | Hoàn thành grading, AI và Code Lab dựa trên immutable submission |
| 6 | U07 | Hoàn thành reporting/notification projection và kiểm thử hành trình đầu-cuối |

Mỗi unit có một owner chính nhưng ít nhất một reviewer khác. Integration owner luân phiên theo wave. Cuối mỗi wave phải chạy unit test, module integration test, contract test và các journey test liên quan trước khi mở wave phụ thuộc.

## 5. Integration gates

| Gate | Điều kiện bắt buộc |
|---|---|
| G0 Foundation | Authentication, authorization, artifact, job và audit contracts hoạt động; fail closed khi dependency lỗi |
| G1 Academic | Subject/class/enrollment scope được kiểm tra server-side; module khác không đọc bảng Academic trực tiếp |
| G2 Content/Payment | Learner chỉ đọc nội dung được phép; RAG job và payment webhook idempotent |
| G3 Assessment | Draft/review/publish đúng role/scope; AI không tự publish |
| G4 Submission/Group | Full XML immutable; đúng leader mới nộp DOCX; submission không bị nhân đôi |
| G5 Grading/AI/Code | Instructor chọn phương thức chấm; AI chỉ tạo proposal; bài chung không gửi AI; sandbox có giới hạn |
| G6 Reporting/Notification | Read model không vượt quyền; lỗi provider không rollback nghiệp vụ; journey MVP chạy đầu-cuối |

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
- Integration gate của wave đạt trước khi bắt đầu unit phụ thuộc.

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
