# Review B07 — Service đọc quản trị môn học

> Ghi chú xuất bản18/10/2026: tài liệu này ghi bằng chứng của một checkpoint. Trạng thái và gate mới nhất ở [frontend-handoff](frontend-handoff.md); quyết định cần giữ ở [implementation-decisions](implementation-decisions.md). File TEMP đã được loại khỏi thay đổi xuất bản theo yêu cầu commit/push sau đó.

**Cập nhật B10:** helper quyền đã gọi audit denied; constructor SubjectAdminService nhận thêm SubjectAuditRecorder và Clock. Các số test bên dưới là lịch sử B07; bằng chứng mới và transaction xem audit-review.md.

Phạm vi UC16/18, BR-U04-01/05/50/54 và Q05/Q08/Q12. Chỉ thêm service đọc danh sách, chi tiết và danh sách lớp; chưa có controller, màn hình hoặc command. BR-U04-51 audit denied còn ở B10, vì vậy chưa đánh hoàn thành toàn UC hoặc service F1–F6 trong plan chính thức.

## Thứ tự đọc code

1. [SubjectAdminViews](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectAdminViews.java): DTO riêng cho web, không sửa SubjectRef dùng nội bộ.
2. [SubjectAdminService](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectAdminService.java): đọc access.require trước, rồi list/detail/classes và managers. Lượt B08 đã tách kiểm quyền sang [SubjectAdminAccess](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectAdminAccess.java) dùng chung cho đọc/ghi, giữ hành vi B07.
3. [SubjectClassesReader](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectClassesReader.java): deadline, cancel và hai dạng dữ liệu không khả dụng.
4. [SubjectAdminReadTest](../../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectAdminReadTest.java), [SubjectClassesReaderTest](../../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectClassesReaderTest.java) và [wiring test](../../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectContractsWiringTest.java).

## Luồng đọc và tiêu chí tự kiểm

| Phần | Hành vi | Case để tự kiểm |
|---|---|---|
| access.require | Actor phải do server dựng; cần accountId và role ADMIN, rồi hỏi IAuthorization mỗi lần | Anonymous=401; non-admin=403 dù stub allow; U01 deny=403; U01 lỗi/null=503. Các nhánh này không gọi repository/contact/classes |
| list | Default page0/size20, size1–100, trim q, status enum; repository sắp code | Trang2/size10 được chuyển đúng xuống repository; count dùng q+status; statusCounts chỉ dùng q theo Q08 |
| managers | distinct managerId trong trang, getContact tối đa một lần/ID/request; không cache liên request | Hai môn cùng manager chỉ tra một lần; lần request sau tra lại. Null managerId không gọi lookup |
| detail | Missing=404 sau kiểm quyền; trả description, timestamps và public version | Không có môn thì không gọi U05; lỗi DB truyền lên handler chung, không giả 404 hoặc dữ liệu rỗng |
| classes | Kiểm Admin và môn tồn tại trước gọi provider, không tạo quyền sửa lớp | AVAILABLE + [] là không có lớp đã xác minh; lỗi/timeout/null list là UNAVAILABLE + null |

managerId khác null nhưng manager=null nghĩa là contact thiếu/lỗi/không khớp ID. Giữ assignment đó để UI hiện “không tra được”, không hiển thị “chưa phân công”. Tên/email chỉ để hiển thị; không dùng contact lookup thay authorization hoặc kiểm manager hợp lệ khi gán ở B08.

classCount=null nghĩa là không kiểm được; 0 chỉ được trả khi provider xác nhận0. Một trang chỉ gọi countClasses một lần với đúng ID của trang, không gọi từng môn. Missing key/null/negative count giữ unknown; không dùng mặc định0. Trang rỗng không gọi contact/classes provider. DTO có kiểu tương ứng frontend foundation; B11 sẽ kiểm JSON HTTP thực tế.

## Deadline hoạt động thế nào

