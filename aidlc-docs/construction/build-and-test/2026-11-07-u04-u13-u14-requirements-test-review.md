# Rà soát test U04, U13, U14 theo requirements và TDD

Ngày: 2026-11-07. Baseline: `staging@45baad9b3a484f4bcb9b2077d8afe8ecde520e45`.

## Kết luận

Cả ba unit đều có test tự động. Có nhiều assertion nghiệp vụ có giá trị, nhưng không thể chứng nhận toàn bộ test được thiết kế hoàn toàn từ requirements, độc lập với implementation. Suite trộn test nghiệp vụ, contract/API, UI component, wiring, kỹ thuật và regression; một số assertion còn lệch tài liệu hoặc chưa đủ để chứng minh điều tên test tuyên bố.

Về TDD: U13/U14 có tài liệu ghi nhận RED trước sửa và GREEN sau sửa ở một số đợt hồi quy. Chưa đủ bằng chứng xác nhận toàn bộ suite ban đầu của U04/U13/U14 được viết theo TDD. Không có bằng chứng không đồng nghĩa chắc chắn không dùng TDD.

Đây là báo cáo rà soát, không sửa code/test để làm kết quả phù hợp và không chứng nhận hoàn tất toàn bộ UC.

## Cách đánh giá tính hợp lệ

- Expected result phải truy được về FR/UC/BR hoặc quyết định đã được chốt, không chỉ sao chép giá trị code hiện tại trả về.
- Dựng input, boundary và nhánh từ chối sao cho test có thể phát hiện đúng lỗi; một exception bất kỳ hoặc mock được gọi chưa đủ.
- Unit test được phép import code, gọi hàm và mock dependencies. Điều cần độc lập là lý do chọn scenario và expected result; sử dụng code trong test không tự làm test mất giá trị.
- Test kỹ thuật có thể hợp lệ theo design/NFR. Không gọi tất cả test kỹ thuật là test sai hoặc bắt buộc xóa chúng khỏi suite.
- Test pass chứng minh các assertion được thực thi thành công trong môi trường đó; không chứng minh mọi yêu cầu đã được bao phủ, test được viết trước code, hoặc author chưa đọc code.
- Requirements hiện tại có nhãn `AS-BUILT/source-observed`: [requirements.md](../../inception/requirements/requirements.md) dòng5, và đầu các BR unit. Mapping với tài liệu đã đồng bộ theo source không chứng minh nguồn expected result độc lập trước implementation.

## Inventory hiện tại

Các số dưới đây đếm từ source. “Ca mở rộng” tính từng input parameterized của runner, không tính mỗi assertion hoặc vòng lặp trong một test thành ca mới. Đây không phải số pass mới.

| Unit | Backend: file / khai báo / ca mở rộng | Frontend: file / khai báo / ca mở rộng | Tổng ca mở rộng |
|---|---|---|---:|
| U04 Subject Administration, UC16–19 | 13 / 148 / 183 | 11 / 68 / 87 | 270 |
| U13 Reference Materials, UC58–62 | 10 / 82 / 114 | 7 / 31 / 51 | 165 |
| U14 Announcements & Notifications, UC63–65 | 12 / 101 / 126 | 3 / 28 / 31 | 157 |
| Tổng owner modules | 35 / 331 / 423 | 21 / 127 / 169 | 592 |

U04 backend gồm 31 ca PostgreSQL integration; U14 gồm 2 ca PostgreSQL integration. 390 ca backend còn lại vẫn có controller/context/parser/transaction-proxy checks, không phải tất cả là pure unit. Frontend gồm component, API contract và helper tests với network/dependency mocks, không phải E2E.

U13 phụ thuộc thêm U03 `files`: 10 backend files/33 ca và 3 frontend files/8 ca, tính riêng, không cộng vào 592. Các producer sự kiện U05/U07/U09/U10/U12, shared security và API E2E cũng tính riêng.

## U04: đã có test gì, còn vướng gì

Nguồn: [BR U04](../u04-subject-administration/functional-design/business-rules.md), [frontend design U04](../u04-subject-administration/functional-design/frontend-components.md), FR002/003, UC16–19, US-CAT-001.

Backend ở `backend/src/test/java/edu/aiplatform/subjects/`:

| Test file | Ca mở rộng | Truy vết chính |
|---|---:|---|
| model/SubjectCodeTest, SubjectTest | 10 + 10 | BR02/03/06/11: code, boundary, manager, archive/reopen |
| service/SubjectAdminCommandTest | 25 | BR01/02/03/06/51–54: create/edit/permission/error/audit |
| service/SubjectAdminReadTest | 15 | BR01/05/50/54: list/detail/counts/paging/fail closed |
| service/SubjectStatusTest | 15 | BR04/06/11/52/54: lifecycle, version, cascading contract |
| service/SubjectScopeServiceTest, SubjectQueryServiceTest | 7 + 6 | BR07/10/12: current scope/release/query |
| service/SubjectMutationGuardTest, SubjectAuditTest | 4 + 9 | Archived writes, audit/privacy/fail closed |
| service/SubjectClassesReaderTest, SubjectContractsWiringTest | 6 + 6 | Provider failures/deadlines, bean/contract wiring |
| controller/SubjectAdminControllerTest | 39 | UC16–19 input/principal/error contract |
| repository/SubjectRepositoryIntegrationTest | 31 | Real PostgreSQL unique/CAS/locking/rollback/concurrency |

Frontend ở `frontend/tests/features/subjects/`: validation10; API4; ManagerPicker8; ManagerChangeDialog6; SubjectFormDialog10; SubjectEditForm9; SubjectStatusActions9; SubjectClassesTable4; SubjectListPage10; SubjectDetailPage11; SubjectDetailMutations6.

Các vấn đề cần xử lý trước khi tuyên bố đúng hoàn toàn theo yêu cầu:

1. **Mâu thuẫn cột số lớp.** BR-U04-50 tại `business-rules.md:38` và `frontend-components.md:47` yêu cầu mỗi dòng có số lớp. `frontend/tests/features/subjects/pages/SubjectListPage.test.tsx:135` lại assert không có cột “Số lớp”. Backend `SubjectAdminReadTest.java:128` vẫn kiểm số lớp trả về. Cần tìm/chốt quyết định thay đổi, rồi sửa tài liệu hoặc test/UI theo quyết định đó; chưa đủ cơ sở tự kết luận phía nào sai.
2. **Popup archive và bộ lọc học kỳ.** `frontend-components.md:54` yêu cầu giải thích archive lớp và gỡ manager; `SubjectStatusActions.test.tsx:46–48` lại assert không có từ lớp/chủ nhiệm trong popup. Design dòng55 yêu cầu lọc học kỳ; `SubjectClassesTable.test.tsx:62` assert không có. Đây là mismatch với thiết kế chi tiết, không tự nâng thành yêu cầu FR mới.
3. **Giới hạn tích hợp.** Controller dùng standalone MockMvc/principal thủ công (`SubjectAdminControllerTest.java:115`), không chứng minh JWT/session/CSRF thực. Case archive qua U05 (`:288`) chỉ verify mock được gọi. DB integration dùng bảng lớp và audit fixture riêng (`SubjectRepositoryIntegrationTest.java:136`, `:1086`), chưa chứng minh giữ nguyên enrollment/history của U05 thật hoặc transaction vô hiệu hóa tài khoản U01+U04.
4. **Implementation-specific checks.** Constraint `uk_subjects_code`, thứ tự lock, bean class/name, deadline và debounce300ms là contract/design/regression checks. Có giá trị kỹ thuật nhưng không được gọi toàn bộ là business test được sinh độc lập từ FR.

Không lặp lại phát hiện cũ về student count: BR-U04-05 hiện đã ghi bảng Admin không hiển thị student count, phù hợp test hiện tại.

## U13: đã có test gì, còn vướng gì

Nguồn: [BR U13](../u13-reference-materials/functional-design/business-rules.md), FR004/005/013/035, UC58–62, US-CNT-001/002.

Backend ở `backend/src/test/java/edu/aiplatform/materials/`:

