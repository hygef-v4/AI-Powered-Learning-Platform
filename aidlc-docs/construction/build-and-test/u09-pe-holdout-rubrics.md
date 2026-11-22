# Bộ đề xác nhận điểm U09

Mỗi đề có thang 10. Chuẩn bị 5 bài khác nhau cho mỗi đề, gồm bài tốt, bài đúng một phần và bài yếu. Dùng mã mẫu trong [phiếu nhập điểm](u09-pe-holdout-scores.csv). Tệp điểm và bài nộp đã điền được lưu riêng theo quy định sử dụng dữ liệu của nhóm.

1. Khóa đề và rubric trước khi thu bài. Nếu dùng bài đã được chấm từ một đề khác, lưu đúng đề và rubric của bài đó, rồi thay nhóm tương ứng trong danh sách.
2. Chấm từng mục bằng các mức điểm trong bảng. Ghi đoạn văn hoặc phần sơ đồ làm căn cứ. Ghi lý do nếu cần xác nhận cách hiểu đề hoặc thiếu dữ kiện.
3. Để trống điểm chưa xác định. Điểm 0 chỉ dùng sau khi xác nhận bài không đáp ứng tiêu chí. Nếu có hai người chấm, thống nhất các mục lệch trước khi khóa nhãn.
4. Chỉ nhập điểm đã khóa vào fixture với nguồn `LECTURER_VERIFIED`. Ghi thêm nguồn bài thật đã ẩn danh hoặc bài tổng hợp đã được xác nhận trong metadata thu thập. Dùng bộ này sau khi chốt prompt và model.

## SWR302 H01. Tiếp nhận yêu cầu sửa đồ điện

Một điểm tiếp nhận nhận đồ điện cần sửa. Khách gửi yêu cầu và mô tả lỗi. Nhân viên tiếp nhận kiểm tra, lập phiếu và báo giá. Khách xác nhận báo giá trước khi kỹ thuật viên sửa. Hệ thống gửi thông báo khi sửa xong. Khách có thể từ chối báo giá và đến nhận lại thiết bị. Một phiếu chỉ theo dõi một thiết bị và có thể có nhiều báo giá theo phiên bản. Mỗi phiên bản thuộc đúng một phiếu và chỉ một phiên bản được khách chấp nhận.

Nộp context diagram, use case diagram và ERD khái niệm. Liệt kê một quy tắc xử lý khi khách từ chối. Có thể dùng tên tiếng Anh hoặc tiếng Việt nếu vai trò và luồng được giải thích nhất quán.

| Mục | Điểm | Cách chấm |
|---|---:|---|
| context | 2 | 1 cho đúng phạm vi và tác nhân ngoài. 1 cho đúng trao đổi yêu cầu, báo giá và thông báo |
| use-cases | 2 | 1 cho yêu cầu, báo giá và xác nhận. 1 cho sửa, thông báo và xử lý từ chối |
| roles | 1.5 | 0,5 mỗi vai trò nếu Khách, Nhân viên tiếp nhận và Kỹ thuật viên nối đúng chức năng |
| entities | 1.5 | 0,5 mỗi thực thể nếu Phiếu sửa, Thiết bị và Báo giá được mô hình đúng |
| cardinality | 2 | 1 cho đúng quan hệ Phiếu sửa–Thiết bị. 1 cho Phiếu sửa–Báo giá theo phiên bản |
| rejection-rule | 1 | 1 khi mô tả rõ từ chối báo giá chặn việc sửa và cho phép nhận lại thiết bị |

## SWR302 H02. Quản lý phòng thực hành nhiều cơ sở

Hệ thống có một Quản trị trung tâm. Mỗi cơ sở có một Quản lý cơ sở và một hay nhiều Kỹ thuật viên. Giảng viên chỉ gửi yêu cầu dùng phòng thuộc cơ sở mình. Quản lý cơ sở duyệt yêu cầu tại cơ sở đó. Kỹ thuật viên nhận yêu cầu chuẩn bị thiết bị sau khi lịch được duyệt. Nếu trùng phòng và thời gian, hệ thống từ chối duyệt và đề xuất khung giờ còn trống. Nếu đã duyệt nhưng không đủ thiết bị, kỹ thuật viên ghi thiếu và hệ thống thông báo cho giảng viên.

Nộp sơ đồ tổ chức, đặc tả use case Duyệt lịch và hai NFR. Với tốc độ, đề xuất mục tiêu p95 không quá 500 ms khi 30 giảng viên tra lịch đồng thời trong 5 phút. Với quyền, yêu cầu mọi lần duyệt khác cơ sở trong bộ test âm phải bị chặn ở API. Ghi cách QA kiểm hai mục.

