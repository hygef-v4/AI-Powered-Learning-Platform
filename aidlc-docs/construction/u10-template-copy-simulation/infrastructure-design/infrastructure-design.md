# U10 Template & Copy - Infrastructure Design

**Bản tài liệu 2026-10-08**: UC 53, 54; primary stories: US-ASM-008, US-ASM-009, US-ASM-010. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Template, copy và diff | `backend` |
| Template và lineage trong bảng `assignments` (U08 tạo bảng; cột `subject_id`, `source_assignment_id`, `status`, `config.origin`) | `postgres` |

U10 không chạy trong `worker`, không có queue, Redis key, secret hay dịch vụ ngoài.

## 2. Migration

U10 không có migration: template dùng `assignments` với `subject_id` (index `(subject_id, status)` do U08 tạo).
- Lineage là `source_assignment_id` + `config.origin`, ghi một lần khi tạo bài; người copy và thời điểm nằm trong audit.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |
