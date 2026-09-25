# U03 File & Artifact - Functional Design Plan

## 1. Phạm vi

- Unit hạ tầng, không có use case hay story riêng. Phục vụ U01 (ảnh đại diện), U05 (học liệu), U06/U09/U11/U14 (ảnh trong tài liệu). Caption YouTube thuộc U05; XML Draw.io thuộc U09; U16 xuất bảng điểm trực tiếp, không lưu qua U03.
- Sở hữu: bảng `artifacts`, lưu byte trên Google Drive, kiểm loại/kích thước/checksum và link tải tạm. U09 kiểm XML Draw.io chống XXE; U03 không lưu file dẫn xuất hoặc xóa file nghiệp vụ.
- Không sở hữu: ý nghĩa nghiệp vụ của file, quyết định ai được xem file (unit sở hữu quyết định).

## 2. Việc cần làm

- [x] Đọc unit-of-work, component-methods, services và mô hình dữ liệu `artifacts` của U03.
- [x] Hỏi người dùng qua giao diện, ghi vào `u03-file-and-artifact-functional-design-questions.md`.
- [x] Tạo 4 tài liệu Functional Design.
- [x] Đồng bộ `component-methods.md` (bỏ upload hai bước).
- [x] Trình duyệt Functional Design U03.

## 3. Mô hình dữ liệu đã chốt

- `artifacts` dùng `status` (`ACTIVE`, `BLOCKED`, `DELETED`) và `deleted_at`; không dùng `scan_status`.
- Không có bảng `upload_sessions`.
