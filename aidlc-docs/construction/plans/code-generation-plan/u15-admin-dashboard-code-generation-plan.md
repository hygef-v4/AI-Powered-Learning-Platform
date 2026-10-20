# U15 Admin Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: dùng superpowers:executing-plans để thực thi từng task, hoặc superpowers:subagent-driven-development nếu người dùng chọn khi duyệt. Mỗi bước dùng checkbox. Không bắt đầu code khi plan chưa được duyệt.

**Goal:** UC66 Admin Dashboard chỉ cho ADMIN, số liệu tổng hợp và metadata backup, nguồn lỗi không giả số0/SUCCESS.

**Architecture:** U15 đọc public port U01/U02/U03/U04/U05; một service orchestration/mapping và một reader nội bộ có bounded executor/deadline. Frontend dùng Admin layout/session/shared UI/apiFetch sẵn có. Không bảng, migration, cache counts, queue hay dịch vụ ngoài.

**Tech stack:** Java17 target, Spring Boot3.5.6/Security, JUnit/Mockito/ArchUnit; Next.js/React/TypeScript, Vitest/Testing Library; không thêm dependency.

**Spec:** [contract review C01–C07](../../u15-admin-dashboard/code/implementation-contract-review-2026-10-20.md), [BR](../../u15-admin-dashboard/functional-design/business-rules.md), [logic](../../u15-admin-dashboard/functional-design/business-logic-model.md), [domain/API](../../u15-admin-dashboard/functional-design/domain-entities.md), [UI17/10](../../u15-admin-dashboard/functional-design/frontend-components.md), [NFR](../../u15-admin-dashboard/nfr-requirements/nfr-requirements.md).

**Status:** user duyệt phương án A và thực thi trực tiếp ngày2026-10-20: “Cứ giúp tôi code trc theo plan còn khi nào u3 xong thì xử lý sau”. Phần provider U03 thiếu vẫn pending, không sửa contract. Xem [approval questions](u15-implementation-approval-questions.md).

## Global constraints

- UC66; US-RPT-002 và metadata US-OPS-001; FR-024/038. Theo BR-U15-01/02/10/11/12/20/21/22/30/31.
- Chỉ ADMIN đang ACTIVE qua IAuthorization; Actor từ principal server. Anonymous theo U01 trả401; role khác403; authorization lỗi/null không được fan-out data. ACCESS_DENIED qua IAudit, không copy exception/provider reason nhạy cảm.
- Nguồn gọi song song, timeout mỗi lời gọi **2giây từ submit**, kể cả queue. NFR **p95<=1giây** đo khi nguồn khỏe; không khẳng định mọi fault request<=1giây.
- Không cache số liệu U15; ISettings cache30giây thuộc U03 giữ nguyên. Không mock production, dữ liệu cá nhân/học vụ, download/restore/run backup, sửa lịch hoặc export dashboard.
- Không SQL/repository/internal import của owner khác; không sửa IBackupStatus/BackupRunView hoặc semantics counts. Registry permission và HomePaths U01 là hai thay đổi liên unit có phạm vi rõ trong Task1/6.
- Baseline workspace: branch grading-gradebook, HEAD6148e430; audit/state dirty từ Jira reconciliation phải giữ. Khi execution mới chọn feature checkout/worktree phù hợp; không reset/merge/commit/push hoặc tạo worktree trong lượt planning.
- Clean Code: tên theo nghiệp vụ, DTO immutable, guard trước effects, một operation rõ mỗi dòng; không interface/helper/framework tổng quát để đủ layer. Chỉ tách trách nhiệm có lifecycle riêng hoặc reuse thực tế.

## Review Focus

1. Role/account đã đổi, null/lỗi authorization: không được trả dù chỉ một thẻ — Task1/4.
2. Provider bỏ qua interrupt/queue đầy: worker và queue vẫn bị giới hạn, deadline không nhân theo số nguồn — Task2.
3. Chưa chạy, FAILED, disabled, unavailable và metadata thiếu khác nhau — Task3/5.
4. Sparse map hợp lệ khác null/âm/overflow; tổng không cộng thiếu — Task3.
5. Request cũ sau đổi trang/đóng dialog/hết phiên không ghi đè hoặc lộ data cũ — Task5/6.

## Phạm vi và decision gates

