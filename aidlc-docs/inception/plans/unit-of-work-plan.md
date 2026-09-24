# Unit of Work Plan

> Bản hiệu lực là bộ 16 unit hiện tại trong `application-design/unit-of-work.md`, `unit-of-work-dependency.md` và `unit-of-work-story-map.md`. Các revision 17 unit phía dưới đã được thay thế bởi commit `docs: Rework application design into 16-unit plan`.

## Mục tiêu

Phân rã modular monolith thành các đơn vị lập kế hoạch/triển khai có ranh giới rõ, dependency order hợp lý và truy vết đủ 59 user story. Các module vẫn nằm trong một backend Spring Boot deployable; “unit of work” dùng để tổ chức thiết kế và phát triển tuần tự.

## Các bước Part 1 - Planning

- [x] Xác định mức độ gom nhóm story và số unit
- [x] Xác định dependency/shared-kernel giữa các unit
- [x] Xác định thứ tự triển khai và khả năng làm song song
- [x] Xác định ownership phù hợp quy mô đội ngũ
- [x] Xác định module có nhu cầu scale hoặc worker riêng
- [x] Chốt code organization cho greenfield modular monolith
- [x] Phê duyệt kế hoạch trước khi generation

## Các bước Part 2 - Generation

- [x] Sinh `unit-of-work.md` với định nghĩa, trách nhiệm và code organization
- [x] Sinh `unit-of-work-dependency.md` với dependency matrix và critical path
- [x] Sinh `unit-of-work-story-map.md` ánh xạ đủ 59 story
- [x] Validate ranh giới và dependency
- [x] Validate mọi story được gán đúng unit
- [x] Cập nhật trạng thái và trình checkpoint Units Generation

## Câu hỏi phân rã

### Question 1 - Mức độ phân chia unit

Trong một backend Spring Boot modular monolith, bạn muốn chia kế hoạch phát triển thế nào?

A) Một unit duy nhất chứa toàn bộ ứng dụng, bên trong có các module domain (đúng nghĩa deployable unit tối giản)

B) Nhiều unit logic theo nhóm domain nhưng cuối cùng đóng gói trong cùng một backend (Khuyến nghị cho dự án này)

C) Mỗi module domain là một unit riêng, dù vẫn deploy cùng nhau

X) Other (mô tả sau `[Answer]:`)

[Answer]: b

### Question 2 - Shared foundation

Identity, authorization, audit, file và job platform nên được tổ chức thế nào trong dependency graph?

A) Một Foundation unit triển khai trước, cung cấp contract dùng chung có kiểm soát (Khuyến nghị)

B) Mỗi unit tự triển khai các concern dùng chung

C) Chỉ Identity/Authorization là foundation; file, job và audit nằm trong unit sử dụng đầu tiên

X) Other (mô tả sau `[Answer]:`)

[Answer]: A

### Question 3 - Thứ tự ưu tiên nghiệp vụ

Sau Foundation, luồng nào nên được ưu tiên hoàn chỉnh trước?

A) Môn/lớp/nội dung → bài đánh giá/nộp/chấm → AI/nhóm/payment (Khuyến nghị)

B) AI authoring và grading trước, sau đó mới xây quản lý lớp

C) Payment/entitlement trước, sau đó mới xây hành trình học

X) Other (mô tả sau `[Answer]:`)

[Answer]: a

### Question 4 - Quy mô đội ngũ

Kế hoạch unit nên giả định mô hình phát triển nào?

A) Một người/nhóm nhỏ làm tuần tự, ưu tiên dependency order và giảm context switching (Khuyến nghị)

B) Nhiều nhóm cùng phát triển song song theo domain

C) Chưa xác định đội ngũ; tối ưu contract để hỗ trợ cả tuần tự và song song

X) Other (mô tả sau `[Answer]:`)

[Answer]: c, nhóm 5 người có thể làm tuần tự lẫn song song tùy vào module

### Question 5 - Worker và khả năng scale

Các tác vụ RAG, AI, Code Lab, notification và export nên nằm ở đâu?

A) Một Worker unit/process dùng chung cho MVP, handler tách theo module (Khuyến nghị)

B) Mỗi domain có worker process riêng ngay từ đầu

C) Chạy worker trong cùng Spring Boot process, không có process riêng

X) Other (mô tả sau `[Answer]:`)

[Answer]: a

### Question 6 - Tổ chức source code

Bạn muốn cấu trúc repository greenfield theo kiểu nào?

A) Monorepo: `/frontend`, `/backend`, `/worker`, `/infra`, contract OpenAPI dùng chung (Khuyến nghị)

B) Chỉ `/frontend` và `/backend`; worker nằm trong backend

C) Tách nhiều repository độc lập

X) Other (mô tả sau `[Answer]:`)

[Answer]: b

## Phân tích câu trả lời

