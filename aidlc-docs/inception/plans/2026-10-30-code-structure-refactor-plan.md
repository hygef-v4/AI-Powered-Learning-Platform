# Kế hoạch refactor cấu trúc code — 2026-10-30

> **Đã thay thế theo yêu cầu mới nhất:** chỉ chuyển file/package và sửa tham chiếu. Các phần tách service/editor, thêm public exports, batch lookup, xóa Admin MCP và thêm test/guard trong bản rộng dưới đây đã được hoàn tác. Đây là hồ sơ đề xuất/thực thi cũ; [phạm vi và kiểm tra hiện tại](../../construction/build-and-test/2026-10-30-package-moves-only-results.md).

**Trạng thái:** Đã được người dùng duyệt triển khai và đã thực hiện trên working tree; xem [kết quả và giới hạn](../../construction/build-and-test/2026-10-30-code-structure-refactor-results.md).

**Mục tiêu:** Tìm được controller, service, repository, API, kiểu dữ liệu và component bằng một quy ước ngắn; giữ nguyên hành vi hệ thống.

**Nguồn:** [audit kèm vị trí source](../reverse-engineering/2026-10-30-code-structure-audit.md), [requirements](../requirements/requirements.md), [unit ownership](../application-design/unit-of-work.md), [decision register](../reverse-engineering/decision-register.md). Baseline source HEAD `5a76e428`.

## 1. Phạm vi yêu cầu và lựa chọn

Yêu cầu đã rõ: review cả frontend/backend và AI-DLC; muốn cách chia quen thuộc như controller/service/repository, API/model/component; ưu tiên dễ đọc và ít tầng. Kế hoạch này không bổ sung tính năng, đổi API/DB, nâng version hoặc áp một kiến trúc mới.

| Cách tổ chức | Lợi ích | Chi phí | Đề xuất |
|---|---|---|---|
| Chỉ giữ tầng hiện tại, thêm hướng dẫn | Ít file di chuyển nhất | Vẫn có SQL ở `domain`, hai quy ước repository | Không giải quyết đủ yêu cầu |
| Giữ feature nghiệp vụ, chuẩn hóa tầng bên trong | Dễ tìm theo cả nghiệp vụ lẫn loại công việc; giữ ownership | Cần chuyển package/import/test/docs theo từng feature | **Chọn làm đề xuất** |
| Gom toàn backend vào controller/service/repository chung ở root | Tên tầng rất quen | Các phần cùng một UC xa nhau, ranh giới 15 unit khó giữ | Không phù hợp code hiện tại |

Lựa chọn này được người dùng duyệt qua yêu cầu triển khai theo kế hoạch.

## 2. Cấu trúc đích

### Backend: feature trước, tầng sau

Ví dụ `edu.aiplatform.subjects`:

| Package | Nội dung |
|---|---|
| `controller` | REST controller, HTTP request parsing/response mapping, exception handler |
| `service` | Use case, phân quyền, điều phối transaction, audit và contract implementation |
| `repository` | Repository interface đang có, JDBC/JPA implementation, persistence entity/row mapper |
| `model` | Đối tượng nghiệp vụ, enum/value object nội bộ, quy tắc thuần không phụ thuộc SQL |
| `dto` | DTO request/response hoặc kết quả use case đã tách thành kiểu riêng |
| `contract` | Interface/DTO xuyên unit hiện có; giữ nguyên package và chữ ký |

Chỉ feature cần mới có `config`, `integration` hoặc `security`: cấu hình bean, adapter provider/Drive/Rabbit/SMTP/Redis, filter/JWT. Không tạo các folder rỗng cho đủ mẫu. Job handler/scanner điều phối nghiệp vụ ở `service`; broker listener/sender ở `integration`; topology/bean config ở `config`.

Hướng đọc thông thường: Controller gọi Service; Service gọi Repository, model và contract của unit khác. Repository dùng model và persistence mapping. Unit khác chỉ gọi `contract`, không đọc repository/bảng của nhau. `shared` chỉ giữ hạ tầng dùng chung có thật; composition root bảo mật được ghi rõ như ngoại lệ hiện có, không đổi dây nối lúc chuyển package.

