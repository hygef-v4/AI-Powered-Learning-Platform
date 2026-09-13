# Đặc tả Use Case - AI-Powered Learning Platform

## 1. Mục đích

Tài liệu chuyển hóa toàn bộ 55 user story hiện tại thành các use case nghiệp vụ phục vụ thiết kế, kiểm thử và nghiệm thu. Hệ thống là ứng dụng web, người học chủ yếu sử dụng máy tính để bàn/laptop. Không có vai trò Head of Department/Trưởng bộ môn.

## 2. Tác nhân

| Mã | Tác nhân | Trách nhiệm |
|---|---|---|
| ACT-01 | Người học | Học, làm/nộp bài cá nhân, tham gia bài nhóm, thanh toán và xem kết quả của mình |
| ACT-02 | Giảng viên | Quản lý lớp, nhóm, giao bài, chọn cách chấm và quyết định điểm cuối |
| ACT-03 | Chủ nhiệm môn | Quản lý học liệu, ngân hàng và đề chung trong một hoặc nhiều môn được giao |
| ACT-04 | Quản trị viên | Quản lý tài khoản, quyền, cấu trúc học thuật, AI, thanh toán và audit |
| EXT-01 | Dịch vụ AI | Tạo nội dung nháp/đề xuất chấm; không tự công bố điểm |
| EXT-02 | Cổng thanh toán | Xử lý giao dịch và gửi callback đã xác thực |
| EXT-03 | Dịch vụ thông báo | Gửi email/thông báo thiết yếu |

## 3. Quy ước chung

- Mọi quyền được kiểm tra phía server ở cả mức chức năng và đối tượng.
- Tài khoản người học/giảng viên do trường cấp và đăng nhập bằng email trường; không đăng ký công khai.
- Một người có thể có nhiều role nhưng chỉ thao tác trong phạm vi môn/lớp được gán.
- AI chỉ tạo bản nháp hoặc đề xuất. Con người duyệt nội dung; giảng viên quyết định điểm cuối.
- Bài Draw.io được vẽ trên canvas web. XML đầy đủ là bản nộp chính thức, lưu nguyên vẹn và hiển thị cho giảng viên. XML rút gọn chỉ được tạo làm dữ liệu dẫn xuất khi giảng viên chọn nhờ AI chấm.
- Thao tác nhạy cảm phải có audit bất biến.

## 4. Use case chi tiết

### UC-IAM-01 - Kích hoạt, đăng nhập và đăng xuất

- **Tác nhân**: Người học, Giảng viên, Chủ nhiệm môn.
- **Mục tiêu**: Truy cập hệ thống bằng tài khoản trường cấp một cách an toàn.
- **Tiền điều kiện**: Tài khoản tồn tại, có email trường hợp lệ, chưa bị vô hiệu hóa.
- **Luồng chính**: (1) Người dùng mở liên kết kích hoạt còn hạn và đặt mật khẩu; (2) nhập email trường/mật khẩu; (3) hệ thống kiểm tra thông tin, trạng thái và chống brute-force; (4) tạo phiên và chuyển đến màn hình theo quyền; (5) khi đăng xuất, hệ thống thu hồi phiên.
- **Ngoại lệ**: Link hết hạn/đã dùng được từ chối và có thể cấp lại; sai thông tin trả lỗi trung tính; tài khoản khóa không tạo phiên.
- **Hậu điều kiện**: Phiên hợp lệ được tạo hoặc thu hồi; sự kiện cần thiết được audit.
- **Truy vết**: US-IAM-001, US-IAM-002.

### UC-IAM-02 - Khôi phục và đổi mật khẩu

- **Tác nhân**: Người học, Giảng viên, Chủ nhiệm môn.
- **Tiền điều kiện**: Đổi mật khẩu cần phiên hợp lệ; khôi phục cho phép nhập email công khai.
- **Luồng chính**: Gửi yêu cầu; hệ thống phản hồi trung tính, gửi token một lần nếu hợp lệ; người dùng đặt mật khẩu đạt chính sách; hệ thống lưu an toàn và thu hồi phiên cần thiết.
- **Ngoại lệ**: Token sai/hết hạn/đã dùng hoặc yêu cầu quá tần suất bị từ chối.
- **Hậu điều kiện**: Mật khẩu mới có hiệu lực và có audit.
- **Truy vết**: US-IAM-003, US-IAM-006.

