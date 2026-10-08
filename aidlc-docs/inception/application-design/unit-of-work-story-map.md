# Unit of Work Story and Use Case Map

## 1. Bản hiệu lực và ownership

Theo SRS 4.2/4.4 và [bảng UC/màn](../../../docs/use-cases-and-screens.md), bản ngày 2026-10-08 có **70 UC, 51 story, 16 unit**. Mỗi UC và story có đúng một primary unit; unit khác đóng góp bằng contract đã công bố. Một story có thể hỗ trợ nhiều UC, nhưng không cộng lặp vào số story của unit.

US-PAY-004/005 bổ sung quản trị gói/lịch sử thanh toán. US-AUD-001 chủ trì UC 70. US-AIG-003 không có UC trực tiếp, là vận hành hỗ trợ AI; không thêm UC giả. Mã UC cũ chỉ dùng trong lịch sử, không dùng để triển khai.

## 2. Coverage theo unit

| Unit | UC chủ trì | Số UC | Đóng góp/ranh giới | Story chủ trì | Số story |
|---|---|---:|---|---|---:|
| U01 Account & Access | UC 01–07, 58–62 | 12 | Role/scope cho mọi UC; số tài khoản cho UC 57 | US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007 | 7 |
| U02 Audit | UC 70 | 1 | Audit append-only cho các command đặc quyền/nghiệp vụ | US-AUD-001 | 1 |
| U03 File, Job & Event | Không có UC trực tiếp | 0 | Tệp, jobs, worker và events dùng chung | - | 0 |
| U04 Subject, Class, Enrollment & Learning Access | UC 12–13, 27–28, 45–50, 63–66 | 14 | Scope R2–R5 cho tài nguyên môn/lớp; enrollment và roster hỗ trợ | US-CAT-001, US-CAT-002, US-CAT-003, US-CAT-005, US-LRN-001 | 5 |
| U05 Content, Material & RAG | UC 14, 26, 29–31, 51–52 | 7 | RAG cho AI draft; materials dùng chung UC 12–14 | US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005 | 4 |
| U06 Rubric & Question Bank | UC 32–33, 44, 55–56 | 5 | Question/rubric version cho UC 39–43, 54 | US-QBK-001, US-QBK-002 | 2 |
| U07 Payment & AI Credit | UC 08–10, 67–69 | 6 | Credit và snapshot thanh toán; mức tặng tháng cấu hình | US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005 | 4 |
| U08 Assessment Core & Publication | UC 38 | 1 | CRUD/lifecycle chung cho UC 39–43, schedule/version cho U09/U11/U14 | US-ASM-001 | 1 |
| U09 Question Type Authoring | UC 39–43 | 5 | Cấu hình năm loại bài, document editor và rubric; U08 lifecycle | US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007 | 5 |
| U10 Template & Copy | UC 53–54 | 2 | Copy/template/version/diff trong UC 39–43; R2 cho template môn | US-ASM-008, US-ASM-009, US-ASM-010 | 3 |
| U11 Attempt & Submission | UC 17–22, 24–25 | 8 | Nộp cá nhân và kết quả riêng tư, dùng U09/U13/U15 | US-ASM-003, US-ASM-012 | 2 |
| U12 Group & Allocation | UC 15–16 | 2 | Chia nhóm/leader trong Class Detail, kiểm nhóm cho UC 43/23 | US-GRP-001, US-GRP-002 | 2 |
| U13 AI & Code Execution | Không có UC trực tiếp | 0 | AI draft UC 39–43/54/56; chấm UC 25/35; Judge0 UC 21/41 | US-AIG-001, US-AIG-002, US-AIG-003 | 3 |
| U14 Group Document & Submission | UC 23 | 1 | Tài liệu nhóm và bản nộp cho UC 34–36 | US-GRP-004, US-GRP-005 | 2 |
| U15 Grading | UC 34–37 | 4 | Gradebook nguồn UC 37; U16 xuất tệp; U13 đề xuất AI | US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005 | 6 |
| U16 Reporting & Notification | UC 11, 57 | 2 | Xuất tệp của UC 37; progress/distribution UC 17/34; events | US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001 | 4 |
| **Tổng** | **70 UC, mỗi UC một primary unit** | **70** | | **51 story, mỗi story một primary unit** | **51** |