Reader có executor dùng chung trong bean: tối đa4 worker, queue16, từ chối task khi đầy; không tạo thread theo từng request. Mỗi lần gọi chờ tối đa1s, gồm thời gian trong queue. Hết hạn/interruption thì cancel và dọn task bị hủy khỏi queue; caller bị interrupt giữ lại interrupt flag. Bean đóng bằng PreDestroy/shutdownNow.

Cancel không cưỡng chế dừng SQL/network. Provider U05 phải đặt timeout và đóng tài nguyên của mình; nếu bỏ qua interrupt, tối đa4 worker có thể còn bận, các lần đọc sau trả unavailable. Chưa đo tải hay xác nhận query timeout của U05 thật. Timeout test dùng latch chặn provider và deadline100ms để kiểm cancel; không suy ra p95 production từ test này.

Không chuyển transaction, EntityManager hay SecurityContext của caller sang worker. ISubjectClasses là contract đọc nội bộ, được gọi sau kiểm Admin; provider dùng transaction/connection riêng. Reader này dành cho hiển thị, không dùng để quyết định archive trong transaction của B09.

## Những giới hạn cần giữ rõ khi tích hợp

- Action hiện dùng SUBJECT_LIST với ResourceRef.none và SUBJECT_VIEW với ResourceRef.subject(id), bao gồm bảng lớp chỉ đọc. Đây là tên tại consumer U04; cần đối chiếu action registry của U01 khi bàn giao, chưa có bằng chứng owner U01 đã nhận/chấp nhận. Không sửa implementation hoặc contract signature U01 trong lượt này.
- U01 chịu trách nhiệm current session, account ACTIVE và role hiện thời. IdentityContractStubs vẫn allow; test mock deny minh họa tài khoản disabled, chưa chứng minh JWT/session revoke thật. Chưa mở route HTTP, không được coi context tối thiểu là boot production với auth hoàn chỉnh.
- Audit ACCESS_DENIED chưa ghi ở đây; B10 phải bổ sung một lần tại tầng đã thống nhất với U01/U02. Lifecycle guard, command, optimistic write, Q14 disable tự gỡ và OTP/email còn ở bước sau.
- Các query page/count/countByStatus chưa ở cùng snapshot transaction: dữ liệu có thể thay đổi giữa các lần đọc khi có người sửa đồng thời. Chưa hứa counts và page nhất quán tuyệt đối. Không giữ transaction môn trong lúc chờ U05.
- Contact lookup hiện tối đa100 manager khác nhau/trang, mỗi ID một lần; chưa có batch API hoặc deadline contact ở consumer. Q12 vẫn là hướng kỹ thuật đề xuất, chưa đo p95/list300ms hoặc xác nhận cần mở rộng U01. Không xem contact lỗi là lỗi auth.
- Đã sửa câu NFR cũ “stub trả0/rỗng” theo Q05. Không thay policy hoặc yêu cầu người dùng quyết định lại.

## Chạy lại và bước kế tiếp

Lần chạy cuối ngày18/10/2026: toàn backend166 test PASS,0 failures/errors/skipped; gồm15 admin read,6 classes reader,4 wiring (1 mới),16 PostgreSQL16.15 và3 rule ArchUnit. PostgreSQL cases là regression của persistence/query/scope, không phải test tích hợp Admin với provider thật. Container loopback riêng đã xác minh dọn sau test; không thêm dependency hoặc đổi frontend/config chung. `git diff --check` kiểm trước bàn giao.

Từ repo chạy `mvn -f backend/pom.xml "-Dtest=SubjectAdminReadTest,SubjectClassesReaderTest,SubjectContractsWiringTest" test` để kiểm service/deadline/bean. DB checks dùng môi trường test riêng và điều kiện an toàn trong persistence-review.md; không trỏ vào database dev chung. Test do developer viết chưa thay kiểm tích hợp độc lập của tester.

Sau review B07, B08 sẽ thêm tạo/sửa thông tin và gán/bỏ chủ nhiệm. Guard tài khoản và lifecycle cần phối hợp U01/U05 theo plan; không tự cài nghiệp vụ của các unit đó. Hai file kế hoạch TEMP vẫn phải dọn trước PR vào main. Không commit/push/Jira trong lượt này.