### UC-IAM-03 - Quản lý hồ sơ

- **Tác nhân**: Người học, Giảng viên, Chủ nhiệm môn.
- **Tiền điều kiện**: Đã đăng nhập.
- **Luồng chính**: Mở hồ sơ, sửa trường được phép, hệ thống validate và lưu.
- **Ngoại lệ**: Không cho tự sửa email định danh, role hoặc trường đặc quyền; dữ liệu sai không thay thế dữ liệu cũ.
- **Hậu điều kiện**: Hồ sơ hợp lệ được cập nhật.
- **Truy vết**: US-IAM-004.

### UC-IAM-04 - Quản lý vai trò, phạm vi và tài khoản

- **Tác nhân**: Quản trị viên.
- **Tiền điều kiện**: Có quyền quản trị và xác thực phù hợp.
- **Luồng chính**: Tìm tài khoản; gán/gỡ role hoặc phạm vi môn; khóa/mở/vô hiệu hóa; hệ thống kiểm tra chống leo quyền; xác nhận và ghi thay đổi trước/sau.
- **Ngoại lệ**: Tự leo quyền, gán môn ngoài phạm vi, xóa quyền bảo vệ cuối cùng hoặc sửa audit bị từ chối.
- **Hậu điều kiện**: Quyền/trạng thái mới có hiệu lực, phiên cũ được xử lý và audit bất biến.
- **Truy vết**: US-IAM-005, US-IAM-007.

### UC-CAT-01 - Quản lý môn, lớp và phân công

- **Tác nhân**: Quản trị viên; Giảng viên là bên được phân công.
- **Tiền điều kiện**: Có quyền quản lý cấu trúc học thuật.
- **Luồng chính**: Tạo/cập nhật môn; tạo lớp thuộc môn; đặt trạng thái lớp; phân công giảng viên; hệ thống validate và lưu.
- **Ngoại lệ**: Trùng mã, môn không tồn tại, người được phân công không hợp lệ hoặc thay đổi làm mất lịch sử bị từ chối.
- **Hậu điều kiện**: Cấu trúc và phân công hợp lệ có audit.
- **Truy vết**: US-CAT-001.

### UC-CAT-02 - Quản lý vòng đời và ghi danh lớp

- **Tác nhân**: Giảng viên, Quản trị viên; Người học là đối tượng ghi danh.
- **Tiền điều kiện**: Lớp tồn tại; tác nhân đúng phạm vi.
- **Luồng chính**: Mở/đóng lớp; thêm hoặc gỡ ghi danh theo chính sách; hệ thống kiểm tra trùng, quyền và tác động lịch sử; lưu và thông báo.
- **Ngoại lệ**: Không xóa dữ liệu học tập khi gỡ ghi danh; giảng viên không thao tác lớp khác.
- **Hậu điều kiện**: Trạng thái lớp/danh sách học viên nhất quán.
- **Truy vết**: US-CAT-002, US-CAT-003.

### UC-CAT-03 - Tự ghi danh bằng mã mời (Phase 2)

- **Tác nhân**: Người học.
- **Tiền điều kiện**: Đã đăng nhập; lớp cho tự ghi danh.
- **Luồng chính**: Nhập mã; hệ thống kiểm tra lớp, hạn, quota, entitlement; người học xác nhận; hệ thống tạo ghi danh đúng một lần.
- **Ngoại lệ**: Mã sai/hết hạn, lớp đóng, đã ghi danh hoặc thiếu quyền lợi bị từ chối an toàn.
- **Truy vết**: US-CAT-005.

### UC-CNT-01 - Quản lý học liệu và RAG cấp môn

- **Tác nhân**: Chủ nhiệm môn.
- **Tiền điều kiện**: Được giao môn mục tiêu.
- **Luồng chính**: Chọn môn; tải/cập nhật tài liệu; hệ thống kiểm tra file; xử lý và lập chỉ mục; hiển thị trạng thái; cho dùng khi hoàn tất.
- **Ngoại lệ**: File độc hại/sai định dạng bị cách ly; lỗi xử lý có retry an toàn; không truy xuất nguồn môn khác.
- **Hậu điều kiện**: Tài liệu hợp lệ có phiên bản, nguồn gốc và phạm vi.
- **Truy vết**: US-CNT-001.

