# AI-DLC State Tracking

## U05/U08/U14 — tổng quan lớp sinh viên, 2026-11-11

Thay Thông báo gần đây và thẻ cần làm chia đôi bằng một thẻ Tổng quan bài tập đủ chiều rộng, đồng bộ giảng viên: số Chưa làm/Đang làm/Đã nộp mở đúng tab của lớp; giữ loại bài và hạn nộp, thẻ co theo nội dung. Bài nhóm đã nộp được loại khỏi helper upcoming dùng chung. Thông báo vẫn có trong menu, không thêm lịch/số buổi/điểm nháp. [Kế hoạch](construction/plans/code-generation-plan/2026-11-11-student-class-overview.md), [kiểm tra](construction/build-and-test/2026-11-11-student-class-overview.md).

Full frontend972/972,138 tệp; kiểm cuối7/7, ESLint/TypeScript/build32 trang qua. UI local student2 trong profile tách riêng,1440/390 sáng/tối; số2/1/3 khớp tab Bài tập, menu thông báo còn, không tràn ngang. Không đổi backend/migration hay ghi bài nộp/điểm; chưa test VPS/thiết bị thật. Đã commit riêng `97ebab28` theo yêu cầu tiếp theo; giữ thay đổi bảng điểm trong commit riêng, chưa push/deploy. Security/Resiliency giữ phạm vi đã duyệt, PBT disabled.

## U09 — bảng điểm dạng ma trận, 2026-11-11

Đã triển khai trên `fix/u05-u09-u15`: nút Bảng điểm ở trang Chấm điểm mở ma trận sinh viên × assignment, lọc một bài/toàn bộ, tìm kiếm/phân trang và xuất XLSX/CSV cùng cấu trúc. Điểm0 và điểm riêng thành viên nhóm được giữ, chưa chấm để trống; XLSX dùng ô số và cố định header/cột định danh. [Kế hoạch](construction/plans/code-generation-plan/2026-11-11-gradebook-matrix.md), [kiểm tra/giới hạn](construction/build-and-test/2026-11-11-gradebook-matrix.md).

Frontend970/970, backend full verify2.153/2.153 và guard57 ca qua; test bổ sung cuối Assembly/Exporter/Controller19/19, giao diện cuối19/19; lint/TypeScript/build qua. UI local đã kiểm màn rộng/hẹp, sáng/tối, lọc/tìm và popup xuất. Writer mới tạo XLSX được đọc lại xác nhận kiểu số/freeze/filter; download live vẫn theo cấu trúc cũ vì backend hiện chạy chưa nạp mã mới. Không restart trái lệnh automatic approval review trước đó. Đã commit riêng `7466539b` theo yêu cầu tiếp theo, chưa push/deploy; không sửa điểm hay dữ liệu workbook mẫu. Trước commit chạy lại26 test frontend liên quan và19 test backend export/assembly/controller, đều qua. Security/Resiliency giữ phạm vi đã duyệt, PBT tắt.

## U12 — Sinh viên tự chia nhóm, giảng viên duyệt — Build and Test, 2026-11-11

Tính năng mới mở rộng U12: sinh viên tự lập đề xuất nhóm trong lớp, giảng viên duyệt thì đề xuất thành nhóm chính thức. Brownfield, dùng lại tài liệu reverse engineering hiện hành. [Câu hỏi xác nhận](inception/requirements/2026-11-11-student-self-grouping-questions.md) (A cả 9 câu); [yêu cầu SG-01…09](inception/requirements/2026-11-11-student-self-grouping.md). Nhánh `feat/student-self-grouping` (`--no-track` từ `origin/staging@e78ac7b9`), worktree `../AI-Powered-Learning-Platform-self-grouping`.

- [x] Workspace Detection: brownfield, bỏ qua Reverse Engineering.
- [x] Requirements Analysis: người dùng duyệt 2026-11-11. Security/Resiliency giữ phạm vi, PBT tắt.
- [x] User Stories: [kế hoạch](inception/plans/2026-11-11-student-self-grouping-story-plan.md) A cả 4 câu; US-GRP-007…010, người dùng duyệt 2026-11-11.
- [x] Workflow Planning: [kế hoạch thực hiện](inception/plans/2026-11-11-student-self-grouping-execution-plan.md), người dùng duyệt 2026-11-11. Chạy Functional Design U12, Code Generation, Build and Test; bỏ qua Application Design, Units Generation, NFR Requirements/Design, Infrastructure Design.
- [x] Functional Design U12: [kế hoạch](construction/plans/functional-design-plan/2026-11-11-u12-student-self-grouping-functional-design-plan.md) A cả 9 câu; BR-U12-70…96, F16–F24, người dùng duyệt 2026-11-11.
- [x] Code Generation U12: [kế hoạch 19 bước](construction/plans/code-generation-plan/2026-11-11-u12-student-self-grouping-code-generation-plan.md) xong 19/19, người dùng duyệt 2026-11-11. Backend 54 test liên quan qua (gồm PostgreSQL); frontend 45 test nhóm, tsc, eslint qua. Chưa thử trên trình duyệt, chưa chạy toàn bộ suite.
- [x] Build and Test: [kết quả](construction/build-and-test/2026-11-11-student-self-grouping-results.md). Backend `mvn -B verify` 2.203 test qua; frontend lint, typecheck, 978/978 test, build qua; thử hai vai trò trên stack riêng `selfgroup`. Người dùng duyệt 2026-11-11; đã commit trên nhánh, chưa push.

## UI/UX đợt 2 — kiểm báo cáo ngày 2026-11-10, đã commit, chưa push, 2026-11-11

Nhánh `fix/ui-ux-review-2` từ `origin/staging@353383f2`. Đối chiếu từng mục của báo cáo đánh giá UI/UX với mã sau PR #64: mục 3 (bảng Bài tập dạng thẻ, cột lớp), 4 (thanh đồng hồ dính, `aria-live`, đáp án 44px), 6 (ngăn kéo `inert`, Esc), 8 (toast lỗi), "Practice", liên kết bỏ qua, Đăng xuất trong menu tài khoản, `Menu` phím mũi tên, tiêu đề popup 22px đã sửa trước đó.

- [x] Sidebar: mục đang chọn `bg-primary-soft`, hover token mới `--c-sidebar-hover` (sáng `#f4f4f5`, tối `#202b39`); bỏ bóng nâu; mũi tên mở nhánh 40px trên màn cảm ứng, liên kết cha chừa lề phải
- [x] Ô nhập 16px dưới 640px (quy tắc chung trong `globals.css`, ô OTP chữ to giữ cỡ)
- [x] Lề `main` và 4 thanh đầu trang dính dùng chung `.main-gutter`/`.main-bleed`; đã đo trên trình duyệt: thanh trùng hai mép `main`
- [x] Công tắc tắt dùng `--c-field` (3,33:1), cả `Switch` và bản trong Thông báo
- [x] Trang chấm bài: `window.confirm` thành popup "Bỏ thay đổi"
- [x] Quiz: "Chưa lưu được" màu cảnh báo, popup khi thoát quiz có giới hạn giờ, dải số câu cuộn ngang ở trên câu hỏi khi hẹp hơn `lg`
- [x] Hạn nộp tương đối ("còn 5 giờ", cảnh báo dưới 24 giờ) ở trang Bài tập của sinh viên; thời gian thông báo dạng tương đối; `shared/dateTime.ts` cố định giờ Việt Nam thay 3 chỗ `toLocaleString` theo giờ máy
- [x] `InfoTip` đóng bằng Esc, rê chuột vào được; `Tabs` có Home/End, `aria-controls`/`tabpanel` ở trang lớp và môn; bảng lượt làm quiz mở được bằng bàn phím
- [x] Câu chữ: "Không có … phù hợp.", "…", "Nhập đúng email do trường cấp.", hằng số `LOAD_FAILED_MESSAGE`; trình xem học liệu `100dvh`
- [ ] Chưa làm: `useAuthoringGuard` (nút Back cần trả lời đồng bộ), thang cỡ chữ/bo góc/z-index, `Overlay`/`Textarea` dùng chung, gộp `Pagination`/`PageNav`, gộp 11 hàm ngày giờ, kiểm thử ảnh chụp, lint chặn giá trị lẻ; hành vi ô chọn môn/lớp trên sidebar chờ người dùng quyết định

Kiểm tra: `tsc --noEmit`, `eslint` qua; Vitest 133 tệp, 950 test qua. Trình duyệt (stack Docker local): sidebar hai theme, ô nhập 16px ở 375px, thanh dính trang soạn bài. Chưa thử trên iPhone thật. Không đổi backend, API hay schema; đã commit trên nhánh, chưa push. Security/Resiliency giữ phạm vi đã duyệt, PBT tắt.

## Sửa 22 điểm UI/UX — build và kiểm thử xong, đã commit, chưa push, 2026-11-10

Người dùng yêu cầu sửa toàn bộ 22 điểm của đợt rà soát UI/UX, chọn phương án A cho cả 9 câu hỏi, duyệt kế hoạch 21 bước và duyệt mã. Nhánh `fix/ui-ux-remediation` tạo từ `staging` `062a43f5`. Chỉ sửa `frontend/` và `PRODUCT.md`; không đổi backend, API, schema. [Yêu cầu và quyết định](inception/requirements/2026-11-10-ui-ux-remediation-questions.md); [kế hoạch và điều chỉnh](construction/plans/code-generation-plan/2026-11-10-ui-ux-remediation.md); [tóm tắt thay đổi](construction/ui-ux-remediation/code/summary.md); [kết quả build và kiểm thử](construction/build-and-test/2026-11-10-ui-ux-remediation-results.md).

- [x] Workspace Detection: brownfield, dùng lại tài liệu reverse engineering hiện có.
- [x] Requirements Analysis: 22 yêu cầu, 9 quyết định (đều A).
- [x] Workflow Planning: chỉ chạy Code Generation và Build and Test; các giai đoạn khác bỏ qua.
- [x] Code Generation: 21/21 bước, người dùng đã duyệt.
- [x] Build and Test: TypeScript và ESLint sạch; Vitest 886/886 ca trong 128 file, ba lần liên tiếp; production build thành công; trình duyệt xác nhận màn đăng nhập và trang 404.
- [ ] Operations: giai đoạn giữ chỗ, chờ người dùng xác nhận.

Chưa kiểm tra trên trình duyệt các màn sau đăng nhập (chỉ có kiểm thử tự động trong jsdom). Đã commit trên nhánh theo yêu cầu sau đó của người dùng; chưa push. Security và Resiliency tuân thủ trong phạm vi đã duyệt, các quy tắc khác N/A; PBT tắt.
## Đồng bộ tài liệu với code staging — 2026-11-10

Đối chiếu `staging@062a43f5` với aidlc-docs và sửa các chỗ lệch do commit code chưa kèm tài liệu: U07 sửa đề bằng AI (`REVISE`, `AI_DRAFT_STALE`), U08 bỏ kiểm nộp và trang biên nhận, U09 phần bỏ trống 0 điểm và điều hướng bản nộp nhóm, U10 số câu theo độ khó, U06 model Gemini chọn sẵn, cây điều hướng môn/lớp (U05/U01), log lỗi Admin môn học (U04). Bảng kê source trong current-code-baseline được bổ sung 15 mục, bỏ 2 liên kết hỏng; API inventory khớp 239 mapping. [Báo cáo](construction/build-and-test/2026-11-10-aidlc-code-sync-gaps.md).

- [x] Đối chiếu commit code sau các đợt sync 2026-11-07 và 2026-11-09
- [x] Sửa functional-design và as-built của unit bị lệch
- [x] Cập nhật current-code-baseline, api-inventory, build-and-test-summary
- [x] Kiểm liên kết cục bộ của các file đã sửa
- [x] Đợt 2 sau merge tới `staging@1e77eb79`: PR #60 (đề mẫu của môn, nộp lại không giới hạn, đánh số câu hỏi, popup tạo bài) vào U07, U08, U09, U05; BR-U07-80…86, BR-U08-70…74; api-inventory 241 mapping, baseline thêm 6 mục
- [ ] Inception (FR-027, US-ASM-009, số lượt nộp bài có chấm) còn mô tả hành vi cũ; chờ xác nhận yêu cầu trước khi sửa

Chỉ sửa Markdown; không chạy application test, không đổi code/schema/config, không commit hay deploy. Thay đổi chưa commit ở `AssignmentFormPage` nằm ngoài phạm vi. Security/Resiliency giữ phạm vi đã duyệt, PBT disabled.

## U08 — Thẻ Góp ý AI trong Lịch sử nộp bài, 2026-11-09

Đã triển khai trên `fix/student-ai-grading` và đồng bộ UC 41, FR-030, US-ASM-012 cùng tài liệu U08. Thẻ luôn hiện theo lượt Practice đã nộp, dùng cột nhận xét trên màn hình rộng và chuyển vào đầu nội dung bài làm trên màn hình hẹp. [Kế hoạch và kết quả kiểm tra](construction/plans/code-generation-plan/2026-11-09-student-ai-feedback-card.md).

