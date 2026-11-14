# Mã số sinh viên (MSSV): tóm tắt Code Generation

Tài liệu này tóm tắt kết quả thực hiện [kế hoạch 20 bước](../../plans/code-generation-plan/2026-11-13-u01-student-code-code-generation-plan.md) trên nhánh `fix/student-code`, ngày 2026-11-13. Các thay đổi chưa được commit.

Phạm vi bám theo các quy tắc nghiệp vụ:

- BR-U01-100…115
- BR-U05-56…60
- BR-U09-73…75
- BR-U11-42
- BR-U12-97, 98

## Backend

**Tệp mới:**

- `db/migration/identity/V20261113_1600__add_account_student_code.sql`
  - Cột `student_code varchar(8)`, cho phép trống.
  - Check định dạng `accounts_student_code_format`.
  - Check `accounts_student_code_role`: chỉ tài khoản STUDENT được có mã.
  - Unique index `accounts_student_code_key`, chỉ áp dụng khi cột có giá trị.
- `identity/contract/StudentCodeFormat.java`: chuẩn hóa và kiểm định dạng mã, dùng chung cho các unit.
- `identity/model/StudentCodePolicy.java`: lớp mỏng, gọi sang `StudentCodeFormat`.
- `identity/service/ImportTableReader.java`: bộ đọc tệp XLSX/CSV, tách ra từ `AccountImportService`.

**Tệp sửa trong U01:**

- `Account` thêm `studentCode` và vẫn giữ constructor cũ 13 tham số.
- `AccountRepository`:
  - Thêm cột mới vào các câu lệnh.
  - Tìm `q` và `findForLookup` khớp thêm theo MSSV.
  - Thêm các hàm `existingStudentCodes`, `findStudentsByCodes`, `updateAccountDetails`, `assignStudentCodes` và `isStudentCodeConflict`.
- `AccountAdminService`:
  - `create` và `update` có overload nhận MSSV.
  - `changeRole` từ chối mọi lần đổi role vào hoặc ra khỏi STUDENT, kèm audit `ROLE_CHANGE_REJECTED`.
- `AccountViews` thêm `studentCode` và `studentCodeEditable`.
- `AccountImportService` thêm cột `student_code` và các lỗi mới.
- `ProfileService.ProfileView` thêm `studentCode`.
- `AdminAccountController` thêm `studentCode` vào request tạo và sửa tài khoản.
- `AccountContact` thêm `studentCode` và vẫn giữ constructor cũ 5 tham số.
- `IAccountLookup` thêm `findByStudentCodes` với cài đặt mặc định.
- `AccountLookupService` cập nhật theo.

**Tệp sửa trong các unit khác:**

- **U05:**
  - `EnrollmentListParser` nhận cả email lẫn MSSV, với header là `email`, `student_code` hoặc `mssv`.
  - `EnrollmentResult.INVALID_EMAIL` đổi thành `INVALID_IDENTIFIER`.
  - `AcademicsService.enrollStudents` tra email và MSSV theo lô, mỗi loại một lần.
  - `AcademicsDtos.Person`/`Student` thêm `studentCode`.
- **U09:**
  - `GradingViews.Person` thêm `studentCode`.
  - Tìm kiếm trong bảng điểm khớp thêm MSSV.
  - Tệp xuất có cột "MSSV" sau STT.
  - `GradebookExporter` cố định 4 cột định danh (`xSplit=4`), ô MSSV kiểu văn bản.
  - `SubmissionReviewService` cập nhật theo.
- **U11:** `PracticeViews.StudentAttempts` thêm `studentCode`.
- **U12:** thêm `studentCode` vào `ClassGroupService.PersonView`/`MemberView`, `ProposalPeople.Person`, `GroupProposalService.MemberView`/`CandidateView` và `MyGroupService.Member`.

## Frontend

**Tệp mới:**

- `src/shared/studentCode.ts`: chuẩn hóa và kiểm định dạng mã, câu báo lỗi, hàm `matchesPersonQuery` tìm theo tên/email/MSSV.
- `src/shared/ui/PersonSubline.tsx`: dòng phụ dạng `SE123456 · email`.

**Tệp sửa trong identity:**

- API và types.
- Labels: thêm các mã lỗi mới.
- `NewAccountDialog`: thêm ô MSSV khi chọn vai trò Sinh viên.
- `AccountDetailPage`:
  - Ô MSSV ở 3 trạng thái: gán mới, sửa khi còn PENDING, chỉ đọc.
  - Ẩn nút Đổi vai trò với tài khoản sinh viên.
- `AccountDialogs`: danh sách vai trò không còn lựa chọn Sinh viên.
- `AccountListPage`:
  - Thêm cột MSSV.
  - Chữ gợi ý ô tìm nhắc đến MSSV.
- `ImportAccountsPanel` thêm cột MSSV.
- `ProfileForm` hiện MSSV ở dạng chỉ đọc.
- Tệp mẫu `account-import.xlsx` thêm cột `student_code`.

**Tệp sửa trong các unit khác:**

- **U05:**
  - `EnrollmentPanel`: cột MSSV, tìm kiếm theo MSSV, nhận danh sách email hoặc MSSV.
  - `PersonPicker` hiện MSSV.
  - `enrollmentList` và `enrollmentWorkbook` nhận email hoặc MSSV.
  - Tệp mẫu `enrollment-import.xlsx` thêm một dòng ví dụ MSSV.