### UC-CNT-02 - Quản lý và truy cập nội dung lớp

- **Tác nhân**: Giảng viên, Người học.
- **Tiền điều kiện**: Giảng viên được phân công; người học đã ghi danh.
- **Luồng chính**: Giảng viên soạn/tải, sắp xếp và xuất bản nội dung; người học mở nội dung đã phát hành.
- **Ngoại lệ**: Bản nháp không hiển thị; file sai hoặc truy cập chéo lớp bị từ chối.
- **Hậu điều kiện**: Nội dung đúng lớp chỉ hiển thị cho người đủ quyền.
- **Truy vết**: US-CNT-002, US-LRN-001.

### UC-CNT-03 - Tìm kiếm, tóm tắt và trao đổi lớp (Phase 2)

- **Tác nhân**: Người học, Giảng viên, Chủ nhiệm môn.
- **Tiền điều kiện**: Có quyền trên lớp/môn.
- **Luồng chính**: Nhập truy vấn/bài đăng; hệ thống giới hạn nguồn theo quyền; trả kết quả có nguồn hoặc đăng nội dung và thông báo.
- **Ngoại lệ**: Thiếu nguồn thì nêu giới hạn; prompt vượt phạm vi/nội dung không hợp lệ bị chặn.
- **Truy vết**: US-CNT-003, US-CNT-004.

### UC-GRP-01 - Thành lập nhóm và chỉ định leader

- **Tác nhân**: Giảng viên.
- **Tiền điều kiện**: Quản lý lớp; thành viên thuộc lớp.
- **Luồng chính**: Tạo nhiều nhóm; thêm thành viên; chỉ định một leader mỗi nhóm; validate và công bố.
- **Ngoại lệ**: Thành viên ngoài lớp, trùng nhóm trái chính sách, nhóm không có hoặc có nhiều leader bị từ chối.
- **Hậu điều kiện**: Mỗi nhóm hợp lệ có đúng một leader.
- **Truy vết**: US-GRP-001.

### UC-GRP-02 - Yêu cầu đổi leader

- **Tác nhân**: Người học, Giảng viên.
- **Tiền điều kiện**: Người yêu cầu thuộc nhóm đang hoạt động.
- **Luồng chính**: Sinh viên gửi lý do/đề xuất; giảng viên duyệt hoặc từ chối; hệ thống cập nhật leader và thông báo.
- **Ngoại lệ**: Người được đề xuất ngoài nhóm hoặc yêu cầu trùng bị từ chối; sinh viên không tự đổi leader.
- **Hậu điều kiện**: Nhóm vẫn có đúng một leader và giữ lịch sử quyết định.
- **Truy vết**: US-GRP-002.

### UC-GRP-03 - Giao và chấm phần cá nhân của bài nhóm

- **Tác nhân**: Giảng viên, Người học; Dịch vụ AI hỗ trợ tùy chọn.
- **Tiền điều kiện**: Nhóm tồn tại; bài nhóm đã phát hành.
- **Luồng chính**: (1) Giảng viên mô tả bài chung và tách các phần như use case diagram, activity diagram; (2) gán mỗi phần cho thành viên; (3) sinh viên nộp phần của mình; (4) sau khi nhận bài, giảng viên chọn chấm tay hoặc nhờ AI đề xuất; (5) giảng viên quyết định điểm cuối.
- **Ngoại lệ**: Không nộp thay người khác; lỗi AI không làm mất bài và vẫn cho chấm tay.
- **Hậu điều kiện**: Bài cá nhân, phiên bản, điểm và phản hồi gắn đúng người/nhóm.
- **Truy vết**: US-GRP-003, US-GRP-004.

### UC-GRP-04 - Nộp và chấm DOCX chung

- **Tác nhân**: Leader nhóm, Giảng viên.
- **Tiền điều kiện**: Đang nhận bài; người nộp là leader hiện tại.
- **Luồng chính**: Nhóm cộng tác soạn ngoài hệ thống; leader upload DOCX; hệ thống kiểm tra quyền/file và tạo biên nhận; giảng viên mở DOCX cùng các phần cá nhân, đối chiếu và chấm tay.
- **Ngoại lệ**: Thành viên không phải leader bị từ chối; file lỗi không thay bản hợp lệ trước; AI không chấm bài chung.
- **Hậu điều kiện**: Bài chung lưu theo nhóm/phiên bản; điểm do giảng viên nhập thủ công.
- **Truy vết**: US-GRP-005, US-GRP-006.

