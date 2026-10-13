# U15 Grading & Gradebook — frontend-components

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 37, 38, 39, 40. **Primary stories:** US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-INT-001. **Requirements:** FR-008/009/020/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u15-grading` không quyết định chức năng hiện hành.

## UI entry và API đề xuất

- /api/v1/classes/{id}/submissions; /api/v1/submissions/{id}/evaluation|ai-proposal|finalize|publish; /activity read via U11; batch operations expectedVersion.

## Component/state contract

- List/Detail/Editor phân theo quyền và entry UC hiện hành; query state loading/ready/empty/unavailable/error tách biệt.
- Form state draft/dirty/saving/saved/conflict/invalid; save gửi expectedVersion và requestRef khi có idempotent command; lỗi không xóa draft.
- State-changing requests không chỉ dựa ẩn nút; server authorize, version/policy errors cần refresh lại context trước retry.
- UI không phơi bày raw keys, hidden Quiz answers, unpublished grades hoặc dữ liệu ngoài scope.

## Hành vi riêng unit

- UC37 submissions/work detail plus activity review assigned teacher R3/R4. Admin or manager only R2 denied. Quiz Practice activity read does not create Gradebook.
- Only human teacher confirms Assignment GRADED official result, finalizes/publishes. AI proposal separate PENDING/READY/ACCEPTED/REJECTED, never automatically final/publish.
- Rubric scores deterministic by checklist/max, linked submission/rubric version; proposal stale if submission changed. Manual override requires reason/audit; batch finalize rejects/informs invalid rows, no silent grading.
- Practice Quiz scorer and Assignment feedback not official; answer reveal follows snapshot policy; Student own published official grades only.
- Group common document score + manual contribution adjustment, bounded and explained; activity/number edits cannot determine contribution or cheating score automatically.
- ActivityQueryPort U11 projection per member/work, after current class/policy check; disabled/no data/unavailable distinct. Does not mutate grading entities from telemetry.

## UI acceptance

- Admin/R2-only denied; stale proposal reject; AI cannot publish/finalize via REST/MCP; unpublished grade hidden.
- Quiz Practice stays outside Gradebook; group adjustment bounds/audit; activity viewing never changes grade; masked missing intervals.
