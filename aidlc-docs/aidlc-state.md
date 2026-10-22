# AI-DLC State Tracking

## U04 Conventional Commits publication — 2026-10-18

- Người dùng yêu cầu phân loại/tạo commit Conventional Commits và push GitHub, thay hạn chế chưa commit trước đó. Branch hiện có subject-administration; fetch origin xác nhận không diverge, không đổi main/rewrite history.
- Commit chia theo backend feature+tests, shared Dialog fix+tests, frontend feature+tests và docs handoff. Mỗi message có What/Why/Validation, Refs KAN-24; không dùng message chung chung hoặc gộp file khác unit.
- Trước commit: Maven verify278 PASS gồm31 PostgreSQL fixture cases,0 lỗi/skip, jar build PASS; container loopback tạm có random process-env password được xác minh tên/label và dọn. Frontend evidence cuối124 PASS, typecheck/lint/build, source không đổi khi chuẩn bị commit. Real provider/JWT/CSRF/NFR/tester gates vẫn pending.
- Người dùng đã chuyển manual TEMP ra ngoài repo; đã đối chiếu bản đó và không sửa/đưa lại repo. Hai TEMP staged được loại khỏi index sau sao lưu ignored, quyết định cần giữ ở implementation-decisions.md; links tài liệu đổi sang relative và được kiểm. Không publish fixture/log/build output/local screenshot hoặc secret, không cập nhật Jira/nhắn owner. Kết quả push/ref cuối được xác minh qua Git khi xuất bản.

## Bàn giao frontend U04 ngày18/10/2026

- Người dùng yêu cầu “hoàn thành nốt frontend để bàn giao unit 4”; đã hoàn thiện toàn màn/luồng consumer còn lại F05/F06/F08: SubjectInfoTab/EditForm, ManagerChangeDialog dùng picker, SubjectStatusActions và ArchiveRejectedDialog; detail khóa editor/action cạnh tranh và dùng chung version từ GET. Code readonly, archived metadata/manager theo Q10, null unassign rõ ràng, version conflict giữ draft chờ user reload, không optimistic/auto retry mutation.
- Success đóng editor/refetch enrichment/classes, GET lỗi sau commit vẫn báo đã lưu và retry chỉ đọc; mutation401/403 che privileged detail, đổi subjectId remount bỏ callback/draft cũ. Focus về action sau cancel/save/reload hoặc notice khi GET fail. Blocker OPEN+DRAFT chỉ đọc, count/partial/malformed khác0, không link lớp hoặc student count theo Q07.
- Thêm32 test (9 edit/6 manager/11 status/6 detail integration); toàn frontend124/24 file PASS cuối với --maxWorkers=4, không nới timeout/assertion. Typecheck/lint/build PASS; một lượt test song song Next dev2 timeout, dừng server và giới hạn tài nguyên rồi pass. Typecheck đầu lỗi count có thể undefined đã sửa narrow number. Backend không sửa/chạy lại, không cộng bằng chứng backend cũ thành run mới.
- Desktop browser fixture loopback kiểm hai phiên conflict giữ draft→user reload, edit, unassign, archive/reopen đổi version/nút, archive reject OPEN+DRAFT và classes readonly. Ảnh ngoài repo u04-version-conflict.png/u04-archived-detail.png/u04-archive-rejected.png. Tab/server riêng đã đóng/dừng theo PID+command line xác minh, dọn fixture/script/metadata và Next agent files tự sinh. Chưa chứng nhận provider thật/mobile/cross-browser/NFR.
- Frontend code consumer sẵn sàng review/tích hợp; F02 RoleGuard và F04 account-search adapter/schema, shared auth/CSRF thật U01 vẫn pending. B03 schema, B08 account guard/Q14, B09 U05 checker/writers, B10 durable/dedup audit, independent tester/NFR không được đánh complete. Tài liệu `construction/u04-subject-administration/code/frontend-handoff.md` có contract/files/checklist và gate bàn giao. Giữ index, không commit/push/Jira/nhắn owner; TEMP phải dọn trước PR main.

## Lát cắt F04 form thêm môn U04 ngày2026-10-18

- Có SubjectFormDialog, ManagerPicker và managerSearch boundary; list mở dialog, chỉ success POST mới đóng/refetch/counts, giữ filter hiện tại. Validate/normalize và gửi managerId/null, không contact/role; duplicate và manager invalid hiện lỗi sửa được,401/403 riêng, lỗi chưa rõ giữ draft/không auto retry. Pending ref chặn hai submit cùng lúc; khóa input/cancel/close/Escape/backdrop.
- Shared Dialog thêm size lg/viewport scroll/focus trap/return focus/dismissible, mặc định consumer cũ giữ nguyên. Frontend92 test/20 file PASS (thêm22), typecheck/lint/build PASS. Browser QA phát hiện focus khi nút mở disabled do refetch; sửa và kiểm lại cả success/GET failure, xác nhận thực tế return focus. Fixture chỉ loopback, đã đóng tab/dừng server/dọn script; không phải provider thật. Test/build sandbox lần đầu lỗi ENOENT cache/Access denied SWC, chạy lại quyền đọc hợp lệ pass, không nới test/config/dependency.
- Chưa có implementation/schema/acceptance U01 account search trong checkout; boundary default503, không gọi HTTP đoán/schema giả hoặc cung cấp fixture production. F04 form/picker consumer complete, F04 tổng chờ adapter thật. B12/RoleGuard/CSRF, B03 schema, B08 Q14/account guard, B09 U05 writers, B10 durable/dedup và NFR/tester giữ pending. Backend không sửa, không chạy lại Maven/DB.
- Đọc `construction/u04-subject-administration/code/create-form-review.md`; tiếp theo F05 sửa metadata/version conflict. Không commit/push/Jira/nhắn owner hoặc thay index; TEMP phải dọn trước PR main.

## Lát cắt frontend đọc và security consumer U04 ngày 2026-10-18

- Có hai route subjects; F03 list/search/debounce/status counts/page và F07 classes chỉ đọc đã có code/test. Detail có info/manager/meta và lazy classes,401/403/404/retry, bỏ response cũ. F05/F06 mới phần hiển thị, forms/mutations F04–F06/F08 còn pending. F02 tổng vẫn pending RoleGuard U01; current-user chưa có role/auth state, admin layout hard-code không phải bằng chứng quyền.
- Frontend70 test/17 file PASS, typecheck/lint/production build PASS. Browser desktop kiểm list/detail/classes/filter với API fixture loopback chỉ cho QA, không tích hợp vào production; đã đóng tab/dừng server. Không chứng nhận mobile/provider/auth thật.
- Backend chỉ thêm test matrix cả7 endpoint với anonymous/ba role khác Admin/U01 deny/unavailable/null; target42 PASS (39 HTTP consumer +3 architecture),0 lỗi/skip. Không đổi backend production, không chạy lại toàn DB; full271/31 PostgreSQL là bằng chứng B11 trước. B12 JWT/session/CSRF/filter-chain và các gate U01/U02/U05/Q14/NFR/tester vẫn pending.
- Hướng dẫn: `construction/u04-subject-administration/code/frontend-read-review.md`. Tiếp theo F04 dialog thêm và ManagerPicker theo boundary đề xuất, không đưa account fixture vào runtime production. Giữ nguyên Git index; không commit/push/Jira/nhắn owner. Tài liệu TEMP phải dọn trước PR main.

