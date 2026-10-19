# U04 Subject Administration — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U04 Subject Administration thuộc Iter 1, người code: Người 4; UC 16–19. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Thiết kế: [business-logic-model](../../u04-subject-administration/functional-design/business-logic-model.md), [business-rules](../../u04-subject-administration/functional-design/business-rules.md), [domain-entities](../../u04-subject-administration/functional-design/domain-entities.md), [frontend-components](../../u04-subject-administration/functional-design/frontend-components.md), [NFR](../../u04-subject-administration/nfr-requirements/nfr-requirements.md), [infrastructure](../../u04-subject-administration/infrastructure-design/infrastructure-design.md).

## Trạng thái

**Bàn giao frontend18/10/2026:** theo yêu cầu hoàn thiện nốt, F05 edit/cancel/version conflict, F06 manager assign/unassign và F08 status/rejection đã có code +32 test mới, tổng124 test/24 file PASS; typecheck/lint/build, desktop QA hai phiên conflict/edit/unassign/archive/reopen/blocker. Consumer screens/flows đủ UC16–19; bước6 tổng còn gate U01 account-search adapter/RoleGuard/shared session-CSRF. Backend không đổi/chạy lại; không mark toàn U04/DoD complete bằng fixture. Hướng dẫn bàn giao `../../u04-subject-administration/code/frontend-handoff.md`.

**F04 consumer ngày2026-10-18:** SubjectFormDialog/ManagerPicker/managerSearch boundary và nút thêm/list refetch đã có,92 frontend test PASS, typecheck/lint/build PASS. Shared Dialog thêm focus/scroll/khóa đóng có regression tests; browser QA xác nhận success/refetch/return focus. F04 tổng còn adapter U01 account search/schema chưa bàn giao; default báo unavailable, tạo không manager vẫn gửi null, không gọi endpoint đoán hoặc giả accounts. Backend không đổi/không chạy lại tests; các gate auth/provider cũ giữ pending. Xem `../../u04-subject-administration/code/create-form-review.md`; tiếp theo F05 edit metadata.

**Frontend đọc/security consumer ngày2026-10-18:** hai route, F03 list và F07 classes readonly đã có; detail info/manager/meta là phần đọc của F05/F06. Frontend70 test PASS, typecheck/lint/build PASS; desktop QA qua fixture loopback, không phải provider thật. Bổ sung matrix cả7 endpoint: target42 PASS gồm39 HTTP consumer và3 architecture; không chạy lại full DB, full271/31 PostgreSQL bên dưới là lượt B11 trước. F02 RoleGuard/B12 auth thật vẫn pending; không dùng layout hard-code ADMIN làm guard. F04–F06/F08 forms/mutations còn lại; toàn bước6/7 và DoD chưa complete. Xem `../../u04-subject-administration/code/frontend-read-review.md`.

**B11 ngày2026-10-18:** bảy REST endpoints và DTO parser/snapshot mapper/scoped error advice đã có,32 MockMvc consumer cases pass. Toàn Maven271 pass,31 PostgreSQL16.15,0 lỗi/skip. Mark bước5 API ở cấp code/developer checks; chưa có HTTP+JWT/CSRF/provider thật, B12 và DoD/integration gates vẫn pending. Strict request validation không đổi Jackson/security chung. Hướng dẫn `../../u04-subject-administration/code/api-review.md`.

**Hướng tiếp tục được người dùng yêu cầu 2026-10-18:** tiếp tục hoàn thiện code-generation-plan U04 theo các phần review được, không chờ toàn bộ U01/U02/U05. Code/test phần consumer có thể làm trước với boundary/test provider; gate schema/provider thật, JWT/session, NFR và tester độc lập vẫn cần bằng chứng riêng. Không biến thiếu bàn giao thành fallback allow hoặc đánh toàn unit/DoD complete bằng fixtures.

**B10 consumer tiếp theo:** ACCESS_DENIED đã nối ở helper dùng chung cho đọc/ghi/status; success hooks giữ transaction, denied tạm dừng caller và ủy thác tx riêng cho U02. Maven239 test pass,31 PostgreSQL16.15,0 lỗi/skip;9 unit audit và2 DB fixture case mới. Chưa có provider U02 thật hoặc chống trùng audit với U01, Q14 còn pending. Bước tiếp theo là B11 API; xem `../../u04-subject-administration/code/audit-review.md`.