### UC-LRN-01 - Lưu và tiếp tục tiến độ

- **Tác nhân**: Người học.
- **Tiền điều kiện**: Đã ghi danh và có quyền nội dung.
- **Luồng chính**: Mở nội dung; tải vị trí gần nhất; tiếp tục/đánh dấu hoàn thành; hệ thống lưu idempotent.
- **Ngoại lệ**: Mất mạng hiển thị chưa đồng bộ và retry không nhân đôi; quyền bị thu hồi thì dừng truy cập.
- **Truy vết**: US-LRN-002.

### UC-LRN-02 - Theo dõi tiến độ lớp

- **Tác nhân**: Giảng viên.
- **Tiền điều kiện**: Được phân công lớp.
- **Luồng chính**: Chọn lớp; lọc theo học viên/nội dung/trạng thái; xem tiến độ và nhận diện người cần hỗ trợ.
- **Ngoại lệ**: Không trả dữ liệu ngoài lớp hoặc trường dữ liệu không cần thiết.
- **Truy vết**: US-LRN-003.

### UC-QBK-01 - Quản lý rubric và ngân hàng câu hỏi

- **Tác nhân**: Giảng viên, Chủ nhiệm môn.
- **Tiền điều kiện**: Có quyền trên lớp/môn.
- **Luồng chính**: Tạo/sửa/nhân bản rubric hoặc câu hỏi; khai báo tiêu chí, đáp án, metadata; xem trước và lưu phiên bản.
- **Ngoại lệ**: Không sửa trực tiếp phiên bản đã dùng; nội dung ngoài phạm vi bị từ chối.
- **Hậu điều kiện**: Mục ngân hàng có nguồn gốc, phiên bản và phạm vi.
- **Truy vết**: US-QBK-001, US-QBK-002.

### UC-QBK-02 - Phân tích chất lượng câu hỏi (Phase 2)

- **Tác nhân**: Giảng viên, Chủ nhiệm môn.
- **Tiền điều kiện**: Đủ dữ liệu và có quyền.
- **Luồng chính**: Chọn câu hỏi/kỳ dữ liệu; hệ thống tính chỉ số, hiển thị cảnh báo; tác nhân có thể tạo phiên bản cải thiện.
- **Ngoại lệ**: Mẫu nhỏ được cảnh báo; hệ thống không tự sửa nội dung đã phát hành.
- **Truy vết**: US-QBK-003.

### UC-AIG-01 - Tạo bản nháp bằng AI

- **Tác nhân**: Giảng viên, Chủ nhiệm môn; Dịch vụ AI.
- **Tiền điều kiện**: Đúng phạm vi, còn quota, nguồn RAG sẵn sàng.
- **Luồng chính**: Chọn nguồn/loại bài/rubric; nhập yêu cầu; hệ thống lọc dữ liệu và gọi AI; trả bản nháp có nguồn; tác nhân sửa và lưu.
- **Ngoại lệ**: Timeout, hết quota, thiếu căn cứ hoặc prompt vượt phạm vi được báo rõ; không tự phát hành.
- **Hậu điều kiện**: Bản nháp có nguồn gốc được lưu.
- **Truy vết**: US-AIG-001, US-AIG-002.

### UC-AIG-02 - Cấu hình và giám sát AI

- **Tác nhân**: Quản trị viên.
- **Tiền điều kiện**: Có quyền quản trị AI.
- **Luồng chính**: Xem usage; đặt model, quota, giới hạn chi phí hoặc kill-switch; validate, xác nhận và áp dụng.
- **Ngoại lệ**: Cấu hình sai/nguy hiểm bị từ chối; secret không hiển thị; kill-switch không làm mất bài đã nộp.
- **Hậu điều kiện**: Cấu hình có phiên bản và audit.
- **Truy vết**: US-AIG-003.

### UC-ASM-01 - Duyệt và phát hành bài đánh giá

