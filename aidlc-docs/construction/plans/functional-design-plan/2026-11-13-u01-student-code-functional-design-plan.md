# U01 (kèm U05/U09/U11/U12) — Mã số sinh viên — kế hoạch Functional Design

Kế hoạch dựa trên 2 tài liệu đã duyệt ngày 2026-11-13:

- [Yêu cầu SC-01…11](../../../inception/requirements/2026-11-13-student-code.md), đã thu hẹp phạm vi theo Q6 và Q7.
- [Kế hoạch thực hiện](../../../inception/plans/2026-11-13-student-code-execution-plan.md).

## Bối cảnh từ mã hiện tại

### U01 — tài khoản

- `Account` là một record. `AccountAdminService` có các thao tác tạo, sửa và đổi role/trạng thái tài khoản; các thao tác sửa kiểm `version` để tránh ghi đè.
  - `create(email, displayName, role)`
  - `update(displayName, phoneNumber, version)`
  - `changeRole(role, version)`
  - `changeStatus(status, version)`
- `StrictRequest` từ chối mọi trường lạ trong request. Vì vậy, muốn nhận MSSV thì phải khai báo trường mới trong DTO.
- `AccountImportService` có 2 bước: kiểm tra, rồi xác nhận.
  - Bước xác nhận kiểm lại toàn bộ tệp trong một transaction.
  - Gặp `DuplicateKeyException` thì trả `IMPORT_CONFLICT`.
  - Cột bắt buộc: `email`, `display_name`, `role`.
- `AccountRepository.search` lọc `ilike` theo email và tên hiển thị.
- `AccountContact` được nhiều module dùng chung. Hàm `getContacts` lấy thông tin theo lô bằng một truy vấn.

### U05 — ghi danh

- `EnrollmentListParser` chỉ nhận email, tối đa 200 dòng. Lỗi theo dòng là `INVALID_EMAIL` hoặc `DUPLICATE_IN_LIST`.
- `AcademicsService.enrollStudents` tra tài khoản bằng `findByEmails` và chạy mỗi dòng trong một transaction riêng.

### Các module chỉ hiển thị

- U09: `GradebookService`, `GradebookExporter`, `SubmissionReviewService`.
- U11: `TeacherQuizAttemptService`.
- U12: `ClassGroupService.PersonView`, `ProposalPeople`, `GroupProposalService.MemberView`.
- Các module này lấy tên và email từ `AccountContact`, rồi trả ra qua DTO `Person`/`PersonView`/`MemberView` gồm 3 trường `(id, name, email)`.

## Các bước

- [x] 1. Domain entities U01
  - Thêm thuộc tính `studentCode` cho `Account`.
  - Thêm ràng buộc dữ liệu: định dạng, duy nhất, theo role.
  - Thêm `StudentCodePolicy`.
  - Thêm `studentCode` vào `AccountContact`.
- [x] 2. Business rules U01: thêm nhóm quy tắc mới từ BR-U01-100 trở đi.
  - Định dạng và chuẩn hóa mã.
  - Khi nào bắt buộc nhập mã.
  - Khi nào được gán, sửa hoặc khóa mã.
  - Xử lý khi đổi role.
  - Thêm cột MSSV khi nhập tài khoản.
  - Gán mã hàng loạt.
  - Tìm kiếm theo MSSV.
  - Hồ sơ.
  - Audit.
- [x] 3. Business logic model U01
  - Luồng tạo, sửa và đổi role tài khoản.
  - Luồng nhập tài khoản có thêm cột MSSV.
  - Luồng gán mã hàng loạt, gồm bước kiểm tra và bước xác nhận.
  - Thứ tự xét lỗi cho từng dòng.
- [x] 4. Frontend components U01
  - Form tạo tài khoản.
  - Trang chi tiết tài khoản.
  - Danh sách tài khoản.
  - Panel nhập tài khoản.
  - Panel gán mã hàng loạt mới.
  - Trang hồ sơ.
- [x] 5. Phần sửa U05
  - Parser nhận cả email và MSSV.
  - Kết quả theo từng dòng.
  - Tìm ứng viên để ghi danh.
  - Hiện MSSV trong danh sách lớp, cho nhân sự và cho sinh viên.
- [x] 6. Phần sửa U09
  - Cột MSSV trong bảng điểm và trong file xuất.
  - Tìm kiếm theo MSSV.
  - Hiện MSSV ở trang chấm bài và chi tiết bài nộp.
- [x] 7. Phần sửa U11: hiện MSSV trong danh sách lượt làm quiz.
- [x] 8. Phần sửa U12
  - Hiện MSSV trong nhóm và đề xuất nhóm, ở cả phía giảng viên và sinh viên.
  - Tìm bạn để mời theo MSSV.
- [x] 9. Kiểm tra theo Security Baseline và Resiliency Baseline. PBT không áp dụng vì extension này đang tắt.

## Câu hỏi

Phương án đầu tiên của mỗi câu là phương án AI đề xuất.

## Question 1
Database bảo đảm các ràng buộc của MSSV bằng cách nào?

