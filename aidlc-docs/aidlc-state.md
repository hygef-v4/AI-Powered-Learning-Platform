# AI-DLC State Tracking

## Project Information
- **Project Type**: Greenfield
- **Start Date**: 2026-09-12T15:04:50Z
- **Current Phase**: CONSTRUCTION
- **Current Stage**: Code Generation Part 2; tài liệu revision 2026-10-09 theo 73 UC/51 story/15 unit. Approvals 16 plan ngày 2026-10-05 thuộc baseline cũ; skeleton/contracts/ports và U03 generated code vẫn cần triển khai/kiểm review revision, không được coi đã đổi bởi sửa tài liệu.
- **Resume action**: Dùng current-srs-contract.md, unit-of-work-story-map.md và checklist revision từng unit theo 73 UC. Settings U03, bài của môn/copy/version U08; U10 đã bỏ. Rà code/contracts/migration theo thiết kế hiện hành; U03 generated steps/integration tests chưa được đánh dấu hoàn tất. Sau U03/U02, tiếp tục U01/U04 theo dependency. Integration test do tester riêng.

## Latest documentation decision
- **2026-10-09 (sau recheck)**: Đóng C01–C05 ở cấp tài liệu: U07/U05/U13 một HOLD và settle lượng dùng thật/trả dư; FR-024/story dùng đúng UC/màn và Chủ nhiệm môn bật phân bố điểm; AiUsagePort có findHold/UsageStart REPLAY/CallSnapshot/checkpoint/scanClaimId; shared infrastructure 15 unit. Các việc code/contracts/migration/integration theo checklist vẫn chưa triển khai; stage progress và approvals giữ nguyên.
- **2026-10-09**: Đồng bộ local 73 UC, 51 story và 15 unit; US-CAT-005 bỏ, US-SET-001 thêm U03; U10 bỏ, US-ASM-008/009/010 về U08. Sửa quyền Admin, mapping/kiến trúc/màn/nguồn, state/wave/gate và U05 retry/credit/summary. Google SRS/hình người dùng/code không sửa trong revision này; giữ approvals và code progress lịch sử.
- **2026-10-08**: Added UC 70 View Audit Log and its Administrator-only permission row to the Google SRS; verified the saved document. Synchronized `docs/use-cases-73.md` and active Inception requirements, personas, stories, application design and planning summaries with 70 UC, 51 stories and 16 units. UC 70 belongs to U02 and uses the existing Audit Log screen. Screen names now match the latest `screen-flow (1).drawio`: Teacher Assigned Classes, Subject Classes, Subject Template and Template Editor (UC 53-54). Student My Classes is unchanged. Construction designs and code plans were synchronized in the follow-up revision; existing contracts/application code and older local specification catalogs are not automatically updated. The user's diagram was not edited; implementation checkpoints and historical audit records are preserved.
- **2026-10-04**: Email provider changed to Brevo SMTP for demo/production; Mailpit retained for local/tests. U01/U16 email design, shared infrastructure, Inception provider selection and code-generation references synchronized. Existing code-generation approval checkpoints remain unchanged.

## Workspace State
- **Existing Code**: Yes (skeleton, contracts, port stubs)
- **Programming Languages**: Java 17 / Spring Boot 3.5, TypeScript / Next.js 16
- **Build System**: Maven, npm
- **Reverse Engineering Needed**: No
- **Workspace Root**: `F:/code/git/AI-Powered-Learning-Platform`

## Code Location Rules
- **Application Code**: Workspace root (NEVER in `aidlc-docs/`)
- **Documentation**: `aidlc-docs/` only
- **Structure Patterns**: See `construction/code-generation.md` in the AI-DLC rule details

## Extension Configuration
| Extension | Enabled | Decided At |
|---|---|---|
| Security Baseline | Yes | Requirements Analysis |
| Resiliency Baseline | Yes | Requirements Analysis |
| Property-Based Testing | No | Requirements Analysis |

