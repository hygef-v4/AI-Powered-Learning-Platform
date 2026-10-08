# U04 Subject, Class, Enrollment & Learning Access - Code Generation Plan

**Bản tài liệu 2026-10-08**: UC 12, 13, 27, 28, 45, 46, 47, 48, 49, 50, 63, 64, 65, 66; primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-CAT-005, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U04. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-CAT-001, US-CAT-002, US-CAT-003, US-CAT-005 (bản đơn giản), US-LRN-001.
- **Primary UC hiện hành**: UC 12, 13, 27, 28, 45, 46, 47, 48, 49, 50, 63, 64, 65, 66. Supporting flows theo current-srs-contract.md.
- **Thiết kế nguồn**: `construction/u04-subject-class-enrollment/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort`, `AccountLookupPort` | U01 (`H`) | Dùng U01 thật; U04 mở sau U01 |
| `AuditPort` | U02 (`H`) | Audit |
| `EventPublisherPort` | U03 (`H`) | Dùng U03 thật |
| `PublishedContentPort` | U05 (`C`) | Adapter tạm trả danh sách rỗng; U05 thay bằng implementation thật ở wave 2 |
| U04 cung cấp `SubjectScopePort`, `ClassScopePort` | cho U01 | Thay adapter giả của U01 bằng implementation thật |

### Dữ liệu U04 sở hữu

PostgreSQL `subjects`, `course_classes`, `enrollments` (bảng nối, khóa `(class_id, account_id)`); Redis `ratelimit:invite-code:*`; routing key `enrollment.activated`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  academics/
    api/                SubjectController, ClassController, EnrollmentController,
                        MeClassController, DTO
    application/        SubjectService, ClassService, EnrollmentService,
                        EnrollmentGuard, InviteCodeService, StudentClassService,
                        ScopeQueryService
    domain/             Subject, CourseClass, Enrollment, trạng thái, EnrollmentRowResult,
                        InviteCodeGenerator
    infrastructure/     JPA repository, EmptyPublishedContentAdapter
    port/               ClassAccessPort, PublishedContentPort
                        (SubjectScopePort, ClassScopePort do U01 khai báo; U04 cài)
