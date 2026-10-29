# U15 Admin Dashboard — implementation handoff

Ngày 2026-10-20, theo phê duyệt “Cứ giúp tôi code trc theo plan còn khi nào u3 xong thì xử lý sau”. Triển khai phương án A của [plan](../../plans/code-generation-plan/u15-admin-dashboard-code-generation-plan.md), áp dụng Clean Code và Superpowers executing-plans/TDD/review/verification. Đây là consumer slice code-ready; chưa nghiệm thu toàn bộ UC66/KAN-80.

## Đã triển khai

- Trang `/admin` trong Admin layout hiện có; ADMIN homePath chuyển từ `/admin/accounts` sang `/admin`, các role khác giữ nguyên. NavEntry hiện hữu được tái sử dụng, không đăng ký trùng.
- GET `/api/v1/admin/dashboard`: counts tài khoản theo role/status, môn/lớp theo status, ghi danh ACTIVE, backup mới nhất/ba lần gần nhất và lịch dự kiến. Section AVAILABLE rỗng khác UNAVAILABLE; không lấy lỗi nguồn làm số 0. Sparse map hợp lệ được điền 0; null/âm/overflow trở thành unavailable.
- GET `/api/v1/admin/dashboard/backups?page=0&size=20`: phân trang 0-based, size 1..100. Nguồn lỗi trả 503; không giả lịch sử trống. Whitelist query, chặn tham số lặp/sai cú pháp/overflow; khoảng số nguyên được service kiểm sau guard.
- Actor lấy từ server principal; service kiểm ADMIN và IAuthorization với current ACTIVE account trước fan-out, ghi ACCESS_DENIED/DENIED khi từ chối. Authorization/audit lỗi thì fail closed. Hai API read-only có no-store; exception trả thông báo an toàn.
- Một service orchestration/mapping, immutable DTO và một reader quản lý lifecycle pool: 8 worker, queue 32, AbortPolicy, deadline 2 giây từ submit gồm queue. Submit 7 nguồn trước await; completion timestamp giữ kết quả hoàn thành đúng hạn. Cancel Future là best effort, không đồng nghĩa đã hủy JDBC.
- UI reuse shared PageHeader/Table/Alert/Dialog/Pagination, loading/error/retry, refresh không giữ metrics cũ, abort/ignore phản hồi cũ sau cleanup. History 401/403 đóng dialog và xóa metrics. Escape/Tab/focus return dùng shared Dialog. Không polling/cache counts hoặc download/restore/run backup; CTA settings đi `/admin/settings`.

## Phạm vi contract

Giữ nguyên IBackupStatus, BackupRunView, ISettings, IAccountLookup, ISubjectQuery và IClassAccess cùng provider U03/U04/U05. Chỉ bổ sung action ADMIN_DASHBOARD_VIEW/policy ADMIN_ONLY trong registry U01 và chuyển ADMIN_HOME có test hồi quy. U15 không có schema/migration/dependency/config mới.

JSON chỉ chứa tổng hợp và whitelist metadata backup; không account contact, ID, archive reference/hash hoặc stack trace. [OpenAPI](../../../../docs/contracts/openapi/admin-dashboard.yaml) ghi hai GET và DTO thực tế; đã parse YAML và kiểm mọi local reference, không coi đó là chứng nhận runtime schema toàn hệ thống.

Các quyết định C01–C07 trong [contract review](implementation-contract-review-2026-10-20.md) đã áp dụng cho slice A:

- `startedAt` luôn null vì U03 chưa cấp; không dùng snapshotAt giả thời điểm bắt đầu.
- `latestSuccessfulAt` và `failuresLast7Days` là UNAVAILABLE. Không suy từ ba lần chạy hoặc scan toàn bộ lịch sử. Chờ public contract U03 rồi bổ sung consumer mapping/UI/tests.
- `nextPlannedAt` chỉ dự kiến theo backup.enabled/dailyAt, Asia/Ho_Chi_Minh; disabled mặc định false, enabled thiếu giờ thì unavailable. Giữ cache 30 giây/stale fallback của ISettings.
- Retention giữ bản SUCCESS mới nhất dù quá 7 ngày; chỉ purgedAt xác nhận đã xóa. Count ghi danh ACTIVE gồm cả lớp DRAFT/ARCHIVED theo port hiện hành, không gán tất cả cho lớp OPEN.

## Verification và independent review

Fresh baseline trước production edits: backend 724 total / 662 passed / 62 skipped; frontend 232 passed / 47 files. Logs scratch tại `.tmp/u15/`, không đưa build output vào Git.