| Mục | Điểm | Cách chấm |
|---|---:|---|
| organization | 2 | 1 cho cấu trúc trung tâm–cơ sở. 1 cho vai trò và mức quản lý đúng cơ sở |
| uc-entry | 1.5 | 0,5 mỗi bằng chứng cho actor duyệt, điều kiện có yêu cầu chờ và điều kiện cùng cơ sở |
| normal-flow | 2 | 1 cho kiểm tra trùng và quyết định duyệt. 1 cho chuyển việc chuẩn bị đến đúng vai trò |
| exceptions | 2 | 1 cho từ chối và đề xuất lịch khác. 1 cho xử lý thiếu thiết bị sau duyệt |
| performance-nfr | 1.5 | 0,5 mỗi phần cho p95 500 ms, tải và thời lượng, cách đo với tiêu chí pass |
| access-nfr | 1 | 0,5 cho phạm vi lỗi khác cơ sở. 0,5 cho kiểm API với kết quả từ chối |

## SWD392 H01. Đặt lịch giao vật liệu tái chế

Người dùng chọn điểm thu gom, loại vật liệu và khung giờ. Một điểm có nhiều khung giờ, mỗi khung chứa số lượt tối đa. Một đặt lịch tham chiếu đúng một khung giờ và một người dùng. Người dùng tạo, xem hoặc hủy đặt lịch của mình. Tạo lịch thành công bắt đầu ở `RESERVED`. Nhân viên điểm thu gom chuyển sang `RECEIVED` sau khi nhận vật liệu. Chủ lịch hoặc nhân viên điểm có thể hủy ở `RESERVED`. `RECEIVED` và `CANCELED` là trạng thái kết thúc. Giữ chỗ phải thất bại nếu hết lượt, kể cả hai yêu cầu đến đồng thời.

Nộp ERD khái niệm, ERD logical với PK/FK, sequence cho thao tác tạo lịch và sơ đồ trạng thái. Sequence phải có xác thực, kiểm phạm vi sử dụng điểm thu gom, thao tác giữ chỗ nhất quán và nhánh từ chối.

| Mục | Điểm | Cách chấm |
|---|---:|---|
| conceptual | 2 | 1 cho các thực thể chính. 1 cho quan hệ điểm, khung giờ, đặt lịch và người dùng |
| logical | 2 | 1 cho PK và hai FK từ đặt lịch. 1 cho FK từ khung về điểm và biểu diễn sức chứa |
| sequence | 1.5 | 0,5 mỗi phần cho luồng UI–Controller, xử lý tại Service và truy cập dữ liệu qua Repository |
| access | 1.5 | 0,5 mỗi phần cho xác thực, kiểm phạm vi và nhánh từ chối trước ghi |
| capacity | 1 | 1 cho giữ chỗ có điều kiện hoặc transaction chống vượt sức chứa, có nhánh hết lượt |
| lifecycle | 2 | 1 cho đủ trạng thái. 1 cho đúng actor, điều kiện và hướng các chuyển tiếp |

## SWD392 H02. Ghép người hướng dẫn với nhóm sinh viên

Một Sinh viên có thể đăng ký nhiều Đợt hướng dẫn. Một đợt có nhiều sinh viên. Mỗi đăng ký có ngày nộp, trạng thái và ghi chú của người hướng dẫn. Trong mô hình lớp, thông tin này phải thuộc mối liên kết Sinh viên–Đợt hướng dẫn. Sinh viên đăng ký ở `PENDING`. Người hướng dẫn chấp thuận khi đợt còn chỗ, chuyển sang `ACCEPTED`. Nếu hết chỗ, yêu cầu vẫn chờ. Sinh viên rút yêu cầu lúc chờ, chuyển sang `WITHDRAWN`. Khi có thay đổi trạng thái, email và thông báo ứng dụng là hai nơi nhận độc lập. Có thể thêm nơi nhận mới mà không đổi logic đăng ký.

Nộp class diagram và statechart. Vẽ sequence hoặc communication diagram cho chấp thuận, ghi thứ tự thông điệp và nhánh hết chỗ. Chọn một design pattern thông báo và giải thích cơ chế. Đề xuất kiến trúc cho ứng dụng web và nêu một đánh đổi của lựa chọn.

| Mục | Điểm | Cách chấm |
|---|---:|---|
| class-relation | 2 | 1 cho quan hệ nhiều–nhiều. 1 cho lớp liên kết giữ đúng dữ liệu của đăng ký |
| state-model | 1.5 | 0,5 mỗi phần cho trạng thái đầu, trạng thái duyệt và trạng thái rút |
| state-guards | 1.5 | 0,5 mỗi phần cho guard còn chỗ, không thay đổi khi hết chỗ và rút lúc chờ |
| interaction | 2 | 1 cho đúng thứ tự/thành phần. 1 cho nhánh hết chỗ với kết quả giữ chờ |
| notification | 2 | 0,5 cho tên và họ pattern hợp lý. 1 cho cơ chế đăng ký và phát sự kiện. 0,5 cho hai nơi nhận và khả năng thêm mới |
| architecture | 1 | 0,5 cho phù hợp yêu cầu web và phân tách phần xử lý. 0,5 cho đánh đổi cụ thể có lý do |
