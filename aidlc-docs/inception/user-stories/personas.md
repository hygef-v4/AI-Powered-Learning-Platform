# Personas - AI-Powered Learning Platform

## 1. Phạm vi

Bộ persona đại diện cho bốn vai trò RBAC của MVP trong một trường học hoặc trung tâm đào tạo. Persona mô tả mục tiêu và hành vi nghiệp vụ; không thay thế đặc tả authorization phía server trong thiết kế.

## 2. P-STUDENT - Người học

### Hồ sơ

- **Bối cảnh**: Học trong một hoặc nhiều lớp đã được ghi danh, chủ yếu dùng web trên máy tính để bàn hoặc laptop; mobile chủ yếu để đọc và xem thông báo/kết quả.
- **Mục tiêu**: Truy cập đúng nội dung, tiếp tục việc học, nộp bài đúng hạn và nhận điểm/phản hồi rõ ràng.
- **Động lực**: Hoàn thành khóa học và hiểu mình cần cải thiện điều gì.
- **Khó khăn**: Dễ mất phương hướng khi nội dung nhiều; cần biết trạng thái bài nộp và lỗi hệ thống mà không phải liên hệ hỗ trợ ngay.
- **Nhu cầu truy cập**: Chỉ dữ liệu, tiến độ, bài nộp, điểm và quyền lợi của chính mình trong các lớp được ghi danh.

### Hành vi điển hình

- Đăng nhập bằng email trường đã được cấp, quản lý hồ sơ và khôi phục mật khẩu; không tự đăng ký tài khoản công khai.
- Sau khi đăng nhập vào Student Menu (danh sách lớp đã ghi danh); mở lớp để xem học liệu đang hiển thị, bài được giao, trạng thái nộp, điểm đã công bố và phân bố điểm ẩn danh khi lớp cho phép.
- Làm bài, nộp bài và xem kết quả sau khi được công bố.
- Làm trắc nghiệm, bài viết, bài tài liệu (có sơ đồ Draw.io nhúng) và Code Lab; lưu nháp, khôi phục và xem lịch sử lần nộp.
- Nhận hoặc được trưởng nhóm giao mục trong tài liệu nhóm, bấm Xong để ghép vào tài liệu chung, trưởng nhóm giao các phần của khung và nộp bất kỳ lúc nào trước hạn; xem điểm và phản hồi do giảng viên quyết định.
- Làm bài `PRACTICE` đúng dạng; Code Lab và Quiz tự chấm theo test/đáp án, Text Essay và Diagram Essay không tự chấm khi nộp; Student bấm chấm với AI khi đủ credit, mỗi attempt tối đa một kết quả.
- Xem ví, mua credit AI và theo dõi lịch sử của mình. Khi thiếu credit, hệ thống báo thiếu; Student mua thêm rồi bấm chấm lại cho attempt đã nộp.
- Nhận thông báo thiết yếu.

### Stories liên quan

`US-IAM-001`, `US-IAM-002`, `US-IAM-003`, `US-IAM-004`, `US-IAM-006`, `US-CAT-005`, `US-GRP-002`, `US-GRP-004`, `US-CNT-004`, `US-LRN-001`, `US-ASM-003`, `US-ASM-012`, `US-GRD-001`, `US-GRD-004`, `US-RPT-001`, `US-PAY-001`, `US-PAY-002`, `US-NTF-001`.

## 3. P-TEACHER - Giảng viên

### Hồ sơ

- **Bối cảnh**: Phụ trách một hoặc nhiều lớp cụ thể, chuẩn bị nội dung riêng, giao bài và theo dõi người học; quyền mặc định không vượt ra ngoài các lớp được phân công.
- **Mục tiêu**: Tổ chức lớp hiệu quả, giảm thời gian soạn/chấm bài bằng AI nhưng giữ quyền quyết định học thuật cuối cùng.
- **Động lực**: Cải thiện chất lượng phản hồi và phát hiện người học cần hỗ trợ.
- **Khó khăn**: Khối lượng nội dung và bài nộp lớn; cần phân biệt rõ tài nguyên cấp môn với nội dung riêng của lớp.
- **Nhu cầu truy cập**: Chỉ lớp, nội dung, người học, bài nộp và kết quả thuộc phân công; không được sửa kho học liệu/RAG cấp môn nếu không đồng thời có quyền Chủ nhiệm môn.

### Hành vi điển hình

