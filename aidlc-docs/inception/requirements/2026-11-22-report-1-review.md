# Nhận xét Report 1 — Project Introduction

**Ngày rà soát:** 22/11/2026
**Tệp được rà soát:** `D:\Download\Report-1_Project Introduction (2).docx` (sửa lần cuối 22/11/2026, change log mới nhất 28/10/2026)
**Đối chiếu với:** [requirements.md](requirements.md), các delta đã duyệt trong thư mục này từ 29/10 đến 16/11/2026, [biên bản đối chiếu phỏng vấn 12/11](2026-11-12-lecturer-interview-gap-analysis.md) và mã nguồn nhánh hiện tại.
**Phạm vi:** chỉ đọc. Không sửa report, không sửa code.

## 1. Kết luận

Report 1 đúng hướng và khớp với phần lõi của hệ thống: tên ALAP, 4 vai trò, Assignment cá nhân GRADED/PRACTICE, bài nhóm chỉ GRADED, Quiz chỉ PRACTICE và không vào sổ điểm, AI chỉ đề xuất, MCP cho Student/Teacher/Subject Manager, bỏ thanh toán và Code Lab.

Report dừng ở mốc 28/10/2026, nên chưa có các quyết định từ 29/10 đến 16/11. Có 4 điểm cần sửa vì report đang mô tả khác hệ thống hiện tại (mục 2). Có khoảng 12 tính năng đã duyệt và đã làm mà report chưa nhắc (mục 3). Ngoài ra còn một số lỗi trình bày (mục 4).

| Mức | Số mục | Ý nghĩa |
|---|---|---|
| 🔴 Sai lệch | 4 | Report mô tả khác hệ thống hoặc khác quyết định đã chốt |
| 🟡 Thiếu | 12 | Tính năng đã duyệt và đã làm sau 28/10 nhưng chưa có trong report |
| ⚪ Trình bày | 9 | Mục lục, từ viết tắt, chính tả, câu lặp |

## 2. Sai lệch cần sửa

### S1. Thời điểm chấm Assignment GRADED

- **Vị trí:** IV (đoạn *Grading and practice*), FE-10, FE-12.
- **Report viết:** "Teachers grade submitted GRADED Assignments using the saved rubric." Câu này hiểu là giảng viên chấm được ngay khi có bài nộp.
- **Thực tế:** theo GRADE-01/CLOSE-01/CLOSE-02 duyệt ngày 15/11 ([2026-11-15-date-ai-grading-window.md](2026-11-15-date-ai-grading-window.md)), trước khi bài hết nhận nộp thì giảng viên chỉ được xem. Bài hết nhận nộp khi qua `lateUntil`, hoặc qua `closesAt` nếu không có hạn nộp muộn, hoặc khi giảng viên bấm **Đóng nhận bài**. Chấm tay, chấm bằng AI, lưu điểm, công bố và MCP đều bị chặn ở backend cho tới lúc đó.
- **Đề xuất:** thêm vào IV và FE-12: *"Grading opens once the Assignment stops accepting submissions, either at its deadline or late window or when the Teacher closes submissions. Before that, Teachers can preview submissions only."* Đưa nút Đóng nhận bài vào FE-07.

### S2. Backup mã hóa

- **Vị trí:** IV (*Grade records and deployment*), FE-14.
- **Report viết:** "Encrypted database backups…"
- **Thực tế:** từ 16/11 (FR-038 as-built), khóa `U03_BACKUP_PUBLIC_KEY` là tùy chọn. Nếu để trống, backup vẫn chạy nhưng tệp dump **không mã hóa** và log có cảnh báo.
- **Đề xuất:** cách 1 là xác nhận production đã đặt khóa rồi giữ nguyên câu. Cách 2 là viết lại: *"Database backups are encrypted when a backup key is configured, which is required for production."*

### S3. Lưu tệp trên Google Drive ở production

- **Vị trí:** IV ("private Google Drive storage in production"), FE-15, LI-10.
- **Thực tế:** FR-013 (as-built 16/11) cho phép hai cách dùng Google Drive: Shared Drive với service account, hoặc Drive cá nhân qua OAuth `drive.file`. Môi trường demo được lưu trên đĩa máy chủ. Lần kiểm tra staging ngày 16/11 ghi lại rằng **production đang lưu tệp trên đĩa VPS**.
- **Đề xuất:** kiểm tra cấu hình production hiện tại. Nếu vẫn lưu trên đĩa thì sửa câu trong IV và LI-10 cho đúng, hoặc chuyển production sang Drive trước khi nộp report. Đây là điểm hội đồng có thể hỏi khi demo.

### S4. Phần AS-IS chưa có nguồn và chưa dùng buổi phỏng vấn

