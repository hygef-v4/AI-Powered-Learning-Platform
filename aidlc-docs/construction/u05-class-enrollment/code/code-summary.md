# U05 — phần độc lập đã triển khai ngày 2026-10-17

> Latest implementation: [U05 code completion and integration handoff](completion-2026-10-18.md). The earlier dependency-free-only scope and test counts below are historical; they do not describe the current completed application/API/frontend code.


> Bản ghi lịch sử trước khi đổi UI. Đã khôi phục backend ngày 2026-10-18 và giữ UI mới; tên component/test count bên dưới mô tả phiên cũ. Xem [kết quả ghép mới nhất](stash-recovery-2026-10-18.md) và [UI hiện tại](ui-summary-2026-10-18.md).

## Phạm vi được người dùng cho phép

"thôi cứ code những phần u05 mà chưa cần đến u04 hay u01 trc đã , đợi người khác làm xong thì code tiếp"

U05 hiện hành là Class & Enrollment, UC 20–30. Chỉ triển khai phần độc lập; không tuyên bố hoàn tất một UC end-to-end hoặc toàn bộ U05. [Kế hoạch](../../plans/code-generation-plan/u05-class-enrollment-code-generation-plan.md) có checklist S1–S6 và kế hoạch tích hợp còn lại.

## Code đã có

| Vị trí | Nội dung |
|---|---|
| `backend/src/main/java/edu/aiplatform/academics/domain/CourseClass.java` | Snapshot bất biến; tạo DRAFT, chuyển trạng thái theo BR-U05-14, yêu cầu giảng viên khi OPEN, chặn sửa ARCHIVED, tăng version và kiểm độ dài dữ liệu |
| `academics/domain/Enrollment.java`, `EnrollmentPolicy.java` | Ghi danh/gỡ/khôi phục cùng định danh, phân biệt ENROLLED/RESTORED/ALREADY_ENROLLED/IN_OTHER_CLASS, chặn lớp ARCHIVED |
| `academics/domain/EnrollmentListParser.java` | CSV một cột hoặc text; BOM/header email tùy chọn; chuẩn hóa email, dòng lỗi/trùng, giữ số dòng; tối đa 200 dòng dữ liệu không rỗng và 102400 byte UTF-8 |
| `academics/domain/ICourseClassRepository.java`, `IEnrollmentRepository.java`, `ISubjectMutationLock.java` | Interface nội bộ, không sửa contract liên unit |
| `academics/infrastructure/` | Adapter Spring JDBC có query tham số hóa, phân trang 1–100, optimistic update, query xung đột enrollment trong môn, khóa transaction theo môn với timeout 3 giây |
| `backend/src/main/resources/db/pending/academics/V20260925_1100__create_classes_enrollments.sql` | SQL chờ: course_classes/enrollments, FK thật, unique/index/check, quyền app SELECT/INSERT/UPDATE |
| `frontend/src/features/academics/` | `ClassInfoForm`, `ClassStateActions`, `AddStudentsListDialog`, `EnrollmentResultTable`, types và parser. Nhận props/callback; không giả danh người dùng hoặc gọi API chưa có |

Các đường dẫn backend viết tắt `academics/` ở bảng trên nằm dưới `backend/src/main/java/edu/aiplatform/`.

Parser chỉ kiểm định dạng và trùng trong danh sách. Dòng hợp lệ chưa có nghĩa tài khoản tồn tại hay đã ghi danh. Đối số `subjectActive` và `conflictingEnrollments` của domain là dữ kiện do application kiểm trong transaction sau này, không phải authorization. EnrollmentPolicy không kiểm role/trạng thái tài khoản của U01.

## Cách giữ khung hiện tại chạy được

- Không thêm JPA entity scan, dependency, API route hoặc security configuration.
- Repository và khóa mặc định không tạo bean. Chỉ bật sau khi sẵn sàng bằng `platform.academics.persistence-enabled=true`.
- SQL nằm ngoài `classpath:db/migration`, chưa được Flyway chạy. Không copy vào đường migration khi accounts/subjects chưa có.
- Giữ các stub liên unit hiện có; không dùng kết quả luôn-cho-phép làm bằng chứng phân quyền.
- Mọi repository write yêu cầu writable transaction. Service tương lai còn phải lấy `ISubjectMutationLock.acquire(subjectId)` trước kiểm xung đột và mọi cập nhật enrollment/trạng thái lớp. Repository không tự cấp quyền, tự audit hoặc phát event.

