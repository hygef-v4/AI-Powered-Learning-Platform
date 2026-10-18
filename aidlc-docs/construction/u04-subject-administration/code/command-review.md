# Review B08 — Tạo/sửa môn và thay phân công

> Ghi chú xuất bản18/10/2026: tài liệu này ghi bằng chứng của một checkpoint. Trạng thái và gate mới nhất ở [frontend-handoff](frontend-handoff.md); quyết định cần giữ ở [implementation-decisions](implementation-decisions.md). File TEMP đã được loại khỏi thay đổi xuất bản theo yêu cầu commit/push sau đó.

Lượt này triển khai phần command U04 để review, không đánh hoàn thành toàn B08/B08-D hoặc B10. Không có HTTP endpoint mới. Provider khóa account U01 và audit bền vững U02 vẫn chưa có trong checkout; không suy ra tình trạng các nhánh của thành viên khác.

## Đọc theo thứ tự

1. [SubjectAdminAccess](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectAdminAccess.java): tách nguyên kiểm quyền B07 để đọc/ghi dùng cùng logic. Constructor service đọc giữ nguyên, test B07 vẫn áp dụng.
2. [SubjectCommandService](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectCommandService.java): ba command có transaction, Clock và thứ tự validate/lưu/audit.
3. [ISubjectManagerAssignmentGate](../../../../backend/src/main/java/edu/aiplatform/subjects/application/ISubjectManagerAssignmentGate.java): port nội bộ U04 chờ adapter tới contract U01. Không có provider mặc định hoặc stub allow; chưa sửa file/implementation U01.
4. [SubjectAuditRecorder](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectAuditRecorder.java): ba success hook, MANDATORY transaction, details allowlist và null manager tường minh.
5. [Command test](../../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectAdminCommandTest.java), [PostgreSQL test](../../../../backend/src/test/java/edu/aiplatform/subjects/infrastructure/SubjectRepositoryIntegrationTest.java) và [wiring test](../../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectContractsWiringTest.java).

Tách SubjectCommandService khỏi SubjectAdminService thay vì dồn vào file B07 như đường dẫn ban đầu trong plan. Mục đích là giữ transaction ghi gọn: không chờ count lớp hoặc lookup contact phục vụ hiển thị trong transaction command. Controller B11 sẽ gọi command rồi ánh xạ snapshot sang DTO; Subject trả về là domain bất biến nội bộ, không phải entity JPA và chưa phải JSON HTTP contract.

## Ba command

| Command | Luồng và business rule | Tự kiểm |
|---|---|---|
| create | Admin → validate code/name/description → duplicate precheck → nếu có manager thì khóa account và kiểm SUBJECT_MANAGER ACTIVE → insert ACTIVE/version1 → audit | UC17; BR-U04-02/03/53. Code trim/uppercase; null manager không cần gate; duplicate cạnh tranh vẫn do unique constraint chặn |
| updateInformation | Admin → version bắt buộc → đọc môn → kiểm version trước no-op → chỉ đổi name/description → CAS → audit | UC19; BR-U04-06; Q10/Q11. Archived vẫn được sửa; code/manager/status/createdAt giữ nguyên; stale luôn409 |
| changeManager | Admin → đọc snapshot sơ bộ/kiểm version → nếu thực sự đổi thì khóa old/new account → khóa và đọc lại môn → kiểm lại version → kiểm contact mới → CAS → audit | UC19; BR-U04-03/10; Q14. Có thể gán/đổi/bỏ khi provider gate sẵn sàng; stale trong lúc chờ khóa phải409 |

Command không nhận status/code trong update metadata. B11 vẫn phải kiểm whitelist JSON để từ chối field lạ; Java record riêng chưa chứng minh request HTTP không thể chèn code/status. Không có sửa role tài khoản, OTP/email hoặc gọi nghiệp vụ lớp U05 ở đây.

Public version tăng đúng một cho mutation; valid no-op giữ nguyên version và không ghi audit thay đổi. Version thiếu hoặc nhỏ hơn1 là400; version không khớp là409 ngay cả khi dữ liệu request trùng giá trị hiện tại. Metadata/manager của môn archived vẫn sửa được theo Q10, không tự reopen.

Chỉ unique constraint `uk_subjects_code` trong DataIntegrityViolationException tại insert được chuyển thành SUBJECT_CODE_DUPLICATE409. Không biến FK/check/constraint khác thành mã trùng. DB lỗi khác vẫn được đẩy lên; chuẩn hóa HTTP lỗi an toàn còn ở B11.

## Tại sao cần gate và đọc lại dưới khóa

IAccountLookup chỉ trả thông tin, không giữ account ACTIVE đến commit. U01 có thể disable/đổi role giữa lúc đọc contact và lưu môn. Gate thật phải khóa account cùng transaction PostgreSQL, giữ đến commit/rollback, và phối hợp cùng giao thức với disable/đổi role U01.