**Phạm vi rule rút gọn (2026-09-24)**: Security chỉ áp dụng SECURITY-03, 04, 05, 08, 09, 12, 15; Resiliency chỉ áp dụng RESILIENCY-04, 06, 10. Các rule còn lại là N/A "ngoài phạm vi đồ án" ở mọi stage, không phải blocking finding. Xem `requirements.md` mục 12-13.

## Stage Progress

### INCEPTION
- [x] Workspace Detection
- [x] Reverse Engineering - skipped (greenfield)
- [x] Requirements Analysis
- [x] User Stories
- [x] Workflow Planning
- [x] Application Design (revision tài liệu 73 UC ngày 2026-10-09; global ERD removed - tables are defined per unit)
- [x] Units Generation (15 unit hiện hành; U10 đã bỏ)

### CONSTRUCTION
- [x] Functional Design - 15 unit hiện hành; approvals baseline cũ giữ lịch sử
- [x] NFR Requirements - 15 unit hiện hành; approvals baseline cũ giữ lịch sử
- [x] NFR Design - 15 unit hiện hành; approvals baseline cũ giữ lịch sử
- [x] Infrastructure Design - 15 unit hiện hành; approvals baseline cũ giữ lịch sử (+ `construction/shared-infrastructure.md`)
- [x] Code Generation Part 1 (plans) - 16 plans approved 2026-10-05 (historical; 15 unit hiện hành) for the previous baseline; scope revision documented 2026-10-09
- [ ] Code Generation Part 2 (code) - in progress: U03 (wave 1)
- [ ] Build and Test
- [ ] Operations (placeholder)

## Unit Progress

Tài liệu hiện hành theo 73 UC/51 story/15 unit ngày 2026-10-09. Approvals 05/10 giữ baseline cũ, không phê duyệt code đã sinh theo scope mới. U03 steps [x] và summaries giữ implementation cũ; revision tasks [ ] tới khi thực sự làm.

| Unit | Design stages | Code plan | Code |
|---|---|---|---|
| U01 Account & Access | Design revision 73 UC ngày 2026-10-09; Role/authentication cho mọi unit; counts cho Admin Dashboard. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U02 Audit | Design revision 73 UC ngày 2026-10-09; Audit append-only trong transaction mọi command nhạy cảm. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U03 File, Job, Event & Settings | Design revision 73 UC ngày 2026-10-09; Tệp/worker/events dùng chung; lưu Settings và kiểm version/audit; U07/U13 khai báo các mục của mình. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | Generated 2026-10-05, awaiting review (branch `feat/files-and-jobs`) |
| U04 Subject, Class, Enrollment & Learning Access | Design revision 73 UC ngày 2026-10-09; R2–R5 scope cho tài nguyên môn/lớp; Admin chỉ quản lý môn, danh sách lớp của môn chỉ đọc. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U05 Content, Material & RAG | Design revision 73 UC ngày 2026-10-09; Tóm tắt/embedding và RAG cho AI; quiz/attempt do U08/U09/U11 giữ. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U06 Rubric & Subject Question Bank | Design revision 73 UC ngày 2026-10-09; Rubric thuộc bài, tự tạo và khóa khi phát hành; câu ngân hàng SUBJECT và câu riêng ASSIGNMENT. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U07 Payment & AI Credit | Design revision 73 UC ngày 2026-10-09; Ví của ba role; snapshot payment, verified webhook và grant định kỳ qua Settings. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U08 Assessment Core, Publication & Copy | Design revision 73 UC ngày 2026-10-09; Vòng đời chung UC 35/42–45, bài của môn, copy/version; U09 cấu hình theo dạng. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U09 Question Type Authoring | Design revision 73 UC ngày 2026-10-09; Cấu hình quiz và bốn dạng assignment, document model; U08 vòng đời, U06 rubric. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U11 Attempt & Submission | Design revision 73 UC ngày 2026-10-09; Quiz Practice tách Student Assignments; lần làm bài của môn ghi classId. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U12 Group & Allocation | Design revision 73 UC ngày 2026-10-09; Chia nhóm/leader hỗ trợ Teacher Class Detail và UC 45/27. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U13 AI & Code Execution | Design revision 73 UC ngày 2026-10-09; AI draft, Practice grading UC 29, proposals UC 38, Judge0 UC 25/43, số liệu AI UC 58. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U14 Group Document & Submission | Design revision 73 UC ngày 2026-10-09; Tài liệu nhóm chỉ ở lớp, GRADED; chỉ leader nộp. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U15 Grading | Design revision 73 UC ngày 2026-10-09; Bài của môn chấm/công bố theo từng lớp; Practice/quiz không vào gradebook. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U16 Reporting & Notification | Design revision 73 UC ngày 2026-10-09; Progress/distribution và export của UC 40; thông báo bài của môn tới mọi lớp OPEN. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |

