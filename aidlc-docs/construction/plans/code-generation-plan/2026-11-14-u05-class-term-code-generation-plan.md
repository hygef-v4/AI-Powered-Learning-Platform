# U05 (kèm U04, U07, U08, U14) — Học kỳ của lớp — kế hoạch Code Generation

Kế hoạch này là nguồn duy nhất cho Part 2. Dựa trên:

- [Yêu cầu CT-01…09](../../../inception/requirements/2026-11-14-class-term.md), quyết định D-U04-05.
- [Kế hoạch thực hiện](../../../inception/plans/2026-11-14-class-term-execution-plan.md).
- [Functional Design](../functional-design-plan/2026-11-14-u05-class-term-functional-design-plan.md): U05 BR-U05-61…72, F17–F20, mục "Học kỳ của lớp" trong frontend-components; phần U04.
- [Quy ước mã nguồn chung](../../code-conventions.md) và quy định persistence JDBC.

## Bối cảnh

- **Nhánh**: `fix/term`, lúc lập kế hoạch ở `6c0bdc16` (trùng `origin/staging`). Không commit/push nếu người dùng chưa yêu cầu.
- **Code**: `backend/` (Spring Boot, JDBC, Flyway, PostgreSQL 16), `frontend/` (Next.js, Vitest), `scripts/e2e/`.
- **Không có User Stories** cho đợt này; truy vết theo CT/BR.
- **Unit chính** U05 `academics`. Phần nhỏ: U04 `subjects` (`SubjectClassSummary`), U07 `assignments` (`CopySource`), U08 `assignmentwork` (`WorkViews.Row`), U14 `announcements` (`ClassOption`) — chỉ thêm trường học kỳ lấy từ `ClassRef.term` để nhãn lớp ở ô lọc và hộp chọn lớp nguồn hiện học kỳ (CT-08, FD Q3).
- **Không đổi**: luật ghi danh, quyền, notifications, AI prompt, `PracticeViews` (U11), bảng lớp đang giữ khi chặn đổi role (U01).

### Quyết định triển khai

- **`term` là cột generated** trong PostgreSQL: `term VARCHAR(8) GENERATED ALWAYS AS (<biểu thức theo starts_on>) STORED`. Database tự bảo đảm `term` luôn khớp `starts_on`, lớp cũ có `term` ngay khi backfill `starts_on`, không có đường nào ghi `term` lệch. Biểu thức chỉ dùng hàm immutable (`extract`, phép `%`, `lpad`, `CASE`), không dùng `to_char`. Domain vẫn tính `term` bằng `ClassTermPolicy` cho snapshot và xem trước; repository không ghi cột `term` và đọc lại sau khi ghi. Đúng với BR-U05-62/67 (term do hệ thống tính, lưu trong cột).
- **`starts_on` không có default** ở database: thiếu giá trị là lỗi, không che lỗi code. Các integration test đang `insert into course_classes` bằng SQL thô sẽ thêm `starts_on`.
- **Tương thích contract**: `ClassRef` thêm `term` ở cuối và giữ hai constructor cũ (term `null`) để các module/test chỉ tạo `ClassRef` tay không phải sửa. `SubjectClassSummary`, `ClassView`, `CopySource`, `ClassOption`, `WorkViews.Row` thêm trường; cập nhật mọi chỗ tạo.
- **Version migration**: `V20261114_1100__add_class_start_date_term.sql` trong `db/migration/classes` (chưa có version `20261114.*` nào).

## Các bước

Mỗi bước viết hoặc sửa test cùng lúc với code. Chạy test nhắm đúng phạm vi sau mỗi nhóm bước; toàn bộ `mvn verify`/Vitest ở Build and Test.

### Backend — domain và dữ liệu

- [x] **Step 1 — `ClassTermPolicy`** (BR-U05-61, 62, 65; domain-entities 2.1)
  - Tạo `backend/src/main/java/edu/aiplatform/academics/model/ClassTermPolicy.java`: `termOf(LocalDate)`, `requireValidStart(LocalDate)` (`INVALID_CLASS_START_DATE`), `parseFilter(String)` (`INVALID_CLASS_TERM`), `compareNewestFirst`; hằng `TERM_PATTERN`, `MIN_START`, `MAX_START`. Lỗi dùng `AcademicRuleException`.
  - Tạo `backend/src/test/java/edu/aiplatform/academics/model/ClassTermPolicyTest.java`: đủ 12 tháng, `-BL5` tháng 4/8/12, `2000-01-01 → SP00`, `2099-12-31 → FA99-BL5`, ngoài khoảng, null, filter rỗng/sai hoa thường/sai mẫu, thứ tự.
