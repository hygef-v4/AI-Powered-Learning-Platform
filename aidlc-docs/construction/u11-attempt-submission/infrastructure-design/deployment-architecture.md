# U11 Individual Work & Quiz Submission — deployment-architecture

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 18, 19, 20, 21, 22, 23, 24, 26, 28, 29, 79. **Primary stories:** US-ASM-003, US-ASM-012. **Requirements:** FR-018/019/030/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u11-attempt-submission` không quyết định chức năng hiện hành.

## Mapping triển khai

Logical unit chạy trong backend/worker chung; data PostgreSQL theo owner, cache/rate Redis không nguồn durable business/quota. Package legacy mục tiêu review `attempts`; không tạo deployable riêng.

Nguồn hạ tầng: [shared-infrastructure](../../shared-infrastructure.md).

- Start snapshot/policy notice → autosave expectedVersion → submit transaction freezes snapshot/receipt → outbox submission → U15.
- Draft feedback → verify ownership/teacher policy + capture contentVersion → U13 personal task → result linked version, never mutate official record.
- Browser visibility event → authenticated minimal payload → dedup/rate/state/policy → persist; query → current authorization → trusted/missing-data flags and per-member timeline.

## Deploy/rollback/secret

- Docker image theo commit SHA, không latest; chỉ reverse proxy public; migration additive có data mapping/backup trước thay đổi và rollback compatibility.
- Credentials protected field với encryption key ngoài DB; không plaintext Settings/environment dump/logs. Backup archive encryption key và recovery secrets lưu riêng.
- Ready/health của dependencies rõ; ports down không trả empty/0/SUCCESS giả. AI unavailable không chặn manual work; event retry/lease tối đa hữu hạn.
- Trước demo chạy isolated restore, negative authorization, migration validation và checklist unit; runtime chưa được chứng minh bởi revision docs.

## Unit integration risks

- Concurrent autosave stale conflict; double submit stable receipt; deadline controls server; enrollment revoke denies save/feedback/activity.
- Monitoring off no events, dedup/out-of-order/offline gaps/iframe legitimate; wrong member/work reject; no grade side effects.
- UC29 vs79 state distinctions; Student shared denied, Graded feedback disabled deny, stale feedback not overwrite.
