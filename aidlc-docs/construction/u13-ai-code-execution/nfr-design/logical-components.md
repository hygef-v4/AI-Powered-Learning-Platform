# U13 AI Connections & MCP — logical-components

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 74, 75. **Primary stories:** US-AIG-001, US-AIG-002, US-AIG-003, US-AIC-001, US-AIC-002. **Requirements:** FR-006/008/012/021/030/035/037.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u13-ai-code-execution` không quyết định chức năng hiện hành.

## Component responsibility

| Layer | Responsibility |
|---|---|
| Controller / MCP adapter | Deserialize/validate, authenticated actor, minimal response; gọi domain service |
| Application service | Scope/state/version, orchestration qua ports, transaction owner |
| Domain policy/model | Rules, transitions, snapshot invariants |
| Repository | Owned entities only; optimistic/locking/unique constraints |
| Outbound adapters | Timeout/retry/provenance; không quyết định học vụ |
| UI | Entry UC, form/query status, không client-only security |

## Owner contract

managePersonalConnection; manageSharedConnection(admin); testSyntheticConnection; revoke; requestAi(task, scopedContext, connectionRef); create/revokeMcpCredential; scoped MCP tools

## Owned data

- AiConnection: id/ownerId nullable institutional/scope/provider/modelAllowlist/encryptedCredential/keyVersion/status/version/lastTestResult/timestamps; encrypted secret never DTO serialized.
- McpCredential: id/actorId/tokenHash/allowedToolScopes/expiry/revokedAt/version; client label optional and no claim of LLM usage authority.
- AiTask: id/requestRef unique/fingerprint/actorId/connectionRef/targetRef/contentVersion/taskType/source/model/policySnapshot/state/dispatchCheckpoint/providerRequestRef/resultRef/usageProvenance/safeError/timestamps.
- CompatibilityInstruction: provider API setup and per-agent MCP tested client/transport/auth/version/date; untested clients labeled unverified, not universally supported.

## Critical paths

- UC74 owner personal or UC70/71 Admin institutional Settings delegated to U13 → validate provider/model/secret → encrypt server-side → audit → masked connection response; test synthetic and revoke independently.
- Direct task → current domain context → source resolve → if shared U07 reserve → checkpoint dispatch → adapter timeout/bounded response → validate result/reconcile usage → domain draft/private feedback/proposal guarded version.
- MCP authenticate ALAP actor → allowlist tool args → domain current authorization → minimal read or expectedVersion write → sanitize response/audit; agent calls model outside platform and reports proposal.
- Source unavailable/revoked/permission stale → fail safely, retain user work; external unknown result does not silently resubmit call or publish grades.
