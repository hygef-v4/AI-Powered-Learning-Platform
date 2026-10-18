# Review B11 — REST API quản trị môn

> Ghi chú xuất bản18/10/2026: tài liệu này ghi bằng chứng của một checkpoint. Trạng thái và gate mới nhất ở [frontend-handoff](frontend-handoff.md); quyết định cần giữ ở [implementation-decisions](implementation-decisions.md). File TEMP đã được loại khỏi thay đổi xuất bản theo yêu cầu commit/push sau đó.

Lượt này nối bảy endpoint vào service U04 đã có, chuẩn hóa request/response và lỗi. Không cài JWT/session/CSRF của U01 hoặc provider U02/U05, không sửa SecurityConfig/Jackson/GlobalExceptionHandler chung. B12 và các gate bàn giao vẫn pending.

## Thứ tự đọc

1. [SubjectAdminController](../../../../backend/src/main/java/edu/aiplatform/subjects/web/SubjectAdminController.java): Actor chỉ lấy bằng AuthenticationPrincipal, controller gọi parser/service rồi trả DTO.
2. [SubjectRequests](../../../../backend/src/main/java/edu/aiplatform/subjects/web/SubjectRequests.java): whitelist JSON, type/presence và null; không dùng Jackson global ignore-unknown/coercion làm kiểm soát quyền sửa field.
3. [SubjectResponses](../../../../backend/src/main/java/edu/aiplatform/subjects/web/SubjectResponses.java): snapshot command → Detail, không trả entity JPA hoặc đọc lại sau commit.
4. [SubjectExceptionHandler](../../../../backend/src/main/java/edu/aiplatform/subjects/web/SubjectExceptionHandler.java): advice ưu tiên cao, chỉ controller U04, archive extensions và lỗi an toàn.
5. [SubjectAdminControllerTest](../../../../backend/src/test/java/edu/aiplatform/subjects/web/SubjectAdminControllerTest.java): MockMvc + service thật + repository/provider test doubles, trusted principal đặt phía server.

## API và tự kiểm

Prefix chung `/api/v1/admin/subjects`; chữ `{id}` dưới đây là UUID.

| Method/path | Input | Kết quả chính |
|---|---|---|
| GET prefix | q?, status?, page?, size? |200 Page; page0/size20 mặc định, max100; statusCounts theo q trước status filter |
| POST prefix | code, name, description?, managerId? |201, Location tương đối tới môn vừa tạo, Detail/public version1 |
| GET /{id} | UUID |200 Detail có contact/count khi đọc được; missing404 |
| GET /{id}/classes | UUID |200 AVAILABLE/items hoặc UNAVAILABLE/items=null; missing404 |
| PATCH /{id} | name, description, version |200 snapshot; description phải có, null nghĩa là xóa mô tả; chỉ hai field metadata được sửa |
| PUT /{id}/manager | managerId, version |200 snapshot; managerId bắt buộc xuất hiện, UUID hoặc null để bỏ phân công |
| PUT /{id}/status | status, version |200 snapshot; ACTIVE/ARCHIVED; stale409, lớp chưa archive409 có count/blockers, provider thiếu503 |

Input code/name/description vẫn được domain/service kiểm độ dài/format/nonblank và normalize. HTTP parser không ép number/bool/object thành chuỗi. Version phải là JSON integer từ1, không nhận chuỗi `"1"`, decimal1.0, null hoặc ngoài Long. managerId JSON phải UUID canonical (cho phép hex uppercase) hoặc null; không nhận chuỗi rỗng/UUID rút gọn. UUID path và enum/query number được Spring bind và báo400 khi sai.

PATCH có code/status/managerId, hay body có actorId/role/field lạ, bị400 trước ghi. Không âm thầm bỏ field khiến client tưởng đã sửa. PUT manager thiếu managerId không được hiểu thành bỏ phân công. Null khác missing được kiểm trực tiếp trên JsonNode.

GET list chỉ nhận q/status/page/size, mỗi param một giá trị; không nhận sort tùy ý vì sort cố định theo code ở repository. q giới hạn kỹ thuật200 ký tự tại SubjectRequests.query, tương ứng giới hạn tên môn; đây là giới hạn input web bổ sung, chưa phải câu SRS cũ. Có thể điều chỉnh tập trung nếu nhóm cần. Frontend foundation hiện gửi đúng bốn query param này.

## Response command khác GET detail ở đâu

