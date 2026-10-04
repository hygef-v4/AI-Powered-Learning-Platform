# Chuẩn bị vẽ lại Use Case Diagram

Nguồn chính: `uc-wave-map.md` (bản rà soát đồng bộ 2026-10-03/04, nhóm gửi riêng, đường dẫn dự kiến `aidlc-docs/construction/uc-wave-map.md`, chưa có trong repo). Bản này khớp với `aidlc-docs/aidlc-state.md` và `unit-of-work-story-map.md` trên `main` (`a9d1552`). Tên UC giữ nguyên tiếng Anh để sơ đồ và SRS khớp nhau.

> [use-case-table.md](use-case-table.md) trong repo vẫn là bản 2026-10-01 và lệch 9 UC so với nguồn này (mục 1). Cần cập nhật bảng 4.2 trước hoặc cùng lúc với sơ đồ.

## 1. Khác biệt so với `docs/use-case-table.md` hiện tại

| ID | Bảng trong repo (01/10) | Wave map (03–04/10) |
|---|---|---|
| 12 | Access Lesson | **View Learning Material**: xem học liệu theo module, tải tệp |
| 14 | Discuss in Class Q&A | **Comment on Announcement**: chỉ bình luận dưới thông báo, không có Q&A riêng |
| 18 | View Learning Overview (Student) | **View Statistics (Administrator)**: số liệu trên Admin Menu; sinh viên không có dashboard cá nhân |
| 22 | Manage AI Service gồm gói credit | Gói credit và mức tặng là cấu hình cố định, không quản lý ở đây |
| 27 | Manage Group Assignment (Teacher) | Thêm **Subject Manager** (qua template) |
| 35 | View Grades (Student) | Thêm **Teacher, Subject Manager** (Gradebook) |
| 36 | Monitor Submissions (T, SM, **Admin**) | Bỏ Administrator |
| 37 | Buy AI Credits | Cả 4 vai trò được tặng credit hằng tháng; sinh viên chỉ dùng credit cho UC 40 |
| 40 | Grade Practice with AI (Student) | **Grade with AI (Student, Teacher)**: sinh viên chấm Practice, giảng viên xin đề xuất cho bài GRADED |

Các thay đổi số UC từ bản 2026-09-30 vẫn giữ nguyên:

| Số cũ | Số mới |
|---|---|
| UC 21 Create Assignment with AI | Gộp vào UC 28 |
| UC 22 Create Template with AI | UC 21 Manage Templates |
| UC 23–27 | UC 22–26 |
| — | UC 27 là UC mới |

## 2. Actor

| Actor | Loại | Ghi chú |
|---|---|---|
| User (Người dùng) | Primary, trừu tượng | Cha của 4 vai trò; giữ 8 UC chung |
| Student | Primary | Kế thừa User |
| Teacher | Primary | Kế thừa User |
| Subject Manager | Primary | Kế thừa User; đăng nhập vào Teacher Menu |
| Administrator | Primary | Kế thừa User |
| AI Service (Gemini) | Secondary | UC 11, 17, 21, 25, 27, 28, 33, 40 |
| YouTube | Secondary | UC 11 |
| Google Drive | Secondary | UC 11 (spec); cũng lưu tệp cho UC 6, 12, 16, 25, 30 |
| Code Sandbox (Judge0) | Secondary | UC 26, 30 |
| PayOS | Secondary | UC 37 |
| Email Service (SMTP) | Secondary | UC 1, 4 (OTP), UC 38 (4 loại email) |

## 3. UC theo từng actor

### User: 8 UC chung, kế thừa xuống cả 4 vai trò

| ID | Use Case | Secondary |
|---|---|---|
| 1 | Activate Account | Email Service |
| 2 | Sign In | |
| 3 | Sign Out | |
| 4 | Recover Password | Email Service |
| 5 | Change Password | |
| 6 | Manage Profile | |
| 37 | Buy AI Credits | PayOS |
| 38 | View Notifications | Email Service |

### Student: 11 UC

