# U12 Group & Allocation — tech-stack-decisions

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 54, 55, 57. **Primary stories:** US-GRP-001, US-GRP-002. **Requirements:** FR-025.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u12-group-allocation` không quyết định chức năng hiện hành.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu | PostgreSQL + JPA; lưu nguyên khối bằng so khác biệt trạng thái gửi lên với DB | Một transaction |
| Ngẫu nhiên | `SecureRandom` + Fisher-Yates | Chuẩn |
| Frontend | Next.js + Tailwind; kéo thả thành viên giữa nhóm bằng `@dnd-kit`; component gắn vào màn của U04 (tab Students, Student Class Detail) và U08 (hộp phát hành) | Nhẹ, miễn phí; theo screen flow không có màn nhóm riêng |

## Quy tắc tích hợp revision hiện hành

- Groups belong class, not assessment. Teacher assigned R3/R4 creates/allocates; Admin/manager without teaching assignment denied.
- Members ACTIVE enrolled, one effective group/class; exactly one leader in members. Random assignment only ungrouped students, preserve existing groups, preview balanced difference <=1 before save.
- Leader-change request PENDING/APPROVED/REJECTED/CANCELLED; max one pending/group; reason/proposed member validated. Only assigned teacher decides, requester may cancel pending.
- Changing/removing leader must select replacement atomically. Removal current membership revokes edit/submit next call but preserves revision authorship.
- Publication readiness for ASSIGNMENT submissionMode GROUP through GroupReadinessPort; không separate Group product type.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Balanced preview preserve grouped students; concurrent version conflict; exactly leader; nonteacher denied.
- Member removal denies U14/AI context next call while historical author retained; group readiness missing leader blocks publish.