**Không thêm:** `BaseController`, `BaseService`, `BaseRepository`, generic CRUD, `IxxxService`/`xxxServiceImpl` cho mọi lớp, mapper framework, CQRS framework hoặc microservice. Repository interface hiện hữu được giữ; concrete JDBC repository không bắt buộc thêm interface.

### Frontend: feature có nhóm file theo công việc

Ví dụ `src/features/subjects`:

| Đường dẫn | Nội dung |
|---|---|
| `api/index.ts` | Hàm gọi HTTP; chuyển từ `api.ts`, giữ được đường import kết thúc bằng `/api` |
| `model/types.ts`, `model/validation.ts` | Kiểu dữ liệu, validation/formatting thuần của feature |
| `pages/SubjectListPage.tsx`, `pages/SubjectDetailPage.tsx` | Component màn hình điều phối dữ liệu và UI |
| `components/` | Table, filter, card, form, dialog |
| `hooks/` | Logic React có vòng đời/state, như `useSubjectRead` |
| `index.ts`, `slots.ts`, `register.ts` | Public exports nhỏ, slot và đăng ký tích hợp |

- Test đi cùng file được kiểm tra, không tạo cây test riêng lặp lại toàn feature.
- `src/app/**/page.tsx` và `layout.tsx` giữ URL, Next.js route và vai trò ghép màn hình. `features/*/pages` là component màn hình, không là một Next Pages Router thứ hai.
- Giữ `shared/api`, `shared/ui`, `shared/document`. Chỉ chuyển vào `shared` khi có ít nhất hai nơi dùng và cùng một ý nghĩa; không gom business logic vào `utils`.
- Folder chỉ có khi có code cần đặt; giữ subgroup `identity/otp`, `identity/profile`, `ai/admin` nếu chúng giúp tìm việc. Không buộc mọi feature thành bản sao cứng của Subjects.
- Public export chỉ gồm API/type/UI thật sự có consumer ngoài feature; không `export *` toàn bộ file và không nạp `register.ts` từ barrel. Giữ type-only imports, client/server boundary và đăng ký side effect tại chỗ hiện có.

## 3. Bảng chuyển file mẫu đủ để làm đợt đầu

Các đường backend dưới đây tính từ `backend/src/main/java/edu/aiplatform/subjects`; frontend từ `frontend/src/features/subjects`.

| Hiện tại | Đích | Ghi chú |
|---|---|---|
| `web/SubjectAdminController.java` | `controller/SubjectAdminController.java` | Endpoint và annotation giữ nguyên |
| `web/SubjectRequests.java`, `SubjectResponses.java`, `SubjectExceptionHandler.java` | `controller/` | HTTP parsing/response helper ở gần controller |
| `application/Subject*Service.java` | `service/` | Giữ tên và vai trò hiện có |
| `application/SubjectAdminAccess.java`, `SubjectAuditRecorder.java`, `SubjectClassesReader.java`, `SubjectMutationGuard.java`, `SubjectLifecycleBudget.java` | `service/` | Giữ lock/timeout/transaction collaborators |
| `application/SubjectAdminViews.java` | `dto/SubjectAdminViews.java` | Giữ record/field/JSON; không tách mỗi nested record một file |
| `domain/ISubjectRepository.java` | `repository/ISubjectRepository.java` | Cùng repository implementation |
| `infrastructure/SubjectRepositoryAdapter.java`, `ISubjectJpaRepository.java`, `SubjectJpaEntity.java` | `repository/` | Giữ persistence helper cùng package để không mở visibility tùy tiện |
| `domain/Subject.java`, `SubjectCode.java` | `model/` | Không gộp Subject domain với JPA entity |
| `contract/**` | Giữ nguyên | Bao gồm `SubjectStatus` đã là kiểu public |
| `api.ts`, `api.test.ts` | `api/index.ts`, `api/index.test.ts` | Giữ API exports; mock/import resolve cùng module |
| `types.ts`, `validation.ts`, `validation.test.ts`, `managerSearch.ts` | `model/` tương ứng | Cập nhật consumer; không sửa thuật toán |
| `SubjectListPage.tsx`, `SubjectDetailPage.tsx` và test | `pages/` | Route import qua public exports sẵn có |
| `useSubjectRead.ts`, `subjectMutation.tsx` | `hooks/` | Chỉ chuyển chỗ ở đợt này |
| Table/filter/card/form/dialog/tab và test tương ứng | `components/` | Bao gồm Subject/Manager component còn lại |
| `index.ts`, `register.ts` | Giữ ở root feature | Chỉ sửa export/import cần thiết |

