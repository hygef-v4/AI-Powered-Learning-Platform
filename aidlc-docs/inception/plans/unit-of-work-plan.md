# Unit of Work Plan

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