- **Vị trí:** II.1–II.5, III.1.
- **Vấn đề:** II mô tả quy trình hiện tại (Word, Visual Paradigm, nén ZIP rồi nộp lên EduNext, nhập điểm ở hệ thống riêng) nhưng không nói thông tin lấy từ đâu. II.5 còn viết *"Further interviews will help establish…"*. Theo requirements.md §2.3, nhóm không được tuyên bố đã khảo sát AS-IS khi chưa có bằng chứng. Hiện nhóm đã phỏng vấn giảng viên ngày **12/11/2026**, và biên bản đối chiếu đề xuất dùng buổi này làm bằng chứng AS-IS (mục 15).
- **Một số chi tiết trong buổi phỏng vấn chưa có trong II.4:** rubric chia điểm nhỏ (0,1–0,2 cho mỗi use case), có mục trừ điểm, đánh giá thành viên nhóm theo 5 mức, hỏi vấn đáp từng bạn, điểm PT trắc nghiệm có giờ kết thúc, xuất điểm theo đầu điểm (PT/Lab/Assignment) để nhập vào phần mềm của trường, và lo ngại về độ chính xác khi AI chấm.
- **Đề xuất:** trong II ghi rõ nguồn, ví dụ *"Based on an interview with an SWR/SWD lecturer on 12/11/2026…"*. Đối chiếu lại các chi tiết Visual Paradigm, ZIP và EduNext với biên bản phỏng vấn gốc (`2026-11-12-tom-tat-nghiep-vu-phong-van.docx`). Bổ sung các thực hành vừa nêu vào II.4. Những nhu cầu chưa đáp ứng (đầu điểm, Quiz tính điểm, mẫu rubric) nên chuyển thành giới hạn trong mục V.2, hoặc ghi là đang chờ GVHD quyết định (M1–M3).

## 3. Tính năng đã duyệt sau 28/10 nhưng chưa có trong report

Lưu ý: biên bản phỏng vấn nói mọi chức năng thêm hoặc bớt phải được GVHD duyệt. Nhóm nên xác nhận với GVHD trước khi đưa các mục dưới đây vào phạm vi của Report 1.

| # | Tính năng (nguồn) | Nên bổ sung vào |
|---|---|---|
| T1 | Sinh viên tự lập đề xuất nhóm, giảng viên duyệt ([SG-01…09](2026-11-11-student-self-grouping.md), 11/11) | FE-11, IV |
| T2 | Đánh giá đóng góp thành viên theo 5 mức, kèm điểm và lý do ([member-contribution-levels](2026-11-13-member-contribution-levels.md), 13/11) | FE-12, GAP-05 |
| T3 | Mã số sinh viên, định dạng `SE123456` và không trùng ([student-code](2026-11-13-student-code.md), 13/11) | FE-01 |
| T4 | Học kỳ của lớp tính tự động từ ngày bắt đầu, lọc lớp theo kỳ ([class-term](2026-11-14-class-term.md), 14/11) | FE-02 |
| T5 | Nhập đề từ ảnh hoặc PDF thành khung đề chỉnh sửa được ([exam-image-import](2026-11-11-exam-image-import.md), 11/11) | IV, FE-06 |
| T6 | Nhập tài khoản, danh sách sinh viên và câu hỏi bằng Excel `.xlsx` (delta 09/11). FE-01 hiện vẫn ghi "CSV import" | FE-01, FE-02, FE-05 |
| T7 | Đề Assignment mẫu cấp môn do Subject Manager soạn, giảng viên sao chép về lớp (FR-027) | FE-06/FE-07 |
| T8 | Nút Đóng nhận bài (CLOSE-02, 15/11), liên quan S1 | FE-07 |
| T9 | Phân bố điểm ẩn danh cho sinh viên, giảng viên bật theo từng bài, tối thiểu 20 người (FR-024) | FE-12 hoặc FE-04 |
| T10 | Quiz bốc câu ngẫu nhiên từ ngân hàng cho từng lượt (`GENERATED`/`DrawRule`), đảo câu và đáp án | FE-05, FE-10 |
| T11 | Giao diện 3 ngôn ngữ (Anh mặc định, Việt, Nhật), chọn ngôn ngữ cho bản nháp AI, trang `/privacy` công khai (as-built 16/11) | FE-01 hoặc FE-04, NFR trong IV |
| T12 | Email cho các sự kiện ghi danh, bài mới, công bố điểm, cùng nhắc hạn tự động 24 giờ trước hạn (FR-011/019). FE-14 mới liệt kê nhắc hạn, thông báo lớp quan trọng và lỗi backup | FE-14 |

Có thêm hai chỗ nhỏ:

- **FE-15:** đổi "Brevo SMTP" thành "Brevo (SMTP or HTTPS API)", vì từ 06/11 hệ thống chọn kiểu gửi qua `MAIL_TRANSPORT`.
- **FE-01 "institution-issued accounts":** từ 06/11, email đăng nhập dùng tên miền nào cũng được. Cụm từ này vẫn đúng nếu hiểu là tài khoản do trường quản lý, nhưng không nên viết thành "institutional email domain".