## Lát cắt REST API U04 ngày 2026-10-18

- Sau lệnh “tiếp tục đi”, B11 có bảy endpoint SubjectAdminController, SubjectRequests JSON whitelist/type/presence, SubjectResponses snapshot và SubjectExceptionHandler scoped. Actor chỉ từ AuthenticationPrincipal; parser không lấy role/actorId request, không đổi Jackson/security/shared handler hoặc dependency. PATCH chỉ metadata, manager PUT bắt buộc có field kể cả null, version JSON integer; GET list whitelist q/status/page/size, q max200 và không repeated params.
- POST201 + relative Location; command response không đọc lại sau commit, manager/classCount unknown=null; GET enrich như B07. ProblemDetail có fieldErrors khi parser lỗi, archive count/blockers sau authorize, phân biệt duplicate/version/integrity/storage/unexpected và không lộ SQL/stack trace.
- Toàn Maven271 test PASS,0 failures/errors/skipped,31 PostgreSQL16.15 regression cases và32 HTTP consumer cases mới,3 rule ArchUnit. Lần đầu30 HTTP pass rồi bổ sung2 case filter/paging và explicit-null manager. Test standalone MockMvc + service thật/repo/provider mocks, principal fixture phía server; không filter chain/JWT/HTTP+DB end-to-end. Container test đã dọn, giữ index; không commit/push/Jira/nhắn owner.
- Mark B11/bước5 API complete ở cấp code/developer checks; B12 JWT/session/CSRF thật và các gate U01/U02/U05, Q14, NFR/tester độc lập vẫn pending. Đọc `construction/u04-subject-administration/code/api-review.md`. Tiếp tục consumer security checks/route/screens theo plan, không chặn toàn bộ việc code vì chưa bàn giao.

## Tiếp tục code-generation-plan và audit consumer U04 ngày 2026-10-18

- Người dùng muốn tiếp tục hoàn thiện code-generation-plan dù nhóm chưa chốt bàn giao. Tiếp tục phần U04 theo checkpoint review; không yêu cầu chờ toàn Epic/provider hoặc tự nhận integration xong. Phần tiếp theo sau B10 là B11 API, rồi security/route/screens/tests theo kế hoạch hiện có.
- B10 consumer thêm ACCESS_DENIED trong SubjectAdminAccess dùng chung, không copy provider reason/secret; audit denied lỗi trả SUBJECT_AUDIT_UNAVAILABLE503 và vẫn chặn quyền. SubjectAuditRecorder.accessDenied NOT_SUPPORTED tạm dừng caller, U02 vẫn phải mở tx riêng; năm success hooks vẫn MANDATORY. Service đọc thêm injection audit/Clock, command/status dùng cùng helper.
- Toàn Maven239 pass,0 failures/errors/skipped,31 PostgreSQL16.15 case;9 unit audit mới,2 DB fixture mới. Có DENIED fixture sống sau outer command rollback và SUCCESS fixture audit row/subject commit hoặc rollback cùng nhau; không phải implementation/schema thật U02. Kiểm unit/regression trước đó73 pass. Container riêng đã dọn, Git index giữ nguyên, không commit/push/Jira/nhắn owner.
- B10 tổng vẫn pending durable provider U02, dedup U01 và Q14; B08/B09 provider integration, U01 migration/JWT/session và NFR/tester thật chưa xong. Đọc `construction/u04-subject-administration/code/audit-review.md`. Không coi mục tiêu hoàn thiện code là cho phép bỏ các gate hoặc mở production mock/allow.

## Lát cắt lifecycle U04 ngày 2026-10-18

- Sau người dùng hỏi khóa account và cho “tiếp tục đi”, thực hiện B09 core: SubjectStatusService archive/reopen, ISubjectMutationGuard/SubjectMutationGuard, SubjectLifecycleBudget, typed archive rejection và status success audit hooks. Khóa subject giữ trong caller transaction READ_COMMITTED, timeout lock/query PostgreSQL có restore khi thành công, không dùng reader async B07.
- Thêm contract ISubjectArchiveCheck U04-owned nhận remainingBudget, count+blockers cùng transaction; chưa có provider production/default stub. Giữ nguyên ISubjectClasses legacy; archive fail closed503 khi thiếu checker mới dù legacy count0, reopen không đọc/sửa lớp. Hai contract mới cần U05 xác nhận và nối mọi writer create/open/reopen; không tự nhận đã bàn giao/triển khai U05.
- Maven228 test PASS,0 failures/errors/skipped,29 PostgreSQL16.15 case; thêm22 unit lifecycle/guard,1 wiring và8 DB case. Có SQL/lock timeout thật, restore setting/isolation guard, status audit rollback và hai thứ tự lock với lớp fixture. Account/auth/audit vẫn test doubles; fixture không thay U05 implementation hoặc independent tester. Chưa đo NFR p95/hard latency của provider thật.
- Đánh subtask core/developer checks B09 và hai success audit hooks; B09/B10 tổng vẫn pending cùng B08 account guard/Q14. Hướng dẫn `construction/u04-subject-administration/code/lifecycle-review.md`; chưa có HTTP/JWT thực tế/frontend screen. Dọn container test riêng, giữ Git index; không commit/push/Jira/nhắn owner.

## Lát cắt command core U04 ngày 2026-10-18

- Sau review B07 và lệnh “tiếp tục đi”, triển khai B08 core ở SubjectCommandService, helper quyền dùng chung SubjectAdminAccess và ba success audit hook qua SubjectAuditRecorder. Create/update metadata/change manager có transaction, Clock, version/no-op/CAS; repo thêm đọc khóa môn MANDATORY, sau khóa account khi đổi phân công.
- Production gate account U01 chưa có; boundary ISubjectManagerAssignmentGate chỉ nội bộ U04 chờ adapter. Thiếu provider thì create có manager và mọi thay đổi phân công thật (kể cả bỏ) trả503, không giả kiểm contact là khóa. Tạo không manager/metadata vẫn hoạt động; same-manager valid no-op không cần gate. Q14 tự gỡ khi disable chưa triển khai; không viết SQL account production hoặc sửa implementation U01.
- Clean Maven test PASS197,0 failures/errors/skipped,21 PostgreSQL16.15 cases. Thêm25 test command,1 wiring và5 DB cases: proxy transaction, audit exception rollback insert/update/manager, lock giữ sau detach, concurrent create trả1 success/1 duplicate. DB gate chỉ khóa accounts UUID fixture, contacts/audit dùng mock; chưa chứng minh guard U01 hoặc atomic audit U02 thật. Sửa tên interface thiếu I theo ArchUnit, không thay rule/test để bỏ kiểm.
- B08/B10 tổng vẫn pending; chỉ đánh subtask có bằng chứng. Hướng dẫn `construction/u04-subject-administration/code/command-review.md`. Chưa có B09 lifecycle/status, HTTP/JWT thật, frontend screen; chưa đo p95 hoặc gán/disable thật. Dọn container test riêng sau chạy, giữ Git index, không commit/push/Jira.

