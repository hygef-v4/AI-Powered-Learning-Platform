# U04 Subject, Class, Enrollment & Learning Access - Domain Entities

## 1. Phạm vi sở hữu

U04 sở hữu môn, lớp, phân công (Chủ nhiệm môn, giảng viên), ghi danh và mã mời. U04 **không** sở hữu: tài khoản và role (U01), nội dung (U05), thanh toán (U07), thông báo (U16), audit (U02).

## 2. `Subject` (môn học)

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | Khóa |
| `code` | chuỗi ≤ 20 | Chữ hoa, số, `-`; duy nhất; **không đổi** sau khi tạo |
| `name` | chuỗi ≤ 200 | Bắt buộc |
| `description` | chuỗi ≤ 2000 | Tùy chọn |
| `status` | enum | `ACTIVE`, `ARCHIVED` |
| `managerAccountId` | UUID | Chủ nhiệm môn; có thể rỗng; tối đa 1 người |
| `createdAt`, `updatedAt` | thời gian | |

## 3. `CourseClass` (lớp học)

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | Khóa |
| `subjectId` | UUID | Thuộc đúng 1 môn; không đổi |
| `code` | chuỗi ≤ 30 | Duy nhất trong môn; không đổi |
| `name` | chuỗi ≤ 200 | Bắt buộc |
| `description` | chuỗi ≤ 2000 | Tùy chọn |
| `term` | chuỗi ≤ 20 | Học kỳ, ví dụ `2026-1` |
| `status` | enum | `DRAFT`, `OPEN`, `ARCHIVED` |
| `instructorAccountId` | UUID | Giảng viên chính; bắt buộc trước khi `OPEN` |
| `inviteCode` | chuỗi 8 | Có thể rỗng; duy nhất toàn hệ thống khi có |
| `inviteEnabled` | bool | Mặc định `false` |
| `showGradeDistribution` | bool | Mặc định `false`; chỉ bật phân bố điểm ẩn danh trên dashboard khi đủ mẫu |
| `inviteExpiresAt` | thời gian | Bắt buộc khi bật mã |
| `createdAt`, `updatedAt` | thời gian | |

## 4. `Enrollment` (ghi danh)

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | Khóa |
| `classId` | UUID | |
| `learnerAccountId` | UUID | Duy nhất theo `(classId, learnerAccountId)` |
| `status` | enum | `ACTIVE`, `REMOVED` |
| `source` | enum | `MANUAL`, `LIST`, `INVITE` |
| `enrolledBy` | UUID | Người thực hiện (chính người học nếu `INVITE`) |
| `enrolledAt`, `removedAt` | thời gian | |

## 5. Trạng thái

```
Lớp:  DRAFT --mở--> OPEN --lưu trữ--> ARCHIVED
        |                                 |
        +---------lưu trữ (hủy)---------->+
                   ARCHIVED --mở lại--> OPEN

Môn:  ACTIVE <--> ARCHIVED

Ghi danh:  ACTIVE <--> REMOVED
```

**Text alternative**: Lớp đi từ `DRAFT` sang `OPEN` rồi `ARCHIVED`; `DRAFT` cũng có thể lưu trữ thẳng; `ARCHIVED` mở lại về `OPEN`, không về `DRAFT`. Môn chuyển qua lại `ACTIVE`/`ARCHIVED`. Ghi danh chuyển qua lại `ACTIVE`/`REMOVED` trên cùng một bản ghi.

## 6. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `SubjectScopePort`, `ClassScopePort` | U04 cung cấp cho U01 | `isSubjectManager`, `isInstructorOf`, `subjectOfClass`, `listAssignments(accountId)` (để chặn hạ role) |
| `ClassAccessPort` | U04 cung cấp cho U05-U16 | `getClassRef(classId)` (môn, trạng thái, giảng viên, `showGradeDistribution`), `isActiveLearner(accountId, classId)`, `listActiveLearners(classId)` |
| `AccountLookupPort` | U04 dùng U01 | Tìm tài khoản theo email hoặc chuỗi tìm kiếm; trả `id`, `displayName`, `email`, `role`, `status` |
| `AuthorizationPort` | U04 dùng U01 | Kiểm role và phạm vi |
| `PublishedContentPort` | U04 dùng, U05 cung cấp (`C`) | Nội dung đã phát hành của lớp và môn |
| `AuditPort`, `EventPublisherPort` | U04 dùng U02 | Audit; event `ENROLLMENT_ACTIVATED` cho U16 |