- [x] **Step 2 — `CourseClass`** (BR-U05-61…64; F17, F18)
  - Sửa `academics/model/CourseClass.java`: thêm `LocalDate startsOn`, `String term`; hàm khởi tạo kiểm `startsOn` và `term == termOf(startsOn)` (`INVALID_CLASS_SNAPSHOT`); `createClass(..., startsOn, ...)`; `updateInfo(name, description, startsOn, now)` tính lại `term`; `copyWithChanges` giữ/đổi ngày.
  - Sửa `academics/model/CourseClassTest.java`.
- [x] **Step 3 — Migration** (BR-U05-67; F20; NFR-CT-03)
  - Tạo `backend/src/main/resources/db/migration/classes/V20261114_1100__add_class_start_date_term.sql`: thêm `starts_on DATE`; backfill `(created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date`; `SET NOT NULL`; check khoảng 2000–2099; thêm `term` generated stored + check mẫu; bỏ `uq_course_classes_subject_code`, thêm `uq_course_classes_subject_term_code (subject_id, term, code)`; thêm `ix_course_classes_subject_term (subject_id, term)`. Không đụng `updated_at`, `version`. Giữ grant hiện có.
- [x] **Step 4 — Test migration**
  - Sửa `academics/integration/ClassTermMigrationIntegrationTest.java`: kiểm việc xóa cột cũ ở target `20261107.2200` (thay vì migrate tới mới nhất); phần kiểm repository sau migration dùng snapshot có `startsOn`.
  - Tạo `academics/integration/ClassStartDateMigrationIntegrationTest.java`: lớp cũ được backfill theo giờ Việt Nam (biên `2026-03-31T17:30Z → 2026-04-01`, `SP26-BL5`), `version`/`updated_at` không đổi, cùng mã khác học kỳ được phép, cùng mã cùng học kỳ bị chặn, `term` không ghi tay được, check mẫu/khoảng ngày.
- [x] **Step 5 — `CourseClassRepository`** (BR-U05-64)
  - Sửa `academics/repository/CourseClassRepository.java`: insert/update ghi `starts_on` (không ghi `term`), `mapCourseClass` đọc `starts_on`, `term`; `DuplicateKeyException` ở cả insert và update → `CLASS_EXISTS` "Mã lớp đã tồn tại trong môn ở học kỳ này".
  - Sửa `academics/integration/AcademicsPersistenceTest.java`: ghi/đọc ngày và học kỳ, update đổi học kỳ, trùng mã khi update.
- [x] **Step 6 — `AcademicsReadRepository`** (BR-U05-65)
  - Sửa `IAcademicsReadRepository.java`, `AcademicsReadRepository.java`: `listForManager/Teacher/Student` nhận `String term` (null là không lọc), thêm `and c.term = :term`; sắp `order by c.code, c.starts_on desc, c.id` (cả `listTaught`, `listOfSubject`).
  - Sửa `academics/repository/AcademicsReadRepositoryTest.java`.
- [x] **Step 7 — SQL thô trong integration test khác**: thêm `starts_on` (`NotificationDeliveryIntegrationTest` dùng bảng `course_classes` giả chỉ có `id`, không cần sửa) vào `insert into course_classes` ở `PracticeFeedbackConcurrencyIntegrationTest`, `EvaluationVersionIntegrationTest`, `GroupRepositoriesIntegrationTest`, `GroupProposalRepositoryIntegrationTest`, `GroupProposalFlowIntegrationTest`, `NotificationDeliveryIntegrationTest`, `JdbcQuizAttemptRepositoryIntegrationTest`, `PlatformWorkflowIntegrationTest`, `RequirementsCoverageIntegrationTest`.

### Backend — service, contract, API

