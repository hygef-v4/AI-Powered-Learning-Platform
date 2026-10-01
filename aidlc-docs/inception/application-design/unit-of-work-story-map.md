# Unit of Work Story and Use Case Map

## 1. Quy tắc đối chiếu

Bản kế hoạch 16 unit và wave không thay thế catalog UC/story trong repository. Phần Learning Access đã được gộp vào U04. Bảng này dùng mã số 1–40 trong `docs/use-case-table.md` và mã story trong `stories.md` (ma trận story ↔ use case ở `stories.md` mục 14). Mỗi UC có đúng một unit chủ trì; UC gộp từ nhiều thao tác có thể có unit khác đóng góp qua public contract trong `unit-of-work-dependency.md`. Một story có thể hỗ trợ nhiều UC nhưng chỉ có một primary unit cho acceptance criteria.

Danh mục hiện hành gồm **40 UC và 49 story thuộc MVP**. Mã cũ dạng `UC-XXX-NN` tra ở bảng Legacy UC codes của `docs/use-case-table.md`. Thông báo/hỏi đáp lớp, dashboard cá nhân và xuất bảng điểm đều thuộc MVP.

## 2. Coverage theo unit

| Unit | UC chủ trì | Unit đóng góp vào UC gộp | Story chủ trì | Story triển khai |
|---|---|---|---|---:|
| U01 Account & Access | UC 1–7 | - | US-IAM-001..007 | 7 |
| U02 Audit, Job & Event | UC 39 | - | US-AUD-001 | 1 |
| U03 File & Artifact | Không có UC trực tiếp | Hạ tầng dùng chung | - | 0 |
| U04 Subject, Class, Enrollment & Learning Access | UC 8, 9, 10, 12, 19 | U12 phần nhóm của UC 9 | US-CAT-001..003, US-CAT-005, US-LRN-001 | 5 |
| U05 Content, Material & RAG | UC 11, 13, 14 | - | US-CNT-001, 002, 004, 005 | 4 |
| U06 Rubric & Question Bank | UC 20 | Ngân hàng câu hỏi có version cho UC 23–27 (mọi dạng bài) | US-QBK-001..002 | 2 |
| U07 Payment & AI Credit | UC 37 | Gói credit và mức tặng cho UC 22 | US-PAY-001..002 | 2 |
| U08 Assessment Core & Publication | UC 28 | U09, U10, U13 (AI soạn bài) đóng góp vào UC 28; U08 cung cấp aggregate/publication cho UC 27 | US-ASM-001 | 1 |
| U09 Question Type Authoring | UC 23, 24, 25 | U06 câu hỏi cho UC 23–25 | US-ASM-004, 006, 007 | 3 |
| U10 Template & Copy (directory name retained for link stability) | UC 21 | Copy template, copy giữa lớp, version/diff của UC 28; U13 bản nháp AI cho UC 21 | US-ASM-008..010 | 3 |
| U11 Attempt & Submission | UC 29, 30, 31, 40 | U13 chạy Code Lab/AI cho UC 30, 40 | US-ASM-003, US-ASM-012 | 2 |
| U12 Group & Allocation | UC 15 | Nhóm, chia ngẫu nhiên, trưởng nhóm trong UC 9; kiểm nhóm hợp lệ trước khi phát hành bài nhóm (UC 27) | US-GRP-001..002 | 2 |
| U13 AI & Code Execution | UC 22, 26 | Bản nháp AI cho UC 21 và UC 28; U07 gói credit cho UC 22; U06 câu hỏi `CODE` cho UC 26 | US-AIG-001..003, US-ASM-005 | 4 |
| U14 Group Document & Submission | UC 16, 27 | U06 khung tài liệu, U08 publication, U09 trình soạn khung, U12 nhóm hợp lệ cho UC 27 | US-GRP-003..005 | 3 |
| U15 Grading | UC 17, 32, 33, 34, 35 | Sổ điểm và lịch sử điểm cho UC 36 | US-GRD-001..005, US-GRP-006 | 6 |
| U16 Reporting & Notification | UC 18, 36, 38 | U04, U11, U14, U15 cấp dữ liệu cho UC 18, 36 | US-RPT-001..003, US-NTF-001 | 4 |
| **Tổng đang triển khai** | **40 UC** | | **49 story** | **49** |

U03 không chủ trì UC: U03 cung cấp FileArtifactService cho các luồng upload/download, Draw.io và worker của các unit khác (kiểm tại gate G1). U10 chủ trì UC 21 Manage Templates (tạo thủ công hoặc nhận bản nháp AI từ U13, sửa, xoá, phát hành template); copy template vào lớp, copy giữa lớp và version/diff vẫn thuộc UC 28 Manage Assignments do U08 chủ trì. AI soạn bài cho lớp (trước là UC riêng) nay là một luồng của UC 28, U13 thực hiện. Bài nhóm tách khỏi Manage Assignments thành UC 27 Manage Group Assignment (U14 chủ trì: tạo, sửa bài nhóm, mục chính, nhả khóa mục); phát hành, nhân bản, version và ngưng giao bài nhóm vẫn theo UC 28.

## 3. Quyết định phân chia các luồng giao nhau

