# U15 Grading & Gradebook — business-rules

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 37, 38, 39, 40. **Primary stories:** US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-INT-001. **Requirements:** FR-008/009/020/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u15-grading` không quyết định chức năng hiện hành.

## Quy tắc nghiệp vụ

- **BR-U15-01**: UC37 submissions/work detail plus activity review assigned teacher R3/R4. Admin or manager only R2 denied. Quiz Practice activity read does not create Gradebook.
- **BR-U15-02**: Only human teacher confirms Assignment GRADED official result, finalizes/publishes. AI proposal separate PENDING/READY/ACCEPTED/REJECTED, never automatically final/publish.
- **BR-U15-03**: Rubric scores deterministic by checklist/max, linked submission/rubric version; proposal stale if submission changed. Manual override requires reason/audit; batch finalize rejects/informs invalid rows, no silent grading.
- **BR-U15-04**: Practice Quiz scorer and Assignment feedback not official; answer reveal follows snapshot policy; Student own published official grades only.
- **BR-U15-05**: Group common document score + manual contribution adjustment, bounded and explained; activity/number edits cannot determine contribution or cheating score automatically.
- **BR-U15-06**: ActivityQueryPort U11 projection per member/work, after current class/policy check; disabled/no data/unavailable distinct. Does not mutate grading entities from telemetry.
- **BR-U15-07**: Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- **BR-U15-08**: expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- **BR-U15-09**: Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- **BR-U15-10**: No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Negative scenarios / acceptance

- Admin/R2-only denied; stale proposal reject; AI cannot publish/finalize via REST/MCP; unpublished grade hidden.
- Quiz Practice stays outside Gradebook; group adjustment bounds/audit; activity viewing never changes grade; masked missing intervals.
