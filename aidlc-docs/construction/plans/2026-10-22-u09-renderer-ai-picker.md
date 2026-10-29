# U09 — renderer và AiSourcePicker, 2026-10-22

Phạm vi người dùng đã yêu cầu triển khai: sửa lỗi hiển thị U09 và nối AiSourcePicker U06 cho đơn/lô, dùng Clean Code, giữ nguyên contract; U15 deferred. Thực hiện inline trên checkout grading-gradebook sạch. Giữ endpoint/DTO, requestRef, version, authorization, transaction, ảnh/token và SVG sanitization. Không thay provider U06/U03/backend hoặc tự lưu/chốt/công bố điểm khi dùng AI.

- [x] Đọc consumer, picker U06, renderer dùng chung và test hiện hữu; xác nhận nguyên nhân metadata bị duyệt như text.
- [x] Thêm regression cho rich text/list/table thực tế và luồng chọn nguồn đơn/lô; xác nhận RED trên production chưa sửa (5 test mới thất bại, 4 test cũ đạt).
- [x] Sửa renderer bằng whitelist nội dung và shared RunsView; bảo toàn dữ liệu legacy, ảnh và SVG.
- [x] Nối picker thật cho đơn/lô; kiểm nguồn không khả dụng, cancel, pending, lỗi và đúng payload. Focused GREEN sau sửa mock ApiError dùng export thật.
- [x] Chạy focused/full frontend tests, lint/typecheck/build, review diff contract và ghi handoff/audit. Full446/78 files đạt; typecheck lại sau khôi phục tsconfig đạt; không sửa API/DTO/backend/shared provider/U15. Không chạy AI/MCP thật hoặc browser E2E.

Đây là sửa lỗi và nối consumer đã được người dùng chỉ định sau integration review; không mở lại phê duyệt tính năng hoặc triển khai U15.
