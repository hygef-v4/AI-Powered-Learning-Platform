# U05 Class & Enrollment — code summary

Code viết ngày 2026-10-18 theo [plan](../../plans/code-generation-plan/u05-class-enrollment-code-generation-plan.md), nhánh `Class&Enrollment`, merge vào `iter1` cùng U01–U04. Integration test (PostgreSQL thật, chạy đồng thời) do tester viết.

## Business logic

| Luồng | Class chính |
|---|---|
| UC 20–21 Sinh viên xem lớp | `AcademicsService.list`/`detail` vai trò `student`: chỉ lớp `OPEN`/`ARCHIVED` có ghi danh `ACTIVE` |
| UC 22–23 Giảng viên xem lớp, sinh viên | `AcademicsService.list`/`detail`/`students`/`student` vai trò `teacher` (gồm Chủ nhiệm môn được giao dạy) |
| UC 24–25 Chủ nhiệm xem lớp của môn | `AcademicsService.list`/`detail` vai trò `manager`, kiểm Chủ nhiệm môn hiện thời qua `ISubjectScope` và `ISubjectQuery` |
| UC 26 Tạo lớp | `create`: lớp `DRAFT`, mã và môn không đổi sau khi tạo, giảng viên tùy chọn phải `TEACHER`/`SUBJECT_MANAGER` đang `ACTIVE` |
| UC 27 Gán giảng viên | `assignTeacher`: không bỏ giảng viên của lớp `OPEN` |
| UC 28 Ghi danh | Ghi danh từng người, nhập danh sách (`EnrollmentListParser`: tối đa 200 dòng, 100 KB, tra U01 một lần, mỗi dòng một transaction), gỡ và khôi phục; một sinh viên chỉ `ACTIVE` ở một lớp chưa lưu trữ của mỗi môn (BR-U05-22) |
| UC 29 Sửa lớp, trạng thái | Sửa thông tin có `version`, bật/tắt phân bố điểm, `DRAFT → OPEN → ARCHIVED` và mở lại; mở lại bị chặn nếu sinh viên đang học lớp khác cùng môn (BR-U05-15) |
| UC 30 Môn học của Chủ nhiệm | `managedSubjects`/`managedSubject`: thẻ môn và số lớp theo trạng thái |
| Khóa theo môn | `ISubjectMutationGuard.lockActive` của U04 (khóa dòng `subjects`, chờ tối đa 1 giây, môn không `ACTIVE` thì từ chối): mọi thao tác ghi lớp và ghi danh trong cùng môn chạy lần lượt, và không chạy chồng với việc U04 lưu trữ môn |
| Contract cho unit khác | `AcademicsContractAdapter` cài `IClassAccess`, `IClassScope` (U01), `ISubjectClasses` (U04); đọc quan hệ hiện thời, không cache. `archiveClassesOfSubject`: U04 lưu trữ môn thì U05 lưu trữ mọi lớp `DRAFT`, `OPEN` của môn trong cùng transaction, ghi audit từng lớp |

Domain: `CourseClass`, `Enrollment`, `EnrollmentPolicy`, `EnrollmentListParser`, `EnrollmentResult`. Hạ tầng: `JdbcCourseClassRepository`, `JdbcEnrollmentRepository`, `JdbcAcademicsReadRepository` (`NamedParameterJdbcTemplate`, tham số gắn tên, trang 1–100).

Kiểm quyền: vai trò và trạng thái tài khoản qua `IAccountLookup`, rồi `IAuthorization` với action đăng ký trong `Actions`/`AccessPolicy` của U01: `CLASS_STUDENT`, `CLASS_TEACHER`, `CLASS_MANAGER` (vào màn theo vai trò), `CLASS_VIEW` (sinh viên, giảng viên xem một lớp, phạm vi lớp), `CLASS_MANAGE` (Chủ nhiệm xem và sửa lớp của môn mình, phạm vi môn). Sai phạm vi trả 404; U01/U04 lỗi thì từ chối. Audit thành công ghi cùng transaction; từ chối và lỗi ghi sau rollback. Ghi danh phát `enrollment.activated` qua `IEventPublisher` của U03.

## API

Gốc `/api/v1`.

