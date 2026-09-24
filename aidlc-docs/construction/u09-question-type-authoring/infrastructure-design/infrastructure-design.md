# U09 Question Type Authoring - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Cấu hình loại bài, khung, nhập/xuất DOCX, kiểm tài liệu, rút gọn XML | `backend` |
| Bảng `question_type_config`, `document_skeletons` | `postgres` |
| Ảnh trong tài liệu | U03 purpose `DOCUMENT_IMAGE` (Google Drive) |
| Trình vẽ | `https://embed.diagrams.net` (trình duyệt tải trực tiếp, không qua server) |

U09 không chạy trong `worker`, không có queue, Redis key hay secret riêng.

## 2. Nginx

- `/api/v1/assignments/*/skeleton:import-docx`: `client_max_body_size 20m`, `proxy_read_timeout 60s`.
- Xuất DOCX trả stream: `proxy_read_timeout 60s`.

## 3. Bộ nhớ backend

- Nhập DOCX 20 MB và xuất 2 lượt đồng thời cần thêm khoảng 300 MB heap lúc cao điểm; backend giữ giới hạn hiện có, `-Xmx` đặt 75% RAM container.

## 4. Migration

`V20260925_1600__u09_question_type.sql`:
- `question_type_config (assignment_id PK FK assignments, assignment_type, config jsonb, updated_at)`.
- `document_skeletons (id, assignment_id FK NULL, bank_item_id FK NULL, blocks jsonb, source_docx_name, created_by, updated_at)`, `CHECK ((assignment_id IS NULL) <> (bank_item_id IS NULL))`.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | CSP chỉ mở `frame-src` cho `embed.diagrams.net`, `youtube-nocookie` |
| Rule còn lại | N/A | Dùng chung deploy, health, secret của backend |
