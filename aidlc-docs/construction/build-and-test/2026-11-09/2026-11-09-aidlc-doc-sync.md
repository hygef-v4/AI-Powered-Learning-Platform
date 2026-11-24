# Đối chiếu AI-DLC với thay đổi 2026-11-09

Phạm vi là working tree chưa commit: Excel import, Bật/Tắt học liệu, mô tả định dạng, soạn quiz tại chỗ, nguồn AI và sửa HTTP source. Báo cáo/code-summary có ngày và audit cũ giữ nguyên bằng chứng lịch sử; delta mới là phần bổ sung hiện trạng.

| Nhóm | Tài liệu đã đồng bộ thêm | Khoảng cách tìm thấy |
|---|---|---|
| U01/U05 Excel | Domain import U01, rule ghi danh U05, as-built hai unit | Entity/API còn nói CSV; rule tải danh sách chưa nói Excel chuyển về text API |
| U10 quiz | Domain, BR-U10-40, as-built | Thiếu instructionsDocument/provenance warnings; rule vẫn yêu cầu popup chọn nguồn |
| U13 học liệu | Domain, frontend, as-built, logical components | Thiếu enabled/document; nhãn còn Enabled/Disabled; chưa phân biệt material JDBC với module JPA |
| U03/U11 | Contract download owner policy; frontend practice | Chưa ghi kiểm lại token tải và hiển thị hướng dẫn định dạng cho sinh viên |
| U06 vận hành | Deployment architecture | Cần phân biệt restart API/worker cũ và lỗi provider503 với lỗi source |
| Toàn dự án | Requirements, acceptance stories, current-code-baseline, persistence policy, state/audit | Truy vết hiện trạng còn dừng ở snapshot cũ; không được khẳng định toàn U13 đã JDBC |

Những tài liệu frontend/rules U01/U05/U10/U13 đã được sửa trong các lượt triển khai trước được giữ lại. Không sửa hàng loạt các đề cập CSV/popup/JPA trong báo cáo test lịch sử hoặc tính năng khác.

## Bằng chứng và giới hạn

- [Excel](2026-11-09-excel-import-results.md): giới hạn, templates, compatibility và kết quả tự động theo đúng scope.
- [Materials/quiz](2026-11-09-material-quiz-ui-results.md): migration2200/2201, security, format và inline authoring; không cộng các tập test chồng nhau.
- [HTTP source](2026-11-09-quiz-http-auto-source-fix.md): red/green, backend50 và frontend2; JAR đã build.
- [Runtime worker](2026-11-09-ai-worker-runtime-diagnosis.md): đọc task lỗi thực tế, dừng worker cũ; không có yêu cầu AI mới xác nhận phục hồi.

Ở lượt sync này chỉ sửa Markdown; kiểm liên kết cục bộ mới và diff whitespace, không chạy lại application tests. Không tạo migration/code/config mới, không commit/deploy và không biến test kết nối Đạt thành sinh quiz thành công. Security/Resiliency giữ phạm vi đã duyệt, PBT disabled; đây không phải audit bảo mật toàn hệ thống.
