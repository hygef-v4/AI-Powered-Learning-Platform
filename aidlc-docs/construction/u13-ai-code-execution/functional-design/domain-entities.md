# U13 AI Connections & MCP — domain-entities

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 74, 75. **Primary stories:** US-AIG-001, US-AIG-002, US-AIG-003, US-AIC-001, US-AIC-002. **Requirements:** FR-006/008/012/021/030/035/037.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u13-ai-code-execution` không quyết định chức năng hiện hành.

## Owned entities và ràng buộc

- AiConnection: id/ownerId nullable institutional/scope/provider/modelAllowlist/encryptedCredential/keyVersion/status/version/lastTestResult/timestamps; encrypted secret never DTO serialized.
- McpCredential: id/actorId/tokenHash/allowedToolScopes/expiry/revokedAt/version; client label optional and no claim of LLM usage authority.
- AiTask: id/requestRef unique/fingerprint/actorId/connectionRef/targetRef/contentVersion/taskType/source/model/policySnapshot/state/dispatchCheckpoint/providerRequestRef/resultRef/usageProvenance/safeError/timestamps.
- CompatibilityInstruction: provider API setup and per-agent MCP tested client/transport/auth/version/date; untested clients labeled unverified, not universally supported.

## Boundary

managePersonalConnection; manageSharedConnection(admin); testSyntheticConnection; revoke; requestAi(task, scopedContext, connectionRef); create/revokeMcpCredential; scoped MCP tools

Không truy cập repository unit khác. IDs cross-unit là refs được kiểm qua ports, foreign key cụ thể chỉ tạo theo schema review; snapshot lịch sử không cascade delete. Schema naming là mục tiêu migration đề xuất, không khẳng định có trong database hiện tại.

## State và integrity

- U13 AI Connections & MCP primary UC74/75; supports institutional configuration behind U03 UC70/71; package legacy aiexecution không giữ code execution. Gemini/OpenAI/Claude là API providers, không bảo đảm chat subscription cấp API hoặc mọi agent client hỗ trợ MCP.
- Connection scope INSTITUTIONAL/PERSONAL; shared chỉ Admin manage/test/revoke bằng synthetic payload và không học vụ. Personal chính Student/Teacher/SubjectManager owner configure; credentials encrypted/keyVersion, masked không readback, không log/return key/ciphertext.
- Provider endpoints/model allowlist cấu hình server, không user arbitrary URL tránh SSRF; model compatibility/test có evidence trước dùng. Test synthetic bounded call có thể bị provider tính phí phải hiển thị nguồn/bên trả; không dùng nội dung học vụ.
- Request direct captures actor/target/version/context/source/model/requestRef, domain authorize/policy; institutional educator-only U07 reserve atomic trước dispatch. Personal owner/rate controls không shared grant; no auto fallback.
- AiTask QUEUED/RUNNING/READY/FAILED/UNKNOWN/CANCELLED; record dispatch checkpoint/provider request id và output/usage provenance. Timeout unknown không blind retry có thể double bill, reconcile U07 giữ uncertainty. Domain accepts validated structured result/version not provider tự publish.
- Minimal scoped context strips hidden keys/unpublished grades/other members private data. Student feedback dispatch/recording requires an owned SUBMITTED PRACTICE snapshot; draft/IN_PROGRESS and Graded requests are denied. Group access follows D03 where applicable. Educator authoring/proposals remain separate; source files outside scope are filtered.
- McpCredential gắn ALAP actor/scope/expiry/revokedAt và hash, không provider key; credential create/revoke owner; mỗi tool tái kiểm actor ACTIVE/session binding/academic permissions/version/state. Chosen HTTP transport/client auth needs compatibility test; no broad anonymous bearer secret.
- Initial tools: read_assignment_context, read_authorized_material, read_own_work, read_authorized_submission (assigned educator only), save_assignment_draft(expectedVersion), record_private_feedback, record_grading_proposal. No submit official/publish_grade/restore/credential read tool. Returned data and incoming tool parameters untrusted schema/size/rate validated.
- External agent calls own LLM; ALAP receives tool metadata and optionally externallyReported model/tokens, not providerMeasured. It cannot know external token usage without report; unavailable not zero, tool count not model tokens. MCP calls do not consume institutional API quota.
- Audit connection/model/quota/provenance references not raw prompt/key. Revocation denies next call; running result must recheck current policy/rights and input version before domain apply. Tool replay expectedVersion/requestRef guards duplicate writes; no exactly-once external promise.