A) Trong cùng một migration, thêm 3 ràng buộc: (đề xuất)
- Unique index trên `student_code`, chỉ áp dụng khi cột có giá trị.
- Check định dạng `^[A-Z]{2}[0-9]{6}$`.
- Check chỉ role `STUDENT` được có mã: `role = 'STUDENT' or student_code is null`.

Mã đã được chuẩn hóa sang chữ in hoa trước khi lưu, nên unique index không cần thêm `upper()`.

B) Chỉ thêm unique index. Định dạng và quy tắc theo role chỉ kiểm ở service.

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
API để Admin gán hoặc sửa MSSV ở trang chi tiết tài khoản?

A) Dùng lại `PATCH` sửa tài khoản hiện có, thêm trường `studentCode`: (đề xuất)
- Không gửi trường này: mã giữ nguyên.
- Gửi đúng mã đang có: không thay đổi gì.
- Gửi mã mới: áp dụng quy tắc SC-03, tức là gán nếu tài khoản chưa có mã, sửa nếu tài khoản còn `PENDING`, còn lại trả `STUDENT_CODE_LOCKED`.
- Gửi chuỗi rỗng khi tài khoản đã có mã: trả `INVALID_STUDENT_CODE`, vì không được xóa trống (SC-02).

Cách này dùng chung `version` và một bản ghi audit `ACCOUNT_UPDATED` với các trường khác.

B) Endpoint riêng `PUT /admin/accounts/{id}/student-code`, có `version` riêng và loại audit riêng `STUDENT_CODE_SET`.

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
Gán mã hàng loạt cho tài khoản `STUDENT` đang `DISABLED`?

A) Vẫn cho gán, nếu tài khoản chưa có mã. Mã là dữ liệu định danh, và tài khoản có thể được mở lại sau này. (đề xuất)

B) Không cho gán. Báo lỗi `ACCOUNT_DISABLED`.

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Màn gán mã hàng loạt nằm ở đâu?

A) Trên trang Danh sách tài khoản, thêm nút "Gán MSSV từ Excel" cạnh nút "Nhập Excel". (đề xuất)
- Panel dùng cùng khuôn với panel nhập tài khoản: chọn tệp, kiểm tra, xem kết quả theo từng dòng, rồi xác nhận.
- Có tệp mẫu tải về gồm 2 cột `email` và `student_code`.

B) Một trang riêng trong menu Quản trị.

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
Ghi danh bằng danh sách dán vào trộn cả email và MSSV: xét trùng và báo lỗi thế nào?

A) Theo các bước sau: (đề xuất)
1. **Xét định dạng**: dòng có `@` được xét là email. Dòng khác, sau khi đổi sang chữ in hoa, khớp `^[A-Z]{2}[0-9]{6}$` thì là MSSV. Không khớp loại nào thì báo `INVALID_IDENTIFIER`.
2. **Xét trùng theo giá trị đã nhập**: cùng email hoặc cùng mã xuất hiện lần nữa thì báo `DUPLICATE_IN_LIST`.
3. **Tra tài khoản**: mã không thuộc tài khoản nào thì báo `ACCOUNT_NOT_FOUND`, giống email không tìm thấy hiện nay.
4. **Trùng người**: một dòng email và một dòng MSSV cùng trỏ tới một sinh viên thì dòng sau nhận kết quả "đã ghi danh", như khi ghi danh lại một người đang học.

Header được chấp nhận ở dòng đầu là `email`, `student_code` hoặc `mssv`, không phân biệt hoa thường. Đổi tên lỗi `INVALID_EMAIL` thành `INVALID_IDENTIFIER`, đồng thời sửa frontend tương ứng.

B) Như A, nhưng giữ tên lỗi `INVALID_EMAIL` cho dòng có `@` và thêm lỗi `INVALID_STUDENT_CODE` cho dòng còn lại.

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
MSSV hiển thị thế nào trên giao diện?

A) Tùy theo loại màn: (đề xuất)
- **Bảng** (danh sách tài khoản, danh sách lớp, bảng điểm, lượt làm quiz): cột "MSSV" riêng, đặt trước cột họ tên.
- **Thẻ và danh sách gọn** (nhóm, đề xuất nhóm, chấm bài, chi tiết bài nộp): dòng phụ dưới tên, dạng `SE123456 · email`.
- **Tài khoản chưa có mã**: không hiện gì ở vị trí mã.
- **Màn hẹp**: cột MSSV gộp vào ô họ tên thành dòng phụ.

B) Luôn ghép vào sau tên dạng `Nguyễn Văn A (SE123456)` ở mọi màn.

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 7
Tìm bạn để mời vào đề xuất nhóm theo MSSV làm thế nào?

A) Dùng lại cách tìm hiện có của danh sách ứng viên mời. (đề xuất)
- Ô tìm khớp theo tên, email hoặc MSSV, không phân biệt hoa thường.
- Phạm vi ứng viên không đổi: chỉ sinh viên `ACTIVE` của lớp chưa có nhóm chính thức.
- Nếu hiện nay danh sách được lọc ở frontend thì thêm MSSV vào dữ liệu trả về. Nếu lọc ở backend thì thêm MSSV vào điều kiện lọc.

