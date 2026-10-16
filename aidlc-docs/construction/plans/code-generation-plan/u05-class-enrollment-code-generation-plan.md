# U05 Class & Enrollment — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U05 Class & Enrollment thuộc Iter 1, người code: Người 5; UC 20–30. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 20–30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Thiết kế: [business-logic-model](../../u05-class-enrollment/functional-design/business-logic-model.md), [business-rules](../../u05-class-enrollment/functional-design/business-rules.md), [domain-entities](../../u05-class-enrollment/functional-design/domain-entities.md), [frontend-components](../../u05-class-enrollment/functional-design/frontend-components.md), [NFR](../../u05-class-enrollment/nfr-requirements/nfr-requirements.md), [infrastructure](../../u05-class-enrollment/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/academics/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../academics/contract/` | interface `IClassAccess`; kiểu dữ liệu `ClassRef`, `ClassStatus` |
| `backend/.../academics/contract/stub/AcademicsContractStubs` | Bản tạm trả dữ liệu mẫu cho `IClassAccess` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `frontend/src/features/academics/slots.ts` | Slot đã khai báo: `studentClassSections`, `teacherClassTabs`, `studentDetailSections`, `dashboardNavEntries` |

## Checklist

- [ ] **Bước 1 — Migration:** `db/migration/classes/V20260925_1100__create_classes_enrollments.sql` theo infrastructure-design.
- [ ] **Bước 2 — Domain:** `CourseClass`, `ClassStatus`, `Enrollment`, `EnrollmentStatus`, `EnrollmentSource`, kiểm chuyển trạng thái (BR-U05-14); repository chỉ dùng trong package.
- [ ] **Bước 3 — Interface:** `ClassScopeService` cài `IClassScope` (U01), `ISubjectClasses` (U04), `IClassAccess`; đọc không cache.
- [ ] **Bước 4 — Service:** `ClassViewService` (F1–F6, F8), `ClassAdminService` (F7, F9, F13, F14), `EnrollmentService` (F10–F12, khóa hàng); kiểm quyền qua `IAuthorization`, môn qua `ISubjectQuery`, tài khoản qua `IAccountLookup`; audit theo BR-U05-52; phát `enrollment.activated`.
- [ ] **Bước 5 — API:** 3 controller theo domain-entities mục 5; ngoài phạm vi trả `404`.
- [ ] **Bước 6 — Frontend:** các màn ở frontend-components; vẽ các slot `StudentClassSection`, `TeacherClassTab`, `StudentDetailSection`, `DashboardNavEntry` đã khai báo trong `src/features/academics/slots.ts`.
- [ ] **Bước 7 — Unit test (người code viết):** chuyển trạng thái hợp lệ và không hợp lệ, mở lớp thiếu giảng viên, mở lại bị vướng, ghi danh lần đầu, khôi phục, đã ghi danh, ở lớp khác cùng môn, danh sách có dòng lỗi, gỡ rồi mất quyền, đổi giảng viên làm người cũ mất quyền, Admin bị từ chối, phát sự kiện khi lớp `OPEN`; test component frontend. Integration test do tester viết.
- [ ] **Bước 8 — Review:** đối chiếu UC 20–30, story, BR-U05 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Hai yêu cầu ghi danh đồng thời một sinh viên vào hai lớp cùng môn: chỉ một thành công.
- Gỡ sinh viên rồi dùng phiên của sinh viên vào lớp: bị từ chối ngay; đổi giảng viên rồi dùng phiên giảng viên cũ: bị từ chối.
- Thêm theo danh sách 200 email có đủ các loại kết quả dòng.
- Phân quyền 4 role trên mọi endpoint, gồm dùng ID lớp của người khác.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