- [x] **Step 8 — `AcademicsService`** (BR-U05-61, 63, 64, 65; F17–F19)
  - `createClass(..., LocalDate startsOn, ...)`, `updateClassInfo(..., LocalDate startsOn, version)`, `listClasses(..., String term, ...)` gọi `ClassTermPolicy.parseFilter`; `mapClassView` thêm `startsOn`, `term`; thêm `INVALID_CLASS_START_DATE`, `INVALID_CLASS_TERM` vào danh sách mã lỗi miền trả `400` (cạnh `ENROLLMENT_CONFLICT`… hiện có).
  - Sửa `AcademicsServiceTest`, `AcademicsAccessAndReadTest`, `AcademicsBatchTransactionTest`.
- [x] **Step 9 — Contract và adapter** (BR-U05-68)
  - `academics/contract/ClassRef.java`: thêm `term` (giữ constructor cũ). `contract/stub/AcademicsContractStubs.java`.
  - `subjects/contract/SubjectClassSummary.java`: thêm `term` sau `code`.
  - `academics/service/AcademicsContractAdapter.java`: điền `term` cho `ClassRef`, `SubjectClassSummary`; gọi repository với `term = null`.
  - Sửa `AcademicsContractAdapterTest`, `SubjectRepositoryIntegrationTest`, `SubjectClassesReaderTest` (nếu tạo `SubjectClassSummary`).
- [x] **Step 10 — Controller và DTO** (BR-U05-61, 62, 63, 65, 68)
  - `academics/controller/AcademicsController.java`: `CreateInput`, `InfoInput` thêm `@NotNull LocalDate startsOn`; ba endpoint danh sách thêm `@RequestParam(required = false) String term`.
  - `academics/dto/AcademicsDtos.java`: `ClassView` thêm `LocalDate startsOn`, `String term`.
  - Sửa `AcademicsApiContractTest`: thiếu `startsOn` → `400`; body có `term` → từ chối như trường lạ; `term` sai mẫu → `400 INVALID_CLASS_TERM`; ngày ngoài khoảng → `400 INVALID_CLASS_START_DATE`; response có `startsOn`, `term`.
- [x] **Step 11 — MCP** (BR-U05-69)
  - `academics/service/AcademicsMcpTools.java`: schema `list_my_classes` thêm `term` tùy chọn; lọc và trả `term`.
  - Sửa `AcademicsMcpToolsTest`.
- [x] **Step 12 — Module dùng nhãn lớp** (CT-08, FD Q3)
  - U14: `announcements/dto/AnnouncementViews.ClassOption` thêm `term`; `AnnouncementService` điền từ `ClassRef.term`. Sửa `AnnouncementServiceTest`.
  - U08: `assignmentwork/dto/WorkViews.Row` (và `Detail` nếu cùng nhãn) thêm `classTerm`; `StudentAssignmentService` điền. Sửa test liên quan.
  - U07: `assignments/dto/AssignmentViews.CopySource` thêm `term`; `AssignmentCopyService` điền cho nguồn lớp (nguồn môn để `null`). Sửa `AssignmentCopyServiceTest`.

### Frontend

- [x] **Step 13 — Model và API** (CT-02, CT-06, CT-07)
  - Tạo `frontend/src/features/academics/model/term.ts`: `termOf`, `compareTermsNewestFirst`, `termOptions`.
  - `features/academics/model/types.ts`: `CourseClassView` thêm `startsOn`, `term`; `ClassInfoInput`, `CreateClassInput` thêm `startsOn`.
  - `features/academics/api/index.ts`: `listClasses` truyền `term` khi có.
  - Tạo `frontend/tests/features/academics/model/term.test.ts`; sửa `tests/features/academics/api/index.test.ts`.
- [x] **Step 14 — UI dùng chung** (FD Q6)
  - Tạo `frontend/src/shared/ui/TermFilter.tsx` (kiểu `Menu` như `ClassFilter`), export trong `shared/ui/index`.
  - `shared/ui/ClassFilter.tsx`: `ClassLabel` nhận `term`, dòng nhỏ `mã môn · học kỳ`; `ClassFilterOption` thêm `term`.
  - Sửa/thêm test trong `tests/shared/ui/ui.test.tsx`.