| Trường hợp | Unit chủ trì | Unit cung cấp contract |
|---|---|---|
| Tạo đề thủ công, duyệt và phát hành lớp | U08 | U04 scope, U06 bank, U09 cấu hình kiểu câu hỏi |
| Cấu hình quiz/essay/tài liệu (không có đề chung cấp môn) | U09 | U08 aggregate/publication, U06 rubric/question versions |
| AI tạo bản nháp đề (UC 28) hoặc template (UC 21) | U13 | U05 nguồn học liệu, U06 bank; U08 duyệt/lưu/publish bài, U10 lưu template |
| Thay đổi assignment đã giao | U08 | Không sửa version đang giao; ngưng giao/đóng rồi sửa tạo version mới. U10 giữ template/copy và diff version |
| Nộp bài Practice Text/Diagram Essay và chấm AI | U11 sở hữu attempt và kích hoạt một yêu cầu chấm khi nộp đủ credit | U08 cung cấp dạng/chế độ; U07 giữ/trừ credit; U13 chạy AI; U15 chỉ xử lý điểm `GRADED` |
| Soạn bài tài liệu (DOCUMENT) có sơ đồ Draw.io | U09 sở hữu mô hình tài liệu, khung, nhập/xuất DOCX và quy tắc kiểm XML; U11 xác nhận DOCX của người học vào lượt đang làm | U03 giữ ảnh; U08 aggregate/publication; U11 sở hữu bản nháp/lượt |
| Soạn Code Lab và nộp bài tài liệu/Code Lab | U13 sở hữu Code Lab authoring và CodeExecution; U11 sở hữu attempt/submission | U03 giữ artifact; U08 publication; U09 mô hình tài liệu |
| Bài nhóm | U12 nhóm và trưởng nhóm; U14 tài liệu nhóm, nhận mục, ghép realtime, nộp | U09 mô hình tài liệu; U15 lưu grade cuối |
| Trang lớp và học liệu | U04 kiểm quyền và trả lớp/nội dung | U04 enrollment, U05 content |
| Dashboard kết quả cá nhân | U16 tổng hợp bài sắp hạn, trạng thái nộp và điểm đã công bố của người học | U04 enrollment/cờ phân bố, U08 assignment, U11/U14 submission, U15 grade |
| Xuất bảng điểm CSV/XLSX | U16 kiểm quyền và tạo tệp theo yêu cầu | U04 phạm vi lớp, U15 điểm cuối/sổ điểm, U11/U14 trạng thái nộp |
| Chấm bài nhóm | U15 chấm tài liệu nhóm (tay), phần đóng góp (tay/AI), điểm cuối từng người | U14 cung cấp bản nộp và tác giả từng mục; U13 chỉ đề xuất khi giảng viên chọn |

## 4. Learning Access trong U04 và chức năng đã loại

- UC 12 Access Lesson và UC 19 Access Enrolled Class thuộc U04: U04 kiểm enrollment rồi trả lớp, bài học đã phát hành và dữ liệu không nhạy cảm trong phạm vi. UC 18 View Learning Overview do U16 tổng hợp từ dữ liệu U04, U08, U11, U14, U15.
- Các use case và story về lưu/xem tiến độ hoàn thành nội dung đã bị xóa khỏi danh mục; U04 không lưu trạng thái hoàn thành bài học.
- Không có learning path trong plan hiện hành. U04 không tạo lộ trình, prerequisite, completion record hay `learning_progress`. Trạng thái nộp bài và job vẫn được U11/U16/U02 quản lý theo nghiệp vụ riêng.

## 5. Coverage assertions

- U01-U16 bao phủ đúng 40 UC trong phạm vi, mỗi UC một unit chủ trì; U03 có 0 UC chủ trì.
- Mọi story trong `stories.md` xuất hiện một lần ở bảng unit; catalog chỉ chứa 49 story MVP.
- Simulation Exam (`UC-ASM-18`, `US-ASM-011`) đã rút khỏi phạm vi; UC 40 Grade Practice with AI và `US-ASM-012` dành cho AI chấm bài Practice của Student.
- Nhân bản, version sau khi ngưng giao, diff, copy template vào lớp và copy giữa lớp đều thuộc UC 28 (U08 chủ trì, U10 đóng góp); tạo, sửa, xoá và phát hành template môn thuộc UC 21 (U10 chủ trì); khóa nội dung bài đã phát hành là invariant của U08.
- Ngân hàng câu hỏi (U06) giữ version câu hỏi của mọi dạng bài: `ESSAY` cho Text Essay (UC 23), `MCQ_SINGLE`/`MCQ_MULTI` cho quiz (UC 24), `DOCUMENT` cho Diagram Essay (UC 25), `CODE` cho Code Lab (UC 26) và `DOCUMENT` có mục việc cho bài nhóm (UC 27). Câu hỏi được tìm, nhập hàng loạt và dùng lại trong use case của dạng bài tương ứng; không có UC ngân hàng câu hỏi riêng.
- Không đưa quản lý học kỳ/nhân bản lớp hoặc phân công chấm chéo vào MVP; hai chức năng này không có story trong catalog hiện hành.
