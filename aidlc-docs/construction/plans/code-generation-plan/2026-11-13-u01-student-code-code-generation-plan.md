# U01 (kèm U05/U09/U11/U12): Mã số sinh viên, kế hoạch Code Generation

Kế hoạch này là nguồn duy nhất cho bước Code Generation của tính năng MSSV. Kế hoạch dựa trên các tài liệu đã duyệt ngày 2026-11-13:

- [Yêu cầu SC-01…11](../../../inception/requirements/2026-11-13-student-code.md)
- [Kế hoạch thực hiện](../../../inception/plans/2026-11-13-student-code-execution-plan.md)
- Functional design:
  - [U01 BR-U01-100…115](../../u01-account-and-access/functional-design/business-rules.md)
  - U05 BR-U05-56…60
  - U09 BR-U09-73…75
  - U11 BR-U11-42
  - U12 BR-U12-97…98

## Bối cảnh

- **Nhánh**: `fix/student-code`, đang ở `3b86e57d`. Lúc bắt đầu phiên, nhánh sạch. Hiện nhánh chỉ có tài liệu AIDLC chưa commit.
- **Không có user story**: giai đoạn User Stories đã bỏ qua. Mỗi bước được truy về mã yêu cầu SC-xx và mã quy tắc BR.
- **Quy ước**: theo [code-conventions.md](../../code-conventions.md) và [persistence-conventions.md](../../persistence-conventions.md).
  - Persistence chỉ dùng Spring JDBC.
  - Tên service theo dạng động từ + đối tượng.
  - Field dependency có hậu tố `Service`.
- **Tương thích**:
  - `AccountContact` có thêm thành phần `studentCode`. Constructor 5 tham số cũ được giữ lại và gán `studentCode = null`, nên khoảng 20 test và stub đang dùng constructor này vẫn biên dịch được.
  - Các DTO `Person`/`PersonView`/`MemberView`/`CandidateView` chỉ thêm trường mới, JSON cũ không đổi.
- **Unit sở hữu dữ liệu**: chỉ U01 sở hữu bảng `accounts`. Các unit khác đọc MSSV qua `IAccountLookup`/`AccountContact`, không truy vấn thẳng bảng `accounts`.
- **Extension**: bật Security và Resiliency, tắt PBT.

## Các bước

### A. Backend U01: dữ liệu và model

- [x] **Bước 1: Migration** (SC-01, NFR-SC-04, BR-U01-101, 102)
  - Tạo `backend/src/main/resources/db/migration/identity/V20261113_1600__add_account_student_code.sql`, gồm:
    - Cột `student_code varchar(8)`.
    - Unique index `ux_accounts_student_code`, chỉ áp dụng khi `student_code` có giá trị.
    - Check định dạng.
    - Check `role = 'STUDENT' or student_code is null`.
  - Kiểm lại grant cho app role nếu migration hiện có cấp quyền theo cột.
- [x] **Bước 2: Model** (BR-U01-100)
  - Sửa `identity/model/Account.java`: thêm `studentCode`, cập nhật `newPending`.
  - Tạo `identity/model/StudentCodePolicy.java` với các hàm `normalize`, `isValid`, `looksLikeStudentCode`.
  - Test: tạo `StudentCodePolicyTest`, sửa `AccountTest`.
- [x] **Bước 3: Repository** (BR-U01-101, 112)
  - Sửa `identity/repository/AccountRepository.java`:
    - Row mapper, `insert` và `update` có thêm cột mới.
    - Ô tìm `search` và `findStudents`/`findAccounts` khớp thêm `student_code`.
    - Thêm `existingStudentCodes(codes)`, `findByStudentCodes(codes)` (chỉ trả role `STUDENT`).
    - Thêm `assignStudentCodes(...)`: chỉ ghi khi `student_code is null and role = 'STUDENT'`, có tăng `version`.
    - Lỗi trùng của database được chuyển thành lỗi miền.
  - Test: sửa `AccountRepositoryTest`, `AccountRepositoryIntegrationTest` (PostgreSQL). Trường hợp cần test: unique index, hai check constraint, tìm theo mã, trả đúng số dòng đã gán.

