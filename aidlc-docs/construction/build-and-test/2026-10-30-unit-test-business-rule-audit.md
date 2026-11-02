# Unit test ↔ business rule ↔ use case audit — 2026-10-30

**Kết luận:** Chưa thể xác nhận test đã đúng và đủ toàn bộ BR/66 UC. Nhiều assertion đúng với code hiện tại, nhưng doc còn quy tắc cũ; có fixture/exception assertion cho phép test pass sai lý do và có nhánh nghiệp vụ quan trọng chưa được kiểm trực tiếp. Đây là rà soát và báo cáo, chưa sửa application code hoặc test.

**Baseline:** `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`, bao gồm bản doc đồng bộ chưa commit từ phiên trước. Đọc rule, production branch, assertion/fixture và mock; không dùng sự tồn tại BR-ID trong comment làm thước đo coverage. 15 unit/66 UC được chia thành ba lượt kiểm độc lập và có bảng từng UC ở phụ lục. Các bảng chỉ nêu assertion đã kiểm và gap/pending, không chứng nhận mọi scenario đã bao phủ.

## Kết quả chạy mới

| Check | Kết quả thực tế | Giới hạn |
|---|---|---|
| Backend Maven verify | BUILD SUCCESS; 1.471 total, 1.372 pass, 99 skip, 0 failure/error; kết thúc 12:29:42 +07 | Có cả unit và integration declarations, không gọi toàn bộ 1.471 là unit-only |
| Required PostgreSQL guard | FAIL: SubjectRepositoryIntegrationTest 31/31 skipped | Maven success chưa đạt gate CI; NotificationDeliveryIntegrationTest cũng phải chạy không skip |
| Frontend Vitest | 597 pass, 0 fail/skip; 94 files; exit0 | Network/UI mocks; không phải E2E hoặc coverage% |
| Source/config diff | Không thay đổi backend/frontend/infra/scripts hoặc manifest/lockfile | Existing doc sync preserved; report/state/audit only |

Môi trường chạy: JDK21.0.12.1, Java release17; Node25.8.1/npm11.11.0, khác baseline CI Java17/Node22. Lượt frontend đầu có 592 assertions pass nhưng hai file không nạp được do thiếu pptx-preview/xlsx; không coi là pass. Sau npm ci đúng lockfile, chạy lại đủ597 assertions pass. Maven offline đầu không có Surefire runner; đã cho Maven tải dependency thiếu và verify lại. Không đổi dependency versions. Hai JDBC fixture URLs U06/U12 được để trống chỉ trong tiến trình test để không đụng DB ngoài; Docker-dependent tests bị skip khi Docker không khả dụng cho lần chạy. Không mở live stack, không gửi email thật/điểm hoặc gọi provider.

Commands đã dùng (từ frontend/backend tương ứng): `node node_modules/vitest/vitest.mjs run --maxWorkers=2 --reporter=json --outputFile=../.tmp/2026-10-30-test-rule-frontend-complete.json`; Maven3.9.12 `-B -Dmaven.repo.local=<existing user cache> verify`; từ root `./scripts/assert-required-backend-tests.ps1`. Log/JSON mới ở `.tmp/2026-10-30-test-rule-*`; không đưa cache/log vào commit.

## Kết quả theo từng unit

Source BR/test-file counts không phải coverage. Test pass ở đây chỉ là kết quả run hiện tại, không phủ nhận các lỗi assertion/fixture dưới đây.

