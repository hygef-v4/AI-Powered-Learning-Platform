# U07 Payment & AI Credit - Frontend Components

```
components/credits/CreditBalanceBadge     số dư trên thanh điều hướng (cả bốn vai trò)
app/credits/                              CreditsPage (màn AI Credits; Student, Teacher, Subject Manager, Admin)
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