### B. Backend U01: nghiệp vụ

- [x] **Bước 4: Contract** (BR-U01-113)
  - Sửa `identity/contract/AccountContact.java`: thêm `studentCode`, giữ constructor cũ.
  - Sửa `IAccountLookup`: thêm `findByStudentCodes` có cài đặt mặc định.
  - Sửa `identity/contract/stub/IdentityContractStubs.java`.
  - Sửa `identity/service/AccountLookupService.java` để map thêm mã.
  - Test: sửa `AccountLookupAndPolicyTest`, `NotificationAccountLookupTest`.
- [x] **Bước 5: Tạo, sửa và đổi role** (BR-U01-103, 105, 107, 115)
  - Sửa `identity/service/AccountAdminService.java`:
    - `create` nhận `studentCode`.
    - `update` áp quy tắc BR-U01-105, ném `STUDENT_CODE_LOCKED`/`STUDENT_CODE_EXISTS`/`STUDENT_CODE_NOT_ALLOWED`/`INVALID_STUDENT_CODE`/`MISSING_STUDENT_CODE`.
    - `changeRole` chặn mọi lần đổi vào hoặc ra khỏi `STUDENT` bằng `STUDENT_ROLE_CHANGE_NOT_ALLOWED`, kiểm trước BR-U01-64…66.
    - Metadata audit chỉ ghi thao tác, không ghi giá trị mã.
  - Sửa `identity/service/AccountViews.java`: thêm `studentCode` và `studentCodeEditable`.
  - Test: sửa `AccountAdminServiceTest`, phủ đủ 7 nhánh của BR-U01-105 và mọi chiều đổi role vào hoặc ra khỏi `STUDENT`.
- [x] **Bước 6: Nhập tài khoản có cột MSSV** (BR-U01-108)
  - Sửa `identity/service/AccountImportService.java`:
    - Đọc cột tùy chọn `student_code` từ cả XLSX và CSV.
    - Thêm 5 lỗi mới.
    - Tra trùng theo lô bằng `existingStudentCodes`.
    - Ghi mã khi tạo tài khoản.
  - Test: sửa `AccountImportServiceTest`.
- [x] **Bước 7: Gán MSSV hàng loạt (F16)** (BR-U01-109…111, 115)
  - Tạo `identity/service/StudentCodeImportService.java` với 2 hàm `validateStudentCodes` và `commitStudentCodes`.
  - Dùng chung bộ đọc XLSX/CSV với `AccountImportService` bằng cách tách thành helper package-private. Không đổi hành vi của luồng nhập tài khoản.
  - Thứ tự xét lỗi theo BR-U01-110. Bước xác nhận chạy trong một transaction. Gặp xung đột thì trả `IMPORT_CONFLICT`. Ghi audit `STUDENT_CODES_ASSIGNED`.
  - Test: tạo `StudentCodeImportServiceTest`, phủ từng mã lỗi, trường hợp trùng trong tệp, tài khoản `DISABLED`, `UNCHANGED`, và xung đột khi xác nhận.
- [x] **Bước 8: Hồ sơ** (BR-U01-106, SC-05)
  - Sửa `identity/service/ProfileService.java`: `ProfileView` có thêm `studentCode`. DTO `PATCH` không đổi.
  - Test: sửa `ProfileAndPasswordChangeTest`.
- [x] **Bước 9: Controller và xử lý lỗi**
  - Sửa `identity/controller/AdminAccountController.java`:
    - Request tạo và sửa tài khoản có thêm `studentCode`.
    - Request đổi role không thêm trường nào.
    - Thêm endpoint `POST /api/v1/admin/accounts/student-code-imports:validate` và `:commit`.
  - Sửa `MeController.java`.
  - Sửa `IdentityExceptionHandler.java`: thêm HTTP status cho các mã lỗi mới. Lỗi kiểm dữ liệu trả `400`; `STUDENT_CODE_EXISTS`, `STUDENT_CODE_LOCKED`, `STUDENT_ROLE_CHANGE_NOT_ALLOWED`, `IMPORT_CONFLICT` trả `409`. Thống nhất với cách xử lý lỗi hiện có.
  - Test: sửa `IdentityWebSecurityTest` hoặc test controller hiện có. Trường hợp cần test: chỉ ADMIN gọi được các endpoint mới, và request có trường lạ bị từ chối.

