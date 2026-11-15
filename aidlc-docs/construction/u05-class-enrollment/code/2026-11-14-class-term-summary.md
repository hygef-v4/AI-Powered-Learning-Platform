# U05 (kèm U04) — Học kỳ của lớp: tóm tắt code, 2026-11-14

Nguồn: [yêu cầu CT-01…09](../../../inception/requirements/2026-11-14-class-term.md), quyết định D-U04-05, [Functional Design](../functional-design/business-rules.md) BR-U05-61…72, [kế hoạch Code Generation](../../plans/code-generation-plan/2026-11-14-u05-class-term-code-generation-plan.md).

## Quy tắc đã cài

- Lớp có **Ngày bắt đầu** (`startsOn`, bắt buộc, 2000-01-01…2099-12-31). Học kỳ (`term`) do hệ thống tính, client không gửi được.
- Tháng 1–4 `SP`, 5–8 `SU`, 9–12 `FA`, cộng 2 số cuối của năm; lớp bắt đầu tháng 4, 8, 12 thêm `-BL5`. Ví dụ 3/4/2027 → `SP27-BL5`.
- Mã lớp duy nhất theo (môn, học kỳ, mã), nên dùng lại được qua các học kỳ. Trùng trong cùng học kỳ trả 409 `CLASS_EXISTS`.
- Lọc theo học kỳ ở API danh sách lớp, MCP `list_my_classes` và các trang danh sách lớp. Không có chức năng quản lý học kỳ.

## Backend

| Phần | File | Thay đổi |
|---|---|---|
| Domain | `academics/model/ClassTermPolicy.java` (mới) | `termOf`, `requireValidStart`, `parseFilter`, `compareNewestFirst`; mẫu `^(SP\|SU\|FA)[0-9]{2}(-BL5)?$` |
| Domain | `academics/model/CourseClass.java` | Thêm `startsOn`; `term()` là accessor suy ra, không lưu riêng; `createClass`, `updateInfo` nhận ngày |
| Migration | `db/migration/classes/V20261114_1100__add_class_start_date_term.sql` (mới) | `starts_on` lấp từ ngày tạo theo giờ Việt Nam, `NOT NULL` + check khoảng; `term` là cột `GENERATED ALWAYS ... STORED` + check mẫu; đổi khóa duy nhất sang `(subject_id, term, code)`, thêm index `(subject_id, term)` |
| Repository | `CourseClassRepository`, `IAcademicsReadRepository`, `AcademicsReadRepository` | Ghi `starts_on` (không ghi `term`); trùng khóa khi sửa cũng báo `CLASS_EXISTS`; ba hàm danh sách nhận `term`; sắp `code, starts_on desc, id` |
| Service | `AcademicsService` | `listClasses(..., term, ...)` kiểm mẫu (sai → 400 `INVALID_CLASS_TERM`); tạo/sửa nhận `startsOn` (sai → 400 `INVALID_CLASS_START_DATE`) |
| API | `AcademicsController`, `AcademicsDtos.ClassView` | `?term=` ở ba endpoint danh sách; `startsOn` bắt buộc ở POST/PATCH; `term` trong body bị từ chối (strict input); response thêm `startsOn`, `term` |
| MCP | `AcademicsMcpTools` | `list_my_classes` có tham số `term` (sai mẫu → input không hợp lệ) và trả `term` |
| Contract | `ClassRef`, `SubjectClassSummary`, `AcademicsContractAdapter` | Thêm `term`; `ClassRef` giữ hàm khởi tạo cũ (term rỗng) cho stub/test |
| U07, U08, U14 | `AssignmentViews.CopySource`, `AssignmentCopyService`, `WorkViews.Row`, `StudentAssignmentService`, `AnnouncementViews.ClassOption`, `AnnouncementService` | Thêm học kỳ của lớp vào dữ liệu nhãn lớp; giữ hàm khởi tạo cũ để test hiện có không đổi |

## Frontend