Đồng thời chuyển test Java sang package phản chiếu vị trí mới. Giữ package-private access bằng bố trí đúng collaborator/test; không đổi tất cả thành `public` chỉ để compile. Record command đang lồng trong service tiếp tục ở đó khi chỉ phục vụ use case đó.

## 4. Các hành vi phải giữ

1. HTTP path/method/status, JSON shape, null/unknown khác 0/rỗng, paging và error code. Không thống nhất paging U04 từ 0 với U05 từ 1 trong refactor này.
2. Cookie/CSRF/refresh, current-role và object authorization; 403/404 và thứ tự validation/guard. Không cache quyền ngoài request.
3. Lock order, optimistic version/CAS, isolation và `REQUIRES_NEW` từng dòng batch; archive môn/lớp cùng transaction. Không bỏ lần recheck sau khóa.
4. AI reservation commit trước provider call; proposal bean/transaction riêng; snapshot/hash/version; retry/idempotency, unknown usage và audit.
5. Worker terminal persistence/retry handoff trước ACK; backup retention sau commit; email/outbox theo cơ chế hiện có. Không “đơn giản hóa” bằng bắt rồi nuốt exception.
6. Student MCP UC41, Admin bị chặn, token READ_ONLY không ghi; public contract giữ nguyên. Không xóa endpoint Admin web AI/dashboard cùng code MCP dư.
7. Autosave, stale response, abort/unmount, focus/caret, undo/redo, khóa block người soạn, Draw.io XML và snapshot đã nộp.

## 5. Trình tự thực hiện đề xuất

Mỗi mục là một đợt có diff riêng; chỉ commit/push khi được yêu cầu. Ưu tiên làm trực tiếp, tuần tự theo lựa chọn trước của người dùng về không dùng subagent. Không cần đổi worktree cho lượt viết kế hoạch này.

### Đợt 0 — Chốt baseline và guard trước khi chuyển package

- [x] Ghi HEAD/working tree, dùng dependency versions đã khóa. Làm cho Maven resolve được dependency; không coi lỗi offline cache là test fail của ứng dụng.
- [x] Chạy backend `mvn verify` và script required DB guard; ghi pass/fail/skipped đúng thực tế. Frontend baseline mới lượt audit đã có 599 pass, lint/typecheck pass; chạy lại nếu source hoặc môi trường liên quan thay đổi.
- [x] Sửa `backend/src/test/java/edu/aiplatform/ArchitectureTest.java` để bảo vệ **toàn bộ internals của từng package unit**, không chỉ tên `application/domain/infrastructure/web`. Giữ map nhiều package cùng unit và ngoại lệ composition root đã có.
- [x] Bổ sung fixture kiểm guard bắt được một import sai qua `controller/service/repository/model/dto`; không để `allowEmptyShould(true)` che việc mất toàn bộ đối tượng kiểm khi đổi tên tầng. Chạy guard với cả tên cũ/mới trước khi di chuyển.

Điều kiện xong: baseline được phân biệt rõ với historical evidence; architecture guard không mất tác dụng khi package đổi tên. Không viết thêm test chỉ xác nhận tên thư mục.

### Đợt 1 — Dọn hai implementation Admin MCP hết scope

