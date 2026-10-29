# U04 Subject Administration — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U04 Subject Administration thuộc Iter 1, người code: Người 4; UC 16–19. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Thiết kế: [business-logic-model](../../u04-subject-administration/functional-design/business-logic-model.md), [business-rules](../../u04-subject-administration/functional-design/business-rules.md), [domain-entities](../../u04-subject-administration/functional-design/domain-entities.md), [frontend-components](../../u04-subject-administration/functional-design/frontend-components.md), [NFR](../../u04-subject-administration/nfr-requirements/nfr-requirements.md), [infrastructure](../../u04-subject-administration/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/subjects/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../subjects/contract/` | interface `ISubjectClasses`, `ISubjectQuery`; kiểu dữ liệu `SubjectClassSummary`, `SubjectRef`, `SubjectStatus` |
| `backend/.../subjects/contract/stub/SubjectsContractStubs` | Bản tạm trả dữ liệu mẫu cho `ISubjectQuery`, `ISubjectClasses` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `frontend/src/features/subjects/` | Chỉ có `index.ts` trống |

## Checklist

- [ ] **Bước 1 — Migration:** `db/migration/subjects/V20260925_1000__create_subjects.sql` theo infrastructure-design.
- [ ] **Bước 2 — Domain:** package `edu.aiplatform.subjects`: `Subject`, `SubjectStatus`, `SubjectCode` (kiểm định dạng), `ISubjectRepository` dùng trong package.
- [ ] **Bước 3 — Interface:** `ISubjectQuery`, `SubjectRef`; contract `ISubjectClasses`, `SubjectClassSummary`; `SubjectScopeService` cài `ISubjectScope` của U01; `SubjectQueryService`; tới khi U05 cài, bản tạm `ISubjectClasses` trong khung trả danh sách rỗng và số lớp 0.
- [ ] **Bước 4 — Service:** `SubjectAdminService` cho F1–F6: kiểm `ADMIN`, kiểm mã và Chủ nhiệm môn qua `IAccountLookup`, khóa lạc quan, chặn lưu trữ qua `ISubjectClasses`, audit theo BR-U04-52.
- [ ] **Bước 5 — API:** `SubjectAdminController` cho 7 endpoint ở domain-entities mục 4; lỗi dạng an toàn.
- [ ] **Bước 6 — Frontend:** `SubjectListPage`, `SubjectFilters`, `SubjectTable`, `SubjectFormDialog`, `ManagerPicker`, `SubjectDetailPage`, `SubjectEditForm`, `ManagerCard`, `ManagerChangeDialog`, `SubjectMetaCard`, `SubjectStatusActions`, `ArchiveRejectedDialog`, `SubjectClassesTable`; đăng ký `NavEntry` mục Môn học.
- [ ] **Bước 7 — Unit test (người code viết):** mã sai định dạng và trùng, Chủ nhiệm môn sai role hoặc không `ACTIVE`, version lệch, lưu trữ khi còn lớp hoặc khi U05 không trả lời, đổi Chủ nhiệm môn làm người cũ mất quyền, người không phải Admin bị từ chối; test component frontend. Integration test do tester viết.
- [ ] **Bước 8 — Review:** đối chiếu UC 16–19, US-CAT-001, BR-U04 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Tạo môn với mã trùng, mã sai định dạng; gán người không phải Subject Manager.
- Đổi Chủ nhiệm môn rồi dùng phiên của người cũ thao tác tài nguyên môn: bị từ chối ngay.
- Lưu trữ môn còn lớp đang mở bị từ chối; lưu trữ rồi mở lại.
- Người không phải Admin gọi từng endpoint quản trị môn.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
