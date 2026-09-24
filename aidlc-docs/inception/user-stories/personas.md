# Personas - AI-Powered Learning Platform

## 1. Phạm vi

Bộ persona đại diện cho bốn vai trò RBAC của MVP trong một trường học hoặc trung tâm đào tạo. Persona mô tả mục tiêu và hành vi nghiệp vụ; không thay thế đặc tả authorization phía server trong thiết kế.

## 2. P-LEARNER - Người học

### Hồ sơ

- **Bối cảnh**: Học trong một hoặc nhiều lớp đã được ghi danh, chủ yếu dùng web trên máy tính để bàn hoặc laptop; mobile chủ yếu để đọc và xem thông báo/kết quả.
- **Mục tiêu**: Truy cập đúng nội dung, tiếp tục việc học, nộp bài đúng hạn và nhận điểm/phản hồi rõ ràng.
- **Động lực**: Hoàn thành khóa học và hiểu mình cần cải thiện điều gì.
- **Khó khăn**: Dễ mất phương hướng khi nội dung nhiều; cần biết trạng thái bài nộp, thanh toán và lỗi hệ thống mà không phải liên hệ hỗ trợ ngay.
- **Nhu cầu truy cập**: Chỉ dữ liệu, tiến độ, bài nộp, điểm và quyền lợi của chính mình trong các lớp được ghi danh.

### Hành vi điển hình

- Đăng nhập bằng email trường đã được cấp, quản lý hồ sơ và khôi phục mật khẩu; không tự đăng ký tài khoản công khai.
- Truy cập lớp, học nội dung, đánh dấu hoàn thành và tiếp tục từ vị trí gần nhất.
- Làm bài, nộp bài và xem kết quả sau khi được công bố.
- Vẽ sơ đồ trên canvas Draw.io và nộp XML đầy đủ cho giảng viên, hoặc làm trắc nghiệm, Code Lab và bài viết luận; lưu nháp, khôi phục và xem lịch sử lần nộp.
- Làm và nộp phần cá nhân của bài nhóm để hệ thống đưa vào tài liệu tổng hợp; xem phản hồi phần cá nhân và điểm cuối do giảng viên quyết định.
- Làm simulation exam trong giới hạn lượt, biết rõ bài có hoặc không tính điểm thành phần và chính sách lấy kết quả.
- Thực hiện thanh toán và theo dõi trạng thái cấp quyền.
- Nhận thông báo thiết yếu.

### Stories liên quan

`US-IAM-001`, `US-IAM-002`, `US-IAM-003`, `US-IAM-004`, `US-IAM-006`, `US-CAT-005`, `US-GRP-002`, `US-GRP-004`, `US-CNT-004`, `US-LRN-001`, `US-LRN-002`, `US-ASM-003`, `US-ASM-011`, `US-GRD-001`, `US-GRD-004`, `US-GRD-006`, `US-GRD-007`, `US-RPT-002`, `US-PAY-001`, `US-PAY-002`, `US-NTF-001`.

## 3. P-INSTRUCTOR - Giảng viên

### Hồ sơ

- **Bối cảnh**: Phụ trách một hoặc nhiều lớp cụ thể, chuẩn bị nội dung riêng, giao bài và theo dõi người học; quyền mặc định không vượt ra ngoài các lớp được phân công.
- **Mục tiêu**: Tổ chức lớp hiệu quả, giảm thời gian soạn/chấm bài bằng AI nhưng giữ quyền quyết định học thuật cuối cùng.
- **Động lực**: Cải thiện chất lượng phản hồi và phát hiện người học cần hỗ trợ.
- **Khó khăn**: Khối lượng nội dung và bài nộp lớn; cần phân biệt rõ tài nguyên cấp môn với nội dung riêng của lớp.
- **Nhu cầu truy cập**: Chỉ lớp, nội dung, người học, bài nộp và kết quả thuộc phân công; không được sửa kho học liệu/RAG cấp môn nếu không đồng thời có quyền Chủ nhiệm môn.

### Hành vi điển hình

- Quản lý vòng đời lớp/khóa học và nội dung riêng của lớp.
- Ghi danh người học khi được cấp quyền.
- Chia lớp thành nhóm, chỉ định trưởng nhóm, phân công phần cá nhân và xử lý yêu cầu đổi trưởng nhóm.
- Định nghĩa cấu trúc bài nhóm, xem trước tài liệu do hệ thống tổng hợp, điều chỉnh thứ tự/phần được dùng và chốt version để chấm.
- Nhờ AI đề xuất feedback cho phần cá nhân, tự chấm tài liệu chung và quyết định điểm cuối từng sinh viên dựa trên hai nguồn mà không bị hệ thống ép công thức.
- Dùng AI tạo bản nháp câu hỏi từ nội dung được phép.
- Quản lý rubric/ngân hàng câu hỏi theo version; copy assignment/rubric giữa các lớp mình phụ trách.
- Soạn, xem trước từng loại bài đánh giá và cấu hình simulation exam giới hạn lượt, có thể tính hoặc không tính điểm thành phần.
- Duyệt, xuất bản bài riêng của lớp; sau khi nhận bài, chủ động chọn chấm thủ công hoặc nhờ AI đề xuất rồi tự quyết định điểm cuối.
- Xem tiến độ nộp, gửi nhắc, chốt điểm và xem sổ điểm của lớp được phân công.

### Stories liên quan