Đã xác nhận 71 kiểm thử trong phạm vi Assignment Work và trình xem tài liệu, lint, TypeScript trong production build và build. Lượt chạy toàn bộ Vitest bị dừng sau lỗi `ENOENT` ở thư mục tạm của sandbox. Lượt chạy phạm vi liên quan dùng thư mục tạm trong workspace. Build hoàn tất với cảnh báo tải font Google. Chưa xác nhận trên trình duyệt hoặc với nhà cung cấp AI trực tiếp. Security và Resiliency giữ phạm vi đã duyệt. PBT tắt theo cấu hình dự án.

## Integration-test delivery — regressions verified, publication ready, 2026-11-09

Initial commits15400ac2/8d53a1c6 pushed normally. Staging fb20c6bc merged as a55f8426; later staging198b796f merged as65f86a6c. Complete first-merge checks: backend2,028/2,028 plus57-case guard; frontend build/lint/typecheck and800/800 tests. Latest U12 increment:95/95 backend, current frontend build/typecheck/scoped lint and15/15 tests. Keep those scopes separate; no summed or fresh-final-full-run claim.

All168 actual API/proxy/SSE E2E assertions passed against `65f86a6c432c63e318737cef88195ad5ffd30ab1` with local Mailpit OTP/SMTP and isolated services. Only the task stack/frontend stopped; original7 platform container IDs preserved. [Delivery results](construction/build-and-test/2026-11-09-integration-test-delivery-results.md) and [evidence](construction/build-and-test/2026-11-09-integration-test-delivery-evidence.json) record exact snapshots, hashes and earlier resource-sensitive timeouts. No assertion/timeout or production behavior was changed to obtain a pass.

Final evidence is ready for commit/push, then the authorized PR to staging. PR/CI publication state is recorded externally on GitHub and in the final response; no PR merge or deployment is authorized. Existing security/resiliency retained; PBT disabled. The57-case workbook remains unchanged and separately delivered. Prior no-commit/no-push statements below are historical.

## Prioritized integration completion — verified, 2026-11-09

Task8 is complete on integration-test. Eight additional methods extend the composed suite from16 to24. Final sequential backend verify passed2,028/2,028 with zero failures/errors/skips at2026-11-09T11:18:00+07 and packaged the executable JAR. All seven unchanged local latency checks passed; earlier failed attempts remain preserved. Current required guard passed57 mapped report cases/62 executions, with all four negative QA fixtures rejected. Independent review's quota-clock and grading-lock findings were addressed; no deferred findings remain.

The English integration report now contains57 representative cases across15 units:49 original case cells/styles/execution dates and the Cover revision preserved, eight cases appended. Source template/native styles/drawings/widths/hidden groups remain intact. Five affected unit tabs and summaries/trace/header views were visually reviewed; eleven unchanged unit/security sheets compared with the baseline. Reopened formula recalculation passed. Scenario/evidence/results and Task8 ledger contain source hashes, workbook hash, actual boundaries and remaining per-unit exclusions.

Only the composed backend test source changed from the historical1,031-file source snapshot. No production source/schema/dependency/frontend change, live provider call, commit, push, PR or merge. Reports5.3–5.5 remain untouched. External mail is controlled; deployed browser/proxy/provider acceptance and exhaustive requirements coverage remain outside this report. Existing security/resiliency retained; PBT disabled. Historical49-case results below describe the preceding phase.

## Integration tests and English report — design review, 2026-11-08

User selected scope B: representative automated integration coverage across all15 units, delivered in groups, plus an English report preserving `Report-5.2_Integration Test_Template.xlsx` styling. Created `integration-test` from freshly fetched `origin/staging`321f8892 with a clean starting checkout. [Scope answer and design review](inception/requirements/2026-11-08-integration-test-questions.md); [concrete design](inception/application-design/2026-11-08-integration-test-design.md).

- [x] Workspace detection and branch setup.
- [x] Confirm all15-unit scope and inspect initial requirements/test/template structure.
- [x] Write implementation-aware, requirement-grounded design with per-unit coverage targets and existing evidence candidates.
- [x] Independent builder check: no important unanswered planning questions.
- [x] User approval of the concrete design (question2=A), including planning and sequential implementation.
- [x] Detailed implementation plan and per-group execution:16 new real-component integration methods, production source unchanged.
- [x] Fresh full backend verification:2,020 passed, zero failures/errors/skips;49 traced report cases resolve to54 passing executions. Guard and four negative checks passed; independent review found no actionable findings.
- [x] English report authored/recalculated/exported/reopened and all19 sheets visually reviewed;49 cases, accurate execution statistics, traceable methods/requirements and preserved template styling/native parts.

Initial search found24 Testcontainers/composed-context candidates; classification inspected actual collaborators rather than filenames. New composed tests use production owners, disposable PostgreSQL/Redis/Rabbit and current Flyway. Existing focused suites are reused with explicit mocked/persistence-only boundaries. [Plan](construction/plans/code-generation-plan/2026-11-08-integration-test.md), [results/gaps](construction/build-and-test/2026-11-08-integration-test-results.md), [scenario manifest](construction/build-and-test/2026-11-08-integration-test-cases.json) and [source-hashed execution evidence](construction/build-and-test/2026-11-08-integration-test-evidence.json). Final Maven verify completed19:47:22+07 on2026-11-08. The early overlapping-compile baseline was invalidated and is not acceptance evidence. Existing Maven/CI entry point, public contracts, migrations, dependency versions and production persistence choices preserved. Final English workbook has19 sheets and49 Passed Round1 cases; later rounds remain Pending. It was reopened, visually reviewed and checked for formula reactivity, source/template SHA, style/native-part preservation and navigation. Existing security/resiliency scope retained; PBT disabled. Branch remains uncommitted/unpushed; no PR, deployment, live data/provider writes or full-requirements acceptance claim.

## fix/remove-term delivery and staging merge — 2026-11-08

User explicitly authorized scoped commits, push, staging→current-branch merge and E2E. Six scoped commits pushed to origin/fix/remove-term through fc31721b, then origin/staging528cc50e merged at6007c5fb. Only audit history conflicted; all lines from both branches preserved. Test-only integration repairs committed at44b5781d: restore three needed imports removed by staging cleanup and align the staff-email E2E oracle with BR-U14-21/D-U14-09. Verified merge/test integration through44b5781d has also been normally pushed to the same branch. No postmerge production behavior change.

Fresh premerge complete backend2,015/2,015 and frontend805/805 passed. Postmerge frontend affected suites286/286, scoped lint and build31pages passed. Isolated API E2E168/168 (40 U04/U13/U14,16 U02,58 U12,35 U12b,19 U12c) passed with actual Nginx/auth/OTP/worker/Mailpit and separate data. First full postmerge backend2,003/2,004 passed, one load-sensitive U14 p95 assertion368.3049ms exceeded300ms during concurrent E2E; unchanged isolated benchmark then passed120.9493ms. Final complete retry without E2E load **2,004/2,004**, zero failures/errors/skips, BUILD SUCCESS17:21:26+07; announcements p95 **83.9347ms**, required PostgreSQL31/7/10/1 passed. All1,625 source/config hashes match final verification; see [delivery plan and evidence](construction/plans/code-generation-plan/2026-11-08-remove-term-staging-e2e.md).

E2E resources stopped with volumes retained; existing platform stack preserved. API E2E does not close browser/proxy deployment capacity/provider acceptance or all requirements gates. Report drafting remains paused; historical draft contents preserved. Existing security/resiliency retained; PBT disabled.

## U04/U13/U14 expanded clause remediation — 2026-11-08

Continued the user's explicit expanded review request on `fix/remove-term`, using current `aidlc-docs` and the settled reconciliation decisions. [Current clause map, evidence and open acceptance gates](construction/plans/code-generation-plan/2026-11-08-u04-u13-u14-clause-closure.md). Fixed demonstrated missing U13 current-version provenance, U04 null archive-result handling and blocked normal lifecycle transaction, missing module denial audit, swallowed GROUP submission dependency failure, and reminder truncation after500 candidates. Added real owner PostgreSQL/security/JWT/audit composition, Redis token expiry, Rabbit two-hub delivery, parser and local performance tests without new dependencies or a persistence migration.

Fresh full backend verify **2,010 passed**, zero failures/errors/skips; required PostgreSQL31/7/10/1 and migration2/1 passed. Later test-only content/snapshot refinements **6/6**, module transaction/retention refinements **3/3** and final multirow CAS rollback/retry **1/1** passed separately. Fresh affected frontend **233/233**, TypeScript/scoped lint/production build31pages passed. Full backend verification predates the late test-only refinements, and is not represented as a later rerun. Independent review of the production changes and late rollback/retention/CAS oracles found no remaining actionable finding.

Local measured p95/fanout/51MB parser evidence is explicitly scoped. Browser/proxy/provider acceptance, representative concurrent load, complete adversarial parser isolation and universal caller-owned lifecycle deadlines remain open; this is not100% requirements acceptance. Prior minimal-slice sections below remain historical. Report drafting remains paused; staged drafts preserved; no commit/push/deploy/live DB/provider writes. Existing security/resiliency scope retained; PBT disabled.

## U04/U13/U14 confirmed requirement baseline — 2026-11-08

Decisions1–11 are confirmed in [reconciliation questions](inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md). Current requirements synchronized for U04 cascade/class-count/close behavior, fixed PDF/DOCX/PPTX/XLSX MATERIAL formats, XLSX AI extraction, archived staff reference access and staff email default-on with saved preferences preserved. User approved the [concrete test-remediation plan](construction/plans/code-generation-plan/2026-11-08-u04-u13-u14-requirements-tests.md) at question12=a. Approved U04/U13/U14 test additions and archive popup explanation implemented and independently reviewed. Fresh full backend verify1,879/1,879, required PostgreSQL31/7/10/1 plus migration1/2 passed; final fixture-only material query file15/15 passed. Frontend affected feature suite231/231 and final detail-page file13/13 passed, including new0/7 count assertions; TypeScript/scoped lint/build31pages passed. Initial frontend timeouts resolved on unchanged reruns. The plan contains the requirement/assertion map and explicit remaining contract/integration/NFR gates. Historical reports/plans unchanged; report drafting remains paused. Existing branch/user changes preserved; no commit/push/deploy/live writes.

## Class semester removal — implemented 2026-11-07

Implemented D-U04-04 on user-requested `fix/remove-term` from staging c9b7ce96: removed semester from U04/U05 request/response/domain/contracts/JDBC/frontend payloads, fixtures and E2E seeds; synchronized current semester requirements. New Flyway migration `V20261107_2200__remove_class_term.sql` drops only the old class column; applied migrations unchanged. [Decisions](inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md), [implementation plan and evidence](construction/plans/code-generation-plan/2026-11-07-remove-class-term.md).

Fresh backend Maven verify1,846 pass, zero failures/errors/skips; required PostgreSQL guard31+2+10+1 passed. New migration suite2/2 passed, including seeded upgrade/fresh install/replay and JDBC CRUD with retained class/enrollment data. Frontend focused149/149, TypeScript/scoped lint/production build31pages passed; E2E script syntax passed. RED evidence: frontend3 expected failures and backend12 expected failures before removal. Independent review: no remaining actionable findings. No live migration/database write, commit/push/deploy or report drafting; existing staged drafts preserved. This completes only the approved semester slice, not all U04/U13/U14 coverage remediation. Existing Security/Resiliency scope retained; PBT disabled.

## Current documentation/source index — 2026-11-07

Source snapshot `f68a6ec5` on `fix/u05-u09-u15`; documentation synchronized by the user's request. [Source inventory](inception/reverse-engineering/current-code-baseline.md), [API mappings](inception/reverse-engineering/api-inventory.md), [services](inception/application-design/services.md), [contract types](inception/application-design/component-methods.md) and 15 as-built summaries reflect current file/type ownership. Mail/deploy/AI-key/source behavior and required-test guidance were updated from source; [results and verification limits](construction/build-and-test/2026-11-07-aidlc-code-sync-results.md).

No application/config/test changes or fresh application run during this sync. The earlier 1,830-backend/799-frontend evidence predates the staging merge and is not current-HEAD acceptance. Dated sections below are historical records of their original scope, including statements about commit/deployment status at that time. Existing security/resiliency configuration remains; PBT disabled. No commit, push or deployment.

## Continuous deployment to the VPS — 2026-11-06

