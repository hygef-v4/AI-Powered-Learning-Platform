# Unit of Work Story and Use Case Map

## 1. Quy tắc đối chiếu

Bản kế hoạch 16 unit và wave không thay thế catalog UC/story trong repository. Phần Learning Access đã được gộp vào U04. Bảng này dùng mã hiện có trong `use-cases.md` và `stories.md`. Mỗi UC còn hiệu lực có đúng một unit chủ trì; một story có thể hỗ trợ nhiều UC nhưng chỉ có một primary unit cho acceptance criteria. Các unit khác tham gia qua public contract trong `unit-of-work-dependency.md`.

Catalog gốc: 90 UC, 59 story (48 MVP, 11 Phase 2). Điều chỉnh thiết kế đã bỏ 3 UC tiến độ bài học và 2 story tương ứng. Phạm vi triển khai: **87 UC, 57 story (46 MVP, 11 Phase 2)**.

## 2. Coverage theo unit

| Unit | UC chủ trì | Story chủ trì | MVP | Phase 2 | Tổng story |
|---|---|---|---:|---:|---:|
| U01 Account & Access | UC-IAM-01..12 | US-IAM-001..007 | 7 | 0 | 7 |
| U02 Audit, Job & Outbox | UC-OPS-02 | US-AUD-001 | 1 | 0 | 1 |
| U03 File & Artifact | Không có UC trực tiếp | Hạ tầng dùng chung | 0 | 0 | 0 |
| U04 Subject/Class/Enrollment & Learning Access | UC-CAT-01..13, UC-LRN-01, 02, UC-CNT-04 | US-CAT-001..003, US-CAT-005, US-LRN-001 | 4 | 1 | 5 |
| U05 Content/Material/RAG | UC-CNT-01..03, 05..08 | US-CNT-001..005 | 3 | 2 | 5 |
| U06 Rubric & Question Bank | UC-QBK-01..03 | US-QBK-001..003 | 2 | 1 | 3 |
| U07 Payment & Entitlement | UC-PAY-01..02 | US-PAY-001..003 | 3 | 0 | 3 |
| U08 Assessment Core & Publication | UC-ASM-01, 07 | US-ASM-001 | 1 | 0 | 1 |
| U09 Question Type Authoring | UC-ASM-02, 03, 04, 08 | US-ASM-002, 004, 006, 007 | 4 | 0 | 4 |
| U10 Template, Copy & Simulation | UC-ASM-15..18 | US-ASM-008..011 | 3 | 1 | 4 |
| U11 Attempt & Submission | UC-ASM-09..14 | US-ASM-003 | 1 | 0 | 1 |
| U12 Group & Allocation | UC-GRP-01..05, UC-ASM-06 | US-GRP-001..003 | 3 | 0 | 3 |
| U13 AI & Code Execution | UC-AIG-01..03, UC-ASM-05 | US-AIG-001..003, US-ASM-005 | 4 | 0 | 4 |
| U14 Part Submission & Composite | UC-GRP-06..08 | US-GRP-004..006 | 3 | 0 | 3 |
| U15 Grading | UC-GRD-01..12 | US-GRD-001..008 | 5 | 3 | 8 |
| U16 Reporting & Notification | UC-RPT-01..04, UC-OPS-01 | US-RPT-001..004, US-NTF-001 | 2 | 3 | 5 |
| **Tổng đang triển khai** | **87 UC** | **57 story** | **46** | **11** | **57** |

Các dải `01..12` bao gồm cả hai đầu. U03 không có UC/story trực tiếp vì cung cấp FileArtifactService cho các luồng upload/download, Draw.io và worker của các unit khác; trách nhiệm bảo mật và hợp đồng của U03 được kiểm tại gate G1.

## 3. Quyết định phân chia các luồng giao nhau

| Trường hợp | Unit chủ trì | Unit cung cấp contract |
|---|---|---|
| Tạo đề thủ công, duyệt và phát hành lớp | U08 | U04 scope, U06 bank, U09 cấu hình kiểu câu hỏi |
| Đề chung cấp môn và cấu hình quiz/essay | U09 | U08 aggregate/publication, U06 rubric/question versions |
| AI tạo bản nháp đề | U13 | U05 nguồn học liệu, U06 bank; U08 duyệt/lưu/publish |
| Sửa assignment đã giao | U08 | U10 giữ template/copy/simulation policy; tạo version kế tiếp trên cùng stable key |
| Soạn bài Draw.io | U09 sở hữu cấu hình loại bài và quy tắc kiểm XML | U03 giữ artifact full XML/checksum; U08 aggregate/publication |
| Soạn Code Lab và nộp bài Draw.io/Code Lab | U13 sở hữu Code Lab authoring và CodeExecution; U11 sở hữu attempt/submission | U03 giữ artifact; U08 publication; U09 cấu hình loại bài Draw.io |
| Bài nhóm | U12 nhóm/phân phần; U14 nộp phần và composite | U11 attempt; U15 lưu grade cuối |
| Learning dashboard | U04 kiểm quyền và trả lớp/nội dung; UI ghép assignment/notification | U04 enrollment, U05 content, U08/U16 read APIs khi có |
| Chấm bài nhóm | U14 đối chiếu/chốt composite; U15 chấm và công bố điểm | U13 chỉ được đề xuất chấm phần cá nhân khi giảng viên chọn |

## 4. Learning Access trong U04 và chức năng đã loại

- `UC-LRN-01`, `UC-LRN-02` và `UC-CNT-04` nay thuộc U04 nhưng phần mô tả cũ về tiến độ trong UC-LRN-01/02 không còn hiệu lực. U04 kiểm enrollment rồi trả lớp, bài học đã phát hành và dữ liệu dashboard không nhạy cảm trong phạm vi.
- `UC-LRN-03` (lưu/tiếp tục tiến độ), `UC-LRN-04` (xem tiến độ cá nhân), `UC-LRN-05` (giảng viên xem tiến độ lớp), `US-LRN-002` và `US-LRN-003` nằm ngoài phạm vi triển khai.
- Không có learning path trong plan hiện hành. U04 không tạo lộ trình, prerequisite, completion record hay `learning_progress`. Trạng thái nộp bài và job vẫn được U11/U16/U02 quản lý theo nghiệp vụ riêng.

## 5. Coverage assertions

- U01-U16 bao phủ đúng 87 UC còn hiệu lực, mỗi UC một primary unit; U03 có 0 UC vì là hạ tầng.
- Mọi story còn hiệu lực trong `stories.md` xuất hiện một lần ở bảng unit; hai story bị loại được nêu riêng ở mục 4.
- `UC-ASM-15` vẫn là Phase 2 cho clone/retire nâng cao; quy tắc sửa đề đã giao tăng version là invariant MVP của U08.
- Không mở lại `US-CAT-004` hoặc `US-GRD-009` vì chúng không nằm trong catalog được duyệt.