- [x] Bỏ `ai/application/AdminAiMcpTools.java`, `admindashboard/application/AdminDashboardMcpTools.java` và positive test `AdminAiMcpToolsTest.java` sau khi kiểm lại refs tại HEAD thực hiện.
- [x] Trong `McpStaffCatalogTest`, thay hai provider thật bằng provider giả tối thiểu cho assertion Admin bị chặn; giữ toàn bộ matrix role/token và schema assertions của tool còn hỗ trợ.
- [x] Giữ `McpInput.admin` public helper và toàn bộ contract khác ở đợt này; không xóa API contract chỉ vì hiện không còn caller trong repo.
- [x] Chạy `McpStaffCatalogTest`, `McpStaffAdmissionTest`, `McpCredentialServiceTest`, `McpServerTest`, `McpRolePolicyTest`, `McpPolicyTest`, `PracticeFeedbackMcpParityTest` và architecture guard.
- [x] Cập nhật inventory hiện hành có nhắc hai class; giữ lịch sử requirement/approval 26/10.

Điều kiện xong: bốn tool Admin không có implementation dư; deny Admin và Student feedback vẫn được kiểm, Admin web AI/dashboard giữ nguyên.

### Đợt 2 — Làm mẫu Subjects, rồi dừng đánh giá độ dễ đọc

- [x] Frontend: chuyển theo bảng ở mục 3, cập nhật relative import, route exports, test mocks. Không tách logic hoặc đổi UI cùng diff.
- [x] Chạy `npx vitest run src/features/subjects`, `npm run typecheck`, `npm run lint` và production build nếu module graph/client boundary đổi.
- [x] Backend: chuyển Subjects theo bảng; cập nhật Java test packages, explicit scanning/reflection/config references nếu có. Giữ bean name và public contracts.
- [x] Chạy `ArchitectureTest,Subject*Test`; yêu cầu `SubjectRepositoryIntegrationTest` chạy PostgreSQL thật, không chỉ skip rồi báo xanh.
- [x] Cập nhật README và link source hiện hành của U04 trong cùng đợt.

Điều kiện xong: tìm được Subject API, page, controller, service, SQL và model bằng quy ước mới; UC16–19, version/rollback và unknown class count vẫn đúng. Nếu mẫu thêm thao tác điều hướng mà không giúp tìm code, giảm folder trước khi nhân rộng.

### Đợt 3 — Nhân rộng bằng các đợt chuyển chỗ riêng

Không có dependency deployment mới vì giữ nguyên contract/API. Thứ tự dưới đây theo độ phức tạp và vùng liên quan, không là DAG nghiệp vụ bắt buộc.

| Nhóm đợt | Phạm vi | Gate cần chú ý |
|---|---|---|
| A | U02 `audit`, phần `settings` U03, U14 `announcements/notifications`, U15 `admindashboard` | DTO/audit filters, email recipient/idempotency, required Notification PostgreSQL suite |
| B | U05 `academics` | Scope, null teacher, per-row enrollment transactions, U04–U05 archive contract |
| C | U07 `assignments`, U08 `assignmentwork`, U09 `grading` | Snapshot/version, autosave, rubric, proposal transaction và Student MCP parity |
| D | U10 `quizzes`, U11 `quizpractice`, U13 `materials` | Publication history, retry eligibility, module ownership và Quiz/Materials imports |
| E | U12 `groups/groupdocs` | Section ownership, stream, leader submit, CAS/resubmission và auto-submit |
| F | U01 `identity`, U06 `ai`, phần `files/jobs/backup` U03 | Security filter composition, config/adapter wiring, secrets, quota/locks/ACK/retention |

Mỗi hàng có thể chia thành frontend rồi backend từng feature; không gom cả hàng thành một diff lớn. Test Java đi cùng package mới; giữ `contract/**`, Flyway paths/versions, JSON schema, provider prompt và URL. Giữ interface prefix `I` theo quy ước repo, không đổi tên mọi interface trong đợt chuyển package.

### Đợt 4 — Tách đúng các phần đang ôm nhiều việc

