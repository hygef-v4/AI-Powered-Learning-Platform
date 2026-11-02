# Đối chiếu AI-DLC với code — 2026-10-30

## Baseline và giới hạn

- Branch `staging`, HEAD `794a405304bf02dd2593a9fa2c88662d7cd52845`; working tree sạch trước lượt rà soát.
- Rà soát tĩnh: inventory 295 file Markdown AI-DLC, cấu trúc backend/frontend, contracts, migration, CI/Compose; đọc các tài liệu tổng quan, quy tắc và code trọng điểm. Không phải kiểm chứng từng câu của toàn bộ 295 file.
- So với commit tài liệu MCP `92750b2a`, có 119 file backend, 383 file frontend, 1 file infra thay đổi; AI-DLC chỉ có 7 file thay đổi và không có thay đổi trong `docs/contracts`. Đây là chỉ dấu phạm vi, không phải số lỗi hoặc bằng chứng mọi thay đổi đều cần sửa doc.
- Code hiện tại là nguồn mô tả **AS-BUILT**. Yêu cầu đã duyệt và đề xuất chưa duyệt phải được giữ riêng; code trái yêu cầu được ghi thành delta, không tự biến thành quyết định nghiệp vụ được duyệt.
- Có file test không có nghĩa test đã pass. Lượt này không chạy build, unit/integration/E2E, không kiểm trạng thái DB/runtime, provider hoặc CI từ xa.

## Chênh lệch có bằng chứng

P0: dễ khiến triển khai tiếp sai quyền/ownership. P1: mô tả sai hành vi hoặc hợp đồng. P2: khó tra cứu hoặc đánh giá bằng chứng.

