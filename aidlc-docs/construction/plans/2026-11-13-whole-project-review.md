# Kế hoạch rà soát toàn dự án — 13/11/2026

Yêu cầu: kiểm tra code, kiến trúc, bug, UI/UX và gửi báo cáo. Baseline af245ac3 trên feat/ai-prompt-guardrail, có thay đổi chưa commit. Đánh giá trạng thái thực tế, không lấy test cũ làm kết quả mới.

- [x] Workspace Detection: đọc CLAUDE, conventions, state, quy tắc chung và báo cáo trước.
- [x] Requirements/Workflow Planning: phạm vi review chỉ đọc source, chạy kiểm tra, tạo báo cáo có bằng chứng; dùng lại brownfield context. Không cần stories, thiết kế, code generation, deploy.
- [ ] Kiểm chứng backend/frontend và guard tích hợp.
- [ ] Rà ranh giới kiến trúc, phân quyền, transaction và luồng dữ liệu.
- [ ] Rà UI/UX: điều hướng, biểu mẫu, trạng thái lỗi/lưu, responsive, accessibility.
- [ ] Phân loại phát hiện, xác minh vị trí, ghi giới hạn và bàn giao báo cáo.

Security/Resiliency giữ phạm vi trong aidlc-state; PBT tắt. Không sửa mã ứng dụng trong review. Không thay kết quả lịch sử. Findings được báo cáo, không coi báo cáo là chứng nhận production.