**A (khuyến nghị):** làm counts/latest/recent3/history/schedule với port hiện có; startedAt và summary latestSuccessfulAt/failures7 chưa có dữ kiện không suy diễn. Code-ready cho lát cắt A, chưa full UC66 Done. **B:** chờ U03 bàn giao contract bổ sung rồi cập nhật Task3/5 trước code Backup đầy đủ.

C01–C07 trong spec addendum là quyết định đề xuất cần duyệt: metadata thiếu, UI mới dùng sidebar/Settings List, ngoại lệ retention, nghĩa count enrollment, settings freshness và hai mục tiêu latency. Không tự mở rộng contract owner. Lát cắt A không được đánh dấu hoàn thành yêu cầu last SUCCESS/7-day summary.

## File map và boundary

Java paths dưới đây tính từ `backend/src/main/java/edu/aiplatform/`; test cùng package từ `backend/src/test/java/edu/aiplatform/`.

| File | Trách nhiệm |
|---|---|
| admindashboard/application/AdminDashboardService.java | Quyền, submit/ghép nguồn, mapping counts/backup/history |
| admindashboard/application/AdminDashboardViews.java | Immutable DTO availability/counts/metadata/page |
| admindashboard/infrastructure/DashboardSourceReader.java | Pool bounded, deadline, cancel/shutdown |
| admindashboard/web/AdminDashboardController.java | Hai GET và query/response |
| admindashboard/web/AdminDashboardExceptionHandler.java | ProblemDetail scoped, không sửa global handler |
| identity/contract/Actions.java, identity/application/AccessPolicy.java | Một action mới ADMIN_DASHBOARD_VIEW, ADMIN/NONE |
| identity/application/HomePaths.java | ADMIN_HOME=/admin chỉ khi Task6 pass |
| docs/contracts/openapi/admin-dashboard.yaml | API mới, nullable/partial/empty/error examples |
| frontend/src/features/admindashboard/types.ts, api.ts | Model JSON và HTTP client |
| frontend/src/features/admindashboard/{AdminDashboardPage,CountCard,BackupSection,BackupHistoryDialog,StatusBreakdown}.tsx | Màn a01/popup a01b theo thiết kế |
| frontend/src/app/admin/page.tsx | Mount dashboard dưới layout hiện có |
| frontend/src/features/admindashboard/index.ts, register.ts | Export; giữ duy nhất NavEntry admin-dashboard đã có |

Các folder contract/domain đang .gitkeep không cần tạo class. Private access/mapping giữ cùng service trừ khi có lý do tách thực tế; reader riêng vì sở hữu worker/deadline/lifecycle.

## Task1 — Permission registry và read boundary

**Files:** Actions/AccessPolicy; tạo AdminDashboardService/AdminDashboardViews; test `admindashboard/application/AdminDashboardAccessTest.java`, `identity/application/AdminDashboardPolicyTest.java`.

**Interfaces:** consumes `IAuthorization.authorize(Actor,String,ResourceRef)`, `IAudit.record(AuditEvent)`, `Clock`; produces `dashboard(Actor):AdminDashboardView`, `backups(Actor,int page,int size):BackupHistoryPage` ở service và private requireAdmin. DTO hoàn thiện Task3; không return fixture thành công để vượt tests.

- [x] RED: ADMIN hiện hành ACTIVE allowed; STUDENT/TEACHER/SUBJECT_MANAGER, ADMIN DISABLED/PENDING, stale principal, actor null, decision null/deny/exception đều không gọi count/backup/settings. Policy mới chưa đăng ký phải deny.
- [x] Thêm Actions.ADMIN_DASHBOARD_VIEW và ADMIN+ScopeKind.NONE cho cả hai GET; không nới action khác. Service guard trước scheduling; permission deny403 DASHBOARD_FORBIDDEN, auth dependency/null503 DASHBOARD_AUTHORIZATION_UNAVAILABLE; missing actor401 theo conventions U01.
- [x] Audit denied: ACCESS_DENIED, objectType ADMIN_DASHBOARD, objectId null, result DENIED, reason là mã an toàn; không chứa exception text. Audit lỗi ->503 DASHBOARD_AUDIT_UNAVAILABLE và vẫn không đọc số liệu. Không success audit cho GET nếu spec không yêu cầu.
- [x] GREEN policy/access + ArchitectureTest. Actor fixture chỉ là unit-test seam, chưa phải filter-chain/JWT/session evidence.