| ID | Mức | Tài liệu hiện tại | Code/bằng chứng hiện tại | Cách đồng bộ |
|---|---|---|---|---|
| D01 | P0 | `application-design/components.md` vẫn gán U13 cho AI, U15 cho Grading, U16 cho Notification; thiếu U10. `aidlc-state.md` còn Payment/Code Execution và U16. | `application-design/unit-of-work.md` đã có bảng U01–U15 mới; source dùng `ai`, `grading`, `quizzes`, `notifications`, `admindashboard`. | Dùng bảng unit mới làm gốc, xác minh với packages; sửa components/methods/services/dependencies/story map và bảng tiến độ. Không đổi ID UC để che lệch. |
| D02 | P1 | State mục Project/Workspace ghi chỉ có khung chưa nghiệp vụ; Construction README và unit-summary ghi chưa code, delta chưa triển khai; Stage Progress còn U03 wave 1. | Có controller/service/persistence, frontend và test cho cả 15 unit; nhiều ghi nhận implementation 17–26/10 trong cùng state. | Tách current snapshot khỏi lịch sử; mỗi unit ghi source có, evidence ngày/commit nào và gate còn mở. Không tick toàn bộ Build & Test. |
| D03 | P0 | U06 `BR-U06-30` cho Student tạo MCP credential, Admin không được; Inception FR-035 và personas/stories cần đối chiếu cùng. | Requirement delta `2026-10-26-mcp-staff-expansion.md` R01/R11 đã đổi sang Teacher/Manager/Admin. `ai/application/McpCredentialService.java` dùng MCP_SELF; staff expansion có report và role tests. | Hợp nhất role/tool matrix và credential UI vào tài liệu chính; ghi riêng quyền API cá nhân với quyền MCP. |
| D04 | P1 | U06 `BR-U06-02`, domain-entities: tối đa một institutional connection/provider. | `ai/V20261027_0900__allow_many_institutional_keys.sql` bỏ unique index; `AiConnectionService.java` có quản lý từng connection. Migration ghi chọn key ACTIVE tạo sớm nhất. | Cập nhật cardinality, chọn key, CRUD/test/delete và UI Settings; đối chiếu repository khi mô tả thuật toán chọn key. |
| D05 | P0 | U06 `BR-U06-31`: token chỉ hiện một lần; entity chỉ hash/hint. | `McpCredentialService.reveal` cho chủ credential còn hiệu lực xem lại, ghi MCP_TOKEN_REVEALED; migration `V20261027_1000__store_mcp_token_ciphertext.sql` thêm ciphertext/IV/key version. Token cũ hoặc thiếu encryption key có thể không reveal được. | Mô tả đúng hash dùng xác thực và bản mã phục vụ reveal; cập nhật API/UI/audit/test/security, không ghi token thật vào doc. |
| D06 | P1 | U05 `BR-U05-12/14`: lớp OPEN bắt buộc có giảng viên. | `academics/domain/CourseClass.changeStatus` đã bỏ điều kiện; `AcademicsService.requireTeacherStillEligible(null)` trả ngay; migration `classes/V20261027_1100__allow_open_class_without_teacher.sql` bỏ DB constraint. | Sửa lifecycle, quyền khi chưa có teacher, form và test case. Không suy ra create luôn OPEN: `CourseClass.create` hiện vẫn tạo DRAFT. |
| D07 | P1 | U08 `BR-U08-54`: Settings activity.retentionDays 1–365, chưa đặt thì không bật activity; yêu cầu chính còn mô tả module/Lesson là mở rộng. | `ActivityService.RETENTION = Duration.ofDays(1)`; scanner dùng hằng này. `ActivitySettingDefinitions.java` đã xóa trong diff. | Cập nhật retention 1 ngày và kiểm các rule bật activity ở U07/U10; đối chiếu Settings/FR-039 và BF-04/06/07. Đây là thay đổi retention, tách khỏi quyết định trừ điểm. |
| D08 | P1 | U12 `BR-U12-10/11`: yêu cầu đổi leader cần lý do 10–1.000 ký tự; từ chối bắt buộc lý do. | `groups/application/LeaderRequestService.java`: lý do gửi yêu cầu null thành chuỗi rỗng, từ chối trống thành null, tối đa 1.000; migration `groups/V20261029_1100__make_leader_request_reason_optional.sql` bỏ check cũ. | Sửa rule/entity/validation/frontend/acceptance và BF-12, giữ trường hợp quá 1.000 ký tự. |
| D09 | P1 | U13 design chưa mô tả đầy đủ module; checklist góp ý 29/10 mục module còn unchecked và gán U05. `materials.yaml` chưa có module. | `MaterialModuleService`, `MaterialModuleController`, `MaterialModule`, `MaterialModuleRepositoryAdapter`; migration `materials/V20261029_1000__create_material_modules.sql`; frontend `ModuleBoard.test.tsx`, backend `MaterialModuleServiceTest.java`. | Ownership U13, U05 tích hợp trang lớp; tài liệu bắt buộc module_id, backfill Module 1, thứ tự, xóa/đổi tên, gắn Quiz tối đa một module; cập nhật API và BF-02/06. Tách module cơ bản đã có khỏi Lesson/RAG mở rộng. |
| D10 | P1 | U10 `BR-U10-20/23`: chỉ sửa DRAFT, ngưng chỉ từ OPEN và bắt buộc lý do, chưa nêu reopen. | `quizzes/domain/Quiz.edited` giữ OPEN khi sửa quiz OPEN; `retired` nhận mọi trạng thái chưa RETIRED; `reopened` trả DRAFT/OPEN tùy mốc publishedAt/retiredAt. `QuizPublicationService` mặc định lý do “Lưu trữ”, có reopen. | Ghi state machine AS-BUILT, điều kiện service, callback U11 và tác động lượt làm/snapshot. Không ghi REVIEWED đã bỏ chỉ dựa góp ý 29/10. |
| D11 | P1 | U15 thiết kế tập trung counts/backup; OpenAPI admin-dashboard chỉ có root và backups. | `AdminDashboardController` có GET `/activity`; `AdminDashboardActivityService` tổng hợp biến động tài khoản hôm nay, assignments 7 ngày, sự kiện đáng ngờ 30 ngày và partial availability. Có service test. | Bổ sung DTO/API/UI, phụ thuộc IAuditStats/IAssignmentQuery, cửa sổ Asia/Ho_Chi_Minh, thiếu nguồn và chính sách refresh. |
| D12 | P1 | `build-and-test-summary.md` và build instructions vẫn mang scope U14 ngày 21/10, ghi thiếu schema U07/U08, provider stub, Docker không chạy như trạng thái hiện tại. | Source có U07/U08 migrations; có e2e scripts U02/U06/U12 và reports 24–26/10, CI chạy Maven verify + required DB test guard. | Chuyển kết luận môi trường cũ thành lịch sử theo baseline; tạo bảng evidence hiện hành có scope/commit/thời điểm/skip. Docker/runtime hiện tại chưa được kiểm tra nên không kết luận healthy hoặc blocked. |
| D13 | P2 | `inception/README.md` ghi 65 UC, `unit-of-work.md` và Construction ghi 66; components vẫn nói D01–D04 còn mở trong khi requirements đã ghi chốt 15/10. | Unit-of-work map UC01–66; requirements có quyết định D01–D04; nhiều lớp tài liệu dùng baseline khác nhau. | Đếm đối chiếu catalog + bảng report, giữ ID, ghi active/retired/extension; đồng bộ decision register và revision header. |
| D14 | P2 | State trỏ `u04.../code/frontend-handoff.md` và `implementation-decisions.md` nhưng hai file không có trong checkout; Inception README còn trỏ lịch sử `docs/history/use-cases-73.md` không có trong inventory hiện tại. | Kiểm tra trực tiếp các đường dẫn không tồn tại; U04 code backend/frontend vẫn có. | Kiểm toàn bộ link và raw path; khôi phục nội dung cần thiết từ Git hoặc thay bằng đường dẫn/commit lịch sử xác minh được. Không tạo file rỗng chỉ để link xanh. |