| Endpoint | UC |
|---|---|
| `GET /me/classes`, `/me/classes/{id}` | 20–21 |
| `GET /teaching/classes`, `/teaching/classes/{id}`, `/teaching/classes/{id}/students`, `/teaching/classes/{id}/students/{accountId}` | 22–23 |
| `GET /manager/classes`, `/manager/classes/{id}`, `/manager/classes/{id}/students` | 24–25 |
| `POST /manager/classes`; `GET /manager/teacher-candidates` | 26 |
| `PUT /manager/classes/{id}/teacher` | 27 |
| `POST /manager/classes/{id}/enrollments`, `enrollments:batch`; `PUT /manager/classes/{id}/enrollments/{accountId}/status`; `GET /manager/classes/{id}/student-candidates` | 28 |
| `PATCH /manager/classes/{id}`; `PUT /manager/classes/{id}/grade-distribution`, `/status` | 29 |
| `GET /manager/subjects`, `/manager/subjects/{id}` | 30 |

`version` lệch trả 409; mở lại vướng sinh viên trả 409 kèm danh sách; field lạ trong body trả 400.

## Migration

`db/migration/classes/V20260925_1100__create_classes_enrollments.sql`: bảng `course_classes` (FK `subject_id → subjects`, `teacher_id → accounts`, unique `(subject_id, code)`, lớp `OPEN` phải có giảng viên) và `enrollments` (khóa `(class_id, account_id)`, FK `account_id → accounts`, `REMOVED` phải có `removed_at`); `app` chỉ `SELECT`, `INSERT`, `UPDATE`.

## Frontend

- Route: `/classes`, `/classes/[id]` (sinh viên; giảng viên và Chủ nhiệm cũng vào được vì trang chủ của U01 trỏ tới đây); `/teaching/classes`, `/teaching/classes/[id]` (giảng viên, Chủ nhiệm); `/manager`, `/manager/subjects`, `/manager/subjects/[id]`, `/manager/classes`, `/manager/classes/[id]` (Chủ nhiệm môn). Mỗi nhóm có `layout.tsx` bọc `SessionProvider` + `RoleGuard` của U01: chưa đăng nhập về `/login`, sai role về trang chủ của mình.
- `features/academics`: `routes.tsx` (nạp dữ liệu, xử lý 409), `ClassListPage`, `ClassFormDialog`, `ManagedClassPage`, `ClassInfoForm`, `EnrollmentPanel`, `ClassDashboardPage`, `ReadOnlyClassPage`, `ManagerDashboardPage`, `useCandidates` (debounce, hủy request cũ), `api.ts`, `types.ts`.
- Host đủ 5 slot cho unit sau: `StudentClassSection`, `TeacherClassTab` (`info`, `students`, `materials`, `evals`, `gradebook`), `StudentDetailSection`, `SubjectSummarySection`, `ClassCardSection`. Menu đăng ký qua `NavEntry` trong `register.ts`.

## Unit test

Backend: domain, parser, repository (điều kiện transaction, conflict), kiểm quyền và đọc theo vai trò, batch mỗi dòng một transaction, API (ánh xạ, validate, lỗi), contract adapter. Frontend: API client, route, form, ghi danh, CSV, `useCandidates`, slot.

## Khác với thiết kế

- Khóa ghi danh theo cả môn (khóa dòng môn của U04) thay cho "khóa hàng theo (sinh viên, môn)" ở F10.
- Frontend dùng khung riêng `AcademicsShell` và CSS module thay cho `AppShell`, `shared/ui`; vì vậy layout chỉ dùng `RoleGuard`, chưa dùng `AuthenticatedLayout`.

## Còn lại

- Dùng `AppShell`, `shared/ui`; khi đó đổi 3 layout sang `AuthenticatedLayout`.

## Clean Code follow-up — 2026-10-20

- Theo yêu cầu giữ nguyên hành vi/contract: làm rõ resource/mutation/view flow trong `routes.tsx`, bỏ Frame không còn chức năng; đặt tên biến phân trang trong hai danh sách; làm rõ tên biến và trình tự xử lý trong `AcademicsService`. Không đổi API/DTO, kiểm quyền/khóa, transaction, audit/event, schema hoặc dependency.
- Giữ thêm chín characterization case đã pass trên code cũ: request/refresh cancellation, GET lỗi sau ghi thành công, reopen conflict, tải nhiều trang và các hành vi phân trang hiện có. Không sửa/xóa/nới test cũ.
- Kiểm cuối: Maven verify 724 total / 662 passed / 62 skipped / zero failures/errors, executable JAR pass; frontend 232 passed / 47 files, lint/typecheck và production standalone build pass. Review độc lập không có finding; cấu hình TypeScript tự sinh bởi build đã khôi phục và typecheck lại pass.
- Các gate tích hợp DB/provider/browser/NFR chưa được chứng nhận thêm. Không commit/push. Chi tiết phạm vi, lệnh chạy và bằng chứng: [refactor plan](../../plans/2026-10-20-u05-clean-code-refactor.md).
