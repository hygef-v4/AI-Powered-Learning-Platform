# Bổ sung theo góp ý hội đồng — 2026-10-13

Người dùng yêu cầu bổ sung các tính năng này vào Inception. Đây là phạm vi dự kiến triển khai và tiêu chí nghiệm thu, chưa phải bằng chứng tính năng đã chạy. Chỉ có hai loại bài ASSIGNMENT/QUIZ; không mở lại bán credit, Code Lab hoặc thi có giám sát toàn diện.

## 1. Backup Google Drive — FR-038, UC 66, US-OPS-001

- U03 thực hiện backup PostgreSQL nhất quán theo lịch cấu hình, kèm phiên bản schema/app, thời gian, checksum và trạng thái hoàn tất; chỉ backup database. Admin cấu hình lịch qua Settings và xem lần thành công gần nhất, lịch sử, dung lượng và lỗi. Giờ chạy cần xác nhận trước triển khai; các bản được giữ 7 ngày (chốt 2026-10-15).
- Bản backup nằm trong thư mục Google Drive riêng tư, tách khỏi thư mục tệp đang dùng và khỏi VPS. Tệp học tập đang lưu trên Drive không được backup (chốt 2026-10-15); tệp bị mất hoặc xóa thì không khôi phục được từ backup.
- Archive chứa dữ liệu nhạy cảm phải được mã hóa trước upload; khóa giải mã và secrets cần để khôi phục do người vận hành giữ riêng ngoài archive/VPS. Không xuất API key hay thông tin xác thực dạng rõ. Quyền xem trạng thái không đồng nghĩa quyền tải toàn bộ dữ liệu học vụ.
- Chỉ đánh dấu SUCCESS sau khi dump và upload/kiểm checksum thành công. Retry hữu hạn; lỗi hoặc thiếu dung lượng không xóa bản tốt gần nhất. U03 phát sự kiện backup thất bại để U16 gửi in-app/email cho Admin; lỗi email không thay đổi kết quả backup.
- Restore do người vận hành thực hiện theo runbook vào môi trường cô lập trước: kiểm số bản ghi, quyền, tài liệu, XML Draw.io, bài nộp, điểm và usage/quota; tệp trên Drive không được khôi phục. Không thêm nút ghi đè production hoặc MCP restore tool vào MVP.
- Tiêu chí nghiệm thu: tạo backup hợp lệ; mô phỏng lỗi upload; khôi phục bản hợp lệ và ghi bằng chứng. Đo RPO/RTO từ lịch và bài thử, không cam kết không mất dữ liệu hoặc coi kiểm checksum là kiểm thử restore.

## 2. Ghi nhận hoạt động làm bài — FR-039, UC 42, US-INT-001

- Giảng viên bật/tắt `activityMonitoringEnabled` cho từng Assignment/Quiz, mặc định tắt; policy được snapshot khi bắt đầu làm. Sinh viên được thông báo dữ liệu thu thập và mục đích trước khi làm; không thay chính sách âm thầm giữa lượt.
- Khi bật, ghi nhận trang chuyển hidden/visible: thời điểm, số lần, khoảng thời gian rời trang và khoảng không xác định khi mất kết nối. Blur/focus nếu thu thập chỉ là tín hiệu phụ, không cộng trùng với visibility. Tương tác với iframe Draw.io trong trang không tự tính là rời trang.
- U11 sở hữu ActivityEvent; U14 ghi sự kiện bài nhóm qua public port U11. Server kiểm tài khoản, enrollment, attempt/group membership, trạng thái lượt và giới hạn tần suất; nhận trùng không tăng số lần. Lưu thời điểm nhận phía server cùng thời điểm client, không coi client là nguồn chống giả mạo tuyệt đối.
- Giảng viên được giao dạy xem timeline/tổng hợp từng sinh viên trong màn review U15. Bài nhóm phân biệt hoạt động từng thành viên; không quy lỗi toàn nhóm. Sinh viên chỉ xem dữ liệu của mình. Thời hạn giữ/xóa dữ liệu phải cấu hình và thông báo trước triển khai.
- Đây là dấu hiệu để đối chiếu, không bằng chứng chắc chắn gian lận: chuyển trang có thể dùng công cụ được phép; trình duyệt không phát hiện mọi thiết bị/ứng dụng ngoài. Không tự trừ điểm, kết luận gian lận hoặc tự nộp vì rời trang. Không webcam, ảnh màn hình hoặc chặn hệ điều hành trong MVP.
- Tiêu chí nghiệm thu: policy off không thu thập; hidden/visible không đếm trùng; Draw.io không bị báo nhầm; offline hiển thị khoảng thiếu; ngoài quyền bị từ chối; dữ liệu không tự sửa điểm.

## 3. Email sự kiện quan trọng — FR-011/023, US-NTF-001