`US-IAM-001`, `US-IAM-002`, `US-IAM-004`, `US-IAM-006`, `US-CAT-002`, `US-CAT-003`, `US-GRP-001` đến `US-GRP-006`, `US-CNT-002` đến `US-CNT-005`, `US-LRN-003`, `US-QBK-001` đến `US-QBK-003`, `US-AIG-001`, `US-ASM-001`, `US-ASM-003` đến `US-ASM-011`, `US-GRD-001` đến `US-GRD-008`, `US-RPT-001`, `US-RPT-003`, `US-RPT-004`, `US-NTF-001`.

## 4. P-SUBJECT-MANAGER - Chủ nhiệm môn

### Hồ sơ

- **Bối cảnh**: Giảng viên được giao trách nhiệm học thuật cấp môn cho một hoặc nhiều môn; mỗi môn có thể gồm nhiều lớp do các giảng viên khác nhau đứng lớp.
- **Mục tiêu**: Duy trì nguồn học liệu chuẩn cấp môn và bảo đảm đề chung được áp dụng nhất quán cho mọi lớp thuộc môn.
- **Động lực**: Nâng chất lượng học thuật và giảm việc biên soạn trùng lặp giữa các lớp.
- **Khó khăn**: Cần thao tác xuyên lớp nhưng tuyệt đối không vượt sang môn chưa được phân công; cần biết tài liệu nào đã xử lý thành công để dùng cho RAG.
- **Nhu cầu truy cập**: Quản lý kho học liệu/RAG, rubric, ngân hàng câu hỏi và đề chung của các môn được gán; phát hành trực tiếp đề chung cho mọi lớp thuộc môn mà không quản lý hoạt động thường ngày hoặc điểm của lớp thay giảng viên.

### Hành vi điển hình

- Quản lý học liệu và nguồn RAG cấp môn, gồm video/playlist YouTube gắn theo bài giảng và trạng thái caption/phiên âm/lập chỉ mục.
- Yêu cầu AI tạo câu hỏi từ đúng nguồn của môn.
- Quản lý rubric/ngân hàng câu hỏi và xem trước các loại bài dùng chung của môn.
- Duyệt và phát hành đề chung xuyên các lớp thuộc môn hoặc phát hành template có version để giảng viên copy thành đề riêng.
- Theo dõi trạng thái xử lý tài liệu và nhận thông báo liên quan.

### Stories liên quan

`US-IAM-001`, `US-IAM-002`, `US-IAM-004`, `US-IAM-006`, `US-CNT-001`, `US-CNT-003`, `US-CNT-005`, `US-QBK-001` đến `US-QBK-003`, `US-AIG-002`, `US-ASM-002`, `US-ASM-004` đến `US-ASM-009`, `US-NTF-001`.

## 5. P-ADMIN - Quản trị viên

### Hồ sơ

- **Bối cảnh**: Tài khoản vận hành có thể được giao cho cán bộ CNTT, quản trị hệ thống, cán bộ phòng đào tạo hoặc giáo vụ được trường ủy quyền; đây là quyền hệ thống, không mặc định đồng nghĩa với chức danh quản lý học thuật.
- **Mục tiêu**: Cấu hình hệ thống đúng quyền, xử lý ngoại lệ vận hành và có bằng chứng audit khi cần điều tra.
- **Động lực**: Giữ nền tảng an toàn, nhất quán và đủ ổn định cho đợt thử nghiệm với người thật.
- **Khó khăn**: Sai phân quyền hoặc cấp quyền thanh toán có thể làm lộ dữ liệu; cần thông tin rõ nhưng không được thay đổi/xóa audit log.
- **Nhu cầu truy cập**: Quyền quản trị được kiểm soát phía server; hỗ trợ MFA; mọi thay đổi đặc quyền và nghiệp vụ quan trọng phải được audit.

### Hành vi điển hình

- Quản lý tài khoản, bốn vai trò và phạm vi môn của Chủ nhiệm môn.
- Cấu hình quota, model, giới hạn chi phí/kill-switch và giám sát dịch vụ AI theo cách không khóa nhà cung cấp.
- Tạo cấu trúc môn/lớp, phân công và ghi danh.
- Đối soát giao dịch và quyền truy cập.
- Tra cứu audit theo phạm vi quản trị.

### Stories liên quan

`US-IAM-002`, `US-IAM-004` đến `US-IAM-007`, `US-CAT-001`, `US-CAT-003`, `US-AIG-003`, `US-GRD-004`, `US-RPT-003`, `US-RPT-004`, `US-PAY-002`, `US-PAY-003`, `US-AUD-001`, `US-NTF-001`.

## 6. Ma trận persona - miền nghiệp vụ

| Persona | Danh tính | Học thuật/nội dung | Nhóm | Học tập | AI/đánh giá | Điểm | Thanh toán | Thông báo | Audit |
|---|---|---|---|---|---|---|---|---|---|
| Người học | Chính | Đọc theo ghi danh | Thành viên/leader | Chính | Làm/nộp bài | Xem cá nhân | Chính | Nhận | Không |
| Giảng viên | Chính | Quản lý lớp | Tạo nhóm, chỉ định leader | Theo dõi | Tạo/giao/chấm bài lớp | Duyệt lớp | Không | Nhận | Qua hành động được ghi |
| Chủ nhiệm môn | Chính | Quản lý cấp môn | Không mặc định | Không trực tiếp | Tạo/giao đề chung | Không mặc định | Không | Nhận | Qua hành động được ghi |
| Quản trị viên | Quản trị | Quản trị cấu trúc | Không mặc định | Theo quyền | Theo quyền quản trị | Tổng hợp | Đối soát | Cấu hình/nhận | Chính |

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