Các mục độc lập, làm từng mục; không phải điều kiện để gọi việc sắp xếp thư mục đã xong.

| Việc | File đích dự kiến | Bảo vệ hành vi |
|---|---|---|
| Tách document editor | Giữ `shared/document/DocumentEditor.tsx`; thêm `editor/blockTransforms.ts`, `editor/BlockEditor.tsx`, `editor/EditorToolbar.tsx` khi extraction có boundary đủ nhỏ | Dùng lại `document.test.tsx`; bổ sung test ở hành vi chưa bảo vệ: undo/redo sau format, caret/list split, bảng người soạn trong student mode, ảnh/diagram và readOnly |
| Tách nguồn và vòng đời draft | `features/assignments/hooks/useAiDraftSources.ts`, `useAssignmentAiDraft.ts`; dialog/preview ở `components` sau đợt chuyển chỗ | Test hiện có `AiDraftDialog.test.tsx`; kiểm thêm abort/unmount và apply dùng version mới nếu coverage chưa có |
| Tách U05 theo công việc | `academics/service/AcademicsQueryService.java`, `ClassService.java`, `EnrollmentService.java`; guard/collaborator chỉ khi cần | Giữ assertion `AcademicsServiceTest`, `AcademicsAccessAndReadTest`, `AcademicsBatchTransactionTest`, HTTP contract và adapter tests; tên test có thể đổi theo owner mới |
| Tách xử lý document backend | Giữ `DocumentModelService` là `IDocumentModel` entry; chuyển validate/sanitize vào `DocumentValidator.java`, `DocumentSanitizer.java` trong `assignments/service/document` | Dùng `DocumentModelServiceTest`; giữ thứ tự/giới hạn validation issues, sanitize/serialization và plain-text output; không nhân bản `PartCalculator`/`DiagramXml` |

Đích DocumentModelService còn là entry vì nó thực sự triển khai contract nhiều consumer, không tạo thêm tầng chuyển tiếp cho tất cả service. Không tách `GradingProposalTransactions`, command/query/timeout guard đã có chỉ để đạt số dòng mục tiêu.

Với logic rủi ro còn thiếu coverage: viết characterization test qua hành vi ổn định, chạy trên code nguyên trạng trước khi tách. Tái dùng test đủ tốt; không viết test mirror implementation hoặc thay assertion lỗi bằng assertion chung để dễ pass.

### Đợt 5 — Thu gọn phụ thuộc frontend và tối ưu N+1 riêng

- [x] Công khai API/types thật sự dùng ngoài ở `quizzes/api`, `ai/api` hoặc export nhỏ có tên rõ; công khai Module dialog/navigation cần dùng ngoài ở `materials/index.ts`. Consumer dùng public path; tránh barrel tự import feature đăng ký.
- [x] Chuyển import Quiz/Materials trong cùng đợt, giữ module outlines, tạo/copy/archive Quiz, role và side effect. Không kết luận cần thêm slot mới nếu public exports đã giải quyết được việc chuyển file.
- [x] Tối ưu đọc U05 sau khi đã tách query owner: batch contacts bằng `IAccountLookup.getContacts`, memo subject trùng trong request. Không đọc bảng unit khác hoặc thêm distributed cache.
- [x] Bảo vệ DTO/thứ tự/error khi thiếu contact/subject; test trang có teacher/môn trùng và trang sinh viên. Kiểm số lần truy vấn ở boundary lookup bổ sung cho assertion kết quả, không dùng call count thay kiểm hành vi.
- [x] Chạy full backend verify + DB guard; full frontend tests/lint/typecheck/build ở trạng thái cuối. Browser smoke/E2E theo role chưa chạy; giới hạn này được ghi rõ trong báo cáo kết quả.

Điều kiện xong: public imports ổn định, lookup không lặp theo từng dòng cho cùng đối tượng, toàn bộ gate áp dụng có kết quả và giới hạn rõ ràng.

## 6. Đồng bộ AI-DLC cùng code

