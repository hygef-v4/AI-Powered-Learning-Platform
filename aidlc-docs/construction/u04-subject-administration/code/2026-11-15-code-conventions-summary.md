# U04 — Áp dụng quy ước mã nguồn (2026-11-15)

Thay đổi chỉ đổi tên và định dạng lại theo [Shared Code Conventions](../../code-conventions.md). Hành vi, URL, HTTP method, JSON, SQL, schema, migration, sự kiện, mã lỗi và message giữ nguyên. Danh mục đổi tên đầy đủ nằm trong [kế hoạch sinh mã](../../plans/code-generation-plan/2026-11-15-u04-u13-u14-code-conventions-code-generation-plan.md).

## Backend

- Sửa 20 file trong `backend/src/main/java/edu/aiplatform/subjects/`:
  - field phụ thuộc đặt theo kiểu, bỏ tiền tố `I` (ví dụ `subjectRepository`, `subjectAdminAccess`, `accountLookup`, `subjectClassesReader`, `subjectAuditRecorder`);
  - method theo mẫu động từ + đối tượng: controller `listSubjects`, `loadSubject`, `updateSubjectInformation`…; repository `findSubjectPage`, `insertSubject`…; handler `handle…`.
- Giữ nguyên:
  - hợp đồng `subjects.contract`, gồm các method hiện thực `ISubjectScope` và các interface khác;
  - tên `@Bean` (`subjectQueryStub`, `subjectClassesStub`);
  - factory tĩnh `Subject.create`;
  - method nghiệp vụ của domain model (`archive`, `reopen`, `revise`);
  - Spring Data derived query.
- Dòng code dài hơn 100 ký tự được xuống dòng; còn lại chỉ là chuỗi SQL hoặc literal.
- Sửa 11 file test trong `backend/src/test/java/edu/aiplatform/subjects/` và lời gọi trong `requirements/RequirementsCoverageIntegrationTest.java`.

## Frontend

- Đổi tên file `hooks/subjectMutation.tsx` thành `hooks/useSubjectMutation.tsx` (`git mv`).
- API:
  - `getSubjects` → `listSubjects`, `getSubject` → `loadSubject`, `getSubjectClasses` → `listSubjectClasses`;
  - `subjectPath` → `buildSubjectPath`.
- Helper:
  - `subjectProblem` → `readSubjectProblem`, `needsSubjectReload` → `shouldReloadSubject`;
  - `run` → `runMutation`;
  - `ManagerPicker.label` → `formatManagerLabel`.
- Callback UI đổi sang `handle…`: `handleSubmit`, `handleClose`, `handleReset`, `handleChoose`, `handleReload`, `handleSaved`.
- Prop callback (`onSaved`, `onClose`…) giữ nguyên.
- Giữ `reload` mà `useSubjectRead` trả về vì đây là hình dạng chung của hook đọc dữ liệu ở nhiều feature.
- Sửa 10 file trong `frontend/src/features/subjects/` và 4 file test. Khóa trong `vi.mock` cũng đổi theo tên mới.

## Kiểm tra

- Backend `mvn -o -B clean test` (subjects, materials, announcements, notifications): 493 test, 0 lỗi; 40 test bỏ qua vì máy không có Docker (Testcontainers PostgreSQL).
- So sánh tập literal trong diff: không đổi chuỗi nào.
- Frontend:
  - `tsc --noEmit` và ESLint sạch;
  - Vitest bốn feature: 23 file, 178 test đạt.