## Lát cắt service đọc Admin U04 ngày 2026-10-18

- Sau review B06 và lệnh “tiếp tục đi”, triển khai B07: SubjectAdminService/SubjectAdminViews/SubjectClassesReader. Kiểm ADMIN + IAuthorization trước đọc; list/detail/classes, paging/counts Q08, contact dedup mỗi request, classes count batch theo trang và deadline1s với tài nguyên giới hạn. Lỗi auth chặn đọc, lỗi DB không bị giả thành empty, lỗi contact/classes chỉ hạ phần hiển thị.
- Toàn backend166 test pass,0 failures/errors/skipped, gồm16 PostgreSQL16.15 case cũ và22 test mới (15 admin read,6 deadline/reader,1 wiring),3 rule ArchUnit. Container test riêng được dọn; không thay frontend, Git index, U01/U05 implementation, dependency hoặc config chung.
- Đánh hoàn thành riêng B07 trong kế hoạch tạm; service F1–F6 chính thức vẫn pending command/guard/audit. Đọc `construction/u04-subject-administration/code/admin-read-review.md`; sau review sẽ sang B08. Tên action consumer cần đối chiếu U01; chưa có bằng chứng JWT/session/provider thật hoặc NFR p95. B03 migration U01 thật, B10 audit denied, Q14 và HTTP API vẫn pending. Không commit/push/Jira.

## Lát cắt query và scope U04 ngày 2026-10-18

- Người dùng đã đọc persistence-review và cho tiếp tục B06. Đã cài SubjectQueryService/ISubjectQuery và SubjectScopeService/ISubjectScope bằng repository U04; scope chỉ kiểm phân công hiện thời, không gọi ngược authorization U01, không cache hoặc loại môn archived. U01 vẫn kiểm current session/role/account ACTIVE.
- Đã thêm existsByIdAndManagerId cho kiểm scope một truy vấn và14 unit/wiring test; thêm1 PostgreSQL case A→B→null/archived/global counts. Toàn backend144 test pass,0 failures/errors/skipped, gồm16 case PostgreSQL16.15 và3 rule ArchUnit. Context tối thiểu xác nhận bean query/scope thật tự thay đúng stub; provider classes vẫn là placeholder đến khi U05 giao bean thật.
- Theo Q05 đã chốt, ISubjectClasses placeholder U04 ném SUBJECT_CLASSES_UNAVAILABLE, không trả rỗng/0 giả. Không sửa contract signature, IdentityContractStubs hoặc implementation U01/U05. Chưa có HTTP auth/admin controller, guard lifecycle hoặc disable tự gỡ môn.
- Đánh [x] B06/bước interface tương ứng trong plan chính thức; B03 với migration U01 thật và tester độc lập vẫn pending. Đọc `construction/u04-subject-administration/code/query-scope-review.md`; bước sau review là B07 service đọc quản trị môn, guard/lifecycle vẫn cần phối hợp theo kế hoạch. Không commit, push, Jira write hoặc thay Git index.

## Lát cắt persistence U04 ngày 2026-10-18

- Người dùng đã đọc B04/F01 và yêu cầu “đã đọc xong, tiếp tục đi”; triển khai riêng B03/B05 để review tiếp. Đã thêm migration subjects, SubjectJpaEntity, ISubjectJpaRepository và SubjectRepositoryAdapter; mở rộng boundary nội bộ với search/page/count/managed-subject. Không có service/controller/scope/guard mới hoặc thay code U01/U05.
- B05 developer checks đã pass: toàn backend129 test,0 failures/errors/skipped, gồm15 persistence test trên PostgreSQL16.15 thật và3 rule ArchUnit. Kiểm schema/Flyway, public1↔DB0, stale/concurrent update, duplicate concurrent insert, FK/check/grants, query literal, rollback và persistence context không clear cả unit khác.
- B03 đã có SQL cùng kiểm riêng, nhưng còn [ ] chạy toàn chuỗi với migration accounts thật U01. Test dùng accounts UUID fixture chỉ trong src/test/resources, schema riêng có UUID; không giả nhận U01 schema hoặc G1 đã xong. Tester độc lập vẫn cần review/integration evidence liên unit.
- Đánh [x] B05 và các subtask B03 có bằng chứng; không đánh complete U04 hay migration gate chung. Đọc `construction/u04-subject-administration/code/persistence-review.md`; bước sau sau review là B06 query/scope và guard phối hợp U05. Không commit, push, Jira write hoặc thay Git index.

## Lát cắt code U04 đầu tiên ngày 2026-10-18

- Người dùng yêu cầu “bắt tay vào làm luôn” sau kế hoạch song song. Phạm vi lượt đầu là B04 domain/test và F01 frontend foundation/test để review; không triển khai cả U04 hoặc phần U01/U05.
- B04 đã có SubjectCode, Subject bất biến và ISubjectRepository nội bộ. Maven toàn backend pass114 test, gồm20 lượt test domain U04 và3 rule ArchUnit. Chưa có migration, persistence adapter, service hoặc controller U04; test trên không là integration evidence.
- F01 frontend types/API/validation có15 lượt test U04 pass; toàn frontend49 test pass, typecheck/lint pass (typecheck đã chạy lại sau sửa mock). DTO web và API client chưa tích hợp server. Đọc `construction/u04-subject-administration/code/code-summary.md`. Giữ nguyên index/staging hiện có của người dùng, không git add/reset, commit/push hoặc Jira.
- Đã đánh [x] B04/F01 trong kế hoạch tạm và bước domain tương ứng trong plan chính thức; các bước implementation còn lại chưa hoàn thành. Chờ người dùng review lát cắt này trước khi sang persistence/service theo cách làm chủ code đã yêu cầu.

## Phiên lập kế hoạch U04 ngày 2026-10-18

