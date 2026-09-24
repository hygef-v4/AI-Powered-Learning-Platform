# Services and Orchestration

## 1. Service layer pattern

Mỗi module có application service làm transaction boundary. Controller nhận REST request, validate hình thức, tạo actor context rồi gọi service. Domain object thực thi invariants; repository/port xử lý persistence hoặc external dependency. Cross-module workflow dùng application service và domain event/outbox.

## 2. Dịch vụ nghiệp vụ

| Service | Orchestration chính | Không được làm |
|---|---|---|
| AccountService | Activation, session, reset, account state | Không cấp role ngoài AuthorizationAdminService |
| AuthorizationService | Role + subject/class/object decision | Không tin quyền do frontend truyền |
| AcademicService | Subject/class/assignment/enrollment | Không xóa lịch sử học tập |
| GroupService | Membership, leader invariant, allocation | Không cho learner tự đổi leader |
| ContentService | Version/publish nội dung, file/YouTube source và transcript metadata | Không xử lý download/phiên âm/vector trực tiếp trong request |
| LearningService | Kiểm tra enrollment và entitlement trước khi trả nội dung/lớp | Không trả dữ liệu học viên khác hoặc nội dung ngoài quyền truy cập |
| BankService | Rubric/question versions | Không sửa hồi tố version đã dùng |
| AssessmentService | Tạo đề thủ công/AI draft, review/publish/retire; template/copy assignment lineage; tạo version mới khi sửa assignment đã giao; simulation policy và attempt snapshot contract | Không sao chép khóa học/lớp, không sửa version đã phát hành, không để AI tự publish hoặc đổi policy sau attempt đầu tiên |
| SubmissionService | Autosave, submit, receipt, immutable artifact và group composite orchestration | Không chấm hoặc sửa bản đã nộp/source part |
| GradingService | Manual/deterministic/AI proposal review, manual composite grade và per-student finalize | Không cho AI hoặc công thức tự động quyết định final grade |
| AiOrchestrationService | Context scope, quota, job, provider port | Không sở hữu dữ liệu nguồn hoặc grade cuối |
| CodeExecutionService | Xác thực quyền chạy Code Lab, test visibility/quota, tạo sandbox job và lưu kết quả run bất biến | Không chạy mã trong API/worker không cô lập hoặc tiết lộ hidden tests |
| FileArtifactService | Upload/download có phân quyền, kiểm tra loại file/scan/checksum; validate Draw.io XML và vô hiệu external entities/XXE trước khi lưu artifact bất biến | Không phát signed access cho actor ngoài scope hoặc đưa XML chưa kiểm tra sang AI |
| JobService | Enqueue, lease, retry/backoff, dead-letter và scoped status/correlation | Không quyết định nghiệp vụ, cấp quyền dữ liệu hoặc sửa kết quả domain trực tiếp |
| PaymentService | Intent, webhook verification, entitlement | Không tin browser redirect |
| ReportingService | Read model/report/export jobs | Không vượt row/object authorization |
| NotificationService | Outbox-to-delivery workflow | Không rollback nghiệp vụ khi provider lỗi |
| AuditService | Append-only event và authorized query | Không cung cấp update/delete API |

## 3. Orchestration quan trọng

### Nộp Draw.io và AI đề xuất chấm

1. SubmissionService authorize learner và publication.
2. FileArtifactService validate XML, tắt external entities, checksum và lưu full XML immutable.
3. SubmissionService tạo submission/receipt tham chiếu full artifact.
4. Giảng viên mở full XML qua authorized access.
5. Khi giảng viên chọn AI, GradingService tạo grading request.
6. AiOrchestrationService enqueue job; worker tạo compact derived XML từ full artifact.
7. Adapter AI nhận compact XML + rubric tối thiểu.
8. Proposal được lưu riêng; GradingService cho giảng viên duyệt/sửa/bỏ.
9. Chỉ GradingService dưới actor giảng viên mới finalize/publish điểm.

### Bài tập nhóm

