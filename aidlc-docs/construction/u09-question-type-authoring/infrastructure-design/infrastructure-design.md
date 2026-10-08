# U09 Question Type Authoring - Infrastructure Design

**Bản tài liệu 2026-10-08**: UC 39, 40, 41, 42, 43; primary stories: US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Cấu hình loại bài, khung, nhập/xuất DOCX, kiểm tài liệu, rút gọn XML | `backend` |
| Cột `config` của `assignments` (U08 tạo bảng); khung câu ngân hàng trong `questions.definition` (U06) | `postgres` |
| Ảnh trong tài liệu | U03 purpose `DOCUMENT_IMAGE` (Google Drive) |
| Trình vẽ | `https://embed.diagrams.net` (trình duyệt tải trực tiếp, không qua server) |

U09 không chạy trong `worker`, không có queue, Redis key hay secret riêng.

## 2. Nginx

- `/api/v1/assignments/*/skeleton:import-docx`: `client_max_body_size 20m`, `proxy_read_timeout 60s`.
- `/api/v1/attempts/*/docx:preview`: cùng giới hạn 20 MB và timeout 60 s; chỉ U11 cho lượt DOCUMENT đang làm.
- Xuất DOCX trả stream: `proxy_read_timeout 60s`.

## 3. Bộ nhớ backend

- Nhập DOCX 20 MB và xuất 2 lượt đồng thời cần thêm khoảng 300 MB heap lúc cao điểm; backend giữ giới hạn hiện có, `-Xmx` đặt 75% RAM container.

## 4. Migration

U09 không có migration: cấu hình loại bài và khung tài liệu nằm trong `assignments.config` (`jsonb`, U08 tạo); U09 ghi qua `AssignmentExtensionPort`.
- `config.parts[]` giữ `rubricId` của từng phần, `config.questionRubrics[]` giữ `rubricId` của từng câu Text Essay (không là khóa ngoại; U09 kiểm khi duyệt).
- Khung của câu `DOCUMENT` trong ngân hàng nằm trong `questions.definition` của U06; U09 chỉ kiểm qua `DocumentModelPort`.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | CSP chỉ mở `frame-src` cho `embed.diagrams.net`, `youtube-nocookie` |
| Rule còn lại | N/A | Dùng chung deploy, health, secret của backend |
