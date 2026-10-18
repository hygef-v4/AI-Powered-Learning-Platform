# Review B10 — Audit tại consumer U04

> Ghi chú xuất bản18/10/2026: tài liệu này ghi bằng chứng của một checkpoint. Trạng thái và gate mới nhất ở [frontend-handoff](frontend-handoff.md); quyết định cần giữ ở [implementation-decisions](implementation-decisions.md). File TEMP đã được loại khỏi thay đổi xuất bản theo yêu cầu commit/push sau đó.

Người dùng muốn tiếp tục hoàn thiện code-generation-plan khi các unit chưa chốt bàn giao. Hướng thực hiện là viết phần U04 theo boundary đã có/đề xuất, kiểm bằng test doubles có ghi rõ, rồi thay provider khi nhóm thống nhất. Không chặn toàn bộ việc code; cũng không đánh hoàn thành integration/DoD dựa trên mock.

Lượt này bổ sung ACCESS_DENIED vào helper quyền chung; năm success hook của B08/B09 giữ nguyên. B10 tổng vẫn pending audit U02 thật, phối hợp U01 chống ghi trùng, Q14 và tester độc lập. Không sửa implementation U01/U02 hoặc mở HTTP route.

## Thứ tự đọc

1. [SubjectAdminAccess](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectAdminAccess.java): mọi nhánh từ chối đều đi qua reject, gọi audit một lần rồi ném lỗi; không truy vấn repository khi bị từ chối.
2. [SubjectAuditRecorder](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectAuditRecorder.java): success mặc định MANDATORY, accessDenied override NOT_SUPPORTED và giao việc ghi transaction riêng cho IAudit U02.
3. [SubjectAuditTest](../../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectAuditTest.java): event, privacy, một event mỗi lần từ chối, audit failure vẫn đóng quyền, không ghi DENIED khi allow.
4. [PostgreSQL checks](../../../../backend/src/test/java/edu/aiplatform/subjects/infrastructure/SubjectRepositoryIntegrationTest.java): hai case fixture kiểm SUCCESS atomic và DENIED tồn tại sau command rollback.

## Payload và cách tự kiểm

| Tình huống | Audit / lỗi trả về | Tự kiểm |
|---|---|---|
| Anonymous hoặc thiếu accountId | ACCESS_DENIED / DENIED, reason AUTHENTICATION_REQUIRED;401 | actorId có thể null; list không có objectId; không gọi IAuthorization/repository |
| Non-admin hoặc U01 deny | ACCESS_DENIED / DENIED, reason SUBJECT_FORBIDDEN;403 | actorId từ Actor server; không copy reason nội bộ U01 |
| U01 lỗi/null decision | ACCESS_DENIED / DENIED, reason SUBJECT_AUTHORIZATION_UNAVAILABLE;503 | Đây là một yêu cầu bị chặn vì không xác minh được quyền; không suy ra U01 đã quyết định deny |
| Ghi audit của từ chối bị lỗi | SUBJECT_AUDIT_UNAVAILABLE503 | Vẫn chặn quyền; một lần thử ghi, không nuốt lỗi rồi cho thao tác tiếp; không lộ SQL/cause |
| Allow | Không ghi ACCESS_DENIED | Mutation thành công vẫn dùng hook tương ứng; read thành công không tạo audit mutation |

Event có UUID mới, objectType SUBJECT, objectId UUID môn nếu có, timestamp từ Clock. Details chỉ attemptedAction và scope. Action là hằng nội bộ U04, không lấy từ body/query. Không ghi token/OTP/email/name/description/request body hoặc AuthorizationDecision.reason. Các success payload trước đó vẫn là code/managerId, changedFields, old/newManagerId và before/after status theo allowlist.

## Transaction: vì sao không gọi chung một kiểu

SUCCESS phải đi cùng commit/rollback môn, nên recorder mặc định Propagation.MANDATORY. Nếu audit provider ném lỗi, command rollback; không retry ghi success ở transaction riêng.

