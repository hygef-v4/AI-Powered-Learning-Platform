# U07 AI Usage & Teacher Quota — infrastructure-design

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 33. **Primary stories:** US-AIC-003, US-AIC-004. **Requirements:** FR-021/034/036.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u07-payment-credit` không quyết định chức năng hiện hành.

## Mapping triển khai

Logical unit chạy trong backend/worker chung; data PostgreSQL theo owner, cache/rate Redis không nguồn durable business/quota. Package legacy mục tiêu review `billing`; không tạo deployable riêng.

Nguồn hạ tầng: [shared-infrastructure](../../shared-infrastructure.md).

- UC15 Admin quota edit via U03 → U07 validate ACTIVE/version/policy → audit; quota owner remains U07.
- Shared request → authorize → reserve all applicable windows transaction → dispatch checkpoint → provider response → reconcile transaction → emit usage status.
- Crash/timeout → UNKNOWN with conservative retained reservation → reconciliation task/operational resolution using evidence; no exactly-once provider promise.
- UC33 own history → source/provenance/usage; Admin aggregate shared-only; external token report nullable not zero.

## Deploy/rollback/secret

- Docker image theo commit SHA, không latest; chỉ reverse proxy public; migration additive có data mapping/backup trước thay đổi và rollback compatibility.
- Credentials protected field với encryption key ngoài DB; không plaintext Settings/environment dump/logs. Backup archive encryption key và recovery secrets lưu riêng.
- Ready/health của dependencies rõ; ports down không trả empty/0/SUCCESS giả. AI unavailable không chặn manual work; event retry/lease tối đa hữu hạn.
- Trước demo chạy isolated restore, negative authorization, migration validation và checklist unit; runtime chưa được chứng minh bởi revision docs.

## Unit integration risks

- Parallel reserves cannot exceed reservation limits; Student denied; same ref replay no second reserve; different fingerprint conflict.
- Timeout/crash unknown retains uncertainty; retry not dispatch again silently; measured overage stored/blocks future.
- Personal/MCP never subtract shared quota; no usage shown as 0 when missing; timezone hour/day boundaries and source switch explicit.
