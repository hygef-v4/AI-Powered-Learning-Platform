# Construction sync theo Inception 66 UC — 2026-10-13

> Historical synchronization record. Current scope and validation are superseded by [65-UC table synchronization](../../inception/plans/uc-table-65-sync-2026-10-13.md). Counts/checklists below retain the earlier revision evidence and are not current implementation approval.

**Raw request:** giúp tôi đồng bộ construction theo inception mới

Documentation-only continuation; sửa các design/plan hiện hành cho scope người dùng đã chốt, không triển khai code hoặc tự chuyển stage approvals. Không hỏi lại các quyết định đã thống nhất; D01–D04 giữ pending và numeric backup/activity settings để vận hành xác nhận trước enable.

## Checklist

- [x] Đọc workflow/state/Inception/contracts và identified gaps, lưu baseline/manifest trước thay active docs.
- [x] Đồng bộ current contract 66 UC/primary owners, authorities, DTO/ports/events và migration constraints.
- [x] Đồng bộ 15 unit design (functional/NFR/infrastructure), giữ quy tắc chi tiết tương thích U01/U02/U04/U12.
- [x] Revision U07 Usage/Quota, U13 API connections/MCP; retire payment/code; Assignment/Quiz/doc optional Draw.io; references core/Lesson extension.
- [x] Backup/restore/encrypted archive, activity opt-in/member review, early feedback, email important/backup failure và UC consolidation.
- [x] Rewrite current code-generation plans thành delta chưa code; giữ bản completed/approval gốc trong snapshot.
- [x] Kiểm links/Markdown/UC-story-owner alignment, stale active logic và record result.
- [ ] Code/contracts/migrations/runtime tests/restore/API-MCP client integration (ngoài request docs).

## Dependency và iteration

Wave1 U03/U02/U01/U04 nền; Iter1 chỉ Admin môn/account/Settings/audit/backup foundation/dashboard. U03 define neutral quota/UI status ports, không hard-depend U07/U16. Wave2 U05 references/U06/U07/U12/U08 config; Wave3 U09/U11/U14/U15; Wave4 U13/U16 hoặc AI adapter khi nền/quota đủ. Iter2 triển khai lớp/làm/nộp/chấm/feedback/activity. OTP U01 có Iter1; email backup failure U16 tích hợp khi adapter sẵn sàng; no fake gate completion.

## Validation gates cần code evidence

G1 auth/admin/settings/audit/backup foundation; G2 refs/rubric/MCQ/groups/lifecycle/quota; G3 bốn tổ hợp Assignment và Quiz không Lesson, autosave/group/manual grade/activity; G4 provider API/MCP/revoke/usage/version/notifications. Manual workflow không chờ AI. Isolated restore với dữ liệu nhiều unit trước demo; quota concurrency/unknown dispatch phải integration test.

## Extension scope

Security subset 03/04/05/08/09/12/15: auth/input/secrets/audit/failure design constraints; Resiliency subset04/06/10: deploy/health/timeout/failure constraints; runtime chưa tested. Remaining rules N/A theo state; PBT disabled. Backup archive bảo vệ riêng, không silently enable full DB encryption/proctoring/monitoring stack.

## Không suy diễn hoàn thành

Theo repository stage gates, cần user review design/delta trước implementation và evidence thật mới đánh code tests pass. User request sync là authorization sửa tài liệu, không mặc định approval mới cho Code Generation Part2. Không sửa các bảng Stage Progress cũ hoặc approval cũ.

## Kết quả kiểm tra static

PASS: 169 current Construction artifacts + 24 Inception artifacts; 66 UC owners và 51 stories khớp, 34 active FR được mapping (5 IDs retired), tables/fences/local links hợp lệ; 172 snapshot hashes đúng. 15 current delta code plans không có implementation checkbox completed. Kiểm loại bài/credit/account-revocation cũ không còn trong active designs. Không chạy runtime tests vì không sửa application code.

Rà soát cuối sửa U01: loại account credit fields/routes và grace window quyền 15 phút để current authority được kiểm request tiếp theo; bổ sung assigned-educator submission reader cho MCP theo scope đã có ở Inception. Schema/SDK/client compatibility là nghiệm thu khi code, không tự xác nhận.