- Q1, Q2 và Q3 hợp lệ: hệ thống dùng nhiều unit logic theo domain trong một modular monolith; Foundation được triển khai trước; luồng môn/lớp/nội dung được ưu tiên trước đánh giá, AI, nhóm và payment.
- Q4 được làm rõ bằng Clarification 1A: nhóm 5 người triển khai theo các wave phụ thuộc; các unit trong cùng wave có thể làm song song và phải qua integration gate trước wave tiếp theo.
- Q5 và Q6 được làm rõ bằng Clarification 2C: worker dùng chung được tách thành project/process/container riêng. Quyết định này thay thế lựa chọn Q6B ban đầu; cấu trúc hiệu lực là monorepo `/frontend`, `/backend`, `/worker`, `/infra` với contract được version hóa.
- Không còn câu trả lời trống, mơ hồ hoặc mâu thuẫn. Kế hoạch sẵn sàng cho checkpoint phê duyệt trước khi generation.

## Câu hỏi làm rõ

### Clarification Question 1 - Quy tắc làm việc song song

Nhóm 5 người sẽ áp dụng quy tắc nào để quyết định unit được triển khai tuần tự hay song song?

A) Làm theo các wave phụ thuộc: Foundation hoàn thành trước; các unit không phụ thuộc trực tiếp trong cùng wave có thể làm song song; cuối mỗi wave có integration gate (Khuyến nghị)

B) Cố định người phụ trách theo unit và cho phép làm song song ngay sau khi contract giữa các unit được chốt

C) Chủ yếu làm tuần tự theo unit; chỉ chia frontend/backend/test để làm song song bên trong cùng một unit

X) Other (mô tả sau `[Answer]:`)

[Answer]: a

### Clarification Question 2 - Vị trí và cách chạy worker

Với lựa chọn worker dùng chung ở Q5 và cấu trúc không có thư mục `/worker` riêng ở Q6, worker nên được tổ chức và chạy thế nào?

A) Mã worker nằm trong `/backend`, dùng chung domain/application contracts, nhưng chạy thành process/container riêng với API (Khuyến nghị)

B) Mã worker nằm trong `/backend` và chạy ngay trong cùng Spring Boot process với REST API

C) Tạo project `/worker` riêng và chạy thành process/container riêng

X) Other (mô tả sau `[Answer]:`)

[Answer]: c

## Hướng dẫn trả lời

Tất cả câu hỏi đã được trả lời và kiểm tra. Part 2 chỉ bắt đầu sau khi Unit of Work Plan được phê duyệt rõ ràng.

## Revision 2026-09-22

- [x] Giữ tám unit hiện có; không tạo unit mới vì boundary vẫn phù hợp.
- [x] Mở rộng U03 cho YouTube transcript RAG và question/rubric version.
- [x] Mở rộng U04 cho template/copy lineage, simulation policy và attempt snapshot contract.
- [x] Thay U05 leader-upload DOCX bằng group composite generation/version/finalization.
- [x] Mở rộng U06 cho manual composite grade và manual per-member final grade.
- [x] Cập nhật U07 audit/reporting events cho version, copy, simulation và grade override.
- [x] Cập nhật dependency contracts, worker handlers, waves và integration gates.
- [x] Ánh xạ đủ 59/59 stories đúng một primary unit.
- [ ] Trình checkpoint phê duyệt lại Units Generation.

## Revision 2026-09-24 sau Application Design được duyệt

- [x] Giữ tám unit logic và cấu trúc backend/worker đã được duyệt trước đây.
- [x] Đưa AI Orchestration vào U03 để U04 tạo đề và U06 chấm đề xuất qua public contract mà không tạo cycle.
- [x] Làm rõ U01 sở hữu FileArtifactService và JobService; U06 sở hữu CodeExecutionService.
- [x] Cập nhật U04 cho luồng tạo đề chính và quy tắc sửa đề đã giao tăng assignment version.
- [x] Bỏ tiến độ bài học khỏi U03 và tách hai story đã loại khỏi bảng phân công triển khai.
- [x] Đồng bộ dependency matrix, worker ownership, waves và integration gates.
- [x] Kiểm tra 57 story còn hiệu lực được gán đúng một unit; hai story bị loại có ghi lý do.
- [ ] Trình checkpoint phê duyệt lại Units Generation trước khi tiếp tục Construction.

## Revision cũ: 17 unit theo hai ảnh tham chiếu (đã thay thế)

- [x] Đối chiếu danh sách 17 unit và sơ đồ wave với catalog UC/story hiện hành; không dùng các số UC trong ảnh khi khác catalog.
- [x] Kiểm tra riêng U14 với yêu cầu Learning hiện hành: không thêm learning path hay tiến độ bài học.
- [x] Viết lại `unit-of-work.md` thành 17 unit logic, giữ một backend modular monolith và worker riêng.
- [x] Viết lại `unit-of-work-dependency.md` theo các wave/contract có hướng, tránh vòng phụ thuộc.
- [x] Viết lại `unit-of-work-story-map.md`, gán 57 story còn hiệu lực đúng một lần; ghi hai story bị loại và UC liên quan.
- [x] Kiểm tra nhất quán với Application Design, cú pháp bảng/diagram và trạng thái workflow.
- [ ] Trình checkpoint phê duyệt bản Unit of Work mới.