- Theo yêu cầu người dùng: chỉ lập kế hoạch tiếng Việt để người dùng tự code U04 Subject Administration (UC16–19), không triển khai ứng dụng, commit/push hoặc cập nhật Jira.
- Đã khảo sát source tại HEAD `3f7e650`, thiết kế U04 hiện hành, phụ thuộc U01/U02/U05, SRS cục bộ ngày 09/10 và sáu UI artboard a05–a07d. Jira KAN-24 chưa đọc được qua công cụ; chưa xác nhận SRS Google mới nhất.
- Kết quả tạm: `construction/plans/u04-kan24-manual-implementation-TEMP.md` (25 bước) và `construction/plans/u04-kan24-decisions-TEMP.md` (14 quyết định). Phải dọn hai file TEMP trước PR vào main; chuyển quyết định đã chốt sang tài liệu chính thức nếu cần giữ.
- Đã đọc câu trả lời: Q01–Q11/Q13 chọn A; Q14 chọn X, disable Subject Manager tự gỡ phân công môn. Kế hoạch bổ sung B08-D (contract U01 khai báo/U04 cài, gỡ mọi môn ACTIVE/ARCHIVED, version/audit/transaction/race test); không tự mở rộng sang phân công giảng viên lớp. Q12 chọn X “Tôi không biết”: ghi phương án kỹ thuật đề xuất, không giả nhận người dùng đã chọn A/B.
- Rà soát tiếp theo theo thông tin nhóm code song song U01–U05: đã đọc Jira KAN-18/KAN-21/KAN-24 qua trình duyệt đăng nhập và các story giao thoa KAN-144/146/50/140/150. Mapping U01 UC01–12, U04 UC16–19 (KAN-147–150), U05 UC20–30 khớp. Epic mô tả Blocks theo contract readiness cho tích hợp, không chờ toàn Epic Done. Kế hoạch tạm bổ sung owner/file ownership, C01–C06, thứ tự P0–P4 và mốc tích hợp G0–G5 riêng của U04; B12/B13 là nhận bàn giao U01. “Chưa có code” chỉ nói checkout local, chưa kiểm nhánh thành viên. Jira chỉ đọc, không thay trạng thái hoặc nội dung.
- Không thay approval/checklist code chính thức hoặc đánh complete U04. Bước tiếp theo của người dùng: hoàn tất bàn giao/tài liệu B01 rồi code theo kế hoạch; JWT/session/OTP thuộc U01, audit persistence thuộc U02, dữ liệu lớp thuộc U05. Quyết định Q04/Q14 trong kế hoạch tạm cần đồng bộ baseline và owner trước tích hợp; các lịch sử bên dưới giữ nguyên.
## U05 stash recovery — 2026-10-18

- Người dùng yêu cầu commit local trên nhánh `Class&Enrollment`; sẽ tự merge U01/U04 khi các thành viên hoàn thành. Không push hoặc mở rộng scope tích hợp trong đợt này.
- [x] Theo yêu cầu "giúp tôi xử lý", khôi phục 20 file backend/SQL/test từ stash `86fc91e092915f1b292019f782b99bbcfa55ee63`; ghép tài liệu/audit, giữ UI mới.
- [x] Chuyển các test UI cũ còn thiếu, sửa khác biệt regex email backend/frontend; U05 có 29 test backend và 21 test frontend.
- [x] Chạy mới: backend 123 test/29 suite qua, frontend 37 test/8 file qua; typecheck/lint và diff check qua. Component/CSS/preview giữ nguyên hash; không chạy lại build/browser vì không đổi frontend thực thi.
- [x] Sao lưu vào `.tmp/u05-stash-recovery-2026-10-18/`; cả ba stash giữ nguyên, Git index không thay đổi, chưa commit/push.
- U05 vẫn chờ U01/U04/U02 cho auth, API/service, DB và audit thật. SQL pending và persistence opt-in giữ nguyên. Chi tiết mới nhất: `construction/u05-class-enrollment/code/code-summary.md`.

## U05 UI reference implementation — 2026-10-18

- Người dùng yêu cầu: "check ui này và sửa lại ui cho phần u05 hiện tại", kèm `ui-reference.zip`. Nhánh `Class&Enrollment` chỉ có U05 slots lúc bắt đầu; implementation phiên cũ không có trong checkout này.
- [x] Áp dụng phong cách HTML/CSS tham chiếu cho các view U05 và dialog, CSS module riêng, props/callback độc lập; giữ slot liên unit.
- [x] Development preview `/preview/u05` có dữ liệu minh họa và ba vai trò; không dựng auth/API/backend giả. Server page chặn production bằng notFound.
- [x] 32 test frontend (16 test U05) qua; typecheck/lint qua; browser desktop/mobile kiểm tạo lớp, detail, hủy gỡ và role. Mobile 390px không tràn toàn trang. Ảnh ở `.tmp/u05-ui-evidence/`.
- [x] Build Webpack bản cuối qua; kiểm HTTP production: preview 404, không có nội dung minh họa; health 200. Trang development vẫn hoạt động tại `/preview/u05`.
- Tài liệu: `construction/plans/u05-ui-reference-2026-10-18.md`, `construction/u05-class-enrollment/code/code-summary.md`.
- Toàn bộ U05 và kiểm quyền/integration vẫn chưa complete. Chờ U01/U04 và các service/controller thật để nối dữ liệu; trạng thái lịch sử bên dưới không phải implementation scope hiện tại.

## Latest authorized UC-table synchronization

- 65 active UC, 50 MVP stories + 1 extension, 15 units. Inception and Construction docs now follow docs/usecase_table.md. The untracked pre-sync snapshot in aidlc-docs/historical was removed at the user's request on 2026-10-14.
- UC26/76/79 removed/consolidated; UC45/71 restored. YouTube video links are MVP. Student feedback only after submitting Practice work. Settings UI includes institutional AI API via U13 protected records.
- This is documentation-only. Jira, runtime/code/contracts/migrations, Google Reports and diagram files are not updated; old stage/code approvals remain historical. D01–D04 remain open.

## Current Inception revision — 2026-10-13

- Người dùng yêu cầu sửa Inception sau khi chốt hai loại bài Assignment/Quiz, SWR/SWD, no Code/payment; Assignment cá nhân/nhóm với Draw.io tùy chọn; shared educator quota, personal API/MCP cho educator/Student.
- Nguồn Inception: `inception/README.md`, `inception/requirements/requirements.md`, `inception/requirements/use-cases.md`; **65 active UC, 50 MVP stories + 1 extension story, 15 logical units**. U07 đổi thành Usage/Quota, U13 Connections/MCP; IDs cũ giữ và retired không reuse.
- D01–D04 chưa chốt; D05 đã chốt bổ sung backup/activity/email/minh bạch AI theo request mới. Chi tiết tại `inception/requirements/council-features-2026-10-13.md`; Giữ Quiz Practice/Group Graded theo policy cũ, Student group context hạn chế, không gán numeric quota hay chứng nhận survey.
- **Construction design/shared infrastructure/code plans đã revision theo 65 UC; contracts/code/migrations thực tế, Google Reports và diagrams chưa revision theo scope này.** Stage/approvals/progress dưới đây thuộc lịch sử 09/10 và không xác nhận scope mới đã implemented/approved.
- Bảng report `docs/usecase_table.md` đã sync 65 UC; bản 73 UC giữ tại `docs/history/use-cases-73.md`, các liên kết baseline được chuyển tới bản lưu, không đổi scope Construction.
- Không tạo workflow mới hoặc tiến sang code; documentation-only sync đã kiểm catalog/ownership/links. Bước tiếp theo cần review decisions và downstream impact trước code mới.


- Consolidation đã chốt: UC70 View Settings / UC71 Edit Setting riêng; UC76/78/82 vào Settings, UC26 vào UC24/27, UC79 retired; UC42 Individual / UC45 Group; UC80 vào58, UC81 vào37. Giữ ID lịch sử, chức năng/stories/unit services không bị loại.

- Đồng bộ Inception chi tiết theo 65 UC: requirements/stories/personas/ports/orchestration/dependency và kế hoạch current đã review. Lịch sử bên dưới giữ nguyên.

