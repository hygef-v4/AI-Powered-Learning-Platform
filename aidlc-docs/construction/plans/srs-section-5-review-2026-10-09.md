# Review SRS — 5. System Functionalities

Nguồn: [SRS](https://docs.google.com/document/d/1ebPCxJyusasIm8aiMmm3pWiIAaEr7D7n/edit#heading=h.91vaopdapgz), file `Report-3_Software Requirement Specification.docx`, modified `2026-10-09T07:32:04.569Z`; đọc lại qua Drive. Diagram đối chiếu là ảnh người dùng gửi; baseline AI-DLC là commit `a4fce63`. Chỉ review và chuẩn bị nội dung thay thế; chưa ghi lên SRS online.

## Kết luận

5.1.a đã dùng diagram mới, nhưng 5.1.b vẫn là bảng cũ **55 dòng**. Phần III đã có bộ **51 màn hình/popup**, với tên gần khớp diagram hơn, nhưng một số mô tả nghiệp vụ trong III cũng còn cũ. Không nên chỉ đổi tên trong bảng 55 dòng: cần xây lại danh mục và đánh số theo bộ mới, rồi sửa nội dung/entry/role tương ứng.

Navigation Bar/sidebar là component chung, tab nằm trong Class Detail, không đưa thành màn độc lập. New Account, Password Change, Rubric Detail, Credit Package Detail và Student Detail được giữ như popup vì có node trong diagram. Không tự bổ sung màn mới cho nút tóm tắt; nút nằm trong Learning Material/Material Detail.

## Các điểm phải sửa trong 5.1.b

| Vị trí bảng cũ | Vấn đề | Hướng sửa |
|---|---|---|
| #1 Login; #9–10 Admin Sidebar/Statistic; #20–22 Teacher Sidebar/Assigned Classes/Subject Classes; #39–40 Student Sidebar/My Classes | Tên và post-login còn thuộc flow cũ; thiếu Manager Dashboard | User Login → Admin Dashboard / Manager Dashboard / Class Dashboard theo role. Sidebar là component; Class Dashboard có view Student/Teacher, Subject Manager có link giảng dạy khi được giao lớp. |
| #2–6 | Account Activation/Profile không khớp nhãn; Password Recovery và Password Reset đang là hai dòng; thiếu Password Change | Activate Account; Password Reset một flow nhiều bước; User Profile; Password Change popup riêng. Navigation Bar không đánh số màn. |
| #7–8, #17, #54–55 | Credit Packages gộp nhiều màn; Payment Result và AI Usage là dòng riêng; Settings bị gọi supporting screen | Tách My Credit Package, Public Credit Packages, Credit Package Checkout; kết quả payment trong Checkout. Credit Package List/Detail và Setting List/Detail là màn quản trị rõ ràng. AI usage là phần Admin Dashboard, không màn độc lập. |
| #14–16, #21–22, #25–26 | Vẫn cấp Administrator quyền R2/giảng dạy/tạo lớp, gộp quyền cấu trúc và dạy học | Admin chỉ account/subject/settings/package/payment/audit/dashboard. Class List/Class Detail cấu trúc do Subject Manager; Teacher Class Detail và Student Class Detail tách theo quyền. Admin không tài nguyên học thuật. |
| #23–24 Subject Template/Template Editor | Chức năng template đã bỏ | Xóa hai dòng; Assignment List/Form của Subject Manager phục vụ bài của môn, không phải template. |
| #25–27 | Class Question Bank vẫn tồn tại; Question Editor tên cũ | Chỉ Question List/Question Detail của ngân hàng môn; Teacher dùng câu ACTIVE qua selector soạn bài, không quản trị bank. |
| #28–29 | Thiếu Material List/Material Detail; thiếu nút tóm tắt theo quyết định mới | Material List cho môn, tab Materials cho lớp, Material Detail xem/quản lý. Upload không AI/credit; người có quyền xem bấm Tóm tắt tài liệu; summary dùng chung, requester chịu credit. |
| #30 Class Announcements | Còn comment/hide-comment | Bỏ toàn bộ comment. Teacher tạo/sửa/xóa mềm; chỉ create gửi notification. |
| #31–36 | Nhiều editor riêng thay Assignment Form; Assignment List chỉ mô tả bài lớp | Quiz List/Detail riêng; Assignment Form dùng chung bốn dạng bài, Rubric Detail popup. Subject Manager vào Assignment List cho bài của môn; bài lớp mở từ Evals của Teacher Class Detail. |
| #37 Student Submissions | Nhãn khác node Submission Detail; thiếu Student Detail | Dùng Submission Detail cho danh sách/review bài nộp và entry Grading Workspace; Student Detail popup từ tab Students. UC 37 vẫn tên View Student Submissions, không cần đổi UC theo tên màn. |
| #41 My Group | Node riêng không còn trong diagram | Nội dung nhóm nằm trong Student Class Detail; Group Essay Workspace là màn làm bài nhóm. |
| #42–46 | Năm danh sách bài riêng, quiz có deadline và mở Assignment Detail | Một Student Assignments cho bốn dạng non-quiz; Quiz Practice History/Detail/Taking/Result riêng, gắn học liệu, không lịch mở/đóng hoặc Gradebook. |
| #49 Quiz Workspace | Tên cũ và thiếu History/Detail/Result | Quiz Taking; bổ sung ba màn Quiz Practice History, Quiz Practice Detail, Quiz Result. |

## Bảng 5.1.b đề xuất thay thế

Giữ các cột gốc `# / Screen Name / Feature / Description`; description tiếng Anh có thể chép vào SRS. Thứ tự và số theo Screen #1–#51 của phần III hiện tại; cần cập nhật cross-reference đang dùng số cũ. Bảng này giữ thông tin của diagram và bổ sung quyết định AI-DLC hiện hành, không xác nhận mock-up đã có đủ nút/field.

| # | Screen Name | Feature | Description |
|---|---|---|---|
| 1 | User Login | Authentication | Signs in an ACTIVE account with school email/password; routes Administrator to Admin Dashboard, Subject Manager to Manager Dashboard, and Teacher/Student to Class Dashboard. |
| 2 | Activate Account | Authentication | Requests and verifies an emailed OTP for a PENDING account, sets the first password and signs in automatically. |
| 3 | Password Reset | Authentication | Multi-step recovery using a neutral email response, OTP and one-use reset ticket; revokes sessions and returns to User Login. |
| 4 | User Profile | Profile Management | Views own account and edits display name/phone; email and role remain read-only; opens Password Change. No avatar upload. |
| 5 | Password Change | Authentication | Profile popup verifying the current password and setting a new one; keeps the current session and revokes other sessions. |
| 6 | Admin Dashboard | Administration | Administrator landing screen with current account/subject/class/enrollment counts and AI usage metrics; opens administration destinations. No academic access. |
| 7 | Setting List | System Settings | Administrator views registered settings in File, Credit and AI groups and opens Setting Detail. |
| 8 | Setting Detail | System Settings | Administrator edits allowed values with version checks and audit: file size/formats, monthly free credits, AI models/enabled flags, requests per minute and daily cost cap. |
| 9 | Account List | Account Management | Administrator filters/pages accounts, opens New Account/Account Detail and imports CSV with per-row results; no hard deletion. |
| 10 | New Account | Account Management | Popup creating a school-issued PENDING account without a password; the account holder activates it separately. |
| 11 | Account Detail | Account Management | Administrator edits permitted information, role and status with audit/version and last-Admin safeguards; email is immutable. |
| 12 | Payment History | Payment Administration | Administrator queries platform purchase history with filters and pagination; read-only, without manual reconciliation or provider secrets. |
| 13 | Subject List | Subject Management | Administrator lists, searches and creates subjects; selecting a subject opens Subject Detail. |
| 14 | Subject Detail | Subject Management | Administrator manages subject information and manager assignment; assigned Subject Manager views the managed subject. Structural administration does not grant material/teaching access. |
| 15 | Credit Package List | Payment Administration | Administrator lists credit packages and opens Credit Package Detail to add/edit; hiding uses on-sale status, not deletion. |
| 16 | Credit Package Detail | Payment Administration | Popup adding/editing package information, price and credits with validation/version/audit; preserves existing payment snapshots. |
| 17 | Audit Log | Audit | Administrator searches append-only events and opens read-only details; records cannot be edited/deleted. |
| 18 | Manager Dashboard | Subject Management | Subject Manager landing screen showing assigned subjects and entry points to Subject Detail, Question/Quiz/Material/Assignment/Class Lists; assigned teaching access is scoped separately. |
| 19 | Question List | Subject Question Bank | Assigned Subject Manager lists/filters subject questions of supported types and manages their lifecycle; Teacher selects ACTIVE bank questions through authoring selectors. No class bank. |
| 20 | Question Detail | Subject Question Bank | Authors and validates a subject question version by type; unused drafts may be deleted, referenced/active versions retain history. |
| 21 | Quiz List | Quiz Authoring | Subject Manager lists Practice quizzes linked to subject materials and opens Quiz Detail; no Graded mode or opening/closing schedule. |
| 22 | Quiz Detail | Quiz Authoring | Authors lesson-linked Practice quiz questions, answers, scoring, time limit, attempts and result visibility; opens from Quiz List or Teacher Class Detail Evals. |
| 23 | Material List | Learning Content | Subject Manager lists shared subject modules/materials, manages modules and adds file/video materials; upload only extracts text/captions without AI credit. |
| 24 | Material Detail | Learning Content | Scoped Teacher/Subject Manager previews/downloads a material; authorized manager renames/orders/archives it without replacing source. Viewer may request AI summary; requester pays once and others reuse the result. |
| 25 | Assignment List | Subject Assignments | Subject Manager lists/authors subject Text Essay, Diagram Essay and Code Lab assignments shared across all OPEN subject classes with one schedule; no Group or Quiz assignments in this list. |
| 26 | Assignment Form | Assignment Authoring | Shared type-dependent form for Essay, Code Lab, Diagram and class-only Graded Group assignments; authors content, required rubrics, schedules/attempts, preview/review and publication under subject/class scope. |
| 27 | Rubric Detail | Rubric Authoring | Popup editing the auto-created rubric for an Essay question or Diagram/Group leaf part while assignment is DRAFT; score equals criterion totals, no rubric versions, locked on publication. |
| 28 | Class List | Class Management | Assigned Subject Manager lists/filters and creates classes of a managed ACTIVE subject; opens structural Class Detail. |
| 29 | Class Detail | Class Management | Subject Manager edits class structure/lifecycle/distribution settings, assigns teachers and manages enrollment; structural access alone does not grant grading resources. |
| 30 | Class Dashboard | Class Access | Student/Teacher landing screen listing enrolled/assigned classes; opens the corresponding Student/Teacher Class Detail and permitted shared destinations. |
| 31 | Student Class Detail | Learning Access | Student views enrolled-class materials, eligible class/subject assignments and group/leader information; opens Learning Material/workspaces and requests leader changes. |
| 32 | Learning Material | Learning Content | Student previews/downloads an authorized material, starts linked Practice quizzes and requests AI summary with own credit; shows shared summary/status without charging again for an existing result. |
| 33 | Class Announcements | Class Communication | Student and assigned Teacher read announcements of eligible classes; Teacher creates/updates/soft-deletes with version/audit. Only creation notifies. No comments. |
| 34 | Student Assignments | Assignment Delivery | One scoped list of four non-quiz assignment types across enrolled OPEN classes, including subject assignments; displays deadlines, attempt/submission state and permitted published results. |
| 35 | Assignment Detail | Assignment Delivery | Student reads instructions/rubric/deadlines/attempt limits, starts/resumes the type workspace and opens Submission History; eligible Practice AI requests are made separately. |
| 36 | Submission History | Submission Review | Student views own retained attempts/receipts/results and explicitly requests AI grading of eligible submitted Practice Text/Diagram work; results remain private and outside Gradebook. |
| 37 | Codelab Workspace | Assignment Workspace | Writes code, runs public tests synchronously and submits for deterministic scoring; trial runs do not submit and consume no AI credit. |
| 38 | Text Essay Workspace | Assignment Workspace | Writes essay answers with autosave and submission; submitting Practice work does not start AI grading. |
| 39 | Diagram Essay Workspace | Assignment Workspace | Completes a locked skeleton with own text/Draw.io blocks, optional DOCX import with preview, autosave and submission. |
| 40 | Group Essay Workspace | Group Assignment | Members edit claimed/assigned parts; leader manages permitted detailed parts/allocation and submits. Deadline auto-submit includes saved unfinished drafts; one editor per part. |
| 41 | Quiz Practice History | Quiz Practice | Lists eligible lesson-linked Practice quizzes and own attempt history; opens Quiz Practice Detail/Quiz Result; results do not enter Gradebook. |
| 42 | Quiz Practice Detail | Quiz Practice | Shows one quiz's instructions, limits and own attempts/results; starts or resumes an eligible attempt. No opening/closing schedule. |
| 43 | Quiz Taking | Quiz Workspace | Answers quiz questions with autosave/countdown and manual or timer-expiry submission; opened from Learning Material or Quiz Practice Detail. |
| 44 | Quiz Result | Quiz Results | Shows deterministic attempt score and permitted answers according to snapshotted settings; opens another eligible attempt/history. |
| 45 | Teacher Class Detail | Teaching | Assigned Teacher/Subject Manager views Class Detail, Materials, Students and Evals tabs; manages class materials/groups/assessments and scoped submissions/Gradebook export. No class-structure/enrollment edits. |
| 46 | Student Detail | Student Information | Read-only popup from Teacher Class Detail Students tab showing permitted student/enrollment/group information; submission review follows the class submission flow. |
| 47 | Submission Detail | Submission Review | Assigned teacher reviews Graded submissions for the class, including students taking subject assignments; filters status/lateness and opens Grading Workspace or requests AI proposals. No Practice grading queue. |
| 48 | Grading Workspace | Grading | Grades one submission using rubric checks/manual feedback or reviewed AI proposals, handles permitted corrections with reasons, and navigates submissions/attempts; proposals remain hidden from students. |
| 49 | My Credit Package | Payment | Student/Teacher/Subject Manager views own granted/purchased balances and purchase/usage history; opens Public Credit Packages. Admin has no wallet. |
| 50 | Public Credit Packages | Payment | Lists packages on sale for Student/Teacher/Subject Manager; selecting a package opens Credit Package Checkout. Credit purchases grant no learning access. |
| 51 | Credit Package Checkout | Payment | Creates PayOS checkout and shows the buyer's verified payment result on the same screen; credits are granted only by verified webhook/reconciliation. |

## Phần 5.2–5.3 cũng cần đồng bộ

| Vị trí | Hiện tại | Cần sửa |
|---|---|---|
| 5.2 Gemini, dòng #3 | Chỉ drafting/grading/embedding | Thêm summary theo nút của Student/Teacher/Subject Manager trong quyền xem; cùng HOLD summary+embedding của requester. |
| 5.3 JOB-01 | Khẳng định không worker lease | Không có central job table vẫn đúng; bỏ phủ định lease vì U05 có claim/lease/fencing để phục hồi worker. |
| 5.3 JOB-03 | Còn nhắc comments không tạo notification | Bỏ references comments vì chức năng đã rút; chỉ announcement create gửi notification. |
| 5.3 JOB-06 | Upload/rescan → embedding, uploader credit; có NO_CREDIT | Đổi thành Extract File Learning Materials: upload → trích chữ không OCR → EXTRACTED, không AI/HOLD. Không authorized rescan hay trạng thái NO_CREDIT. |
| 5.3 JOB-07 | Caption → indexing/rescan | Add video → caption → EXTRACTED, không AI/credit. Không caption NO_CAPTION; không rescan thủ công. |
| 5.3 summary job | Thiếu job xử lý nút mới | Bổ sung Material Summary and Indexing: POST summary → quyền xem/AI guard/credit → một HOLD/request → summary/checkpoint → embedding → INDEXED; settle thực dùng/trả dư. Có thể thêm dòng riêng để dễ truy vết. |
| 5.3 JOB-09 | Mọi reservation >30 phút hoàn credit | Normal 30 phút; HOLD học liệu 25 giờ từ nhận yêu cầu, AI deadline 24 giờ. Có dùng thì settle actual và trả dư, chỉ usage=0 mới hoàn toàn bộ; child NONE không hoàn riêng. |
| 5.3 JOB-10 | Mô tả chung assignment schedules | Ghi rõ không áp quiz Practice; bài của môn dùng lịch chung cho mọi lớp OPEN. |

## Các mô tả phần III nên sửa cùng để không lại lệch

Các mục dưới đây là đối chiếu bổ sung, không đổi scope của review thành sửa toàn bộ phần III.

- III.2.3 Setting Detail: đang ghi **daily request limit**; AI-DLC dùng **requests per minute** và **daily cost cap**; bổ sung type/range/version validation.
- III.3.6 Material List: bỏ retry scanning và embedding ngay sau upload.
- III.3.7 Material Detail: bỏ replace file/move module chưa được thiết kế; chỉ đổi tên/thứ tự và archive; thêm nút summary theo quyền xem.
- III.3.8 Assignment List: đang là bài của các lớp gồm Group/Quiz và UC 41, trong khi node từ Manager Dashboard là bài của môn; chỉnh scope như bảng mới, không lẫn Teacher class Evals.
- III.3.4–3.5 Quiz List/Detail: bỏ filter Graded/Practice mode; chỉ Practice, gắn material, không schedule; ghi rõ review trước open.
- III.4.3 Learning Material: thiếu nút summary của Student theo quyết định mới.
- III.4.4 Class Announcements: bỏ comment/hide comment và các action tương ứng.
- III.4.14 Quiz Taking: timer expiry auto-submit vẫn đúng; bỏ assignment closing deadline cho quiz không schedule.
- III.6.1–6.3: role list Student/Teacher còn thiếu Subject Manager; có thể dùng chung màn theo role scope dù diagram không vẽ lại mọi entry.
- III.7.6–7.7/7.9: cùng các lỗi extraction/summary/payer/HOLD ở bảng JOB phía trên.

## Truy vết AI-DLC

- [Shared contract](../current-srs-contract.md): role, retired features, màn và quyết định summary mới.
- [U05 rules](../u05-content-material-rag/functional-design/business-rules.md), [U05 flow](../u05-content-material-rag/functional-design/business-logic-model.md): upload, summary button, quyền xem, requester billing.
- [U08 rules](../u08-assessment-core-publication/functional-design/business-rules.md): ba scope bài/quiz, shared subject schedule, Practice quiz.
- [U06 rules](../u06-rubric-question-bank/functional-design/business-rules.md): subject bank và rubric không version.
- [U03 Settings](../u03-file-and-artifact/functional-design/business-rules.md), [U07 billing](../u07-payment-credit/functional-design/business-rules.md), [U13 rules](../u13-ai-code-execution/functional-design/business-rules.md).

Lưu ý nội bộ: `component-methods.md` của AI-DLC vẫn còn chữ ký `quoteUploadCredit` và comment HOLD cùng tạo lesson trái với revision mới, dù phần prose và U05/U07/U13 detailed design đã đổi. Review này lấy yêu cầu người dùng và detailed design làm chuẩn; không tự sửa lỗi đó trong lượt review SRS.

Validation: đã đọc lại nguồn online, bảng Screen List 55 dòng, descriptions phần III và diagram ảnh đính kèm; bảng thay thế có 51 dòng liên tục, một tên mỗi dòng. Đây là kiểm tra tài liệu, không xác nhận giao diện/code đã triển khai. Không sửa online hoặc commit.

## Applied to source SRS — 2026-10-09

User authorized correction of all section 5. Updated the existing Drive Office DOCX in place, same file ID, modified 2026-10-09T07:54:03.873Z. Replaced the section 5.1.b inventory with 51 screens/popups, synchronized all eight external API descriptions and 17 background jobs, including explicit material-summary/indexing job JOB-17. Preserved section 5.1.a diagram and all other document content; the supplementary section III findings above remain review notes, not edits in this operation.

Verified local OOXML: only word/document.xml changed, only the three target tables differed, all 86 media parts and all other package parts were preserved. Rendered the 11-page section preview with Microsoft Word after the packaged renderer reported missing LibreOffice; visually inspected every page. Drive readback confirms new inventories and absence of obsolete screen names. No code change or new commit.
