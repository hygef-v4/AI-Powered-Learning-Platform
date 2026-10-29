# U14 — Implementation handoff

Ngày2026-10-21, branch `announcements-and-notifications`, UC63–65. Kế hoạch11 bước và C01–C08 được duyệt tại `u14-implementation-approval-questions.md`, Answer a. Người dùng duyệt application code bằng “tiếp tục” ngày2026-10-21; acceptance tích hợp vẫn mở. Theo yêu cầu tiếp theo, source đã chia thành7commit cục bộ từ `6cef92a` đến `adfc51f`, tài liệu trong commit riêng; chưa push/deploy, không gửi email thật.

## Phạm vi

| UC / luồng | Kết quả |
|---|---|
| UC63 / F1 | Feed thông báo của lớp OPEN đang học/dạy, lọc lớp, mới nhất trước, pagination≤50, card đăng ký StudentClassSection |
| UC64 / F2–F3 | Giảng viên chính Teacher/Subject Manager tạo/sửa/xóa mềm; validate title/body, cấm raw HTML, giữ tác giả/thời điểm và important/email gốc; CAS version409; chỉ create phát class.announcement-posted sau commit; audit metadata/hash trước/sau |
| UC65 / F5,F8 | Chuông HeaderWidget cho4role, popup tất cả/chưa đọc, load-more, own-only read/read-all, SSE account channel; preference5loại chỉ Student, mặc định bật |
| F4 | Event envelope/schema/source/type validation; recipients enrollment/assignment/quiz/grade/group/backup/announcement; notification+email atomic và unique dedup; SSE sau commit, read/read-all cũng invalidate các tab khác |
| F6 | EMAIL_SEND qua U03 jobs.email, IMail U01, status/row lock/CAS/terminal SENT, cap300/day Asia/Ho_Chi_Minh, preference SKIPPED, cap DEFERRED, retry hữu hạn và IPendingSweeper |
| F7 | IScheduledScanner Assignment reminder, cửa sổ5phút sau mốc24giờ, lớp/môn OPEN, non-submitters giao với active enrollment, unique bài/lớp/hạn, mở muộn SKIPPED, đổi hạn có dấu mới; email kiểm lại nguồn/hạn trước SMTP |
| F9 | Deferred ưu tiên backup > deadline > important announcement > assignment > grade > enrollment; skip expired reminders; retention180ngày tối đa10.000 mỗi batch |

## Code và thay đổi liên unit

- Backend `announcements/{application,domain,infrastructure,web}` và `notifications/{application,domain,infrastructure,web}`; domain entities dùng JPA, adapters dùng EntityManager/JPQL và native PostgreSQL cho ON CONFLICT/cleanup/advisory lock. Hai package thuộc cùng U14, repository không mở ra contract cho unit khác.
- Migration `backend/src/main/resources/db/migration/notifications/V20260925_2000__create_notification_tables.sql`:5bảng, FK announcements->course_classes, unique event/recipient/type, email/notification và reminder; chỉ cấp DELETE trực tiếp notifications, email con cascade để retention không để orphan. Không sửa migration cũ hoặc tạo bảng owner giả.
- U01 được sửa đúng phạm vi duyệt: `Actions`, `AccessPolicy` thêm ANNOUNCEMENT_READ/MANAGE và NOTIFICATION_SELF; `IAccountLookup`, `AccountLookupService`, `AccountRepository`, `IdentityContractStubs` thêm ACTIVE Admin keyset pages≤100, không dùng search cap20 cho fanout; default/stub fail closed nếu provider chưa có. Có unit test hơn20và125Admin/multiple pages. Không đổi login hay quyền cũ.
- Không sửa contract/implementation U03/U05/U06/U07/U08/U12. EMAIL_SEND/queue đã có U03; U14 tự khai báo queue sự kiện jobs.notification và5retry queue TTL cùng parking queue notifications.failed. DomainEvent retry không đưa vào retry queue JobMessage của U03. Worker listener manual ack, publisher-confirm trước ack khi retry; unsupported schema không tạo dữ liệu, malformed/exhausted vào parking, không log payload.
- Frontend `features/announcements`, `features/notifications`, route `/announcements`; dùng SessionProvider/CSRF/API/slots/shared Dialog, local fonts và icons. Dependency mới được duyệt: react-markdown10.1.0, rehype-sanitize6.0.0; raw HTML/unsafe links/images bị chặn. Không dùng raw HTML injection hay UI kit mới.
- C07 áp riêng CSP solution từ U13 vào `src/proxy.ts`, `src/proxy.test.ts`, root layout request rendering; không merge U13 hoặc đổi branch history. Script nonce256bit theo request/strict-dynamic, DENY/frame-ancestors none và đủ HSTS/nosniff/referrer. Style attribute exception của React và eval/HMR chỉ development được ghi trong source; production script không unsafe-inline/eval.
- API spec `docs/contracts/openapi/notifications.yaml` (OpenAPI3.1, JSON-compatible YAML1.2) khớp12operation/9path, cookie access_token, CSRF header, pagination/error/schema. File không được frontend gọi lúc runtime.
- Config dùng `U14_EMAIL_DAILY_CAP` đã được compose truyền sẵn; link email dùng `APP_PUBLIC_URL` hiện có, không thêm localhost URL mặc định vào production config. SMTP vẫn do U01 cấu hình và gửi.

## Bằng chứng developer checks