## Current Construction document revision — 2026-10-13

- Design/plans current: construction/README.md và unit-summary.md; 15 units, 65 active UC, 50 MVP + 1 extension và 39 FR IDs theo Inception.
- Snapshot trước revision, plans/questions/reviews cũ và folder unit cũ đã xoá ngày 2026-10-15 (xem lại qua git history); current delta plans đều chưa triển khai.
- U07 Usage/Teacher Quota; U13 Connections/MCP; Assignment/Quiz, diagram tùy chọn, YouTube links MVP; Lesson processing extension; backup/activity/email và merged UC70/58/37 được mô tả.
- Next: review design/delta, resolve D01–D04 hoặc giữ defaults khi triển khai phần liên quan; actual contracts/code/schema/Compose và tests không được đánh complete chỉ vì docs đồng bộ.

## Project Information
- **Project Type**: Greenfield
- **Start Date**: 2026-09-12T15:04:50Z
- **Current Phase**: CONSTRUCTION
- **Current Stage**: Construction documentation revision theo 65 UC; current delta code plans chưa triển khai. Code Generation Part 2 và completed checkpoints trước revision là lịch sử, không xác nhận scope mới.
- **Resume action**: Đọc construction/README.md và unit-summary.md. Review current delta plans và decisions D01–D04 trước code/contracts/migration; không chạy baseline payment/Code Lab hoặc kế thừa approval cũ cho scope mới. Tester riêng viết/chạy integration evidence.

## Latest documentation decision
- **2026-10-09 (sau recheck)**: Đóng C01–C05 ở cấp tài liệu: U07/U05/U13 một HOLD và settle lượng dùng thật/trả dư; FR-024/story dùng đúng UC/màn và Chủ nhiệm môn bật phân bố điểm; IAiUsage có findHold/UsageStart REPLAY/CallSnapshot/checkpoint/scanClaimId; shared infrastructure 15 unit. Các việc code/contracts/migration/integration theo checklist vẫn chưa triển khai; stage progress và approvals giữ nguyên.
- **2026-10-09**: Đồng bộ local 73 UC, 51 story và 15 unit; US-CAT-005 bỏ, US-SET-001 thêm U03; U10 bỏ, US-ASM-008/009/010 về U08. Sửa quyền Admin, mapping/kiến trúc/màn/nguồn, state/wave/gate và U05 retry/credit/summary. Google SRS/hình người dùng/code không sửa trong revision này; giữ approvals và code progress lịch sử.
- **2026-10-08**: Added UC 70 View Audit Log and its Administrator-only permission row to the Google SRS; verified the saved document. Synchronized `docs/use-cases-73.md` and active Inception requirements, personas, stories, application design and planning summaries with 70 UC, 51 stories and 16 units. UC 70 belongs to U02 and uses the existing Audit Log screen. Screen names now match the latest `screen-flow (1).drawio`: Teacher Assigned Classes, Subject Classes, Subject Template and Template Editor (UC 53-54). Student My Classes is unchanged. Construction designs and code plans were synchronized in the follow-up revision; existing contracts/application code and older local specification catalogs are not automatically updated. The user's diagram was not edited; implementation checkpoints and historical audit records are preserved.
- **2026-10-04**: Email provider changed to Brevo SMTP for demo/production; Mailpit retained for local/tests. U01/U16 email design, shared infrastructure, Inception provider selection and code-generation references synchronized. Existing code-generation approval checkpoints remain unchanged.

## Workspace State
- **Existing Code**: Yes (khung code tạo lại 2026-10-16: package của 15 unit, interface `Ixxx` trong `contract`, slot frontend; chưa có nghiệp vụ)
- **Programming Languages**: Java 17 / Spring Boot 3.5, TypeScript / Next.js 16
- **Build System**: Maven, npm
- **Reverse Engineering Needed**: No
- **Workspace Root**: thư mục gốc của repo (đường dẫn tùy máy, ví dụ `D:/Github/AI-Powered-Learning-Platform`)

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
- [x] Infrastructure Design - 15 unit hiện hành; approvals baseline cũ giữ lịch sử
- [x] Code Generation Part 1 (plans) - 16 plans approved 2026-10-05 (historical; 15 unit hiện hành) for the previous baseline; scope revision documented 2026-10-09
- [ ] Code Generation Part 2 (code) - in progress: U03 (wave 1)
- [ ] Build and Test
- [ ] Operations (placeholder)

## Unit Progress

Tài liệu hiện hành theo 73 UC/51 story/15 unit ngày 2026-10-09. Approvals 05/10 giữ baseline cũ, không phê duyệt code đã sinh theo scope mới. Code cũ (gồm phần U03 đã code ngày 2026-10-05) đã xóa; khung code tạo lại ngày 2026-10-16. Các dòng chưa cập nhật dưới đây giữ baseline lịch sử; riêng U05 được cập nhật theo Class & Enrollment hiện hành ngày 2026-10-18.