| Unit | Primary UC | BR rows | Backend Test files | Backend pass / skip | Nhận xét ngữ nghĩa |
|---|---|---|---|---|---|
| U01 | 01–12 | 61 | 26 | 154 / 30 | Test xác thực/OTP/current role có assertion hữu ích; BR95 về Redis còn trái code và test 503; cần mô tả refresh grace đúng. |
| U02 | 13 | 11 | 4 | 32 / 0 | Test đúng recordView/filter hiện tại; BR07 “mọi tra cứu đều audit” và BR08 actor lookup chưa khớp. DB dedup/rollback cần bằng chứng thật. |
| U03 | 14–15 | 53 | 26 | 94 / 6 | Ưu tiên P1: worker ACK trước bảo đảm terminal/retry, backup bỏ kết quả CAS, safeError chỉ truncate; test hiện chưa bắt được. |
| U04 | 16–19 | 15 | 13 | 152 / 31 | CAS/lock/rollback có test PostgreSQL nhưng lần này bị skip; UC18 UI test ẩn student count trái BR05; tích hợp U04–U05 còn mock. |
| U05 | 20–30 | 32 | 12 | 214 / 0 | Null teacher đã có domain/UI test đúng; fixture API bất khả thi, popup BR47 và eligibility giữa các batch rows cần xử lý. |
| U06 | 31–33 | 34 | 20 | 108 / 5 | Role/quota/task có test đúng; multi-key SQL ordering và negative reveal còn thiếu assertion trực tiếp. |
| U07 | 34–36 | 40 | 13 | 74 / 0 | Publication/rubric có test; BR binary scoring, Student MCP và mô tả diagram optional còn cũ so với code. |
| U08 | 37–41 | 32 | 4 | 35 / 0 | Attempt/feedback/activity có test; fixed one-day retention được kiểm. UC38 visibility và UC41 Student MCP trái source. |
| U09 | 42–45 | 30 | 23 | 77 / 6 | Điểm thành phần, version, finalize/publication có assertion đúng; BR binary cũ, duplicate-feedback fixture bị count guard che. |
| U10 | 46–48 | 29 | 15 | 98 / 13 | Test tự gắn nhãn OPEN thực tế kiểm RETIRED hai lần; reopen service/HTTP guards chưa có test trực tiếp. |
| U11 | 49–52 | 16 | 4 | 24 / 4 | Attempt/score/snapshot có test; blocked reason trái BR; result canRetry thiếu trạng thái Quiz/class; hidden-result fixture không sát DTO. |
| U12 | 53–57 | 34 | 16 | 67 / 2 | Optional reason đã có test; leader rename/UC54 MyGroup UI còn trái BR01/50; submission stale/CAS/resubmit cần thêm assertion. |
| U13 | 58–62 | 24 | 10 | 87 / 0 | Cross-subject test có thể pass bởi guard quyền khác; module reorder/CAS/last-module/teacher visibility còn thiếu. |
| U14 | 63–65 | 22 | 12 | 102 / 2 | Event/recipient/retry/privacy có assertion; conjunction important+sendEmail chưa đủ matrix. |
| U15 | 66 | 10 | 7 | 44 / 0 | Counts/activity algorithm có test; HTTP /activity thiếu role/query/no-store matrix trực tiếp; partial-source nhánh khác cần bổ sung. |

Shared/architecture cases are outside the per-unit rows. Full source inventory and exact test methods, assertions, file/line references and per-UC rows: [2026-10-30-unit-test-audit-u01-u05.md](2026-10-30-unit-test-audit-u01-u05.md), [2026-10-30-unit-test-audit-u06-u10.md](2026-10-30-unit-test-audit-u06-u10.md), [2026-10-30-unit-test-audit-u11-u15.md](2026-10-30-unit-test-audit-u11-u15.md).

## Phát hiện ưu tiên

### P1 — U03 worker có thể ACK khi chưa lưu trạng thái lỗi/handoff retry

[JobListener.java](../../../backend/src/main/java/edu/aiplatform/jobs/infrastructure/JobListener.java) — ACK trong finally tại dòng64; JobDispatcher.fail nuốt onFailed exception. Test dispatcher thường không kiểm failure của terminal persistence/retry publication; chưa có JobListener test. Rủi ro source về mất retry/message, chưa tái hiện trên broker thật.

### P1 — U03 backup test mặc định chấp nhận CAS không cập nhật dòng nào

[BackupJobHandlerTest.java](../../../backend/src/test/java/edu/aiplatform/backup/application/BackupJobHandlerTest.java) — successfulRunStoresAnEncryptedArchiveThatDecryptsToTheDump dòng95 không stub markSuccess, Mockito trả0; handler dòng112 bỏ kết quả rồi audit BACKUP_COMPLETED và purge. Cần success fixture trả1 và case0 riêng, không chỉ verify đã gọi repository.

### P1 — U03 safeError chưa có assertion che secret

[JobDispatcher.java](../../../backend/src/main/java/edu/aiplatform/jobs/application/JobDispatcher.java) — safe chỉ truncate message; BackupJobHandler.onFailed đưa message vào DB/audit/event. Test chỉ kiểm length hoặc action không phát hiện key/password sentinel. Đây là sanitization gap theo source, không bằng chứng secret thực đã rò rỉ.

### P2 — U10 test OPEN thực tế kiểm RETIRED hai lần

[QuizServicesTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/application/QuizServicesTest.java) — publishedQuizContentNeverChangesForAttemptsToRelyOn dòng344–365 gọi retire ngay trong List.of trước vòng lặp. Cả hai lần service load RETIRED. OPEN editable hiện tại không được kiểm bởi những assertion này; test khác có kiểm saveItems OPEN nhưng không thay thế add/update/delete-inline branches.

### P2 — U13 test khác môn có thể pass vì không có quyền

[MaterialModuleServiceTest.java](../../../backend/src/test/java/edu/aiplatform/materials/application/MaterialModuleServiceTest.java) — attachRejectsQuizOfAnotherSubject dòng52–56 chấp nhận mọi ApiException, canManageQuiz mock mặc định false. Xóa same-subject guard vẫn có thể throw ở guard quyền. Cần cho quyền true và assert MATERIAL_QUIZ_INVALID/400, cùng không ghi link.

### P2 — U09 test duplicate feedback bị count validation che

[GradingProposalValidatorTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingProposalValidatorTest.java) — Dòng236–254 duplicate item duy nhất thành2 items; count>rubric-total bị reject trước seenItems duplicate guard. Cần rubric2 items và duplicate1 giữ cùng cardinality; assert đúng diagnostic.

### P2 — U11 result vẫn quảng bá retry khi Quiz đã retired

[QuizAttemptService.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/application/QuizAttemptService.java) — canRetry dòng150 chỉ xét maxAttempts trong snapshot và count, chưa kiểm Quiz/class hiện hành; frontend hiện nút theo flag, start lại từ chối. Retirement test không assert result flag. Rủi ro source/UI chưa tái hiện bằng browser trong lượt này.

### P2 — Doc chưa đồng bộ hết quyền và thuật toán

[business-rules.md](../u09-assignment-grading/functional-design/business-rules.md) — U07 BR22/U09 BR20 vẫn binary checklist trong khi code/tests cho partial points. U08 BR41/45 và UC41 vẫn hứa Student MCP đã bị admission/catalog chặn. U01 BR95 Redis, U02 recordView audit, U05 popup, U11 blocked reason, U12 leader rename cũng còn lệch. Các test này thường đúng với code; không sửa chúng mù quáng theo doc cũ.

## Khoảng trống cần ưu tiên bổ sung

- U05: service/API OPEN/reopen teacher=null, target student đổi role/status giữa batch rows, fixture contract createDRAFT/version/clear-teacher; real enrollment/archive concurrency.
- U06: nhiều institutional keys thật trong DB, ACTIVE/create-time/id tie-breaking; reveal wrong-owner/expired/revoked/no ciphertext/PERSONAL và key DISABLED.
- U10: service và controller reopen với wrong-status/version/container/role/audit; retire DRAFT/REVIEWED và default reason.
- U12: stale/lost document CAS, resubmit giữ snapshot cũ, auto-close unchanged không tạo evaluation/event mới; optional reason whitespace/1000/1001.
- U13: module rename/reorder duplicate/stale/foreign-subject/lost CAS; last module archived move; teacher/manager visibility; migration unique/cascade/backfill trên DB.
- U14: đủ bảng important × sendEmail và recipient emailType; giữ test privacy/retry hiện có.
- U15: HTTP /activity role/anonymous/unknown-duplicate-query/no-store và zero provider reads on denial; partial null/failure source matrix.

Mock verify/call arguments không chứng minh rollback DB, SQL unique/locking, Rabbit delivery hoặc hành trình nhiều role. Các test tích hợp bị skip phải được chạy lại trong môi trường phù hợp trước khi kết luận gate CI hoặc nghiệm thu UC.

## Đề xuất thứ tự sửa

1. Viết regression cho ba vấn đề U03 P1, sau đó sửa code nếu xác nhận yêu cầu; không đổi rule để hợp thức hóa mất message/CAS hoặc lộ lỗi thô.
2. Sửa ba test pass sai lý do ở U10/U13/U09 và fixture API U05; mỗi negative case kiểm đúng error/side effect.
3. Cập nhật doc cho hành vi đã thay đổi được xác nhận: partial score, MCP staff-only, leader rename, Redis fail-closed, pre-start content; quyết định riêng phần popup/blocked reason và audit policy nếu chủ đích chưa rõ.
4. Bổ sung test guard/matrix còn thiếu, chạy lại DB integration + required guard, rồi mới đóng các gate UC.

Lượt này chỉ rà soát và ghi báo cáo. Không sửa business behavior/test để khiến suite xanh; không commit/push/deploy. Không có coverage instrumentation hoặc mutation run nên không công bố phần trăm coverage hay “đủ66UC”.