| Test file | Ca mở rộng | Truy vết chính |
|---|---:|---|
| model/YoutubeVideoTest | 30 | BR10 URL allowlist/canonicalization; metadata/snapshot |
| service/MaterialAccessPolicyTest | 16 | BR01–03/40/42: role/scope/lifecycle/fail closed |
| service/MaterialServiceTest | 14 | BR10–14/30/40, module/scope/version/job |
| service/MaterialModuleServiceTest | 15 | M01–06: attach/delete/reorder/CAS/visibility |
| service/MaterialQueryAndReferenceTest | 12 | BR12/20/31/32/40: token/reference/MCP/text budget |
| service/MaterialTextExtractJobHandlerTest | 8 | BR30/42: exact version, stale/race/retry/no text |
| integration/MaterialTextExtractorTest | 6 | Parser PDF/DOCX/PPTX thật, timeout/stream/text handling |
| controller/MaterialControllerTest | 11 | Input/principal/version/safe HTTP error/delegation |
| service/MaterialMcpTransactionTest, ReferenceMaterialWiringTest | 1 + 1 | Proxy error/bean-selection regressions |

Frontend ở `frontend/tests/features/materials/`: API27; MaterialDialogs5; MaterialPreview4; ModuleBoard10; moduleNav3; noncedStyles1; xlsx1. Viewer/network có mock; không chứng minh PDF thực render đúng hay video thực phát được.

Các vấn đề đáng ưu tiên:

1. **File policy chưa thống nhất.** BR-U13-10 (`business-rules.md:24`), FR013 và BR-U03-87 nói PDF/DOCX/PPTX và loại file qua Settings. `backend/src/test/java/edu/aiplatform/files/model/PurposePolicyTest.java:25` thêm XLSX; `:34` kiểm fixed types. Git `f074c1d2` chủ đích bỏ cấu hình loại file, nhưng chưa tìm thấy yêu cầu đã duyệt tương ứng được cập nhật ở các nguồn này. Ghi là documentation/decision drift, chưa kết luận implementation sai. YouTube với list/index đã có quyết định và BR cập nhật 02/11, không phải mismatch hiện tại.
2. **Fixture lịch sử chưa đúng tên test.** `MaterialQueryAndReferenceTest.java:90` gọi “studentsCannotDownloadOldVersions”, nhưng fixture currentVersion=1 (`:47`) lại yêu cầu version2 (`:91`). Nó kiểm phiên bản không hiện hành/tương lai, chưa kiểm historical v1 khi current=v2. Cần hai version thật và expected result theo vai trò.
3. **Thiếu kiểm trực tiếp nguồn AI.** Không có core test gọi implementation `ReferenceMaterialService.listSelectable`; controller `MaterialControllerTest.java:125` chỉ verify mocked `classList`. Chưa đủ chứng minh active/authorized/scope filtering theo BR31/23. Consumer mock/fake ở U07/U10 không thay thế test implementation U13.
4. **Audit và boundary còn thiếu.** `MaterialServiceTest.java:122` kiểm MATERIAL_CREATED; chưa có assertion success cho MATERIAL_UPDATED, MATERIAL_VERSION_CREATED, MATERIAL_ARCHIVED. Handler kiểm mật độ chữ hai điểm (`MaterialTextExtractJobHandlerTest.java:73`) nhưng chưa biên49/50 ký tự/trang hoặc cap1,000,000; chưa có scenario trích chữ FAILED nhưng vẫn xem/tải được.
5. **Tên test UI quá rộng.** `MaterialDialogs.test.tsx:80` nói đóng popup trước khi mở Quiz, nhưng `:112` chỉ assert callback báo đã tạo; không assert close/router/order.
6. **Persistence và quyền chưa toàn diện.** Module tests chủ yếu mock repository, chưa chứng minh DB unique/cascade/backfill/rollback. Archived subject và U04/U05 outage thiếu owner assertion trực tiếp; không suy ra app chắc chắn sai.

Phát hiện cũ “cross-subject test pass sai lý do” đã được sửa: `MaterialModuleServiceTest.java:95` hiện cấp manage permission, kiểm đúng400/MATERIAL_QUIZ_INVALID và không ghi link/audit. Không dùng audit cũ thay kiểm tra hiện tại.

## U14: đã có test gì, còn vướng gì

Nguồn: [BR U14](../u14-announcements-and-notifications/functional-design/business-rules.md), FR011/019/023, UC63–65, US-CNT-004/US-NTF-001.

Backend dưới `backend/src/test/java/edu/aiplatform/announcements/` và `notifications/`:

