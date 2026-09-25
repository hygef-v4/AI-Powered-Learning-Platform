# U04 Subject, Class, Enrollment & Learning Access - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U04. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-CAT-001, US-CAT-002, US-CAT-003, US-CAT-005 (bản đơn giản), US-LRN-001.
- **Use case**: UC-CAT-01..13, UC-LRN-01, UC-LRN-02, UC-CNT-04.
- **Thiết kế nguồn**: `construction/u04-subject-class-enrollment/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort`, `AccountLookupPort` | U01 (`H`) | Dùng U01 thật; U04 mở sau U01 |
| `AuditPort`, `EventPublisherPort` | U02 (`H`) | Dùng U02 thật |
| `PublishedContentPort` | U05 (`C`) | Adapter tạm trả danh sách rỗng; U05 thay bằng implementation thật ở wave 2 |
| U04 cung cấp `SubjectScopePort`, `ClassScopePort` | cho U01 | Thay adapter giả của U01 bằng implementation thật |

### Dữ liệu U04 sở hữu

PostgreSQL `subjects`, `classes`, `enrollments`; Redis `u04:invite-fail:*`; routing key `u04.enrollment.activated`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  u04/
    api/                SubjectController, ClassController, EnrollmentController,
                        MeClassController, DTO
    application/        SubjectService, ClassService, EnrollmentService,
                        EnrollmentGuard, InviteCodeService, LearnerClassService,
                        ScopeQueryService
    domain/             Subject, CourseClass, Enrollment, trạng thái, EnrollmentRowResult,
                        InviteCodeGenerator
    infrastructure/     JPA repository, EmptyPublishedContentAdapter
    port/               ClassAccessPort, PublishedContentPort
                        (SubjectScopePort, ClassScopePort do U01 khai báo; U04 cài)
/backend/src/main/resources/db/migration/u04/
/frontend/src/app/admin/subjects/
/frontend/src/app/teaching/classes/
/frontend/src/app/learn/
/contracts/openapi/u04-academic.yaml
/contracts/messages/u04-enrollment-activated.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - Thêm biến cấu hình U04 theo `logical-components.md` §3 vào `application.yml`.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain: `Subject`, `CourseClass` (gồm `showGradeDistribution` mặc định false), `Enrollment` với trạng thái và chuyển trạng thái hợp lệ; `InviteCodeGenerator` (BR-U04-02, 11, 14, 17, 21, 30).
- [ ] **Bước 3** - Port: `ClassAccessPort`, `PublishedContentPort`; `EmptyPublishedContentAdapter`. `SubjectScopePort`, `ClassScopePort` dùng interface U01 đã khai báo.
- [ ] **Bước 4** - `SubjectService`: tạo, sửa, gán Chủ nhiệm môn, lưu trữ/mở lại (BR-U04-01…04).
- [ ] **Bước 5** - `ClassService`: tạo lớp, gán giảng viên, sửa, đổi trạng thái, bật/tắt phân bố điểm, mở lại có kiểm vướng, khóa lạc quan, phát event khi mở lớp (BR-U04-10…17, 26, P4, P5).
- [ ] **Bước 6** - `EnrollmentGuard` (advisory lock theo người học + môn, khóa theo thứ tự khi mở lại) (P2).
- [ ] **Bước 7** - `EnrollmentService`: tìm người học, thêm từng người, thêm theo danh sách (mỗi dòng một transaction, tra U01 một lần), gỡ, ghi danh lại, phát event (BR-U04-20…26, P3).
- [ ] **Bước 8** - `InviteCodeService`: bật/tắt/đổi mã, tự ghi danh, rate limit Bucket4j chỉ trừ khi sai, thông báo chung (BR-U04-30…34, P6).
- [ ] **Bước 9** - `LearnerClassService`: danh sách lớp "Đang học"/"Đã kết thúc", trang lớp có nội dung, U05 lỗi thì vẫn trả thông tin lớp (BR-U04-40…44, P8).
- [ ] **Bước 10** - `ScopeQueryService` cài `SubjectScopePort`, `ClassScopePort`, `ClassAccessPort`; bỏ `NoAssignmentScopeAdapter` của U01 (P1).
- [ ] **Bước 11** - `loadForActor` che giấu đối tượng ngoài quyền và audit theo BR-U04-51, 52.
- [ ] **Bước 12** - Unit test cho mọi `BR-U04-xx`.
- [ ] **Bước 13** - Tóm tắt: `aidlc-docs/construction/u04-subject-class-enrollment/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 14** - Flyway `V20260925_1100__u04_subjects_classes_enrollments.sql` theo `infrastructure-design.md` §2, gồm cờ `show_grade_distribution` mặc định false và `REVOKE DELETE` khỏi `app`.
- [ ] **Bước 15** - JPA repository và query phạm vi có index.
- [ ] **Bước 16** - Integration test Testcontainers (PostgreSQL, Redis, RabbitMQ): hai yêu cầu ghi danh đồng thời vào hai lớp cùng môn chỉ một thành công; ghi danh 200 dòng ≤ 5 s; event gửi sau commit; `app` không DELETE được.
- [ ] **Bước 17** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 18** - `/contracts/openapi/u04-academic.yaml` (endpoint theo `frontend-components.md`) và schema event `u04-enrollment-activated.json`.
- [ ] **Bước 19** - Controller + DTO + validation (định dạng mã, email, CSV ≤ 200 dòng/100 KB).
- [ ] **Bước 20** - Test MockMvc: 4 role trên mọi endpoint, ID lớp người khác trả 404, `409` khi lệch `version`, `429` khi vượt rate limit mã mời.
- [ ] **Bước 21** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 22** - Admin: `SubjectListPage`, `SubjectFormDialog`, `AssignManagerDialog`.
- [ ] **Bước 23** - Quản lý lớp: `ClassListPage`, `ClassFormDialog`, `ClassDetailPage` (tab Thông tin, Học viên, Mã mời), `AssignInstructorDialog`, `ClassStateActions`, `GradeDistributionToggle` (mặc định tắt, BR-U04-17).
- [ ] **Bước 24** - Ghi danh: `AddLearnerSearch`, `AddLearnersListDialog`, `EnrollmentResultTable`, `EnrollmentTable`.
- [ ] **Bước 25** - Người học: `MyClassesPage`, `JoinByCodeDialog`, `LearnerClassPage`.
- [ ] **Bước 26** - Test frontend: chặn > 200 dòng, hiện kết quả từng dòng, xác nhận gỡ, lỗi chung khi mã sai.
- [ ] **Bước 27** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 28** - Cập nhật `README.md`: luồng tạo môn/lớp/ghi danh, cách unit khác dùng `ClassAccessPort`.
- [ ] **Bước 29** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-CAT-001 (UC-CAT-01..04, 06, 08) | 4, 5, 22, 23 |
| US-CAT-002 (UC-CAT-05, 07, 09) | 5, 11, 23 |
| US-CAT-003 (UC-CAT-10..12) | 6, 7, 16, 24 |
| US-CAT-005 (UC-CAT-13) | 8, 25 |
| US-LRN-001 (UC-LRN-01, 02, UC-CNT-04) | 9, 25 |
| Contract cho U01 và U05-U15 | 3, 10 |

## 5. Ngoài phạm vi

- Implementation thật của `PublishedContentPort` (U05).
- Gửi thông báo và email ghi danh, giới hạn 300 email/ngày (U16).
- Dashboard ghép assignment (U08) và thông báo (U16).