User-chosen pull model: GitHub-hosted runners cannot reach the VPS, which blocks international inbound traffic, and a self-hosted runner cannot reach the Actions broker. `platform-deploy` (systemd timer, 2 min) asks the GitHub API for the latest successful `ci.yml` push run on `main`. For each new SHA it pulls images, dumps the database (keeps 5), checks out the SHA, sets `IMAGE_TAG`, runs `up -d` and gates on container health plus HTTPS 200. On failure it rolls back automatically and skips that SHA. Manual `rollback`, `status` and `unskip` commands are available. Database migrations are not reversed; the runbook covers dump restore. The RabbitMQ healthcheck timeout was raised: the 7 s ping made it falsely unhealthy, which would block every restart. CI gains a `deploy-script` job and drops the placeholder deploy job. Fake-command tests 11/11 passed, shellcheck clean. Installed and enabled on the VPS; a same-SHA deploy passed in 28 s. [Plan and results](construction/plans/code-generation-plan/2026-11-06-continuous-deployment.md), runbook section 10. Not yet seen: a real deploy from a new `main` commit; no commit.

## Any email domain — 2026-11-06

At the user's request, BR-U01-03 no longer restricts the domain. Admin create, CSV import, OTP requests and the bootstrap Admin accept any well-formed email of at most 254 characters. Uniqueness and normalization are unchanged, and there is still no self-registration. `U01_ALLOWED_EMAIL_DOMAINS` and `EMAIL_DOMAIN_NOT_ALLOWED` were removed. Schema unchanged; `school_email` is kept as the field name. Requirements, US-IAM-001, U01 design docs, code summary, main business flows, E2E README and deploy runbook were synced. Backend 1,776 passed, zero skips, required PostgreSQL suites passed; frontend identity 38 passed, typecheck and lint passed. [Plan and results](construction/plans/code-generation-plan/2026-11-06-any-email-domain.md). No commit or deployment.

## Brevo HTTPS API mail transport — 2026-11-06

Requested because the production VPS provider blocks all outbound SMTP while `api.brevo.com:443` is reachable. `MAIL_TRANSPORT=brevo-api` with `BREVO_API_KEY` selects the new `BrevoApiMailAdapter`. `smtp` stays the default, and Mailpit stays the local default. An unknown value stops startup. The fixed endpoint is used; logs and errors carry only the HTTP status and Brevo error code. Compose, `.env.example` and the deploy runbook have been updated. Fresh full backend run: 1,773 passed, zero skips; required PostgreSQL suites passed. [Plan and results](construction/plans/code-generation-plan/2026-11-06-brevo-api-mail.md). Not yet done: real Brevo call, image build and deployment, commit. Security/resiliency scope retained; PBT disabled.

## Staging pull request delivery scope — 2026-11-06

User requested a PR from fix/u05-u09-u15 to staging, authorizing the needed push. Refreshed refs: staging is an ancestor, the feature branch was synchronized and no open PR existed for it. Reviewed scope includes U05/U09/U15 readability/simplification, U09 JDBC persistence, global AI-DLC JDBC policy and U05 repository names. PR evidence distinguishes the full JDBC 1,830-backend/799-frontend checks from the latest naming-only 200 Academics/Architecture run; no product changes or redundant runtime tests for delivery metadata. Document no-op save/version distinction and remaining JPA owners; create the PR for review without merging/deploying or claiming CI success in advance.

## U05 repository names simplified — 2026-11-06

At the user's screenshot/naming request, removed Jdbc from the three U05 concrete repository class/file names: AcademicsReadRepository, CourseClassRepository and EnrollmentRepository. Updated constructor/method-reference/test callers and the test dependency field, current U05 summary and shared naming conventions; interfaces and NamedParameterJdbcTemplate remain unchanged. No named qualifier references found. Verified all five Java files differ only by the requested identifier replacements, preserving SQL/transactions/API and behavior text. Fresh compilation of 718 main/246 test files and focused Academics/Architecture run passed all 200 cases, zero failures/errors/skips; stale old-name class output absent, whitespace check passed. [Plan and evidence](construction/plans/code-generation-plan/2026-11-06-u05-repository-names.md). Other units, frontend, runtime restart and automatic commit/push outside this naming-only scope. Security/resiliency retained; PBT disabled.

## JDBC local commit scope — 2026-11-06

User requested clear local commits for the verified JDBC work. Delivery grouping: one U09 JDBC code/test/required-DB-guard commit, followed by one project-wide AI-DLC policy/implementation/verification documentation commit. U05 remains JDBC unchanged; U04/U13/U14 remain separately documented JPA migration debt. Reviewed scoped diff and whitespace; 1,572 source/config hashes match the passing 1,830-backend/799-frontend verification, so no redundant runtime rerun. Exclude ignored scratch logs, credentials and build artifacts; retain running local apps. No push, remote write, merge, deployment or Git configuration change authorized by this request.

## JDBC backend/frontend restarted and freshly tested — 2026-11-06

At the user's explicit restart/test request, stopped only verified app PIDs, rebuilt the standard backend JAR and restarted backend :8080 (PID 50084, local,worker) plus frontend :3000 (listener 3184, launcher 43712). Fresh normal-target full Maven verify: 1,830 tests/236 suites, zero failures/errors/skips, executable repackage successful; the earlier thin-JAR limitation is resolved. Required PostgreSQL suites 31 + 2 + 10 + 1 passed. Frontend 799 tests/116 files, full lint/typecheck/production build and final post-restart typecheck passed. Backend health/liveness/readiness UP/200; frontend root/login 200; protected API via frontend and directly 401. Live Flyway validated 32 migrations with none pending. Restored next-env.d.ts/AGENTS.md generated changes; all 1,572 source/config hashes match the pre-operation snapshot, preserving pending JDBC changes. Existing Docker dependencies healthy, Mailpit restarted, old Docker worker retained stopped. Apps left running; no authenticated UI/provider/load acceptance, product-source/schema/dependency edit, commit or push. [Fresh results and limits](construction/build-and-test/2026-11-06-jdbc-restart-test-results.md). Security/resiliency scope retained; PBT disabled.

## U05/U09 JDBC transition and project-wide persistence decision — 2026-11-06

User discarded the previous JPA migration and explicitly chose U05/U09 JDBC-only code plus JDBC as the AI-DLC persistence standard for all units. Verified a clean checkout before this task; prior discarded changes were not restored. U05 remains JDBC; U09 evaluation/proposal adapters now use NamedParameterJdbcTemplate with plain JSON row mapping; visibility stays JdbcTemplate. Public interfaces/API/schema/Flyway unchanged; row/advisory locks, explicit version/CAS and caller rollback protected. New scoped architecture rule rejects production JPA/Hibernate in U05/U09. Baseline 209 behavior/other-rule cases passed with JDBC rule RED; the same 210 passed after migration; expanded focused run 446 passed including nine full-migration PostgreSQL cases and shared JpaTransactionManager rollback. Final isolated full Maven verify passed: 1,830 tests/236 suites, zero failures/errors/skips, executable JAR packaged; required PostgreSQL guard 31 + 2 + 10 + 1 passed. Independent review complete: U04 @Version documentation contradiction fixed and explicit no-op save/version characterization added. Standard repackage was blocked by the running JAR; executable artifact is under .tmp/jdbc-transition-20261106/target, and the normal backend/target JAR remains thin until rebuilt when unlocked. 1,008 source/config hashes matched final verification, no new broken documentation links. [Plan](construction/plans/code-generation-plan/2026-11-06-u05-u09-jdbc.md), [policy and remaining unit gaps](construction/persistence-conventions.md), [evidence](construction/build-and-test/2026-11-06-u05-u09-jdbc-results.md). U04/U13/U14 still JPA pending separate code migrations; global JPA infrastructure retained for them. No frontend/source changes outside this scope, commit/push or live restart/migration. Security/resiliency retained; PBT disabled.

## Local commit delivery — 2026-11-06

User requested one local commit of the verified U05/U09/U15 formatting, behavior-preserving simplification, characterization tests and AI-DLC documentation; explicitly **no push**. Source/config hashes match the successful backend 1,822 and frontend 799 test/lint/typecheck/build verification. This delivery authorization supersedes the earlier no-commit scope; no source change, test rerun, remote write, merge or deployment is included.

## Local backend/frontend restart and tests — 2026-11-06

At the user's request, reran backend verify: **1,822 passed, zero failures/errors/skips**, JAR packaged; required PostgreSQL guard **31 + 2 passed**. Frontend: **799 passed / 116 files**, full lint/typecheck/production build passed; generated config restored from snapshots and typechecked again. Started backend on :8080 with local,worker and frontend dev Webpack on :3000, restarted existing Mailpit and retained the stopped old Docker worker. HTTP health for both services UP/200, login 200 and unauthenticated protected API via frontend 401. Source/config hashes unchanged. [Evidence and limits](construction/build-and-test/2026-11-06-restart-test-results.md). Processes left running; no dependency/source edit, new live migration, authenticated UI mutation, commit/push or deployment. Existing security/resiliency scope retained; PBT disabled.

## U05/U09/U15 simplification completed — 2026-11-06

Implemented the approved [plan](construction/plans/code-generation-plan/2026-11-06-u05-u09-u15-simplification.md) on the existing formatted checkout: inline U05's single-use mutation helper, share U09's identical batch-size guards and consolidate U15 source-to-section mapping. Exactly seven production/three test files changed versus the pre-refactor snapshot; frontend, contracts and config unchanged. Added 18 characterization cases green before production edits; focused U05/U09/U15 runs pass before and after. Independent read-only review found no actionable findings. Final backend verify: **1,822 passed, zero failures/errors/skips**, executable JAR built; required real PostgreSQL guard **31 + 2 passed**. Initial full attempt failed because Docker Desktop was stopped; started it and reran successfully without source changes. [Evidence and limits](construction/build-and-test/2026-11-06-u05-u09-u15-simplification-results.md). No commit/push/PR, live migration, app restart or frontend/manual UI/provider rerun. Security/resiliency scope retained; PBT disabled.

## U05/U09/U15 simplification plan — 2026-11-06

Created a [scoped refactor plan](construction/plans/code-generation-plan/2026-11-06-u05-u09-u15-simplification.md) at the user's planning-only request. Source review identified one single-caller U05 mutation helper, three identical U09 batch-size guards, and two identical U15 source-to-section mappings. Plan preserves API/DTOs, transaction/lock/error order, audit/events and dashboard deadlines/cancellation; protects all three changes with existing tests plus characterization only where needed. The prior 71-file formatting work remains the baseline. Implementation and fresh runtime tests are pending review; no source code edited for this plan. Security/resiliency scope retained; PBT remains disabled.

## U05/U09/U15 Java readability formatting — 2026-11-06

Completed the requested formatting in `academics`, `grading`, `admindashboard`: 71 Java files changed, all 85 checked; inline returns 37 to zero. Preserved exact source tokens/comments/literals and existing wildcard imports, with CRLF/LF normalization only for Git baseline comparison. Formatter second run changes zero files, whitespace diff check passes, fresh isolated JDK 17 compilation succeeds for 720 main + 246 test source files. Shared conventions now explicitly separate method bodies from declarations and wrap long calls. [Plan](construction/plans/code-generation-plan/2026-11-06-u05-u09-u15-format.md), [evidence and memory limits](construction/build-and-test/2026-11-06-u05-u09-u15-format-results.md). Frontend/runtime tests not rerun; no behavior change, commit/push, restart or migration. Existing security/resiliency scope unchanged.

## U05/U09/U15 staging PR and CI delivery — 2026-11-06

User authorized publishing the current `fix/u05-u09-u15` branch as a PR into `staging` and checking CI before any PR merge. Initial remote head `b466400b` matches the clean local checkout; GitHub comparison is ahead by three commits, behind zero, and no open PR exists for this head/base. Reuse the unchanged source's [verified merge results](construction/build-and-test/2026-11-06-merge-staging-u05-u09-u15.md): backend 1,804 / frontend 799 passing, lint/typecheck/build passing and required real PostgreSQL guard passing. Commit only this delivery record before publishing; no new source changes or local test rerun needed. GitHub PR creation triggers the existing backend/frontend CI jobs; check the final PR head and report its live results. Images/deploy do not run on this PR. No PR merge, auto-merge or deployment authorized; existing security/resiliency scope retained and PBT disabled.

## Merge staging into U05/U09/U15 — 2026-11-06

Resolved the user's existing local merge of `staging` at `b91413ca` into `fix/u05-u09-u15` at `2440cbe4`. Six conflicts resolved by retaining staging behavior and the branch's convention names; all incoming staged changes preserved. Fixed the directly related inherited `@NotNull source` mismatch after reproducing HTTP 400 for single/batch automatic AI requests; added seven HTTP cases and synchronized grading OpenAPI. Independent reviews found no remaining introduced or follow-up issue. Final backend verify **1,804 passed, zero failures/errors/skips**, required PostgreSQL guard **31 + 2 passed**, JAR built. Frontend **799 passed / 116 files**, lint/typecheck/production build passed; generated configuration restored and typechecked again. Deliver through the authorized local merge commit; [plan, evidence and memory constraints](construction/build-and-test/2026-11-06-merge-staging-u05-u09-u15.md). No push, live migration, application restart or deployment; manual UI/provider acceptance not repeated. Existing security/resiliency scope retained; PBT disabled.

