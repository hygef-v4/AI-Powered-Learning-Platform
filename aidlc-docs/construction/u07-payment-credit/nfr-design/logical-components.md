# U07 AI Usage & Teacher Quota — logical-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 77. **Primary stories:** US-AIC-003, US-AIC-004. **Requirements:** FR-021/034/036.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u07-payment-credit` không quyết định chức năng hiện hành.

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

reserveTeacherQuota(actor, connection, model, requestRef, estimate); reconcileUsage; markUsageUnknown; listOwnUsage; getTeacherQuotaPolicy/setTeacherQuota(admin, policy, version) (support UC 70; owner U07)

## Owned data

- TeacherQuotaPolicy: educatorId/provider/model, hourLimit/dayLimit, globalPolicyRef, timezone, enabled, version. Numeric settings unresolved D04.
- UsageWindow: scope(actor/global), provider/model, windowType(HOUR/DAY), windowStart, reservedTokens, measuredTokens, uncertainTokens, version; unique scope/window.
- QuotaReservation: requestRef unique, fingerprint, actorId/connectionRef/model/targetRef/contentVersion, estimate/bound, state RESERVED/DISPATCHED/SETTLED/CANCELLED/UNKNOWN, timestamps.
- AiUsageRecord: requestRef/owner/source/provider/model/input/output/cache/reasoning nullable, provenance providerMeasured/estimated/externallyReported/unavailable, state/error/version.
- RequestCheckpoint: dispatch marker and provider request ID if available; duplicate worker does not dispatch when previous status unknown.

## Critical paths

- UC70 Admin quota edit via U03 → U07 validate ACTIVE/version/policy → audit; quota owner remains U07.
- Shared request → authorize → reserve all applicable windows transaction → dispatch checkpoint → provider response → reconcile transaction → emit usage status.
- Crash/timeout → UNKNOWN with conservative retained reservation → reconciliation task/operational resolution using evidence; no exactly-once provider promise.
- UC77 own history → source/provenance/usage; Admin aggregate shared-only; external token report nullable not zero.