| ID | Use Case | Secondary |
|---|---|---|
| 10 | Join Class | |
| 12 | View Learning Material | |
| 14 | Comment on Announcement | |
| 15 | Request Leader Change | |
| 16 | Submit Group Document | |
| 19 | Access Enrolled Class | |
| 29 | View Assigned Work | |
| 30 | Submit Assignment | Code Sandbox |
| 31 | Review Attempts | |
| 35 | View Grades | |
| 40 | Grade with AI | AI Service |

### Teacher: 18 UC

| ID | Use Case | Secondary |
|---|---|---|
| 9 | Manage Classes | |
| 11 | Manage Content | YouTube, Google Drive, AI Service |
| 13 | Post Class Announcement | |
| 14 | Comment on Announcement | |
| 17 | Grade Group Document | |
| 20 | Manage Rubrics | |
| 23 | Manage Text Essay | |
| 24 | Manage Quiz | |
| 25 | Manage Diagram Essay | AI Service |
| 26 | Manage Code Lab | Code Sandbox |
| 27 | Manage Group Assignment | AI Service |
| 28 | Manage Assignments | AI Service |
| 32 | Review Submissions | |
| 33 | Grade Submissions | |
| 34 | Finalize Grades | |
| 35 | View Grades | |
| 36 | Monitor Submissions | |
| 40 | Grade with AI | AI Service |

### Subject Manager: 12 UC

| ID | Use Case | Secondary |
|---|---|---|
| 9 | Manage Classes | |
| 11 | Manage Content | YouTube, Google Drive, AI Service |
| 20 | Manage Rubrics | |
| 21 | Manage Templates | AI Service |
| 23 | Manage Text Essay | |
| 24 | Manage Quiz | |
| 25 | Manage Diagram Essay | AI Service |
| 26 | Manage Code Lab | Code Sandbox |
| 27 | Manage Group Assignment | AI Service |
| 28 | Manage Assignments | |
| 35 | View Grades | |
| 36 | Monitor Submissions | |

### Administrator: 6 UC

| ID | Use Case | Secondary |
|---|---|---|
| 7 | Manage Accounts | |
| 8 | Manage Subjects | |
| 9 | Manage Classes | |
| 18 | View Statistics | |
| 22 | Manage AI Service | |
| 39 | View Audit Log | |

Kiểm tra độ phủ: 40 UC, không UC nào bị sót.

| Nhóm | Số UC | UC |
|---|---|---|
| Chung (User) | 8 | 1–6, 37, 38 |
| Chỉ Student | 8 | 10, 12, 15, 16, 19, 29, 30, 31 |
| Chỉ Teacher | 5 | 13, 17, 32, 33, 34 |
| Chỉ Subject Manager | 1 | 21 |
| Chỉ Administrator | 5 | 7, 8, 18, 22, 39 |
| Dùng chung nhiều actor | 13 | 9, 11, 14, 20, 23–28, 35, 36, 40 (mục 4) |

## 4. UC dùng chung nhiều actor

| ID | Use Case | Actor |
|---|---|---|
| 9 | Manage Classes | Administrator, Teacher, Subject Manager |
| 11 | Manage Content | Subject Manager, Teacher |
| 14 | Comment on Announcement | Student, Teacher |
| 20 | Manage Rubrics | Teacher, Subject Manager |
| 23–28 | Manage Text Essay / Quiz / Diagram Essay / Code Lab / Group Assignment / Assignments | Teacher, Subject Manager |
| 35 | View Grades | Student, Teacher, Subject Manager |
| 36 | Monitor Submissions | Teacher, Subject Manager |
| 40 | Grade with AI | Student, Teacher |

## 5. Quan hệ giữa các UC

### Đã ghi rõ trong wave map (vẽ được ngay)

| Quan hệ | Ký hiệu | Căn cứ |
|---|---|---|
| UC 23, 24, 25, 26, 27 `«extend»` UC 28 Manage Assignments | Nét đứt, đầu mũi tên mở, nhãn «extend», trỏ từ UC con về UC 28 | Wave map ghi "UC 23–27 kế thừa UC 28 và thêm phần soạn riêng của từng dạng bài"; nhóm chọn vẽ thành «extend» (2026-10-04) |
| UC 16 Submit Group Document `«extend»` UC 30 Submit Assignment | Như trên | Wave map ghi "UC 16 kế thừa UC 30"; vẽ thành «extend» |