## Wildcard import preference — 2026-11-05

Delivery follow-up: user authorized a descriptive local commit of the convention refactor and compact-import follow-up on the current branch. No push requested; this supersedes the prior no-commit scope. Reuse the recorded full verification and subsequent import-only compilation; no further source changes.

User requested compact imports after the convention refactor. The canonical convention now permits Java wildcard/static wildcard imports, including contract types; keep explicit imports when needed to disambiguate names. Restored original import blocks in 71 changed U05/U09/U15 Java files (883 fewer import statements), retaining all naming changes and all bytes outside those import blocks. Backend source and all 244 test source files compile successfully (`mvn -DskipTests test-compile`); full runtime suites were not repeated for this import-only follow-up. The earlier full test/build results below describe the preceding refactor state. [Follow-up evidence](construction/build-and-test/2026-11-05-u05-u09-u15-conventions-results.md#wildcard-import-follow-up--2026-11-05). No behavior, public contract, frontend, schema, dependency, security/resiliency scope, commit or push change; PBT remains disabled.

## U05/U09/U15 shared convention refactor — 2026-11-05

Implemented the [convention plan](construction/plans/code-generation-plan/2026-11-05-u05-u09-u15-conventions.md) from `3bfc2fb8` in the existing `fix/u05-u09-u15` checkout. Concrete dependencies identify their service/repository roles; internal actions use verb-object names, predicates/guards have appropriate names, fixed constants use UPPER_SNAKE_CASE, four React modules use descriptive PascalCase and scoped Java wildcard imports are expanded. All affected callers/imports/mocks updated. Preserve public contracts, HTTP/JSON/DB/event/MCP identifiers, hook return keys, authorization, transactions, grade locks/resubmission and rendering. JSON regression protects against new bean fields. Independent review: no Critical/Important defect; its minor naming correction applied. Final backend **1,783 passed, zero failures/errors/skips**, required PostgreSQL guard **31 + 2 passed**, JAR built; frontend **776 passed**, full lint/typecheck/production build passed. Generated configuration restored and typecheck passed again. [Evidence, compatibility exceptions and memory-constrained verification](construction/build-and-test/2026-11-05-u05-u09-u15-conventions-results.md). No commit/push, migration, live grade write, restart or deployment; manual browser/provider acceptance not repeated. Existing security/resiliency scope retained; PBT disabled. Build/test stage complete; no operations change requested.

## Shared code conventions — 2026-11-05

- [x] Review existing naming guidance and approved proposal; scope is documentation for all units, not a U09 code rename.
- [x] Publish [shared code conventions](construction/code-conventions.md) covering Java/TypeScript names, service dependencies, action methods, files, framework/contract exceptions and refactor verification.
- [x] Link README and CLAUDE.md to the canonical convention document.
- [x] Verify documentation references, relative migration link, Markdown fences and whitespace diff. Runtime/build/tests are not required for this documentation-only change. No source rename, new tooling or contract change; implementation/design stages skipped as unnecessary. Existing extension scope retained; runtime security/resiliency checks N/A.

## U05/U09/U15 follow-up implementation — 2026-11-05

Delivery follow-up: user requested an English commit, push of the existing feature branch and PR into `staging`, with CI verification. Current PR head and remote check results are tracked on GitHub and reported with delivery; no merge/deployment requested. This supersedes the implementation-only no-commit scope below.

Implemented the [follow-up plan](construction/plans/code-generation-plan/2026-11-05-u05-u09-u15-followup.md) in existing `fix/u05-u09-u15`: independent/bounded U15 loading and cancellation, one-student gradebook lookup, typed submission adapter, rubric scoring in the model, and U05 presentation helper/import cleanup. Independent review found one stale-loading P2; regression reproduced and fixed, no other confirmed source finding. Fresh backend **1,782 passed/zero skipped**, required PostgreSQL guard passed; frontend **776 passed**, full lint/TypeScript/production build passed. [Evidence, environment setup and limits](construction/build-and-test/2026-11-05-u05-u09-u15-followup-results.md). Manual Orca browser acceptance deferred because `orca tab list` could not resolve the executable. Public contracts, grade locks/resubmission, authorization, unit folders and DB schema unchanged. No commit/push/deployment. Existing security/resiliency scope retained; PBT disabled.

## U05/U09/U15 audit remediation — 2026-11-04

Follow-up: on user request, started the existing Frontend :3000 and Backend :8080 Orca terminals; login returned HTTP 200 and backend health UP. Local backend includes its worker; the old Docker worker was stopped to avoid duplicate task consumers. User subsequently requested a descriptive local commit in English; no push/merge/deployment requested. Keep the verified cleanup as one atomic commit because navigation/scoring imports and workspace regression changes share files.

Implemented the [cleanup plan](construction/plans/code-generation-plan/2026-11-04-u05-u09-u15-cleanup.md) on existing `fix/u05-u09-u15` from `45baad9b`: preserve grading navigation filters, guard save-next when the current evaluation leaves its queue, batch/index gradebook reads and load export sources once, explain conflicts by business code, move pure scoring helpers into the feature model, and remove one duplicated U05 permission check. U15 source unchanged; regression included. Full backend 1,771 passed/zero skipped with required PostgreSQL suites, frontend 760 passed, final lint/TypeScript/production build passed. Independent review rechecked its one corrected P2 and found no remaining actionable introduced finding. [Evidence and limits](construction/build-and-test/2026-11-04-u05-u09-u15-cleanup-results.md). No DB migration, live grade write, application restart, commit or push. Manual browser acceptance deferred because `orca` is unavailable; automated UI/export tests passed. Existing security/resiliency scope retained; PBT disabled.

## U04/U13/U14 fixes and formatting — 2026-11-02

U13 accepts single-video YouTube links with playlist context and stores the canonical URL. U14 exposes role-specific settings for every role; new staff email is opt-in and the Admin backup alert remains required. U04/U13/U14 source/tests were formatted with syntax-equivalence checks, in separate commits from the fixes. The user subsequently authorized merging staging, verification, pushing the feature branch and creating [PR #44 into staging](https://github.com/hygef-v4/AI-Powered-Learning-Platform/pull/44). Merge `1ef38be7` preserves staging's newer YouTube/UI behavior. Fresh local backend: 1,764 passed, zero skipped, including required PostgreSQL suites (31 + 2). Frontend: 743 passed, lint/typecheck/production build passed. Isolated API E2E: 40/40 passed after worker readiness; its Docker project and volumes were removed. The U04 mutation/readback test now awaits its async React updates. [Plan](construction/plans/code-generation-plan/2026-11-02-u04-u13-u14.md), [verification and limits](construction/build-and-test/2026-11-02-u04-u13-u14-results.md), [live PR checks](https://github.com/hygef-v4/AI-Powered-Learning-Platform/pull/44/checks). CI is checked against the latest PR head before handoff. No PR merge or deployment. Existing security/resiliency scope retained; PBT disabled. Unrelated report DOCX retained outside the commits.

## Dedicated MCP documentation — 2026-11-02

Implemented the requested /ai/mcp-guide and compact Agent MCP help link, available even on credential errors. Four client configurations/English copy prompt reused, SSR origin hydration fixed, 29 provider tool names covered with role/write effects, usage/review/apply/rubric limits and troubleshooting. U06 synced. Fresh AI/MCP tests 39 pass after fixing the single duplicate-copy failure in a full 727-case run (726 other cases passed); TypeScript/focused ESLint/final Next 16.4.3 production build passed. Desktop/mobile light/dark screenshots inspected with zero page overflow; independent review no Critical/Important findings. Synced existing lockfile dependencies and restarted frontend only; generated tsconfig/AGENTS changes restored. No backend/DB changes, live MCP business write, commit or push. [Plan](construction/plans/code-generation-plan/2026-11-02-mcp-documentation-page.md), [evidence and limits](construction/build-and-test/2026-11-02-mcp-documentation-results.md). Security/resiliency scope retained; PBT disabled.

## Finalized grade lock — 2026-11-02

Latest user decision: "tôi nghĩ để cái cũ đi nộp lại thì bỏ điểm cũ" supersedes retaining prior official grading rounds. Keep manual/AI/member editing locked after FINALIZED/PUBLISHED, but resubmission invalidates previous grades: individual SUPERSEDED, group reset existing root/member to PENDING. Removed the unexecuted group-round migration and snapshot/current-flag changes; no DB schema/index change. Changed-receipt reload resets old local marks/proposal selection. [Decision](inception/requirements/2026-11-02-locked-grades.md), [plan](construction/plans/code-generation-plan/2026-11-02-locked-grades.md), [results](construction/build-and-test/2026-11-02-locked-grades-results.md). Fresh full backend 1,741 pass/zero failures/errors/skips, required PostgreSQL suites pass; grading frontend 118 pass, TypeScript/scoped lint/production build pass. Reviewer rechecked receipt-reload fix: no remaining Critical/Important findings. Prior 1,743/710 evidence applies to the superseded implementation only. No live migration/restart/commit/push. Security/resiliency scope unchanged; PBT disabled.

## Staging check fixes — 2026-11-02

Findings of the `staging` check, logged as KAN-178 to KAN-182 and fixed on `fix/staging-check-2026-11-02`: `next` 16.4.3; `echarts` and `uuid` overrides under `pptx-preview`; class quiz sidebar without `classId`; audit labels; leftover cleanup. Frontend `npm ci`, lint, typecheck, 700 tests and build passed; `npm audit --omit=dev` found 0 vulnerabilities. Backend code unchanged. Charts with no explicit series color show no bars in `pptx-preview`, with `echarts` 5 and 6 alike. Not pushed or deployed.

## U05/U09/U15 defect remediation — 2026-11-02

Implemented KAN-153–157 on `fix/u05-u09-u15` from clean staging `86b16d92`: preserve partial points/adjustments and AI feedback/review gates; lock manual/restore enrollment targets before subjects; support authorized SUBJECT grading context; restore backup operational metadata and inline paginated history. [Plan](construction/plans/code-generation-plan/2026-11-02-u05-u09-u15-defects.md), [verification and limits](construction/build-and-test/2026-11-02-u05-u09-u15-defects-results.md). Backend 411 distinct tests/zero skips with real PostgreSQL, frontend 189/189, typecheck/focused lint/final production build passed. Full lint has one unrelated existing warning. Fresh reviewer rechecked the corrected legacy metadata issue, no remaining Critical/Important findings. Actual admin dashboard checked; no live grade write, backend restart, broad role/mobile/load acceptance or full-project rerun. Security/resiliency scope retained; PBT disabled. User requested a local commit after verification; no push/merge/deployment.

## Quiz rework — 2026-11-01

User requested a roadmap and implementation of all seven Quiz items with documentation sync. Recommended options were first applied as defaults; on 2026-11-02 the user decided Question 2 option B and kept the other answers. Implemented on `feat/quiz-rework-2026-11-01`: removed Quiz REVIEWED and quiz material attachment; generated papers by tag rules; duplicate control; teacher proposals with Subject Manager approval; leave signal flags and a configurable suggested penalty that only the teacher applies or dismisses per attempt; one question per page in results. Also rendered the never-mounted attempts slot found during browser testing. [Roadmap](construction/plans/code-generation-plan/2026-11-01-quiz-rework.md), [results and limits](construction/build-and-test/2026-11-01-quiz-rework-results.md), [assessment](inception/requirements/2026-11-01-quiz-feedback-assessment.md), [questions](inception/requirements/2026-11-01-quiz-rework-questions.md). Backend clean verify 1,712 passed, zero skipped, required suites passed; frontend 677 passed, typecheck and build passed, lint zero errors. API end-to-end 56 checks and the penalty browser flow passed on an isolated, since removed Docker stack. BF-06 PNG not redrawn; real mobile and load not measured. Security/resiliency scope retained; PBT disabled. Merged into `staging` through PR #36; not deployed. End-to-end on the merged code passed, using a fake school AI key on the test database for U06.

## U06/U12 review feedback — 2026-11-02

Triage of the 2026-11-02 review feedback for the units Võ Minh Tài owns (U02, U06, U12); Quiz, Assignment, Grading and materials items belong to U07–U11/U13 owners and were not changed. Implemented as additions outside the frozen business rules (FB-U06-01, FB-U12-01..04 in `construction/plans/code-generation-plan/2026-11-02-feedback-additions.md`): U06 default AI source; U12 leader finalize with a responsible member, section history with restore, DOCX/Draw.io import mapped by section title without AI, peer review; image upload per BR-U12-35. Take-over was added then removed (conflicts with BR-U12-33); the frozen `business-rules.md` files were restored; user guide `docs/guides/user-guide-audit-ai-group.md`. Peer scores are informational; weighting into the final grade (document/peer/presentation) needs U07/U09. Bilingual UI not started (cross-unit decision). [Plan](construction/plans/code-generation-plan/2026-11-02-u06-u12-feedback.md), code summaries U06/U12. Branch `feat/u06-u12-feedback-2026-11-02`.

## Server-managed MCP workflows — 2026-11-01

Implemented full workflow delivery via read_agent_workflow (AUTHOR_ASSIGNMENT/GRADE_WITH_RUBRIC), role-filtered READ access and deterministic content versions. Full source lives in backend classpath resources; local skills are optional wrappers, no user download required. Capabilities/requirements guide discovery; QUIZ/BANK remain separate. Current U06/U07/U09 rules and guides synchronized. [Requirements](inception/requirements/2026-11-01-server-agent-workflows.md), [plan](construction/plans/code-generation-plan/2026-11-01-server-agent-workflows.md), [evidence/limits](construction/build-and-test/2026-11-01-server-agent-workflows-results.md). Backend focused/architecture: 39 tests passed, zero skipped; hook checks 14 passed; package and required jar resource checks passed. Existing MCP capability call advertises new tool; direct authenticated RPC fetched both complete workflows without local skills. Backend reloaded; clients with stale tool metadata need refresh/new session. No provider call, assignment/grade write, private config installation, commit or push.

## External-agent workflow — 2026-11-01

Implemented the approved agent-first flow: conversation/clarifications in the external agent, exact UI review links, contiguous current-context read enforcement in Redis before MCP grading writes, and optional skill guidance available through capabilities/requirements. No mandatory platform initiation form or session table. Current U06/U07/U09 business rules synchronized. [Requirements](inception/requirements/2026-11-01-external-agent-workflow.md), [plan](construction/plans/code-generation-plan/2026-11-01-external-agent-workflow.md), [results/limits](construction/build-and-test/2026-11-01-external-agent-workflow-results.md). Backend affected/architecture: 67 passed, zero skipped; frontend affected: 38 passed; build/typecheck passed; lint zero errors/two existing warnings. Real Redis script and existing older proposal link checked through Orca; backend restarted, health UP. No real model quality or all-client acceptance claim; no global install, commit or push.

## MCP quality remediation — 2026-11-01

Latest setup scope: only Codex, Claude Code, OpenCode and Antigravity; user requested removing Gemini CLI. Codex is now the default in both guide and token dialog. Gemini API provider unchanged. Focused 18 tests and typecheck passed; no external config removal.

Setup follow-up: added copyable English agent setup prompt and preview in the shared guide/token dialog, preserving client selection and other servers. Token is included only in the explicit token dialog, with disclosure. Focused 18 tests, typecheck, targeted lint and production build passed; Orca guide checked. No real client setup or secret copy performed.

Implemented strict complete AI proposal validation with legacy replay compatibility, shared API/MCP assignment context and policy, metadata-only discovery, server-sourced rubric limit warnings, four-client application/file-based setup and two repository-local skills with optional Gemini hook adapters. User requested no terminal setup instructions; guide follows that preference. [Plan](construction/plans/code-generation-plan/2026-11-01-mcp-quality-remediation.md), [results and limits](construction/build-and-test/2026-11-01-mcp-quality-remediation-results.md). Backend 1,630 runtime cases plus 2 separately rerun architecture cases passed; frontend full 644 plus final targeted setup regressions passed. No global installation, provider call, new token, commit or push. Actual teacher-scored quality, authenticated acceptance in all external clients and load latency remain unmeasured. Existing security/resiliency scope retained; PBT disabled.

## Back navigation and page selections — 2026-11-01

Implemented the user's request for Back to restore the actual previous page/tab. Shared history-aware links retain fallback destinations and navigation guards; page tabs and relevant selections/filters live in URL parameters. Next native-history integration verified after correcting the internal-state bypass. Full frontend: 641 tests/96 files passed; build/typecheck passed; lint zero errors/two existing warnings. Orca verified grading → submissions → Back restores tab/student/filter, and same-page tab Back/Forward restores the visible tab. [Plan](construction/plans/code-generation-plan/2026-11-01-back-navigation-state.md), [results/limits](construction/build-and-test/2026-11-01-back-navigation-state-results.md). No backend/data changes, commit or push.

## Neutral white palette follow-up — 2026-11-01

User found the Slate Blue light surfaces cold and requested a white background matching the supplied reference. Light canvas is now #FAFAFA; sidebar/topbar/cards white; text, borders, sunken surfaces and shadows neutral gray. Blue remains the control/selection accent; dark palette unchanged from the preceding trial. PRODUCT.md updated. Fourteen representative contrast pairs passed (minimum 4.75:1); Orca confirmed the actual canvas/sidebar colors and no horizontal overflow, and the rendered page was inspected. CSS-only follow-up; no new tests or behavior changes. No commit/push.

## Slate Blue palette trial — 2026-11-01

User requested trying Slate Blue after reporting discomfort with the warm theme. Shared light tokens now use cool neutral surfaces, white cards and muted blue accents; dark tokens use slate surfaces and pale blue accents. Removed the oversized shell watermark, softened shadows and aligned the wordmark badge/login seal with the theme. Small brush logo brand colors retained. PRODUCT.md records this as a trial. Production build/typecheck, two shared suites (9 tests) and targeted ESLint passed. Fourteen representative palette contrast pairs passed (minimum 4.67:1); Orca inspected light/dark and confirmed no horizontal overflow at 390 px. No backend/behavior changes, new dependencies, commit or push. Full visual/role coverage not rerun.

## Gradebook and toolbar bug fixes — 2026-11-01

Implemented: historical rubric object envelopes now read alongside current arrays without rewriting stored data; editor tools have visible enabled hover/focus feedback and pointer cursors; assignment header dropdown stacks above the toolbar. [Plan](construction/plans/code-generation-plan/2026-11-01-gradebook-toolbar-fixes.md) and [results/limits](construction/build-and-test/2026-11-01-gradebook-toolbar-fixes-results.md). Fresh backend assignment/grading: 175 passed/zero skipped; frontend focused: 77 passed; typecheck/targeted lint/build passed. Backend restarted; same gradebook API returns 200, and Orca confirmed grading/progress data and menu visibility. Existing pending changes preserved; no persisted data rewrite, commit or push.

## Assignment distribution visibility — 2026-11-01

Implemented: anonymous grade-distribution visibility moved from class management to the assigned teacher's assignment settings, separately for each class. Defaults off; existing score publication and privacy guards remain. [Execution plan](construction/plans/code-generation-plan/2026-11-01-assignment-distribution-visibility.md) and [fresh verification/limits](construction/build-and-test/2026-11-01-assignment-distribution-visibility-results.md). Backend: 174 distinct focused tests passed, including actual PostgreSQL races and architecture. Frontend: 615 tests passed; build/typecheck passed; lint zero errors/two existing warnings. Local backend restarted with additive migration; Orca verified saving and persistence, leaving the test preference off. Pending scoped UI changes preserved. Security/resiliency scope retained; PBT disabled. No commit or push.

## Scoped workspace UI — 2026-11-01

Grading follow-up: blank answer space grows to fit the corresponding rubric when desktop side comments are taller. Resize/font/content updates recalculate it; mobile removes the imposed min-height. Focused tests and lint passed; Orca measured matching desktop section/card heights.

Follow-up: widen the shared assignment authoring/grading workspaces as well; document body text is 16 px with 28 px line height, and wide paper uses smaller side padding. Grading now identifies actual students from all pages of the existing submissions API and offers searchable student navigation while preserving unsaved-work confirmation. Related tests: 155/155 pass; build/typecheck and focused ESLint pass. Orca confirmed the student name and search menu at 390 px, and the author paper expands to 950 px plus a 300 px rubric rail at 1600 px, with 16 px body text and no page overflow. No backend changes, commit or push.

After discarding the previous remediation, the user requested only a wider student answer workspace and inline AI grading review. Changes are limited to an opt-in full-width editor and student shell width, plus replacing the grading AI dialog with a page section. Verified: 49 focused tests pass; production build/typecheck and focused ESLint pass. Orca measured 1110 px paper at 1440 px viewport, no student page overflow at 390 px, and inline AI review with zero dialogs at 603 px. No provider calls, backend/data changes, commit or push.

## Font trial — 2026-11-01

At the user's request, Plus Jakarta Sans now replaces Be Vietnam Pro in the frontend as a reversible trial. Six official self-hosted WOFF2 subsets and their OFL license were added; unused Be Vietnam Pro assets were removed. Orca verified Vietnamese normal/bold/italic loading and no document overflow at 390/1280 px in both themes. Frontend production build and lint passed; two existing lint warnings remain. The user subsequently requested a commit; no push was requested.

## Development UI follow-up — 2026-11-01

Disabled the Next.js on-screen development indicator reported on the mobile assignment page. Orca no longer lists the Dev Tools controls; production build passed. Plus Jakarta Sans was researched as an alternative typeface, with the existing user-confirmed Be Vietnam Pro choice retained pending selection.

## UI remediation — 2026-11-01

User requested a plan and implementation for observed UI defects. [Execution plan](inception/plans/2026-11-01-ui-remediation.md) tracks the fixes and checks. Scope is frontend presentation based on the existing visual audit and four screenshots; no backend, schema, or AI provider changes.

Completed: 599 frontend tests pass; lint 0 errors/2 pre-existing warnings; typecheck and production build pass. Orca confirmed dropdown layering, no document-level horizontal scroll at 390 px in the inspected AI tabs, and the add-key dialog fits the 390 × 844 px viewport. Broader visual regression across every role and task-running AI states remains unchecked.

After the dark-mode select report, added theme foreground/background to native select options. Fresh production build and lint passed; the full test suite passed 599/599 with four workers. The default worker configuration timed out once on the subject detail mutation test in two runs; that test passed alone. Orca measured the corrected option CSS colors but cannot capture the native popup surface.

## Current source/documentation baseline — 2026-10-30

- Project origin: Greenfield, started 2026-09-12; current task: package/file moves only, per the latest user clarification; original implementations retained.
- Current source: branch `aidlc-docs`; package/file relocation included in this commit, after documentation commit `11df6307`. Code baseline `5a76e428`; 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Earlier snapshots below remain historical evidence.
- Current scoped stage: move-only scope correction and fresh local Build and Test complete; current Construction documentation synchronized. Earlier remediation/doc sync preserved. Student MCP remains UC41; Admin MCP denied; no deployment. Work executed directly without subagents.
- Source-observed means code exists; full application acceptance, fresh CI/runtime/provider/security/performance/restore effectiveness remain pending.
- [Execution plan](inception/plans/2026-10-30-aidlc-doc-sync-plan.md), [traceability](inception/reverse-engineering/doc-code-traceability.md), [decision register](inception/reverse-engineering/decision-register.md).

## Unit Progress (current source snapshot)

| Unit | Current responsibility | Source | Fresh acceptance |
|---|---|---|---|
| U01 | Account & Access | [source-observed](construction/u01-account-and-access/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U02 | Audit | [source-observed](construction/u02-audit-job-and-outbox/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U03 | Job and Settings | [source-observed](construction/u03-job-and-settings/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U04 | Subject Administration | [source-observed](construction/u04-subject-administration/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U05 | Class & Enrollment | [source-observed](construction/u05-class-enrollment/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U06 | AI Connections | [source-observed](construction/u06-ai-connections/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U07 | Assignment Authoring | [source-observed](construction/u07-assignment-authoring/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U08 | Assignment Work | [source-observed](construction/u08-assignment-work/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U09 | Assignment Grading | [source-observed](construction/u09-assignment-grading/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U10 | Quiz Authoring | [source-observed](construction/u10-quiz-authoring/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U11 | Quiz Practice | [source-observed](construction/u11-quiz-practice/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U12 | Group Assignment | [source-observed](construction/u12-group-assignment/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U13 | Reference Materials | [source-observed](construction/u13-reference-materials/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U14 | Announcements & Notifications | [source-observed](construction/u14-announcements-and-notifications/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U15 | Admin Dashboard | [source-observed](construction/u15-admin-dashboard/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |

## Extension Configuration

| Extension | Enabled | Decided At |
|---|---|---|
| Security Baseline | Yes | Requirements Analysis; scope retained |
| Resiliency Baseline | Yes | Requirements Analysis; scope retained |
| Property-Based Testing | No | Requirements Analysis; unchanged |

Security scope: SECURITY-03/04/05/08/09/12/15. Resiliency scope: RESILIENCY-04/06/10. Other controls N/A outside approved project scope. Automated authorization/privacy/resiliency regressions passed within the approved remediation scope; this does not certify every runtime control. Key-reveal design delta remains explicit.

## Historical lifecycle and approvals

[Full pre-sync state, approvals, dated test counts and pending issues](history/aidlc-state-before-2026-10-30-sync.md). This archive preserves the previous content including date-specific implementation notes; stale unit names/statuses there are historical. Audit remains append-only in [audit.md](audit.md). No historical approval is converted into fresh full-UC acceptance.

## Documentation verification

- [x] Six synchronization tasks complete; code-derived deltas D01–D14 addressed, additional D15 key reveal recorded.
- [x] Current Markdown links, OpenAPI parse/local references/operation IDs/path parameters, draw.io XML/page consistency, 12 refreshed PNG exports and visual inspection.
- [x] Independent read-only review findings corrected; source application/config/DB unchanged.
- [ ] Fresh application/runtime/provider/security/performance/restore acceptance remains pending; no such success claimed.

[Sync report](construction/build-and-test/2026-10-30-documentation-sync-report.md).

## Rule/use-case/test audit — 2026-10-30

- [x] Read assertions/fixtures and code for all15units, with per-UC evidence/gap rows in three appendices.
- [x] Fresh backend1372pass/99skip; frontend597pass/94files after restoring locked dependencies. No source/manifest/lockfile changes.
- [x] Subsequent remediation ran all backend cases: 1,611 passed/0 skipped; required PostgreSQL guard passed. The earlier 1,372/99 audit run remains historical.
- [x] Approved U03 P1 fixes, false-positive fixtures, scoped missing assertions, current BR/UC drift and Student/Admin MCP correction implemented and verified. Broader acceptance gaps remain explicit in the remediation report.

[Consolidated audit](construction/build-and-test/2026-10-30-unit-test-business-rule-audit.md). Passing suites do not certify full business-rule/UC acceptance.

## Remediation closure — 2026-10-30

[Results and practical limits](construction/build-and-test/2026-10-30-remediation-student-mcp-results.md). Backend 1,611 pass/0 skip; frontend 599 pass/94 files; required DB guard, lint/typecheck/build passed. BF-06/BF-08 exports inspected. Migration tested in disposable databases only. No commit/push/live migration/deploy.

## Code structure review and refactor proposal — 2026-10-30

- User requested a whole-project review with `ponytail-audit` and `clean-code`, including AI-DLC, and a simple refactor plan. Scope is review/planning, not implementation.
- Source reviewed at HEAD `5a76e428`; working tree initially clean. [Structure audit](inception/reverse-engineering/2026-10-30-code-structure-audit.md) and [proposed refactor plan](inception/plans/2026-10-30-code-structure-refactor-plan.md) prepared for review. Preserve feature/unit ownership; normalize internal controller/service/repository/model and frontend API/model/components/pages/hooks.
- Fresh frontend: 599 passed across 94 files; lint exit 0 with two existing warnings; typecheck passed. Focused backend architecture run did not reach tests because the workspace offline dependency cache was incomplete. Earlier 1,611-pass evidence remains historical, not a fresh result of this review.
- No application/test/config/dependency/DB changes; no commit/push/deploy. Security/resiliency scope retained; PBT disabled. Refactor execution is proposed and pending review; no construction approval is inferred from “Làm tiếp” during the review.

## Withdrawn broader refactor snapshot — 2026-10-30

This section records the superseded working-tree implementation. All non-move edits below were undone after the latest user clarification; use the move-only result at the end of this document.

- Explicit approval: “Giúp tôi refactor lại dự án theo kế hoạch”. Executed directly without subagents in the existing checkout, preserving prior documentation work.
- All 15 backend units and 18 frontend features now follow the approved structure. Admin-only unreachable MCP implementations removed; architecture guard hardened; U05/document/editor/AI-draft responsibilities separated; U05 list lookups batched; frontend public imports stabilized.
- Fresh final backend `mvn clean verify`: 1,614 passed, 0 failures/errors/skips; required PostgreSQL guard passed. Frontend: 601 passed/94 files; lint/typecheck/production build passed. Public unit contracts, dependency versions/lockfiles, migrations and infrastructure config unchanged.
- Current source inventory, all 15 Construction as-built/logical-component documents, services and test guidance synchronized. Earlier reports remain dated evidence, not a fresh acceptance claim.
- [Results and limits](construction/build-and-test/2026-10-30-code-structure-refactor-results.md); [execution ledger](construction/plans/code-generation-plan/2026-10-30-code-structure-refactor-progress.md). No commit, push, deployment or live migration. Browser/provider/load/security/restore acceptance remains unverified.

## Current move-only result — 2026-10-30

User clarified that only moving files/packages is wanted. All original 1,501 source/resource files are retained; 1,066 relocated, with necessary package/import/path updates. Broader splits, cleanup, optimizations, new public barrels and extra tests were undone. Original class/test bodies are retained, with only ArchitectureTest layer names and required-suite FQNs adapted.

Fresh backend clean verify: 1,611 passed/0 skipped, required PostgreSQL guard passed. Frontend: 599 passed/94 files; typecheck/build passed; lint exit 0 with two existing unused-import warnings. No staged changes, commit, push, history rewrite or deployment.

[Current results and limits](construction/build-and-test/2026-10-30-package-moves-only-results.md). Security/resiliency scope unchanged; PBT remains disabled. Existing full runtime/provider/E2E acceptance gaps remain outside this move-only change.

## Fresh verification after git mv — 2026-10-30 17:11 +07:00

HEAD `11df6307` contains documentation only; code remains staged as 1,066 renames and 86 reference updates. Fresh backend clean verify: 1,611 tests passed, 0 failures/errors/skips, JAR built; required PostgreSQL suites passed. Frontend: 599 tests passed/94 files, production build/typecheck passed; lint exit 0 with two existing unused-import warnings. Logs: `.tmp/2026-10-30-after-gitmv-*.log`. No source fix, commit, push or deploy. Existing browser/provider/load/restore acceptance limits remain unchanged.

## Commit authorization — 2026-10-30 17:13 +07:00

User requested “giúp tôi commit”. Include the verified file/package relocation and latest verification/audit notes in this commit. Pre-commit comparison confirms all 1,501 original source/resource files retained, 908 Java bodies and 506 TypeScript/TSX ASTs unchanged except necessary package/import/reference updates. Backend/frontend source matches the tested tree; test/build evidence above applies. No push, deployment or history rewrite.

## U09 scoped structure cleanup — 2026-11-06

**Subsequent full verification requested and completed:** final isolated Maven verify passed 1,830 tests with zero failures/errors/skips and packaged the executable JAR; all four required PostgreSQL suites passed. Frontend 799 tests/116 files, full lint, production build and typecheck passed. [Fresh results, environment corrections and limits](construction/build-and-test/2026-11-06-u09-structure-full-verification.md). No application/config/dependency changes from verification; temporary backend POM removed. No commit, push, restart or deployment.

- [x] User approved two concrete review recommendations with “ok giúp tôi sửa”; implemented directly without subagents.
- [x] Moved frontend error formatter/test into model; updated all six external consumers. Renamed two concrete JDBC repositories without Adapter suffix; updated integration wiring and current documentation.
- [x] Fresh frontend 134 tests/16 files, typecheck and scoped lint passed. Backend compiled all main/test sources; final repository/PostgreSQL/architecture run passed 19 tests with zero skips after starting Docker Desktop.
- [x] Exact move/rename comparisons and whitespace check passed; no business logic, SQL, contracts, dependencies or schema changes.

[Execution and verification details](construction/plans/code-generation-plan/2026-11-06-u09-structure-cleanup.md). No full-suite/production-build/manual acceptance rerun, application restart, commit, push or deployment; Docker Desktop remains running. Security/resiliency scope retained; PBT disabled.

## Bug remediation and doc sync — 2026-11-01

Commit `1cd58c02` on local branch `fix/review-bugs-2026-11-01` fixes 26 review bugs plus the DOCX preview CSP issue. Business rules, frontend components, API inventory, contracts and decision register were synchronized to the new behavior; summary in [review-bugs remediation](construction/remediation/review-bugs-2026-11-01/README.md). Frontend lint/typecheck/605 tests/build passed; backend compiled and unit-tested without Maven (environment-caused failures only). `mvn verify` and Testcontainers suites not run; nothing pushed.

## Excel import / YouTube playback — 2026-11-09

- [x] Workspace Detection: brownfield; baseline/source inspected; initial tree clean.
- [x] Requirements: scoped document created; user requested continuation with “continue”.
- [x] Workflow Planning: [execution plan and approval](inception/plans/2026-11-09-excel-youtube-execution-plan.md).
- [ ] Plan approval before application changes.
- [ ] Implementation and fresh verification.

CSV-only import confirmed; YouTube runtime cause unconfirmed. No application changes or fresh tests. Security/Resiliency scope retained; disabled PBT skipped.

## Excel imports — implementation, 2026-11-09

- [x] User approved the plan with “a”; later explicitly canceled YouTube work and requested every Admin/Manager CSV import use Excel.
- [x] U01 account import: POI XLSX reader with shared validation, preview/commit, limits and legacy CSV API.
- [x] U05 enrollment: XLSX email reader, existing text API and server checks, Excel template.
- [x] U10 question import: Excel-only picker/labels and real XLSX template; existing parser retained.
- [x] Fresh frontend 814 tests/119 files, related backend 39 tests/0 skipped, lint and production build passed. Standalone typecheck pending final confirmation after stale generated receipt types were regenerated by build.
- [ ] Final diff, typecheck and result report.

No YouTube/CSP changes, migration, dependency addition, commit, push or deployment. Security/Resiliency scope retained; disabled PBT skipped.

## Excel imports — final verification, 2026-11-09

- [x] Standalone typecheck passed after production build regenerated stale Next types.
- [x] Final related backend 39/0 skipped; frontend 814/119 files; full lint and production build passed.
- [x] Diff reviewed and documentation synchronized; [results and limits](construction/build-and-test/2026-11-09-excel-import-results.md).

Approved Excel scope complete. Live browser import, full backend/DB/performance and deployment remain outside the completed verification. Backend not restarted; no commit/push/deploy. YouTube work canceled by user.

## Materials and quiz UI proposal — 2026-11-09

- [x] Read-only scoped code/DOM/API audit of materials, quiz authoring, AI material selection and draft dispatch.
- [x] Prepared [UI proposal and findings](inception/requirements/2026-11-09-material-quiz-ui-proposal.md).
- [ ] User chooses Disabled behavior and approves design before implementation planning.

Current live quiz has three YouTube sources without text and no AI drafts; no provider failure claimed. No application/test/config changes from this proposal. Existing Excel diff retained. No task creation, production writes, test/build run or deployment in this scope.

## Materials / inline quiz authoring — completed, 2026-11-09

- [x] User approved implementation; default Disabled behavior adopted. Final visible status copy is Vietnamese Bật/Tắt.
- [x] Material visibility plus direct/token-redemption access, formatted material/quiz instructions and student view; JDBC touched repository with two safe migrations.
- [x] Inline manual/bank/AI authoring, source dropdown, persistent truncation warnings, empty-source rejection, retry/input/navigation protection.
- [x] Four Important edit-loss review fixes and list marker gutter follow-up corrected.
- [x] Backend clean533/0skip plus overlapping focused104/119/10 and required guard passed; backend JAR packaged.
- [x] Frontend836/124files, full lint/build/typecheck passed. Gutter final82/10files+build/typecheck; Vietnamese copy final55/8files passed.
- [x] [Final results and applying migrations](construction/build-and-test/2026-11-09-material-quiz-ui-results.md), current contracts/docs and plan checks synchronized.

Manual UI/mobile/provider acceptance remains with user by explicit request. No live migration, application restart, commit, push or deployment. Existing Excel diff retained. Security/Resiliency scope retained; PBT disabled. Backend update needs Flyway material2200/quiz2201 before testing full behavior on running app.

## Quiz AI HTTP source follow-up — 2026-11-09

- [x] Fixed stale mandatory source validation in shared quiz/bank HTTP parser; absent/null source reaches existing automatic selection.
- [x] Regression red reproduced; backend50/0skip and JAR packaging passed; frontend payload2 passed.
- [x] [Fix evidence and limits](construction/build-and-test/2026-11-09-quiz-http-auto-source-fix.md).

No additional migration. Restart backend using updated artifact for user acceptance. Live provider/UI acceptance remains unperformed. Previous changes and extension configuration retained.

## Live AI worker diagnosis — 2026-11-09

- [x] Matched failed quiz task to personal Gemini model and persisted HTTP503; current connection probe passed later.
- [x] Found duplicate older Docker worker and stopped only that container; current local,worker backend remains active with four AI consumers and empty queue.
- [x] [Runtime evidence and remaining provider limit](construction/build-and-test/2026-11-09-ai-worker-runtime-diagnosis.md).

No source/config/schema change or real AI retry submitted. Previous FAILED draft remains unchanged; user can submit a new request. Upstream provider availability remains unverified.

## Current-change documentation reconciliation — 2026-11-09

- [x] Compared implementation with requirements/stories, U01/U05/U10/U13 current behavior and U03/U06/U11 cross-unit contracts.
- [x] Synchronized19 Markdown files;22 added local links resolve and scoped diff whitespace passed. Historical evidence retained.
- [x] [Document map and verification limits](construction/build-and-test/2026-11-09-aidlc-doc-sync.md).

Documentation-only; no application test rerun or new code/schema/config/runtime change. Manual UI and successful real AI generation remain unverified. Material/version JDBC conversion does not include module persistence. Security/Resiliency scope retained; PBT disabled.

## AI draft source label fix — 2026-11-09

- [x] AUTO/null provenance now displays Hệ thống tự chọn nguồn AI; PERSONAL without provider displays API cá nhân; SHARED/MCP/provider labels retained.
- [x] Regression red3 cases, green37tests/2files; scoped ESLint and whitespace passed. U10 frontend documentation synchronized.

No backend restart/migration/provider call or manual UI test. Source selection behavior unchanged.

## AI quiz review follow-up — 2026-11-09

- [x] Xem/Thu gọn reflects actual open draft; unsaved edits guarded.
- [x] Optional prompt with selected source documents, server default instructions and same-call relevance warning persisted.
- [x] Chấp nhận tất cả uses current edited/nondropped questions, validation, busy lock and existing versioned accept API.
- [x] Frontend66/5files, scoped lint/typecheck; backend98/5suites0skip and JAR package passed; affected docs synchronized.
- [x] [Evidence and applying](construction/build-and-test/2026-11-09-ai-quiz-review-followup.md).

Restart current backend/worker for new behavior; no additional migration. No live provider/UI acceptance. Old drafts not reassessed; model semantic relevance can be wrong. Prior source-label fix and earlier changes retained.

## Admin subject-list runtime recovery — 2026-11-09

- [x] Read Frontend terminal in Orca as requested, checked backend/metadata and separated proxy/CSP/session evidence.
- [x] Added server-only unexpected-U04 exception logging; existing error/API/auth behavior retained. Controller/service54 and isolated PostgreSQL31 passed; frontend27/3files passed.
- [x] Restarted current local API/worker after zero-running-AI check; health200. User confirmed “Đã tải được môn học”.
- [x] [Evidence and limits](construction/build-and-test/2026-11-09-admin-subjects-runtime-diagnosis.md).

No proven original500 root cause, CSP relaxation, data/migration/auth change or browser UI test. Runtime recovery is confirmed by user; diagnostic logging retained for recurrence. Security/Resiliency retained; PBT disabled.

## AI source grounding enforcement — 2026-11-10

- [x] Mismatch output no longer becomes READY; empty/PENDING plus warning, one source-only retry, second mismatch fails closed.
- [x] Current rights/version/source/text/quota, row lock, atomic job/task metadata, rollback-safe failure, after-commit and owner-only recovery verified.
- [x] Final backend103/6suites0skip/package; frontend44/2files; scoped diff and29 added links passed.
- [x] U10/current stories synchronized; [results and limits](construction/build-and-test/2026-11-10-ai-source-grounding-fix.md).
- [x] Gracefully restarted current local API/worker after zero-running-task check; PID55012, health200.

No additional migration or real provider/UI acceptance. Extra retry costs at most one additional task; AI semantic checks are fallible. Old incorrect READY proposals remain historical and must be discarded/recreated. No commit/push/deploy.

## Local commit delivery — 2026-11-10

- [x] User accepted current behavior and requested clear commits.
- [x] 5d628228: Excel account/enrollment/question imports and real XLSX templates.
- [x] b1729529: material visibility/formatting/JDBC and inline quiz/AI source grounding, safeguards and tests.
- [x] 0df689df: subject server error diagnostics, generic client response retained.
- [x] Final AI-DLC documentation commit carries scoped plans, requirements, evidence and this receipt.
- [x] Fresh backend239/19suites0skip plus separate subject54/2suites0skip; full frontend849/124files, full lint and typecheck passed.

Logs .tmp/commit-final-backend-tests.log, commit-final-subject-tests.log, commit-final-frontend-tests.log, commit-final-frontend-lint.log and commit-final-frontend-typecheck.log. No source/runtime change, push, PR or deployment in this delivery turn; no new real-provider/UI acceptance. Focused backend scope only. Security/Resiliency retained; PBT disabled.

## PR preparation — 2026-11-10

User requested a PR before merge into staging. Source headfb8846ad has four feature/documentation commits; current origin/staginga35d9854 is5commits ahead and feature4ahead. Merge preview clean; no existing open head/base PR. Publish the current branch via normal fast-forward push and create PR targeting staging; do not merge or enable auto-merge. Only this preparation receipt changes after verified source: backend239 plus subject54, frontend849/124files, lint/typecheck passed. GitHub CI provides merged-tree validation. Live PR URL/status is reported separately from GitHub; no deployment is triggered by this PR under current CI configuration. Security/Resiliency retained; PBT disabled.

## PR59 conflict resolution / merge authorization — 2026-11-10

User explicitly requested resolving PR59 and merging into staging. Latest staging9a07e1e introduces U08 feedback/U12 changes. Only audit.md had a textual conflict; preserve both histories, do not drop either side. Application changes merged automatically. Refresh checks on the resolution head and merge with exact-head protection only after CI passes; preserve feature commits/branch and do not deploy production. Previous PR-only authorization is superseded for this merge. Security/Resiliency retained; PBT disabled.


## Grading remediation continuation — 2026-11-10

- [x] Restored the 37-file Antigravity stash; completed the five-wave scope sequentially, without subagents.
- [x] Official group-grade preservation, member history/transfer/adjustments (including legacy data), AI validation, group evidence/freshness, rubric/deadline/quiz fixes, query batching, student feedback, batch controls and bounded polling.
- [x] Requirements/stories, affected unit docs, OpenAPI and the original/continuation plans synchronized.
- [x] Final backend 2,095 tests with zero failures/errors/skips and executable JAR; required five-suite/57-case guard passed. Frontend 870 tests/127 files, full lint/typecheck/build passed. Source snapshot unchanged.

[Results, evidence and applying migrations](construction/build-and-test/2026-11-10-grading-remediation-results.md). Original stash retained; changes unstaged, no commit/push/deploy. Existing browser was at login; no authenticated UI/mobile/provider acceptance or live grade write. No running API/worker restart or live migration. Update the local backend through the existing Flyway startup process to apply the two new migrations. Security/Resiliency retained; PBT disabled.


## Grading UI screenshot follow-up — 2026-11-10

- [x] Measured toolbar-relative score/comment offset, readable auto-growing feedback, clean evidence links, dated successful proposal cards; latest failure notice retained outside history. Internal diagnostics preserved.
- [x] Full frontend873/127files, lint/typecheck/build; desktop/mobile real-component fixture inspected. No authenticated session/provider/live grade write or backend change/restart.

[Results and acceptance](construction/build-and-test/2026-11-10-grading-ui-feedback-polish.md). Existing work retained, no subagents/commit/push. Security/Resiliency retained; PBT disabled.


## AI grading action and reported rejection — 2026-11-10

- [x] Larger36px AI grading action with permanent label; narrow read-only toolbar wraps. Regression206/20files, scoped lint, typecheck/build and synthetic desktop/mobile fixture passed.
- [x] Read-only runtime diagnosis: two Gemini tasks READY then grading REJECTED. Output deliberately cleared after handling; exact invalid field remains unproven. No backend fix or successful new provider run claimed.

[Evidence and limits](construction/build-and-test/2026-11-10-grading-ai-button-and-error.md). No subagents/live grade/provider retry/application restart/commit/push. Security/Resiliency retained; PBT disabled.


## AI grading rejection fixed — 2026-11-10

- [x] Reproduced zero-score/full-credit-check contradiction with controlled Gemini request; provider output now omits redundant checks and existing normalization derives them. Full MCP schema/strict validation retained.
- [x] Red/green regression, connected210/38classes0skip, final14 regression, Maven verify/JAR and live Gemini READY with16 feedback items passed. Local API/worker refreshed; grade fingerprint/version unchanged in final verification.
- [x] Opened new proposal in a separate authenticated Chrome tab; original tab preserved. Two provider calls; no grade apply/save/finalize/publish, migration, commit/push or subagents.

[Results and limits](construction/build-and-test/2026-11-10-grading-ai-rejection-fix.md). Other models were not tested live; whole-backend suite not repeated. Security/Resiliency retained; PBT disabled.


## Grading feedback formatting — 2026-11-10

- [x] General paragraph action/readable type; strengths/improvements as separate editable bulleted ideas with retained newlines and add/remove/limit controls. Blank draft rows excluded only at save.
- [x] Grading/document210/20files, final grading154/17files, scoped lint, typecheck/build and synthetic desktop/mobile inspection passed.

[Evidence and acceptance](construction/build-and-test/2026-11-10-grading-feedback-format.md). No live grade save, provider call, backend/runtime change, commit/push or subagents. Security/Resiliency retained; PBT disabled.


## Repeated grade actions — 2026-11-10

- [x] Shared state eligibility for both batch selectors; readable completed/race feedback and safe draft-only publish fallback. Backend guards retained.
- [x] Grading167/18files, scoped lint, typecheck/build; authenticated gradebook shows published student with all batch actions disabled.

[Results and limits](construction/build-and-test/2026-11-10-grading-repeat-actions.md). No live finalize/publish, backend/runtime change, migration, commit/push or subagents. Security/Resiliency retained; PBT disabled.


## Grading local commit delivery — 2026-11-10

- [x] User requested clear local commits, explicitly no push. Five groups: rubric/deadline, quiz/activity, grading/group/AI backend, grading UI, then AI-DLC records.
- [x] Fresh full backend2096 tests0skip/JAR and five-suite/57-case guard; full frontend891/128files, lint/typecheck passed. Source unchanged during packaging.
- [x] Source commits51428543,66af2cd5,27ec9f0a,8f0ac9ea; this final documentation commit completes the series. Original Antigravity stash retained.

[Commit receipt and limits](construction/build-and-test/2026-11-10-grading-local-commits.md). No push, GitHub CI, deployment, runtime restart, provider call, live grade write or subagents in this turn. Security/Resiliency retained; PBT disabled.


## Staging merge resolution — 2026-11-10

- [x] Resolve the existing merge of staging57b0bb8d, retaining both audit histories exactly.
- [x] Align the published-feedback test fixture with the new deadline-gated history behavior.
- [x] Verify the merged source and complete the authorized local merge delivery; no push. Full backend verify2110 tests0fail0error0skip/JAR; frontend908 tests/129files, full lint/typecheck passed.

Existing Security/Resiliency scope retained; PBT disabled. No production behavior changed by manual resolution. No UI/provider acceptance, frontend production build, deployment, live migration or application restart in this merge task.


## Authorized staging-to-main release — 2026-11-10

User authorized publishing a staging-to-main PR and merging after CI passes. Application source is the verified7fbf5703 tree; this checkpoint only records authorization. The GitHub PR/workflow and final chat response carry the release outcome without adding another source commit after verification. Existing Security/Resiliency scope retained; PBT disabled.

## Gradebook export naming — 2026-11-10

- [x] Scoped workspace/export review and requirements documented in inception/requirements/2026-11-10-gradebook-export.md.
- [x] User approved implementation; safe assignment filenames shared by API and UI. Backend14 tests, frontend183 tests plus final35-case subset, typecheck/scoped lint passed. Evidence: construction/build-and-test/2026-11-10-gradebook-export.md. No live Excel/browser acceptance, full suite/build, restart, deployment or commit/push. Security/Resiliency retained; PBT disabled.
- [x] Screenshot follow-up: Vietnamese headers/statuses/late flags/time/sheet, active-assignment export default, single-assignment title and ID-free aggregate filenames. Backend15/15, frontend35/35, typecheck/lint passed. Local API/worker refreshed; two pending staging migrations applied on startup. Real local browser downloads and XLSX XML assertions passed for assignment5rows and aggregate20rows/4assignments. User's original tab retained. No VPS release, grade write, commit/push or subagents; current evidence/limits in the same report.

## Diagram-only AI grading consistency — 2026-11-10

- [x] Reproduced erroneous empty detection for real compressed Draw.io pages; grading now reuses the shared bounded document parser. Live heading/rubric snapshot mapping matches2+4+4=10.
- [x] Full backend2116 tests/253suites0fail0error0skip/JAR and required guard passed. One successful live Gemini retry gives READY8.50 with all3 sections recognized; persisted grade remains DRAFT0.00/version1 and submission hash unchanged.

Evidence: construction/build-and-test/2026-11-10-grading-diagram-parity.md. No grade save/finalize/publish, assignment/submission edit, migration, VPS update, commit/push or subagents. Qualitative scoring still needs teacher review. Existing Security/Resiliency retained; PBT disabled.

## Accurate Vietnamese AI errors — 2026-11-10

- [x] Translate actual grading validation failures and old proposal messages without exposing uppercase codes; preserve precise provider reasons and internal server diagnostics.
- [x] Recognize explicit token-truncation metadata for3 providers with charged usage retained; distinguish known quota/rate errors without guessing from429 alone.
- [x] Final343 connected tests0fail0error0skip and authenticated browser inspection pass. Local API/worker refreshed; original browser form retained and saved grade/version/hash unchanged. No paid model request, grade write, migration, commit/push or VPS update.

Evidence and limits: construction/build-and-test/2026-11-10-grading-ai-errors.md. Existing diagram fix retained. Security/Resiliency retained; PBT disabled.


## Deterministic AI grading totals — 2026-11-10

- [x] Provider schema/prompt delegate totals to existing backend calculation; complete MCP contract and strict item/check/evidence validation retained.
- [x] Final349 connected tests0fail0error0skip; local API/worker refreshed, health200. One live proposal READY7.50 matches12 item scores and section totals2+3+2.5 in storage/UI. Saved grade/state/version/hash unchanged.

Evidence: construction/build-and-test/2026-11-10-grading-derived-total.md. Original tab retained; separate test tab shows unsaved proposal. No official grade action, commit/push or VPS update. Other providers not tested live; full suite/JAR not repeated. Security/Resiliency retained; PBT disabled.


## Compact optional manual grading feedback — 2026-11-10

- [x] Remove grade-history/paragraph-split/whole-feedback-clear UI; optional change note collapsed. Manual strengths/improvements and per-item comments/evidence available without AI; blank manual fields accepted, AI completeness/review checks retained.
- [x] Inline required adjustment reason and readable fallback; autosave pauses after errors and resumes on edits.
- [x] Final173 frontend/204 backend tests, TypeScript/scoped lint and45 final-polish cases pass. Local API/worker refreshed and health200; DOM checks pass. Browser-test accidental local draft mutation restored to exact7.40 score/non-audit contents via normal saves; version/history correction retained, no finalize/publish.

Evidence and limits: construction/build-and-test/2026-11-10-manual-feedback-parity.md. No commit/push, VPS release, migration, provider call or subagents. Full build/suites/mobile visual verification not repeated; screenshot follow-ups timed out. Security/Resiliency retained; PBT disabled.


## Student assignment context columns — 2026-11-10

- [x] Class and subject codes shown across3 tabs; fixed widths/overflow and correct empty/loading spans. Existing filtering/navigation retained.
- [x] Assignment-work23 tests plus final4 page cases, TypeScript/scoped lint and whitespace pass. No API/data change, account switch, commit/push or deployment. Prior grading work preserved.

Evidence: construction/build-and-test/2026-11-10-student-assignment-context.md. Live student/mobile visual acceptance and full build not repeated. Security/Resiliency retained; PBT disabled.


## Quiet30-second grading autosave and one optional note — 2026-11-10

- [x] Mock diagnosis confirms the old1500ms global busy transition replaced score editors without an actual evaluation reload.30-second idle background save retains inputs/focus; view-only renders do not reset the timer.
- [x] Single-flight/revision/version handling preserves newer edits; explicit Save/Ctrl+S, error pause and group/stale/locked-grade guards retained. One optional change note replaces required adjustment reason, with user-authorized domain rule update and legacy-note preservation.
- [x] Final176 frontend/206 backend cases, final13 hook cases, TypeScript/scoped lint/whitespace pass. Local API/worker refreshed, health200. All grade mutation tests mocked; no live grade edits, commit/push or VPS deploy.

Evidence: construction/build-and-test/2026-11-10-grading-quiet-autosave.md. Prior pending changes retained. Full build/real multi-session/mobile visual checks not repeated. Security/Resiliency retained; PBT disabled.

## Student published result entry — 2026-11-10

- [x] Confirmed local published8/10 is hidden behind a newer draft plus pre-deadline history filtering. Detail now retains submitted result and a published-only detail link; read-only viewer admits current grading candidate before deadline without older history/drafts. Existing backend ownership/publication controls retained.
- [x] Connected26 frontend/9 GradebookService cases and final13 affected page cases pass; TypeScript/scoped lint/whitespace pass. No live grade writes, student-account switch, subagents, commit/push or deployment. Prior pending changes retained.

Evidence: construction/build-and-test/2026-11-10-student-published-result-entry.md. Student live visual acceptance/full build not performed. Security/Resiliency retained; PBT disabled.

## Local commits and fresh verification — 2026-11-10

- [x] ee0a3838: compact optional manual feedback, unified optional note and quiet30-second autosave with edit/version preservation.
- [x]23f9309a: student assignment class/subject columns and regression coverage.
- [x]4873b931: student published result and detail access alongside newer drafts, preserving private-grade controls.
- [x] Fresh202 frontend/206 backend architecture cases, TypeScript/scoped lint/whitespace checks pass; cumulative audit/state recorded separately. No push/deploy, live grade mutation or subagents. Full production build/live student visual acceptance not repeated.

## Editable published grades and background AI — implemented, 2026-11-11

- [x] User selects immediate student-visible corrections after every successful save, and editing until class ARCHIVED including past assignment deadlines. No separate published snapshot or republish step.
- [x] Latest AI clarification supersedes the earlier wait-on-screen wording: background task must not block grading/navigation. Reviewed model/save/publication, group and proposal callback guards and current busy/modal/auto-apply behavior on2444c3f4.
- [x] User approved the six-task plan with `$ponytail:ponytail triển khai theo kế hoạch`, including explicit Apply/notification. Implemented direct publication/legacy FINALIZED compatibility, preserving PUBLISHED after saves, AI review safeguards, current receipt/version rules and background result isolation/recovery. Removed finalize buttons; retained quiet30-second autosave.
- [x] Full Maven verify2146 tests with no failures/errors/skips and required integration/traced-suite guard pass. Full frontend956 tests/134 files pass; final connected211 tests/25 files and workspace39 tests pass after subsequent additions (overlapping counts). TypeScript, scoped ESLint, production frontend build and whitespace pass. No live grade edits, paid model calls, subagents, commit/push or deploy.
- [x] User restarted the local backend; direct Orca UI acceptance passed for published corrections8→8.25, student visibility/detail access, background AI across navigation, concurrent manual edits preserved when AI completes, explicit Apply without automatic saving and quiet30-second autosave without reload. Restored saved8.00 and original feedback. New AI proposals remain in history. Archived-class/group UI acceptance was not performed; automated coverage remains. The earlier rejected restart command stopped/restarted no process.

Plan: construction/plans/code-generation-plan/2026-11-11-editable-grades-and-background-ai.md. Evidence: construction/build-and-test/2026-11-11-editable-grades-and-background-ai.md. Security/Resiliency compliant for existing scope; PBT disabled. Existing U12 preservation of official group grades on resubmission remains, with stale-receipt edits denied. Student tabs see saved corrections on load/refetch; no realtime push added.

User subsequently authorized descriptive local commits. Source/tests and documentation recorded separately; fresh precommit frontend grading/assignment-work run passed211 tests/25 files. No push or deployment.

## Ba lỗi UI/UX staging — 2026-11-11

Người dùng duyệt "sửa 3 lỗi trên". Baseline origin/staging ce40e447; thực hiện tại worktree riêng `.claude/worktrees/ui-ux-staging`, nhánh `codex/ui-ux-staging`.

- [x] Workspace Detection, yêu cầu và kế hoạch sửa giới hạn; dùng lại brownfield context.
- [x] Pagination/PageNav xuống dòng; Switch có accessible name từ nhãn; Field nối lỗi tới control và giữ focus khi lỗi thay đổi.
- [x] 332 test/51 file, TypeScript, ESLint, diff check qua. Production build exit 0, có cảnh báo font bị chặn và 27 standalone traced files ở ngoài root do junction dependency.
- [x] Ghi giới hạn: chưa kiểm tra layout trên browser/mobile, chưa xác nhận standalone deployment; không commit/push/deploy hay thay checkout gốc.

[Kế hoạch](construction/plans/code-generation-plan/2026-11-11-three-ui-fixes.md) và [kết quả](construction/build-and-test/2026-11-11-three-ui-fixes.md). Security/Resiliency giữ phạm vi đã duyệt, PBT disabled. Các stage thiết kế mới/stories/hạ tầng/operations không cần cho ba lỗi hiện hữu.

### Staging readiness — ba lỗi UI/UX, 2026-11-11

- [x] Fetch origin/staging vẫn cùng ce40e447; review không thấy blocker.
- [x] Full frontend 963 test/135 file qua; Docker build sạch cảnh báo font/tracing và image smoke test health UP/login/icon 200.
- [x] Đủ điều kiện commit/tạo PR staging; merge sau CI PR. Chưa commit/push/merge; chưa xác minh browser/mobile thực. Chi tiết trong báo cáo ba lỗi UI/UX ở trên.

### Cho phép tạo PR — ba lỗi UI/UX, 2026-11-11

Người dùng yêu cầu "tạo pr vào staging cho tôi". Cho phép commit/push nhánh `codex/ui-ux-staging` và tạo PR base `staging`; không bao gồm merge. Source là bản đã qua 963 test/135 file và Docker smoke. Trạng thái phát hành cuối theo PR GitHub và phản hồi giao hàng.

## Teacher class overview — 2026-11-11

- [x] User approved replacing submission-progress tabs/recent announcements on teacher class info with compact read-only class facts, three assignment summary links and nearest3 upcoming deadlines. Student class info and announcement routes preserved. Reused assignment list/submission summaries; no full gradebook load, new API or schedule scope.
- [x] RED/GREEN:264 related tests/31 files pass; final targeted34/2 pass after link fix. TypeScript, scoped ESLint and whitespace pass. Local browser verified1440px/390px, light/dark, correct counts and actionable filtered gradebook link. No class/grade writes. Security/Resiliency retained; PBT disabled.
- Limits: numbers refresh on page reload; summary links select the first assignment with matching work. Production build/backend suite not repeated for this frontend-only change; no commit/push/deploy. Plan/evidence: construction/plans/code-generation-plan/2026-11-11-teacher-class-overview.md.

- Subsequent user request authorizes local commit, push and PR to staging. Fresh34 targeted tests and TypeScript pass; fetched staging has no divergence from the implementation baseline and merge preview has no conflict. Publish the overview changes only; no automatic merge/deployment.

## Gradebook bulk selection in the table — 2026-11-11

- [x] Approved proposal implemented inline: no duplicate selection list/toggle, row/header mixed-state checkboxes,20 rows/page, explicit all-filtered selection capped100. Counts reflect action eligibility. Selection survives page changes but resets for assignment/search/status/mode. Group roots selected once; member grades remain separately accessible.
- [x] RED4 new tests, GREEN189 grading tests/20 files; final targeted20/2 pass, TypeScript/scoped lint pass. Local5-student/group-root UI verified at1440px/390px, light/dark, no document overflow; no AI/publish/data writes.30/101 scenarios covered with fixtures.
- No backend/API/dependency, production build, commit/push/deploy. Security/Resiliency retained, PBT off. Plan/evidence: construction/plans/code-generation-plan/2026-11-11-gradebook-bulk-selection.md.

- Approved follow-up: global announcement navigation/title is “Tất cả thông báo”; nested class navigation/title remains “Thông báo lớp”. URLs/filtering/authorization unchanged.11 announcement tests and scoped lint pass; local browser verifies both destinations and titles. Checkbox cursors also verified as pointer/enabled and not-allowed/disabled. Changes remain uncommitted.

- Shared cursor follow-up: base CSS applies pointer to enabled native/custom interactive controls and not-allowed to disabled, preserving text/drag/pending utility overrides. Actual group choices and14 browser style probes pass;324 related tests/44 files, lint and production build/typecheck pass. No backend/data changes, commit/push. Not every screen/state was manually exercised.

- Subsequent user authorization: “giúp tôi commit”. Record bulk-selection, announcement labels, shared cursors and documentation in separate local commits;15 fresh precommit tests/3 files pass. No push/merge/deploy.

- PR74 CI follow-up: feature-registration test retained the old global announcement labels. Reproduced RED locally and updated all3 role expectations to the approved label; fixed-class labels remain unchanged. Full frontend967 tests/136 files and scoped lint pass. User authorizes pushing the fix to staging to repair PR74; no merge/deploy requested. Evidence: .tmp/pr74-red.log, pr74-frontend-full.log.

### PR delivery — ba lỗi UI/UX

Tích hợp staging `1f713615`; giữ cả hai lịch sử audit/state. Fresh full frontend
963 test/135 file, ESLint/TypeScript, Docker build và runtime smoke qua. Người dùng
cho phép push/tạo PR vào staging; không tự merge PR. Xem PR GitHub để biết trạng thái CI.