Khi đổi/bỏ manager, U04 gom old/new ID khác null, distinct và sort theo Java UUID.compareTo. Adapter phải giữ đúng thứ tự đó; U01 cần cùng quy ước khi khóa nhiều account. Account cũ có thể đã disabled: gate chỉ khóa account, kiểm eligibility chỉ áp dụng account mới. Sau đó repository.findByIdForUpdate lấy khóa PESSIMISTIC_WRITE và đọc lại môn; version được kiểm lại để phát hiện phân công đổi trong lúc chờ khóa. Detach entity không nhả khóa DB; transaction caller còn giữ nó.

`findByIdForUpdate` và success audit recorder dùng Propagation.MANDATORY để không tự mở rồi đóng transaction con trước command. Không dùng async worker của SubjectClassesReader cho command, không gọi command bằng self-invocation để né Spring proxy.

**Khi thiếu provider gate:** tạo có manager và mọi thay đổi phân công thực sự, kể cả bỏ manager, trả SUBJECT_ASSIGNMENT_GUARD_UNAVAILABLE503 trước ghi DB/audit. Tạo không manager và sửa metadata vẫn chạy; same-manager no-op với version đúng không cần gate. Không có endpoint HTTP nào được mở bởi lượt này.

Đây là boundary nội bộ chờ nối, không phải contract U01 đã bàn giao. `IAccountAssignmentGuard` và `ISubjectManagerAssignments` trong plan phối hợp vẫn chưa được U01 khai báo/triển khai tại checkout. Chưa triển khai Q14 tự gỡ tất cả môn khi disable, lock account/status thật, session revoke, enable không reassign hoặc concurrency gán/disable thật. Không viết trực tiếp SQL bảng accounts trong production U04 để thay owner U01.

## Audit và transaction

Đã nối phần B10 cần ngay cho command: SUBJECT_CREATED (code/managerId), SUBJECT_UPDATED (changedFields, không nội dung description), SUBJECT_MANAGER_CHANGED (oldManagerId/newManagerId, có null tường minh). UUID event, actor lấy Actor server, objectType SUBJECT, objectId môn, timestamps dùng Clock/domain snapshot. Không ghi email/token/OTP vào details. Mutation phải gọi hook sau khi save thành công, trước commit; exception audit không bị nuốt.

AuditContractStubs hiện chỉ log, không chứng minh lưu audit bền vững. Test PostgreSQL dùng mock IAudit cố ý ném lỗi để chứng minh transaction command rollback insert/update/manager change. Không có bằng chứng bảng audit U02 ghi cùng commit, không thay test propagation U02 thật. Audit denied/failure bằng transaction riêng, tránh ghi lặp với U01, status actions và reason ACCOUNT_DISABLED vẫn ở B10/B08-D.

## Kiểm thử và giới hạn

Lần chạy cuối 18/10/2026: `mvn -f backend/pom.xml clean test` PASS197 test,0 failures/errors/skipped; gồm25 command test,5 wiring (1 mới),21 PostgreSQL16.15 case (5 mới) và3 rule ArchUnit. Có test command tạo cùng mã đồng thời: một thành công, một SUBJECT_CODE_DUPLICATE, một môn và một success audit hook. Test không ép buộc cả hai request luôn vượt precheck; unit riêng kiểm dịch đúng tên unique constraint. Container loopback test đã dọn; clean build cũng loại class cũ sau khi đổi tên interface theo convention Ixxx.

Chạy riêng `mvn -f backend/pom.xml "-Dtest=SubjectAdminCommandTest,SubjectAdminReadTest,SubjectContractsWiringTest" test` để kiểm policy/wiring. Unit command gọi object trực tiếp nên không chứng minh annotation transaction; bằng chứng rollback/khóa dùng command Spring proxy với PostgreSQL thật trong SubjectRepositoryIntegrationTest.

Fixture DB vẫn chỉ có accounts UUID PK. Provider gate trong test chỉ khóa row fixture bằng SELECT FOR UPDATE; contact role/status dùng mock. Các test đó kiểm orchestration/transaction U04, không phải implementation eligibility hoặc disable U01 thật. Không đổi fixture thành migration production. Thiếu env DB test thì các DB case bị skip; không báo kiểm DB nếu chưa cấu hình đúng môi trường an toàn trong persistence-review.md.

Action consumer mới SUBJECT_CREATE/SUBJECT_UPDATE/SUBJECT_MANAGER_CHANGE cần đối chiếu registry U01; actor role/session/current ACTIVE vẫn qua U01. Chưa đo NFR p95/lock wait hoặc xác nhận timeout của guard thật. Không thêm dependency, frontend, shared auth/config hay Git staging.

Sau review, tiếp tục nối provider account guard/disable theo B08-D khi contract sẵn sàng, hoặc sang B09 lifecycle/archive với giới hạn tích hợp được ghi rõ. B08 và B10 chỉ được đánh hoàn thành toàn bộ khi có đủ provider/atomic audit evidence. Hai kế hoạch TEMP vẫn phải dọn trước PR vào main; không commit/push/Jira trong lượt này.