## Task2 — Bounded fan-out và deadline

**Files:** `admindashboard/infrastructure/DashboardSourceReader.java`; test cùng package `DashboardSourceReaderTest.java`.

**Interfaces:** consumes Callable<T>, Duration và JDK Future/executor; produces `submit(Callable<T>):PendingSource<T>` và `await(PendingSource<T>):SourceResult<T>`, nested immutable types AVAILABLE/UNAVAILABLE chỉ nội bộ U15. Production constructor2s; package-private test constructor nhận duration/executor để điều khiển thời gian.

- [x] RED với latch/barrier: tất cả nguồn được submit trước await; lỗi một nguồn giữ nguồn khỏe; deadline tính từ submit kể cả queue; interrupted giữ cờ interrupt; rejection thành UNAVAILABLE. Nguồn khỏe đã xong đúng hạn vẫn AVAILABLE dù được await sau nguồn chậm; nguồn hoàn thành sau deadline phải UNAVAILABLE. Không sleep2s hoặc test dựa timing sát.
- [x] Implement ThreadPoolExecutor8worker, ArrayBlockingQueue32, AbortPolicy; không commonPool/CallerRuns hoặc pool mỗi request. Deadline monotonic, get chỉ remaining budget, không cấp lại2s mỗi lần chờ. Ghi completion time trong task wrapper: Future đã done được đọc không chờ và so completion với deadline riêng, không từ chối kết quả đúng hạn chỉ vì caller await muộn. Future timeout/caller interruption cancel phần chưa xong, purge queue; @PreDestroy close() shutdownNow.
- [x] GREEN: task bỏ qua interrupt không vượt pool/queue; hai nguồn timeout không cộng thành4s trong test deadline có kiểm soát. Không truyền request transaction/EntityManager vào worker; provider tự đọc trong boundary của mình. Cancel Future không được tuyên bố đã hủy JDBC: query/connection timeout thật cần integration.

## Task3 — Counts, backup và DTO

**Files:** hoàn thiện AdminDashboardService/AdminDashboardViews; tests `admindashboard/application/AdminDashboardServiceTest.java`, `AdminDashboardBackupTest.java`.

**Consumes:** Task1/2; đúng public ports/signature ở spec addendum. Key consumer typed: SettingKey<Boolean>(backup.enabled,Boolean.class), SettingKey<LocalTime>(backup.dailyAt,LocalTime.class), không import BackupSettingDefinitions nội bộ U03.

**Produces — schema đề xuất để đóng băng Task4:**

- Section<T>(Availability availability,T data), enum AVAILABLE/UNAVAILABLE; UNAVAILABLE luôn data=null. AVAILABLE collection rỗng hợp lệ; latest AVAILABLE+null là chưa có run.
- AdminDashboardView(calculatedAt,accounts,subjects,classes,activeEnrollments,backup). accounts Section<AccountCounts>; AccountCounts(byRoleAndStatus,total,activeTotal). subjects/classes Section<Map<enum,Long>>; activeEnrollments Section<Long>. Instant calculatedAt từ Clock trước fan-out, không khẳng định snapshot nguyên tử giữa owner.
- BackupSection(latest,schedule,recentRuns,latestSuccessfulAt,failuresLast7Days); từng field là Section độc lập, data types lần lượt BackupRunItem, BackupSchedule, List<BackupRunItem>, Instant, BackupFailureSummary(count,latestSafeError).
- BackupRunItem(scheduledFor,state,snapshotAt,startedAt,completedAt,byteSize,safeError,purgedAt); không id/archiveRef/hash/key. startedAt nullable vì contract hiện thiếu; UI hiển thị chưa có dữ liệu, không dùng snapshotAt thay.
- BackupSchedule(enabled,dailyAt,timeZone,nextPlannedAt): Boolean/LocalTime/string/Instant, timezone Asia/Ho_Chi_Minh, next chỉ dự kiến theo cấu hình nhận được.
- BackupHistoryPage(items,page,size,totalElements,totalPages); public page0-based, default size20, range1..100; projection từ Page U03, không expose Spring Page.

