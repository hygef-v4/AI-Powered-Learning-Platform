# Bảng điểm dạng ma trận và xuất Excel

Người dùng yêu cầu sửa cấu trúc theo file Done-FA26 - MLN131 - SE1925-JS(IT).xlsx: hàng là sinh viên, cột là assignment; nút Bảng điểm mở nơi xem từng bài/toàn bộ và xuất bảng điểm. Triển khai inline, không subagent/commit/push/deploy.

File mẫu được đọc cục bộ, không chỉnh sửa: sinh viên theo hàng, đầu điểm CC/A1/A2/PT theo cột, không có công thức tổng kết. Hệ thống hiện cung cấp họ tên/email, không mã sinh viên; dùng STT, Họ và tên, Email, tên assignment. Không thêm nhóm cố định cho toàn lớp, trọng số hay điểm trung bình.

- [x] Tái sử dụng trang lớp với URL gradebookView=matrix; nút Bảng điểm thay Xuất bảng điểm ở màn chấm. Giữ màn chấm và lựa chọn hàng loạt. Header trang đổi theo chế độ, có Quay lại chấm điểm.
- [x] Ma trận: một hàng/sinh viên, cột theo assignmentId; lọc một bài/toàn bộ, tìm sinh viên,20 hàng/trang; tiêu đề cột có điểm tối đa, điểm0 giữ nguyên, điểm chưa lưu/chưa nộp trống. Bài nhóm dùng điểm cá nhân đã tính/override từ gradebook API. Phân biệt nháp/công bố qua nhãn truy cập và có link xem chi tiết.
- [x] Xuất XLSX/CSV dùng cùng ma trận, gồm mọi sinh viên trong phạm vi bài đã chọn, không chỉ trang/tìm kiếm hiện tại. Giữ kiểm tra quyền đọc/xuất, receipt/member override, trạng thái lưu, audit và chống công thức CSV/XML. Giữ tên file theo assignment khi chọn một bài. XLSX số điểm là số, cố định header/cột định danh và độ rộng cột phù hợp.
- [x] RED/GREEN cho vị trí cột, thiếu điểm/0, filter/export/pagination, nhiều sinh viên, nhóm và quyền hiện có. Typecheck/lint/build, test backend export/grading liên quan. UI đã kiểm trực tiếp; workbook mới đã đọc lại bằng openpyxl. Download live vẫn cấu trúc cũ do backend chưa nạp mã; không restart trái lệnh chặn trước, giới hạn ghi rõ trong evidence.
- [x] Cập nhật spec/evidence/state/audit, không tự commit/push.

Giữ Security/Resiliency và phạm vi phân quyền đã duyệt; PBT tắt. Không schema/dependency mới. Không nhập điểm/dữ liệu sinh viên từ file mẫu.

Kết quả: [kiểm tra và giới hạn](../../build-and-test/2026-11-11-gradebook-matrix.md). Frontend970 test/full backend2.153 test qua; test bổ sung cuối19/19. Chưa xác nhận download live với backend mới.