## 4. Trình bày và định dạng

1. **Mục lục chưa cập nhật:** thiếu II.1–II.5 (Teaching Context … Proposed Opportunities and Evaluation). Bấm chuột phải vào mục lục, chọn **Update Field → Update entire table**.
2. **Ngày trên trang bìa** là "Hanoi, October 2026". Nếu nộp bản mới thì đổi thành November 2026 và **thêm một dòng change log** cho lần sửa này. Bản (2) hiện chưa có dòng nào sau 28/10.
3. **Bảng từ viết tắt:**
   - Có định nghĩa nhưng không dùng trong thân bài: **FR, NFR, LLM, RBAC**. Nên bỏ, hoặc dùng các từ này trong bài.
   - Có dùng nhưng chưa định nghĩa: **SRS, SDD, SWR, SWD, CSV, OTP, SMTP, LMS, SSO, URL**, cùng các mã GAP/FE/LI. SWR và SWD đã được viết đầy đủ ngay trong bài, nhưng vẫn nên đưa vào bảng.
4. **Cách viết EduNext chưa thống nhất:** "EduNext" xuất hiện 7 lần, "eduNext" 1 lần (LI-03) và 1 lần trong change log. Nên chọn một cách viết.
5. **Viết hoa chưa thống nhất:** "Practice Quizzes" (IV) và "practice Quizzes" (FE-10). Nên dùng "PRACTICE" theo bảng thuật ngữ, hoặc thống nhất một cách viết.
6. **Câu tóm tắt bị lặp:** II.2 (đoạn cuối) và II.3 (đoạn cuối) nhắc lại gần nguyên văn đoạn ngay phía trên. Nên bỏ, hoặc thay bằng sơ đồ AS-IS.
7. **I.2 Project Purpose quá dài** (khoảng 250 từ) và gộp cả mục đích lẫn chi tiết API/MCP. Nên giữ 3–4 câu về mục đích. Chi tiết GRADED/PRACTICE và API/MCP đã có ở IV nên chuyển sang đó.
8. **Phần Weaknesses của Artemis và PrairieLearn** ("requires testing", "requires a configured exercise") là việc nhóm cần làm, chưa phải điểm yếu của sản phẩm. Hội đồng dễ hỏi vì sao chưa thử. Nên nêu điểm yếu cụ thể, ví dụ Artemis không có tài liệu SRS/SDD nhiều phần cho nhóm và chưa có AI chấm theo rubric, PrairieLearn không có trình vẽ UML tích hợp. Nếu chưa thử được thì ghi rõ là "chưa đánh giá".
9. **Câu GAP-07 khó đọc:** "Preserve recoverable academic state when external integrations fail through bounded retries…". Nên viết lại: *"When an external service fails, keep academic data recoverable by using bounded retries, idempotent requests and version checks."* Tiêu đề LI-12 gộp 3 ý khác nhau (thanh toán, chạy code, quyết định AI), nên tách ra hoặc đổi tên.

## 5. Các điểm đã kiểm và khớp

- Bài nhóm tự nộp khi đóng: có `GroupAutoSubmitScanner`.
- Tự chọn nguồn AI khi không chỉ định: ưu tiên API chung nếu còn đủ quota, nếu không thì dùng khóa cá nhân (`AiTaskService.resolveSource`, yêu cầu 05/11). Report mô tả **đúng với code**. Tuy vậy, requirements.md §2.2, FR-006, FR-021, FR-034 và FR-036 vẫn ghi "hết quota không tự dùng personal". Tài liệu yêu cầu đang mâu thuẫn với code, nên sửa requirements.md chứ không sửa report.
- Giảng viên sửa được điểm đã công bố khi lớp còn mở: khớp quy tắc U09 ngày 11/11. Quy định khóa điểm ngày 02/11 đã bị thay.
- Admin không có MCP; Student chỉ nhận góp ý AI cho bài PRACTICE cá nhân đã nộp; bài nhóm không có AI cho Student.
- Quiz không có lịch mở/đóng, chấm theo từng lượt và không vào sổ điểm. Gợi ý trừ điểm khi rời trang chỉ áp dụng cho Quiz và giảng viên quyết định có trừ hay không.
- Xuất sổ điểm CSV/Excel có cả điểm nháp. Backup chỉ gồm database và giữ trong 7 ngày. Không có restore trên production.
- Công nghệ Next.js, Spring Boot và PostgreSQL khớp với NFR-001.

## 6. Việc nên làm tiếp

1. Sửa S1–S4. S3 cần kiểm cấu hình production trước khi sửa.
2. Xin GVHD xác nhận T1–T12 có thuộc phạm vi Report 1 không, rồi bổ sung vào FE và IV.
3. Sửa các mục trình bày ở phần 4, cập nhật mục lục, ngày trên bìa và change log.
4. Ngoài report: đồng bộ requirements.md §2.2 với quy tắc tự chọn nguồn AI đã chạy trong code.
