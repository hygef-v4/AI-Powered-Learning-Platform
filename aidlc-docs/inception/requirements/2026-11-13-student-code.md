# Mã số sinh viên (MSSV) — yêu cầu

Mở rộng U01 (tài khoản), kèm phần sửa ở U05 (lớp, ghi danh), U09 (chấm điểm, bảng điểm), U11 (lượt làm quiz) và U12 (nhóm). Quyết định: [câu hỏi xác nhận](2026-11-13-student-code-questions.md), người dùng trả lời trong chat ngày 2026-11-13.

## Phân tích yêu cầu

- **Yêu cầu**: "tôi muốn thêm trường mã số sinh viên cho roll student".
- **Loại**: Enhancement. Thêm một thuộc tính mới vào tài khoản, kèm 3 chức năng phụ.
- **Phạm vi**: nhiều thành phần.
  - Backend: `identity` gồm bảng `accounts`, model, policy, service admin, nhập tệp, hồ sơ, repository và contract `AccountContact`/`IAccountLookup`. Các module `academics`, `grading`, `groups`, `quizpractice` thêm MSSV vào DTO hiển thị.
  - Frontend: `features/identity`, `academics`, `grading`, `groups`, `quizpractice`.
- **Độ phức tạp**: vừa. Không có logic nghiệp vụ khó, nhưng chạm nhiều màn hình và có thêm một luồng nhập tệp.
- **Độ sâu yêu cầu**: Standard.
- **Không đổi**: đăng nhập, OTP, kích hoạt và đặt lại mật khẩu vẫn chỉ dùng email. Quyền truy cập, điểm, bài nộp và dữ liệu nhóm giữ nguyên.

## Thuật ngữ

- **MSSV**: mã số sinh viên do trường cấp, gắn với một tài khoản role `STUDENT`.
- **Tài khoản cũ**: tài khoản `STUDENT` đã có trước khi tính năng này được triển khai.

## Yêu cầu chức năng

### SC-01 — Định dạng và tính duy nhất (Q1)

- MSSV gồm đúng 2 chữ cái Latin, theo sau là đúng 6 chữ số, ví dụ `SE123456`. Định dạng cố định trong code, Admin không cấu hình.
- Trước khi kiểm định dạng, hệ thống cắt khoảng trắng ở hai đầu và đổi chữ cái thành in hoa: `se123456` được lưu là `SE123456`. Không chấp nhận khoảng trắng hay ký tự khác ở giữa mã.
- Mỗi MSSV thuộc tối đa một tài khoản, so sánh sau khi đã chuẩn hóa. Database cũng phải bảo đảm điều này.
- Chỉ tài khoản role `STUDENT` được có MSSV.

### SC-02 — Bắt buộc với tài khoản mới, cho phép trống với tài khoản cũ (Q2)

- Tạo tài khoản `STUDENT`, dù Admin tạo từng tài khoản hay nhập từ tệp, thì bắt buộc phải có MSSV hợp lệ.
- Tài khoản cũ được để trống MSSV và vẫn dùng mọi chức năng như hiện nay. Hệ thống không có chức năng gán mã cho các tài khoản này, vì mọi tài khoản sinh viên sẽ có MSSV ngay từ lúc tạo (người dùng chốt 2026-11-14). Database vẫn cho phép cột này trống.
- Tài khoản `STUDENT` đã có MSSV thì không được xóa trống mã.

### SC-03 — MSSV nhập khi tạo; không có chức năng đổi mã (Q3, thu hẹp phạm vi 2026-11-13 và 2026-11-14)

Người dùng bỏ chức năng đổi MSSV vì nằm ngoài phạm vi chính của hệ thống.

- Form tạo tài khoản có ô MSSV. Ô này chỉ hiện và chỉ bắt buộc khi chọn role `STUDENT`.
- Ở trang chi tiết tài khoản, Admin chỉ sửa được MSSV gõ sai khi tài khoản còn ở `PENDING`, tức sinh viên chưa kích hoạt.
- Tài khoản không còn ở `PENDING` thì không đổi hay gán được mã, kể cả tài khoản cũ chưa có mã. Thao tác này bị chặn trên giao diện, và backend trả lỗi `STUDENT_CODE_LOCKED` nếu gọi qua API.
- Hệ thống kiểm tra phiên bản bản ghi (`version`) như các thao tác sửa hiện có.
- Mỗi lần đặt hoặc sửa MSSV đều ghi audit. Audit chỉ ghi thao tác, không ghi giá trị mã.
- Trùng mã với tài khoản khác thì báo lỗi `STUDENT_CODE_EXISTS`. Sai định dạng thì báo lỗi `INVALID_STUDENT_CODE`.

