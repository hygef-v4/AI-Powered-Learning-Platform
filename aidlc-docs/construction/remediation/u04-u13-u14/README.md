# Hồ sơ sửa và kiểm thử U04, U13, U14

Nhánh: `fix-u04-u13-u14`. Phạm vi: các lỗi phát hiện khi kiểm thử ba unit, dependency U09 chặn luồng công bố điểm và cải thiện nội dung thông báo U14. Đây là hồ sơ bổ sung cho thiết kế unit hiện có.

## Đọc từ đâu

| Mục đích | Tài liệu |
| --- | --- |
| Kế hoạch sửa và phê duyệt phương án A | [Kế hoạch remediation](plans/u04-u13-u14-blackbox-remediation-plan.md) |
| Kế hoạch cải thiện nội dung thông báo | [Kế hoạch notification summary](plans/u14-notification-summary-plan.md) |
| Những gì đã sửa và truy vết business rule | [Mô tả implementation](implementation/u04-u13-u14-remediation-code-summary.md) |
| Danh mục nội dung thông báo và xử lý dữ liệu cũ | [Notification summary](implementation/notification-summary-refinement.md) |
| Kết quả trước sửa: 30 đạt, 3 lỗi, 17 bỏ qua | [Baseline ngày 23/10](verification/u04-u13-u14-blackbox-2026-10-23.md) |
| Kiểm thử tự động, kiểm lại UI và các xác nhận thủ công | [Verification ngày 24/10](verification/u04-u13-u14-remediation-2026-10-24.md) |
| Kiểm thử riêng nội dung thông báo | [Notification verification](verification/u14-notification-summary-2026-10-24.md) |

Kết quả checklist cuối: **U04 13/13, U13 22/22, U14 15/15 đạt**. U14-15 dùng bằng chứng thủ công dời hạn khoảng **5 phút** và xác nhận bài Ngưng giao không gửi nhắc; chưa chờ biến thể **+2 ngày** trên UI. Biến thể +2 ngày có coverage tự động riêng. Các snapshot cũ giữ nguyên giá trị lịch sử, không phải kết quả hiện tại. Kết quả này không thay cho nghiệm thu toàn dự án hoặc Operations.

## Bằng chứng ngoài Git

Theo yêu cầu ngày 24/10, toàn bộ 23 tệp trong thư mục `reports` đã chuyển sang:

`E:/FPTUni/sem9_fa26/test_report/reports`

- `blackbox-2026-10-23/`: ảnh và snapshot trước/sau sửa; kết quả cuối trong `checklist-results-final.json` và `artifact-u14-final.png`.
- `u14-notification-summary-2026-10-24/`: ảnh trước/sau và readback nội dung thông báo.

Đã đối chiếu SHA-256 cho đủ 23 tệp trước/sau chuyển, không ghi đè dữ liệu có sẵn ở thư mục đích. Bằng chứng này nằm trên máy local và không đi cùng Git clone. Log chạy test trong `.tmp` tiếp tục được bỏ qua bởi `.gitignore`.

## Sắp xếp tài liệu và giữ lịch sử

Chỉ gom 7 tài liệu mới của đợt fix này; không di chuyển kế hoạch hoặc thiết kế gốc của từng unit. Ba nhóm hiện tại là `plans`, `implementation`, `verification`; README này là điểm vào chung.

- Hai kế hoạch bổ sung trước đây ở `construction/plans/code-generation-plan` chuyển vào `plans`.
- Ba báo cáo kiểm thử trước đây ở `construction/build-and-test` chuyển vào `verification`.
- `u04-u13-u14-remediation-code-summary.md` và `notification-summary-refinement.md` chuyển vào `implementation`.
- Lịch sử [audit](../../../audit.md) giữ nguyên các đường dẫn tại thời điểm ghi, có mục bổ sung giải thích việc di chuyển. Phê duyệt phương án A đã được ghi trong audit và kế hoạch; file câu hỏi riêng được nhắc ở lịch sử không còn trong working tree khi bắt đầu sắp xếp, không được tái tạo như bằng chứng gốc.
- Các câu “không commit” trong kế hoạch/báo cáo cũ mô tả phạm vi lúc thực hiện. Yêu cầu mới ngày 24/10 cho phép chia commit local; vẫn không push.

## Bàn giao bằng commit local

- [x] Kiểm tra nhánh và phân nhóm thay đổi; giữ nguyên mã ứng dụng đã kiểm thử.
- [x] Chuyển bằng chứng ra ngoài repository và xác minh toàn vẹn.
- [x] Gom tài liệu, cập nhật đường dẫn và giữ audit dạng append-only.
- [x] Commit riêng: CSRF upload U13, cập nhật extraction U13, hướng dẫn U04, sanitization U14, version U09, regression reconnect/reminder U14, nội dung thông báo U14, healthcheck Docker local và tài liệu. Hồ sơ này nằm trong commit tài liệu cuối của đợt bàn giao.
- [x] Kiểm tra nội dung từng nhóm staged, liên kết tài liệu và whitespace; `.tmp` và bằng chứng ngoài Git không được stage. Chỉ tạo commit local, không push.

| Commit mã/test/cấu hình | Nội dung |
| --- | --- |
| `5a67892` | U13: CSRF cho multipart upload |
| `370398c` | U13: cập nhật trạng thái extraction |
| `6d0e9e4` | U04: hướng dẫn bảng lớp chỉ xem |
| `b81ad4d` | U14: làm sạch HTML trong thông báo |
| `b161b6d` | U09: sửa version khi đọc JSON, gỡ blocker U14 |
| `498e654` | U14: regression reconnect và đổi hạn/ngưng giao |
| `88dadeb` | U14: mô tả hành động theo loại thông báo |
| `d630655` | Healthcheck Docker local có sẵn |

Các kết quả kiểm thử đã có: backend verify 1.297 test (1.262 đạt, 35 bỏ qua do cấu hình môi trường); GradingService 7/7; frontend bị tác động 45/45, typecheck/lint/build đạt; notification refinement 75/75 và build/UI local đạt. Việc sắp xếp/commit không sửa thêm mã ứng dụng và không được ghi thành một lần chạy lại toàn bộ test.

Các rule security/resiliency không có thay đổi hành vi trong đợt sắp xếp này (N/A cho thay đổi runtime); các kiểm chứng bảo mật của bản sửa nằm trong báo cáo tương ứng. Property-Based Testing giữ trạng thái disabled theo state.