- **Tác nhân**: Giảng viên, Chủ nhiệm môn.
- **Tiền điều kiện**: Bản nháp hợp lệ, đúng phạm vi.
- **Luồng chính**: Cấu hình lịch, hạn, lượt nộp, rubric; xem trước như người học; duyệt; giảng viên phát hành cho lớp hoặc Chủ nhiệm môn phát hành đề chung cho mọi lớp thuộc môn.
- **Ngoại lệ**: Thiếu cấu hình thì không phát hành; không phát hành sang lớp/môn ngoài quyền.
- **Hậu điều kiện**: Bài được phát hành đúng đối tượng và thông báo.
- **Truy vết**: US-ASM-001, US-ASM-002.

### UC-ASM-02 - Làm và nộp bài

- **Tác nhân**: Người học.
- **Tiền điều kiện**: Đã ghi danh; bài đang mở.
- **Luồng chính**: Mở bài; tải nháp; trả lời; tự lưu; xác nhận nộp; hệ thống lưu lần nộp bất biến và trả biên nhận.
- **Ngoại lệ**: Quá hạn/hết lượt xử lý theo chính sách; retry không tạo bản trùng; lỗi tạm thời không xóa nháp.
- **Hậu điều kiện**: Bản nộp gắn đúng người và phiên bản đề.
- **Truy vết**: US-ASM-003.

### UC-ASM-03 - Soạn và nộp sơ đồ Draw.io

- **Tác nhân**: Giảng viên, Người học; Dịch vụ AI tùy chọn.
- **Tiền điều kiện**: Bài Draw.io đã phát hành.
- **Luồng chính**: (1) Giảng viên soạn yêu cầu/rubric; (2) sinh viên vẽ trên canvas; (3) hệ thống lưu nháp; (4) sinh viên nộp XML đầy đủ; (5) hệ thống kiểm tra cấu trúc, kích thước, allowlist và cấm external entities; (6) lưu nguyên vẹn XML đầy đủ và tạo biên nhận; (7) giảng viên xem bản đầy đủ; (8) chỉ khi giảng viên chọn AI, hệ thống tạo XML rút gọn dẫn xuất cho lần gọi đó; (9) giảng viên quyết định điểm.
- **Ngoại lệ**: XML nguy hiểm/sai bị từ chối nhưng giữ nháp hợp lệ; AI lỗi không đổi bản đầy đủ và cho chấm tay; bản rút gọn không thay thế/hiển thị như bài gốc.
- **Hậu điều kiện**: XML đầy đủ là bản chuẩn để review, lưu trữ và audit.
- **Truy vết**: US-ASM-004, US-GRD-002, US-GRD-003.

### UC-ASM-04 - Soạn và kiểm thử Code Lab

- **Tác nhân**: Giảng viên, Người học.
- **Tiền điều kiện**: Môi trường thực thi được cấu hình.
- **Luồng chính**: Giảng viên khai báo đề, ngôn ngữ, test và quota; chạy thử và phát hành; sinh viên viết/chạy/nộp; hệ thống chạy cô lập và trả kết quả được phép.
- **Ngoại lệ**: Quá tài nguyên/thời gian hoặc mã nguy hiểm bị dừng; test ẩn/secret không lộ.
- **Truy vết**: US-ASM-005.

### UC-ASM-05 - Soạn trắc nghiệm, bài viết luận và tái sử dụng

- **Tác nhân**: Giảng viên, Chủ nhiệm môn.
- **Tiền điều kiện**: Có quyền phù hợp.
- **Luồng chính**: Chọn trắc nghiệm hoặc bài viết luận; thêm yêu cầu/câu hỏi/rubric/đáp án; xem trước; lưu và phát hành qua UC-ASM-01.
- **Phase 2**: Nhân bản để tái sử dụng, sửa bản mới hoặc ngừng giao nhưng giữ đề cũ, bài nộp, điểm và lịch sử.
- **Ngoại lệ**: Không sửa hồi tố nội dung đã có bài nộp; ngừng giao không xóa lịch sử.
- **Truy vết**: US-ASM-006, US-ASM-007, US-ASM-008.

### UC-GRD-01 - Tự chấm câu hỏi xác định

- **Tác nhân**: Hệ thống; Giảng viên giám sát.
- **Tiền điều kiện**: Bài nộp và đáp án/rule đúng phiên bản tồn tại.
- **Luồng chính**: Tải dữ liệu đúng phiên bản; chấm idempotent; lưu chi tiết; đưa kết quả sang duyệt/công bố theo chính sách.
- **Ngoại lệ**: Thiếu cấu hình/lỗi chấm chuyển cho giảng viên, không tự gán điểm sai.
- **Truy vết**: US-GRD-001.