| Unit | Design stages | Code plan | Code |
|---|---|---|---|
| U01 Account & Access | Design revision 73 UC ngày 2026-10-09; Role/authentication cho mọi unit; counts cho Admin Dashboard. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | Code và unit test viết 2026-10-17 (UC 01–12, xem `u01-account-and-access/code/code-summary.md`; backend 206 test, frontend 50 test toàn repo), chưa chạy với datastore thật, chưa commit, chờ người dùng review |
| U02 Audit | Design revision 73 UC ngày 2026-10-09; Audit append-only trong transaction mọi command nhạy cảm. | Plan chi tiết hóa và được duyệt 2026-10-18 | Code và unit test viết 2026-10-18 trên nhánh `audit` (backend 26 test, frontend 11 test), người dùng duyệt 2026-10-18, chờ gộp qua PR |
| U03 Job and Settings | Design revision 73 UC ngày 2026-10-09; Tệp/worker/events dùng chung; lưu Settings và kiểm version/audit; U07/U13 khai báo các mục của mình. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | Code và unit test viết lại 2026-10-16 theo plan mới (backend 84 test, frontend 12 test), chờ người dùng review |
| U04 Subject, Class, Enrollment & Learning Access | Design revision 73 UC ngày 2026-10-09; R2–R5 scope cho tài nguyên môn/lớp; Admin chỉ quản lý môn, danh sách lớp của môn chỉ đọc. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U05 Class & Enrollment | Current UC20-30 design and existing contracts; other rows retain historical baseline where noted. | Full completion authorized 2026-10-18; completion execution plan checked. | Code implemented: service/API/contracts and frontend wiring; backend 304 (U05: 210) / frontend 70 tests, lint/typecheck/build pass. Awaiting real cross-unit/schema/session integration; see construction/u05-class-enrollment/code/code-summary.md. |
| U06 Rubric & Subject Question Bank | Design revision 73 UC ngày 2026-10-09; Rubric thuộc bài, tự tạo và khóa khi phát hành; câu ngân hàng SUBJECT và câu riêng ASSIGNMENT. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U07 Payment & AI Credit | Design revision 73 UC ngày 2026-10-09; Ví của ba role; snapshot payment, verified webhook và grant định kỳ qua Settings. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U08 Assessment Core, Publication & Copy | Design revision 73 UC ngày 2026-10-09; Vòng đời chung UC 35/42–45, bài của môn, copy/version; U09 cấu hình theo dạng. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U09 Question Type Authoring | Design revision 73 UC ngày 2026-10-09; Cấu hình quiz và bốn dạng assignment, document model; U08 vòng đời, U06 rubric. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U11 Attempt & Submission | Design revision 73 UC ngày 2026-10-09; Quiz Practice tách Student Assignments; lần làm bài của môn ghi classId. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U12 Group & Allocation | Design revision 73 UC ngày 2026-10-09; Chia nhóm/leader hỗ trợ Teacher Class Detail và UC 45/27. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U13 AI & Code Execution | Design revision 73 UC ngày 2026-10-09; AI draft, Practice grading UC 29, proposals UC 38, Judge0 UC 25/43, số liệu AI UC 58. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U14 Group Document & Submission | Design revision 73 UC ngày 2026-10-09; Tài liệu nhóm chỉ ở lớp, GRADED; chỉ leader nộp. | Approved 2026-10-05 (historical baseline); revision tasks unchecked | - |
| U15 Admin Dashboard | UC66/KAN-80 hiện hành; counts và backup metadata, current-contract slice A duyệt 2026-10-20. | Plan bảy task duyệt 2026-10-20; C01/C02 metadata U03 deferred | Code-ready slice A: backend verify 755 total/693 pass/62 skip; frontend253 pass, lint/typecheck/build pass; independent review P2 đã sửa. Full UC66 còn U03/integration/NFR acceptance. Xem construction/u15-admin-dashboard/code/implementation-summary.md |
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
- 2026-09-26: Messaging redesign: audit INSERT in the business transaction (no audit queue); required cross-unit reactions (grading on submit, auto-submit on retire, group docs on open/new group, lock release on member removal, Code Lab score) via ports that enqueue U02 jobs; events only for U16 notifications. RabbitMQ: exchanges `jobs`, `platform.events`, `platform.realtime`; eight U02 job queues (`jobs.scheduled`, `jobs.triggered`, `jobs.email`, `jobs.gemini`, `jobs.youtube`, `jobs.code`, `jobs.drive`, `jobs.payos`), U16's `jobs.notification`, and temporary `jobs.realtime.{instanceId}` queues. Redis: 12 key groups named by purpose; Gemini daily cost cap shared by U05/U13 via `IAiBudget`. The affected code plans await re-approval.
- 2026-09-27: Screen flow (`docs/screen-flow.drawio`) chốt theo bản nhóm vẽ; frontend gộp theo đó: nhập CSV thành `ImportAccountsPanel` trên danh sách tài khoản (U01), cài đặt email thành toggle trên trang thông báo (U16), tổng quan tài liệu nhóm thành panel trong danh sách bài nộp (U14), chấm nhóm thành chế độ `groups` của `GradingWorkspacePage` (U15). Plan U01/U14/U15/U16 chỉ đổi tên component tương ứng.
- 2026-09-27: Updated UC56, UC57 and UC75 in `docs/use-case-specifications.md` to match the current design; this documentation change did not advance the Construction stage.
- Full chronological log: `audit.md`.

## UC57 naming correction — 2026-10-14

- [x] Confirm requested name from conversation: Create/Update/Delete Class Group.
- [x] Update English UC table and Inception catalog; verify names agree.
- Documentation-only correction; no stage approval or application implementation advanced.

## UC57 document synchronization — 2026-10-14

- [x] Đồng bộ UC57 — Create/Update/Delete Class Group trong FR-025, stories, component map, thiết kế nhóm/lớp, shared contract và code generation plan.
- [x] Tách truy vết: UC23 là điểm truy cập; UC54 xem nhóm; UC55 gửi/hủy yêu cầu; UC57 giảng viên thao tác nhóm và xử lý yêu cầu.
- [x] Kiểm 66 UC hiện hành, tên UC57 nhất quán, ánh xạ giao diện và diff Markdown. Kết quả: `inception/plans/uc57-document-sync-2026-10-14.md`.
- Không chuyển stage, duyệt hoặc triển khai code; folder unit legacy và tài liệu lịch sử giữ nguyên.


## U05 navigation merge repair - 2026-10-18
- [x] Reproduce the removed dashboardNavEntries import after merging main (3edc6f1).
- [x] Use the shared navEntries registry for external links; preserve local preview navigation and filter duplicate U05 entries.
- [x] Add four real-registry regression tests; observe failure before repair and success after repair.
- [x] Verify 59 frontend tests, lint, typecheck and production build all pass.
- Scope and evidence: `construction/plans/u05-merge-nav-repair-2026-10-18.md`. Existing unit progress, backend integration limitations and extension configuration retained; no commit/push.

## U05 Class & Enrollment code completion - 2026-10-18
- [x] User authorizes full U05 against existing contracts, superseding the older dependency-free-only scope.
- [x] Implement application lifecycle/enrollment services, scoped REST APIs and real IClassAccess/IClassScope/ISubjectClasses providers.
- [x] Wire frontend production routes to APIs; preserve development-only fixtures and all five extension slots.
- [x] Verify backend 164 tests, frontend 70 tests, lint/typecheck and production build; independent review's409 message defect repaired and re-reviewed.
- Code-ready for merging with other units; SQL remains pending and U05_PERSISTENCE_ENABLED defaults false until real U01/U04 schemas/contracts are ready. U02 audit persistence, real session authorization and PostgreSQL concurrency remain tester/integration gates; they are not marked complete.
- Results: `construction/u05-class-enrollment/code/code-summary.md`; execution: `construction/plans/u05-completion-2026-10-18.md`. No commit/push.

## U05 unit test expansion - 2026-10-18
- [x] Add read/scope/role matrix and successful DTO/candidate/summary cases, including denied write access on all nine mutation entrypoints.
- [x] Add API request/response validation and server-principal mapping cases.
- [x] Observe independent batch transactions, later-row failures/retry and an ambient caller rollback without requiring live PostgreSQL.
- [x] Clean full backend run: 304 tests/36 suites pass; 210 U05 cases, including 140 newly added. Independent quality review and source SHA256 check passed.
- Production source unchanged from before this test-only task. Coverage percentage and live integration are not claimed; no commit/push.
- Evidence: `construction/plans/u05-unit-test-expansion-2026-10-18.md`.

## U09 Clean Code review follow-up - 2026-10-20

- [x] Resume existing U09 Construction and review the existing uncommitted refactor under the explicitly requested Clean Code skill.
- [x] Retain 5 new MCP characterization cases verified against unchanged production code, then extract private MCP validation/lookup and clarify local names in batch/proposal services and gradebook UI.
- [x] Verify full backend 724 total / 662 passed / 62 skipped / zero failures or errors; frontend 223 passed, lint/typecheck passed. Existing offline executable packaging and real integration/NFR gates remain open.
- Scope and evidence: `construction/plans/2026-10-20-u09-clean-code-review.md`. No public contract/schema, migration, authorization or transaction changes; no commit/push.