### Không vẽ

- Không thêm include/extend nào ngoài 6 quan hệ ở bảng trên (chốt 2026-10-04).
- Không vẽ `«include» Sign In` vào từng UC, vì đăng nhập là tiền điều kiện.

## 6. Bản vẽ

[use-case-diagrams-en.drawio](../aidlc-docs/inception/user-stories/use-case-diagrams-en.drawio) — cùng chỗ và cùng cách làm với bộ ngày 2026-09-15 (tên trang `D1 - User` … `D5 - Administrator`, ellipse co theo tên, khổ A4/A3). Kiểu vẽ chốt ngày 2026-10-04:

- Đen trắng; ellipse chỉ ghi tên UC.
- Actor đặt bên trái, đường nối toả hình quạt.
- User là actor trừu tượng, tên in nghiêng.
- Hệ thống ngoài đặt bên phải.

File có 5 trang:

| Trang | UC trên trang |
|---|---|
| User | 8 UC chung (1–6, 37, 38) |
| Student | 11 |
| Teacher | 18 |
| Subject Manager | 12 |
| Administrator | 6 |

Cộng lại 8 + 11 + 18 + 12 + 6, bỏ các UC trùng giữa các trang, được đúng 40 UC.

- **Trang vai trò:** có actor User ở trên, nối xuống vai trò bằng mũi tên kế thừa, giống bộ cũ.
- **«extend»:** UC 23–27 nằm bên phải UC 28, UC 16 nằm bên phải UC 30. Các UC này nối về UC cha bằng mũi tên «extend» nét đứt; nhãn đặt gần UC con cho dễ đọc. Actor chỉ nối với UC gốc, vì UC mở rộng chạy bên trong luồng của UC gốc.
- **Kiểm tra tự động:** không có đường nối nào cắt qua UC hay actor khác.

## 7. Điểm cần nhóm xác nhận

- **Subject Manager không kế thừa Teacher.** Sơ đồ theo đúng cột Actor của file. UC 17 và UC 32–34 chỉ ghi Teacher.
- **Bảng 4.2**: cập nhật `docs/use-case-table.md` theo mục 1 để SRS khớp sơ đồ.
- **Note họp**: note chỉ có 2 vai trò và giữ Simulation, docs có 4 vai trò và Code Lab; sơ đồ đang theo docs.

## 8. Sửa từ bộ UCD cũ sang bộ mới

Bộ cũ dùng các UC chi tiết (View/Update/extend). Bộ mới gộp lại theo đúng 40 UC của wave map. Bộ mới chỉ còn 6 quan hệ «extend»: UC 23–27 → UC 28 và UC 16 → UC 30.

### User

| UC cũ | UC mới |
|---|---|
| Sign In; Recover Password «extend» Sign In | UC 2 Sign In; UC 4 Recover Password (tách riêng, bỏ extend) |
| Sign Out, Activate Account, Change Password | UC 3, UC 1, UC 5 (giữ) |
| View Profile; Update Profile «extend» | UC 6 Manage Profile |
| Receive and View Notifications | UC 38 View Notifications |
| Buy AI Credits (trước nằm ở từng vai trò) | UC 37, chuyển lên User |
| Notification Service | Email Service; thêm PayOS, Google Drive |

### Learner → Student

| UC cũ | UC mới |
|---|---|
| View Learning Overview, View Personal Result Dashboard | **Bỏ**: không có dashboard sinh viên; UC 18 giờ là View Statistics của Admin |
| Configure and Take Simulation Exam | **Bỏ**: Simulation nằm ngoài MVP |
| Access Enrolled Class, View Group Information | UC 19 Access Enrolled Class (gồm My Group) |
| Access Lesson | UC 12 View Learning Material |
| Request Leader Change «extend» | UC 15 Request Leader Change (bỏ extend) |
| View and Submit Group Document, Claim and Complete Group Document Section | UC 16 Submit Group Document «extend» UC 30 |
| Self-Enroll with Invite Code | UC 10 Join Class |
| View Assigned Work | UC 29 |
| View History and Resubmit «extend» | UC 31 Review Attempts |
| Complete and Submit Quiz / Essay / DOCUMENT Assignment / Code Lab | UC 30 Submit Assignment |
| View Grades and Feedback | UC 35 View Grades |
| Discuss in Class Q&A | UC 14 Comment on Announcement |
| — | **Thêm** UC 40 Grade with AI (AI Service) |