- Quản lý vòng đời lớp được phân công (sửa, mở, lưu trữ) và nội dung riêng của lớp.
- Ghi danh người học khi được cấp quyền.
- Chia nhóm trong danh sách sinh viên của lớp (tạo tay hoặc chia ngẫu nhiên), chỉ định trưởng nhóm, soạn khung bài nhóm (tự viết, lấy từ ngân hàng hoặc nhờ AI đề xuất; khung tự chia phần) và tạo rubric từng phần và xử lý yêu cầu đổi trưởng nhóm.
- Xem tiến độ tài liệu nhóm, nhả khóa mục khi cần, chấm tài liệu chung như bài tài liệu và chấm điểm đóng góp từng thành viên.
- Nhờ AI đề xuất cho từng bài hoặc chấm hàng loạt rồi xác nhận từng bài; điểm đóng góp thành viên nhóm mặc định bằng điểm tài liệu chung, chấm tay khi cần.
- Dùng AI tạo bản nháp câu hỏi từ nội dung được phép.
- Mua credit AI để dùng cho các chức năng AI thuộc phạm vi lớp được phân công; theo dõi thanh toán và số dư của mình.
- Quản lý rubric/ngân hàng câu hỏi theo version; copy assignment/rubric giữa các lớp mình phụ trách.
- Soạn, xem trước và phát hành các dạng bài `GRADED`/`PRACTICE` hợp lệ; chỉ bài `GRADED` vào quy trình chấm/chốt điểm của Teacher.
- Duyệt, xuất bản bài riêng của lớp; sau khi nhận bài, chủ động chọn chấm thủ công hoặc nhờ AI đề xuất rồi tự quyết định điểm cuối.
- Xem tiến độ nộp, chốt điểm, xuất bảng điểm của lớp được phân công; hệ thống tự nhắc người chưa nộp trước hạn.

### Stories liên quan

`US-IAM-001`, `US-IAM-002`, `US-IAM-004`, `US-IAM-006`, `US-CAT-002`, `US-CAT-003`, `US-GRP-001` đến `US-GRP-006`, `US-CNT-002`, `US-CNT-004`, `US-CNT-005`, `US-QBK-001`, `US-QBK-002`, `US-AIG-001`, `US-ASM-001`, `US-ASM-003` đến `US-ASM-010`, `US-GRD-001` đến `US-GRD-005`, `US-RPT-001`, `US-RPT-003`, `US-PAY-001`, `US-PAY-002`, `US-NTF-001`.

## 4. P-SUBJECT-MANAGER - Chủ nhiệm môn

### Hồ sơ

- **Bối cảnh**: Giảng viên được giao trách nhiệm học thuật cấp môn cho một hoặc nhiều môn; mỗi môn có thể gồm nhiều lớp do các giảng viên khác nhau đứng lớp.
- **Mục tiêu**: Duy trì nguồn học liệu chuẩn cấp môn và cung cấp template đề và ngân hàng cấp môn để các lớp dùng thống nhất.
- **Động lực**: Nâng chất lượng học thuật và giảm việc biên soạn trùng lặp giữa các lớp.
- **Khó khăn**: Cần thao tác xuyên lớp nhưng tuyệt đối không vượt sang môn chưa được phân công; cần biết tài liệu nào đã xử lý thành công để dùng cho RAG.
- **Nhu cầu truy cập**: Quản lý kho học liệu/RAG, rubric, ngân hàng câu hỏi và template đề của các môn được gán; tạo và quản lý lớp của các môn được gán như quản trị viên (tạo lớp, gán giảng viên chính, mở/lưu trữ, ghi danh); không phát hành bài thay giảng viên và không chấm hoặc chốt điểm của lớp thay giảng viên.

### Hành vi điển hình

- Tạo module của môn, tải học liệu của môn (tệp hoặc video YouTube) vào module và theo dõi trạng thái quét/lập chỉ mục.
- Yêu cầu AI tạo câu hỏi từ đúng nguồn của môn.
- Mua credit AI để xử lý học liệu/RAG và tạo câu hỏi trong các môn được phân công; theo dõi thanh toán và số dư của mình.
- Quản lý rubric/ngân hàng câu hỏi và xem trước các loại bài dùng chung của môn.
- Phát hành template có version để giảng viên copy thành bài của lớp.
- Mở Class List từ Subject Detail để tạo lớp, gán giảng viên chính, mở/lưu trữ lớp và ghi danh trong các môn được phân công.
- Theo dõi trạng thái xử lý tài liệu và nhận thông báo liên quan.
- Theo dõi tiến độ nộp bài và xem/xuất bảng điểm CSV/XLSX của các lớp thuộc môn được phân công (UC 36); không chấm hoặc chốt điểm thay giảng viên.

### Stories liên quan

`US-IAM-001`, `US-IAM-002`, `US-IAM-004`, `US-IAM-006`, `US-CAT-001` đến `US-CAT-003`, `US-CNT-001`, `US-CNT-005`, `US-QBK-001`, `US-QBK-002`, `US-AIG-002`, `US-ASM-004` đến `US-ASM-009`, `US-RPT-001`, `US-RPT-003`, `US-PAY-001`, `US-PAY-002`, `US-NTF-001`.