1. GroupService tạo nhóm, đảm bảo đúng một leader và phân phần cá nhân.
2. Mỗi thành viên nộp phần của mình qua SubmissionService.
3. Giảng viên yêu cầu SubmissionService tạo composite theo cấu trúc và các version phần đã nộp; worker tạo derived artifact nhưng không sửa source.
4. Giảng viên xem trước, đổi thứ tự/loại phần và chốt composite version.
5. GradingService có thể tạo AI proposal cho phần cá nhân nếu giảng viên chọn.
6. Composite luôn vào manual review với tiêu chí tích hợp/nhất quán; giảng viên nhập điểm cuối từng sinh viên từ evidence cá nhân và điểm chung, không có công thức bắt buộc.

### Tạo đề và phát hành assignment

1. Giảng viên hoặc Chủ nhiệm môn chọn scope, loại đề, rubric/question version, thời gian và chính sách lượt làm.
2. AssessmentService kiểm tra quyền và đọc `QuestionVersion`/`RubricVersion` qua BankService; giảng viên có thể soạn trực tiếp hoặc yêu cầu AiOrchestrationService tạo draft qua JobService.
3. AI chỉ tạo bản nháp từ nguồn Content/File được phép. RAG là cơ chế hỗ trợ truy xuất nguồn khi cần, không phải bước bắt buộc của luồng tạo đề.
4. Giảng viên duyệt, sửa, rồi phát hành một assignment version bất biến cho lớp; SubmissionService đóng băng assignment/question/rubric version khi learner bắt đầu attempt.
5. Khi sửa đề đã giao, AssessmentService tạo `version_no + 1` trên cùng `stable_key` và publication mới trỏ tới version mới; attempt cũ tiếp tục dùng snapshot cũ.

### Nạp nguồn RAG hỗ trợ AI

1. ContentService đăng ký material file hoặc URL video/playlist theo bài giảng.
2. File sạch hoặc URL hợp lệ phát event enqueue ingestion; video ưu tiên caption và fallback sang phiên âm audio.
3. Worker parse/transcribe, tạo transcript có timestamp, chunk/index qua RAG port và cập nhật trạng thái từng source.
4. AiOrchestrationService chỉ lấy source trong subject/class scope của actor.
5. Chỉ job tạo đề được actor yêu cầu mới dùng index này để sinh assessment draft; giảng viên duyệt trước publish.

### Thanh toán và entitlement

1. PaymentService tạo intent với idempotency key.
2. Gateway redirect phục vụ UX, không cấp quyền.
3. Verified webhook được kiểm tra signature, timestamp, amount và replay key.
4. Transaction ghi payment event và entitlement đúng một lần.
5. Reconciliation job tìm chênh lệch; sửa thủ công cần reason + audit.

## 4. Job policies

| Job | Retry | Idempotency/result |
|---|---|---|
| RAG ingestion | Hữu hạn + backoff | Theo material version/checksum |
| YouTube transcript ingestion | Hữu hạn + backoff; không retry lỗi URL/quyền vĩnh viễn | Theo video ID + transcript language/version |
| Group composite generation | Hữu hạn + backoff | Theo group assignment + ordered source version set |
| AI generation/grading | Hữu hạn theo failure class | Theo request ID; không nhân đôi proposal ngoài policy |
| Code execution | Không retry lỗi code; retry giới hạn lỗi hạ tầng | Theo run ID, sandbox result immutable |
| Notification | Hữu hạn + backoff | Theo event/recipient/channel |
| Payment reconciliation | Theo lịch và retry | Theo provider transaction + period |
| Report export | Retry lỗi tạm thời | Artifact có TTL và scope owner |

## 5. REST contract

- Base path `/api/v1`; OpenAPI là contract giữa Next.js và Spring Boot.
- Command có side effect quan trọng hỗ trợ `Idempotency-Key` khi phù hợp.
- Lỗi dùng problem-details an toàn, correlation ID và không lộ stack trace.
- Job dài trả `202 Accepted` + job ID; MVP dùng polling `GET /api/v1/jobs/{id}`.
- SSE/WebSocket chưa bắt buộc ở MVP; có thể bổ sung sau mà không thay REST business APIs.