### SC-04 — Không đổi role vào hoặc ra khỏi `STUDENT` (Q4, người dùng quyết định 2026-11-13T15:50:59Z)

- Không có chức năng đổi role từ `STUDENT` sang role khác, và cũng không đổi được từ role khác sang `STUDENT`.
- Backend từ chối mọi request như vậy với lỗi `STUDENT_ROLE_CHANGE_NOT_ALLOWED`, kể cả khi gọi thẳng API.
- Đổi role giữa các role không phải `STUDENT` (`TEACHER`, `SUBJECT_MANAGER`, `ADMIN`) vẫn chạy như hiện nay. Các quy tắc chặn đổi role đang có không thay đổi.
- Vì không có đường nào để một tài khoản rời khỏi hoặc vào `STUDENT`, MSSV luôn thuộc tài khoản `STUDENT`. Không cần xóa hay ẩn mã.
- Muốn sửa một tài khoản tạo nhầm role thì không có thao tác nào trong hệ thống làm được việc này. Email cũng không dùng lại được, vì email là duy nhất và tài khoản không xóa được (BR-U01-73).

### SC-05 — Sinh viên xem MSSV của mình (Q3)

- Trang hồ sơ của sinh viên hiện MSSV ở dạng chỉ đọc. Nếu tài khoản chưa có mã thì hiện "Chưa có MSSV".
- API cập nhật hồ sơ không nhận trường MSSV. Nếu request có trường này thì bị từ chối như mọi trường lạ.

### SC-06 — Nhập tài khoản từ tệp có cột MSSV (Q2)

- Tệp nhập Excel/CSV có thêm cột `student_code`. Cột này bắt buộc với dòng `STUDENT`, phải để trống với các role khác.
- Lỗi theo từng dòng gồm:
  - `MISSING_STUDENT_CODE`: dòng `STUDENT` không có mã.
  - `INVALID_STUDENT_CODE`: mã sai định dạng.
  - `STUDENT_CODE_NOT_ALLOWED`: dòng role khác có điền mã.
  - `DUPLICATE_STUDENT_CODE_IN_FILE`: mã trùng với một dòng khác trong tệp.
  - `STUDENT_CODE_EXISTS`: mã đã thuộc một tài khoản có sẵn.
- Tệp mẫu tải về được cập nhật thêm cột này. Tệp không có cột `student_code` thì mọi dòng `STUDENT` bị lỗi `MISSING_STUDENT_CODE`, còn các dòng role khác vẫn nhập bình thường.

### SC-07 — Gán MSSV hàng loạt cho tài khoản đã có: ĐÃ BỎ (2026-11-14)

Người dùng bỏ chức năng này vì mọi tài khoản sinh viên sẽ có MSSV ngay từ lúc tạo. Không có luồng nhập, API hay màn hình nào để gán mã cho tài khoản cũ.

### SC-08 — Tìm tài khoản theo MSSV (Q5, thu hẹp phạm vi 2026-11-13)

- Ô tìm kiếm ở Danh sách tài khoản tìm được cả theo MSSV, ngoài email và tên như hiện nay.
- Danh sách tài khoản có cột MSSV. Tài khoản chưa có mã thì để trống ô.
- Người dùng đã bỏ bộ lọc "Sinh viên chưa có MSSV".

### SC-09 — Ghi danh lớp bằng MSSV (Q5)

- Ô tìm ứng viên khi ghi danh từng sinh viên tìm được theo MSSV.
- Danh sách dán vào để ghi danh nhiều sinh viên chấp nhận mỗi dòng là email hoặc MSSV. Hệ thống tự nhận biết: dòng có `@` là email, dòng khớp định dạng SC-01 là MSSV, còn lại báo dòng không hợp lệ. Một danh sách được trộn cả hai loại.
- MSSV không tìm thấy tài khoản thì báo lỗi riêng cho dòng đó, giống email không tìm thấy. Các dòng khác vẫn được xử lý vì mỗi dòng chạy trong transaction riêng như hiện nay.
- Kết quả từng dòng hiện đúng giá trị đã nhập (email hoặc MSSV).

