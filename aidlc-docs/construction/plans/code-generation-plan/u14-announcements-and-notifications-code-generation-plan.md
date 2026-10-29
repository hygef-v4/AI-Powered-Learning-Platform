# U14 Announcements & Notifications — Code Generation Plan

> Implementation planning revision 2026-10-21, dựa trên baseline 2026-10-15 và [unit-of-work](../../../inception/application-design/unit-of-work.md): U14 Announcements & Notifications thuộc Iter 3; UC 63–65. Đây là kế hoạch thực thi; chưa sinh application code hoặc xác nhận runtime U14.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Thiết kế: [business-logic-model](../../u14-announcements-and-notifications/functional-design/business-logic-model.md), [business-rules](../../u14-announcements-and-notifications/functional-design/business-rules.md), [domain-entities](../../u14-announcements-and-notifications/functional-design/domain-entities.md), [frontend-components](../../u14-announcements-and-notifications/functional-design/frontend-components.md), [NFR](../../u14-announcements-and-notifications/nfr-requirements/nfr-requirements.md), [NFR design](../../u14-announcements-and-notifications/nfr-design/nfr-design-patterns.md), [infrastructure](../../u14-announcements-and-notifications/infrastructure-design/infrastructure-design.md).

## Trạng thái

**Cập nhật commit:** Sau handoff dưới đây, người dùng yêu cầu chia nhỏ và commit cục bộ. Đã tạo7source commits `6cef92a`–`adfc51f` và commit tài liệu riêng trên `announcements-and-notifications`; chưa push. Các mô tả chưa commit bên dưới là trạng thái tại thời điểm handoff trước yêu cầu này; acceptance gates không đổi.