| Test file | Ca mở rộng | Truy vết chính |
|---|---:|---|
| announcements/model/AnnouncementSanitizationTest | 5 | BR02: Markdown/HTML, length, atomic invalid edit |
| announcements/service/AnnouncementServiceTest | 14 | BR01–06/40/41: author/version/create/edit/delete/permissions |
| notifications/service/RecipientResolverTest | 21 | BR03/11/12/20: recipients/privacy/all four announcement flags |
| notifications/service/AssignmentNotificationRoutingTest | 3 | Class/subject fanout/dedup/current membership/routes |
| notifications/service/NotificationServiceTest | 31 | BR10/12/13/20/21: dedup/after-commit SSE/owner/role preferences |
| notifications/service/NotificationEventServiceTest | 3 | Safe role/action copy, grade privacy |
| notifications/service/EmailDeliveryServiceTest | 21 | BR20–24: preference/cap/VN-day/deferred/retry/safe body |
| notifications/service/DeadlineReminderScannerTest | 12 | BR30–32: deadlines/retirement/no-submission/late-open/idempotence |
| notifications/controller/NotificationControllerTest | 7 | Principal/own settings/feed/SSE/HTTP contract |
| notifications/integration/NotificationEventListenerTest | 5 | ACK/retry/parking/schema protocol |
| notifications/integration/NotificationTopologyContextTest | 2 | Spring startup/bean-name regression |
| notifications/integration/NotificationDeliveryIntegrationTest | 2 | Real PostgreSQL retry/rollback/idempotent replay |

Frontend: `announcements/components/Announcements.test.tsx`6; `announcements/pages/ClassAnnouncementsPage.test.tsx`5; `notifications/components/Notifications.test.tsx`20.

Các giới hạn cần ghi trong report:

1. **Retry test yếu.** `NotificationEventListenerTest.java:72–76` chỉ verify `rabbit.invoke(any())` và ACK; mocked callback không được chạy. Không kiểm retry publication, header tăng, routing/backoff hoặc publisher confirm. Tên “bounded retry” chưa tương xứng bằng chứng.
2. **List/delete persistence chưa đủ.** `AnnouncementServiceTest.java:226` dùng list rỗng từ mock; chưa chứng minh ordering/filter đa lớp/ẩn deleted. Delete `:141` kiểm soft-delete/actor, chưa chứng minh retained reference, timestamp và audit đúng nội dung.
3. **Nhắc bài nhóm chưa có contract tương ứng.** Scanner fixture là INDIVIDUAL (`DeadlineReminderScannerTest.java:69`), danh sách recipients mock. U08 provider test cũng chỉ scenario cá nhân; chưa chứng minh members của nhóm chưa nộp theo BR31.
4. **Retention chỉ verify invocation.** `DeadlineReminderScannerTest.java:337` verify `deleteOld(now−180d)`; chưa kiểm DB boundary/cascade hoặc dữ liệu mới được giữ theo BR14.
5. **Opt-out/cap/retry có giới hạn mock.** `EmailDeliveryServiceTest.java:238` chỉ kiểm email SKIPPED, chưa đọc lại in-app notification. Reservation số299/300 từ mock không chứng minh concurrency cap; gọi failed callback không chứng minh năm lần retry thật hoặc SMTP exactly-once.
6. **Một số assertion audit/denial chưa đủ đặc hiệu.** `AnnouncementServiceTest.java:96` chỉ kiểm có before/after keys; controller404 được stub. Cần kiểm đúng action/snapshot/error/audit của service thật cho nhánh role/scope denial.

Không lặp lại các vấn đề đã được bổ sung: hiện RecipientResolverTest có đủ important×sendEmail matrix; preference tests đã bao phủ bốn role, staff mặc định tắt và Admin backup bắt buộc theo quyết định02/11. Topology test là mock/lightweight context dù nằm folder integration; không gọi nó real-broker integration.

## TDD: có bằng chứng gì và có cần thiết không

TDD là viết test theo yêu cầu hoặc lỗi cần sửa, chạy thấy fail đúng lý do (RED), viết code tối thiểu để pass (GREEN), rồi refactor trong khi test vẫn pass. TDD mô tả thứ tự phát triển; requirements-based testing mô tả nguồn của scenario và expected result. Hai khái niệm không thay thế nhau.