| Kiểm tra cuối | Kết quả |
|---|---|
| Backend Maven verify sau review fix | 755 total, 693 passed, 62 skipped, 0 failures/errors; executable JAR packaged |
| Backend focused U15 + policy + AuthService + ArchUnit | 49 passed, 0 failures/errors/skips |
| Frontend U15 focused | 21 passed / 5 files |
| Frontend full | 253 passed / 52 files |
| ESLint / TypeScript | Pass; typecheck lại sau restore tsconfig do Next tự sinh |
| Next production build | Pass; `/admin` được prerender, standalone output; frontend production code không đổi sau build |
| OpenAPI / whitespace / provider scope | YAML và local refs hợp lệ; diff check pass; không sửa provider U03/U04/U05 |

TDD ghi nhận RED thiếu production classes/modules, RED homePath `/admin` trước cutover, rồi GREEN từng batch. Independent reviewer đọc source/test/contract và chỉ ra một P2: controller kiểm range phân trang trước guard làm non-ADMIN nhận 400 và thiếu audit. Thêm ba case HTTP qua service thật (page=-1/size=0/size=101), thấy cả ba RED 400 thay vì 403. Bỏ range validation ở controller, cho signed integer đi tới service guard; cùng test kiểm mọi non-ADMIN 403 + DENIED audit, anonymous 401 + audit, ADMIN 400, không đọc provider. Focused và full backend GREEN sau sửa. Cú pháp/unknown/repeated/overflow vẫn bị chặn ở web boundary. Không còn finding cụ thể khác; không dispatch review vòng lặp.

Thêm public-UI characterization cho history 401/403 xóa dashboard và Escape/Tab/focus return; không sửa production frontend sau review. Commands và ma trận tester: [U15 verification](../../build-and-test/u15-verification.md).

## Các gate còn mở

1. C01/C02: U03 bàn giao metadata bắt đầu và latest-success/7-day summary cùng absence/time-window/error semantics; không tự đổi contract owner.
2. Tester độc lập với real sessions/current role/account revoked và audit persisted; provider thật cho counts/settings/latest/history. MockMvc standalone/Mockito không chứng minh security filter chain/JWT, DB wiring hoặc audit transaction thật.
3. Full application startup với contract stubs disabled và migrations/provider đầy đủ. Prerequisite migration U07/U08 của U09 có trước U15; không sửa lan sang unit khác. 62 skipped integration tests được giữ, không tính là pass.
4. Browser login redirect `/admin`, real history paging/error, request cancellation/freshness, không lộ PII/archive. jsdom/build không thay thế nghiệm thu browser.
5. NFR: benchmark healthy-source p95 <= 1 giây; fault deadline 2 giây + overhead; stress nguồn bỏ qua interrupt và xác nhận connection/query được giải phóng. Bounded pool và unit deadline tests không chứng minh cancellation tài nguyên thật.
6. Requirement/code/test-case owners độc lập theo dự án; AI review không thay tester acceptance. Chưa full UC66 Done, chưa đổi Jira trong lượt code này.

## Workflow, extensions và rollback

Tái sử dụng checkout `grading-gradebook` tại HEAD 6148e430, giữ audit/state/planning dirty từ lượt trước; không reset hay ghi Git index. Bookkeeping shell của skill được chuyển sang PowerShell ledger `.tmp/u15/progress.md`; plan/audit/handoff là bản lưu lâu dài. Không commit/push/merge.

Security03/05/08/12/15: local coverage cho safe errors, query parsing, server actor/current authorization, deny audit và DTO privacy. Security04/09: dùng platform security/headers/session, không nới config; no-store được kiểm tại HTTP controller, full-chain headers còn integration gate. Resiliency04: rollback ADMIN_HOME về `/admin/accounts` trước rồi gỡ route/API, không data migration. Resiliency06: kế thừa health, full startup gate còn mở. Resiliency10: isolation/deadline/rejection/interrupt có unit coverage, JDBC release/NFR còn mở. Các rule ngoài reduced scope N/A theo state; Property-Based Testing disabled, không tự bật. Không tuyên bố deployed/runtime compliance.

## Phase B addendum — 2026-10-22

User approved U03/U15 work. C01/C02 provider/consumer are implemented: actual startedAt, latest historical SUCCESS completedAt and final FAILED count for168h, with safe errors and empty-vs-outage states. Legacy backup contracts unchanged; U15 latestSuccessfulAt allows AVAILABLE/null. See [execution evidence](backup-completion-verification-2026-10-22.md).

Backend verify1250 passed/35 environment-gated skipped; backup/U15 tests68 passed incl PostgreSQL8. Frontend448 passed; lint/typecheck/build pass. C01/C02 missing-provider implementation gate is superseded; independent full-chain/session/audit/browser, stubs-disabled startup and dashboard load/latency acceptance remain open. No implementation commit/push/merge/Jira transition.
