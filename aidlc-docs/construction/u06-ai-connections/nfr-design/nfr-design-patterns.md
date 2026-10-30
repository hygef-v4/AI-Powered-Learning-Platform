# U06 AI Connections — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Giữ chỗ quota nguyên tử
- Tạo trước dòng cửa sổ còn thiếu (`INSERT ... ON CONFLICT DO NOTHING`), rồi `SELECT ... FOR UPDATE` theo thứ tự ngày toàn hệ thống, ngày người gọi; kiểm `used + reserved + ước tính ≤ mức`; cộng vào `reserved` và lưu mốc cửa sổ trên task.
- Quyết toán trên đúng các cửa sổ đã lưu trên task, kể cả khi đã sang ngày mới.

## P2 - Mốc gửi trước khi gọi provider
- Commit `RUNNING` và `dispatchedAt` trước khi gọi; message đến lại thấy mốc gửi mà chưa có kết quả thì `UNKNOWN`.
- Lỗi trước mốc gửi (database tạm lỗi) cho U03 thử lại; sau mốc gửi không ném lỗi để U03 thử lại.

## P3 - Kết quả qua handler
- Kết quả `READY` lưu trước, gọi handler sau trong transaction riêng; handler lỗi thì lượt thử lại chỉ gọi handler, không gọi provider.

## P4 - Bảo vệ key
- Giải mã ngay trước khi gọi provider, không giữ trong cache hay log; lớp `Masked` cho mọi DTO trả về.
- Đổi khóa mã hóa: thêm khóa mới với `keyVersion` tăng, mã hóa lại dần khi kết nối được dùng hoặc bằng lệnh vận hành.

## P5 - MCP an toàn
- Bộ lọc xác thực bearer riêng cho `/mcp`; actor dựng từ credential, không nhận actor từ tham số.
- Danh sách tool lọc theo role và quyền credential; schema kiểm trước khi gọi unit sở hữu; kết quả cắt theo kích thước.

## P6 - Dọn dữ liệu nhạy cảm
- Xóa `inputPayload`, `resultPayload` khi task kết thúc và đã chuyển cho handler; usage chỉ giữ số liệu.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