## U09 build repair - 2026-10-20

- [x] Resolve local Maven cache origin mismatch using an optional settings profile; full backend verify and executable JAR packaging passed (724 total / 662 passed / 62 skipped / zero failures or errors).
- [x] Bundle existing Be Vietnam Pro fonts locally, retain preload/fallback/weights, include public assets in the Docker runtime stage and use a Windows-safe Webpack build launcher. Full frontend build including standalone packaging, 223 tests, lint and typecheck passed.
- [x] Verify standalone startup Ready and packaged font references/assets. HTTP smoke blocked by sandbox socket permission; Docker image/browser and real integration/NFR remain unverified. No U09 contract/behavior/dependency changes, commit or push.
- Evidence: `construction/plans/2026-10-20-u09-build-repair.md`.

## U05 Clean Code assessment - 2026-10-20

- [x] Review U05 with Clean Code skill, existing unit design and U09 review as reference; initial working tree clean.
- [x] Identify focused readability work: route orchestration, backend naming/operation outlines, pagination arithmetic and obsolete Frame wrapper. No rewrite or extra service layers justified.
- [x] Verify unchanged-source baseline: backend 213 passed/zero failures/errors/skips; frontend 33 passed/5 files. Initial frontend sandbox temp ENOENT recorded; process-local temp and one worker rerun passed.
- [x] Record findings and behavior-preservation/test constraints in `construction/plans/2026-10-20-u05-clean-code-review.md`.
- Assessment only: production/tests/config unchanged, no commit/push; existing integration/NFR gates retained. Student archived dashboard/detail distinction is intentional, not a defect. No Construction implementation stage advanced.

## U05 behavior-preserving Clean Code refactor - 2026-10-20

- [x] User approves the previously reviewed scope; preserve prior documentation changes and execute inline.
- [x] Retain nine characterization cases passed on unchanged source; verify stale-response sensitivity and restore original before refactoring.
- [x] Clarify frontend route orchestration, typed mutation results and pagination calculations; remove obsolete Frame wrapper. Improve backend local names and operation outlines without changing call order.
- [x] Verify full backend Maven verify: 724 total / 662 passed / 62 skipped / zero failures/errors; executable JAR packaged. Frontend 232 passed/47 files, lint/typecheck and production standalone build passed; restore build-generated tsconfig changes and typecheck again.
- [x] Independent review found no regression; backend public declarations/string literals and lock/access/error blocks unchanged, frontend visible JSX text unchanged, final whitespace check passed.
- Scope/evidence: `construction/plans/2026-10-20-u05-clean-code-refactor.md`; handoff updated. Existing 62 skipped integration cases and DB/provider/browser/NFR gates retained. No API/DTO/schema/dependency/configuration changes, commit or push.

## U05 local commit grouping - 2026-10-20

- User requests local commit only, without push; use current grading-gradebook branch and preserve all existing U09 history.
- Separate Conventional Commit groups: retained characterization tests (26971c2e), backend refactor (c5cd5511), frontend refactor (1bdff199), and documentation containing the review, execution evidence, handoff, state and audit.
- Source/tests unchanged since successful verification; no additional application edits or integration claims during commit preparation. Commit results and final working-tree state verified through Git.

## Jira status reconciliation - 2026-10-20

- Live Jira: U05 KAN-21 already Done; eleven UC20-30 stories remain In Review. Kept existing statuses pending independent acceptance evidence.
- Updated U09 KAN-31 and all four UC42-45 stories (KAN-84/58/59/81) from To Do to In Review; five successful transition responses confirmed final statuses. Code/build complete does not claim real integration acceptance.
- Remaining newly assigned work: U15 KAN-32 / UC66 KAN-80, both To Do. Existing U05/U09 integration, independent tester and NFR gates retained.
- No source changes, Jira comments/messages, commit or push; this reconciliation audit/state update remains local.

## U15 implementation plan draft - 2026-10-20

- [x] Resume existing U15 design and current UC66/KAN-32/KAN-80 scope; inspect scaffold, real public ports/providers, Admin session/layout/nav and temporary homePath.
- [x] Apply Clean Code and Superpowers planning; update `construction/plans/code-generation-plan/u15-admin-dashboard-code-generation-plan.md` to seven testable tasks with interfaces, permission guards, bounded fan-out, DTO/API, UI, navigation, verification and acceptance gates.
- [x] Record C01-C07 evidence/proposals in `construction/u15-admin-dashboard/code/implementation-contract-review-2026-10-20.md`; important U03 metadata gaps remain explicit, no cross-unit contract change assumed.
- [x] Validate links, text/code fences, seven-task structure and diff whitespace; product/runtime tests not run for documentation-only planning.
- [x] User approved slice A and inline execution: “Cứ giúp tôi code trc theo plan còn khi nào u3 xong thì xử lý sau”; see approval questions and execution below.
- Recommend current-contract consumer implementation with metadata unavailable rather than fabricated; full UC66 completion still needs U03 contract handoff and independent integration/NFR acceptance. Jira unchanged, no owner messages, no source changes/commit/push. Preserve prior local audit/state updates.

## U15 current-contract implementation — 2026-10-20

- [x] User authorizes slice A and inline execution following the approved plan; missing U03 metadata remains deferred.
- [x] Implement ADMIN-only aggregate/history APIs with current-account authorization, deny audit, safe DTOs, bounded fan-out/deadline and partial availability. Keep provider contracts/implementations unchanged.
- [x] Implement `/admin` dashboard/history using existing layout/nav/session/shared UI; ADMIN_HOME=/admin. Missing startedAt stays null, latest-success/7-day summary stays UNAVAILABLE; no fabricated zeros or backup actions.
- [x] Retain RED/GREEN evidence and independent review. Repair pagination range before guard using three real-service HTTP regressions, then rerun backend focused/full checks; no other concrete review findings.
- [x] Final backend verify:755 total/693 passed/62 skipped/zero failures/errors, executable JAR. Frontend253 passed/52 files, focused21, lint/typecheck/build pass; restore generated tsconfig and typecheck again. OpenAPI YAML/local refs and whitespace checks pass.
- [ ] Full UC66 acceptance: U03 C01/C02 handoff, independent real provider/session/audit/browser integration, healthy p95 and fault/stress/JDBC resource-release evidence. Existing U07/U08 migration prerequisites of U09 remain a separate startup gate; skipped tests are not counted as pass.
- Handoff: `construction/u15-admin-dashboard/code/implementation-summary.md`; repeat commands/tester matrix: `construction/build-and-test/u15-verification.md`. Global Construction/Build&Test status not advanced; no commit/push/Jira transition or owner messages. Reduced enabled extensions locally covered with deployment/integration limits documented; PBT disabled.

## U15 local commits — 2026-10-20

- User requests commit; retain current grading-gradebook branch. Fresh precommit checks: backend49/frontend21 pass, whitespace clean.
- Local commits d3d8c7b5 (backend/API/contracts/tests), a71d7fd1 (frontend/tests), plus documentation commit containing approved plan, implementation handoff, verification and audit/state. No source changes during commit preparation.
- No push/merge/Jira mutation. U03 metadata, independent integration/browser/NFR acceptance gates remain pending; commit does not imply full UC66 Done.