- **U09:**
  - `GradebookMatrix` có cột MSSV cố định.
  - `ClassGradebookTab`, `GradingWorkspaceHeader`, `GradebookBatchControls` và `SubmissionDetailPage` hiện MSSV.
- **U11:** `TeacherQuizSections` có cột MSSV.
- **U12:**
  - `ClassGroupsPanel`, `GroupFormDialog`, `GroupProposalSection`, `MyGroup` và `ProposalReviewList` hiện MSSV.
  - `ProposalDialogs` lọc ứng viên mời theo MSSV.

## Điều chỉnh so với thiết kế

1. **Định dạng MSSV nằm ở `identity.contract.StudentCodeFormat`.** ArchitectureTest cấm unit khác dùng package `model` của U01, nên U05 phải lấy định dạng mã qua contract.
2. **`AcademicsDtos.RowResult` giữ tên trường `email`.** Trường này chứa đúng giá trị đã nhập, có thể là email hoặc MSSV. Giữ tên cũ để không đổi hợp đồng JSON.
3. **Gợi ý dưới ô nhập được viết thành dòng chữ riêng.** Thuộc tính `hint` của `Field` theo thiết kế không hiện ra.
4. **Một số màn U12 không có cột MSSV riêng.**
   - Ở bảng sinh viên của `ClassGroupsPanel`, MSSV hiện ở dòng phụ trong cột Email.
   - Ở `ProposalReviewList`, MSSV hiện ngay sau tên.
   - Bảng điểm, danh sách tài khoản, danh sách lớp và lượt làm quiz vẫn có cột MSSV riêng.
5. **Không sửa trang lớp phía sinh viên (`StudentClassPage`).** Trang này không có danh sách bạn cùng lớp, chỉ có thông tin giảng viên. Phần SC-11 được làm ở các màn nhóm, đề xuất nhóm và ô mời bạn.

## Kiểm tra trong bước này

**Backend:**

- Lệnh: `mvn -o test` cho các module identity, academics, grading, groups, quizpractice và ArchitectureTest.
- Kết quả: 805 test, 0 failure.
- Có 2 lỗi do máy không có Docker: `GroupProposalRepositoryIntegrationTest` và `GroupProposalFlowIntegrationTest`.
- 56 test bị bỏ qua vì không có Docker, gồm cả 2 test PostgreSQL mới trong `AccountRepositoryIntegrationTest`.

**Frontend:**

- `tsc --noEmit` không có lỗi, trừ các type cũ được sinh sẵn trong `.next/`.
- ESLint `src` và `tests` sạch.
- Vitest: 1043 test qua.
  - Khi chạy toàn bộ suite trên máy đang tải nặng, có 5 test bị timeout 5 giây: authoringFlow, ClassAnnouncements, enrollmentSelection và bulk-selection.
  - Chạy lại riêng các file đó thì 25/25 test qua.
- Đã thêm test cho `studentCode`.

**Chưa làm, để lại cho Build and Test:**

- Chạy toàn bộ `mvn verify` có PostgreSQL. Bước này cần Docker.
- Thử trên trình duyệt với đủ 3 vai trò.

## Sửa thêm khi thử trên trình duyệt (2026-11-14)

- **`GradingWorkspacePage` và `GradingWorkspaceHeader`:** header trang chấm một bài hiện MSSV (`studentCode`), theo BR-U09-75.
- **`GroupProposalSection`:** dòng thành viên vẫn hiện MSSV khi có ghi chú, ví dụ `SE000055 · Trưởng nhóm`.
- **`GradebookMatrix`:** ở màn hẹp, MSSV gộp vào ô Sinh viên; từ `sm` trở lên vẫn là cột riêng cố định.
- **Test:** cập nhật `workspace.test.tsx` và `gradebook-matrix.test.tsx`.

## Thu hẹp phạm vi sau khi duyệt mã (2026-11-14)

Mọi tài khoản sinh viên sẽ có MSSV ngay từ lúc tạo, nên người dùng bỏ chức năng gán MSSV cho tài khoản cũ chưa có mã. Database vẫn cho phép cột `student_code` để trống.

**Đã gỡ:**

- Luồng gán MSSV hàng loạt (F16):
  - Backend: `StudentCodeImportService`, 2 endpoint `student-code-imports:*` và hàm `AccountRepository.assignStudentCodes`.
  - Frontend: `AssignStudentCodesPanel`, nút "Gán MSSV từ Excel" và tệp mẫu `student-code-import.xlsx`.
  - Toàn bộ test của luồng này.
- Trường hợp gán mã cho tài khoản chưa có mã ở trang chi tiết.

**Quy tắc sửa MSSV bây giờ:**

- Admin chỉ sửa được MSSV khi tài khoản còn `PENDING`.
- Tài khoản không còn `PENDING`, kể cả tài khoản cũ chưa có mã, sẽ bị chặn với lỗi `STUDENT_CODE_LOCKED`.
- `studentCodeEditable` là `true` chỉ khi tài khoản là `STUDENT` và đang `PENDING`.
- Tài khoản cũ chưa có mã thì trang chi tiết hiện "Chưa có MSSV" ở dạng chỉ đọc.
