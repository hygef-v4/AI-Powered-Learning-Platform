# U15 Grading & Gradebook — infrastructure-design

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 37, 38, 39, 40. **Primary stories:** US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-INT-001. **Requirements:** FR-008/009/020/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u15-grading` không quyết định chức năng hiện hành.

## Mapping triển khai

Logical unit chạy trong backend/worker chung; data PostgreSQL theo owner, cache/rate Redis không nguồn durable business/quota. Package legacy mục tiêu review `grading`; không tạo deployable riêng.

Nguồn hạ tầng: [shared-infrastructure](../../shared-infrastructure.md).

- Submission event → register scope/snapshot → teacher manual or selected-source AI request → validate proposal/rubric → teacher confirms → finalize → publish event.
- Student view grade only published own; teacher export/Gradebook official Assignments only via U16.
- UC37 activity tab → authorize → U11 scoped query → display known intervals/gaps; no grade mutation or verdict automation.

## Deploy/rollback/secret

- Docker image theo commit SHA, không latest; chỉ reverse proxy public; migration additive có data mapping/backup trước thay đổi và rollback compatibility.
- Credentials protected field với encryption key ngoài DB; không plaintext Settings/environment dump/logs. Backup archive encryption key và recovery secrets lưu riêng.
- Ready/health của dependencies rõ; ports down không trả empty/0/SUCCESS giả. AI unavailable không chặn manual work; event retry/lease tối đa hữu hạn.
- Trước demo chạy isolated restore, negative authorization, migration validation và checklist unit; runtime chưa được chứng minh bởi revision docs.

## Unit integration risks

- Admin/R2-only denied; stale proposal reject; AI cannot publish/finalize via REST/MCP; unpublished grade hidden.
- Quiz Practice stays outside Gradebook; group adjustment bounds/audit; activity viewing never changes grade; masked missing intervals.
