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
| ContentService | Version/publish nội dung và material metadata | Không xử lý vector trực tiếp trong request |
| LearningService | Access check và progress | Không trả dữ liệu học viên khác |
| BankService | Rubric/question versions | Không sửa hồi tố version đã dùng |
| AssessmentService | Draft/review/publish/retire | Không để AI tự publish |
| SubmissionService | Autosave, submit, receipt, immutable artifact | Không chấm hoặc sửa bản đã nộp |
| GradingService | Manual/deterministic/AI proposal review/finalize | Không cho AI quyết định final grade |
| AiOrchestrationService | Context scope, quota, job, provider port | Không sở hữu dữ liệu nguồn hoặc grade cuối |
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
3. Leader hiện tại nộp DOCX chung; server kiểm tra leader tại thời điểm submit.
4. GradingService có thể tạo AI proposal cho phần cá nhân nếu giảng viên chọn.
5. Bài DOCX chung luôn vào manual review; giảng viên đối chiếu các phần cá nhân.

### RAG/AI authoring

1. ContentService đăng ký material và tạo artifact.
2. Scan thành công phát event enqueue ingestion.
3. Worker parse/chunk/index qua RAG port và cập nhật trạng thái.
4. AiOrchestrationService chỉ lấy source trong subject/class scope của actor.
5. Bản AI sinh ra là assessment draft, cần human review trước publish.

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