Command trả snapshot vừa lưu, gồm id/code/name/status/managerId/description/timestamps/version. manager và classCount là null vì chưa enrichment; không giả0 lớp hoặc xóa managerId. GET detail sau đó tra contact/classes như B07. Frontend cần refetch/invalidate dữ liệu khi lưu thành công hoặc hiển thị unknown đúng cách.

Không gọi lại GET detail sau command để tránh tình huống command đã commit nhưng lần đọc bổ sung lỗi làm client tưởng save thất bại rồi gửi lại. Controller không tự gọi thêm authorize hoặc ghi audit lần thứ hai cho cùng command. Các test HTTP new object service không chứng minh transaction; bằng chứng proxy/rollback có ở PostgreSQL tests của các lượt trước.

## Lỗi

- ApiException giữ status/code/detail nghiệp vụ. InvalidRequest thêm fieldErrors an toàn, không phản chiếu giá trị body. Validation domain vẫn có code/detail riêng, không hứa mọi400 đều có fieldErrors.
- Archive rejection thêm unarchivedCount và blockingClasses, chỉ sau service đã kiểm Admin. Non-admin không truy vấn provider hoặc nhận blockers.
- JSON hỏng, sai UUID/enum/number →400 SUBJECT_REQUEST_INVALID; missing môn404; stale409; missing guard/checker503.
- Unique constraint mã đã được command dịch thành SUBJECT_CODE_DUPLICATE; integrity khác là SUBJECT_DATA_CONFLICT409, không bị gọi nhầm là duplicate.
- Storage lỗi503 SUBJECT_STORAGE_UNAVAILABLE; lỗi không dự kiến500 SUBJECT_INTERNAL_ERROR. Không đưa SQL/password/cause/stack trace vào response.
- Advice chỉ áp dụng SubjectAdminController và có ưu tiên trước GlobalExceptionHandler để không mất archive extensions; không thay handler của unit khác. Lỗi từ security filter xảy ra trước controller vẫn do U01/common security xử lý.

## Bằng chứng và giới hạn bảo mật

Lần chạy cuối 18/10/2026: toàn Maven271 test PASS,0 failures/errors/skipped;32 MockMvc HTTP consumer cases,31 PostgreSQL16.15 regression cases và3 rule ArchUnit đều pass. Lượt tập trung đầu tiên30 HTTP cases pass rồi bổ sung filter/paging và explicit-null unassign. PostgreSQL container loopback đã dọn. Không thêm dependency, không sửa frontend/Git index hoặc code U01/U02/U05.

Test HTTP dùng MockMvc standalone, AuthenticationPrincipalArgumentResolver và TestingAuthenticationToken chứa Actor tạo phía server. Test không tạo JWT, không thêm spring-security-test, không chạy filter chain, không vô hiệu hóa filter của production. SecurityContext được clear sau từng case. Có case wrong principal/anonymous, header X-Role giả và body role/actorId bị từ chối; không suy ra chữ ký JWT/sid/current account ACTIVE/CSRF đã được kiểm.

Shared SecurityConfig vẫn yêu cầu authenticated cho các route này, giữ CSRF mặc định. Khi U01 chưa có JWT filter/provider thật, chưa thể coi app đã nhận token/cookie hoặc trả401/403 JSON đúng contract end-to-end. B12 phải kiểm với chain thật; test standalone401 chỉ là hành vi service/controller khi không nhận được Actor.

Binding/parser có thể trả400 trước khi service được gọi; các lỗi đó không tạo ACCESS_DENIED ở helper. Chúng không đọc dữ liệu môn. Audit/auth cho đường bị chặn bởi filter, thứ tự xử lý và tránh ghi trùng U01/U04 vẫn cần tích hợp B12.

Chạy riêng `mvn -f backend/pom.xml "-Dtest=SubjectAdminControllerTest" test`. Phần PostgreSQL là regression độc lập, không phải HTTP+DB+JWT end-to-end. Frontend chưa thay đổi; types/API F01 đã dùng đúng path/nullable response, các màn hình còn cần triển khai.

## Bước tiếp theo

B12 nhận/test security U01 khi provider sẵn sàng, có thể chuẩn bị consumer checks trước. Sau đó tiếp tục route/màn hình frontend theo F02–F08 và test. B03 migration thật, B08 Q14/account guard, B09 U05 checker/writers, B10 audit U02/dedup, NFR và tester độc lập vẫn pending. Không mở fallback allow hoặc dữ liệu mẫu production để làm plan toàn dấu x. Hai file TEMP phải dọn trước PR vào main; chưa commit/push/Jira hoặc nhắn thành viên.
