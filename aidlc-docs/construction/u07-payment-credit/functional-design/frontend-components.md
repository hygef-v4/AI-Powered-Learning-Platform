# U07 Payment & AI Credit - Frontend Components

```
components/credits/CreditBalanceBadge     số dư trên thanh điều hướng (chỉ vai trò có credit)
app/credits/                              CreditsPage (giảng viên, Chủ nhiệm môn, admin)
  PackageList                             thẻ gói, nút Mua (chỉ vai trò mua)
  PaymentHistoryTable                     giao dịch của chính mình nếu có
  LedgerTable                             lịch sử cộng/trừ
app/credits/result/                       PaymentResultPage (return/cancel URL)
app/admin/credits/
  PackageAdminPage                        tạo/sửa/ẩn gói, mức tặng tháng
```

| Component | Hành vi | API |
|---|---|---|
| `CreditBalanceBadge` | Chỉ Giảng viên, Chủ nhiệm môn và Quản trị viên xem số dư; không hiển thị cho Người học | `GET /api/v1/me/credits` |
| `PackageList` | Chỉ `INSTRUCTOR`, `SUBJECT_MANAGER`, `ADMIN`: bấm Mua tạo `Idempotency-Key`, chuyển sang `checkoutUrl`; frontend ẩn với `LEARNER`, backend kiểm quyền cả hai API | `GET /api/v1/credit-packages`, `POST /api/v1/payments` |
| `PaymentResultPage` | "Đang xác nhận thanh toán…", poll tới `PAID` hoặc hết 2 phút; không tự báo thành công khi chưa `PAID` | `GET /api/v1/payments/{orderCode}` |
| `PaymentHistoryTable`, `LedgerTable` | Chỉ vai trò có credit xem lịch sử giao dịch và sổ cái của chính mình, phân trang 20 | `GET /api/v1/me/payments`, `GET /api/v1/me/credit-ledger` |
| `PackageAdminPage` | Giá ≥ 2 000đ, credit > 0 | `/api/v1/admin/credit-packages`, `PUT /api/v1/admin/credit-settings` |

Webhook: `POST /api/v1/payments/payos/webhook` (không cần đăng nhập, kiểm chữ ký).
