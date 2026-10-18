# Review B09 — Archive/reopen và khóa vòng đời môn

> Ghi chú xuất bản18/10/2026: tài liệu này ghi bằng chứng của một checkpoint. Trạng thái và gate mới nhất ở [frontend-handoff](frontend-handoff.md); quyết định cần giữ ở [implementation-decisions](implementation-decisions.md). File TEMP đã được loại khỏi thay đổi xuất bản theo yêu cầu commit/push sau đó.

Phạm vi UC19, BR-U04-04/07/52/54; Q05/Q06/Q10/Q11/Q12. Đây là phần U04 để review. U05 chưa nối writer/provider thật; B09 tổng và integration gate vẫn chưa hoàn thành. Không tạo endpoint HTTP hoặc code nghiệp vụ lớp U05.

## Thứ tự đọc

1. [SubjectStatusService](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectStatusService.java): changeStatus kiểm Admin → validate → khóa môn → kiểm version/no-op → kiểm lớp nếu archive → CAS → success audit.
2. [ISubjectMutationGuard](../../../../backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectMutationGuard.java) và [SubjectMutationGuard](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectMutationGuard.java): guard U04 cài để U05 gọi trước tạo/mở/mở lại lớp.
3. [ISubjectArchiveCheck](../../../../backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectArchiveCheck.java): provider U05 cần cung cấp; nhận ngân sách còn lại, trả count và blockers. Chưa có production implementation hoặc stub trả0.
4. [SubjectRepositoryAdapter](../../../../backend/src/main/java/edu/aiplatform/subjects/infrastructure/SubjectRepositoryAdapter.java): overload findByIdForUpdate có Duration, transaction MANDATORY và timeout PostgreSQL; không thay overload B08.
5. [SubjectStatusTest](../../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectStatusTest.java), [SubjectMutationGuardTest](../../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectMutationGuardTest.java) và [PostgreSQL checks](../../../../backend/src/test/java/edu/aiplatform/subjects/infrastructure/SubjectRepositoryIntegrationTest.java).

## Luồng và cách tự kiểm

| Luồng | Kết quả | Case cần đọc |
|---|---|---|
| Archive, verified count0 | ACTIVE→ARCHIVED, version tăng1, giữ manager và lịch sử lớp | Không có lớp hoặc mọi lớp ARCHIVED đều hợp lệ; isActive=false, scope manager vẫn giữ |
| Archive, OPEN/DRAFT |409 SUBJECT_HAS_UNARCHIVED_CLASSES, không update/audit success | Count tổng và blockers chỉ đọc được giữ trong SubjectArchiveRejectedException để B11 ánh xạ; chưa có HTTP extensions |
| Archive, provider thiếu/lỗi/null |503 SUBJECT_CLASSES_UNAVAILABLE; không suy ra0 | Có ISubjectClasses legacy trả0 cũng không đủ để archive |
| Reopen | ARCHIVED→ACTIVE, version tăng1 | Không gọi provider lớp, không đổi trạng thái lớp hoặc tạo lớp mới |
| Status giống hiện tại | Version đúng thì no-op; version cũ409 | Không gọi provider hoặc success audit; vẫn lấy khóa môn và kiểm version |
| Mutation guard | Chỉ trả SubjectRef khi môn ACTIVE và đã giữ khóa | Missing404, archived409 SUBJECT_ARCHIVED, lock lỗi503; không có transaction thì lỗi MANDATORY |

Ví dụ version3 ACTIVE → archive thành version4 ARCHIVED → reopen thành version5 ACTIVE. Một request archive version3 gửi lại sau lần đầu bị409, không được coi là no-op hợp lệ.

Guard chỉ bảo đảm trạng thái và thứ tự ghi, không cấp quyền người dùng. U05 vẫn phải kiểm actor/session/role/scope qua U01. Thứ tự chung: các khóa account nếu có → khóa subject → ghi class. Không lấy khóa class rồi quay lại subject trong writer có thể cạnh tranh archive.

## Vì sao khóa lạc quan chưa đủ

Version subject chặn hai người ghi cùng môn, nhưng tạo lớp có thể không sửa version môn. Archive kiểm0 lớp rồi tạo lớp chạy vào khoảng trống trước commit vẫn nguy hiểm nếu hai thao tác không giữ chung khóa subject.

Khi writer lớp giữ khóa trước, archive chờ; writer commit rồi archive đọc lớp phải thấy lớp mới và từ chối. Khi archive giữ khóa trước, writer chờ; archive commit rồi writer kiểm ACTIVE phải thấy ARCHIVED và từ chối tạo/mở lớp. Mọi writer tạo/open/reopen U05 phải tham gia giao thức thì lập luận này mới đúng.

