# Unit of Work Story and Use Case Map — 73 UC

## 1. Baseline và primary ownership

Bản 2026-10-09 theo [73 UC](../../../docs/use-cases-73.md), [requirements](../requirements/requirements.md), [stories](../user-stories/stories.md) và Page-2 của `docs/G21_Diagrams.drawio`: **73 UC, 51 story, 15 unit**. ID unit giữ ổn định U01–U09, U11–U16; không đánh số lại sau khi bỏ U10.

| Unit | Primary UC | Primary stories |
|---|---|---|
| U01 | 01, 02, 03, 04, 05, 06, 07, 59, 60, 61, 62, 63 | US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007 |
| U02 | 73 | US-AUD-001 |
| U03 | 70, 71 | US-SET-001 |
| U04 | 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67 | US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001 |
| U05 | 15, 30, 33, 34, 36, 54, 55 | US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005 |
| U06 | 46, 56, 57 | US-QBK-001, US-QBK-002 |
| U07 | 08, 09, 10, 11, 68, 69, 72 | US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005 |
| U08 | 41 | US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010 |
| U09 | 35, 42, 43, 44, 45 | US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007 |
| U11 | 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 | US-ASM-003, US-ASM-012 |
| U12 | 16, 17 | US-GRP-001, US-GRP-002 |
| U13 | Không primary UC; hỗ trợ AI/Judge0 | US-AIG-001, US-AIG-002, US-AIG-003 |
| U14 | 27 | US-GRP-004, US-GRP-005 |
| U15 | 37, 38, 39, 40 | US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005 |
| U16 | 12, 58 | US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001 |

## 2. Boundary và đóng góp

| Unit | Phạm vi/đóng góp |
|---|---|
| U01 | Role/authentication cho mọi unit; counts cho Admin Dashboard. |
| U02 | Audit append-only trong transaction mọi command nhạy cảm. |
| U03 | Tệp/worker/events dùng chung; lưu Settings và kiểm version/audit; U07/U13 khai báo các mục của mình. |
| U04 | R2–R5 scope cho tài nguyên môn/lớp; Admin chỉ quản lý môn, danh sách lớp của môn chỉ đọc. |
| U05 | Tóm tắt/embedding và RAG cho AI; quiz/attempt do U08/U09/U11 giữ. |
| U06 | Rubric thuộc bài, tự tạo và khóa khi phát hành; câu ngân hàng SUBJECT và câu riêng ASSIGNMENT. |
| U07 | Ví của ba role; snapshot payment, verified webhook và grant định kỳ qua Settings. |
| U08 | Vòng đời chung UC 35/42–45, bài của môn, copy/version; U09 cấu hình theo dạng. |
| U09 | Cấu hình quiz và bốn dạng assignment, document model; U08 vòng đời, U06 rubric. |
| U11 | Quiz Practice tách Student Assignments; lần làm bài của môn ghi classId. |
| U12 | Chia nhóm/leader hỗ trợ Teacher Class Detail và UC 45/27. |
| U13 | AI draft, Practice grading UC 29, proposals UC 38, Judge0 UC 25/43, số liệu AI UC 58. |
| U14 | Tài liệu nhóm chỉ ở lớp, GRADED; chỉ leader nộp. |
| U15 | Bài của môn chấm/công bố theo từng lớp; Practice/quiz không vào gradebook. |
| U16 | Progress/distribution và export của UC 40; thông báo bài của môn tới mọi lớp OPEN. |

## 3. Quyền và coverage assertions

- Student theo R5, Teacher theo R3, Subject Manager theo R2 hoặc R4 của lớp được giao. Admin chỉ User và quản trị, không nhận phân công môn/lớp, không ví và không AI.
- 73 ID UC liên tục, đúng một primary unit mỗi UC; 51 story đúng một primary unit. US-SET-001 thuộc U03; US-ASM-009/010 thuộc U08; US-CAT-005, US-PAY-003 và US-ASM-011 đã rút.
- U09 chủ trì authoring UC 35/42–45; U08 đóng góp vòng đời, lịch, version và copy. Subject Manager soạn bài của môn theo FR-027 là supporting flow trong các UC authoring; không tự thêm UC vào catalog.
- U13 không chủ trì UC riêng; Settings do U03 giữ, cấu hình nhóm AI do U13 khai báo. U02 Audit UC 73; U16 notification UC 12/Admin Dashboard UC 58.
- Câu riêng scope ASSIGNMENT và rubric scope CLASS/SUBJECT vẫn hợp lệ; chỉ ngân hàng lớp bị bỏ. Quiz gắn học liệu, không lịch/gradebook.
- Code/contracts chưa được đồng bộ bởi revision tài liệu. Approvals trước đây giữ baseline cũ; thực hiện checklist revision trước khi coi yêu cầu đã triển khai.
