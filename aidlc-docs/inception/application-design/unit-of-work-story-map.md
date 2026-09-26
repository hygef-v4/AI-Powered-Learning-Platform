# Unit of Work Story and Use Case Map

## 1. Quy tắc đối chiếu

Bản kế hoạch 16 unit và wave không thay thế catalog UC/story trong repository. Phần Learning Access đã được gộp vào U04. Bảng này dùng mã hiện có trong `use-cases.md` và `stories.md`. Mỗi UC còn hiệu lực có đúng một unit chủ trì; một story có thể hỗ trợ nhiều UC nhưng chỉ có một primary unit cho acceptance criteria. Các unit khác tham gia qua public contract trong `unit-of-work-dependency.md`.

Danh mục hiện hành chỉ gồm **77 UC và 49 story thuộc MVP**. Thông báo/hỏi đáp lớp, dashboard cá nhân và xuất bảng điểm đều thuộc MVP. Mọi UC/story ngoài phạm vi đã được xóa khỏi hai catalog.

## 2. Coverage theo unit

| Unit | UC chủ trì trong phạm vi | Story chủ trì trong phạm vi | Story triển khai |
|---|---|---|---:|
| U01 Account & Access | UC-IAM-01..12 | US-IAM-001..007 | 7 |
| U02 Audit, Job & Event | UC-OPS-02 | US-AUD-001 | 1 |
| U03 File & Artifact | Không có UC trực tiếp | Hạ tầng dùng chung | 0 |
| U04 Subject, Class, Enrollment & Learning Access | UC-CAT-01..13, UC-LRN-01, 02, UC-CNT-04 | US-CAT-001..003, US-CAT-005, US-LRN-001 | 5 |
| U05 Content, Material & RAG | UC-CNT-01..03, 06..08 | US-CNT-001, 002, 004, 005 | 4 |
| U06 Rubric & Question Bank | UC-QBK-01..02 | US-QBK-001..002 | 2 |
| U07 Payment & AI Credit | UC-PAY-01 | US-PAY-001..002 | 2 |
| U08 Assessment Core & Publication | UC-ASM-01, 07 | US-ASM-001 | 1 |
| U09 Question Type Authoring | UC-ASM-02, 03, 04, 06 | US-ASM-004, 006, 007 | 3 |
| U10 Template, Copy & Simulation | UC-ASM-15..18 | US-ASM-008..011 | 4 |
| U11 Attempt & Submission | UC-ASM-09..14 | US-ASM-003 | 1 |
| U12 Group & Allocation | UC-GRP-01..04 | US-GRP-001..002 | 2 |
| U13 AI & Code Execution | UC-AIG-01..03, UC-ASM-05 | US-AIG-001..003, US-ASM-005 | 4 |
| U14 Group Document & Submission | UC-GRP-05..07 | US-GRP-003..005 | 3 |
| U15 Grading | UC-GRD-01..07, UC-GRP-08 | US-GRD-001..005, US-GRP-006 | 6 |
| U16 Reporting & Notification | UC-RPT-01..03, UC-OPS-01 | US-RPT-001..003, US-NTF-001 | 4 |
| **Tổng đang triển khai** | **77 UC** | **49 story** | **49** |

Các dải `01..12` bao gồm cả hai đầu. U03 không có UC/story trực tiếp vì cung cấp FileArtifactService cho các luồng upload/download, Draw.io và worker của các unit khác; trách nhiệm bảo mật và hợp đồng của U03 được kiểm tại gate G1.

## 3. Quyết định phân chia các luồng giao nhau

| Trường hợp | Unit chủ trì | Unit cung cấp contract |
|---|---|---|
| Tạo đề thủ công, duyệt và phát hành lớp | U08 | U04 scope, U06 bank, U09 cấu hình kiểu câu hỏi |
| Cấu hình quiz/essay/tài liệu (không có đề chung cấp môn) | U09 | U08 aggregate/publication, U06 rubric/question versions |
| AI tạo bản nháp đề | U13 | U05 nguồn học liệu, U06 bank; U08 duyệt/lưu/publish |
| Thay đổi assignment đã giao | U08 | Không sửa version đang giao; ngưng giao/đóng rồi sửa tạo version mới. U10 giữ template/copy/simulation policy và diff version |
| Soạn bài tài liệu (DOCUMENT) có sơ đồ Draw.io | U09 sở hữu mô hình tài liệu, khung, nhập/xuất DOCX và quy tắc kiểm XML; U11 xác nhận DOCX của người học vào lượt đang làm | U03 giữ ảnh; U08 aggregate/publication; U11 sở hữu bản nháp/lượt |
| Soạn Code Lab và nộp bài tài liệu/Code Lab | U13 sở hữu Code Lab authoring và CodeExecution; U11 sở hữu attempt/submission | U03 giữ artifact; U08 publication; U09 mô hình tài liệu |
| Bài nhóm | U12 nhóm và trưởng nhóm; U14 tài liệu nhóm, nhận mục, ghép realtime, nộp | U09 mô hình tài liệu; U15 lưu grade cuối |
| Trang lớp và học liệu | U04 kiểm quyền và trả lớp/nội dung | U04 enrollment, U05 content |
| Dashboard kết quả cá nhân | U16 tổng hợp bài sắp hạn, trạng thái nộp và điểm đã công bố của người học | U04 enrollment/cờ phân bố, U08 assignment, U11/U14 submission, U15 grade |
| Xuất bảng điểm CSV/XLSX | U16 kiểm quyền và tạo tệp theo yêu cầu | U04 phạm vi lớp, U15 điểm cuối/sổ điểm, U11/U14 trạng thái nộp |
| Chấm bài nhóm | U15 chấm tài liệu nhóm (tay), phần đóng góp (tay/AI), điểm cuối từng người | U14 cung cấp bản nộp và tác giả từng mục; U13 chỉ đề xuất khi giảng viên chọn |

## 4. Learning Access trong U04 và chức năng đã loại

- `UC-LRN-01`, `UC-LRN-02` và `UC-CNT-04` nay thuộc U04 nhưng phần mô tả cũ về tiến độ trong UC-LRN-01/02 không còn hiệu lực. U04 kiểm enrollment rồi trả lớp, bài học đã phát hành và dữ liệu dashboard không nhạy cảm trong phạm vi.
- Các use case và story về lưu/xem tiến độ hoàn thành nội dung đã bị xóa khỏi danh mục; U04 không lưu trạng thái hoàn thành bài học.
- Không có learning path trong plan hiện hành. U04 không tạo lộ trình, prerequisite, completion record hay `learning_progress`. Trạng thái nộp bài và job vẫn được U11/U16/U02 quản lý theo nghiệp vụ riêng.

## 5. Coverage assertions

- U01-U16 bao phủ đúng 77 UC trong phạm vi, mỗi UC một primary unit; U03 có 0 UC vì là hạ tầng.
- Mọi story trong `stories.md` xuất hiện một lần ở bảng unit; catalog chỉ chứa 49 story MVP.
- `UC-ASM-15` (nhân bản, version sau khi ngưng giao, diff) thuộc MVP ở U08/U10; khóa nội dung bài đã phát hành là invariant của U08.
- Không đưa quản lý học kỳ/nhân bản lớp hoặc phân công chấm chéo vào MVP; hai chức năng này không có story trong catalog hiện hành.
