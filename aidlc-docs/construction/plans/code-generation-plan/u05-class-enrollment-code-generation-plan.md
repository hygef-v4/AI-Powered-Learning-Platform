# U05 Class & Enrollment — Code Generation Plan

> **Latest user instruction, 2026-10-18:** "giúp tôi haonf thiện u05" authorizes completing U05 against the existing unit contracts before cross-unit merge. It supersedes the former domain-only scope described below. Current implementation and remaining integration gates: [completion handoff](../../u05-class-enrollment/code/completion-2026-10-18.md); [execution plan](../u05-completion-2026-10-18.md). The original integration checklists below remain historical and are not a statement that current services/controllers are absent.


> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U05 Class & Enrollment thuộc Iter 1, người code: Người 5; UC 20–30. Ngày 2026-10-17 bắt đầu code lát cắt độc lập theo chỉ đạo người dùng; các phần nối U01/U04 chờ sau.

**Primary UC:** 20–30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Thiết kế: [business-logic-model](../../u05-class-enrollment/functional-design/business-logic-model.md), [business-rules](../../u05-class-enrollment/functional-design/business-rules.md), [domain-entities](../../u05-class-enrollment/functional-design/domain-entities.md), [frontend-components](../../u05-class-enrollment/functional-design/frontend-components.md), [NFR](../../u05-class-enrollment/nfr-requirements/nfr-requirements.md), [infrastructure](../../u05-class-enrollment/infrastructure-design/infrastructure-design.md).

## Trạng thái

**Cập nhật 2026-10-18:** Backend độc lập đã được khôi phục từ stash, giữ UI mới theo ZIP; không dùng lại các component UI cũ. Chạy mới backend 123 test (U05 29), frontend 37 test (U05 21), typecheck/lint qua. Checklist S1–S6 và số liệu ngày 17/10 bên dưới là mốc lịch sử; [recovery plan](../u05-stash-recovery-2026-10-18.md) và [recovery summary](../../u05-class-enrollment/code/stash-recovery-2026-10-18.md) ghi bằng chứng hiện tại. Các bước tích hợp vẫn chờ U01/U04/U02.

**Chỉ đạo mới 2026-10-17 (ưu tiên):** Người dùng cho phép code ngay phần U05 không cần U01/U04; chờ người khác hoàn tất rồi mới nối phần phụ thuộc. Đây là approval cho lát cắt độc lập bên dưới, không phải duyệt triển khai toàn bộ tám bước.

### Lát cắt độc lập đã được cho phép

- [x] S1: Domain thuần Java cho lớp, ghi danh, chuyển trạng thái và các điều kiện nội bộ U05; unit test trước implementation.
- [x] S2: Parser danh sách email/CSV một cột, chuẩn hóa, giới hạn 200 dòng/100 KB, trùng dòng; chưa lookup tài khoản hoặc ghi danh thật.
- [x] S3: Repository interface và adapter JDBC chỉ đọc/ghi bảng U05; version và khóa transaction theo môn. Dùng NamedParameterJdbcTemplate (cùng Spring JDBC đã dùng ở U03), thay lựa chọn JPA trong kế hoạch khảo sát; không thêm entity scan khiến schema validation đòi bảng chưa có. Bean repository chỉ bật khi `platform.academics.persistence-enabled=true`; mặc định tắt. Chưa xác minh truy vấn/khóa trên PostgreSQL thật.
- [x] S4: Soạn SQL có FK thật trong `backend/src/main/resources/db/pending/academics/`, ngoài đường quét Flyway; chờ migration accounts/subjects trước khi chuyển sang `db/migration/classes/`. Không tạo bảng giả hoặc giảm kiểm tra FK. Chưa chạy migration thật.
- [x] S5: Component UI thuần nhận props/callback (form thông tin lớp, nút vòng đời, nhập danh sách và bảng kết quả), parser và unit test frontend; chưa tạo route chạy bằng actor/role giả.
- [x] S6: Chạy unit tests, architecture, kiểm frontend, tổng kết bằng chứng và checklist nối U01/U02/U04 trong [code-summary](../../u05-class-enrollment/code/code-summary.md). Backend report tổng 122 test (U05 28), frontend 27 test (U05 11), typecheck/lint và Webpack build qua; Turbopack mặc định lỗi CSS process. Không xác nhận integration, concurrency hay toàn bộ U05 hoàn thành.