Code Generation Part 1 được duyệt ngày2026-10-21, [approval questions](u14-implementation-approval-questions.md) Answer a. Part2 bước1–11 hoàn tất; người dùng duyệt code bằng “tiếp tục” ngày2026-10-21. Source trên branch `announcements-and-notifications`, chưa commit/push. [Code summary](../../u14-announcements-and-notifications/code/code-summary.md) ghi bằng chứng và acceptance gates; [Build & Test summary](../../build-and-test/build-and-test-summary.md) ghi kiểm tra hiện tại và điều kiện tích hợp còn thiếu. Không coi stub là integration pass.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/announcements/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../announcements/contract/` | Trống: unit không cung cấp interface mới, chỉ cài interface của unit trước |
| `frontend/src/features/announcements/` | Chỉ có `index.ts` trống |
| `backend/src/main/java/edu/aiplatform/notifications/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../notifications/contract/` | Trống: unit không cung cấp interface mới, chỉ cài interface của unit trước |
| `frontend/src/features/notifications/` | Chỉ có `index.ts` trống |

Scaffold frontend còn có `announcements/register.ts` đăng ký hai mục menu, và `features/register.ts` đã nạp file này. Sửa đăng ký hiện có, không thêm mục menu trùng.

## Phạm vi và nguyên tắc thực thi

- Branch thực tế đã là `announcements-and-notifications` khi nhận yêu cầu; working tree/index ban đầu sạch. Không merge hoặc mang toàn bộ U13 sang branch này, không tự commit/push.
- UC63: feed các lớp OPEN đang học/dạy, lọc lớp, card Student Class Detail. UC64: tạo/sửa/xóa mềm bởi giảng viên chính; Subject Manager chỉ thao tác khi được giao dạy chính lớp đó. UC65: chuông/popup cho cả bốn role, đọc/đánh dấu/SSE và tùy chọn email của Student.
- Thực hiện cả F1–F9 trong baseline: event fanout, email cap/retry, nhắc hạn Assignment, deferred email và retention; không giảm phạm vi thành CRUD màn hình.
- Thứ tự checklist bên dưới là nguồn thực thi chính. Chỉ đánh dấu bước khi có code và kiểm tra tương ứng; bước11 đã được người dùng duyệt. Không coi consumer tests với mock/stub là integration pass.

## Contract thực tế và quyết định cần duyệt

### C01 — Quyền và ranh giới unit

U14 chỉ đọc U01/U05/U07/U08/U12 qua package `contract`. Dùng `IAuthorization` với action khai báo rõ trong U01: `ANNOUNCEMENT_READ` (Student/Teacher/Subject Manager, CLASS), `ANNOUNCEMENT_MANAGE` (Teacher/Subject Manager, CLASS), `NOTIFICATION_SELF` (cả bốn role, ACCOUNT). U14 kiểm thêm lớp OPEN và teacherId; Admin không đọc/quản lý thông báo lớp. Actor lấy từ principal server; tài khoản không còn ACTIVE hoặc dependency lỗi thì từ chối. Thành công announcement và audit chung transaction; denied audit giữ được sau rollback; audit trước/sau chỉ metadata cho phép, không log body/email/điểm.

Các thay đổi U01 giới hạn ở `identity/contract/Actions.java`, `identity/application/AccessPolicy.java`, kiểm thử action và C02. Không đổi luồng đăng nhập hay quyền cũ.

### C02 — Danh sách mọi Admin ACTIVE, tránh mất người nhận

`IAccountLookup.findAccounts` và provider hiện chặn tối đa20 kết quả, không có cursor; không dùng kết quả này rồi khẳng định đã gửi mọi Admin. Đề xuất bổ sung `listActiveAdmins(UUID afterId, int limit)` với keyset theo UUID, page tối đa100, vào `identity/contract/IAccountLookup.java`; cài truy vấn trong `identity/domain/AccountRepository.java`, provider `identity/application/AccountLookupService.java` và stub liên quan, có test >20 Admin và phân trang. Default method fail closed khi provider cũ chưa hỗ trợ để giữ source compatibility, provider thật override. U14 lấy trang tới hết; không truy cập trực tiếp bảng accounts hoặc domain U01.

### C03 — Event schema và bounded retry

Envelope dùng `jobs.contract.DomainEvent` schemaVersion1 và `EventTypes` hiện có. Nguồn thực tế: enrollment có accountId/classId; grade có studentIds/classId/assignmentId; quiz có quizId/scope/subjectId/classId; backup có runId. Chuẩn hóa theo từng loại và sourceUnit, không dùng chung một trường recipients giả định. Group/Assignment chưa có producer thật trên branch: theo contract thiết kế, test consumer với fixture; ghi gate xác nhận payload owner, không sửa producer unit khác.

Topology nằm trong `notifications/infrastructure`, queue durable `jobs.notification` bind chính xác các event được hỗ trợ trên `platform.events`. Listener worker xác thực schema/type/payload giới hạn, ack sự kiện lạ/không hỗ trợ không tạo dữ liệu. Lỗi provider tạm thử lại hữu hạn/backoff theo chính sách U03 (30s,1m,2m,4m,8m), không hot requeue; dùng retry topology dành DomainEvent của U14 vì retry queue JobMessage của U03 quay về exchange jobs và không xử lý được envelope sự kiện. Message không đọc được và hết retry có đường xử lý lỗi rõ, không log raw payload. Không sửa dispatcher/routing job U03: EMAIL_SEND đã được khai báo và định tuyến sẵn.

INSERT notification ON CONFLICT dựa trên eventId/recipient/type, email unique notificationId; tạo notification và email atomic. Sau commit mới phát SSE; lỗi SSE không đảo transaction nguồn. Lặp message không tạo email/thông báo trùng.

### C04 — Email cap, retry và giới hạn SMTP

Trần `U14_EMAIL_DAILY_CAP` mặc định300 dùng ngày `Asia/Ho_Chi_Minh`. Đếm/reserve PENDING+SENT và tạo/chuyển email dưới cùng khóa transaction PostgreSQL để không vượt trần giữa nhiều worker; OTP không tính. Hoãn theo đúng thứ tự BR-U14-22, thứ tự ổn định trong cùng ưu tiên; pending retry không reserve hai lần, ngày đổi kiểm lại budget. Tôn trọng preference lúc tạo, nhắc hạn quá hạn SKIPPED. Gửi text tối thiểu với link nội bộ từ cấu hình base URL, không lấy raw HTML/link/điểm từ event.

EmailSendJobHandler cài IJobHandler + IPendingSweeper, queue EMAIL, timeout hữu hạn; IMail ngoài transaction nghiệp vụ nguồn. Claim/lock/CAS cho delivery tránh hai worker gửi cùng lúc; SENT là terminal, retry/onFailed không ghi đè SENT. IMail hiện không có provider idempotency key: crash đúng sau SMTP accept nhưng trước khi lưu SENT không thể được chứng minh exactly-once chỉ bằng DB. Đây là giới hạn phải báo trong handoff và fault test; không hứa bảo đảm không trùng tuyệt đối qua crash window, không đổi contract IMail âm thầm.

### C05 — Reminder, retention và ownership

Scanner worker dùng IScheduledScanner; xử lý batch hữu hạn, mỗi (assignment,class,closesAt) có dấu unique trong transaction cùng fanout. Chỉ Assignment OPEN, lớp OPEN, opensAt không muộn hơn closesAt-24h, reminder nằm trong cửa sổ NFR5phút quanh mốc; bỏ dấu SKIPPED cho bài mở muộn; đổi hạn có khóa mới. Recheck trạng thái/hạn trước khi gửi; người chưa nộp từ ISubmissionQuery rồi giao với active enrollment để tránh gửi người đã rời lớp. Môn -> mọi lớp OPEN; group eligibility thuộc U08/U12, không truy vấn bảng ngoài unit.

Retention xóa tối đa10.000 notification cũ hơn180ngày, cascading email child theo FK nếu cần (DELETE trực tiếp chỉ notifications), không xóa thông báo mới, audit hay preference. Deferred stale reminder SKIPPED, pending sweeper chỉ enqueue công việc còn hợp lệ.

### C06 — UI local và markdown an toàn

UI nguồn `E:/FPTUni/sem9_fa26/ui-reference`: `screens/s06-class-announcements.html`, `t08-class-announcements.html`, `t08b-announcement-edit-popup.html`, `t08c-announcement-delete-popup.html`, `c14-notifications-popup.html`; dùng bố cục/nhãn, không mang mock data hay remote font vào production.

Tái sử dụng shared API/CSRF/session, AppShell, Dialog và slots. Đề xuất thêm đúng hai dependency frontend `react-markdown` + `rehype-sanitize` để render markdown, không bật raw HTML; chặn unsafe URL và ảnh/HTML thô, không dùng dangerouslySetInnerHTML hay UI kit mới. Phiên bản chọn tương thích React19 và lock bằng package-lock khi triển khai. Test script/link XSS; preview không cấp quyền link đích.

SSE EventSource dùng cookie cùng origin; kênh accountId do server quyết định qua ISseHub.subscribe, không nhận channel do client truyền. Reconnect có backoff và fallback refresh unread/list khi stream mất, giới hạn kết nối và cleanup khi unmount/logout. Query pagination ≤50, stale request hủy bằng AbortController, popup không mở trang notifications riêng, lỗi tải preference không giả định lưu thành công.

### C07 — Header SEC-004 trên branch hiện tại

Branch này chưa có sửa nonce của U13. Áp lại riêng giải pháp đã kiểm của commit `28b9f46` vào `frontend/src/proxy.ts`, `frontend/src/proxy.test.ts`, `frontend/src/app/layout.tsx` (không cherry-pick/merge/commit tự động): nonce riêng từng request, script nonce/strict-dynamic, DENY/frame-ancestors none, HSTS/nosniff/referrer; dynamic rendering để nonce khớp HTML. Giữ child frame PDF/YouTube/Draw.io. Style attribute exception và dev eval/HMR được ghi rõ; production script không unsafe-inline/eval. Smoke nonce/header và hydration sau build.

### C08 — Dependency và acceptance gates

U07 IAssignmentQuery, U08 ISubmissionQuery và U12 ISseHub trên branch hiện chỉ có stub; không cài thay owner hoặc coi danh sách rỗng/realtime đóng stream là integration thành công. Cài U14 consumer và kiểm bằng mock; nhắc hạn thật, group recipient và SSE hai instance phải chờ owner providers. Không tạo schema giả để che migration thiếu U07/U08/U12; U14 migration FK vào lớp thật, thứ tự2000 sau1900 khi tích hợp. Không đưa U13 vào chỉ để lấp số migration.

Mọi gate còn mở phải xuất hiện trong code-summary và tester matrix: migration PostgreSQL thật, event payload owner/group và contract ACTIVE, Rabbit retry/SMTP lỗi/accept-crash, email cap nhiều instance, reminder đổi hạn, SSE hai instance, session/CSRF/audit rollback, browser/mobile và p95/throughput. Không tự đánh UC63–65 Done hoặc chuyển Operations.

## Mapping đường dẫn và kiểm tra theo bước

| Bước | Đường dẫn chính | Kiểm tra cần có |
|---|---|---|
| 1 | `backend/src/main/resources/db/migration/notifications/V20260925_2000__create_notification_tables.sql` | 5 bảng, unique/index/check/FK, grant/revoke đúng; không sửa migration cũ |
| 2 | `announcements/domain/Announcement.java`, `notifications/domain/{Notification,EmailDelivery,NotificationPreference,DeadlineReminder}.java`, I-prefixed repositories; infrastructure adapters/rows | Domain validation, optimistic CAS, stable newest-first, dedup/reminder keys |
| 3 | `announcements/application/{AnnouncementService,AnnouncementAccessPolicy,AnnouncementCommands,AnnouncementViews}.java`, C01 U01 action changes | Role/scope/OPEN, create event after commit, update không resend, soft delete/version409, audit atomic |
| 4 | `notifications/application/{NotificationService,RecipientResolver}.java`, `notifications/infrastructure/{NotificationTopology,NotificationEventListener}.java`, C02 U01 lookup changes | Event từng loại, studentIds/accountId, >20 Admin, duplicate/concurrent event, unsupported schema, retry/ack |
| 5 | `notifications/application/{EmailDeliveryService,EmailSendJobHandler,EmailCapPolicy}.java`, config U14 cap/base URL | Preference SKIPPED, cap DEFERRED, priority, dates/OTP exclusion, bounded retry/SENT terminal/sweeper, safe body |
| 6 | `notifications/application/DeadlineReminderScanner.java` | Exact due window, individual/group/subject, no submitter, late open, changed deadline, unique marker, transaction rollback |
| 7 | `notifications/application/{NotificationPreferenceService,NotificationMaintenanceScanner}.java` | Self/student only, whitelist types, preference changes, deferred order/expired skip, bounded retention |
| 8 | `announcements/web/{AnnouncementController,MyAnnouncementController}.java`, `notifications/web/{NotificationController,NotificationPreferenceController}.java`, exceptions; `docs/contracts/openapi/notifications.yaml` | MVC validation/4roles/404/409, owner-only read/SSE channel, X-Accel-Buffering no, CSRF shared policy |
| 9 | `frontend/src/app/announcements/{layout,page}.tsx`, `features/announcements/{api,types,ClassAnnouncementsPage,AnnouncementFeed,AnnouncementComposer,AnnouncementDialogs,ClassAnnouncementsCard,AnnouncementMarkdown,register}.tsx/ts`, `features/notifications/{api,types,NotificationBell,NotificationPopup,EmailPreferencePanel,register}.tsx/ts`, CSS modules and features/register.ts | Teacher/student reference UI, important/email create-only, card filter, bell HeaderWidget/all roles, preference Student only, SSE cleanup, sanitized markdown, pagination/errors/mobile; C07 security files |
| 10 | `backend/src/test/java/edu/aiplatform/{announcements,notifications}/**`, focused U01 tests; frontend feature and proxy tests | JUnit/Mockito/MockMvc + ArchitectureTest; full mvn verify, Vitest, typecheck/lint/build; production HTTP/browser smoke; independent tester handoff |
| 11 | `aidlc-docs/construction/u14-announcements-and-notifications/code/code-summary.md`, `aidlc-docs/construction/build-and-test/u14-verification.md`, canonical plan/audit/state | Developer review, actual pass/skip/errors, unresolved gates và compliance; người dùng review trước khi chuyển stage |

## Part 1 readiness

- [x] Đọc kế hoạch và toàn bộ functional/NFR/infrastructure artifacts U14, UC/story và contract nguồn.
- [x] Kiểm branch sạch, scaffold, event payload thực tế, U01 cap20 và stub U07/U08/U12.
- [x] Mapping local UI-reference và đường dẫn code/test cho cả11 bước; ghi C01–C08, thay đổi liên unit có giới hạn và acceptance gates.
- [x] Người dùng duyệt kế hoạch cụ thể cùng C01–C08 ngày2026-10-21: approval questions Answer a. Bắt đầu Part2, không commit/push.

## Checklist

- [x] **Bước 1 — Migration:** `db/migration/notifications/V20260925_2000__create_notification_tables.sql` theo infrastructure-design; PostgreSQL thật vẫn là gate tester.
- [x] **Bước 2 — Domain:** `Announcement`, `Notification`, `EmailDelivery`, `NotificationPreference`, `DeadlineReminder` và enum; repository chỉ dùng trong package; backend compile pass.
- [x] **Bước 3 — Thông báo lớp:** `AnnouncementService` (F1–F3), phát `class.announcement-posted`; unit tests quyền/version/audit/no resend pass.
- [x] **Bước 4 — Sự kiện:** queue `jobs.notification`; `NotificationEventListener`, `RecipientResolver`, `NotificationService` (F4, F5, P1); resolver/duplicate/after-commit/listener tests pass.
- [x] **Bước 5 — Email:** `EmailDeliveryService`, `EmailSendJobHandler`, `EmailCapPolicy`, mẫu tối thiểu (F6, P2); cap/preference/deferred/terminal/sanitized retry unit tests pass.
- [x] **Bước 6 — Nhắc hạn:** `DeadlineReminderScanner` (F7, P3); window/once/changed deadline/subject/late-open unit tests pass; provider thật pending.
- [x] **Bước 7 — Tùy chọn, dọn dữ liệu:** `NotificationPreferenceService` (F8), `NotificationMaintenanceScanner` (F9, P4); owner/types/180day cutoff tests pass.
- [x] **Bước 8 — API:** controller theo domain-entities mục4; `docs/contracts/openapi/notifications.yaml`; ngoài phạm vi404; final MVC tests/SSE headers/OpenAPI refs pass.
- [x] **Bước 9 — Frontend:** page/feed/composer/dialog/card, bell/popup/preference và slots; full314pass/61files (22new), final typecheck/lint/build/HTTP nonce and desktop browser fixture QA pass; mobile/full provider acceptance pending.
- [x] **Bước 10 — Unit test (người code viết):** role/scope/version/no resend, recipients/dedup/after-commit, cap/preference/priority/SENT/retry, reminder window/once/changed close/late-open, safe email/retention180day, HTTP and UI. Full backend919total/842pass/77skip; final focused71pass (66U14+3U01+2Arch), packaged JAR; final frontend314pass. Independent integration remains tester work.
- [x] **Bước 11 — Review:** đối chiếu UC 63–65, story, BR-U14 với code; người dùng duyệt bằng “tiếp tục” ngày2026-10-21, tiếp tục chuẩn bị Build & Test trong phạm vi U14.

### Review và bàn giao

- [x] Developer review sửa preference immutable-map null handling, SSE no-buffer headers, SMTP timeout envelope, public URL config, reminder source recheck, deferred priority, read/read-all realtime invalidation và stale query/pagination/UI errors.
- [x] Code summary và build-and-test/u14-verification ghi actual pass/skip/error, baseline failures/retries, dependencies/stub và SMTP crash limits, compliance và independent tester matrix.
- [x] Người dùng review/approve code ngày2026-10-21. Không tự chuyển global stage, commit/push hoặc đánh UC63–65 Done; independent acceptance còn mở.

## Integration test cases (tester viết)

- Đăng thông báo quan trọng có email: người học nhận trong app và trong Mailpit theo tùy chọn.
- SMTP lỗi: nghiệp vụ không rollback, email `FAILED` sau 5 lượt.
- Bài hết hạn sau 24 giờ có người chưa nộp (cá nhân và nhóm): nhắc đúng người, đúng một lần.
- Chuông nhận thông báo qua SSE khi backend có hai instance.
- Phân quyền 4 role trên mọi endpoint, gồm đọc thông báo của người khác và đăng thông báo lớp không dạy.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.

## U14 startup regression correction — 2026-10-22

Scope: check and repair reported configuration bean collision on the refreshed `staging` checkout `0d1cf27`. User reports duplicate `notificationTopology` bean and requests recheck after pulling. No branch switch, commit/push or topology behavior changes.

- [x] Confirm current HEAD matches origin/staging and collision remains in production source.
- [x] Add NotificationTopologyContextTest using a real Spring ApplicationContextRunner, covering bean overriding both disabled and enabled without Rabbit/DB dependencies.
- [x] Reproduce both startup failures before changing production source:2cases fail with overriding false/true; `.tmp/u14-topology-fix/red.log` on staging0d1cf27.
- [x] Rename only the Declarables factory method to notificationDeclarables; preserve queues, exchanges, TTLs and bindings.
- [x] Full backend verify1112total/1033passed/79skipped/0failures/errors and executable JAR,2026-10-22; both context regression cases pass after rename. Logs `.tmp/u14-topology-fix/red.log` and `verify.log`. Audit/state record evidence; no full API/worker startup against real infrastructure claimed. This targeted developer regression does not replace independent full application acceptance.