### C. Backend: các unit phụ thuộc

- [x] **Bước 10: U05 ghi danh** (BR-U05-56…60)
  - Sửa `academics/model/EnrollmentListParser.java`:
    - Nhận cả email lẫn MSSV.
    - Header ở dòng đầu là `email`, `student_code` hoặc `mssv`.
    - Giá trị không hợp lệ báo `INVALID_IDENTIFIER`.
  - Sửa enum `EnrollmentResult`: đổi `INVALID_EMAIL` thành `INVALID_IDENTIFIER`.
  - Sửa `AcademicsService.enrollStudents`: tra theo lô bằng `findByEmails` và `findByStudentCodes`.
  - Sửa `AcademicsDtos.Person`/`Student`: thêm `studentCode`, cập nhật `mapPerson`.
  - Test: sửa `AcademicsServiceTest`, `AcademicsBatchTransactionTest`, test parser hiện có.
- [x] **Bước 11: U09 chấm điểm** (BR-U09-73…75)
  - Sửa `grading/dto/GradingViews.Person`: thêm `studentCode`.
  - Sửa `GradebookService`:
    - Ô tìm khớp thêm MSSV.
    - Hàng của ma trận có MSSV.
  - Sửa `GradebookExporter`: thêm cột "MSSV" sau STT, ô kiểu văn bản, có chống công thức, nằm trong vùng cố định.
  - Sửa `SubmissionReviewService`.
  - Test: sửa `GradebookAssemblyTest` và test exporter hiện có.
- [x] **Bước 12: U11 lượt làm quiz** (BR-U11-42)
  - Thêm `studentCode` vào view sinh viên của `quizpractice/service/TeacherQuizAttemptService` và `PracticeViews`.
  - Test: sửa `QuizPracticeServicesTest`.
- [x] **Bước 13: U12 nhóm** (BR-U12-97, 98)
  - Thêm `studentCode` vào các DTO sau:
    - `ClassGroupService.PersonView`/`MemberView`
    - `ProposalPeople.Person`
    - `GroupProposalService.MemberView`/`CandidateView`
    - `MyGroupService.Member`
  - Test: sửa các test nhóm liên quan.

### D. Frontend

- [x] **Bước 14: Types và API identity**
  - Sửa `frontend/src/features/identity/model/types.ts`, `api/index.ts`, `admin/api/index.ts`.
  - Thêm các hàm gọi API `validateStudentCodeImport` và `commitStudentCodeImport`.
  - Tạo helper `frontend/src/shared/studentCode.ts` gồm `normalizeStudentCode`, `isValidStudentCode` và câu báo lỗi.
- [x] **Bước 15: Màn Admin** (BR-U01-103…112, frontend-components U01 mục 6)
  - `AccountListPage`: thêm cột MSSV, đổi chữ gợi ý ô tìm, thêm nút "Gán MSSV từ Excel".
  - `NewAccountDialog`: thêm ô MSSV, chỉ hiện khi chọn vai trò Sinh viên.
  - `AccountDetailPage`: ô MSSV ở 3 trạng thái (gán mã, sửa mã, chỉ đọc).
  - `AccountDialogs`, phần đổi vai trò:
    - Ẩn nút "Đổi vai trò" với tài khoản sinh viên.
    - Bỏ lựa chọn Sinh viên khỏi danh sách vai trò.
  - `ImportAccountsPanel`: thêm cột MSSV vào bảng và vào tệp mẫu.
  - Tạo `AssignStudentCodesPanel.tsx`.
  - Thêm `data-testid` theo mẫu `{component}-{element-role}`.