## Open Items

- Thay đổi ngày 2026-09-29 đã được người dùng xác nhận và đồng bộ vào Inception/Construction: `STUDENT`/`TEACHER`; năm dạng bài (Code Lab, Text Essay, Diagram Essay, Group Assignment, Multiple-Choice Quiz), không có Simulation Exam; `GRADED` và `PRACTICE` theo ràng buộc từng dạng. Student có thể mua credit, dùng AI tóm tắt học liệu được xem (revision 2026-10-09) và chấm attempt Practice Text/Diagram Essay của mình: nộp không tự chấm, Student bấm "Chấm với AI" và cần đủ credit lúc bấm; thiếu credit thì mua thêm rồi bấm lại (cập nhật 2026-10-04). Teacher chỉ chấm bài Graded.
- Nhóm chỉ triển khai một MVP: 51 story và 73 use case hiện hành theo SRS và `docs/use-cases-73.md`. Hai story mới là `US-PAY-004` (Admin quản lý gói credit) và `US-PAY-005` (Admin xem lịch sử thanh toán toàn hệ thống). US-CNT-004, US-RPT-002 và US-RPT-003 vẫn thuộc MVP. Danh mục đã bỏ `US-PAY-003`/`UC-PAY-02`; đối soát PayOS chỉ còn job tự động trong `US-PAY-002`. Không có kế hoạch triển khai Phase 2. Các mốc UC cũ trong lịch sử không được đánh số lại.
- Critical path (by H edges, 2026-10-04): U02 hoặc U03 → U01 → U04 → U05 (hoặc U06) → U08 → U09 → U11 (hoặc U14) → U15 → U16. Waves: W1 U03 ∥ U02 → U01 → U04; W2 U05–U08, U12; W3 U09 (năm loại bài, UC 35/42–45 hiện hành), U11, U14, U15; W4 U13, U16. U15 → U13 và U09 → U13 là `C`; U03 → U01, U02 là `C`.
- VPS sizing suggestion: 4 vCPU / 8 GB RAM / 60 GB SSD (Judge0 included).