- [x] RED fixture: accounts STUDENT/ACTIVE=3, TEACHER/PENDING=2 -> total5/activeTotal3, cell khác0. Empty successful map available0; null/negative/null entry/overflow unavailable. Đếm subjects/classes tương tự, enrollments âm unavailable. Dùng Math.addExact hoặc sum checked; không đọc lại port để tính total. Public-seam assertions tối thiểu:

```java
assertThat(view.accounts().data().total()).isEqualTo(5L);
assertThat(view.accounts().data().activeTotal()).isEqualTo(3L);
assertThat(view.accounts().data().byRoleAndStatus().get(Role.ADMIN).get(AccountStatus.ACTIVE)).isZero();
```

- [x] Submit7task: accounts,subjects,classes,enrollments,backup.latest,backup.list(PageRequest.of(0,3)),schedule đọc2key. Partial errors giữ các section độc lập; successful sparse maps zero-fill đúng enum; không biến exception/null thành0.
- [x] RED backup: latest empty khác FAILED/lỗi nguồn; CREATED/RUNNING/FAILED/SUCCESS giữ nguyên; purgedAt quyết định đã dọn. Không suy lastSUCCESS/7-day count từ latest3 hoặc scan toàn history. A giữ hai summary UNAVAILABLE; B chỉ thực hiện sau contract handoff đã duyệt và cập nhật interfaces/tests. Metadata chưa có startedAt luôn null.
- [x] RED schedule: enabled optional.empty dùng default false theo BR-U03-88; dailyAt không có default; enabled=true thiếu giờ ->schedule unavailable; get lỗi ->unavailable. Disabled next=null. Fixed Clock test trước/đúng/sau giờ và qua ngày: slot hôm nay nếu chưa qua, còn lại ngày sau; copy là dự kiến, không hứa scheduler đã tạo run. Không bypass cache/stale fallback U03.
- [x] GREEN focused service/backup/access/reader/policy+architecture. JSON chỉ whitelist count/metadata; safeError từ field đã sanitize U03, không Throwable.message. Không log count map kèm dữ liệu cá nhân.

## Task4 — HTTP/OpenAPI

**Files:** controller, scoped handler, `docs/contracts/openapi/admin-dashboard.yaml`; tests `admindashboard/web/AdminDashboardControllerTest.java`, `AdminDashboardApiContractTest.java`.

**Consumes:** service/DTO Task1–3, U01 principal và shared ApiException/ProblemDetail. **Produces:** GET /api/v1/admin/dashboard; GET /api/v1/admin/dashboard/backups?page=0&size=20; Cache-Control:no-store cho hai response. Không mutation/download/restore endpoint.

- [x] Viết OpenAPI shape/enum/nullability/examples trước consumer. Authorized dashboard200 có partial-unavailable; history dependency lỗi503 DASHBOARD_BACKUP_UNAVAILABLE, không empty200. page<0,size0/>100,sai kiểu hoặc số page vượt int400 DASHBOARD_PAGE_INVALID; page là int không âm, offset tính long, không dùng phép nhân int có thể overflow. Chỉ query page/size, không actor/role/sort/filter tự đặt. Validation nghiệp vụ sau guard và trước provider; lỗi parse ở web không được gọi provider hoặc trả dữ liệu.
- [x] RED MockMvc contract: principal mapping,401/403/503/400, full/partial/empty, defaults/page totals, ordering do U03 sở hữu (scheduledFor desc), no-store. JSON loại name/email/id/archiveRef/hash/key/stack trace. Test invalid query được từ chối, không tới provider.
- [x] Implement tối thiểu; GREEN service/controller/architecture, parse YAML và resolve refs. Real filter-chain/session/current account revocation là tester gate riêng; không lấy standalone MockMvc thay bằng chứng đó.

## Task5 — Dashboard và backup history UI

**Files:** types/api, năm component trong file map; tests `frontend/src/features/admindashboard/api.test.ts`, `AdminDashboardPage.test.tsx`, `BackupHistoryDialog.test.tsx`. Có thể mount route khi phát triển; HomePaths chưa đổi.

**Interfaces:** consumes JSON Task4 và shared apiFetch/session/UI; produces `getAdminDashboard(signal?:AbortSignal):Promise<AdminDashboardView>`, `getBackupHistory(page:number,size:number,signal?:AbortSignal):Promise<BackupHistoryPage>`, named page/components theo FD17/10. Dùng PageHeader/Section/Alert/Table/Pill/Dialog/Pagination sẵn có; Pagination page0-based.