- [x] **Bước 16: Hồ sơ**
  - `ProfileForm`: hiện dòng MSSV chỉ đọc cho sinh viên.
- [x] **Bước 17: U05, U09, U11, U12**
  - Thêm `studentCode` vào types của từng feature.
  - Bảng có cột MSSV đặt trước họ tên. Thẻ có dòng phụ `SE123456 · email`.
  - Các màn cần sửa:
    - U05: `EnrollmentPanel` (đổi chữ gợi ý, câu báo `INVALID_IDENTIFIER`, tệp mẫu), `PersonPicker`, `ManagedClassPage`, `StudentClassPage`.
    - U09: `GradebookMatrix`, `ClassGradebookTab`, `GradingWorkspaceHeader`, `SubmissionDetailPage`.
    - U11: `TeacherQuizSections`.
    - U12: `ClassGroupsPanel`, `GroupFormDialog`, `ProposalReviewList`, `MyGroup`, `GroupProposalSection`, `ProposalDialogs` (lọc ứng viên theo MSSV).
  - Tạo component dùng chung `frontend/src/shared/components/PersonSubline.tsx` để hiện dòng phụ, dùng thay cho việc lặp code ở từng màn.
- [x] **Bước 18: Test frontend**
  - Thêm hoặc sửa test Vitest trong `frontend/tests/features/...`:
    - Form tạo tài khoản, ô MSSV ở trang chi tiết, panel gán MSSV hàng loạt.
    - Popup đổi vai trò không có lựa chọn Sinh viên.
    - Thông báo lỗi `INVALID_IDENTIFIER` khi ghi danh.
    - Cột MSSV trong bảng điểm, lọc ứng viên mời nhóm theo MSSV.
    - Helper `studentCode`.

### E. Tài liệu

- [x] **Bước 19: Tóm tắt code**
  - Tạo `aidlc-docs/construction/u01-account-and-access/code/2026-11-13-student-code-summary.md`. Nội dung gồm: file đã sửa và file đã tạo, API mới, migration, test đã viết, và điều chỉnh so với thiết kế nếu có.
- [x] **Bước 20: Kiểm tra nhanh theo phạm vi**
  - Chạy `mvn -q -Dtest=...` cho các test đã sửa ở backend.
  - Chạy `tsc`, `eslint` và Vitest cho các file đã sửa ở frontend.
  - Toàn bộ `mvn verify` và các bài thử trên trình duyệt để sang giai đoạn Build and Test.

## Không làm

- Không có chức năng đổi MSSV ngoài trường hợp sửa mã khi tài khoản còn `PENDING`.
- Không có bộ lọc "Sinh viên chưa có MSSV".
- Không đăng nhập bằng MSSV.
- Không sắp xếp theo MSSV.
- Không đưa MSSV vào AI, thông báo hay email.
- Không đổi role vào hoặc ra khỏi `STUDENT`.
- Không commit, push hay deploy khi người dùng chưa yêu cầu.

## Rủi ro khi thực hiện

- **Đổi tên `INVALID_EMAIL` thành `INVALID_IDENTIFIER` ở U05**: phải tìm và sửa mọi chỗ dùng mã này ở frontend và test.
- **Tách bộ đọc tệp của `AccountImportService`**: phải giữ nguyên hành vi hiện có, gồm BOM, dòng thiếu cột, giới hạn kích thước và checksum. Test hiện có phải vẫn qua.
- **Thêm cột vào tệp xuất bảng điểm**: thứ tự cột trong XLSX/CSV thay đổi, nên test hiện có kiểm theo chỉ số cột phải được sửa theo.

## Kết quả

Cả 20 bước đã xong ngày 2026-11-13. Xem [tóm tắt và các điều chỉnh](../../u01-account-and-access/code/2026-11-13-student-code-summary.md).

- 2026-11-14: bỏ chức năng gán MSSV cho tài khoản cũ (F16 và trường hợp gán ở trang chi tiết), xem phần cuối của [tóm tắt](../../u01-account-and-access/code/2026-11-13-student-code-summary.md).