Phần để sau: ClassScopeService đầy đủ (cần manager môn/tên tài khoản), ClassView/ClassAdmin/Enrollment application services có authorization/audit, controllers, dashboard/pages/API wiring và RoleGuard. Điều kiện môn ACTIVE/giảng viên đúng role/sinh viên đủ điều kiện phải do các service đó kiểm; domain không giả định U01/U04 đã xác nhận.

Plan đầy đủ theo unit 2026-10-15 chưa được duyệt toàn bộ. Lát cắt độc lập S1–S6 được cho phép riêng ngày 2026-10-17; không kế thừa approval cũ cho những phần tích hợp còn lại.

**Review triển khai 2026-10-17 (trước chỉ đạo mới):** Đã đọc thiết kế U05, stories và contract hiện có, trình bày kế hoạch đầy đủ. Sau đó người dùng chỉ đạo code phần không phụ thuộc U01/U04; checklist S1–S6 ở trên là phạm vi được cho phép hiện tại.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/academics/` | Đã thêm domain và JDBC infrastructure độc lập; application/web chờ tích hợp |
| `backend/.../academics/contract/` | interface `IClassAccess`; kiểu dữ liệu `ClassRef`, `ClassStatus` |
| `backend/.../academics/contract/stub/AcademicsContractStubs` | Bản tạm trả dữ liệu mẫu cho `IClassAccess` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `frontend/src/features/academics/slots.ts` | Slot đã khai báo: `studentClassSections`, `teacherClassTabs`, `studentDetailSections`, `subjectSummarySections`, `classCardSections` |

## Checklist

- [ ] **Bước 1 — Migration:** `db/migration/classes/V20260925_1100__create_classes_enrollments.sql` theo infrastructure-design.
- [ ] **Bước 2 — Domain:** `CourseClass`, `ClassStatus`, `Enrollment`, `EnrollmentStatus`, `EnrollmentSource`, kiểm chuyển trạng thái (BR-U05-14); repository chỉ dùng trong package.
- [ ] **Bước 3 — Interface:** `ClassScopeService` cài `IClassScope` (U01), `ISubjectClasses` (U04), `IClassAccess`; đọc không cache.
- [ ] **Bước 4 — Service:** `ClassViewService` (F1–F6, F8), `ClassAdminService` (F7, F9, F13, F14), `EnrollmentService` (F10–F12, khóa hàng); kiểm quyền qua `IAuthorization`, môn qua `ISubjectQuery`, tài khoản qua `IAccountLookup`; audit theo BR-U05-52; phát `enrollment.activated`.
- [ ] **Bước 5 — API:** 3 controller theo domain-entities mục 5; ngoài phạm vi trả `404`.
- [ ] **Bước 6 — Frontend:** các màn ở frontend-components; vẽ các slot `StudentClassSection`, `TeacherClassTab`, `StudentDetailSection`, `SubjectSummarySection`, `ClassCardSection` đã khai báo trong `src/features/academics/slots.ts`; đăng ký `NavEntry` cho các mục menu bên của U05.
- [ ] **Bước 7 — Unit test (người code viết):** chuyển trạng thái hợp lệ và không hợp lệ, mở lớp thiếu giảng viên, mở lại bị vướng, ghi danh lần đầu, khôi phục, đã ghi danh, ở lớp khác cùng môn, danh sách có dòng lỗi, gỡ rồi mất quyền, đổi giảng viên làm người cũ mất quyền, Admin bị từ chối, phát sự kiện khi lớp `OPEN`; test component frontend. Integration test do tester viết.
- [ ] **Bước 8 — Review:** đối chiếu UC 20–30, story, BR-U05 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Hai yêu cầu ghi danh đồng thời một sinh viên vào hai lớp cùng môn: chỉ một thành công.
- Gỡ sinh viên rồi dùng phiên của sinh viên vào lớp: bị từ chối ngay; đổi giảng viên rồi dùng phiên giảng viên cũ: bị từ chối.
- Thêm theo danh sách 200 email có đủ các loại kết quả dòng.
- Phân quyền 4 role trên mọi endpoint, gồm dùng ID lớp của người khác.

## Kết quả khảo sát ngày 2026-10-17

- [x] Đọc đủ functional/NFR/infrastructure design U05 và story map; U05 hiện hành là Class & Enrollment, UC 20–30.
- [x] Đối chiếu `IClassAccess`, `IClassScope`, `ISubjectClasses`, `IAccountLookup`, `IAuthorization`, `ISubjectQuery`, `IAudit`, `IEventPublisher` và slot frontend.
- [x] Kiểm trạng thái code: `academics` chưa có nghiệp vụ; U01/U04 mới có contract/stub, chưa có migration `accounts`/`subjects`, authentication filter hoặc RoleGuard thật. U02 audit hiện là stub; U03 có publisher thật.
- [x] Xác định chỗ cần bổ sung trong kế hoạch: khóa khi chưa có enrollment, thứ tự khóa khi mở lại lớp, giới hạn kiểm thử khi phụ thuộc chưa được triển khai.
- [x] Người dùng đã duyệt lát cắt S1–S6 bằng chỉ đạo trực tiếp; chưa duyệt kế hoạch tích hợp đầy đủ.

## Chi tiết thực hiện của 8 bước

Phần này bổ sung checklist gốc; không đánh dấu các bước code hoàn thành khi chỉ mới lập kế hoạch.

### Bước 1 — Migration và điều kiện chạy

- [ ] Tạo `backend/src/main/resources/db/migration/classes/V20260925_1100__create_classes_enrollments.sql`, hai bảng đúng domain design, khóa ngoại, unique/index, trạng thái hợp lệ, `version`, và quyền app không DELETE.
- [ ] Giữ FK tới bảng do U01/U04 sở hữu. Không tạo bảng `accounts`/`subjects` giả hoặc bỏ FK để migration U05 chạy riêng. Chỉ xác nhận migration chạy thật sau khi hai migration phụ thuộc có mặt.

### Bước 2 — Domain và lưu trữ

- [ ] Tạo entity, quy tắc chuyển trạng thái, repository interface `Ixxx` tại `backend/src/main/java/edu/aiplatform/academics/domain/`; adapter Spring Data JPA tại `academics/infrastructure/`.
- [ ] Dùng enum `academics.contract.ClassStatus` đã có, không tạo enum trạng thái lớp trùng ở domain. `EnrollmentStatus` và `EnrollmentSource` ở domain.
- [ ] Khóa lạc quan cho mọi sửa lớp; trùng mã lớp và version cũ trả `409`, không trả lỗi SQL ra client.
- [ ] Bổ sung khóa transaction PostgreSQL theo môn trước khi khóa hàng lớp/enrollment và kiểm BR-U05-22. Mọi ghi danh, khôi phục, gỡ, và đổi trạng thái lớp dùng cùng thứ tự khóa. Phương án đề xuất: `pg_advisory_xact_lock` trên khóa ổn định có namespace suy ra từ subject UUID; tuần tự hóa thay đổi ghi danh/vòng đời trong một môn, không thêm bảng khóa. Giới hạn thời gian chờ khóa, không retry vô hạn.

Lý do bổ sung: `FOR UPDATE` chỉ khóa hàng tìm thấy. Với sinh viên chưa có enrollment, hai transaction có thể cùng thấy danh sách rỗng và cùng INSERT vào hai lớp khác nhau; PK `(class_id, account_id)` không chặn được trường hợp này. Mở lại lớp cũng phải tham gia cùng cơ chế khóa. Đây là hiệu chỉnh kỹ thuật được đề xuất để đáp ứng BR-U05-22, chưa được triển khai. Nguồn: [PostgreSQL explicit locking](https://www.postgresql.org/docs/current/explicit-locking.html), mục Row-Level Locks và Advisory Locks. Đổi lại, các thao tác ghi trong cùng môn được tuần tự hóa; cần tester đo batch 200 dòng trước khi xác nhận NFR thời gian.

### Bước 3 — Contract cho unit khác

- [ ] Tạo `academics/application/ClassScopeService.java` cài cả ba interface hiện có; không đổi chữ ký contract nếu chưa cần.
- [ ] Truy vấn quan hệ hiện thời, không cache. `IClassScope` phản ánh quan hệ cả ở lớp DRAFT/ARCHIVED; API Student detail tự chặn lớp không OPEN.
- [ ] Kiểm bean thật thay đúng stub của ba interface; giữ kiểm tra ranh giới unit trong `backend/src/test/java/edu/aiplatform/ArchitectureTest.java`.

### Bước 4 — Application services

- [ ] Tạo `ClassViewService`, `ClassAdminService`, `EnrollmentService` ở `academics/application/`, DTO phù hợp với API và phân trang tối đa 100.
- [ ] Actor chỉ lấy từ principal phía server; chặn sai role, kiểm quyền qua U01 và kiểm quan hệ thật qua dữ liệu lớp/môn. Admin không có quyền thao tác lớp. Không thêm header/query để giả actor.
- [ ] Từ chối khi actor/phụ thuộc thiếu hoặc lỗi; không dùng stub luôn-cho-phép làm bằng chứng phân quyền. Unit test inject mock contract có dữ liệu và quyết định rõ ràng.
- [ ] Batch: kiểm cả giới hạn 200 dòng/100 KB phía server, chuẩn hóa email và phát hiện trùng; lookup U01 một lần. Dùng transaction riêng mỗi dòng qua bean riêng hoặc `TransactionTemplate`, tránh self-invocation làm mất ranh giới transaction. Giữ thứ tự và đúng chín mã kết quả theo BR-U05-24.
- [ ] Audit thành công trong transaction nghiệp vụ qua `IAudit`; truy cập ngoài phạm vi trả `404` và ghi ACCESS_DENIED theo contract audit. Gọi `IEventPublisher` trong transaction; publisher U03 hiện có chịu trách nhiệm gửi sau commit, không tạo cơ chế after-commit thứ hai.

### Bước 5 — API

- [ ] Tạo `StudentClassController`, `TeachingClassController`, `ManagerController` ở `academics/web/` theo toàn bộ API mục 5 của domain design.
- [ ] Dùng `@AuthenticationPrincipal Actor`, Bean Validation, `ApiException` và error handler chung. Kiểm UUID, enum, chuỗi, page/size, version và payload giới hạn; không lộ dữ liệu lớp trước khi kiểm scope.
- [ ] Test controller bằng principal thử nghiệm trong test; không mở public endpoint để demo trong lúc U01 chưa có authentication thật.

### Bước 6 — Frontend

- [ ] Tạo `types.ts`, `api.ts`, các page/component theo frontend design trong `frontend/src/features/academics/`; dùng `shared/api/client.ts`, registry/SlotOutlet đã có. Registry thực tế là `features/academics/slots.ts`, không tạo lại `shared/class-slots` như đường dẫn mô tả cũ.
- [ ] Tạo route mỏng trong `frontend/src/app/classes/`, `app/teaching/classes/`, `app/manager/`, gồm detail, danh sách lớp và My Subject. Dùng API tương ứng của từng dashboard; không lấy role từ query/localStorage.
- [ ] Form tạo/sửa lớp, tìm giảng viên/sinh viên, CSV một cột, bảng kết quả từng dòng, gỡ có xác nhận, mở/lưu trữ/mở lại, phân bố điểm và xử lý version `409`.
- [ ] Render đủ bốn slot đã khai báo; vùng chưa có unit đăng ký không lỗi. Thêm loading/error/empty state, label và `data-testid` ổn định.
- [ ] Tích hợp RoleGuard/session của U01 khi có contract thật. Khi U01 chưa có, chỉ xác nhận component với mock API; route không thể được coi là đã chạy đầy đủ với đăng nhập thật.

### Bước 7 — Kiểm thử và bằng chứng

- [ ] Viết/chạy JUnit/Mockito cho domain, service và controller U05: trạng thái, scope bốn role, version, người dùng bị gỡ/giảng viên cũ, batch từng loại kết quả, audit và publish/rollback.
- [ ] Chạy kiểm kiến trúc và bộ test backend hiện có sau khi tìm runtime Maven phù hợp; Maven hiện chưa có trong PATH của phiên khảo sát.
- [ ] Viết/chạy Vitest component theo flow chính; chạy `npm test`, `npm run typecheck`, `npm run lint`, `npm run build` trong `frontend` và ghi rõ lỗi môi trường nếu có.
- [ ] Bàn giao tester các ca PostgreSQL thực: ghi danh lần đầu đồng thời, mở lại đồng thời với ghi danh, migration/FK/quyền app, transaction audit và phân quyền qua phiên thật. Theo DoD, integration test do tester riêng viết; unit test mock không chứng minh concurrency hoặc p95.

### Bước 8 — Review và tổng kết

- [ ] Tạo `aidlc-docs/construction/u05-class-enrollment/code/code-summary.md` gồm mapping UC/BR, file/API đã làm, kết quả test thật và phụ thuộc còn thiếu.
- [ ] Cập nhật checkbox và `aidlc-state.md` đúng bằng chứng. Không đánh complete U01/U02/U04 hoặc integration của tester trong phạm vi U05; người dùng review U05 trước khi chuyển unit.

## Extension compliance của bản kế hoạch

Đây là đánh giá thiết kế, chưa phải xác nhận runtime compliance.

| Rule đang bật | Trạng thái bản kế hoạch | Cách xử lý |
|---|---|---|
| SECURITY-03 | Đã đưa vào plan | Không log email/PII; audit theo accountId, dùng logging chung |
| SECURITY-04 | Phụ thuộc nền tảng, chưa xác minh | Header HTML thuộc Next.js/gateway chung; kiểm khi triển khai frontend |
| SECURITY-05 | Đã đưa vào plan | Validation và giới hạn request phía server; query tham số hóa |
| SECURITY-08 | Đã đưa vào plan, chờ U01 để xác minh thật | Principal phía server, role và scope; 404 ngoài phạm vi |
| SECURITY-09 | Đã đưa vào plan | Error handler an toàn; không mở route demo công khai |
| SECURITY-12 | Phụ thuộc U01, chưa xác minh | Không thêm credential/session riêng trong U05 |
| SECURITY-15 | Đã đưa vào plan | Fail closed khi actor hoặc U01/U04 không hợp lệ/lỗi |
| RESILIENCY-04 | N/A cho thay đổi triển khai hạ tầng | U05 không thêm service/deployment; migration vẫn phải kiểm với schema phụ thuộc |
| RESILIENCY-06 | N/A cho health endpoint mới | Dùng health của backend hiện có |
| RESILIENCY-10 | Đã đưa vào plan | Giới hạn chờ khóa; lỗi thông báo sau commit theo publisher U03 |
| Các rule ngoài subset | N/A | Theo phạm vi đồ án đã chốt trong aidlc-state.md |
| Property-Based Testing | Disabled | Giữ quyết định hiện có, không hỏi bật lại |

## Duyệt kế hoạch

Xem [u05-implementation-approval-questions.md](u05-implementation-approval-questions.md). Approval bao gồm tám bước, hiệu chỉnh khóa transaction và giới hạn xác minh khi U01/U02/U04 chưa hoàn tất.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