- [x] RED UI/HTTP boundary: loading->data, unavailable khác0,401/403 không giữ metric cũ,503 có retry, remount fetch mới. Không cache/polling. Tài khoản total/breakdown đúng literal fixture, count enrollment không bị gán cho lớp OPEN.
- [x] RED backup states: chưa chạy,disabled,CREATED/RUNNING/FAILED/SUCCESS,purged,startedAt thiếu,summary unavailable; nút Settings `/admin/settings`; không có download/restore/run. Copy retention đúng BR-U03-96, không suy purged theo tuổi. nextPlannedAt ghi dự kiến, không giả actual scheduled job.
- [x] RED dialog: open fetch page0, page2 gửi page1, empty/error/retry riêng; request cũ sau đổi trang/close không ghi đè, reopen lấy mới. AbortController + request identity/cleanup. Dùng shared Dialog và test keyboard/Escape/focus return qua public interactions.
- [x] Implement page chỉ orchestration; presentational component không fetch, history dialog sở hữu pagination/request. JSX text an toàn, không dangerouslySetInnerHTML; testId ổn định cho load/refresh/history/settings/paging/cards. GREEN focused Vitest/lint/typecheck; không viết shared framework hoặc shell/modal mới.

## Task6 — Navigation và Admin homePath

**Files:** app/admin/page.tsx, features/admindashboard/index.ts/register.ts, backend identity/application/HomePaths.java; update `identity/application/AuthServiceTest.java`; add `frontend/src/features/admindashboard/navigation.test.tsx`; mở rộng RoleGuard.test.tsx nếu cần coverage mới.

**Consumes:** page/API Task4/5 và existing AuthenticatedLayout/NavEntry/session. **Produces:** ADMIN_HOME=/admin, các role khác giữ nguyên.

- [x] RED nav duy nhất admin-dashboard href/admin, role ADMIN; không shortcut Manager/Class/học vụ. Guard chặn nonADMIN trước mount/nạp data. homePath ADMIN=/admin; Student/Teacher=/classes và SubjectManager=/manager giữ nguyên; không hard-code lại redirect ở LoginPage/PostLoginRedirect.
- [x] Mount page, reuse app/admin/layout.tsx, giữ register hiện hữu; chỉ sửa comment/export cần thiết. Đổi duy nhất ADMIN_HOME khi dashboard route/API đã pass; không đổi shape response homePath.
- [x] GREEN auth/nav/shell regressions. Real browser login redirect còn trong integration gate Task7. Rollback: đưa ADMIN_HOME về /admin/accounts trước nếu dashboard chưa bàn giao; U15 không có data migration cần đảo.

## Task7 — Verification/review/handoff

**Files:** tạo `aidlc-docs/construction/u15-admin-dashboard/code/implementation-summary.md`; cập nhật plan/state/audit và hướng dẫn U15 trong `aidlc-docs/construction/build-and-test/` khi execution.

- [x] Trước code ghi working-tree/index/source baseline và checks mới. Evidence lịch sử U05 (724backend total/662pass/62skip;232frontend pass) không tính là run U15. Fresh baseline đã chạy lại, logs .tmp/u15/baseline-*.
- [x] Mỗi batch RED->GREEN qua public seam. Focused Maven `-Dtest=edu.aiplatform.admindashboard.**,AdminDashboardPolicyTest,AuthServiceTest,ArchitectureTest test` 49 pass; Vitest `npm --prefix frontend test -- src/features/admindashboard --maxWorkers=1` 21 pass. Không test private helper/format hay mirror implementation.
- [x] Final backend `mvn -f backend/pom.xml verify`; existing launcher/local-cache settings/startup ByteBuddy agent: 755 total/693 pass/62 skip/0 failures/errors, JAR packaged. Exact commands trong build-and-test/u15-verification.md.
- [x] Final frontend full:253 pass/52 files, lint/typecheck/build pass. Process-local TEMP/TMP và NEXT_DIST_DIR scratch; restore tsconfig tự sinh rồi typecheck lại pass. Không đổi dependency/build config để né test.
- [x] Self-review requirement/contract/diff và ArchitectureTest; independent review phát hiện P2 pagination range trước guard, đã tái hiện 3 RED HTTP cases và sửa trong một fix pass, focused/full GREEN. Không mở rộng cleanup sang U05/U09/shared framework. Không commit/push/merge/Jira Done.
- [ ] Tester độc lập: real session/current role/account revoked, audit DENIED persisted, provider real counts/backup/settings/history, fault/timeout/queue full/JDBC release, data freshness/cancellation, không PII/archive. Integration dùng contract-stubs disabled; fixture/context cô lập không chứng minh full startup. Full repository hiện còn prerequisite migration U07/U08 của U09: ghi như gate sẵn có, không sửa trong U15.
- [ ] NFR benchmark nguồn khỏe p95<=1s; fault path deadline2s+overhead riêng; stress provider bỏ qua interrupt và connection/query resource release. Unit tests không chứng minh NFR. Requirement/code/test-case owners phải ba người khác nhau; không tự gán owner hoặc coi review AI là nghiệm thu tester.
- [x] Handoff phân biệt code-ready với full acceptance: chọnA thì C01/C02 vẫn pending; full UC66 Done chỉ sau metadata contract/independent integration/NFR/reviewer acceptance. Defect/fix, limitations và cách chạy lại trong code/implementation-summary.md và build-and-test/u15-verification.md. Hai acceptance gate phía trên chưa hoàn tất.

