# Revision: tóm tắt từ View Material

Yêu cầu: upload chưa tóm tắt; Student, Teacher và Subject Manager bấm nút trên màn xem học liệu để yêu cầu tóm tắt. Đây là revision tài liệu hiện hành, không sinh code hoặc thay phê duyệt cũ.

- [x] Tách upload/trích chữ không AI khỏi yêu cầu tóm tắt trong requirements, stories và application design.
- [x] Đồng bộ U03/U05: nút, API, quyền xem, trạng thái và xử lý nền.
- [x] Đồng bộ U07/U13: Student được tóm tắt học liệu có quyền xem; người bấm chịu credit; một HOLD tại lúc yêu cầu.
- [x] Cập nhật code plans chưa triển khai, state và audit; rà nội dung cũ và liên kết.

Lựa chọn thiết kế giữ từ baseline: một bản tóm tắt dùng chung mỗi lesson; có kết quả thì xem lại không trừ thêm credit. Embedding vẫn tạo từ summary để phục vụ RAG, cùng HOLD của người yêu cầu. Upload chỉ trích chữ/phụ đề, không gọi Gemini hoặc giữ credit. Quyền tóm tắt theo quyền xem, không theo quyền sửa học liệu.

Security: SECURITY-08 áp dụng cho nút/API và actor chịu credit; SECURITY-03/04/05/09/12/15 giữ các ràng buộc hiện có, kiểm runtime N/A cho sửa tài liệu. Resiliency: RESILIENCY-10 giữ timeout/retry hữu hạn và claim fencing; RESILIENCY-04/06 hạ tầng không thay đổi, runtime N/A. Các rule ngoài phạm vi đồ án N/A; Property-Based Testing đã tắt.

Validation: rà các phần hiện hành theo upload/view/credit; 10 kiểm tra contract tài liệu, liên kết local, code fences và preservation bước code-plan [x] đều đạt; git diff --check đạt. Không chạy test ứng dụng vì chỉ sửa tài liệu. UC/story/unit giữ 73/51/15.