| Phần | File | Thay đổi |
|---|---|---|
| Model | `features/academics/model/term.ts` (mới) | `termOf` (chỉ xem trước), `isTerm`, `compareTermsNewestFirst`, `termOptions` |
| Model, API | `model/types.ts`, `model/format.ts`, `api/index.ts` | `CourseClassView.startsOn/term`; input tạo/sửa có `startsOn`; `formatClassFieldError` gắn `CLASS_EXISTS`/`INVALID_CLASS_START_DATE` vào ô; `formatStartDate`; `listClasses` truyền `term` |
| UI dùng chung | `shared/ui/TermFilter.tsx` (mới), `shared/ui/ClassFilter.tsx` | Ô lọc học kỳ kiểu `Menu`; `ClassLabel` có dòng nhỏ `mã môn · học kỳ` |
| Form | `ClassFormDialog`, `ClassInfoForm` | Ô Ngày bắt đầu (bắt buộc, để trống ban đầu), "Học kỳ: SP27"; form sửa có Học kỳ chỉ đọc và "cũ → mới" |
| Danh sách | `ClassListPage`, `ClassDashboardPage` | Ô Học kỳ, URL `classTerm` (giá trị lạ = Tất cả), cột Học kỳ; số trên tab tính theo học kỳ; thẻ ghi `mã môn · học kỳ` |
| Trang lớp, sidebar | `ManagedClassPage`, `ReadOnlyClassPage`, `StudentClassPage`, `ClassNavContexts` | Học kỳ cạnh mã lớp ở tiêu đề và breadcrumb, trong thẻ Thông tin lớp; sidebar `mã môn · học kỳ` |
| Module khác | `SubjectClassesTable` (U04), `AnnouncementDialogs` (U14), `StudentAssignmentsPage` (U08), `CreateAssignmentDialog` (U07) | Cột Học kỳ; nhãn lớp có học kỳ; đề copy từ lớp ghi `tên bài · mã lớp · học kỳ` |

## Khác với thiết kế

- Tiêu đề trang lớp hiện tại không ghi "mã môn · mã lớp", nên học kỳ đặt cạnh mã lớp (Chủ nhiệm môn) và trong thẻ Thông tin lớp (giảng viên, sinh viên) thay vì nối vào tiêu đề.
- `CreateQuizDialog` chưa hiện học kỳ: danh sách quiz copy được chỉ có tên quiz và số câu, không có thông tin lớp để gắn học kỳ. Muốn hiện thì U10 phải trả thêm lớp nguồn; không làm trong lần này.
- `CourseClass` không lưu `term` riêng nên không có mã `INVALID_CLASS_SNAPSHOT` như bản thiết kế ghi; đã ghi chú trong domain-entities.

## Script và tài liệu

- `scripts/e2e/seed.mjs`, `scripts/e2e/u04-u13-u14.mjs`: gửi `startsOn` khi tạo lớp.
- `aidlc-docs/inception/reverse-engineering/api-inventory.md`: ghi chú tham số và trường mới (không thêm annotation).

## Test

- Backend mới/sửa: `ClassTermPolicyTest`, `CourseClassTest`, `ClassStartDateMigrationIntegrationTest`, `ClassTermMigrationIntegrationTest`, `AcademicsPersistenceTest`, `AcademicsReadRepositoryTest`, `AcademicsServiceTest`, `AcademicsAccessAndReadTest`, `AcademicsApiContractTest`, `AcademicsMcpToolsTest`, `AcademicsContractAdapterTest` và SQL thô của 8 integration test khác.
- Frontend mới/sửa: `term.test.ts`, `api/index.test.ts`, `academics.test.tsx`, `navContexts.test.tsx`, `ui.test.tsx`, `SubjectClassesTable.test.tsx`, `ClassAnnouncementsPage.test.tsx`, `StudentAssignmentsPage.test.tsx`, `assignments.test.tsx` và fixture của các test academics khác.
- Kết quả chạy: xem mục Kết quả kiểm tra bên dưới.

## Kết quả kiểm tra

Chạy ngày 2026-11-15 trên máy làm việc (Windows, Docker 29.7.2), mã nguồn chưa commit trên nhánh `fix/term`:

| Lệnh | Kết quả |
|---|---|
| `mvn -o test` (toàn bộ backend, gồm integration test Testcontainers PostgreSQL 16) | 2426 test, 0 lỗi, 0 bỏ qua; `ClassStartDateMigrationIntegrationTest` 11, `ClassTermMigrationIntegrationTest` 2, `AcademicsPersistenceTest` 7 |
| `npx tsc --noEmit` | Không lỗi |
| `npx eslint` các file frontend đã sửa | Không lỗi |
| `npx vitest run` thư mục academics, announcements, assignments, assignmentwork, subjects, quizzes, shared (`--testTimeout=60000 --maxWorkers=4`) | 73 file, 540 test đạt |

Ghi chú: chạy Vitest với timeout mặc định 5 giây và nhiều worker trên máy này thì một số test nặng (không liên quan học kỳ, ví dụ `authoringFlow`, `SubmissionHistoryPage`) quá giờ, mỗi lần một nhóm khác nhau; tăng timeout thì đều đạt. Kết quả kiểm trên trình duyệt và chạy e2e ở stack riêng: xem [kết quả Build and Test](../../build-and-test/2026-11-15-class-term-results.md).