## 5. P-ADMIN - Quản trị viên

### Hồ sơ

- **Bối cảnh**: Tài khoản vận hành có thể được giao cho cán bộ CNTT, quản trị hệ thống, cán bộ phòng đào tạo hoặc giáo vụ được trường ủy quyền; đây là quyền hệ thống, không mặc định đồng nghĩa với chức danh quản lý học thuật.
- **Mục tiêu**: Cấu hình hệ thống đúng quyền, xử lý ngoại lệ vận hành và có bằng chứng audit khi cần điều tra.
- **Động lực**: Giữ nền tảng an toàn, nhất quán và đủ ổn định cho đợt thử nghiệm với người thật.
- **Khó khăn**: Sai phân quyền hoặc cấp quyền thanh toán có thể làm lộ dữ liệu; cần thông tin rõ nhưng không được thay đổi/xóa audit log.
- **Nhu cầu truy cập**: Quyền quản trị được kiểm soát phía server; mọi thay đổi đặc quyền và nghiệp vụ quan trọng phải được audit.

### Hành vi điển hình

- Quản lý tài khoản, bốn vai trò và phạm vi môn của Chủ nhiệm môn.
- Cấu hình quota, model, giới hạn chi phí/kill-switch và giám sát dịch vụ AI theo cách không khóa nhà cung cấp.
- Tạo cấu trúc môn/lớp, phân công và ghi danh.
- Gói credit AI cố định trong hệ thống; việc xác minh thanh toán chạy tự động.
- Tra cứu audit theo phạm vi quản trị.
- Xem thống kê tài khoản, môn, lớp và ghi danh ngay trên Admin Menu.

### Stories liên quan

`US-IAM-002`, `US-IAM-004` đến `US-IAM-007`, `US-CAT-001`, `US-CAT-003`, `US-AIG-003`, `US-PAY-001`, `US-PAY-002`, `US-RPT-002`, `US-AUD-001`, `US-NTF-001`.

## 6. Ma trận persona - miền nghiệp vụ

| Persona | Danh tính | Học thuật/nội dung | Nhóm | Học tập | AI/đánh giá | Điểm | Thanh toán | Thông báo | Audit |
|---|---|---|---|---|---|---|---|---|---|
| Người học | Chính | Đọc theo ghi danh | Thành viên/leader | Chính | Làm/nộp bài, dùng AI chấm Practice Essay khi đủ credit | Xem cá nhân | Mua/xem credit cá nhân | Nhận | Không |
| Giảng viên | Chính | Quản lý lớp | Tạo nhóm, chỉ định leader | Theo dõi | Tạo/giao/chấm bài lớp | Duyệt lớp | Mua credit cá nhân | Nhận | Qua hành động được ghi |
| Chủ nhiệm môn | Chính | Quản lý cấp môn | Không mặc định | Không trực tiếp | Tạo template đề | Không mặc định | Mua credit cá nhân | Nhận | Qua hành động được ghi |
| Quản trị viên | Quản trị | Quản trị cấu trúc | Không mặc định | Theo quyền | Theo quyền quản trị | Không xem sổ điểm | Mua credit cá nhân (gói cố định) | Cấu hình/nhận | Chính |

## 7. Nguyên tắc phân quyền xuyên persona

- Quyền được kiểm tra phía server ở cả mức chức năng và đối tượng.
- Một người có thể được gán nhiều vai trò, nhưng mỗi thao tác chỉ dùng quyền và phạm vi đã được cấp.
- Quyền Chủ nhiệm môn chỉ có hiệu lực với các môn được gán; không suy rộng toàn tổ chức.
- Giảng viên không được truy cập lớp hoặc bài nộp ngoài phân công.
- Người học không được đọc dữ liệu của người học khác.
- Quản trị viên không được sửa hoặc xóa audit log của ứng dụng.
- Không tồn tại role Head of Department/Trưởng bộ môn; quyền cấp môn thuộc Chủ nhiệm môn và quyền quản trị tổ chức thuộc Quản trị viên.

## 8. Truy vết nguồn

Các persona được dẫn xuất từ `FR-001` đến `FR-026`, đặc biệt `FR-002`, `FR-003`, `FR-004`, `FR-006`, `FR-007`, `FR-009`, `FR-015` đến `FR-026`; đồng thời tuân theo `SEC-001`, `SEC-002`, `SEC-003`, `SEC-005`, `SEC-007`, các quyết định làm rõ User Stories Q1-Q3 và quyết định loại Head of Department/Trưởng bộ môn.
