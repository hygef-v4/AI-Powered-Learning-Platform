# U13 AI & Code Execution - Tech Stack Decisions

**Bản tài liệu 2026-10-08**: không primary UC; primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Gọi Gemini | REST `generateContent` bằng Spring `RestClient`, `responseMimeType: application/json` + `responseSchema` | Không phụ thuộc SDK; JSON có cấu trúc |
| Kiểm JSON | `networknt/json-schema-validator` | Nhẹ |
| Sandbox | Judge0 CE 1.13.1 (server, workers, postgres, redis riêng) như `demo_do_an`, gửi nhiều file bằng `additional_files` (zip base64) | Đã chạy được trong demo |
| Ngôn ngữ Judge0 | Java (OpenJDK), Python 3, C (GCC), C++ (GCC), JavaScript (Node.js), Dart, C# (Mono); ánh xạ `language_id` trong cấu hình, kiểm bằng `/languages` khi khởi động | Đúng danh sách đã chọn |
| Soạn code | Monaco Editor (`@monaco-editor/react`) | Miễn phí, tô màu 7 ngôn ngữ |
| Giới hạn tần suất | Bucket4j + Redis | Đã có |
