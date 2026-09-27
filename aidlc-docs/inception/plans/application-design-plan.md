# Application Design Plan

## Mục tiêu

Thiết kế boundary component/service cấp cao cho AI-Powered Learning Platform dựa trên requirements, 49 user story thuộc MVP, persona và 77 use case hiện hành. Thiết kế tập trung vào trách nhiệm, interface, orchestration, dependency và data flow; business rule chi tiết sẽ thực hiện ở Functional Design theo unit.

## Các bước thực hiện

- [x] Phân tích capability và ranh giới component
- [x] Chốt interface/method cấp cao
- [x] Thiết kế service orchestration và xử lý bất đồng bộ
- [x] Lập dependency matrix và data flow
- [x] Sinh `components.md`
- [x] Sinh `component-methods.md`
- [x] Sinh `services.md`
- [x] Sinh `component-dependency.md`
- [x] Sinh `application-design.md` tổng hợp
- [x] Kiểm tra tính nhất quán, security và resiliency

## Câu hỏi quyết định thiết kế

### Question 1 - Boundary triển khai

Bạn muốn tổ chức component theo cách nào ở giai đoạn đầu?

A) Modular monolith: một backend Spring Boot với module/domain boundary rõ ràng (Khuyến nghị)

B) Nhiều microservice độc lập ngay từ đầu

C) Backend theo domain module nhưng có thể tách service sau khi MVP ổn định

X) Other (mô tả sau `[Answer]:`)

[Answer]: a

## Revision 2026-09-22

- [x] Cập nhật Content cho YouTube/caption/transcript ingestion theo bài giảng.
- [x] Cập nhật Question Bank và Assessment cho immutable version/snapshot theo attempt.
- [x] Bổ sung template lineage, cross-class copy và simulation policy.
- [x] Thay leader-upload DOCX bằng composite generation/version và instructor finalization.
- [x] Cập nhật Grading cho manual shared grade, consistency rubric và manual per-student final score.
- [x] Đồng bộ component, methods, services, dependency, flows và screens/jobs; mô hình bảng cuối cùng được chốt theo từng unit ở Construction.
- [x] Kiểm tra Security/Resiliency và content consistency.
- [x] Trình checkpoint phê duyệt lại Application Design.

## Revision 2026-09-24

- [x] Làm rõ không sao chép khóa học/lớp; sửa assignment đã giao tạo version kế tiếp và giữ attempt snapshot cũ.
- [x] Loại tiến độ từng bài học khỏi component, method, màn hình và phân rã unit.
- [x] Mở rộng dependency matrix với Question Bank, Payment và các hàng Academic, AI Orchestration, Code Execution.
- [x] Đặt luồng tạo đề làm orchestration chính; RAG chỉ hỗ trợ nguồn cho AI.
- [x] Thêm CodeExecutionService, FileArtifactService và JobService vào bảng dịch vụ.
- [x] Bỏ tài liệu và kế hoạch business flow riêng.
- [x] Kiểm tra tham chiếu còn sót và cú pháp tài liệu.
- [x] Trình checkpoint phê duyệt bản sửa Application Design; người dùng duyệt ngày 2026-09-24.

### Question 2 - Giao tiếp bất đồng bộ

Các tác vụ AI, RAG ingestion, Code Lab, notification và payment reconciliation nên dùng mô hình nào?

A) Job queue + worker, có trạng thái và retry hữu hạn (Khuyến nghị)

B) Xử lý đồng bộ trong request, chỉ timeout đơn giản

C) Kết hợp: đồng bộ cho thao tác nhanh, queue cho tác vụ vượt ngưỡng thời gian

X) Other (mô tả sau `[Answer]:`)

[Answer]: a

### Question 3 - Lưu trữ file và XML Draw.io

XML đầy đủ, XML rút gọn dẫn xuất, DOCX và tài liệu học liệu nên được tổ chức thế nào?

A) Object storage abstraction + metadata trong relational database; XML đầy đủ immutable, XML rút gọn là artifact dẫn xuất (Khuyến nghị)

B) Lưu trực tiếp mọi file trong relational database

C) Object storage cho file, nhưng không lưu version metadata chi tiết

X) Other (mô tả sau `[Answer]:`)

[Answer]: a

### Question 4 - Tích hợp AI và nhà cung cấp

Ranh giới adapter AI nên được thiết kế thế nào?

A) Provider-neutral port/adapter, có mock/sandbox contract test (Khuyến nghị)

B) Gắn trực tiếp một SDK/provider cụ thể trong domain service

C) Một gateway nội bộ duy nhất, provider cụ thể quyết định ở infrastructure stage

X) Other (mô tả sau `[Answer]:`)

[Answer]: a

### Question 5 - Tách frontend/backend

Frontend Next.js và backend Spring Boot nên phối hợp theo kiểu nào?

A) REST API versioned, OpenAPI contract và server-side authorization là nguồn chuẩn (Khuyến nghị)

B) GraphQL làm API chính

C) REST cho nghiệp vụ, WebSocket/SSE chỉ cho trạng thái job/thông báo realtime

X) Other (mô tả sau `[Answer]:`)

[Answer]: a

## Hướng dẫn trả lời

Điền một lựa chọn hợp lệ sau mỗi `[Answer]:`. Nếu chọn `X`, ghi rõ quyết định ngay sau chữ X. Vui lòng hoàn tất toàn bộ câu hỏi trước khi yêu cầu sinh artifact Application Design.
