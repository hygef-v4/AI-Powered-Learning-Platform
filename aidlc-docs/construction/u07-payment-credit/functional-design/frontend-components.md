# U07 Payment & AI Credit - Frontend Components

```
components/credits/CreditBalanceBadge     số dư trên thanh điều hướng (tặng + mua)
app/credits/                              CreditsPage
  PackageList                             thẻ gói, nút Mua
  PaymentHistoryTable
  LedgerTable                             lịch sử cộng/trừ
app/credits/result/                       PaymentResultPage (return/cancel URL)
app/admin/credits/
  PackageAdminPage                        tạo/sửa/ẩn gói, mức tặng tháng
  PaymentAdminPage                        lọc, xem chi tiết, Đối soát
  AdjustCreditsDialog                     tài khoản, số credit (+/-), lý do
```

| Component | Hành vi | API |
|---|---|---|
| `CreditBalanceBadge` | Hiện tổng, tooltip tách tặng/mua | `GET /api/v1/me/credits` |
| `PackageList` | Bấm Mua: tạo `Idempotency-Key` phía client, chuyển sang `checkoutUrl` | `GET /api/v1/credit-packages`, `POST /api/v1/payments` |
| `PaymentResultPage` | "Đang xác nhận thanh toán…", poll tới `PAID` hoặc hết 2 phút; không tự báo thành công khi chưa `PAID` | `GET /api/v1/payments/{orderCode}` |
| `PaymentHistoryTable`, `LedgerTable` | Phân trang 20 | `GET /api/v1/me/payments`, `GET /api/v1/me/credit-ledger` |
| `PackageAdminPage` | Giá ≥ 2 000đ, credit > 0 | `/api/v1/admin/credit-packages`, `PUT /api/v1/admin/credit-settings` |
| `PaymentAdminPage` | Nút Đối soát từng giao dịch | `GET /api/v1/admin/payments`, `POST .../{id}/reconcile` |
| `AdjustCreditsDialog` | Lý do bắt buộc ≥ 10 ký tự, xác nhận | `POST /api/v1/admin/credit-adjustments` |

Webhook: `POST /api/v1/payments/payos/webhook` (không cần đăng nhập, kiểm chữ ký).
