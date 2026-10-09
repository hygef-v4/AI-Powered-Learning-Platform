# U13 AI & Code Execution - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: không primary UC; phần chạy AI và Judge0 của UC 25, 29, 38, 43, nhóm AI của Settings UC 70–71, luồng phụ AI soạn đề (UC 35, 42–45, 57) và tóm tắt/embedding học liệu (UC 34, 55) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Gọi Gemini | REST `generateContent` bằng Spring `RestClient`, `responseMimeType: application/json` + `responseSchema` | Không phụ thuộc SDK; JSON có cấu trúc |
| Kiểm JSON | `networknt/json-schema-validator` | Nhẹ |
| Cấu hình AI | `SettingDefinition` nhóm AI, đọc qua `SettingsPort` của U03 (bảng `system_settings`, cache 30 s) | Admin sửa trên Settings, không cần bảng hay màn riêng |
| Sandbox | Judge0 CE 1.13.1 (server, workers, postgres, redis riêng) như `demo_do_an`, gửi nhiều file bằng `additional_files` (zip base64) | Đã chạy được trong demo |
| Ngôn ngữ Judge0 | Java (OpenJDK), Python 3, C (GCC), C++ (GCC), JavaScript (Node.js), Dart, C# (Mono); ánh xạ `language_id` trong cấu hình, kiểm bằng `/languages` khi khởi động | Đúng danh sách đã chọn |
| Băm nội dung câu `CODE` | SHA-256 trên JSON chuẩn hóa (khóa sắp xếp) | Ổn định giữa các lần đọc |
| Soạn code | Monaco Editor (`@monaco-editor/react`) | Miễn phí, tô màu 7 ngôn ngữ |
| Giới hạn tần suất | Bucket4j + Redis | Đã có |