/backend/src/main/resources/db/migration/academics/
/frontend/src/app/admin/subjects/
/frontend/src/app/teaching/ (Assigned Classes, subjects/, classes/)
/frontend/src/app/admin/classes/
/frontend/src/app/learning/
/contracts/openapi/academics.yaml
/contracts/messages/enrollment-events.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - Thêm biến cấu hình U04 theo `logical-components.md` §3 vào `application.yml`.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain: `Subject`, `CourseClass` (gồm `showGradeDistribution` mặc định false), `Enrollment` với trạng thái và chuyển trạng thái hợp lệ; `InviteCodeGenerator` (BR-U04-02, 11, 14, 17, 21, 30).
- [ ] **Bước 3** - Port: `ClassAccessPort`, `PublishedContentPort`; `EmptyPublishedContentAdapter`. `SubjectScopePort`, `ClassScopePort` dùng interface U01 đã khai báo.
- [ ] **Bước 4** - `SubjectService`: tạo, sửa, gán Chủ nhiệm môn, lưu trữ/mở lại (BR-U04-01…04).
- [ ] **Bước 5** - `ClassService`: tạo lớp, gán giảng viên, sửa, đổi trạng thái, bật/tắt phân bố điểm, mở lại có kiểm vướng, khóa lạc quan, phát event khi mở lớp (BR-U04-10…17, 26, P4, P5).
- [ ] **Bước 6** - `EnrollmentGuard` (advisory lock theo người học + môn, khóa theo thứ tự khi mở lại) (P2).
- [ ] **Bước 7** - `EnrollmentService`: tìm người học, thêm từng người, thêm theo danh sách (mỗi dòng một transaction, tra U01 một lần), gỡ, ghi danh lại, phát event (BR-U04-20…26, P3).
- [ ] **Bước 8** - `InviteCodeService`: bật/tắt/đổi mã, tự ghi danh, rate limit Bucket4j chỉ trừ khi sai, thông báo chung (BR-U04-30…34, P6).
- [ ] **Bước 9** - `StudentClassService`: danh sách lớp "Đang học"/"Đã kết thúc", trang lớp có nội dung, U05 lỗi thì vẫn trả thông tin lớp (BR-U04-40…44, P8).
- [ ] **Bước 10** - `ScopeQueryService` cài `SubjectScopePort`, `ClassScopePort`, `ClassAccessPort` (gồm `countSubjectsByStatus`, `countClassesByStatus`, `countActiveEnrollments` cho UC 57); bỏ `NoAssignmentScopeAdapter` của U01 (P1).
- [ ] **Bước 11** - `loadForActor` che giấu đối tượng ngoài quyền và audit theo BR-U04-51, 52.
- [ ] **Bước 12** - Unit test cho mọi `BR-U04-xx`.
- [ ] **Bước 13** - Tóm tắt: `aidlc-docs/construction/u04-subject-class-enrollment/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 14** - Flyway `V20260925_1100__u04_subjects_classes_enrollments.sql` theo `infrastructure-design.md` §2, gồm cờ `show_grade_distribution` mặc định false và `REVOKE DELETE` khỏi `app`.
- [ ] **Bước 15** - JPA repository và query phạm vi có index.
- [ ] **Bước 16** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test Testcontainers (PostgreSQL, Redis, RabbitMQ): hai yêu cầu ghi danh đồng thời vào hai lớp cùng môn chỉ một thành công; ghi danh 200 dòng ≤ 5 s; event gửi sau commit; `app` không DELETE được.
- [ ] **Bước 17** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 18** - `/contracts/openapi/academics.yaml` (endpoint theo `frontend-components.md`) và schema event `enrollment-events.json`.
- [ ] **Bước 19** - Controller + DTO + validation (định dạng mã, email, CSV ≤ 200 dòng/100 KB).
- [ ] **Bước 20** - Test MockMvc: 4 role trên mọi endpoint, ID lớp người khác trả 404, `409` khi lệch `version`, `429` khi vượt rate limit mã mời.
- [ ] **Bước 21** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 22** - Admin: `SubjectListPage`, `SubjectDetailPage`, `SubjectFormDialog`, `AssignManagerDialog`, Subject Classes/Class Detail dưới `app/admin/classes/`; Chủ nhiệm môn: `SubjectManagerMenuPage`, `SubjectHubPage` (gắn `ModuleList` U05 chế độ môn) và `ClassListPage` lọc theo môn.
- [ ] **Bước 23** - Quản lý lớp: `TeacherMenuPage` (Assigned Classes, đích sau đăng nhập `/teaching`), `ClassListPage`, `ClassFormDialog`, `ClassDetailPage` (tab Thông tin, Học liệu (gắn `ModuleList` U05 chế độ lớp), Học viên (U12 nhúng panel nhóm khi được code), Mã mời; `ClassNavLinks` tới Announcements, Assignment List, Gradebook), `AssignTeacherDialog`, `ClassStateActions`, `GradeDistributionToggle` (mặc định tắt, BR-U04-17).
- [ ] **Bước 24** - Ghi danh: `AddStudentSearch`, `AddStudentsListDialog`, `EnrollmentResultTable`, `EnrollmentTable`.
- [ ] **Bước 25** - Người học: `MyClassesPage`, `JoinByCodeDialog`, `StudentClassPage`.
- [ ] **Bước 26** - Test frontend: chặn > 200 dòng, hiện kết quả từng dòng, xác nhận gỡ, lỗi chung khi mã sai.
- [ ] **Bước 27** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 28** - Cập nhật `README.md`: luồng tạo môn/lớp/ghi danh, cách unit khác dùng `ClassAccessPort`.
- [ ] **Bước 29** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-CAT-001 (UC 45, 46, 47, 48, 49, 63, 64, 65, 66) | 4, 5, 22, 23 |
| US-CAT-002 (UC 45, 46, 47, 48, 49) | 5, 11, 23 |
| US-CAT-003 (UC 45, 46, 47, 48, 49) | 6, 7, 16, 24 |
| US-CAT-005 (UC 28, 46) | 8, 25 |
| US-LRN-001 (UC 12, 13, 14) | 9, 25 |
| UC 57 (số đếm cho U16) | 10 |
| Contract cho U01 và U05-U16 | 3, 10 |

## 5. Ngoài phạm vi

- Implementation thật của `PublishedContentPort` (U05).
- Gửi thông báo và email ghi danh, giới hạn 300 email/ngày (U16).
- Không có dashboard cá nhân; My Classes (`MyClassesPage`) là trang đích của Student.

## 6. Revision implementation scope - 2026-10-08
- [ ] Gán manager SUBJECT_MANAGER/ADMIN ACTIVE; teacher TEACHER/SUBJECT_MANAGER/ADMIN ACTIVE, từ chối PENDING/DISABLED.
- [ ] Tách STRUCTURE_EDIT với TEACHING/ROSTER; Teacher-only bị từ chối UC 49 dù xem được Class Detail.
- [ ] ScopeQueryService isTeacherOf/isSubjectManager theo actor hiện thời; Admin Full cấu trúc không bypass tài nguyên dạy; kiểm thu hồi scope.
- [ ] Màn My Classes / Assigned Classes / Subject Classes dùng query scope khác nhau; ClassInfo/State và Gradebook panel kiểm action riêng.
