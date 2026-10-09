# U07 Payment & AI Credit - Frontend Components

**Bản tài liệu 2026-10-09**: UC 08, 09, 10, 11, 68, 69, 72 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); mục credit tặng định kỳ của Settings (UC 70–71); primary stories: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role | Mở từ |
|---|---|---|---|---|
| My Credit Package | `MyCreditPackagePage` | 08 | Student, Teacher, Subject Manager | Class Dashboard |
| Public Credit Packages | `PublicCreditPackagesPage` | 09 | Student, Teacher, Subject Manager | Class Dashboard |
| Credit Package Checkout | `CreditCheckoutPage` | 10, 11 | Student, Teacher, Subject Manager | Public Credit Packages; PayOS quay về |
| Credit Package List | `CreditPackageListPage` | 68 | Admin | Admin Dashboard |
| Credit Package Detail (popup) | `CreditPackageDialog` | 69 | Admin | Credit Package List |
| Payment History | `PaymentHistoryPage` | 72 | Admin | Admin Dashboard |

Mức tặng định kỳ hiện trên Setting List/Setting Detail của U03 (nhóm Credit), không có màn riêng của U07.

## 2. Cây component

```
components/credits/CreditBalanceBadge    số dư trên thanh điều hướng (Student, Teacher, Subject Manager)
app/credits/                             MyCreditPackagePage      màn My Credit Package
  CreditBalanceSummary                   credit tặng, credit mua, tổng
  PaymentHistoryTable                    lịch sử mua của chính mình
  CreditUsageTable                       lần dùng credit (qua CreditUsagePort của U13)
app/credits/packages/                    PublicCreditPackagesPage màn Public Credit Packages
  PackageCard                            tên, thông tin, credit, giá, nút Mua
app/credits/checkout/                    CreditCheckoutPage       màn Credit Package Checkout (?packageId= trước khi trả, ?orderCode= khi PayOS quay về)
  CheckoutSummary                        gói đã chọn, nút Thanh toán
  PaymentStatusPanel                     đang chờ / thành công / đã hủy / hết hạn / lỗi
app/admin/credit-packages/               CreditPackageListPage    màn Credit Package List
  CreditPackageDialog                    popup Credit Package Detail: thêm/sửa tên, thông tin, giá, credit, trạng thái bán
app/admin/payments/                      PaymentHistoryPage       màn Payment History
  PaymentFilters                         tài khoản, gói, khoảng thời gian, trạng thái
```

Route theo `RoleGuard` của U01: `/credits/*` cho Student, Teacher, Subject Manager; `/admin/*` cho Admin. Backend vẫn kiểm chủ ví và vai trò.

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `CreditBalanceBadge`, `CreditBalanceSummary` | Số dư của chính mình; Admin không có badge | `GET /api/v1/me/credits` |
| `PaymentHistoryTable`, `CreditUsageTable` | Lịch sử mua và lần dùng credit của chính mình, phân trang 20; trước khi có U13 bảng dùng credit trống (UC 08) | `GET /api/v1/me/payments`, `GET /api/v1/me/credit-usage` |
| `PublicCreditPackagesPage`, `PackageCard` | Các gói đang bán; bấm Mua sang Checkout (UC 09) | `GET /api/v1/credit-packages` |
| `CheckoutSummary` | Bấm Thanh toán tạo `Idempotency-Key`, chuyển sang `checkoutUrl` của PayOS; lỗi tạo link hiện "Thử lại" (UC 10) | `POST /api/v1/payments` |
| `PaymentStatusPanel` | Khi PayOS quay về: "Đang xác nhận thanh toán…", tự cập nhật 3 giây một lần tới khi xong hoặc hết 2 phút; không báo thành công khi chưa `PAID`; về qua `cancelUrl` thì gọi hủy (UC 11) | `GET /api/v1/payments/{orderCode}`, `POST /api/v1/payments/{orderCode}:cancel` |
| `CreditPackageListPage` | Danh sách mọi gói kèm trạng thái bán; nút Thêm (UC 68) | `GET /api/v1/admin/credit-packages` |
| `CreditPackageDialog` | Form thêm/sửa kèm `version`; `409` báo tải lại; lỗi hiện theo trường; không có nút xóa (UC 69) | `POST /api/v1/admin/credit-packages`, `PATCH /api/v1/admin/credit-packages/{id}` |
| `PaymentHistoryPage`, `PaymentFilters` | Lọc và phân trang lịch sử toàn nền tảng, chỉ đọc (UC 72) | `GET /api/v1/admin/payments` |

Webhook: `POST /api/v1/payments/payos/webhook` (không cần đăng nhập, kiểm chữ ký). Không có màn Payment Result riêng (bỏ 2026-10-09).