**B09 core ngày2026-10-18:** SubjectStatusService và ISubjectMutationGuard/SubjectMutationGuard đã có; thêm ISubjectArchiveCheck riêng chờ U05 provider, không đổi signature ISubjectClasses. Archive cùng thread/transaction READ_COMMITTED, khóa môn rồi kiểm lớp với remainingBudget, fail closed khi thiếu provider. Reopen không đổi lớp; thêm hai status success audit hook. Toàn Maven228 pass,29 PostgreSQL16.15,0 lỗi/skip;22 unit+1 wiring+8 DB case mới. Race fixture/SQL timeout/rollback là developer evidence, chưa phải U05 writer/provider thật hoặc durable audit U02. B09/B10/full service step vẫn pending; xem `../../u04-subject-administration/code/lifecycle-review.md`.

**B08 command core ngày2026-10-18:** SubjectCommandService tách khỏi service đọc; SubjectAdminAccess dùng chung; thêm success audit hook MANDATORY và account gate nội bộ fail closed khi thiếu provider. Clean test197 pass,21 PostgreSQL case,0 failures/errors/skipped;25 command test,1 wiring mới và5 DB case mới. Chưa có account guard thật/disable auto-unassign U01, durable atomic audit U02, status command hoặc HTTP controller; B08/B10 và toàn bước4 vẫn chưa hoàn thành. Xem `../../u04-subject-administration/code/command-review.md`.

**B07 ngày2026-10-18 sau người dùng cho tiếp tục:** đã có service đọc Admin F1/F3 và DTO list/detail/classes, kiểm quyền trước DB, dedup contact và classes deadline1s;166 test backend pass,16 PostgreSQL,0 lỗi/skip. Service F2/F4/F5/F6, audit denied/success, controller và integration auth/provider thật chưa xong; không đánh hoàn thành toàn bước4. Tên action consumer SUBJECT_LIST/SUBJECT_VIEW cần đối chiếu U01, chưa đo hiệu năng. Hướng dẫn: `../../u04-subject-administration/code/admin-read-review.md`.

**B06 tiếp theo 2026-10-18, sau người dùng review persistence:** đã cài SubjectQueryService/SubjectScopeService và kiểm bean thật thay stub theo interface. Thêm14 unit/wiring test và1 PostgreSQL case; toàn backend144 test pass,16 case DB,0 skip/failure/error. ISubjectClasses placeholder U04 đã chuyển sang báo không khả dụng theo Q05; không sửa U01/U05 implementation hay contract signature. Bước3 hoàn thành ở cấp code/developer checks, chưa phải provider/JWT/lifecycle integration hoàn chỉnh. B03 vẫn chờ accounts migration thật của U01.

**Lát cắt tiếp theo 2026-10-18, sau người dùng review và yêu cầu tiếp tục:** đã tạo migration subjects và adapter JPA/query, thêm15 test persistence chạy PostgreSQL16.15 thật; toàn backend129 test pass,0 skip/failure/error. B05 hoàn thành ở cấp implementation/developer checks. Migration U01 chưa có trong checkout nên B03 chỉ hoàn tất SQL/kiểm riêng với fixture accounts, chưa đánh x bước1 hoặc gate migration toàn hệ thống. Không triển khai service/API hay scope/guard U04 trong lượt này; chưa sửa U01/U05 hoặc pom.

- [x] Persistence B05: SubjectJpaEntity, ISubjectJpaRepository, SubjectRepositoryAdapter; public/storage version, search/page/count, unique/FK/grants, rollback và concurrency kiểm trên PostgreSQL.

