# Execution Plan — Inception revision 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

65 active UC; 50 MVP stories + 1 extension; 15 logical units
## Phạm vi request

- [x] Đọc state, requirements, stories, application/unit artifacts và CLAUDE/rule details.
- [x] Đồng bộ scope hai loại bài và các quyết định AI đã chốt ở cấp Inception.
- [x] Viết catalog/migration và unit/story mapping mới; giữ bản cũ lịch sử.
- [x] Ghi các quyết định chưa chốt, không giả vờ có survey/approval/runtime evidence.
- [x] Revision Construction design/ports/schema mục tiêu và code plans theo scope mới.
- [ ] Revision contracts/code/migrations thực tế và runtime tests (ngoài request docs).
- [ ] Xác minh Google Reports/diagrams theo catalog mới (ngoài request này).

## Iteration theo yêu cầu nhóm

| Iter | Outcome |
|---|---|
| 1 | Login/roles, code skeleton, Admin accounts/subjects/Settings/audit, basic Admin Dashboard. U04 chỉ phần Admin; không toàn lớp/enrollment. API chung có thể cấu hình nhưng end-to-end AI/MCP Iter 2. |
| 2 | Triển khai lớp/ghi danh, Assignment cá nhân/nhóm/doc/diagram, Quiz, rubric/workspace/submission/grading/feedback; AI integration kiểm riêng khi phụ thuộc đủ. |

Code và integration test cases Iter 1 theo hạn người dùng đã nêu: thứ Ba tuần sau của cuộc trao đổi gốc; test/fix đến Chủ nhật tuần sau. Không tự suy ra lại ngày tuyệt đối khi thiếu ngày họp; cần xác nhận milestone calendar trước cập nhật Report 2. Không sửa effort/deadline lịch sử.

Một feature: requirement owner, independent test-case owner, code owner khác nhau. Developer unit tests vẫn do coder làm; integration evidence/retest do reviewer/tester riêng. Không tự gán tên/điểm cho 5 người trong revision.

## Gates

G1 nền/quản trị; G2 references/rubric/groups/lifecycle/quota; G3 Assignment bốn tổ hợp và Quiz không Lesson, manual grade; G4 API ba provider/MCP/usage/feedback/notification. Manual Assignment không chờ AI. Waves và dependency chi tiết theo unit-of-work*.md.

## Compliance

Security/Resiliency subset trước giữ; extension PBT disabled. Đây là document-only authorized revision, không bắt đầu code stage, không advance historical approvals. Open decisions D01–D04 và compatibility actual agents chưa nghiệm thu.

## Bổ sung hội đồng — current scope

Backup/restore evidence U03, policy activity U09, ActivityEvent U11 (U14 tích hợp), teacher review U15, email quan trọng/failure U16; minh bạch AI U07/U13. Không thêm unit hoặc đổi nhân sự. Xem [đặc tả hội đồng](../requirements/council-features-2026-10-13.md). D05 đã được người dùng yêu cầu bổ sung, D01–D04 còn mở. Đây là phạm vi tài liệu, chưa runtime evidence.

- [x] Bổ sung FR-038/039, các luồng backup/activity (nay thuộc UC 58/37), US-OPS-001/US-INT-001; đồng bộ email, minh bạch AI và ownership.
- [x] Construction design/plans theo current scope.
- [ ] Code và kiểm thử restore/activity/email thực tế trước nghiệm thu.

Iteration 1 bổ sung nền backup/config/status U03; email OTP vẫn U01. Activity/policy/review thuộc Iteration 2. Email backup/thông báo lớp tích hợp khi U16 sẵn sàng, không tuyên bố G1 đã có cảnh báo email. Trước demo phải kiểm restore với dữ liệu các unit.

## UC consolidation — 65 active UC

UC 70 View Settings và UC 71 Edit Setting là hai UC riêng, primary U03; bao gồm UI cấu hình quota/API chung. U07 giữ quota service/data; U13 giữ connection/provider/credentials bảo vệ và test synthetic qua public port. UC 76/78/82 không còn độc lập. UC 58 gộp 80 (U16 dashboard, U03 backup); UC 37 gộp 81 (U15 review, U11/U14 activity). UC 42 soạn Individual, UC 45 soạn Group (U09); Draw.io tùy chọn trong UC 24/27, UC 26 retired. UC 79 retired; UC 29 chỉ feedback Assignment Practice đã nộp. 50 MVP + 1 extension story, 15 units; không tự phê duyệt D01–D04 hoặc runtime.

Construction current docs: [README](../../construction/README.md). Stage approvals/code progress không thay vì revision tài liệu.
