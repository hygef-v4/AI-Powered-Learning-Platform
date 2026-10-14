# U15 Grading & Gradebook — nfr-requirements

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-INT-001. **Requirements:** FR-008/009/020/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u15-grading` không quyết định chức năng hiện hành.

## Required controls

- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

- Validation MIME/XML/schema/IDs/filter allowlists phù hợp unit, bounded payloads và rate limit phía server. Không log secret, raw prompt hoặc dữ liệu học vụ nhạy cảm.
- Truy vấn phân trang/index theo owner/resource/time, timeout hữu hạn mọi port/provider; không promise external API p95 như query local.
- Target baseline local APIs p95 500ms tại 100 concurrent/1000 accounts là mục tiêu cần benchmark, chưa có evidence; heavy files/AI/backup chạy worker và status queries.
- Session/password/header controls giữ SEC-001–006; quota/backup/credential protected riêng, không yêu cầu mã hóa toàn DB.

## Unit-specific NFR risks

- UC42 submissions/work detail plus activity review assigned teacher R3/R4. Admin or manager only R2 denied. Quiz Practice activity read does not create Gradebook.
- Only human teacher confirms Assignment GRADED official result, finalizes/publishes. AI proposal separate PENDING/READY/ACCEPTED/REJECTED, never automatically final/publish.
- Rubric scores deterministic by checklist/max, linked submission/rubric version; proposal stale if submission changed. Manual override requires reason/audit; batch finalize rejects/informs invalid rows, no silent grading.
- Practice Quiz scorer and Assignment feedback not official; answer reveal follows snapshot policy; Student own published official grades only.
- Group common document score + manual contribution adjustment, bounded and explained; activity/number edits cannot determine contribution or cheating score automatically.
- ActivityQueryPort U11 projection per member/work, after current class/policy check; disabled/no data/unavailable distinct. Does not mutate grading entities from telemetry.

## Test evidence cần có

- Admin/R2-only denied; stale proposal reject; AI cannot publish/finalize via REST/MCP; unpublished grade hidden.
- Quiz Practice stays outside Gradebook; group adjustment bounds/audit; activity viewing never changes grade; masked missing intervals.

Security enabled subset 03/04/05/08/09/12/15: constraints auth/input/audit/secret/failure được thể hiện, chưa runtime verified. Resiliency enabled subset 04/06/10: deployment/health/timeouts giữ; rule khác N/A theo state, PBT disabled.