DENIED cần còn sau khi command ném lỗi. accessDenied có Propagation.NOT_SUPPORTED: Spring tạm dừng transaction caller trước khi gọi IAudit. U02 vẫn phải tự lưu DENIED trong transaction riêng theo contract, kể cả read không có transaction. NOT_SUPPORTED không tự tạo một transaction ghi mới và không thay implementation U02.

Helper nhận bean SubjectAuditRecorder do Spring inject; không tự new recorder trong production hoặc gọi method annotated qua this. Unit test new object trực tiếp chỉ kiểm logic; PostgreSQL test dùng proxy thật để xác nhận suspension/transaction.

Audit provider/stub hiện có phải phân biệt: AuditContractStubs chỉ log. Không được coi log stub là lưu bền vững hoặc bằng chứng propagation U02 đúng.

## Bằng chứng fixture và giới hạn

Lần chạy cuối 18/10/2026: toàn Maven239 test PASS,0 failures/errors/skipped,31 PostgreSQL16.15 case;9 unit audit và2 DB case mới,3 rule ArchUnit đều pass. Lần chạy tập trung audit/read/command/status/wiring trước đó73 pass. Container test loopback đã dọn. Không thay dependency, migration production, U01/U02/U05 implementation, frontend hoặc Git index.

- SUCCESS fixture dùng JdbcTemplate cùng transaction command: một lần create commit cả subject/audit row; lỗi sau audit insert làm rollback cả hai.
- DENIED fixture kiểm caller transaction đã bị suspend, mở REQUIRES_NEW ghi audit row, rồi command/outer transaction rollback; một DENIED row vẫn còn và không có subject mới.
- Bảng u04_audit_events chỉ được tạo trong schema test, không có migration production; provider là mock delegate tới fixture. Đây là bằng chứng giao tiếp U04 đúng mô hình transaction, không phải U02 audit schema/provider thật, retry/idempotency hoặc log retention.
- Các regression đọc/command/status vẫn kiểm auth trước dữ liệu và đã bổ sung assertion DENIED thay vì kỳ vọng không gọi audit.

Chạy riêng `mvn -f backend/pom.xml "-Dtest=SubjectAuditTest,SubjectAdminReadTest,SubjectAdminCommandTest,SubjectStatusTest,SubjectContractsWiringTest" test`. DB tests cần môi trường riêng như persistence-review.md; thiếu U04_TEST_JDBC_URL thì DB cases bị skip. Không trỏ test vào DB dev chung.

## Những mục chưa được tự coi là đã bàn giao

U04 ghi một ACCESS_DENIED mỗi lần service entry từ chối. Chưa có cơ chế biết U01 filter/authorization đã ghi event hay chưa; không thể tuyên bố chống trùng liên unit chỉ nhờ test một helper. Khi U01 bàn giao, chốt một tầng sở hữu audit cho mỗi đường đi và test HTTP để không ghi hai lần. Không thêm ThreadLocal hoặc sửa AuthorizationDecision để đoán trạng thái audit của U01.

Q14 reason ACCOUNT_DISABLED và bulk unassignment còn ở B08-D; generic business FAILURE events chưa được thêm ngoài action catalog. U02 chịu trách nhiệm cơ chế riêng cho DENIED/FAILURE theo IAudit. U04 hiện chưa có provider account guard hoặc archive checker U05 thật; các thao tác phụ thuộc vẫn fail closed như trước.

## Thứ tự tiếp tục code-generation-plan

Sau review này, triển khai B11 API/DTO/error mapping với Actor do server cung cấp, B12 integration/security consumer checks, rồi các route/màn hình/frontend tests. Có thể code/test các phần đó bằng provider test; không mở fallback allow hoặc dữ liệu giả trong production để vượt gate.

Các checkbox coding có thể hoàn thành khi có code và test đúng phạm vi. Các gate migration U01 thật, guard/account disable, class writers U05, durable audit U02, JWT/session thật, NFR measurements và tester độc lập giữ pending đến khi có bằng chứng. Không đánh toàn unit/story/DoD complete hoặc dọn mất lịch sử audit chỉ để làm plan toàn dấu x. Hai file TEMP vẫn phải dọn trước PR vào main. Không commit/push/Jira hoặc nhắn thành viên nếu chưa được người dùng yêu cầu.