## U10 giai đoạn 1 (UC 46, 47) — 2026-10-20

- [x] Code Generation Part 1: plan giai đoạn 1 được người dùng duyệt (D1–D7).
- [x] Code Generation Part 2: bước 1.1–1.11 xong trên nhánh `quiz-authoring` (chưa commit); xem `construction/u10-quiz-authoring/code/code-summary.md`.
- [x] Bước 1.12: người dùng duyệt code giai đoạn 1 ngày 2026-10-20.
- Giai đoạn 2 (UC 48), 3 (U11), 4 (AI/MCP) chưa bắt đầu.

## U10 giai đoạn 2 (UC 48) — 2026-10-20

- [x] Code Generation Part 1: plan giai đoạn 2 được duyệt (E1–E8); giai đoạn 1 commit `5d70173`.
- [x] Code Generation Part 2: bước 2.1–2.12 xong trên `quiz-authoring` (chưa commit).
- [x] Bước 2.13: người dùng duyệt code giai đoạn 2 ngày 2026-10-20.

## U11 giai đoạn 3 (UC 49–52) — 2026-10-20

- [x] Code Generation Part 1: plan giai đoạn 3 được duyệt (G1–G7).
- [x] Code Generation Part 2: bước 3.0–3.13 xong trên nhánh `quiz-practice` (tách từ `quiz-authoring` `f3b54bc`, chưa commit); xem `construction/u11-quiz-practice/code/code-summary.md`.
- [x] Bước 3.14: người dùng duyệt code giai đoạn 3 ngày 2026-10-20.

## staging merge conflict resolution — 2026-10-20

- Resolved four additive conflicts in aidlc-state/audit and Actions/AccessPolicy by preserving both branches. Kept current U15 Admin Dashboard row rather than historical U15 Grading row; retained U10/U11 progress and all audit entries.
- Verified exact union of 36 declared actions and 36 policy entries, with role/scope unchanged on both sides. No unrelated incoming source edits.
- Fresh merged-tree backend verify:852 total/775 passed/77 skipped/0 failures/errors, executable JAR packaged. Frontend292 passed/57 files; lint/typecheck pass. Logs .tmp/u15/merge-*.log. Full browser/integration/NFR gates unchanged.
- Marked only four conflict paths resolved in index. Merge remains pending its final commit; no merge commit/push performed for this check.

## U13 Code Generation Part 1 — 2026-10-21

- [x] Resume existing design for UC58–62, US-CNT-001/002; inspect real contracts, registration scaffolds and local UI-reference sources on branch reference-materials.
- [x] Expand existing ten-step code-generation plan with concrete implementation paths, C01–C05, role/scope matrix, job race/resource limits, UI/slot mapping and verification gates.
- [x] User approval of Part 1: Answer a in `construction/plans/code-generation-plan/u13-implementation-approval-questions.md`.
- [x] Part 2 implementation and unit tests executed after approval; see final handoff below.
- Current U13 checkpoint: code ready for user review; do not change global stage or mark UC acceptance complete. Proposed U06-compatible consumer keeps version/provenance metadata handoff explicit. Independent provider/DB/browser/NFR acceptance remains pending; no commit/push/external mutation.

## U13 implementation in progress — 2026-10-21
- [x] Approval A confirmed; user resumes implementation and explicitly keeps U06 DTO unchanged.
- [x] Steps1–8 code/developer checks: migration resource, immutable domain/JPA persistence, commands/read policy, isolated parser/job/sweeper, U06 consumer/MCP, REST/OpenAPI and frontend routes/slots.
- [x] Step9 verification completed; final frontend321pass and backend focused80pass. Step10 user review remains pending. Full backend first run928 total/851pass/77skip; packaged parser smoke passed.
- Full PostgreSQL migrations/concurrency, real session/audit/Drive/Rabbit integration, independent tester and NFR measurements remain open; these are not claimed by the code checkboxes.


## U13 code ready for review — 2026-10-21
- [x] Execute approved steps1–9 for UC58–62 with local UI reference; U06 DTO retained as requested.
- [x] Backend full928 total/851pass/77skip/0fail/0error; after final review fixes, focused80pass (78U13+2architecture), JAR packaged; parser tested from executable JAR.
- [x] Frontend321pass/61files, typecheck/lint/build pass; standalone route/header smoke and browser manager fixture/mobile QA. Generated Next instruction files removed after backup; no unrelated source or index changes.
- [x] Developer review/handoff and tester matrix recorded in construction/u13-reference-materials/code/code-summary.md and construction/build-and-test/u13-verification.md.
- [ ] User code review/approval (step10). Do not advance global stage or UC/story acceptance automatically. Real PostgreSQL/migration/provider/session/audit/Drive/Rabbit/NFR tests, native PDF browser rendering and U06 version metadata remain pending.
- Local commits created on reference-materials at the user's subsequent request; no push/Jira/owner messaging. UI QA fixture is ignored and never part of production source/build.

Final checkpoint: code available for review; independent acceptance gates retained. SEC-004 configuration mismatch corrected on user request: Next Proxy applies per-request script nonce/strict-dynamic, DENY/frame-ancestors none and required headers on HTML page routes; root request rendering prevents cached/static nonce mismatch. Full frontend323pass/62files, typecheck/lint/production build, HTTP nonce/script/header and browser hydration/U13 navigation/YouTube checks pass. Style attribute exception documented; native PDF renderer and deployed Nginx/HTTPS/provider checks remain open. No global stage/UC acceptance advanced.

## U13 local commit handoff — 2026-10-21

- User requested small commits on reference-materials and explicitly prohibited push. Seven code commits: 14dec5b domain/persistence; 7c6a339 scoped management; d5c5d92 extraction; 0629906 AI/MCP; 94b3f5b REST/OpenAPI; 9ae5458 frontend; 28b9f46 SEC-004 headers/nonces. Final documentation commit includes approved plan, handoff, verification and audit/state.
- No application source edits during commit organization; existing verification evidence retained. Commit authorization does not automatically close step10, UC/story acceptance or the documented integration/NFR gates. No push/merge/deployment performed.

## U10 giai đoạn 4 (AI, MCP, tài liệu tham khảo) và U11 G4 — 2026-10-22

- [x] Code Generation Part 1: plan giai đoạn 4 được người dùng duyệt (H1–H10), nhánh `quiz-ai-mcp` tách từ `staging` `08a6134`.
- [x] Code Generation Part 2: bước 4.0–4.13 xong trên `quiz-ai-mcp` (4.0 commit `cf80f2e`, code chưa commit); xem `construction/u10-quiz-authoring/code/code-summary.md`. `staging` không dựng được DB mới do migration `grading` (U09) cần bảng `assignments` (U07 chưa merge).
- [x] Bước 4.14: người dùng duyệt code giai đoạn 4 ngày 2026-10-22; commit local trên `quiz-ai-mcp`.
