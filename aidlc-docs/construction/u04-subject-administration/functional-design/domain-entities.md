# U04 Subject Administration — domain-entities

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

Thiết kế độc lập công nghệ.

**Quyết định 2026-11-07 — D-U04-04:** Bỏ học kỳ lớp khỏi UI, request/response, domain và dữ liệu hiện hành; xem [quyết định đã duyệt](../../../inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md). Migration mới `V20261107_2200__remove_class_term.sql` xóa cột cũ khi áp dụng; không sửa migration hoặc bằng chứng lịch sử. Các quy tắc khác giữ nguyên phạm vi đã chốt.

**Yêu cầu đã chốt 2026-11-08:** Áp dụng các [quyết định U04/U13/U14](../../../inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md). Đây là baseline yêu cầu để thiết kế test; không phải xác nhận code/runtime hoặc coverage đã đầy đủ. Các snapshot và số kiểm thử có ngày cũ là lịch sử.

## 1. Tổng quan

| Entity | Loại | Lưu ở | Unit ghi |
|---|---|---|---|
| `Subject` | Aggregate root | `subjects` | U04 |

U04 không sở hữu: tài khoản và role (U01), lớp, giảng viên lớp và ghi danh (U05), tài nguyên cấp môn (U07, U10, U13), audit (U02).

## 2. `Subject`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | Khóa |
| `code` | chuỗi ≤ 20 | Chữ hoa, số, `-`; duy nhất; không đổi sau khi tạo |
| `name` | chuỗi ≤ 200 | Bắt buộc |
| `description` | chuỗi ≤ 2.000 | Tùy chọn |
| `status` | enum | `ACTIVE`, `ARCHIVED` |
| `managerId` | UUID | Cột `manager_id` → `accounts`; `SUBJECT_MANAGER` đang `ACTIVE` lúc gán; có thể rỗng |
| `createdAt`, `updatedAt`, `version` | | Khóa lạc quan |

### Trạng thái

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: Admin thêm môn
    ACTIVE --> ARCHIVED: Lưu trữ môn và các lớp DRAFT OPEN
    ARCHIVED --> ACTIVE: Mở lại
```

**Text alternative**: Môn tạo ra ở `ACTIVE`. Lưu trữ môn đồng thời lưu trữ mọi lớp `DRAFT`, `OPEN` trong cùng transaction, giữ enrollment/lịch sử và gỡ Chủ nhiệm môn; lỗi thì rollback toàn bộ. Môn `ARCHIVED` có thể mở lại về `ACTIVE`, không tự mở lại lớp hoặc gán lại Chủ nhiệm. Không xóa môn.

## 3. Contract

### Interface U04 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `ISubjectScope` (contract của U01, U04 cài) | `isSubjectManager(accountId, subjectId)`, `listManagedSubjects(accountId)`, `releaseManagerAssignments(accountId, actor)` | U01 | Kiểm quyền R2 theo phân công hiện thời; chặn đổi role người đang là Chủ nhiệm môn; gỡ Chủ nhiệm khỏi mọi môn khi tài khoản bị vô hiệu hóa (BR-U04-12) |
| `ISubjectQuery` | `getSubject(subjectId)` → `SubjectRef`, `isActive(subjectId)`, `listManagedSubjects(accountId)` → `List<SubjectRef>`, `countSubjectsByStatus()` | U05, U07, U10, U13, U15 | Đọc mã, tên, trạng thái, Chủ nhiệm môn; kiểm môn còn `ACTIVE`; số đếm cho Admin Dashboard |
| `ISubjectMutationGuard` | `lockActive(subjectId, timeout)` → `SubjectRef` | U05 | Khóa dòng môn tới cuối transaction của U05 (chờ tối đa 1 giây), môn không `ACTIVE` thì từ chối `SUBJECT_ARCHIVED`. U05 gọi trước mọi thao tác ghi lớp và ghi danh; lưu trữ môn khóa cùng dòng nên hai bên chạy lần lượt (BR-U04-04, BR-U05-22) |
| `NavEntry` (contract frontend của U01, U04 cài) | - | U01 | Mục Môn học trên menu bên của Admin |

`SubjectRef`: `subjectId`, `code`, `name`, `status`, `managerId`.

### Contract U04 cung cấp (unit khác cài)

| Contract | Phương thức | Unit cài | Dùng để |
|---|---|---|---|
| `ISubjectClasses` | `listClassesOfSubject(subjectId)` → `List<SubjectClassSummary>`, `countClasses(subjectIds)` → số lớp theo môn, `countUnarchivedClasses(subjectId)`, `archiveClassesOfSubject(subjectId, actor)` | U05 | Hiện lớp chỉ đọc và tổng số lớp tại Subject Detail; lưu trữ đồng thời các lớp DRAFT/OPEN khi lưu trữ môn trong cùng transaction, không làm U04 phụ thuộc ngược implementation U05 |

`SubjectClassSummary`: `classId`, `code`, `name`, `status`, `teacherName`, `activeStudentCount`.

### Interface U04 dùng

| Interface | Unit | Dùng để |
|---|---|---|
| `IAuthorization`, `IAccountLookup` | U01 | Kiểm quyền Admin; kiểm và hiện Chủ nhiệm môn |
| `IAudit` | U02 | Audit |

## 4. API

| API | UC |
|---|---|
| `GET /api/v1/admin/subjects` | 16 |
| `POST /api/v1/admin/subjects` | 17 |
| `GET /api/v1/admin/subjects/{id}` | 18 |
| `GET /api/v1/admin/subjects/{id}/classes` | 18 |
| `PATCH /api/v1/admin/subjects/{id}` | 19 |
| `PUT /api/v1/admin/subjects/{id}/manager` | 17, 19 |
| `PUT /api/v1/admin/subjects/{id}/status` | 19 |