## Phạm vi 15 unit cần đưa vào vòng đồng bộ

Bảng này là inventory nguồn và ưu tiên kiểm tiếp, không phải chứng nhận mọi UC đã hoàn thành.

| Unit | Package backend chính | Frontend/nguồn cần đối chiếu | Phần tài liệu ưu tiên |
|---|---|---|---|
| U01 | identity | auth, profile, admin/accounts | Quyền thực tế, phiên/OTP, evidence thay vì kết luận datastore chưa chạy từ cũ |
| U02 | audit | admin/audit | Query/filter/stats, IAuditQuery/IAuditStats và U15 consumers |
| U03 | files, jobs, settings, backup | Settings, file preview/download, backup | SettingDefinition hiện tại, loại/kích thước file, worker/backup/restore và CI |
| U04 | subjects | admin/subjects | Subject lifecycle, teacher/manager ownership; bổ sung handoff còn thiếu |
| U05 | academics | manager/classes, teaching/classes, classes | D06, trang lớp/roster/statistics, quyền và tích hợp module |
| U06 | ai | ai, admin/mcp, Settings | D03–D05; source/quota/task/tool matrices, reports 26/10 |
| U07 | assignments | Assignment editor và AI authoring | API/DTO/copy/reference/sample/rubric thực tế và tích hợp U06 |
| U08 | assignmentwork | my-assignments, attempts | D07; autosave, nộp/late/activity/feedback |
| U09 | grading | teaching/evaluations, submissions | Structured feedback, AI delivery/review, partial scores, gradebook; hợp nhất reports 25–26/10 |
| U10 | quizzes | quizzes, manager/questions | D10; bank, publication, module và AI picker |
| U11 | quizpractice | quiz-practice | Attempt/snapshot/results/activity, tác động U10, không suy diễn sinh đề theo tag đã có |
| U12 | groups, groupdocs | classes/[id]/group, group-docs | D08; rename group, claims/locks, submission, lỗi nhóm bị xóa |
| U13 | materials | Materials/ModuleBoard, class/subject integration | D09, quyền xem/tải, module schema và contract |
| U14 | announcements, notifications | announcements, preferences | Events/recipient/email/SSE, reports remediation 23–26/10 |
| U15 | admindashboard | admin | D11; counts/activity/backup, partial failures |

## Những điều chưa được phép kết luận từ rà soát này

- Không có bằng chứng mới để chốt toàn bộ NFR, security effectiveness, provider thật hoặc số test pass tại HEAD.
- Góp ý 29/10 về sinh Quiz theo tag, tự trừ điểm out-tab, bỏ bước review, peer assessment, nhập Word/Draw.io vào ô, song ngữ: phải kiểm từng item với code rồi ghi implemented/partial/proposed/conflicting. Không tick cả danh sách theo commit “feat(ui)”.
- D03 về bỏ AI Student cho bài nhóm và FR-039 về không tự trừ điểm không tự bị thay bởi checklist góp ý.
- `main-business-flows` đang ghi TO-BE; nếu cập nhật theo code cần gắn nhãn AS-BUILT hoặc ghi rõ delta, giữ phân biệt với AS-IS khảo sát thực tế.

## Kết luận

Lệch lớn nhất là ownership/quyền, lifecycle/schema mới và snapshot tiến độ; không chỉ thiếu mô tả UI. Tài liệu đã có nhiều ghi nhận đúng trong delta reports nhưng chưa được hợp nhất vào các file người đọc được hướng dẫn xem đầu tiên. Kế hoạch thực hiện: [đồng bộ tài liệu](2026-10-30-aidlc-doc-sync-plan.md).
