# Unit of Work Story and Use Case Map

## 1. Quy tắc đối chiếu

Ảnh tham chiếu cung cấp cách tách 17 unit và wave, không thay thế catalog UC/story trong repository. Bảng này dùng mã hiện có trong `use-cases.md` và `stories.md`. Mỗi UC còn hiệu lực có đúng một unit chủ trì; một story có thể hỗ trợ nhiều UC nhưng chỉ có một primary unit cho acceptance criteria. Các unit khác tham gia qua public contract trong `unit-of-work-dependency.md`.

Catalog gốc: 90 UC, 59 story (48 MVP, 11 Phase 2). Điều chỉnh thiết kế đã bỏ 3 UC tiến độ bài học và 2 story tương ứng. Phạm vi triển khai: **87 UC, 57 story (46 MVP, 11 Phase 2)**.

## 2. Coverage theo unit

| Unit | UC chủ trì | Story chủ trì | MVP | Phase 2 | Tổng story |
|---|---|---|---:|---:|---:|
| U01 Account & Access | UC-IAM-01..12 | US-IAM-001..007 | 7 | 0 | 7 |
| U02 Audit, Job & Outbox | UC-OPS-02 | US-AUD-001 | 1 | 0 | 1 |
| U03 File & Artifact | Không có UC trực tiếp | Hạ tầng dùng chung | 0 | 0 | 0 |
| U04 Subject/Class/Enrollment | UC-CAT-01..13 | US-CAT-001..003, US-CAT-005 | 3 | 1 | 4 |
| U05 Content/Material/RAG | UC-CNT-01..03, 05..08 | US-CNT-001..005 | 3 | 2 | 5 |
| U06 Rubric & Question Bank | UC-QBK-01..03 | US-QBK-001..003 | 2 | 1 | 3 |
| U07 Payment & Entitlement | UC-PAY-01..02 | US-PAY-001..003 | 3 | 0 | 3 |
| U08 Learning Access & Dashboard | UC-LRN-01, 02, UC-CNT-04 | US-LRN-001 | 1 | 0 | 1 |
| U09 Assessment Core & Publication | UC-ASM-01, 07 | US-ASM-001 | 1 | 0 | 1 |
| U10 Question Type Authoring | UC-ASM-02, 03, 08 | US-ASM-002, 006, 007 | 3 | 0 | 3 |
| U11 Template, Copy & Simulation | UC-ASM-15..18 | US-ASM-008..011 | 3 | 1 | 4 |
| U12 Attempt & Submission | UC-ASM-09..14 | US-ASM-003 | 1 | 0 | 1 |
| U13 Group & Allocation | UC-GRP-01..05, UC-ASM-06 | US-GRP-001..003 | 3 | 0 | 3 |
| U14 AI & Code Execution | UC-AIG-01..03, UC-ASM-04, 05 | US-AIG-001..003, US-ASM-004, 005 | 5 | 0 | 5 |
| U15 Part Submission & Composite | UC-GRP-06..08 | US-GRP-004..006 | 3 | 0 | 3 |
| U16 Grading | UC-GRD-01..12 | US-GRD-001..008 | 5 | 3 | 8 |
| U17 Reporting & Notification | UC-RPT-01..04, UC-OPS-01 | US-RPT-001..004, US-NTF-001 | 2 | 3 | 5 |
| **Tổng đang triển khai** | **87 UC** | **57 story** | **46** | **11** | **57** |

Các dải `01..12` bao gồm cả hai đầu. U03 không có UC/story trực tiếp vì cung cấp FileArtifactService cho các luồng upload/download, Draw.io và worker của các unit khác; trách nhiệm bảo mật và hợp đồng của U03 được kiểm tại gate G1.

## 3. Quyết định phân chia các luồng giao nhau

| Trường hợp | Unit chủ trì | Unit cung cấp contract |
|---|---|---|
| Tạo đề thủ công, duyệt và phát hành lớp | U09 | U04 scope, U06 bank, U10 cấu hình kiểu câu hỏi |
| Đề chung cấp môn và cấu hình quiz/essay | U10 | U09 aggregate/publication, U06 rubric/question versions |
| AI tạo bản nháp đề | U14 | U05 nguồn học liệu, U06 bank; U09 duyệt/lưu/publish |
| Sửa assignment đã giao | U09 | U11 giữ template/copy/simulation policy; tạo version kế tiếp trên cùng stable key |
| Tạo/nộp bài Draw.io và Code Lab | U14 sở hữu authoring/CodeExecution; U12 sở hữu attempt/submission | U03 giữ artifact full XML/checksum; U09 publication |
| Bài nhóm | U13 nhóm/phân phần; U15 nộp phần và composite | U12 attempt; U16 lưu grade cuối |
| Learning dashboard | U08 kiểm quyền và trả lớp/nội dung; UI ghép assignment/notification | U04 enrollment, U07 entitlement, U05 content, U09/U17 read APIs khi có |
| Chấm bài nhóm | U15 đối chiếu/chốt composite; U16 chấm và công bố điểm | U14 chỉ được đề xuất chấm phần cá nhân khi giảng viên chọn |

## 4. U08 và chức năng đã loại

- `UC-LRN-01`, `UC-LRN-02` và `UC-CNT-04` vẫn thuộc U08 nhưng phần mô tả cũ về tiến độ trong UC-LRN-01/02 không còn hiệu lực. U08 kiểm enrollment + entitlement rồi trả lớp, bài học đã phát hành và dữ liệu dashboard không nhạy cảm trong phạm vi.
- `UC-LRN-03` (lưu/tiếp tục tiến độ), `UC-LRN-04` (xem tiến độ cá nhân), `UC-LRN-05` (giảng viên xem tiến độ lớp), `US-LRN-002` và `US-LRN-003` nằm ngoài phạm vi triển khai.
- Không có learning path trong plan hiện hành. U08 không tạo lộ trình, prerequisite, completion record hay `learning_progress`. Trạng thái nộp bài và job vẫn được U12/U17/U02 quản lý theo nghiệp vụ riêng.

## 5. Coverage assertions

- U01-U17 bao phủ đúng 87 UC còn hiệu lực, mỗi UC một primary unit; U03 có 0 UC vì là hạ tầng.
- Mọi story còn hiệu lực trong `stories.md` xuất hiện một lần ở bảng unit; hai story bị loại được nêu riêng ở mục 4.
- `UC-ASM-15` vẫn là Phase 2 cho clone/retire nâng cao; quy tắc sửa đề đã giao tăng version là invariant MVP của U09.
- Không mở lại `US-CAT-004` hoặc `US-GRD-009` vì chúng không nằm trong catalog được duyệt.
