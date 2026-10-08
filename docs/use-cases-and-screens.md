# Use Cases and Screens

Đối chiếu với bảng 4.2 gồm **70 UC** và Permission Matrix 4.4 trong [SRS](https://docs.google.com/document/d/1ebPCxJyusasIm8aiMmm3pWiIAaEr7D7n/edit). ID và tên UC giữ nguyên theo bảng mới; UC 70 View Audit Log được bổ sung theo yêu cầu.

- `User` gồm Student, Teacher, Subject Manager và Administrator, sử dụng các chức năng chung của tài khoản.
- Chức năng của Student chỉ áp dụng cho lớp đã ghi danh và dữ liệu được phép xem. Subject Manager và Administrator thực hiện chức năng Teacher khi được giao dạy lớp tương ứng; chức năng học liệu, template và ngân hàng câu hỏi cấp môn yêu cầu được giao quản lý môn tương ứng, theo mục 4.4.
- Cột **Màn** dùng đúng nhãn trong [Screen Flow](screen-flow%20%281%29.drawio), đối chiếu với file `screen-flow (1).drawio` mới được cung cấp ngày 2026-10-08. Phần trong ngoặc mô tả vai trò hoặc thao tác trên màn, không phải tên màn riêng.
- UC 17 dùng năm màn danh sách bài theo loại bài của Student. UC 24 có màn `Submission History` riêng. UC 53–54 dùng Subject Template và Template Editor trong Drawio mới. Đổi mật khẩu, notifications, rubric và Gradebook/export được ghi là thao tác trên màn chứa chúng; Drawio chưa thể hiện riêng các thao tác này.

| ID | Use Case | Actor | Màn |
|---|---|---|---|
| **Tài khoản, credit và thông báo** | | | |
| 01 | Activate Account | User | Account Activation |
| 02 | Login | User | Login |
| 03 | Logout | User | Navigation Bar (nút Logout) |
| 04 | Forgot Password | User | Password Recovery, Password Reset |
| 05 | Change Password | User | Profile (đổi mật khẩu) |
| 06 | View Profile Information | User | Profile |
| 07 | Update Profile Information | User | Profile |
| 08 | View Credit Package | User | Credit Packages |
| 09 | Purchase Credit Package | User | Credit Packages (PayOS checkout) |
| 10 | View Payment Result | User | Payment Result |
| 11 | View Notifications | User | Navigation Bar (notifications) |
| **Student: lớp học và bài tập** | | | |
| 12 | View Enrolled Classes | Student | My Classes (Student) |
| 13 | View Enrolled Class Detail | Student | Class Detail (Student) |
| 14 | View Learning Material | User (theo quyền học liệu) | Learning Material |
| 15 | View My Group | Student | My Group |
| 16 | Request Leader Change | Student | My Group (yêu cầu đổi trưởng nhóm) |
| 17 | View Student Assignment List | Student | Quiz Assignments, Codelab Assignments, Text Essay Assignments, Diagram Essay Assignments, Group Essay Assignments |
| 18 | View Assignment Detail | Student | Assignment Detail |
| 19 | Complete Essay Assignment | Student | Text Essay Workspace |
| 20 | Complete Quiz Assignment | Student | Quiz Workspace |
| 21 | Complete Code Lab | Student | Codelab Workspace |
| 22 | Complete Diagram Assignment | Student | Diagram Essay Workspace |
| 23 | Complete Group Assignment | Student | Group Essay Workspace |
| 24 | View Submission History | Student | Submission History |
| 25 | Grade Practice Assignment | Student | Assignment Detail (yêu cầu chấm Practice bằng AI) |
| 26 | View Class Announcements | Student; Teacher; Subject Manager; Administrator | Class Announcements |
| **Teacher: lớp học, bài tập và chấm điểm** | | | |
| 27 | View Assigned Class List | Teacher; Subject Manager; Administrator | Assigned Classes (Teacher) |
| 28 | View Assigned Class Detail | Teacher; Subject Manager; Administrator | Class Detail (Teacher) |
| 29 | View Uploaded Learning Materials | Teacher; Subject Manager; Administrator | Uploaded Learning Materials |
| 30 | Add/Update/Delete Learning Material | Teacher; Subject Manager; Administrator | Uploaded Learning Materials (thêm/sửa/xóa) |
| 31 | Create/Update/Delete Announcement | Teacher; Subject Manager; Administrator | Class Announcements (tạo/sửa/xóa) |
| 32 | View Class Question Bank | Teacher; Subject Manager; Administrator | Class Question Bank |
| 33 | Create/Update/Delete Class Question | Teacher; Subject Manager; Administrator | Class Question Bank, Question Editor (phạm vi lớp) |
| 34 | View Student Submissions | Teacher; Subject Manager; Administrator | Student Submissions |
| 35 | Grade Submission With AI | Teacher; Subject Manager; Administrator | Grading Workspace (AI Grading Proposals) |
| 36 | Grade Submissions Manually | Teacher; Subject Manager; Administrator | Grading Workspace |
| 37 | View/Export GradeBook | Teacher; Subject Manager; Administrator | Class Detail (Teacher, Gradebook/export) |
| 38 | View Teacher Assignment List | Teacher; Subject Manager; Administrator | Assignment List |
| 39 | Create/Update/Delete Essay | Teacher; Subject Manager; Administrator | Assignment List, Text Essay Editor |
| 40 | Create/Update/Delete Quiz | Teacher; Subject Manager; Administrator | Assignment List, Quiz Editor |
| 41 | Create/Update/Delete Code Lab | Teacher; Subject Manager; Administrator | Assignment List, Codelab Editor |
| 42 | Create/Update/Delete Diagram Assignment | Teacher; Subject Manager; Administrator | Assignment List, Diagram Essay Editor |
| 43 | Create/Update/Delete Group Assignment | Teacher; Subject Manager; Administrator | Assignment List, Group Essay Editor |
| 44 | Add/Update Rubric | Teacher; Subject Manager; Administrator | Text Essay Editor, Diagram Essay Editor, Group Essay Editor (rubric) |
| **Subject Manager: môn học và lớp** | | | |
| 45 | View Managed Subject Classes | Subject Manager; Administrator | Subject Classes (Subject Manager/Administrator) |
| 46 | View Managed Class Detail | Subject Manager; Administrator | Class Detail (Subject Manager/Administrator) |
| 47 | Create Class | Subject Manager; Administrator | Subject Classes (tạo lớp) |
| 48 | Assign Teacher To Class | Subject Manager; Administrator | Class Detail (gán giảng viên) |
| 49 | Edit Class Information | Subject Manager; Administrator | Class Detail (sửa thông tin lớp) |
| 50 | View Managed Subject | Subject Manager; Administrator | Subject Detail (Subject Manager); Subject List, Subject Detail (Administrator) |
| 51 | View Subject Materials | Subject Manager; Administrator | Subject Detail (học liệu môn), Learning Material |
| 52 | Add/Update/Delete Subject Material | Subject Manager; Administrator | Subject Detail (thêm/sửa/xóa học liệu môn) |
| 53 | View Subject Templates | Subject Manager; Administrator | Subject Template |
| 54 | Create/Update/Delete Template | Subject Manager; Administrator | Subject Template, Template Editor (tạo/sửa/xóa) |
| 55 | View Subject Question Bank | Subject Manager; Administrator | Subject Question Bank |
| 56 | Create/Update/Delete Subject Question | Subject Manager; Administrator | Subject Question Bank, Question Editor (phạm vi môn) |
| **Administrator: quản trị hệ thống** | | | |
| 57 | View Statistic | Administrator | Statistic |
| 58 | View Account List | Administrator | Account List |
| 59 | Add Account | Administrator | Account List (thêm tài khoản) |
| 60 | View Account Detail | Administrator | Account Detail |
| 61 | Update Account Information | Administrator | Account Detail (sửa thông tin) |
| 62 | Change Account Status | Administrator | Account Detail (đổi trạng thái) |
| 63 | View Subject List | Administrator | Subject List |
| 64 | Add Subject | Administrator | Subject List (thêm môn) |
| 65 | View Subject Detail | Administrator | Subject Detail |
| 66 | Update Subject Information | Administrator | Subject Detail (sửa thông tin) |
| 67 | View Credit Package Setting | Administrator | Credit Package Setting |
| 68 | Add/Edit Credit Package | Administrator | Credit Package Setting (thêm/sửa gói) |
| 69 | View Payment History | Administrator | Payment History |
| 70 | View Audit Log | Administrator | Audit Log |