## History (summary)
- 2026-10-09: Revision theo yêu cầu mới: U03 upload không tóm tắt/AI/credit; U05 trích chữ tới EXTRACTED. Student/Teacher/Subject Manager yêu cầu từ View Material theo quyền xem, requester chịu một HOLD summary+embedding; summary dùng chung. AI deadline/hold 24/25 giờ từ yêu cầu. Inception, U03/U05/U07/U13 và pending code plans được đồng bộ, implementation chưa làm.
- 2026-10-08: Đồng bộ Construction của 16 unit theo 70 UC/51 story, quyền R1–R5/Full, màn mới, package/history Admin, announcement lifecycle và bỏ avatar; giữ lịch sử approvals/bước code đã hoàn thành. Code/contracts chưa triển khai revision.
- 2026-10-08: Đồng bộ Inception theo SRS mới: 70 UC, 51 story, 16 unit; thêm UC 70 View Audit Log vào Google SRS/bảng quyền và danh mục màn hình. Quyền kế thừa theo phạm vi tài nguyên; thêm quản lý gói credit và lịch sử thanh toán của Admin. Construction và code plans vẫn ở baseline trước, cần đồng bộ riêng.
- 2026-10-04: CHAPTER đổi thành MODULE của môn (Chủ nhiệm môn tạo trên Subject Detail, mọi lớp kể cả lớp tạo sau dùng chung; mỗi module có nút tải tệp/gắn link; `lessons.class_id` phân biệt học liệu của môn và của lớp). UC 14 đổi thành Comment on Announcement: chỉ giảng viên đăng thông báo, sinh viên và giảng viên bình luận dưới thông báo (2 bình luận mới nhất, popup xem thêm, không thông báo); bỏ `class_discussions`, `discussion_posts`, thêm bảng nối `announcement_comments`. ERD 19 thực thể/31 quan hệ, database 24 bảng/36 khóa ngoại.
- 2026-10-04: Việc nền, worker, RabbitMQ và sự kiện thông báo chuyển từ U02 sang U03 (U02 chỉ còn audit, U03 thành File, Job & Event); khung dự án chuyển từ plan U01 sang plan U03; thứ tự wave 1: U03 ∥ U02 → U01 → U04. Sửa thiết kế U01–U03, plan, port của các unit dùng việc nền, ma trận và hình phụ thuộc, uc-wave-map.
- 2026-10-04: Wave mới theo dependency graph của người dùng; U09 chủ trì cả năm loại bài (UC 26 từ U13, UC 27 từ U14); U15 và U09 dùng U13 qua `C`. Đồng bộ unit-of-work, dependency (ma trận, wave, gate), story map, thiết kế và plan U09/U13/U14, uc-wave-map.
- 2026-10-04: UC 33 chấm hàng loạt bằng AI, workspace có ‹ › và tự sang bài kế; UC 34 điểm khác AI không cần lý do; UC 16 người đang xem cũng thấy vòng chờ khi tự nộp; UC 17 tài liệu chung chấm như DOCUMENT, điểm đóng góp riêng từng thành viên mặc định bằng điểm tài liệu chung, lý do tùy chọn, nộp lại như lượt mới; UC 35 sinh viên xem điểm trên Assignment List, Gradebook gom theo sinh viên; UC 18 thống kê hiện ngay trên Admin Menu.
- 2026-10-04: UC 16 bỏ trạng thái REVIEW và thông báo đọc lại; người giữ mục sửa trong popup che kín trang; sinh viên không sửa/xóa mục của giảng viên; trưởng nhóm nộp bất kỳ lúc nào trước hạn; hết hạn tự nộp gồm bản nháp của mục đang giữ, người đang sửa được chuyển sang Submitted Assignment; bỏ gợi ý nhả mục giữ lâu. UC 27 viết lại theo cách của UC 28. UC 40 chấm AI khi bấm nút, quá 5 phút báo lỗi; UC 29 hiện bài ngưng giao đã có điểm; UC 31 lượt gần nhất và nút chuyển lượt.
- 2026-10-03 (lần 3): Rà soát đồng bộ docs ↔ aidlc, inception ↔ construction, giữa các unit; sửa 16 chỗ lệch (thanh toán bốn vai trò, `payment.paid` do U07 phát, component-methods, học liệu tải lên rồi quét trong FR/story/UC/business flow, UC 27 có Subject Manager qua template, bỏ rút template riêng, event `group.leader-requested`, port thiếu, truy vết UC 18). Thêm `construction/uc-wave-map.md` (40 UC theo wave và thứ tự code). Sau đó sửa mọi mục còn mở theo screen flow: UC 40 thành Grade with AI (Student, Teacher; popup U11 dùng cả ở Grading Workspace), Class Detail của Teacher là màn U04 nhúng panel U12, UC 35 gồm Gradebook của giảng viên, MVP không hoàn tiền, dời ô chồng ở business flow trang 1.
- 2026-10-03 (lần 2): Theo review ERD: ERD conceptual `erd_new2` (20 thực thể, CHAPTER thay MODULE, nhóm thuộc lớp, GROUP_DOCUMENT, EVALUATION độc lập, RUBRIC, CLASS_DISCUSSION, ANNOUNCEMENT); database 25 bảng (chỉ quan hệ nhiều-nhiều có bảng nối: enrollments, group_members, assignment_questions, payments, discussion_posts) trong `docs/database.drawio`/`.md`/`.png`. Construction U01-U16 viết lại theo đúng bảng này: bỏ bảng job/outbox/artifact/ledger/publication/grade_history/email_outbox; học liệu = tải lên rồi quét; màn hình khớp screen flow (không sửa `screen-flow.drawio`). Requirements/stories/UC 11, 12 đồng bộ.
- 2026-10-03: UC 18 thành View Statistics (Administrator: tài khoản theo role/trạng thái, môn, lớp, ghi danh; popup trên Admin Menu); bỏ dashboard Student, Student vào Student Menu; phân bố điểm ẩn danh hiện trên Assignment List (US-RPT-001 S3); US-RPT-002 viết lại. Chủ nhiệm môn quản lý lớp của môn như ADMIN (FR-003, US-CAT-001 S3, BR-U04-10/12). Gói credit và mức tặng cố định (UC 22, FR-021, US-AIG-003 S4, U07). Bỏ Administrator khỏi UC 36. Sửa ma trận phụ thuộc: bỏ U12→U08, U11→U10, U15→U10; U12 lên wave 1; vẽ lại unit-of-work-dependency.drawio/png. Đồng bộ frontend theo screen flow drawio (U01 popup, U13 AI Usage + AI Setting, U15 điểm trong Class Detail, U16 popup, U04 Subject Detail), port U06/U08. ERD: thêm `forming` vào erd_new2, thêm trang erd-database 19 bảng, viết lại erd.md (tên bảng còn khác Construction).
- 2026-10-01 (lần 7): Tách bài nhóm khỏi Manage Assignments thành UC 27 Manage Group Assignment (Teacher: tạo, sửa, xem trước bài nhóm có rubric, mục chính, nhả khóa mục; phát hành khi nhóm hợp lệ); Manage Assignments thành UC 28, UC 28–39 cũ thành 29–40 (bảng 40 UC). Merged IDs: 27 = 25, 40; 28 = 32, 35, 41, 48–50. Legacy: UC-ASM-06, UC-GRP-05 → 27. Đánh số lại docs, Inception, Construction; tham chiếu bài nhóm giữ UC 27, phần còn lại sang UC 28. U14 chủ trì UC 16, 27; ma trận story: UC 27 = US-QBK-002, US-GRP-003. Lịch sử và audit giữ số cũ.
- 2026-10-01 (lần 6): Bảng use case còn 39 UC: Create Assignment with AI gộp vào UC 27 Manage Assignments, Create Template with AI thành UC 21 Manage Templates (tạo thủ công hoặc bằng AI, sửa, xoá); UC 23–40 cũ thành 22–39. Đánh số lại toàn bộ docs, Inception và Construction (trừ audit và mục lịch sử). U10 chủ trì UC 21 (BR-U10-06, 07: bản nháp AI, xoá template), U13 chủ trì UC 22, 26 và cung cấp luồng AI cho UC 21, 27. FR-006/FR-027, US-ASM-009 (Scenario 3), đặc tả UC 27 và screen flow cập nhật theo.
- 2026-10-01 (lần 5): BR-U01-48 đổi đích Teacher/Subject Manager thành `/teaching/classes` (route `ClassListPage` của U04). `screen-flow.md` gán mã mời và phân bố điểm cho `InviteCodeTab`/`GradeDistributionToggle` trên Class Detail của giảng viên theo U04 (người dùng chọn). Plan U09 Bước 6 thêm lấy khung từ ngân hàng; plan U09 cần duyệt lại.
- 2026-10-01 (lần 4): Dashboard sinh viên (UC 18) gồm lớp đã ghi danh, bài sắp hạn, thông báo chưa đọc, trạng thái nộp và điểm đã công bố ở BR-U16-40, `StudentDashboard`, F6, frontend và plan U16, US-RPT-002. Thêm BR-U01-48: đăng nhập xong Student → `/learn/dashboard`, Teacher/Subject Manager → `/teaching/classes`, Admin → `/admin/accounts`. Plan U01 cần duyệt lại.
- 2026-10-01 (lần 3): Thêm lại SEC-006 vào US-RPT-003 (lần sửa trước xóa nhầm); bỏ chữ "câu hỏi quiz" còn sót ở requirements, context diagram và US-AIG-002; persona Chủ nhiệm môn có xem/xuất bảng điểm (US-RPT-001, US-RPT-003). Theo quyết định của người dùng, giữ trang Dashboard riêng của sinh viên (UC 18, U16) làm trang đích: thêm ô Dashboard vào `screen-flow.drawio` và bảng `screen-flow.md` (93 ô).
- 2026-10-01 (lần 2): Sửa theo báo cáo rà soát thứ hai. Sửa tiêu đề ERD 46 bảng, đầu các plan còn ghi 69 UC, thuật ngữ "sinh viên" trong docs, bỏ theo dõi `debug.log`, viết README gốc. Thêm event `group.document-review` (loại `GROUP_DOCUMENT_REVIEW`) và `group.leader-request-rejected` (loại `GROUP_LEADER_REQUEST_REJECTED`) vào U12/U14/U16. Theo quyết định của người dùng: Chủ nhiệm môn được xuất bảng điểm (FR-024, US-RPT-003); ngân hàng câu hỏi chứa câu hỏi của cả năm dạng bài, dùng trong UC 24–28 (bảng UC, FR-016, US-QBK-002, ma trận story ↔ UC, story map, U06/U08/U09). FR-010/FR-021 thêm gói credit và mức tặng. Bảng màn hình trong `screen-flow.md` dựng lại đúng 92 ô của `screen-flow.drawio`. Plan U06 cần duyệt lại.
- 2026-10-01: Đồng bộ toàn bộ docs với bảng 40 use case: bỏ ghi chú bản nháp, thêm bảng Legacy UC codes, thay mã `UC-XXX-NN` trong thiết kế unit và code plan, viết lại story map/unit-of-work theo UC 1–40, sửa context diagram (Student mua credit), ERD 46 bảng (bỏ cột Simulation, thêm `grading_mode` và cột Practice, `group_documents`, nhóm theo lớp), nhãn Student/Teacher trong screen flow. Theo quyết định của người dùng: gói credit và mức tặng thuộc UC 23; phân bố điểm ẩn danh do giảng viên bật thuộc UC 36 (UC 18 giữ là UC của sinh viên); nhóm chia trong danh sách sinh viên của lớp, có chia ngẫu nhiên, bỏ dùng lại nhóm; giảng viên soạn mục chính, trưởng nhóm thêm/sửa/giao mục chi tiết; tài liệu nhóm vào `REVIEW` khi mọi mục xong rồi trưởng nhóm mới nộp; UC 25 ghi rõ tìm, nhập hàng loạt và dùng lại câu hỏi. Plan U12/U14 cần duyệt lại.
- 2026-09-30: Bảng use case gộp còn 40 UC; đồng bộ đặc tả, README, business flow, screen flow, stories (ma trận story ↔ UC) và requirements.
- 2026-09-29: Bảng use case gộp CRUD cùng actor: profile 07→06, account 09/11/12→08, subject 14/15→13, roster 23/24→22; đổi tên tất cả UC còn hiệu lực thành tối đa bốn từ. Còn 69 UC, 49 story; ID lịch sử không tái sử dụng.
- 2026-09-29: Người dùng xác nhận bản requirements đã làm rõ, yêu cầu sửa tài liệu Inception/Construction. Đồng bộ stories, personas, use-case catalog (49 story/77 UC; mã Simulation Exam retired, mã AI Practice mới), application design, per-unit functional/NFR/infrastructure designs và code plans. Không sinh mã ứng dụng.
- 2026-09-12..22: Inception completed and revised through several change requests (roles, Draw.io, group work, templates/simulation).
- 2026-09-24: Application Design corrected; unit split reworked from 17 to the current 16 units with four non-blocking waves; security/resiliency scope reduced; Construction started with U01.
- 2026-09-24..25: Per-unit design stages and code plans for U01-U16; decisions synced back to Inception (payment = AI credits, DRAWIO → DOCUMENT, no subject-wide assignments, versioning after retire, group work as a shared document, auto-grading on submit, automatic deadline reminders only).
- 2026-09-25: Inception clean-up: requirements/stories/personas/use-cases swept, application design files rewritten for 16 units, global ERD removed, dependency figure added.
- 2026-09-25: U03 simplified to avatar, material and document images (Draw.io XML lives inside documents, U09); dependency matrix and ports re-synced.
- 2026-09-25: Removed live references to the deleted global ERD; aligned the active story count and UC-ASM-01 trace. Clarified AI credit billing for U05 embedding and U13 generation, with U07 as credit owner and a separate system-busy response when AI quota is exhausted.
- 2026-09-25: Chốt thi thử mặc định 3 lượt, giảng viên chỉnh 1-10; người học được nhập DOCX vào lượt DOCUMENT đang làm sau khi xem trước; chưa tính điểm tổng theo hệ số. Mở lại quyết định hoàn tiền U07 để nghiên cứu PayOS.
- 2026-09-25: Nhóm xác nhận không thực hiện Phase 2; giới hạn dự án ở MVP (47 story, 74 use case). Các mục từng ghi Phase 2 chuyển thành ngoài phạm vi và giữ mã lịch sử để truy vết.
- 2026-09-25: Nhóm chọn lại ba tính năng số 2, 7, 8 của danh sách cũ cho MVP: thông báo/hỏi đáp lớp (US-CNT-004), dashboard cá nhân (US-RPT-002), xuất bảng điểm (US-RPT-003). Phạm vi lúc đó là 50 story/78 use case; sáu story còn lại ngoài phạm vi. Quyết định này thay thế dòng phạm vi 47/74 ở trên.
- 2026-09-25: Xóa 12 UC ngoài phạm vi khỏi danh mục use case theo yêu cầu; catalog UC lúc đó có 78 mục MVP, mã đã xóa không được tái sử dụng.
- 2026-09-25: Xóa 9 story ngoài phạm vi khỏi danh mục user story theo yêu cầu; catalog story lúc đó có 50 mục MVP, mã đã xóa không được tái sử dụng.
- 2026-09-26: Bỏ `UC-PAY-02` và `US-PAY-003` cùng thao tác admin đối soát/điều chỉnh credit thủ công; job tự đối soát được giữ trong `US-PAY-002`. Phạm vi tại thời điểm đó: 49 story/77 use case.
- 2026-09-26: Data model consolidated from 62 to 45 PostgreSQL tables (keep only tables that must stand alone, are listed by a use case, or tie to an external system; 1-1 data becomes columns). Five shared tables with owner-unit migrations and extension ports. Domain entities rewritten for all units; FD/NFR/Infra/plans/ERD synced. Account status `PENDING_ACTIVATION` renamed `PENDING`.
- 2026-09-26: Messaging redesign: audit INSERT in the business transaction (no audit queue); required cross-unit reactions (grading on submit, auto-submit on retire, group docs on open/new group, lock release on member removal, Code Lab score) via ports that enqueue U02 jobs; events only for U16 notifications. RabbitMQ: exchanges `jobs`, `platform.events`, `platform.realtime`; eight U02 job queues (`jobs.scheduled`, `jobs.triggered`, `jobs.email`, `jobs.gemini`, `jobs.youtube`, `jobs.code`, `jobs.drive`, `jobs.payos`), U16's `jobs.notification`, and temporary `jobs.realtime.{instanceId}` queues. Redis: 12 key groups named by purpose; Gemini daily cost cap shared by U05/U13 via `AiBudgetPort`. The affected code plans await re-approval.
- 2026-09-27: Screen flow (`docs/screen-flow.drawio`) chốt theo bản nhóm vẽ; frontend gộp theo đó: nhập CSV thành `ImportAccountsPanel` trên danh sách tài khoản (U01), cài đặt email thành toggle trên trang thông báo (U16), tổng quan tài liệu nhóm thành panel trong danh sách bài nộp (U14), chấm nhóm thành chế độ `groups` của `GradingWorkspacePage` (U15). Plan U01/U14/U15/U16 chỉ đổi tên component tương ứng.
- 2026-09-27: Updated UC56, UC57 and UC75 in `docs/use-case-specifications.md` to match the current design; this documentation change did not advance the Construction stage.
- Full chronological log: `audit.md`.