### SC-10 — Hiện MSSV ở các màn nhân sự quản lý sinh viên

- MSSV hiện cạnh tên sinh viên ở các màn sau:
  - Danh sách sinh viên của lớp (giảng viên, Chủ nhiệm môn, Admin).
  - Bảng điểm dạng ma trận và trang Chấm điểm.
  - Chi tiết bài nộp.
  - Danh sách nhóm và đề xuất nhóm ở phía giảng viên.
  - Lượt làm quiz ở phía giảng viên.
- Bảng điểm xuất XLSX/CSV có cột "MSSV" ngay sau số thứ tự và trước họ tên. Ô MSSV trong XLSX ở dạng văn bản để không mất ký tự.
- Tìm kiếm trong bảng điểm tìm được theo MSSV.
- Tài khoản chưa có MSSV thì để trống ô, không hiện chữ thay thế trong file xuất.

### SC-11 — Sinh viên thấy MSSV của mình và của sinh viên khác (người dùng quyết định 2026-11-13, thay giả định G1)

- Ở các màn phía sinh viên đang hiện tên hoặc email của sinh viên khác, MSSV hiện cạnh tên. Các màn này gồm:
  - Thành viên lớp.
  - Nhóm của tôi.
  - Đề xuất nhóm: danh sách thành viên, lời mời, và ô tìm bạn để mời.
- Sinh viên chỉ thấy MSSV của những người mà màn đó vốn đã cho thấy. Phạm vi xem không mở rộng thêm.
- Ô tìm bạn để mời vào đề xuất nhóm tìm được theo MSSV.
- Tài khoản chưa có MSSV thì không hiện gì ở vị trí mã.

## Giả định (người dùng xác nhận khi duyệt tài liệu)

- **G1**: thay bằng SC-11.
- **G2**: MSSV không đưa vào prompt AI, thông báo hay email.
- **G3**: Không thêm đăng nhập bằng MSSV.
- **G4**: Không thêm sắp xếp theo MSSV. Thứ tự hiện có của các danh sách giữ nguyên.

## Yêu cầu phi chức năng

- **NFR-SC-01 (Bảo mật, SECURITY-03)**: log và audit không chứa giá trị MSSV.
- **NFR-SC-02 (Bảo mật, SECURITY-05)**: kiểm đầu vào ở backend theo SC-01. Frontend kiểm trước để báo lỗi sớm, nhưng không thay được kiểm tra ở backend.
- **NFR-SC-03 (Bảo mật, SECURITY-08)**: MSSV chỉ được trả cho Admin, nhân sự có quyền với lớp, chính sinh viên đó, và sinh viên khác trên những màn vốn đã hiện người đó (SC-11). Không có API nào cho tra MSSV ngoài phạm vi quyền đang có.
- **NFR-SC-04 (Dữ liệu)**: migration chỉ thêm cột cho phép trống và chỉ mục unique, không đổi dữ liệu có sẵn. Database có ràng buộc: tài khoản không phải `STUDENT` thì không có MSSV. Ràng buộc này luôn đúng vì không đổi được role vào hoặc ra khỏi `STUDENT` (SC-04).
- **NFR-SC-05 (Hiệu năng)**: tìm theo MSSV dùng chỉ mục. Lấy thông tin liên hệ theo lô (`getContacts`) vẫn chạy bằng một truy vấn.
- **NFR-SC-06 (Tương thích)**: `AccountContact` và các DTO `Person` chỉ thêm trường `studentCode` có thể rỗng, không bỏ hay đổi tên trường đang có.
- **NFR-SC-07 (Kiểm thử)**: unit test cho policy, nhập tệp, khóa đổi mã (`STUDENT_CODE_LOCKED`), đổi role và ghi danh theo MSSV. Có test với PostgreSQL cho chỉ mục unique và ràng buộc role. Có test frontend cho form, bảng nhập và cột bảng điểm. Toàn bộ `mvn verify` và Vitest phải qua.