## 3. Luồng giao nhau

| Luồng | Chủ trì | Đóng góp |
|---|---|---|
| Danh sách bài Teacher (UC 38) và CRUD năm dạng (UC 39–43) | U08 danh sách/lifecycle; U09 nội dung theo dạng | U06 bank/rubric, U10 copy, U13 AI draft, U12 nhóm hợp lệ |
| Student danh sách/chi tiết/làm bài/lịch sử (UC 17–22, 24) | U11 | U04 scope, U09 document/type, U13 Judge0, U15 kết quả đã công bố |
| AI Practice Student (UC 25) | U11 | U13 gọi AI, U07 credit; không cần Teacher duyệt |
| AI Grading Proposals Teacher (UC 35) | U15 | U13 AI, U07 credit; Teacher quyết định điểm cuối |
| Chấm tay và gradebook (UC 36–37) | U15 | U16 tạo CSV/XLSX, U11/U14 bản nộp, U04 scope |
| Nhóm và bài nhóm (UC 15–16, 23, 43) | U12 nhóm; U14 làm/nộp; U09 soạn | U04 roster, U08 lifecycle, U15 grade |
| Học liệu/viewer (UC 14, 29–30, 51–52) | U05 | U03 Drive, U04 scope, U13 embedding |
| Class/Subject Question Bank (UC 32–33, 55–56) | U06 | Dùng cùng scope/version; phân công lớp R3/R4 hoặc môn R2 |
| Template cấp môn (UC 53–54) | U10 | U08 assignment, U09 types, U13 draft; Subject Template và Template Editor |
| Credit và thanh toán (UC 08–10, 67–69) | U07 | U01 quyền Admin/chủ tài khoản, U02 audit, U03 jobs, PayOS |
| Audit Log (UC 70) | U02 | U01 Admin authorization; không cập nhật/xóa log |

## 4. Scope và luồng hỗ trợ

- Student đọc/làm/nộp theo R5; Teacher theo R3; Subject Manager/Administrator dùng Teacher chỉ khi được giao lớp R4, tài nguyên môn khi được giao môn R2.
- Administrator quản lý cấu trúc môn/lớp và tài khoản Full; không suy quyền chấm/gradebook từ Full đó. Phân công người quản lý môn và giảng viên lớp cho phép role Admin ACTIVE.
- U04 Learning Access gồm UC 12–13; UC 14 dùng U05 viewer và scope U04. Không có dashboard Student, learning path hoặc lesson progress; Statistic UC 57 do U16 tổng hợp.
- Ghi danh/mã mời, chia nhóm, bình luận, AI draft, review/publish/copy/retire và chốt/công bố điểm vẫn là luồng hỗ trợ; mã/tên 70 UC không thay đổi vì các luồng này.
- U03 và U13 không chủ trì UC trực tiếp. U02 có UC 70; tính năng quản trị AI hỗ trợ vẫn thuộc US-AIG-003/U13.
- US-GRP-003 thuộc U09; U14 đóng góp tài liệu nhưng không tính story này lần thứ hai.

## 5. Coverage assertions

- Danh mục có ID 01–70 liên tục, không trùng; mỗi ID có đúng một unit và có story trong stories.md mục 14.
- 51 mã story xuất hiện đúng một lần ở cột Story chủ trì. Không tạo lại US-PAY-003 hoặc US-ASM-011 đã rút.
- UC 17/38 tách Student/Teacher; UC 33/56 tách Class/Subject Question; UC 25/35 tách AI Practice/Teacher proposals.
- Cấu trúc 16 unit và hướng dependency được giữ. Thiết kế Construction/code plans cũ còn cần đồng bộ; không coi việc sửa Inception là đã triển khai các UC mới.