- Giữ 15 unit, 66 UC, FR/BR IDs và bảng ownership. Đổi package nội bộ không phải đổi yêu cầu nghiệp vụ.
- Sau từng feature, sửa link hiện hành trong `inception/reverse-engineering/current-code-baseline.md`, `application-design/services.md`, API inventory nếu nó ghi đường controller, `construction/<unit>/code/as-built-baseline.md` và code/test plan còn hiệu lực.
- Cập nhật README quy ước mới và manifest hiện hành; tìm refs package cũ ở tests, reflection/config, scripts và docs. Các kết quả có ngày cũ giữ nguyên như lịch sử; cung cấp liên kết từ inventory mới thay vì viết lại bằng chứng cũ thành test mới.
- Giữ tên folder Construction lịch sử như `u02-audit-job-and-outbox`; không đổi cả cây AI-DLC chỉ để làm tên code giống tài liệu. Kiểm link local sau từng đợt.
- State/audit ghi scope và kết quả thật; không đánh dấu 15 unit được acceptance lại chỉ vì build pass.

## 7. Kiểm thử và tiêu chí hoàn thành

Từ `backend`: `mvn verify`. Từ root: `./scripts/assert-required-backend-tests.ps1`. Từ `frontend`: `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`. Dùng Java 17/Node 22 của CI cho gate cuối khi có thể; môi trường audit hiện tại Java 21/Node 25 khác CI.

- [x] Chuyển package có guard bảo vệ cả tên cũ/mới; không còn cross-unit import internals mới.
- [x] Frontend feature lớn có nhóm API/model/pages/components/hooks rõ; không có thư mục/layer rỗng.
- [x] Concrete repository và interface/adapter được đặt đúng vai trò, không phát sinh interface/Impl hay mapper layer vô ích.
- [x] Test giữ observable behavior; mọi lỗi có trước, skip và giới hạn chạy được ghi rõ. Không thay full suite bằng lint hoặc một vài mock tests.
- [x] Diff chuyển file tách khỏi extraction, cleanup và tối ưu; không đổi API/DB/feature trong cùng đợt.
- [x] Link source trong tài liệu hiện hành đúng, lịch sử còn nguyên, không có dependency mới.

Rollback từng đợt bằng hoàn tác patch/commit tương ứng, không reset working tree chứa thay đổi của người dùng. Kế hoạch không có migration mới nên không đòi rollback schema; giữ cơ chế deploy/tag đang có, không chọn chiến lược triển khai mới.

## 8. AI-DLC và extension trong phạm vi kế hoạch

| Stage/control | Xử lý |
|---|---|
| Workspace detection | Đã xác định brownfield, source baseline và working tree |
| Reverse engineering | Tái dùng inventory hiện hành; bổ sung audit cấu trúc, không sinh lại toàn bộ hệ thống |
| Requirements / design / workflow | Yêu cầu và hướng tổ chức đã được người dùng duyệt triển khai qua kế hoạch này |
| User stories / units generation / infrastructure design | Đề xuất bỏ qua: không có user flow, unit, schema hoặc deploy mới |
| Code generation / build-test | Đã thực hiện sau approval, có checkpoint từng phần và full verification cuối; xem báo cáo kết quả |
| SECURITY-03/04/05/08/09/12/15 | Kế hoạch giữ logging/header/validation/auth/errors/secrets; đây là ràng buộc triển khai, chưa phải chứng nhận runtime. Dọn Admin tool vẫn giữ deny tests |
| RESILIENCY-04/06 | Không đổi deploy/rollback hoặc healthcheck; yêu cầu thiết kế mới N/A |
| RESILIENCY-10 | Giữ timeout, isolation pool và degraded DTO; không gộp bỏ guard để giảm file |
| Property-based testing | Disabled theo state; không mở lại quyết định |

Đã thực hiện và kiểm tra mẫu Subjects trước khi nhân rộng. Các kiểm tra tự động cuối đã hoàn tất; browser E2E/provider/load/restore vẫn nằm trong giới hạn chưa kiểm chứng nêu ở báo cáo kết quả.