| Kiểm tra | Kết quả và giới hạn |
|---|---|
| Full backend clean verify (historical) | 919total /842pass /77skip /0failure/error; JAR. Chạy trước các sửa review cuối về read realtime, email timeout/cap/source/config |
| Full backend verify after code approval | 921total /844pass /77skip /0failure/error; executable JAR, 85s. Final source; `.tmp/u14-build-test-verify.log`, 2026-10-21T16:35:07+07:00 |
| Backend final focused package | 71pass /0failure/error/skip:66U14 +3U01 +2architecture; executable JAR packaged sau sửa review cuối. Không cộng vào full để tạo số pass giả |
| Full frontend cuối | 314pass /61files, gồm22test mới của announcements/notification/proxy; source cuối sau sửa query/pagination/stale request |
| Typecheck/lint/build | Pass; lint không warning/error; production standalone build cuối pass |
| HTTP/OpenAPI smoke | announcement/login200, unknown route404, repeat request nonce mới; nonce header khớp mọi script, không tin client nonce, private/no-store; local OpenAPI refs/syntax pass |
| Browser production fixture | Teacher list/create/edit/delete/important-email behavior, sanitized Markdown, Teacher không thấy learning preferences; Student không thấy mutation/composer,5preferences, read-all count0; console không CSP/hydration warning/error |
| Dependency audit | npm audit --omit=dev trả0vulnerability tại thời điểm kiểm. npm install có báo5high ở toàn dev tree; không tự nâng dependency ngoài phạm vi hoặc chạy audit fix force |

Windows sandbox SWC/Maven cache/temp/IPC cần retry lệnh được cấp quyền; stale `.next` route types của U13 được xóa trong đúng generated folders rồi tái sinh cho branch này. Full frontend lần đầu chạy cạnh tranh CPU với build có5test cũ timeout5000ms; lần cuối maxWorkers2 pass toàn bộ, không đổi timeout hay test cũ để bỏ lỗi. Logs cục bộ ignored và lệnh tái hiện trong [u14-verification.md](../../build-and-test/u14-verification.md).

UI lấy bố cục từ local UI-reference `s06`, `t08`, `t08b`, `t08c`, `c14`; một số wording mẫu đã cũ (email quan trọng luôn gửi, group-email toggle) được thay bằng BR-U14-20/21 và5loại email học tập đã duyệt. Fixture chỉ trong `.tmp`, không nằm trong production source/build.

## Gate còn mở

1. U07 IAssignmentQuery, U08 ISubmissionQuery, U12 ISseHub hiện còn stub trên branch. Reminder/group submission thực, group event payload owner và SSE2instance cần providers thật. Stub rỗng/stream đóng ngay không chứng minh feature vận hành; frontend có fallback refresh.
2. Chạy migration/boot/Hibernate validate trên PostgreSQL thật cùng migration prerequisites của các owner; SQL constraints, CAS/race, cap/advisory lock nhiều worker, dedup concurrent, retention cascade/grants và transaction rollback độc lập chưa được coder integration-test chứng nhận.
3. Rabbit topology/TTL/routing/publisher confirms/retry parking và recovery cần broker thật. Republish lỗi có thể redeliver message; uniqueDB chống trùng notice nhưng hành vi recovery/monitor parking queue cần tester kiểm.
4. SMTP thật/Mailpit/Brevo fault/retry/cancel và cap qua ngày cần tester. IMail không nhận provider idempotency key: SMTP accept rồi process crash trước SENT commit có crash window không thể bảo đảm exactly-once tuyệt đối. Row lock/SENT terminal chỉ xử lý duplicate delivery/concurrent worker trong DB; không che giới hạn này.
5. HTTP với current JWT/session/CSRF thật, đổi role/disable sau event, account channel isolation/expiry, denied/success audit rollback và scope lifecycle race cần full-stack tests.
6. UI fixture không chứng minh quyền/provider thật. Cần browser/mobile acceptance, card trên U05 và header trên4role với API thật; chưa đo viewport mobile trong browser này hoặc chứng nhận NFR.
7. Chưa đo p95≤300ms, fanout200student≤5s, SSE≤5s, reminder5phút và resource release dưới fault/stress. Contract listClosingBetween không pagination; scanner xử lý tối đa500candidate/500class mỗi batch, cần đo/đánh giá cardinality và owner paging khi scale vượt giới hạn này.
8. Người dùng đã duyệt code (bước11); independent tester/UC/story acceptance còn mở; không chuyển global stage/Operations. Xem [Build & Test summary](../../build-and-test/build-and-test-summary.md).

## Compliance trong phạm vi developer review

| Rule | Kết quả | Giới hạn |
|---|---|---|
| SECURITY-03 | Không log body/email/raw event/provider error; audit metadata/hash allowlist; shared logging framework | Centralized deployment logs chưa kiểm |
| SECURITY-04 | Nonce/headers/request rendering implemented; production HTTP và browser checks pass | Nginx/HTTPS deployment composition pending |
| SECURITY-05 | Input lengths/version/type whitelist, safe markdown/link, parameterized DB, bounded page/event sizes | Real schema/fault/load tests pending |
| SECURITY-08 | Principal/self/class authorization fail closed; Student read-only, Admin không vào lớp; unit/HTTP consumer checks |4role matrix/session/providers thật pending |
| SECURITY-09 | No secret/auth bypass, local fonts, production no unsafe script; env config reused | Shared deployment hardening unchanged |
| SECURITY-12 | U01 session/current account authority consumed; no new password/credential | Auth/MFA provider belongs U01 |
| SECURITY-15 | Safe errors, after-commit fanout, CAS/dedup, bounded retries/locks/scanners | DB/SMTP accept-crash/Rabbit faults pending |
| RESILIENCY-04/06 | N/A: no deployment/rollback/health mechanism changed | Existing deployment health/restore acceptance separate |
| RESILIENCY-10 | Bounded event payload/retry, email job35s/transaction30s with existing SMTP timeouts, lock2s and batch limits | Actual provider cancellation/load and scanner cardinality pending |
| Other rules / PBT | N/A reduced scope / PBT disabled | Existing extension decision retained |
