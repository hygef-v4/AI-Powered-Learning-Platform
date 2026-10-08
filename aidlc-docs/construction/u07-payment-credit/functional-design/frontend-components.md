# U07 Payment & AI Credit - Frontend Components

**Bản tài liệu 2026-10-08**: UC 08, 09, 10, 67, 68, 69; primary stories: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
components/credits/CreditBalanceBadge     số dư trên thanh điều hướng (cả bốn vai trò)
app/credits/                              CreditsPage (màn Credit Packages; Student, Teacher, Subject Manager, Admin)
  PackageList                             thẻ gói, nút Mua
  PaymentHistoryTable                     giao dịch của chính mình nếu có
  CreditUsageTable                        lần dùng credit (qua `CreditUsagePort` của U13)
app/credits/result/                       PaymentResultPage (màn Payment Result: return/cancel URL của PayOS)
```

| Component | Hành vi | API |
|---|---|---|
| `CreditBalanceBadge` | Cả bốn vai trò xem số dư của chính mình | `GET /api/v1/me/credits` |
| `PackageList` | Cả bốn vai trò: bấm Mua tạo `Idempotency-Key`, chuyển sang `checkoutUrl`; backend kiểm tài khoản `ACTIVE` và chủ ví | `GET /api/v1/credit-packages`, `POST /api/v1/payments` |
| `PaymentResultPage` | "Đang xác nhận thanh toán…", poll tới `PAID` hoặc hết 2 phút; không tự báo thành công khi chưa `PAID` | `GET /api/v1/payments/{orderCode}` |
| `PaymentHistoryTable`, `CreditUsageTable` | Mỗi người chỉ xem lịch sử mua và lần dùng credit của chính mình, phân trang 20; trước khi có U13 bảng dùng credit trống | `GET /api/v1/me/payments`, `GET /api/v1/me/credit-usage` |

Webhook: `POST /api/v1/payments/payos/webhook` (không cần đăng nhập, kiểm chữ ký).

## Admin screens
CreditPackageSettingPage (màn Credit Package Setting, UC 67–68): danh sách, form add/edit tên/thông tin/giá/credit, version; lỗi 409 tải lại, sai field hiện tại form. GET/POST /api/v1/admin/credit-packages, PATCH /api/v1/admin/credit-packages/{id}. Không xóa gói/đổi tặng tháng.
PaymentHistoryPage (màn Payment History, UC 69): filter account/package/from/to/status, pagination; GET /api/v1/admin/payments, chỉ đọc. Khác bảng /me/payments của chính mình. Route guard ADMIN và backend kiểm lại.