Transaction cần READ_COMMITTED để truy vấn lớp sau khi lấy được khóa thấy dữ liệu vừa commit. Service status khai báo isolation đó; overload repository kiểm transaction_isolation thực tế và từ chối isolation khác, kể cả khi service bị gọi trong outer transaction REPEATABLE_READ. Không lấy dữ liệu lớp trước khóa, không cache count, không mở transaction REQUIRES_NEW hoặc truyền EntityManager sang worker.

## Ngân sách thời gian và giới hạn thực tế

SubjectLifecycleBudget dùng System.nanoTime, ngân sách lock+check tối đa1s; Clock inject chỉ dùng timestamp nghiệp vụ/audit. Overload repository đặt lock_timeout và statement_timeout transaction-local trước SELECT FOR UPDATE, giữ giá trị nhỏ hơn nếu caller đã đặt timeout chặt hơn, rồi khôi phục giá trị cũ sau query thành công. Không sửa connection timeout toàn cục. Query lỗi thì transaction phải rollback; không chạy restore SQL trên transaction đã aborted để che lỗi gốc.

Sau lấy khóa, provider nhận remainingBudget, chạy trực tiếp trên caller thread/transaction. Provider phải tự đặt query/lock timeout cho tổng thao tác và khôi phục setting khi thành công; không được đặt lại1s cho từng query. U04 kiểm ngân sách trước provider và sau khi provider trả: kết quả0 trả muộn không cho phép archive. Lỗi/timeout làm rollback, khóa DB được nhả.

**Giới hạn:** lời gọi Java đồng bộ không thể cưỡng chế dừng provider bỏ qua deadline. Không chuyển sang async để “đủ1s” vì sẽ mất transaction/khóa chung. Chưa có provider U05 thật nên chưa chứng minh thời gian đáp ứng của nó. Deadline này dành cho lock/kiểm lớp; không phải cam kết toàn request/auth/audit/commit luôn xong trong1s. Không tuyên bố đạt NFR p95 từ unit test.

ISubjectClasses hiện có không nhận budget hoặc quy định transaction; giữ nguyên signature cho consumer cũ và hiển thị B07. Contract ISubjectArchiveCheck bổ sung là đề xuất U04-owned cần U05 xác nhận cùng với ISubjectMutationGuard; chưa có bằng chứng owner đã nhận hoặc sửa writer. Chưa có provider mới thì archive fail closed503, reopen vẫn chạy. Reader async B07 chỉ phục vụ hiển thị và không được dùng quyết định archive.

## Audit và bằng chứng cần phân biệt

Lần chạy cuối ngày18/10/2026: toàn Maven228 test PASS,0 failures/errors/skipped; gồm29 PostgreSQL16.15 case,18 unit status,4 unit mutation guard,6 wiring (1 mới) và3 rule ArchUnit. Có8 DB case mới nêu bên dưới; container loopback test đã dọn. Không thêm dependency, không sửa U01/U05 implementation, shared auth/config hoặc frontend. Git index giữ nguyên.

SubjectAuditRecorder thêm SUBJECT_ARCHIVED/SUBJECT_REOPENED cùng transaction, details before/after status, Clock-derived timestamp. No-op/rejected/unavailable không ghi success mutation. Các PostgreSQL test làm audit mock ném lỗi để kiểm rollback trạng thái/version; chưa chứng minh durable atomic audit U02 thật. DENIED/FAILURE và phối hợp tránh audit lặp vẫn pending B10.

Fixture test tạo bảng u04_lifecycle_classes chỉ trong schema test, không nằm trong migration. Provider fixture truy vấn bảng đó trong transaction thật và áp dụng SQL timeout; không phải implementation U05. Hai thứ tự lock và SQL timeout/lock timeout là bằng chứng giao thức U04 với writer hợp tác, chưa thay concurrency test class create/open/reopen thật U05. Accounts fixture và IAuthorization/IAudit vẫn là test doubles như các lượt trước.

Chạy unit: `mvn -f backend/pom.xml "-Dtest=SubjectStatusTest,SubjectMutationGuardTest,SubjectContractsWiringTest" test`. DB checks cần cấu hình môi trường test riêng như persistence-review.md; thiếu U04_TEST_JDBC_URL thì các case DB bị skip, không được báo đã kiểm DB. Frontend chưa thay đổi.

## Việc còn lại sau review

- U05 xác nhận hai contract và cài archive provider, nối guard vào mọi class create/open/reopen, kiểm lock order/READ_COMMITTED/deadline với writer thật.
- U01 guard account, Q14 disable tự gỡ, session/JWT và action registry SUBJECT_STATUS_CHANGE vẫn chưa tích hợp.
- B10 durable/denied audit, B11 HTTP error extensions/whitelist/auth principal, frontend và tester độc lập còn pending. Không đánh hoàn thành service F1–F6 hoặc toàn UC chỉ nhờ fixtures.
- Hai plan TEMP phải dọn trước PR vào main; chưa commit/push/Jira hoặc nhắn owner thay người dùng.