### UC-GRD-02 - Chọn chấm tay hoặc AI đề xuất

- **Tác nhân**: Giảng viên; Dịch vụ AI tùy chọn.
- **Tiền điều kiện**: Hệ thống đã nhận bài thuộc lớp được phân công.
- **Luồng chính**: Mở bài/rubric; chọn chấm tay hoặc “Nhờ AI đề xuất”; nếu AI, hệ thống gửi dữ liệu tối thiểu và hiển thị đề xuất/căn cứ; giảng viên chấp nhận, sửa hoặc bỏ đề xuất.
- **Ngoại lệ**: AI lỗi/không chắc chắn thì báo rõ và cho chấm tay; AI không cập nhật điểm cuối trực tiếp.
- **Hậu điều kiện**: Có kết quả nháp chờ giảng viên chốt.
- **Truy vết**: US-GRD-002.

### UC-GRD-03 - Duyệt, chốt và công bố điểm

- **Tác nhân**: Giảng viên; Người học nhận kết quả.
- **Tiền điều kiện**: Có bài/kết quả cần duyệt.
- **Luồng chính**: Xem bài, rubric và kết quả; nhập/sửa điểm/phản hồi; cung cấp lý do ghi đè khi cần; chốt; công bố; gửi thông báo.
- **Ngoại lệ**: Điểm ngoài thang, thiếu lý do hoặc cập nhật đồng thời bị từ chối/phát hiện; mọi ghi đè có audit.
- **Hậu điều kiện**: Điểm cuối và người quyết định được lưu; người học chỉ thấy sau công bố.
- **Truy vết**: US-GRD-003, US-GRD-005.

### UC-GRD-04 - Xem sổ điểm

- **Tác nhân**: Người học, Giảng viên, Quản trị viên.
- **Tiền điều kiện**: Có quyền dữ liệu.
- **Luồng chính**: Chọn lớp/bài hoặc hồ sơ cá nhân; hệ thống lọc theo role và hiển thị điểm, phản hồi, trạng thái.
- **Ngoại lệ**: Sinh viên không xem điểm chưa công bố/người khác; giảng viên không xem lớp khác.
- **Truy vết**: US-GRD-004.

### UC-GRD-05 - Gia hạn, phúc khảo và tương đồng (Phase 2)

- **Tác nhân**: Người học, Giảng viên.
- **Tiền điều kiện**: Bài/lần nộp đúng người và lớp tồn tại.
- **Luồng chính**: Sinh viên gửi gia hạn/phúc khảo; giảng viên xem và quyết định; hệ thống cập nhật có lý do/lịch sử. Giảng viên có thể yêu cầu kiểm tra tương đồng và tự diễn giải kết quả.
- **Ngoại lệ**: Yêu cầu trái chính sách bị từ chối; chỉ số tương đồng không tự kết luận gian lận hoặc đổi điểm.
- **Truy vết**: US-GRD-006, US-GRD-007, US-GRD-008.

### UC-RPT-01 - Theo dõi nộp bài và nhắc nhở

- **Tác nhân**: Giảng viên; Dịch vụ thông báo.
- **Tiền điều kiện**: Bài đã phát hành trong lớp được phân công.
- **Luồng chính**: Lọc chưa nộp/đã nộp/quá hạn; chọn người nhận; gửi nhắc và theo dõi trạng thái.
- **Ngoại lệ**: Provider lỗi không rollback nghiệp vụ; retry hữu hạn và chống trùng.
- **Truy vết**: US-RPT-001.

### UC-RPT-02 - Dashboard và xuất bảng điểm (Phase 2)

- **Tác nhân**: Người học, Giảng viên, Quản trị viên.
- **Tiền điều kiện**: Có quyền trên phạm vi dữ liệu.
- **Luồng chính**: Chọn phạm vi/chỉ số/định dạng; hệ thống tổng hợp; hiển thị dashboard hoặc tạo file tải có hạn.
- **Ngoại lệ**: Dataset lớn xử lý bất đồng bộ; loại dữ liệu ngoài quyền; file hết hạn không tải được.
- **Truy vết**: US-RPT-002, US-RPT-003.

### UC-RPT-03 - Đối sánh điểm AI và điểm chốt (Phase 2)