- Giữ OTP kích hoạt/khôi phục, ghi danh, bài mới/mở giao, công bố điểm và nhắc hạn. Tạo/import tài khoản vẫn không tự gửi OTP; người dùng yêu cầu kích hoạt theo FR-015.
- Giảng viên có thể đánh dấu thông báo lớp mới là quan trọng và chọn gửi email. Sửa thông báo không tự gửi lại email tạo mới. Backup thất bại gửi cho Admin; không gửi dữ liệu học vụ cho Admin.
- U16 xử lý sau commit với khóa chống trùng theo sự kiện/người nhận/kênh, retry/backoff hữu hạn, trạng thái PENDING/SENT/FAILED; chỉ SENT khi nhà cung cấp chấp nhận, không khẳng định đã vào inbox. OTP vẫn thuộc luồng U01, dùng IMail chung.
- Email học tập tuân tùy chọn người nhận; OTP và cảnh báo vận hành theo chính sách riêng. Kiểm người nhận hiện hành, giới hạn tần suất/hạn mức nhà cung cấp. Email chỉ chứa nội dung tối thiểu và link yêu cầu đăng nhập; không raw điểm/bài nộp/API key hoặc backup attachment.
- Tiêu chí nghiệm thu: đúng đối tượng, sự kiện lặp không gửi trùng, provider lỗi không rollback nghiệp vụ, vượt hạn mức ghi trạng thái rõ và không retry vô hạn.

## 4. Minh bạch AI — FR-034/036, US-AIC-004

- Không bán credit/token hoặc có ví thanh toán. Quota dùng API chung do đơn vị vận hành tài trợ, chỉ dành cho giảng viên đủ quyền; Student dùng kết nối cá nhân.
- Trước yêu cầu AI hiển thị nguồn đang chọn (API chung/API cá nhân/agent MCP), bên chịu chi phí, quota còn lại khi áp dụng và ước lượng nếu có. Hết quota yêu cầu người dùng chủ động chọn nguồn khác, không tự gọi API cá nhân.
- Lịch sử hiển thị nguồn, provider/model nếu biết, thời gian, trạng thái, token input/output khi provider cung cấp và provenance measured/estimated/externallyReported/unavailable. Không cộng số tool-call MCP thành token; thiếu số liệu không ghi bằng 0.
- Nếu hiển thị chi phí quy đổi, phải có nguồn bảng giá/ngày hiệu lực và nhãn ước tính; không là hóa đơn hoặc số dư mua trên ALAP. MVP có thể chỉ hiển thị token đo được, không bắt buộc xây thanh toán hay tự suy giá.
- Tiêu chí nghiệm thu: phân biệt ước lượng và đo thực tế; MCP thiếu usage hiện unavailable; Student không nhận shared quota; hết quota không phát sinh gọi personal tự động.

## 5. Phân bổ triển khai

| Hạng mục | Unit chịu trách nhiệm | Vị trí dự kiến |
|---|---|---|
| Backup/config/status/runbook | U03; U02 audit; U16 nhận failure event | Wave 1 nền; kiểm restore khi có dữ liệu các unit và trước demo |
| Policy activity | U09; U08 lưu version publication | Wave 3, Iter 2 |
| Thu thập activity cá nhân/Quiz/nhóm | U11; U14 tích hợp port U11 | Wave 3, Iter 2 |
| Xem activity và quyết định của giảng viên | U15; U04 kiểm phân công | Wave 3, Iter 2 |
| Email học tập/cảnh báo vận hành | U16; U05 phát sự kiện thông báo lớp | Wave 4 hoặc tích hợp sớm khi event/owner sẵn sàng; OTP Iter 1 |
| Minh bạch usage | U07; U13 nguồn/usage provider | Wave 2 quota, Wave 4 tích hợp AI |

Không đổi phân công nhân sự hoặc hạn cũ. Không thêm unit mới. D05 được chốt bổ sung tối thiểu theo yêu cầu người dùng; D01–D04 vẫn còn mở. Không tuyên bố đã khảo sát hoặc runtime đã nghiệm thu.

## UC consolidation — 66 active UC

UC 14 View Settings và UC 15 Edit Setting là hai UC riêng, primary U03; bao gồm UI cấu hình quota/API chung. U07 giữ quota service/data; U13 giữ connection/provider/credentials bảo vệ và test synthetic qua public port. UC 76/78/82 (ID cũ) không còn độc lập. UC 66 gộp UC 80 (ID cũ) (U16 dashboard, U03 backup); UC 42 gộp UC 81 (ID cũ) (U15 review, U11/U14 activity). UC 35 soạn Individual, UC 53 soạn Group (U09); Draw.io tùy chọn trong UC 39/56, UC 26 (ID cũ) retired. UC 79 (ID cũ) retired; UC 41 chỉ feedback Assignment Practice đã nộp. 50 MVP + 1 extension story, 15 units; không tự phê duyệt D01–D04 hoặc runtime.
