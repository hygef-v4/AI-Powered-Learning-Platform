# U02 Audit - NFR Requirements

Việc nền, RabbitMQ và worker chuyển sang U03 (2026-10-04); yêu cầu của chúng nằm ở NFR-U03-40…55.

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-01 | `AuditPort.record` chỉ thêm một câu INSERT vào transaction của unit gọi. | NFR-003, BR-U02-02 |
| NFR-U02-03 | Tra cứu audit p95 ≤ 500 ms với tới 1 triệu bản ghi. Có index theo `occurred_at`, `(actor_id, occurred_at)`, `(object_type, object_id)`, `action`. | NFR-003, BR-U02-08 |

## 2. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-30 | User database của ứng dụng không có quyền `UPDATE`/`DELETE` trên `audit_logs`. | BR-U02-01, SEC-005 |
| NFR-U02-31 | Tra cứu audit chỉ cho `ADMIN`. | BR-U02-07 |

## 3. Khả dụng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-40 | Audit ghi trong PostgreSQL cùng transaction nghiệp vụ nên không phụ thuộc RabbitMQ; ghi audit lỗi thì thao tác rollback. | BR-U02-04 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-50 | Unit test cho mọi `BR-U02-xx`. Integration test với PostgreSQL bằng Testcontainers: audit trùng `id`, rollback vẫn còn audit `DENIED`/`FAILURE`, user `app` không UPDATE/DELETE được `audit_logs`. | NFR-004 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