- **Tác nhân**: Giảng viên, Quản trị viên.
- **Tiền điều kiện**: Có cả đề xuất AI và điểm cuối.
- **Luồng chính**: Chọn phạm vi; hệ thống ghép cặp, tính chênh lệch và hiển thị phân bố/trường hợp cần xem.
- **Ngoại lệ**: Cảnh báo mẫu nhỏ; không lộ dữ liệu dư thừa và không tự sửa điểm.
- **Truy vết**: US-RPT-004.

### UC-PAY-01 - Thanh toán và cấp quyền

- **Tác nhân**: Người học; Cổng thanh toán.
- **Tiền điều kiện**: Đã đăng nhập; sản phẩm/lớp khả dụng.
- **Luồng chính**: Chọn gói; tạo giao dịch idempotent; chuyển cổng; nhận callback đã xác thực; đối chiếu tiền/trạng thái; cấp entitlement đúng một lần.
- **Ngoại lệ**: Hủy/thất bại/timeout giữ trạng thái; callback giả/replay/sai tiền bị từ chối; không cấp quyền chỉ từ browser redirect.
- **Truy vết**: US-PAY-001, US-PAY-002.

### UC-PAY-02 - Đối soát thanh toán

- **Tác nhân**: Quản trị viên; Cổng thanh toán.
- **Tiền điều kiện**: Có quyền thanh toán.
- **Luồng chính**: Chọn kỳ/trạng thái; hệ thống đối chiếu giao dịch, callback, entitlement; hiển thị lệch; quản trị viên xác minh và xử lý có lý do.
- **Ngoại lệ**: Provider lỗi thì giữ dữ liệu và retry; thao tác thủ công luôn audit.
- **Truy vết**: US-PAY-003.

### UC-OPS-01 - Gửi thông báo thiết yếu

- **Tác nhân**: Tất cả người dùng; Dịch vụ thông báo.
- **Tiền điều kiện**: Sự kiện tài khoản, ghi danh, giao bài, nhóm hoặc kết quả đã hoàn tất.
- **Luồng chính**: Phát sự kiện; tạo thông báo idempotent; chọn kênh; gửi và ghi trạng thái.
- **Ngoại lệ**: Provider timeout thì retry/backoff; không rollback giao dịch chính; không đưa secret/dữ liệu dư thừa vào nội dung.
- **Truy vết**: US-NTF-001.

### UC-OPS-02 - Tra cứu audit

- **Tác nhân**: Quản trị viên.
- **Tiền điều kiện**: Có quyền audit.
- **Luồng chính**: Lọc theo actor, sự kiện, đối tượng, correlation ID hoặc thời gian; xem timestamp, actor, hành động và kết quả đã khử dữ liệu nhạy cảm.
- **Ngoại lệ**: Sửa/xóa hoặc xem ngoài phạm vi bị từ chối và ghi nhận theo chính sách.
- **Hậu điều kiện**: Audit vẫn bất biến.
- **Truy vết**: US-AUD-001.

## 5. Quan hệ include/extend

| Nguồn | Quan hệ | Đích | Ý nghĩa |
|---|---|---|---|
| Mọi use case bảo vệ | include | Kiểm tra phiên/role/quyền đối tượng | Thực hiện trước hành động nghiệp vụ |
| UC-ASM-01 | extend | UC-AIG-01 | AI tạo nháp là tùy chọn |
| UC-GRP-03, UC-ASM-03 | extend | UC-GRD-02 | AI chấm phần cá nhân chỉ khi giảng viên chọn |
| UC-ASM-02 | include | UC-GRD-01 | Chỉ với câu hỏi xác định |
| UC-GRD-03, UC-PAY-01 | include | UC-OPS-01 | Công bố kết quả/cấp quyền tạo thông báo |
| Thay đổi role, leader, đề, điểm, payment, AI | include | Ghi audit | Audit không thể sửa qua ứng dụng |

## 6. Ma trận truy vết 55/55 user story

