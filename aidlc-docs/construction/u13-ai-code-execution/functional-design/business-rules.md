# U13 AI Connections & MCP — business-rules

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 31, 32. **Primary stories:** US-AIG-001, US-AIG-002, US-AIG-003, US-AIC-001, US-AIC-002. **Requirements:** FR-006/008/012/021/030/035/037.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u13-ai-code-execution` không quyết định chức năng hiện hành.

## Quy tắc nghiệp vụ

- **BR-U13-01**: U13 AI Connections & MCP primary UC31/32; supports institutional configuration behind U03 UC14/15; package legacy aiexecution không giữ code execution. Gemini/OpenAI/Claude là API providers, không bảo đảm chat subscription cấp API hoặc mọi agent client hỗ trợ MCP.
- **BR-U13-02**: Connection scope INSTITUTIONAL/PERSONAL; shared chỉ Admin manage/test/revoke bằng synthetic payload và không học vụ. Personal chính Student/Teacher/SubjectManager owner configure; credentials encrypted/keyVersion, masked không readback, không log/return key/ciphertext.
- **BR-U13-03**: Provider endpoints/model allowlist cấu hình server, không user arbitrary URL tránh SSRF; model compatibility/test có evidence trước dùng. Test synthetic bounded call có thể bị provider tính phí phải hiển thị nguồn/bên trả; không dùng nội dung học vụ.
- **BR-U13-04**: Request direct captures actor/target/version/context/source/model/requestRef, domain authorize/policy; institutional educator-only U07 reserve atomic trước dispatch. Personal owner/rate controls không shared grant; no auto fallback.
- **BR-U13-05**: AiTask QUEUED/RUNNING/READY/FAILED/UNKNOWN/CANCELLED; record dispatch checkpoint/provider request id và output/usage provenance. Timeout unknown không blind retry có thể double bill, reconcile U07 giữ uncertainty. Domain accepts validated structured result/version not provider tự publish.
- **BR-U13-06**: Minimal scoped context strips hidden keys/unpublished grades/other members private data. Student feedback dispatch/recording requires an owned SUBMITTED PRACTICE snapshot; draft/IN_PROGRESS and Graded requests are denied. Group access follows D03 where applicable. Educator authoring/proposals remain separate; source files outside scope are filtered.
- **BR-U13-07**: McpCredential gắn ALAP actor/scope/expiry/revokedAt và hash, không provider key; credential create/revoke owner; mỗi tool tái kiểm actor ACTIVE/session binding/academic permissions/version/state. Chosen HTTP transport/client auth needs compatibility test; no broad anonymous bearer secret.
- **BR-U13-08**: Initial tools: read_assignment_context, read_authorized_material, read_own_work, read_authorized_submission (assigned educator only), save_assignment_draft(expectedVersion), record_private_feedback, record_grading_proposal. No submit official/publish_grade/restore/credential read tool. Returned data and incoming tool parameters untrusted schema/size/rate validated.
- **BR-U13-09**: External agent calls own LLM; ALAP receives tool metadata and optionally externallyReported model/tokens, not providerMeasured. It cannot know external token usage without report; unavailable not zero, tool count not model tokens. MCP calls do not consume institutional API quota.
- **BR-U13-10**: Audit connection/model/quota/provenance references not raw prompt/key. Revocation denies next call; running result must recheck current policy/rights and input version before domain apply. Tool replay expectedVersion/requestRef guards duplicate writes; no exactly-once external promise.
- **BR-U13-11**: Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- **BR-U13-12**: expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- **BR-U13-13**: Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- **BR-U13-14**: No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Negative scenarios / acceptance

- Three provider adapters synthetic/invalid key/unsupported model, secret not in response/log, personal owner and Admin synthetic-only.
- Student direct or MCP cannot institutional grant; quota exhausted no personal fallback; concurrency/idempotency/checkpoint unknown call not redelivered blindly.
- MCP hidden answers/out-of-class/member drafts denied; revoked scope next request denied; save stale version conflict; no official publish tool.
- External tokens reported/unavailable not measured/zero; prompt injection cannot alter tool allowlist/scope; late AI result cannot overwrite changed draft or grade.