## Kiểm chứng

- Backend: tổng cộng 122 test từ 29 suite, không failure/error; U05 có 28 test, kiến trúc có 3 test. Đã chạy toàn bộ backend trước hai test repository bổ sung, sau đó chạy lại suite repository (7 test) thành công. JDK máy là 21, compiler target theo pom là Java 17.
- Frontend: 27 test/9 file thành công, gồm 11 test U05 (2 parser, 9 component); typecheck và lint thành công.
- Build frontend bằng `next build --webpack` thành công, Node 24.19.0. Lệnh build mặc định dùng Turbopack vẫn lỗi tạo tiến trình xử lý `globals.css` trên môi trường máy; đã tái hiện cả trong/ngoài sandbox, không sửa script build của cả dự án để che lỗi này.
- Test red ban đầu xác nhận module/class mới chưa tồn tại; implementation được viết sau. Maven cần tải dependency/plugin còn thiếu; Vitest chạy bằng Node 24 ngoài sandbox vì worker không đọc được file transform tạm trong sandbox. Không thay package-lock/pom.
- Chưa chạy PostgreSQL/migration, test transaction thật, concurrency, p95 hay UI với đăng nhập thật. Unit test repository kiểm điều kiện transaction, conflict, query đọc lại và bean opt-in, không chứng minh SQL/locking đúng trên PostgreSQL.

## Checklist tiếp tục sau U01/U04

- [ ] Đối chiếu schema accounts/subjects và thứ tự Flyway với người phụ trách; chuyển SQL pending sang `db/migration/classes/` sau review, chạy bằng migrator và kiểm quyền app.
- [ ] Xác nhận principal `Actor`, tên action của `IAuthorization`, account lookup và subject query hoạt động; không thêm actor header hoặc role từ localStorage để thay thế U01.
- [ ] Cài `ClassScopeService` cho IClassScope/ISubjectClasses/IClassAccess, query quan hệ hiện thời, không cache; thay stub bằng bean thật.
- [ ] Cài view/admin/enrollment services; quyền R2/R3/R5, kiểm trạng thái account và subject, điều phối subject lock → đọc/khóa dữ liệu → kiểm BR → ghi → audit/event trong transaction.
- [ ] Batch lookup U01 một lần, transaction riêng mỗi dòng; giữ thứ tự và đủ chín mã kết quả. Domain/parser hiện tại chưa chạy lookup hoặc tạo enrollment.
- [ ] U02 audit thật: success cùng transaction, denied/failure còn tồn tại sau rollback. Publish enrollment.activated qua U03 để gửi sau commit.
- [ ] Cài controller theo API design và route/dashboard/slot host; nối component đã có qua callback, session/RoleGuard của U01, API 404/409 và reload version.
- [ ] Tester riêng viết/chạy integration: enrollment đầu tiên đồng thời vào hai lớp, mở lại cùng lúc ghi danh, gỡ/đổi giảng viên mất quyền request sau, 200 dòng, bốn role/IDOR, FK/app quyền và after-commit.

## Extension compliance trong lát cắt này

SECURITY-05: validation domain/parser, giới hạn kích thước và query tham số hóa đã có. SECURITY-03/09: code mới không log PII/SQL/credential; domain conflict không chứa SQL. SECURITY-08/12/15: authentication, authorization và fail-closed của API đang chờ U01 và service/controller; chưa xác nhận runtime compliance. SECURITY-04: không thêm route HTML, giữ nền tảng hiện có. RESILIENCY-10: có transaction lock timeout; chưa có external call mới. RESILIENCY-04/06: không đổi deployment/health. Rule ngoài subset N/A theo phạm vi đồ án; PBT disabled. Không triển khai/claim chứng nhận security hoặc resiliency toàn hệ thống.