| Use case | User story được bao phủ |
|---|---|
| UC-IAM-01 | US-IAM-001, US-IAM-002 |
| UC-IAM-02 | US-IAM-003, US-IAM-006 |
| UC-IAM-03 | US-IAM-004 |
| UC-IAM-04 | US-IAM-005, US-IAM-007 |
| UC-CAT-01 | US-CAT-001 |
| UC-CAT-02 | US-CAT-002, US-CAT-003 |
| UC-CAT-03 | US-CAT-005 |
| UC-CNT-01 | US-CNT-001 |
| UC-CNT-02 | US-CNT-002, US-LRN-001 |
| UC-CNT-03 | US-CNT-003, US-CNT-004 |
| UC-GRP-01 | US-GRP-001 |
| UC-GRP-02 | US-GRP-002 |
| UC-GRP-03 | US-GRP-003, US-GRP-004 |
| UC-GRP-04 | US-GRP-005, US-GRP-006 |
| UC-LRN-01 | US-LRN-002 |
| UC-LRN-02 | US-LRN-003 |
| UC-QBK-01 | US-QBK-001, US-QBK-002 |
| UC-QBK-02 | US-QBK-003 |
| UC-AIG-01 | US-AIG-001, US-AIG-002 |
| UC-AIG-02 | US-AIG-003 |
| UC-ASM-01 | US-ASM-001, US-ASM-002 |
| UC-ASM-02 | US-ASM-003 |
| UC-ASM-03 | US-ASM-004 |
| UC-ASM-04 | US-ASM-005 |
| UC-ASM-05 | US-ASM-006, US-ASM-007, US-ASM-008 |
| UC-GRD-01 | US-GRD-001 |
| UC-GRD-02 | US-GRD-002 |
| UC-GRD-03 | US-GRD-003, US-GRD-005 |
| UC-GRD-04 | US-GRD-004 |
| UC-GRD-05 | US-GRD-006, US-GRD-007, US-GRD-008 |
| UC-RPT-01 | US-RPT-001 |
| UC-RPT-02 | US-RPT-002, US-RPT-003 |
| UC-RPT-03 | US-RPT-004 |
| UC-PAY-01 | US-PAY-001, US-PAY-002 |
| UC-PAY-02 | US-PAY-003 |
| UC-OPS-01 | US-NTF-001 |
| UC-OPS-02 | US-AUD-001 |

## 7. Quy tắc nghiệp vụ cốt lõi

1. Chủ nhiệm môn có thể phụ trách nhiều môn, nhưng không có quyền hoạt động lớp/điểm nếu không đồng thời được phân công làm giảng viên.
2. Mỗi nhóm có đúng một leader; chỉ leader hiện tại nộp DOCX chung.
3. Bài cá nhân có thể nhận đề xuất AI; bài chung chỉ do giảng viên chấm tay.
4. Giảng viên chọn AI hoặc chấm tay sau khi nhận bài và luôn quyết định điểm cuối.
5. XML Draw.io đầy đủ là nguồn chuẩn; XML rút gọn chỉ là bản dẫn xuất tạm cho AI.
6. Nhân bản/ngừng giao bài không xóa hoặc sửa hồi tố đề đã dùng, bài nộp, điểm và audit.
7. Callback thanh toán phải xác thực, chống replay và idempotent trước khi cấp quyền.

## 8. Bảo mật và khả năng phục hồi

- Validate và giới hạn mọi input, upload, XML, DOCX và dữ liệu từ dịch vụ ngoài.
- XML parser tắt external entities và áp dụng allowlist cấu trúc Draw.io.
- Code Lab chạy cô lập với quota CPU, bộ nhớ, thời gian và mạng.
- Dữ liệu gửi AI phải tối thiểu, đúng môn/lớp; không gửi secret hoặc dữ liệu không cần thiết.
- AI, payment và notification có timeout, retry hữu hạn, backoff và idempotency.
- Lỗi tích hợp không làm mất nháp, bài nộp, điểm đã chốt hoặc giao dịch đã xác nhận.
- Log/audit không chứa mật khẩu, token, secret hoặc dữ liệu bài làm dư thừa.

## 9. Kiểm tra độ đầy đủ

- 55/55 user story có use case truy vết.
- 4/4 persona nghiệp vụ được bao phủ; không có Head of Department.
- Luồng nhóm phân biệt phần cá nhân và DOCX chung.
- Luồng Draw.io phân biệt XML đầy đủ và XML rút gọn dẫn xuất.
- Luồng chấm xác định rõ quyền chọn và quyết định cuối của giảng viên.
- Các chức năng Phase 2 được đánh dấu theo user story nguồn.