## Trình tự, checkpoint và rollback

Task1 -> Task2 -> Task3 -> Task4 -> Task5 -> Task6 -> Task7. Mặc định thực thi tuần tự; owner contract handoff không đòi cả Epic provider phải Done. Phương ánB chặn phần Backup đầy đủ cho tới handoff.

Commit checkpoints đề xuất khi user cho phép: backend+OpenAPI+tests; frontend+tests; homePath/navigation; docs/evidence. Không commit/push trong planning. Rollback đổi homePath trước rồi bỏ route/API U15, không động dữ liệu owner.

## AI-DLC stages và planning self-review

- [x] Workspace detection/resume brownfield; giữ audit/state dirty; đối chiếu scaffold/nav và design cũ.
- [x] Tái dùng requirements/story/FD/NFR/infra hiện có; ghi C01–C07 thay vì làm lại toàn bộ inception hoặc thiết kế unit mới.
- [x] Code Generation Part1: file map/interfaces/TDD/checkpoints/DoD; đã duyệt slice A, Part2 và local build/test đã chạy. Full acceptance còn pending.
- [x] Tự review UC66/BR coverage, task interfaces, review-focus tests, source/contract ownership và giới hạn metadata. Kiểm links/path trước bàn giao.
- [x] User duyệt phương án A và execution trực tiếp; Part2 slice A code-ready, metadata U03 và independent integration/NFR acceptance còn pending.

## Extension coverage cho plan

| Rule | Thiết kế kiểm chứng dự kiến, chưa runtime compliance |
|---|---|
| SECURITY03 logging | Chỉ mã lỗi/nguồn an toàn; không raw PII/secret/provider exception; Task1/3/4 |
| SECURITY04 HTTP headers | Kế thừa platform headers/CSP; không nới CORS; no-store thêm riêng endpoint; verify header integration Task4/7 |
| SECURITY05 input | Query page/size whitelist/type/range/overflow; Task4 |
| SECURITY08 access | ADMIN/current account + server principal, guard trước fan-out, deny audit; Task1/4/6 |
| SECURITY09 hardening | No new service/secret/debug/endpoint mutation; platform config giữ nguyên; Task4/7 |
| SECURITY12 auth/credentials | Dùng session U01 và apiFetch; không client actor/role/key; Task1/4/5/6 |
| SECURITY15 safe exceptions | Fail closed auth, partial-unavailable, generic errors; Task1/2/3/4/5 |
| RESILIENCY04 deploy/rollback | Existing CI/deployment; rollback homePath trước, không migration U15; Task6/7 |
| RESILIENCY06 health | Không process mới; kế thừa health hiện tại, không thêm NFR stack; Task7 kiểm startup gate |
| RESILIENCY10 isolation | Pool/queue bounded, deadline2s, rejection/cancellation/partial degradation, query-resource gate; Task2/7 |

Các rule ngoài reduced scope N/A theo state; PBT disabled giữ nguyên. Đây là design/test coverage, không đánh dấu deployed compliance hay acceptance.