B) Thêm một API tìm riêng theo MSSV.

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 8
Thứ tự xét lỗi của một dòng trong luồng gán mã hàng loạt?

A) Mỗi dòng chỉ nhận lỗi đầu tiên gặp phải, theo thứ tự sau: (đề xuất)
1. Thiếu ô: `MISSING_FIELD`.
2. Email sai định dạng: `INVALID_EMAIL`.
3. Mã sai định dạng: `INVALID_STUDENT_CODE`.
4. Email trùng một dòng khác trong tệp: `DUPLICATE_EMAIL_IN_FILE`.
5. Mã trùng một dòng khác trong tệp: `DUPLICATE_STUDENT_CODE_IN_FILE`.
6. Không tìm thấy tài khoản: `ACCOUNT_NOT_FOUND`.
7. Tài khoản không phải STUDENT: `NOT_STUDENT`.
8. Tài khoản đã có đúng mã này: `UNCHANGED`. Đây là kết quả, không phải lỗi.
9. Tài khoản đã có mã khác: `STUDENT_CODE_ALREADY_SET`.
10. Mã đã thuộc tài khoản khác: `STUDENT_CODE_EXISTS`.

Hai dòng trùng nhau trong tệp thì dòng đầu vẫn được xét bình thường, các dòng sau báo trùng, giống luồng nhập tài khoản hiện có.

B) Trả tất cả lỗi của một dòng.

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Kết quả

Artifact đã ghi (2026-11-13):

- **U01**
  - `business-rules.md` mục 11: BR-U01-100…115.
  - `domain-entities.md` mục 15: `studentCode`, ràng buộc ở database, `StudentCodePolicy`, kết quả F16, contract và API.
  - `business-logic-model.md` mục 8: phần bổ sung cho F7, F9, F10, F11, F14, và luồng mới F16.
  - `frontend-components.md` mục 6.
- **U05**: `business-rules.md` mục 6 (BR-U05-56…60).
- **U09**: BR-U09-73…75.
- **U11**: BR-U11-42.
- **U12**: `business-rules.md` mục 9 (BR-U12-97, 98).
- **frontend-components** của U05, U09, U11 và U12: thêm mục trỏ về phần giao diện của từng unit.

Điều chỉnh so với câu hỏi: ở Q5, tài khoản không tìm thấy dùng mã lỗi `NOT_FOUND` của U05 hiện có, không dùng `ACCOUNT_NOT_FOUND`, để bộ kết quả của BR-U05-24 nhất quán.

Thay đổi sau review:

- 2026-11-13T15:48:13Z: người dùng muốn giữ MSSV khi tài khoản rời `STUDENT` nhưng ẩn mã đi.
- 2026-11-13T15:50:59Z: người dùng chốt không có chức năng đổi role vào hoặc ra khỏi `STUDENT`. Vì vậy:
  - BR-U01-107 chặn các lần đổi này bằng lỗi `STUDENT_ROLE_CHANGE_NOT_ALLOWED`.
  - Cơ chế mã ẩn (BR-U01-116) bị bỏ.
  - Check theo role ở database (Q1) được giữ.

## Tuân thủ extension

| Quy tắc | Kết quả | Ghi chú |
|---|---|---|
| SECURITY-03, ghi log | Tuân thủ | Audit và log không chứa giá trị MSSV (BR-U01-115) |
| SECURITY-05, kiểm đầu vào | Tuân thủ | Backend kiểm định dạng, tính duy nhất và role. Database có check và unique index. `StrictRequest` từ chối trường lạ |
| SECURITY-08, phân quyền | Tuân thủ | Chỉ Admin gán hoặc sửa mã. Mã đã khóa sau khi tài khoản kích hoạt (`STUDENT_CODE_LOCKED`). Sinh viên chỉ thấy MSSV trên những màn vốn đã hiện người đó. Không thêm API tra cứu nào |
| SECURITY-11, thiết kế an toàn | Tuân thủ | Định dạng, tính duy nhất và quy tắc theo role được ràng buộc ở cả service lẫn database. Đổi role vào hoặc ra khỏi `STUDENT` bị chặn ở backend, kể cả khi gọi thẳng API. Lỗi `STUDENT_CODE_EXISTS` không cho biết tài khoản nào đang giữ mã |
| SECURITY-15, xử lý lỗi | Tuân thủ | Lỗi trùng do ghi đồng thời được chuyển thành `STUDENT_CODE_EXISTS` hoặc `IMPORT_CONFLICT` và rollback cả transaction |
| Các quy tắc SECURITY còn lại | N/A | Không đổi xác thực, mã hóa, hạ tầng hay dependency |
| RESILIENCY-03, 04, quản lý thay đổi và rollback | Tuân thủ | Migration chỉ thêm cột cho phép trống. Code cũ bỏ qua cột này, nên rollback ứng dụng vẫn an toàn |
| Các quy tắc RESILIENCY còn lại | N/A | Không đổi hạ tầng, sự kiện, job hay phụ thuộc ngoài |
| PBT | N/A | Extension đang tắt |