- [x] **Step 15 — Form lớp** (F17, F18; FD Q1, Q2)
  - `features/academics/components/ClassFormDialog.tsx` (và `ClassDialogs.tsx` nếu chứa form tạo): ô Ngày bắt đầu bắt buộc để trống, xem trước học kỳ, lỗi `CLASS_EXISTS`/`INVALID_CLASS_START_DATE`.
  - `features/academics/components/ClassInfoForm.tsx`: ô Ngày bắt đầu, dòng Học kỳ, "cũ → mới", gửi `startsOn`.
  - `features/academics/model/format.ts` (nếu là nơi map mã lỗi): thêm câu báo.
  - Sửa `tests/features/academics/components/academics.test.tsx`, `integration.test.tsx`.
- [x] **Step 16 — Danh sách, sidebar, tiêu đề** (CT-06, CT-08; FD Q3, Q4)
  - `features/academics/pages/ClassListPage.tsx`: `TermFilter`, URL `classTerm`, cột Học kỳ, thứ tự.
  - `features/academics/pages/ClassDashboardPage.tsx`: `TermFilter`, URL `classTerm`, số đếm tab theo học kỳ, hiện học kỳ trên thẻ/dòng.
  - `features/academics/components/ClassNavContexts.tsx`: nhãn lớp sidebar `mã môn · học kỳ`.
  - Tiêu đề/đường dẫn: `ManagedClassPage.tsx`, `ReadOnlyClassPage.tsx`, `StudentClassPage.tsx`.
  - Sửa `pagination.characterization.test.tsx`, `routes.test.tsx`, `routes.characterization.test.tsx`, `navContexts.test.tsx`.
- [x] **Step 17 — Frontend module khác** (CT-08; FD Q3)
  - U04: `features/subjects/model/types.ts`, `components/SubjectClassesTable.tsx` (cột Học kỳ); sửa `SubjectClassesTable.test.tsx`.
  - U14: `features/announcements` (type `ClassOption`, `ClassAnnouncementsPage`, `AnnouncementDialogs` truyền `term`); sửa `ClassAnnouncementsPage.test.tsx`.
  - U08: `features/assignmentwork` (type `Row`, `StudentAssignmentsPage` truyền `classTerm`); sửa `StudentAssignmentsPage.test.tsx`.
  - U07/U10: `CreateAssignmentDialog.tsx` (nhãn nguồn có `term`), `CreateQuizDialog.tsx` (lấy học kỳ từ danh sách lớp đã tải); sửa test liên quan nếu có.

### Script, tài liệu, kiểm tra

- [x] **Step 18 — E2E**: `scripts/e2e/seed.mjs`, `scripts/e2e/u04-u13-u14.mjs` gửi `startsOn` khi tạo lớp.
- [x] **Step 19 — Tài liệu code**: tạo `aidlc-docs/construction/u05-class-enrollment/code/2026-11-14-class-term-summary.md` (file sửa/tạo, migration, API, test); cập nhật dòng API lớp trong `aidlc-docs/inception/reverse-engineering/api-inventory.md`.
- [x] **Step 20 — Kiểm tra nhắm phạm vi**: `mvn -q test` cho các test academics/subjects/announcements/assignments/assignmentwork đã sửa và integration test migration (cần Docker); frontend `npx tsc --noEmit`, `npx eslint` cho file đã sửa, Vitest cho thư mục test đã sửa. Ghi kết quả thật, gồm phần không chạy được.

## Truy vết

| Yêu cầu / rule | Bước |
|---|---|
| CT-01, BR-U05-61 | 1, 2, 8, 10, 15 |
| CT-02, BR-U05-62 | 1, 2, 3, 10, 13 |
| CT-03, BR-U05-63 | 2, 5, 8, 10, 15 |
| CT-04, BR-U05-11/64 | 3, 4, 5 |
| CT-05, BR-U05-67 | 3, 4 |
| CT-06, BR-U05-65/66/71 | 6, 8, 10, 13, 14, 16 |
| CT-07, BR-U05-68/69 | 9, 10, 11, 13 |
| CT-08 (FD Q3, Q6) | 12, 14, 16, 17 |
| CT-09, BR-U05-70 | Không đổi code; test ghi danh hiện có phải vẫn qua (20) |
| BR-U05-72, G2 | Không thêm audit/notification mới |
| NFR-CT-01…07 | 1–20 |
