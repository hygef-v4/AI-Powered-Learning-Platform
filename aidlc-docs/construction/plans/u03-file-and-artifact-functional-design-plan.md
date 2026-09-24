# U03 File & Artifact - Functional Design Plan

## 1. Phạm vi

- Unit hạ tầng, không có use case hay story riêng. Phục vụ U01 (ảnh đại diện), U05 (học liệu, transcript), U11 (bài Draw.io, file nộp), U13 (XML rút gọn cho AI), U14 (composite nhóm), U16 (file export).
- Sở hữu: bảng `artifacts`, lưu byte trên Google Drive, kiểm loại/kích thước/checksum, validate Draw.io XML chống XXE, link tải tạm, xóa file dẫn xuất.
- Không sở hữu: ý nghĩa nghiệp vụ của file, quyết định ai được xem file (unit sở hữu quyết định).

## 2. Việc cần làm

- [x] Đọc unit-of-work, component-methods, services, ERD `artifacts`.
- [x] Hỏi người dùng qua giao diện, ghi vào `u03-file-and-artifact-functional-design-questions.md`.
- [x] Tạo 4 tài liệu Functional Design.
- [x] Đồng bộ `component-methods.md` (bỏ upload hai bước).
- [x] Trình duyệt Functional Design U03.

## 3. Cần đồng bộ ERD sau

- Bỏ `scan_status`; thêm `status` (`ACTIVE`, `BLOCKED`, `DELETED`) và `deleted_at`.
- Không có bảng `upload_sessions`.
