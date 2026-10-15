# U13 AI Connections & MCP — Code Generation Plan — revision chưa triển khai

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 31, 32. **Primary stories:** US-AIG-001, US-AIG-002, US-AIG-003, US-AIC-001, US-AIC-002. **Requirements:** FR-006/008/012/021/030/035/037.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u13-ai-code-execution` không quyết định chức năng hiện hành.

## Authorization và trạng thái

User chỉ yêu cầu đồng bộ tài liệu Construction theo Inception. Đây là kế hoạch revision, không chạy Code Generation Part 2 hoặc đánh completed. Checklist/approval/code baseline giữ tại [plan cũ](../../historical/baseline-before-66-uc-2026-10-13/plans/code-generation-plan/u13-ai-code-execution-code-generation-plan.md); approval 05/10 không tự áp dụng scope mới. Không reset lịch sử code đã sinh thành chưa sinh; checklist dưới đây chỉ delta hiện hành.

## Paths cần review trước sửa code

- Existing backend owner/package: `backend/src/main/java/edu/aiplatform/aiexecution`; frontend `frontend/src`, public contracts `backend/src/main/java/edu/aiplatform/contracts` nếu tồn tại; migration `backend/src/main/resources/db/migration`; tests `backend/src/test` và frontend test files.
- Exact classes/routes/migration numbers xác định qua repository inventory trước implementation; không invent files đã tồn tại. Legacy package/folder tên cũ không cho phép tiếp tục payment/code.
- Source-of-truth [current shared contract](../../current-srs-contract.md), unit design theo current folder. [Inception dependency](../../../inception/application-design/unit-of-work-dependency.md) là thứ tự hiện hành.

## Checklist delta

- [ ] **Bước 1:** Inventory code/contracts/migrations baseline, xác định reuse/removal, mapping dữ liệu và exact paths; không drop records/snapshots trước backup/restore.
- [ ] **Bước 2:** Review DTO/status/public ports theo U13 owner; authorizations hiện hành, expectedVersion/idempotency và error projection; replace adapters fail-closed trước tích hợp thật.
- [ ] **Bước 3:** Migration owned entities/constraints/index/retention; preserve IDs/historical snapshots, validate old-to-new mapping trên copy database.
- [ ] **Bước 4:** Domain rules/services/transactions và recovery theo functional-design; dependent services gọi port, không repositories xuyên owner.
- [ ] **Bước 5:** API layer guards/validation/rate/CSRF, minimal projections, sanitized audit/outbox cùng owner transaction; external timeout/retry bounded.
- [ ] **Bước 6:** Frontend state/forms/query permissions theo entry UC hiện hành; conflicts/unavailable rõ; không client-only policy/security.
- [ ] **Bước 7:** Coder viết meaningful unit/service/API/UI tests của delta, negative permissions/state/duplicate/concurrency; không chỉ mirror implementation.
- [ ] **Bước 8:** Tester khác viết integration cases từ requirements/design và chạy adapter thật/sandbox/local mock phù hợp, phân biệt cases viết xong và runtime pass.
- [ ] **Bước 9:** Update deployment env/health/secret rotation và runbook; test migration/rollback/restore nếu unit tác động dữ liệu hoặc credentials.
- [ ] **Bước 10:** Review evidence/FR-story-UC traceability, fix/retest; user review trước next code stage, không tự thay milestones/nhân sự.

## Unit-specific implementation scope

- U13 AI Connections & MCP primary UC31/32; supports institutional configuration behind U03 UC14/15; package legacy aiexecution không giữ code execution. Gemini/OpenAI/Claude là API providers, không bảo đảm chat subscription cấp API hoặc mọi agent client hỗ trợ MCP.
- Connection scope INSTITUTIONAL/PERSONAL; shared chỉ Admin manage/test/revoke bằng synthetic payload và không học vụ. Personal chính Student/Teacher/SubjectManager owner configure; credentials encrypted/keyVersion, masked không readback, không log/return key/ciphertext.
- Provider endpoints/model allowlist cấu hình server, không user arbitrary URL tránh SSRF; model compatibility/test có evidence trước dùng. Test synthetic bounded call có thể bị provider tính phí phải hiển thị nguồn/bên trả; không dùng nội dung học vụ.
- Request direct captures actor/target/version/context/source/model/requestRef, domain authorize/policy; institutional educator-only U07 reserve atomic trước dispatch. Personal owner/rate controls không shared grant; no auto fallback.
- AiTask QUEUED/RUNNING/READY/FAILED/UNKNOWN/CANCELLED; record dispatch checkpoint/provider request id và output/usage provenance. Timeout unknown không blind retry có thể double bill, reconcile U07 giữ uncertainty. Domain accepts validated structured result/version not provider tự publish.
- Minimal scoped context strips hidden keys/unpublished grades/other members private data. Student feedback dispatch/recording requires an owned SUBMITTED PRACTICE snapshot; draft/IN_PROGRESS and Graded requests are denied. Group access follows D03 where applicable. Educator authoring/proposals remain separate; source files outside scope are filtered.
- McpCredential gắn ALAP actor/scope/expiry/revokedAt và hash, không provider key; credential create/revoke owner; mỗi tool tái kiểm actor ACTIVE/session binding/academic permissions/version/state. Chosen HTTP transport/client auth needs compatibility test; no broad anonymous bearer secret.
- Initial tools: read_assignment_context, read_authorized_material, read_own_work, save_assignment_draft(expectedVersion), record_private_feedback, record_grading_proposal. No submit official/publish_grade/restore/credential read tool. Returned data and incoming tool parameters untrusted schema/size/rate validated.
- External agent calls own LLM; ALAP receives tool metadata and optionally externallyReported model/tokens, not providerMeasured. It cannot know external token usage without report; unavailable not zero, tool count not model tokens. MCP calls do not consume institutional API quota.
- Audit connection/model/quota/provenance references not raw prompt/key. Revocation denies next call; running result must recheck current policy/rights and input version before domain apply. Tool replay expectedVersion/requestRef guards duplicate writes; no exactly-once external promise.

## Independent integration acceptance

- Three provider adapters synthetic/invalid key/unsupported model, secret not in response/log, personal owner and Admin synthetic-only.
- Student direct or MCP cannot institutional grant; quota exhausted no personal fallback; concurrency/idempotency/checkpoint unknown call not redelivered blindly.
- MCP hidden answers/out-of-class/member drafts denied; revoked scope next request denied; save stale version conflict; no official publish tool.
- External tokens reported/unavailable not measured/zero; prompt injection cannot alter tool allowlist/scope; late AI result cannot overwrite changed draft or grade.

## DoD

Requirements owner, test-case owner, coder khác nhau theo nhóm; implementation readiness/compatibility và unresolved decisions xử lý trước enable feature tương ứng. No runtime pass/checklist tick chỉ vì docs viết xong; codes under workspace root, không aidlc-docs.