## Revision cũ: đưa Learning Access lên U08 và giới hạn 5 unit mỗi wave (wave đã được thay thế)

- [x] Đổi ID U14 Learning Access & Dashboard thành U08; dịch U08-U13 cũ thành U09-U14 và đồng bộ story/UC map.
- [x] Hoán vị hàng/cột ma trận dependency, kiểm tra DAG không có hard cycle.
- [x] Chia lại wave, mỗi wave tối đa 5 unit; nêu thứ tự phụ thuộc bên trong wave và capacity cho nhóm 5 người.
- [x] Lập đường phụ thuộc chính cho học liệu, bài cá nhân, bài nhóm và AI; đồng bộ gate/state.
- [x] Kiểm tra coverage 87 UC/57 story và cú pháp tài liệu.
- [ ] Trình checkpoint phê duyệt revision mới.

## Revision cũ: biểu đồ dependency Mermaid (bố cục wave đã được thay thế)

- [x] Thêm graph bốn wave vào `unit-of-work-dependency.md`, dùng ID hiện hành với Learning ở U08.
- [x] Hiển thị các cạnh `H` đã rút gọn theo bắc cầu, hai contract `C` của U14 và một event `E` đại diện vào U17; giữ ma trận làm nguồn đầy đủ.
- [x] Kiểm tra cú pháp cấu trúc Mermaid, 17 node, số unit mỗi wave 3/5/5/4 và các cạnh khớp ma trận; sửa hàng U08 thiếu cột U17.
- [ ] Trình checkpoint phê duyệt revision mới.

## Revision cũ: các unit cùng wave chạy song song (cách hiểu wave đã được thay thế)

- [x] Giữ ranh giới 17 unit và ma trận dependency; không dùng contract giả để che cạnh `H` trong cùng wave.
- [x] Tính wave sớm nhất theo DAG `H` và đặt U17 sau nguồn event muộn nhất U16.
- [x] Cập nhật `unit-of-work.md` và Mermaid/dependency path thành 11 wave; mỗi wave tối đa 5 unit, không có cạnh `H`/`E` nội bộ.
- [x] Đồng bộ gate và state; kiểm tra wave/ma trận/story map.
- [ ] Trình checkpoint phê duyệt revision mới.

## Revision cũ: wave là checkpoint, nhánh chạy khi dependency sẵn sàng (đã thay thế)

- [x] Dùng bốn wave 4/5/5/3 unit theo sơ đồ tham chiếu, giữ ID hiện hành với Learning ở U08.
- [x] Cho phép cạnh `H` trong cùng wave và cho phép nhánh ở wave sau mở trước khi toàn bộ wave trước hoàn tất, miễn provider trực tiếp đã sẵn sàng.
- [x] Giới hạn tối đa năm unit đang triển khai đồng thời trên toàn bộ wave; cập nhật bảng và Mermaid.
- [x] Đồng bộ integration gate, state và mô tả scheduler; giữ ma trận dependency và story map.
- [ ] Trình checkpoint phê duyệt revision mới.

## Revision cũ: quan hệ U01/U02 trong plan 17 unit (đã thay thế)

- [x] Đối chiếu AuditService/JobService với AuthorizationService; xác định core audit/job/outbox không cần chờ implementation U01.
- [x] Đổi U02 đọc U01 từ `H` sang `C` cho `queryAudit`/`getJobStatus`, yêu cầu fail closed trước khi phát hành read API.
- [x] Cho U01 và U02 khởi động song song, giữ U03/U04 phụ thuộc `H` vào cả hai; đồng bộ Mermaid và critical path.
- [x] Kiểm tra ma trận 17x17, graph và wave vẫn tối đa năm unit.
- [ ] Trình checkpoint phê duyệt revision mới.

## Revision hiệu lực: 16 unit và bắt đầu Construction

- [x] Chọn bộ Application Design 16 unit hiện tại làm nguồn boundary; Learning Access được gộp vào U04.
- [x] Rà soát dependency/story map 16 unit; sửa mô tả Learning Access thành capability nội bộ U04, không phải self-dependency.
- [x] Đồng bộ requirements, stories và use cases để tiến độ bài học nằm ngoài phạm vi, trong khi trạng thái bài nộp/điểm vẫn được giữ.
- [x] Ghi nhận yêu cầu "review lại doc và giúp tôi triển khai construction phase" là chỉ dẫn bắt đầu Construction theo bộ 16 unit sau khi review.
- [x] Bắt đầu Functional Design U01 Account & Access; tạo kế hoạch và câu hỏi riêng, chưa sinh thiết kế chi tiết khi còn câu hỏi chưa trả lời.