**Cập nhật 2026-10-18:** người dùng yêu cầu bắt đầu sau khi rà kế hoạch tự triển khai và Jira song song. Chỉ triển khai lát cắt B04/F01 của kế hoạch tự triển khai ban đầu (đã dọn; quyết định được giữ tại implementation-decisions.md) để review. Domain có20 lượt test pass; frontend foundation có15 lượt test pass, typecheck/lint pass. Toàn backend114 và frontend49 test pass. Đây không phải duyệt/hoàn thành mọi bước U04; trạng thái khung bên dưới là mô tả trước lần triển khai này. DTO web và API client chưa tích hợp server; phần frontend components vẫn chưa triển khai.

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/subjects/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../subjects/contract/` | interface `ISubjectClasses`, `ISubjectQuery`; kiểu dữ liệu `SubjectClassSummary`, `SubjectRef`, `SubjectStatus` |
| `backend/.../subjects/contract/stub/SubjectsContractStubs` | Query thật thay placeholder; ISubjectClasses chưa có provider thì báo unavailable theo Q05, không giả0/rỗng |
| `backend/.../subjects/application/` | Query/scope, đọc/command/status services và năm success hooks + ACCESS_DENIED; account guard/archive provider/Q14 chờ tích hợp |
| `backend/.../subjects/web/` | B11 bảy endpoint, request whitelist/type/null parsing, snapshot mapping và scoped ProblemDetail advice; real JWT/filter-chain integration pending |
| `frontend/src/features/subjects/` | F01 types/API/validation; F03 list và F07 classes readonly; detail info/manager/meta. Forms/mutations và RoleGuard pending |

## Checklist

- [ ] **Bước 1 — Migration:** `db/migration/subjects/V20260925_1000__create_subjects.sql` đã viết/kiểm PostgreSQL riêng; còn nhận accounts migration thật U01 và kiểm toàn chuỗi ứng dụng theo infrastructure-design, không coi fixture accounts test là U01 hoàn thành.
- [x] **Bước 2 — Domain:** package `edu.aiplatform.subjects`: `Subject`, dùng lại `contract.SubjectStatus`, `SubjectCode` (kiểm định dạng), `ISubjectRepository` dùng trong package. Triển khai/test 2026-10-18; chưa có adapter persistence hay kiểm liên unit.
- [x] **Bước 3 — Interface:** `ISubjectQuery`, `SubjectRef`; contract `ISubjectClasses`, `SubjectClassSummary`; `SubjectScopeService` cài `ISubjectScope` của U01; `SubjectQueryService`. Query/scope thật và backoff của stub đã kiểm; tới khi U05 cài, placeholder `ISubjectClasses` báo `SUBJECT_CLASSES_UNAVAILABLE`, không giả rỗng/0 theo Q05. Bean provider U05 sẽ thay placeholder theo ConditionalOnMissingBean; chưa xác nhận U05 implementation thật.
- [ ] **Bước 4 — Service:** `SubjectAdminService` đọc, `SubjectCommandService` ghi và `SubjectStatusService` lifecycle cho F1–F6: kiểm `ADMIN`, mã/manager, version, chặn archive qua `ISubjectArchiveCheck` cùng khóa/transaction; `ISubjectClasses` giữ cho hiển thị. B07/B08/B09 core đã có developer checks; còn provider account guard/Q14, U05 checker/writers, denied/durable audit và tích hợp thật.
- [x] **Bước 5 — API:** `SubjectAdminController` cho7 endpoint, SubjectRequests/SubjectResponses/SubjectExceptionHandler;32 HTTP consumer cases và full backend pass. Completion ở cấp code/developer checks, không thay B12 JWT/CSRF/provider integration hoặc tester độc lập.
- [ ] **Bước 6 — Frontend:** `SubjectListPage`, `SubjectFilters`, `SubjectTable`, `SubjectFormDialog`, `ManagerPicker`, `SubjectDetailPage`, `SubjectEditForm`, `ManagerCard`, `ManagerChangeDialog`, `SubjectMetaCard`, `SubjectStatusActions`, `ArchiveRejectedDialog`, `SubjectClassesTable`; đăng ký `NavEntry` mục Môn học.
  - [x] Hai route + F03 danh sách và F07 lớp chỉ đọc, detail info/manager/meta; component tests và build đã pass. Giữ nav hiện có.
  - [x] F04 form thêm/ManagerPicker consumer và test; safe error/pending, refetch sau success. Không phải tích hợp U01 thật.
  - [x] F05 SubjectInfoTab/SubjectEditForm, F06 ManagerChangeDialog, F08 SubjectStatusActions/ArchiveRejectedDialog; một version chung, safe errors/conflict/reload/pending và tests.
  - [ ] RoleGuard thật U01; F04 adapter account search; shared JWT/session/CSRF và tích hợp provider thật. Tài liệu bàn giao đã tách rõ code frontend hoàn tất và các gate còn lại.
- [ ] **Bước 7 — Unit test (người code viết):** mã sai định dạng và trùng, Chủ nhiệm môn sai role hoặc không `ACTIVE`, version lệch, lưu trữ khi còn lớp hoặc khi U05 không trả lời, đổi Chủ nhiệm môn làm người cũ mất quyền, người không phải Admin bị từ chối; test component frontend. Integration test do tester viết.
- [ ] **Bước 8 — Review:** đối chiếu UC 16–19, US-CAT-001, BR-U04 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Tạo môn với mã trùng, mã sai định dạng; gán người không phải Subject Manager.
- Đổi Chủ nhiệm môn rồi dùng phiên của người cũ thao tác tài nguyên môn: bị từ chối ngay.
- Lưu trữ môn còn lớp đang mở bị từ chối; lưu trữ rồi mở lại.
- Người không phải Admin gọi từng endpoint quản trị môn.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