### Instructor → Teacher

| UC cũ | UC mới |
|---|---|
| View Classes, Update Class, Manage Class Lifecycle, View Class Roster, Enroll Learner, Remove Learner from Class, Manage Groups and Leaders, Review Leader Change Request, View Group Information | UC 9 Manage Classes |
| Manage Class Content, Publish Class Content, Use YouTube as a Lesson RAG Source | UC 11 Manage Content (không còn bước publish) |
| Post Class Announcement | UC 13 |
| Discuss in Class Q&A | UC 14 Comment on Announcement |
| Manage Rubric Bank | UC 20 Manage Rubrics |
| Manage Question Bank | **Bỏ** UC riêng: ngân hàng câu hỏi nằm trong UC 23–27 |
| Author Essay Assignment | UC 23 Manage Text Essay |
| Author Multiple-Choice Quiz | UC 24 Manage Quiz |
| Author DOCUMENT Assignment with Draw.io | UC 25 Manage Diagram Essay |
| Author and Test Code Lab | UC 26 Manage Code Lab |
| Author Group Assignment, Define Group Document Sections | UC 27 Manage Group Assignment |
| View Managed Assignments, Manage Assignment Lifecycle, Approve and Publish Class Assignment, Copy Assignment and Rubric Across Classes, Generate and Review Class Assignment Draft with AI | UC 28 Manage Assignments |
| Publish and Copy Subject Assignment Template | Copy template nằm trong UC 28; publish template thuộc UC 21 (Subject Manager) |
| Configure and Take Simulation Exam | **Bỏ** |
| Review Submissions | UC 32 |
| Grade Manually, Grade with AI Assistance | UC 33 Grade Submissions; popup AI là UC 40 Grade with AI |
| Review and Grade Group Document | UC 17 Grade Group Document |
| Finalize and Publish Grades, Bulk Finalize Grades | UC 34 Finalize Grades |
| View Gradebook and Grade History; Export Gradebook «extend» | UC 35 View Grades (Gradebook) và UC 36 Monitor Submissions (lịch sử, xuất file) |
| Monitor Submission Status | UC 36 |
| Buy AI Credits | Chuyển lên User |
| Payment Gateway | PayOS |

### Subject Manager

| UC cũ | UC mới |
|---|---|
| Kế thừa Instructor | **Bỏ kế thừa**: nối thẳng UC 9, 20, 23–28, 35, 36 theo cột Actor |
| Manage Subject Materials and RAG, Use YouTube as a Lesson RAG Source | UC 11 Manage Content |
| Generate and Review Subject Template or Question Draft with AI, Publish and Copy Subject Assignment Template | UC 21 Manage Templates |
| Buy AI Credits | Chuyển lên User |

### Administrator

| UC cũ | UC mới |
|---|---|
| View User Accounts, Update Account and Role, Manage Account Status, Create Account Manually, Import Accounts in Bulk | UC 7 Manage Accounts |
| View Subjects, Create Subject, Update Subject, Assign Subject Manager | UC 8 Manage Subjects |
| View Classes, Create Class, Update Class, Assign Primary Instructor, Manage Class Lifecycle, View Class Roster, Enroll Learner, Remove Learner from Class | UC 9 Manage Classes |
| Manage and Monitor AI Service | UC 22 Manage AI Service |
| View Audit Log | UC 39 |
| View Gradebook and Grade History, Export Gradebook | **Bỏ**: Admin không xem sổ điểm (UC 35, 36 không có Admin) |
| — | **Thêm** UC 18 View Statistics |
| Buy AI Credits, Notification Service, Payment Gateway | Chuyển lên trang User |