| Unit | Bằng chứng lịch sử | Kết luận phù hợp |
|---|---|---|
| U04 | `eb011c82` và `72192953` ngày18/10 đưa production và tests vào cùng commit; `7fcd0a95` sửa fixture/expectations sau đó; `808ce7ed` sửa async test synchronization | Không xác nhận được toàn bộ suite theo TDD. Cùng commit không chứng minh code-first hoặc test-first. |
| U13 | Original `7c6a3398` thêm production+tests cùng commit. [Report02/11](2026-11-02-u04-u13-u14-results.md) ghi regression-first11 FE failures/16 BE failures-errors, rồi43/94 pass | Có recorded RED→GREEN cho đợt sửa U13/U14, không phải mọi test U13. |
| U14 | Original `3f4a201c`/`2920fc67` tương tự. Audit dòng5146 ghi topology2 RED trước sửa. [Report24/10](../remediation/u04-u13-u14/verification/u14-notification-summary-2026-10-24.md) ghi39 test/15 expected RED rồi75 GREEN; có thêm report02/11 | Có bằng chứng tài liệu regression-first ở một số đợt sửa; chưa xác nhận toàn suite gốc TDD. |

Các số02/11 là nhóm U13/U14 kết hợp, không cộng thành ca unique và không gán nhầm cho U04. Evidence RED/GREEN trên là ghi nhận lịch sử trong tài liệu, không phải mình tái chạy phiên bản cũ hôm nay; temporary raw logs một số đợt đã được dọn.

TDD không phải điều kiện phổ quát để một test hợp lệ: test viết sau code vẫn có giá trị nếu expected result độc lập theo yêu cầu và thực sự bắt lỗi. TDD cũng không đủ bảo đảm test đúng: viết test trước nhưng mong đợi sai vẫn kiểm sai.

Trong dự án này, skill Superpowers `test-driven-development/SKILL.md` quy định dùng TDD cho feature/bugfix mới. Do đó, các thay đổi mới trong phạm vi quy trình đó phải làm TDD, nhưng không được gắn nhãn hồi tố cho test cũ khi thiếu bằng chứng. Với code đã có, nên giữ và cải thiện test có giá trị; không cần xóa/viết lại tất cả chỉ để tạo một lịch sử TDD giả.

## Kết quả chạy trong phiên rà soát

Đang bổ sung kết quả cuối cùng. Các số inventory phía trên không phải số pass. Hai suite PostgreSQL33 ca được loại khỏi selected run; không có DB/E2E/SMTP/browser acceptance mới.

## Đề xuất dùng trong report nộp

1. Giữ test hiện tại làm nền, tách business unit, API/component/technical và integration/E2E.
2. Với mỗi test case ghi Test ID, FR/UC/BR hoặc decision version, precondition, input, expected result, vị trí test thực thi, actual result và evidence/run date.
3. Chốt các mismatch U04 UI và U13 file policy bằng yêu cầu đã duyệt; không sửa BR theo code chỉ để báo cáo khớp.
4. Sửa fixture/weak assertions và bổ sung gap trước khi ghi “covered”; case chưa thực thi ghi Not run, skip không tính pass.
5. Chỉ ghi TDD ở phạm vi có evidence RED/GREEN; còn lại ghi “không xác định quy trình viết từ bằng chứng hiện có”.

Câu kết luận có thể dùng: “Ba unit đã có test tự động và nhiều case truy vết được tới yêu cầu nghiệp vụ. Bộ test hiện trộn kiểm tra nghiệp vụ và kỹ thuật, còn một số mismatch/gap; chưa đủ bằng chứng khẳng định toàn bộ được thiết kế độc lập từ requirements hoặc toàn bộ được phát triển theo TDD.”

## Phạm vi thay đổi và extension

Chỉ thêm báo cáo/kế hoạch và cập nhật audit/state. Không sửa application/test/config/dependency, không commit/push/deploy hoặc ghi dữ liệu live. Security/resiliency scope giữ nguyên: không lộ secrets, không đổi control; deployment/runtime controls N/A cho review tài liệu. PBT disabled, không bật lại. Không có coverage instrumentation/mutation run nên không công bố phần trăm coverage hay chứng nhận độc lập tác giả.
